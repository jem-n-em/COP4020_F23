package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.Dimension;
import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;

import java.awt.*;
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
            importStrings.append(i).append('\n');
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
            case INT, PIXEL -> {return "int";}
            case STRING -> {return "String";}
            case VOID -> {return "void";}
            case BOOLEAN -> {return "boolean";}
            case IMAGE -> {return "BufferedImage";}
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
        if(declaration.getNameDef().getType() != Type.IMAGE){   //Assignment 4 implementation
            strDeclaration.append((String)declaration.getNameDef().visit(this, arg));
            if(declaration.getInitializer() != null){
                strDeclaration.append(" = ");
                strDeclaration.append((String)declaration.getInitializer().visit(this, arg));
            }
        }
        //Implementation with type = image
        else{
            imports.add("import java.awt.image.BufferedImage;");
            Dimension dimension = declaration.getNameDef().getDimension();
            Expr initializer = declaration.getInitializer();
            strDeclaration.append("final BufferedImage ");
            strDeclaration.append(declaration.getNameDef().getJavaName());
            //all branches for initializer types
            if(declaration.getInitializer() == null){   //Declaration ::= NameDef
                addImport("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;");
                if(dimension == null)
                    throw new CodeGenException("Dimension expected in Namedef");
                strDeclaration.append(" = ImageOps.makeImage(");
                strDeclaration.append((String)dimension.visit(this, arg));
                strDeclaration.append(")");
            }
            else{   //Declaration ::= NameDef Expr
                if(initializer.getType() == Type.STRING){
                    addImport("import edu.ufl.cise.cop4020fa23.runtime.FileURLIO;");

                    String source = (String)initializer.visit(this, arg);
                    strDeclaration.append(" = FileURLIO.readImage(");
                    strDeclaration.append(source);
                    //up until this point, string is: final BufferedImage = FileURLIO.readImage(source
                    //if statement for whether dimension is null
                    if(dimension != null){
                        strDeclaration.append(", ");
                        strDeclaration.append((String)dimension.visit(this, arg));
                    }
                    strDeclaration.append(")");
                }
                else if(initializer.getType() == Type.IMAGE){
                    if(dimension == null){
                        strDeclaration.append(" = ImageOps.cloneImage(");
                        strDeclaration.append((String)initializer.visit(this, arg));
                        strDeclaration.append(")");
                    }
                    else {
                        strDeclaration.append(" = ImageOps.copyAndResize(");
                        strDeclaration.append((String)initializer.visit(this, arg));
                        strDeclaration.append(", ");
                        strDeclaration.append((String)dimension.visit(this, arg));
                        strDeclaration.append(")");
                    }
                }
            }
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
    public String getImageOpsOp(IToken op) throws PLCCompilerException{
        switch(op.kind()){
            case PLUS, MINUS, TIMES, DIV, MOD -> {return "ImageOps.OP." + op.kind().toString();}
            case EQ -> {return "ImageOps.BoolOP.EQUALS";}
            default -> throw new CodeGenException("Cannot generate string for this op in runtime.ImageOps");
        }
    }
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        StringBuilder strBinary = new StringBuilder();
        String strLeft = (String)binaryExpr.getLeftExpr().visit(this, arg);
        String strRight = (String)binaryExpr.getRightExpr().visit(this, arg);
        Type leftType = binaryExpr.getLeftExpr().getType();
        Type rightType = binaryExpr.getRightExpr().getType();
        //new statements added for expr types
        if(leftType == Type.IMAGE && rightType == Type.IMAGE){
            String strOp = getImageOpsOp(binaryExpr.getOp());
            importAdd("ImageOps");
            strBinary.append("ImageOps.binaryImageImageOp(").append(strOp)
                    .append(", ").append(strLeft).append(", ").append(strRight);
        }
        else if(leftType == Type.IMAGE && rightType == Type.PIXEL){
            String strOp = getImageOpsOp(binaryExpr.getOp());
            importAdd("ImageOps");
            strBinary.append("ImageOps.binaryImagePixelOp(").append(strOp).append(", ")
                    .append(strLeft).append(", ").append(strRight).append(')');
        }
        else if(rightType == Type.IMAGE && leftType == Type.PIXEL){
            String strOp = getImageOpsOp(binaryExpr.getOp());
            importAdd("ImageOps");
            strBinary.append("ImageOps.binaryImagePixelOp(").append(strOp).append(", ")
                    .append(strLeft).append(", ").append(strRight).append(')');
        }
        else if(leftType == Type.IMAGE && rightType == Type.INT){
            String strOp = getImageOpsOp(binaryExpr.getOp());
            importAdd("ImageOps");
            strBinary.append("ImageOps.binaryImageScalarOp(").append(strOp).append(", ")
                    .append(strLeft).append(", ").append(strRight).append(')');
        }
        else if(rightType == Type.IMAGE && leftType == Type.INT) {
            String strOp = getImageOpsOp(binaryExpr.getOp());
            importAdd("ImageOps");
            strBinary.append("ImageOps.binaryImageScalarOp(").append(strOp).append(", ")
                    .append(strRight).append(", ").append(strLeft).append(')');
        }
        else if(leftType == Type.PIXEL && rightType == Type.PIXEL){
            String strOp = getImageOpsOp(binaryExpr.getOp());
            importAdd("ImageOps");
            strBinary.append("ImageOps.binaryPackedPixelPixelOp(").append(strOp).append(", ")
                    .append(strLeft).append(", ").append(strRight).append(')');
        }
        else if(leftType == Type.PIXEL && rightType == Type.INT){
            String strOp = getImageOpsOp(binaryExpr.getOp());
            importAdd("ImageOps");
            strBinary.append("ImageOps.binaryPackedPixelScalarOp(").append(strOp).append(", ")
                    .append(strLeft).append(", ").append(strRight).append(')');
        }
        else if(rightType == Type.PIXEL && leftType == Type.INT){
            String strOp = getImageOpsOp(binaryExpr.getOp());
            importAdd("ImageOps");
            strBinary.append("ImageOps.binaryPackedPixelScalarOp(").append(strOp).append(", ")
                    .append(strRight).append(", ").append(strLeft).append(')');
        }
        else if(leftType == Type.PIXEL && rightType == Type.BOOLEAN){
            String strOp = getImageOpsOp(binaryExpr.getOp());
            importAdd("ImageOps");
            strBinary.append("ImageOps.binaryPackedPixelBooleanOp(").append(strOp).append(", ")
                    .append(strLeft).append(", ").append(strRight).append(')');
        }
        else if(rightType == Type.PIXEL && leftType == Type.BOOLEAN){
            String strOp = getImageOpsOp(binaryExpr.getOp());
            importAdd("ImageOps");
            strBinary.append("ImageOps.binaryPackedPixelBooleanOp(").append(strOp).append(", ")
                    .append(strRight).append(", ").append(strLeft).append(')');
        }
        else if(binaryExpr.getOpKind() == Kind.EQ && binaryExpr.getLeftExpr().getType() == Type.STRING ){
            strBinary.append(strLeft).append(".equals(").append(strRight).append(")");
        }
        else if(binaryExpr.getOpKind() == Kind.EXP){
            strBinary.append("((int)Math.round(Math.pow(").append(strLeft).append(",").append(strRight).append(")))");
        }
        else {
            strBinary.append("(").append(strLeft).append(binaryExpr.getOp().text()).append(strRight).append(")");
        }

        return strBinary.toString();
    }
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        StringBuilder strUnary = new StringBuilder();
        strUnary.append("(");
        if(unaryExpr.getOp() == Kind.RES_width){
            strUnary.append((String)unaryExpr.getExpr().visit(this, arg));
            strUnary.append(".getWidth()");
        }
        else if(unaryExpr.getOp() == Kind.RES_height){
            strUnary.append((String)unaryExpr.getExpr().visit(this, arg));
            strUnary.append(".getHeight()");
        }
        else{
        strUnary.append(getOpString(unaryExpr.getOp()));
        strUnary.append((String)unaryExpr.getExpr().visit(this, arg));
        }
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
        LValue lValue = assignmentStatement.getlValue();
        boolean pixSelect = lValue.getPixelSelector() != null;
        boolean channelSelect = lValue.getChannelSelector() != null;
        if(lValue.getVarType() == Type.IMAGE){
            if(channelSelect){
                throw new UnsupportedOperationException("Channel Select in assignment statement not currently supported");
            }
            if(!pixSelect && !channelSelect){
                Expr e = assignmentStatement.getE();
                String strLVal = (String)lValue.visit(this, arg);
                String strExpr = (String)e.visit(this, arg);
                importAdd("ImageOps");
                if(e.getType() == Type.IMAGE){
                    strAssign.append("ImageOps.copyInto(").append(strExpr)
                            .append(", ").append(strLVal).append(')');
                }
                else if(e.getType() == Type.PIXEL){
                    strAssign.append(" ImageOps.setAllPixels(").append(strLVal).append(", ")
                            .append(strExpr).append(')');
                }
                else if(e.getType() == Type.STRING){
                    importAdd("FileURLIO");
                    //load, resize, copy into
                    strAssign.append("BufferedImage image = FileURLIO.readImage(").append(strExpr).append(");");
                    strAssign.append("ImageOps.copyInto(").append(strExpr).append(", ").append(strLVal).append(");");
                }
            }
            else if(pixSelect && !channelSelect){
                //check if selectors are synthetic name def
                //If the variable scope is smaller than the image scope, they're not synthetic

            }
        }
        else if(lValue.getVarType() == Type.PIXEL &&
                channelSelect){
            addImport("import edu.ufl.cise.cop4020fa23.runtime.PixelOps;");
            String color = (String)lValue.getChannelSelector().visit(this, false);
            strAssign.append("PixelOps.setRed(");
            strAssign.append((String)assignmentStatement.getlValue().visit(this, arg));
        }
        else {
            strAssign.append((String)assignmentStatement.getlValue().visit(this, arg));
            strAssign.append( " = ");
            strAssign.append((String)assignmentStatement.getE().visit(this, arg));
        }
        return strAssign.toString();
    }
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        //import string handling
        addImport("import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;");

        StringBuilder strWrite = new StringBuilder();
        if(writeStatement.getExpr().getType() == Type.PIXEL)
            strWrite.append("ConsoleIO.writePixel(");
        else
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
        StringBuilder strConstExpr = new StringBuilder();
        if(constExpr.getName().equals("Z"))
            return "255";
        imports.add("import java.awt.Color.*;");
        Color color = switch(constExpr.getName()){  //BLACK | BLUE | CYAN | DARK_GRAY | GRAY | GREEN | LIGHT_GRAY | MAGENTA | ORANGE | PINK | RED | WHITE | YELLOW
            case "BLACK" -> Color.BLACK;
            case "BLUE" -> Color.BLUE;
            case "CYAN" -> Color.CYAN;
            case "DARK_GRAY" -> Color.DARK_GRAY;
            case "GRAY" -> Color.GRAY;
            case "GREEN" -> Color.GREEN;
            case "LIGHT_GRAY" -> Color.LIGHT_GRAY;
            case "MAGENTA" -> Color.MAGENTA;
            case "ORANGE" -> Color.ORANGE;
            case "PINK" -> Color.PINK;
            case "RED" -> Color.RED;
            case "WHITE" -> Color.WHITE;
            case "YELLOW" -> Color.YELLOW;
            default -> throw new CodeGenException("Color Const is not an enumrated color in java.awt.color");
        };
        strConstExpr.append("0x");
        String hexString = Integer.toHexString(color.getRGB());
        strConstExpr.append(hexString);
        return strConstExpr.toString();
    }
    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        StringBuilder strPostfix = new StringBuilder();
        if(postfixExpr.getType() == Type.PIXEL){
            strPostfix.append((String)postfixExpr.channel().visit(this, arg));
            strPostfix.append("(");
            strPostfix.append((String)postfixExpr.primary().visit(this, arg));
            strPostfix.append(")");
        }
        else{   //postFixExpr is of type image
            boolean channelSelect = postfixExpr.channel() != null;
            boolean pixSelect = postfixExpr.pixel() != null;
            addImport("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n");
            if(pixSelect){
                if(channelSelect){
                    String channel = (String)postfixExpr.channel().visit(this, true);   //true indicates context of postFixExpr
                    strPostfix.append(channel);
                    strPostfix.append('(');
                }
                strPostfix.append("ImageOps.getRGB(");
                strPostfix.append((String)postfixExpr.primary().visit(this, arg));
                strPostfix.append(", ");
                strPostfix.append((String)postfixExpr.pixel().visit(this, arg));
                strPostfix.append(')');
                if(channelSelect)
                    strPostfix.append(')');
            }
            else if(!pixSelect && channelSelect){
                String channel = "";
                if(postfixExpr.channel().color() == Kind.RES_blue)
                    channel = "Blue";
                else if(postfixExpr.channel().color() == Kind.RES_green)
                    channel = "Grn";
                else
                    channel = "Red";
                strPostfix.append("ImageOps.extract").append(channel).append('(');
                strPostfix.append((String)postfixExpr.primary().visit(this, arg)).append(')');
            }
        }
        return strPostfix.toString();
    }
    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        //boolean accepted as arg; true => in context of postFixExpr; false => context of lValue
        boolean inPostFix = (boolean)arg;
        if(inPostFix){
            addImport("import edu.ufl.cise.cop4020fa23.runtime.PixelOps;");
            if(channelSelector.color() == Kind.RES_red)
                return "PixelOps.red";
            else if(channelSelector.color() == Kind.RES_green)
                return "PixelOps.green";
            else if(channelSelector.color() == Kind.RES_blue)
                return "PixelOps.blue";
            else
                throw new CodeGenException("Channel Selector does not have a valid color");
        }
        else{   //in context of LValue
            if(channelSelector.color() == Kind.RES_red)
                return "PixelOps.setRed";
            else if(channelSelector.color() == Kind.RES_green)
                return "PixelOps.setGreen";
            else if(channelSelector.color() == Kind.RES_blue)
                return "PixelOps.setBlue";
            else
                throw new CodeGenException("Channel Selector does not have a valid color");
        }
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        StringBuilder strPixSelect = new StringBuilder();
        strPixSelect.append((String)pixelSelector.xExpr().visit(this, arg));
        strPixSelect.append(", ");
        strPixSelect.append((String)pixelSelector.yExpr().visit(this, arg));
        return strPixSelect.toString();
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        StringBuilder strExpandedPixel = new StringBuilder();
        addImport("import edu.ufl.cise.cop4020fa23.runtime.PixelOps;\n");
        strExpandedPixel.append("PixelOps.pack(");
        strExpandedPixel.append((String)expandedPixelExpr.getRed().visit(this, arg));
        strExpandedPixel.append(", ");
        strExpandedPixel.append((String)expandedPixelExpr.getGreen().visit(this, arg));
        strExpandedPixel.append(", ");
        strExpandedPixel.append((String)expandedPixelExpr.getBlue().visit(this, arg));
        strExpandedPixel.append(')');
        return strExpandedPixel.toString();
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        StringBuilder strDimension = new StringBuilder();
        strDimension.append((String)dimension.getWidth().visit(this, arg));
        strDimension.append(", ");
        strDimension.append((String)dimension.getHeight().visit(this, arg));
        return strDimension.toString();
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        boolean doStatement = (boolean)arg; //false indicates if statement
        StringBuilder strGuardedBlock = new StringBuilder();
        if(doStatement){
            strGuardedBlock.append("if(").append((String)guardedBlock.getGuard().visit(this, arg)).append("){\n\t");
            strGuardedBlock.append("do = true;\n\t");
        }
        else{
            //read as else if; first guarded block generated outside this function
            strGuardedBlock.append("else if(").append((String)guardedBlock.getGuard().visit(this, arg)).append("){\n\t");
        }
        strGuardedBlock.append((String)guardedBlock.getBlock().visit(this, arg)).append("\n}\n");
        return strGuardedBlock.toString();
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        /**
         * boolean do = true;
         * while(do){
         *      do = false;
         *          if(g[i]){
         *              do = true;
         *              b[i];
         *          }
         *          if(g[i+1]){
         *              do = true;
         *              b[i+1];
         *          }
         *          if(...){
         *              ...
         *          }
         *      }
         * }
         */
        List<GuardedBlock> guardedBlocks = doStatement.getGuardedBlocks();
        StringBuilder strDoStatement = new StringBuilder();
        strDoStatement.append("boolean do = true;\nwhile(do){\n\tdo = false;\n\t");
        for(GuardedBlock guardedBlock: guardedBlocks){
            strDoStatement.append((String)guardedBlock.visit(this, true));
        }
        return strDoStatement.toString();
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
            //if G1 -> {B1;} else if G2 -> {B2;} ...
            StringBuilder strIfStatement = new StringBuilder();
            GuardedBlock firstBlock = null;
            try{firstBlock = ifStatement.getGuardedBlocks().get(0);}
            catch (Error e){
                throw new CodeGenException("Calling visit on empty if statement");
            }
            strIfStatement.append("if(\n").append((String)firstBlock.getGuard().visit(this, arg)).append("){\n\t");
            strIfStatement.append((String)firstBlock.getBlock().visit(this, arg));
            strIfStatement.append("}");
            for(GuardedBlock guardedBlock: ifStatement.getGuardedBlocks()){
                strIfStatement.append((String)guardedBlock.visit(this, false)); //false indicates within if statement
            }
            return strIfStatement.toString();
    }
    /*======== HELPER METHODS =========*/
    public void addImport(String toImport){
        if(!imports.contains(toImport))
            imports.add(toImport);
    }
    public void importAdd(String toImport){
        StringBuilder strImport = new StringBuilder();
        strImport.append("import ");
        String importHeading = "edu.ufl.cise.cop4020fa23.runtime.";
        switch(toImport){
            case "ImageOps", "PixelOps", "FileURLIO", "ConsoleIO" -> {
                strImport.append(importHeading).append(toImport).append(';');
            }
            default -> strImport.append(toImport).append(';');
        }
        if(!imports.contains(strImport.toString()))
            imports.add(strImport.toString());
    }
}