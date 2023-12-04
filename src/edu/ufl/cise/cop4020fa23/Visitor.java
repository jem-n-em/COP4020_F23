package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import java.util.List;
public class Visitor implements ASTVisitor {
    //Visit method returns - return any variables that the parent element depends on to fulfill some condition
    SymbolTable symbolTable = new SymbolTable();

    Type programType;
    /*===== Visit methods implemented in order of recursive traversal =======*/
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        //set type
        Type type = Type.kind2type(program.getTypeToken().kind());
        program.setType(type);
        programType = type;
        symbolTable.enterScope();
        List<NameDef> paramList = program.getParams();
        for(NameDef nameDef: paramList){
            nameDef.visit(this, arg);
        }
        program.getBlock().visit(this, arg);
        symbolTable.exitScope();
        return programType;
    }
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        symbolTable.enterScope();
        //check children
        List<Block.BlockElem> blockElems = block.getElems();
        for(Block.BlockElem elem: blockElems){
            elem.visit(this, arg);
        }
        symbolTable.exitScope();
        return block;
    }

    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {

        //get type
        Type type = Type.kind2type(nameDef.getTypeToken().kind());
        nameDef.setType(type);
        //check type conditions

        TypeCheckException e = new TypeCheckException(nameDef.firstToken.sourceLocation(), "Invalid type for " + nameDef.getName());
        Dimension dimension = nameDef.getDimension();
        if(dimension != null){
            dimension.visit(this, arg);
            if(type != Type.IMAGE)
                throw e;
        }
        else{
            if(type == Type.VOID)
                throw e;
        }
        //insert namedef into symbol table
        symbolTable.insert(nameDef);
        return type;
    }
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        //conditions for expr
        Expr initializer = declaration.getInitializer();
        Type exprType;
        Type nameDefType;
        if(initializer != null){
            exprType = (Type)declaration.getInitializer().visit(this, arg);
            nameDefType = (Type)declaration.getNameDef().visit(this, arg);
            if(!(exprType == nameDefType || exprType == Type.STRING && nameDefType == Type.IMAGE)){
                throw new TypeCheckException(declaration.firstToken().sourceLocation(), "Invalid types in declaration");
            }
        }
        else{
           nameDefType = (Type)declaration.getNameDef().visit(this, arg);
        }

        declaration.setType(nameDefType);
        return declaration;
    }

    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        Type guardType = (Type) conditionalExpr.getGuardExpr().visit(this, arg);
        Type trueType = (Type) conditionalExpr.getTrueExpr().visit(this, arg);
        Type falseType = (Type) conditionalExpr.getFalseExpr().visit(this, arg);
        boolean guardExprType = guardType == Type.BOOLEAN;
        boolean trueFalseEq = trueType == falseType;
        if(!(guardExprType && trueFalseEq))
            throw new TypeCheckException(conditionalExpr.firstToken.sourceLocation(), "Invalid types in conditional expr");
        Type type = trueType;
        conditionalExpr.setType(type);
        return type;
    }

    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException{
        Type binaryType = null;
        Type leftType = (Type)binaryExpr.getLeftExpr().visit(this, arg);
        Type rightType = (Type)binaryExpr.getRightExpr().visit(this, arg);
        switch (binaryExpr.getOp().kind()){
            case BITAND , BITOR -> {
                if(leftType == Type.PIXEL && rightType == Type.PIXEL) binaryType = Type.PIXEL;
            }
            case AND, OR -> {
                if(leftType == Type.BOOLEAN && rightType == Type.BOOLEAN)
                    binaryType = Type.BOOLEAN;
            }
            case LT, GT, LE, GE -> {
                if(leftType == Type.INT && rightType == Type.INT)
                    binaryType = Type.BOOLEAN;
            }
            case EQ -> {
                if(leftType == rightType)
                    binaryType = Type.BOOLEAN;
            }
            case EXP -> {
                if(leftType == Type.INT && rightType == Type.INT)
                    binaryType = Type.INT;
                else if(leftType == Type.PIXEL && rightType == Type.INT)
                    binaryType = Type.PIXEL;
            }
            case PLUS -> {
                if(leftType == rightType)
                    binaryType = leftType;
            }
            case MINUS -> {
                if((leftType == Type.INT ||leftType == Type.PIXEL || leftType == Type.IMAGE)
                        && rightType == leftType)
                    binaryType = leftType;
            }
            case TIMES, DIV, MOD -> {
                if((leftType == Type.PIXEL || leftType == Type.IMAGE) && (rightType == leftType || rightType == Type.INT) ||
                    leftType == Type.INT && rightType == leftType)
                    binaryType = leftType;
            }
            default -> throw new TypeCheckException(binaryExpr.firstToken.sourceLocation(), "Invalid types at binary expr");

        }
        binaryExpr.setType(binaryType);
        return binaryType;
    }

    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        Type unaryType;
        Type exprType = (Type)unaryExpr.getExpr().visit(this, arg);
        Kind op = unaryExpr.getOp();
        if(exprType == Type.BOOLEAN && op == Kind.BANG)
            unaryType = Type.BOOLEAN;
        else if(exprType == Type.INT && op == Kind.MINUS)
            unaryType = Type.INT;
        else if(exprType == Type.IMAGE && (op == Kind.RES_height || op == Kind.RES_width))
            unaryType = Type.INT;
        else{
            throw new TypeCheckException(unaryExpr.firstToken.sourceLocation(), "Invalid types at unary expr");
        }
        unaryExpr.setType(unaryType);
        return unaryType;
    }
    //PostfixExpr::= Expr PixelSelector? ChannelSelector?
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException{
        Type postFixType = null;
        Type exprType = (Type)postfixExpr.primary().visit(this, arg);
        PixelSelector pixelSelector = postfixExpr.pixel();
        ChannelSelector channelSelector = postfixExpr.channel();
        boolean pixSelect = pixelSelector != null;
        boolean channelSelect = channelSelector != null;
        if(pixSelect)
                pixelSelector.visit(this, false);
        if(channelSelect)
                channelSelector.visit(this, arg);
        if(!(pixSelect || channelSelect))
            postFixType = exprType;
        else if(exprType == Type.IMAGE){
            if(pixSelect && !channelSelect)
                postFixType = Type.PIXEL;
            else if(pixSelect && channelSelect)
                postFixType = Type.INT;
            else if(!pixSelect && channelSelect)
                postFixType = Type.IMAGE;
        }
        else if(exprType == Type.PIXEL && !pixSelect && channelSelect)
            postFixType = Type.INT;
        else {
            throw new TypeCheckException(postfixExpr.firstToken.sourceLocation(), "Invalid types at postFix expr");
        }
        postfixExpr.setType(postFixType);
        return postFixType;
    }

    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException{
        stringLitExpr.setType(Type.STRING);
        return stringLitExpr.getType();
    }
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException{
        numLitExpr.setType(Type.INT);
        return numLitExpr.getType();
    }

    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException{
        String name = identExpr.getName();
        SymbolTable.Entry entry = symbolTable.lookup(name);

        //assert entry != null: new TypeCheckException(identExpr.firstToken.sourceLocation(), name + " is undeclared or out of scope");
        if(entry == null)
            throw new TypeCheckException(identExpr.firstToken.sourceLocation(), name + " is undeclared or out of scope");
        NameDef nameDef = entry.namedef;
        identExpr.setType(nameDef.getType());
        identExpr.setNameDef(nameDef);
        return identExpr.getType();
    }

    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException{
        Type constType = constExpr.getName().equals("Z") ? Type.INT : Type.PIXEL;
        constExpr.setType(constType);
        return constType;
    }

    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException{
        booleanLitExpr.setType(Type.BOOLEAN);
        return Type.BOOLEAN;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        return channelSelector;
    }
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException{
        Expr xExpr = pixelSelector.xExpr();
        Expr yExpr = pixelSelector.yExpr();
        boolean isLValue = (boolean)arg;
        if(isLValue){
            if(xExpr instanceof IdentExpr){
                SymbolTable.Entry entry = symbolTable.lookup(((IdentExpr) xExpr).getName());
                if(entry == null){
                    SyntheticNameDef syntheticNameDef = new SyntheticNameDef(((IdentExpr) xExpr).getName());
                    symbolTable.insert(syntheticNameDef);
                }
            }
            else{
                if(!(xExpr instanceof NumLitExpr))
                    throw new TypeCheckException(xExpr.firstToken.sourceLocation(), "Invalid type at pixelSelector");
            }
            if(yExpr instanceof IdentExpr){
                SymbolTable.Entry entry = symbolTable.lookup(((IdentExpr) yExpr).getName());
                if(entry == null){
                    SyntheticNameDef syntheticNameDef = new SyntheticNameDef(((IdentExpr) yExpr).getName());
                    symbolTable.insert(syntheticNameDef);
                }
            }
            else {
                if(!(yExpr instanceof NumLitExpr))
                    throw new TypeCheckException(yExpr.firstToken.sourceLocation(), "Invalid type at pixelSelector");
            }

        }

        Type xExprType = (Type)xExpr.visit(this, arg);
        Type yExprType = (Type)yExpr.visit(this, arg);
        if(xExprType != Type.INT){
            throw new TypeCheckException(xExpr.firstToken.sourceLocation(), "Invalid type at pixelSelector");
        }
        if(yExprType != Type.INT){
            throw new TypeCheckException(yExpr.firstToken.sourceLocation(), "Invalid type at pixelSelector");
        }
        return pixelSelector;
    }
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException{
        TypeCheckException e = new TypeCheckException(expandedPixelExpr.firstToken.sourceLocation(), "Invalid type at expandedPixelExpr");
        Type redType = (Type)expandedPixelExpr.getRed().visit(this, arg);
        Type greenType = (Type)expandedPixelExpr.getGreen().visit(this, arg);
        Type blueType = (Type)expandedPixelExpr.getBlue().visit(this, arg);
        if(redType != Type.INT && greenType != Type.INT && blueType != Type.INT)
            throw e;
        expandedPixelExpr.setType(Type.PIXEL);
        return expandedPixelExpr.getType();
    }
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        TypeCheckException e = new TypeCheckException(dimension.firstToken.sourceLocation(), "Invalid type at dimension");
        Type heightType = (Type)dimension.getHeight().visit(this, arg);
        Type widthType = (Type)dimension.getWidth().visit(this, arg);
        if(!(heightType == Type.INT && widthType == Type.INT))
            throw e;
        return dimension;
    }
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        SymbolTable.Entry entry = symbolTable.lookup(lValue.getName());
        if(entry == null)
            throw new TypeCheckException(lValue.firstToken.sourceLocation(), "Undeclared variable, LValue");
        NameDef nameDef = entry.namedef;
        nameDef.visit(this, arg);
        lValue.setNameDef(nameDef);

        boolean pixSelect = lValue.getPixelSelector() != null;
        boolean channelSelect = lValue.getChannelSelector() != null;
        if(pixSelect)   //lValue has pixel selector
            lValue.getPixelSelector().visit(this, true);    //pass in to let pixelSelector know parent is an lValue
        if(channelSelect)   //lValue has channel selector
            lValue.getChannelSelector().visit(this, arg);

        //type checking varType
        TypeCheckException e = new TypeCheckException(lValue.firstToken.sourceLocation(), "Invalid type at lValue");
        assert !channelSelect || (lValue.getVarType() == Type.PIXEL || lValue.getType() == Type.IMAGE) : e;
        assert !pixSelect || lValue.getVarType() == Type.IMAGE : e;
        Type inferLValueType = null;
        if(!(pixSelect || channelSelect))
            inferLValueType = lValue.getVarType();
        else if(lValue.getVarType() == Type.IMAGE){
            if(pixSelect && !channelSelect)
                inferLValueType = Type.PIXEL;
            else if(pixSelect && channelSelect)
                inferLValueType = Type.INT;
            else if(!pixSelect && channelSelect)
                inferLValueType = Type.IMAGE;
        }
        else if(lValue.getVarType() == Type.PIXEL && !pixSelect && channelSelect){
            inferLValueType = Type.INT;
        }
        else{
            throw new TypeCheckException(lValue.firstToken.sourceLocation(), "inferLValue type is not defined");
        }

        lValue.setType(inferLValueType);
        return lValue;
    }
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException{
        symbolTable.enterScope();
        //assignment compatibility
        LValue lValue = (LValue)assignmentStatement.getlValue().visit(this, arg);
        Type lValueType = lValue.getType();
        Type exprType = (Type)assignmentStatement.getE().visit(this, arg);

        if(!assignmentCompatible(lValueType, exprType))
            throw new TypeCheckException(assignmentStatement.firstToken.sourceLocation(), "Invalid types at assignment statement");
        symbolTable.exitScope();
        return assignmentStatement;
    }
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException{
        Type guardType = (Type)guardedBlock.getGuard().visit(this, arg);
        guardedBlock.getBlock().visit(this, arg);
        TypeCheckException e = new TypeCheckException(guardedBlock.firstToken.sourceLocation(), "Invalid type at guardedBlock");
        if(guardType != Type.BOOLEAN){
            throw e;
        }
        return guardedBlock;
    }
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws TypeCheckException, PLCCompilerException{
        Type type = (Type)returnStatement.getE().visit(this, arg);
        TypeCheckException e = new TypeCheckException(returnStatement.firstToken.sourceLocation(),
                "Expected " + programType + " but was " + type);
        if(type != programType){
            throw e;
        }
        return returnStatement;
    }
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        Type type = (Type)writeStatement.getExpr().visit(this, arg);
        return writeStatement;
    }
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        for(GuardedBlock block: doStatement.getGuardedBlocks()){
            block.visit(this, arg);
        }
        return doStatement;
    }
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        for(GuardedBlock block: ifStatement.getGuardedBlocks()){
            block.visit(this, arg);
        }
        return ifStatement;
    }
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException{
        statementBlock.getBlock().visit(this, arg);
        return statementBlock;
    }
    /*######################### HELPER METHODS ##############################*/
    public boolean assignmentCompatible(Type lValueType, Type exprType){
        return lValueType == exprType ||
                lValueType == Type.PIXEL && exprType == Type.INT ||
                lValueType == Type.IMAGE && (exprType == Type.PIXEL || exprType == Type.INT || exprType == Type.STRING);
    }

}
