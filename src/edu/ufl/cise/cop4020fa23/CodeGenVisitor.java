package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;

import java.util.ArrayList;
import java.util.List;

public class CodeGenVisitor implements ASTVisitor {
    List<String> imports;

    CodeGenVisitor(){
        this.imports = new ArrayList<>();
    }
    public String createImports(){
        StringBuilder importStrings = new StringBuilder();
        for(String i: imports){
            importStrings.append(i);
        }
        return importStrings.toString();
    }
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException{
        //accept package name?
        //retrive necessary fields by calling visit on each one
        StringBuilder strProgram = new StringBuilder();
        String ident = program.getName();
        String type = getTypeString(program.getType());
        List<String> paramStrings = new ArrayList<>();
        for(NameDef nameDef: program.getParams()){
            paramStrings.add((String)nameDef.visit(this, arg));
        }
        String block = (String)program.getBlock().visit(this, arg);

        strProgram.append("package edu.ufl.cise.cop4020fa23;\n");
        strProgram.append(createImports());
        strProgram.append("public class ");
        strProgram.append(ident);
        strProgram.append("{\n\t public static ");
        strProgram.append(type);
        strProgram.append(" apply(");
        if(paramStrings.size() >= 1){
            strProgram.append(paramStrings.get(0));
            for(int i = 1; i < paramStrings.size(); i++){
                strProgram.append(",");
                strProgram.append(paramStrings.get(i));
            }
        }
        strProgram.append(")");
        strProgram.append(block);
        strProgram.append("}");
        return strProgram.toString();
    }
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        StringBuilder strBlock = new StringBuilder();
        strBlock.append("{\n\t");
        //check children
        List<Block.BlockElem> blockElems = block.getElems();
        for(Block.BlockElem elem: blockElems){
            strBlock.append((String)elem.visit(this, arg));
            strBlock.append(";\n");
        }
        strBlock.append("}");
        return strBlock.toString();
    }
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        StringBuilder strNameDef = new StringBuilder();
        strNameDef.append(getTypeString(nameDef.getType()));
        strNameDef.append(" ");
        strNameDef.append(nameDef.getJavaName());
        return strNameDef.toString();
    }
    public String getTypeString(Type type) throws PLCCompilerException{
        switch(type){
            case INT -> {return "int";}
            case STRING -> {return "String";}
            case VOID -> {return "void";}
            case BOOLEAN -> {return "boolean";}
            default -> throw new CodeGenException("String not available for given type");
        }
    }

    public String getOpString(Kind op) throws PLCCompilerException{
        String opStr = "";
        opStr = switch(op){
            case PLUS -> "+";
            case MINUS -> "-";
            case TIMES -> "*";
            case DIV -> "/";
            case MOD -> "%";
            case BANG -> "!";
            case LE -> "<=";
            case GE -> ">=";
            case BITAND -> "&";
            case BITOR -> "|";
            case AND -> "&&";
            case OR -> "||";

            default -> throw new PLCCompilerException("Op is not an operand");
        };
        return opStr;
    }
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        StringBuilder strDeclaration = new StringBuilder();
        strDeclaration.append((String)declaration.getNameDef().visit(this, arg));
        if(declaration.getInitializer() != null){
            strDeclaration.append(" = ");
            strDeclaration.append((String)declaration.getInitializer().visit(this, arg));
        }
        return strDeclaration.toString();
    }
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        StringBuilder strConditional = new StringBuilder();
        strConditional.append("( ");
        strConditional.append((String)conditionalExpr.getGuardExpr().visit(this, arg));
        strConditional.append(" ? ");
        strConditional.append((String)conditionalExpr.getTrueExpr().visit(this, arg));
        strConditional.append(" : ");
        strConditional.append((String)conditionalExpr.getFalseExpr().visit(this, arg));
        strConditional.append(")");

        return strConditional.toString();
    }
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        StringBuilder strBinary = new StringBuilder();
        String strLeft = (String)binaryExpr.getLeftExpr().visit(this, arg);
        String strRight = (String)binaryExpr.getRightExpr().visit(this, arg);
        if(binaryExpr.getOpKind() == Kind.EQ && binaryExpr.getLeftExpr().getType() == Type.STRING ){
            strBinary.append(strLeft);
            strBinary.append(".equals(");
            strBinary.append(strRight);
            strBinary.append(")");
            return strBinary.toString();
        }
        else if(binaryExpr.getOpKind() == Kind.EXP){
            strBinary.append("((int)Math.round(Math.pow(");
            strBinary.append(strLeft);
            strBinary.append(",");
            strBinary.append(strRight);
            strBinary.append(")))");
            return strBinary.toString();
        }
        else {
            strBinary.append("(");
            strBinary.append(strLeft);
            strBinary.append(binaryExpr.getOp().text());
            strBinary.append(strRight);
            strBinary.append(")");
            return strBinary.toString();
        }
    }
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        StringBuilder strUnary = new StringBuilder();
        strUnary.append("(");
        strUnary.append(getOpString(unaryExpr.getOp()));
        strUnary.append((String)unaryExpr.getExpr().visit(this, arg));
        strUnary.append(")");
        return strUnary.toString();
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        return identExpr.getNameDef().getJavaName();
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        return numLitExpr.getText();
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        return stringLitExpr.getText();
    }
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        return booleanLitExpr.getText().equals("TRUE") ? "true" : "false";
    }
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        return lValue.getNameDef().getJavaName();
    }
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException{
        StringBuilder strAssign = new StringBuilder();
        strAssign.append((String)assignmentStatement.getlValue().visit(this, arg));
        strAssign.append( " = ");
        strAssign.append((String)assignmentStatement.getE().visit(this, arg));
        return strAssign.toString();
    }
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        //import string handling
        String consoleIOImport = "import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;\n";
        if(!imports.contains(consoleIOImport))
            imports.add(consoleIOImport);
        StringBuilder strWrite = new StringBuilder();
        strWrite.append("ConsoleIO.write(");
        strWrite.append((String)writeStatement.getExpr().visit(this, arg));
        strWrite.append(")");
        return strWrite.toString();
    }
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        StringBuilder strReturn = new StringBuilder();
        strReturn.append("return ");
        strReturn.append((String)returnStatement.getE().visit(this, arg));
        return strReturn.toString();
    }
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        return (String)statementBlock.getBlock().visit(this, arg);
    }

    /*=================== ASSIGNMENT 5 METHODS ==================*/

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        return null;
    }
    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        return null;
    }
    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        return null;
    }

}