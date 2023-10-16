/*Copyright 2023 by Beverly A Sanders
 * 
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the 
 * University of Florida during the fall semester 2023 as part of the course project.  
 * 
 * No other use is authorized. 
 * 
 * This code may not be posted on a public web site either during or after the course.  
 */
package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;

import java.util.ArrayList;
import java.util.List;

public class ExpressionParser implements IParser {
	
	final ILexer lexer;
	private IToken currToken;

	private final List<IToken> tokenList = new ArrayList<IToken>();
	private int iterator;

	/**
	 * @param lexer
	 * @throws LexicalException
	 */
	public ExpressionParser(ILexer lexer) throws LexicalException {
		super();
		this.lexer = lexer;
		currToken = lexer.next();
		while(!isAtEnd()){
			tokenList.add(currToken);
			currToken = lexer.next();
		}
		tokenList.add(currToken);		//add EOF token
		currToken = tokenList.get(0);
		iterator = 0;
	}
	/**
	 * Helper Methods and Variables
	 */
	public IToken consume() throws LexicalException{
		if(!isAtEnd()){
			currToken = tokenList.get(++iterator);
		}
		return currToken;
	}
	public IToken previous() throws LexicalException{
		if(iterator != 0){
			return tokenList.get(iterator - 1);
		}
		else{
			throw new LexicalException(currToken.sourceLocation(), "Attempting to access token at index -1");
		}
	}
	public boolean check(Kind... kinds) throws LexicalException{
		if(isAtEnd())
			return false;
		for(Kind kind: kinds){
			if(currToken.kind() == kind){
				return true;
			}
		}
		return false;
	}
	public boolean match(Kind... kinds) throws LexicalException{
		for(Kind kind: kinds){
			if(currToken.kind() == kind){
				consume();
				return true;
			}
		}
		return false;
	}
	public boolean isAtEnd() throws LexicalException{
		return currToken.kind() == Kind.EOF;
	}
	@Override
	public AST parse() throws SyntaxException, LexicalException {
		return expr();
	}

	//Expr::=  ConditionalExpr | LogicalOrExpr
	private Expr expr() throws SyntaxException, LexicalException {
		if(check(Kind.QUESTION)){
			return conditionalExpr();
		}
		else if(check(Kind.BANG, Kind.MINUS, Kind.RES_width, Kind.RES_height, Kind.STRING_LIT,
				      Kind.NUM_LIT, Kind.BOOLEAN_LIT, Kind.IDENT, Kind.LPAREN, Kind.CONST, Kind.LSQUARE)){
			return logicalOrExpr();
		}
		else{
			throw new SyntaxException(currToken.sourceLocation(), "No valid tokens detected");
		}
	}
	//ConditionalExpr ::= ? Expr -> Expr , Expr
	private Expr conditionalExpr() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		consume();	//already verified first token is '?' in expr()
		Expr guard, trueExpr, falseExpr;
		guard = expr();
		if(!match(Kind.RARROW))
			throw new SyntaxException(currToken.sourceLocation(), "Right arrow expected");
		trueExpr = expr();
		if(!match(Kind.COMMA))
			throw new SyntaxException(currToken.sourceLocation(), "Comma expected");
		falseExpr = expr();
		return new ConditionalExpr(firstToken, guard, trueExpr, falseExpr);
	}
	//LogicalOrExpr ::= LogicalAndExpr ( ( | | || ) LogicalAndExpr)*
	private Expr logicalOrExpr() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		Expr logicalOrExpr = logicalAndExpr();
		Expr rightExpr;
		IToken op;
		while(check(Kind.OR, Kind.BITOR)){
			op = currToken;
			consume();
			rightExpr = logicalAndExpr();
			logicalOrExpr = new BinaryExpr(firstToken, logicalOrExpr, op, rightExpr);
		}
		return logicalOrExpr;
	}
	//LogicalAndExpr ::=  ComparisonExpr ( (   &   |  &&   )  ComparisonExpr)*
	private Expr logicalAndExpr() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		Expr logicalAndExpr = comparisonExpr();
		Expr rightExpr;
		IToken op;
		while(check(Kind.AND, Kind.BITAND)){
			op = currToken;
			consume();
			rightExpr = logicalAndExpr();
			logicalAndExpr = new BinaryExpr(firstToken, logicalAndExpr, op, rightExpr);
		}
		return logicalAndExpr;
	}
	//ComparisonExpr ::= PowExpr ( (< | > | == | <= | >=) PowExpr)*
	private Expr comparisonExpr() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		Expr comparisonExpr = powExpr();
		Expr rightExpr;
		IToken op;
		while(check(Kind.LT, Kind.GT, Kind.EQ, Kind.LE, Kind.GE)){
			op = currToken;
			consume();
			rightExpr = powExpr();
			comparisonExpr = new BinaryExpr(firstToken, comparisonExpr, op, rightExpr);
		}
		return comparisonExpr;
	}
	//PowExpr ::= AdditiveExpr ** PowExpr |   AdditiveExpr
	private Expr powExpr() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		Expr powExpr = additiveExpr();
		Expr rightExpr;
		IToken op;
		if(check(Kind.EXP)){
			op = currToken;
			consume();
			rightExpr = powExpr();
			powExpr = new BinaryExpr(firstToken, powExpr, op, rightExpr);
		}
		return powExpr;
	}
	//AdditiveExpr ::= MultiplicativeExpr ( ( + | -  ) MultiplicativeExpr )*
	private Expr additiveExpr() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		Expr additiveExpr = multiplicativeExpr();
		Expr rightExpr;
		IToken op;
		while(check(Kind.PLUS, Kind.MINUS)){
			op = currToken;
			consume();
			rightExpr = multiplicativeExpr();
			additiveExpr = new BinaryExpr(firstToken, additiveExpr, op, rightExpr);
		}
		return additiveExpr;
	}
	private	Expr multiplicativeExpr() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		Expr multiplicativeExpr = unaryExpr();
		Expr rightExpr;
		IToken op;
		while(match(Kind.TIMES, Kind.DIV, Kind.MOD)){
			op = previous();
			rightExpr = unaryExpr();
			multiplicativeExpr = new BinaryExpr(firstToken, multiplicativeExpr, op, rightExpr);
		}
		return multiplicativeExpr;
	}
	//UnaryExpr ::=  ( ! | - | length | width) UnaryExpr  |  PostfixExpr
	// ::= (! | - | length | width)* PostfixExpr
	private Expr unaryExpr() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		Expr e;
		if(match(Kind.BANG, Kind.MINUS, Kind.RES_width, Kind.RES_height)){
			e = unaryExpr();
			return new UnaryExpr(firstToken, firstToken, e);
		}
		return postfixExpr();
	}
	//UnaryExprPostfix::= PrimaryExpr (PixelSelector | ε ) (ChannelSelector | ε )
	private Expr postfixExpr() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		Expr primaryExpr = primaryExpr();
		boolean isPostFix = false;
		PixelSelector pixelSelector = null;
		ChannelSelector channelSelector = null;
		if(check(Kind.LSQUARE)){
			pixelSelector = pixelSelector();
			isPostFix = true;
		}
		if(check(Kind.COLON)){
			channelSelector = channelSelector();
			isPostFix = true;
		}
		if(isPostFix){
			return new PostfixExpr(firstToken, primaryExpr, pixelSelector, channelSelector);
		}
		return primaryExpr;
	}
	//PrimaryExpr ::=STRING_LIT | NUM_LIT | IDENT | ( Expr ) | CONST | ExpandedPixelExpr
	private Expr primaryExpr() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		Expr primaryExpr;
		if(match(Kind.STRING_LIT)){
			primaryExpr = new StringLitExpr(firstToken);
		}
		else if(match(Kind.NUM_LIT)){
			primaryExpr = new NumLitExpr(firstToken);
		}
		else if(match(Kind.BOOLEAN_LIT)){
			primaryExpr = new BooleanLitExpr(firstToken);
		}
		else if(match(Kind.IDENT)){
			primaryExpr = new IdentExpr(firstToken);
		}
		else if(match(Kind.CONST)){
			primaryExpr = new ConstExpr(firstToken);
		}
		else if(check(Kind.LSQUARE)){
			primaryExpr = expandedPixelExpr();
		}
		else if(match(Kind.LPAREN)){
			primaryExpr = expr();
			if(!match(Kind.RPAREN)){
				throw new SyntaxException(currToken.sourceLocation(), "Right parentheses expected");
			}
		}
		else{
			throw new SyntaxException(currToken.sourceLocation(), "No appropriate token found for primaryExpr");
		}
		return primaryExpr;
	}
	//PixelSelector  ::= [ Expr , Expr ]
	private PixelSelector pixelSelector() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		consume();
		Expr xExpr = expr();
		if(!match(Kind.COMMA))
			throw new SyntaxException(currToken.sourceLocation(), "Comma expected");
		Expr yExpr = expr();
		if(!match(Kind.RSQUARE))
			throw new SyntaxException(currToken.sourceLocation(), "Right Square Bracket expected");
		return new PixelSelector(firstToken, xExpr, yExpr);
	}
	//ChannelSelector ::= : red | : green | : blue
	private ChannelSelector channelSelector() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		consume();

		IToken color;
		if(match(Kind.RES_red, Kind.RES_green, Kind.RES_blue)){
			color = previous();
		}
		else
			throw new SyntaxException(currToken.sourceLocation(), "No valid color option found for channel selector");
		return new ChannelSelector(firstToken, color);
	}
	//ExpandedPixel ::= [ Expr , Expr , Expr ]
	private Expr expandedPixelExpr() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		consume();
		Expr red = expr();
		if(!match(Kind.COMMA))
			throw new SyntaxException(currToken.sourceLocation(), "Comma expected");
		Expr green = expr();
		if(!match(Kind.COMMA))
			throw new SyntaxException(currToken.sourceLocation(), "Comma expected");
		Expr blue = expr();
		if(!match(Kind.RSQUARE))
			throw new SyntaxException(currToken.sourceLocation(), "Right Square Bracket expected");
		return new ExpandedPixelExpr(firstToken, red, green, blue);
	}

	/*################## ASSIGNMENT 2 GRAMMAR #######################*/
	/*========Predict Sets========*/
	Kind[] predictType = {Kind.RES_image, Kind.RES_pixel, Kind.RES_int, Kind.RES_string, Kind.RES_void, Kind.RES_boolean};
	Kind[] predictStatement = {Kind.IDENT, Kind.RES_write, Kind.RES_do, Kind.RES_if, Kind.RETURN, Kind.BLOCK_OPEN};

	//matchSingle is intended for checking for a single token
	public boolean matchSingle(Kind kind) throws SyntaxException, LexicalException{
		if(!match(kind)){
			throw new SyntaxException(currToken.sourceLocation(), kind + " expected");
		}
		return true;
	}
	//Program::= Type IDENT ( ParamList ) Block
	private Program program() throws SyntaxException, LexicalException{
		IToken firstToken = currToken;
		IToken type = type();
		IToken name;
		if(check(Kind.IDENT)){
			name = currToken;
			consume();
		}
		else{
			throw new SyntaxException(currToken.sourceLocation(), "Ident expected");
		}

		matchSingle(Kind.LPAREN);
		List<NameDef> paramList = paramList();
		matchSingle(Kind.RPAREN);
		Block block = block();
		return new Program(firstToken, type, name, paramList, block);
	}
	//Block ::= <: (Declaration ; | Statement ;)* :>
	private Block block() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		List<Block.BlockElem> blockElems = new ArrayList<>();
		matchSingle(Kind.BLOCK_OPEN);
		while(check(predictStatement) || check(predictType)){
			if(check(predictType)){		//predict set of declaration = predictType
				Declaration declaration = declaration();
				blockElems.add(declaration);
			}
			else if(check(predictStatement)){
				Statement statement = statement();
				blockElems.add(statement);
			}
		}
		matchSingle(Kind.BLOCK_CLOSE);
		return new Block(firstToken, blockElems);
	}
	//Type := image | pixel | int | string | void | boolean
	private IToken type() throws SyntaxException, LexicalException {
		IToken type;
		if(!check(predictType)){
			type = currToken;
			consume();
			return type;
		}
		throw new SyntaxException(currToken.sourceLocation(), "Type expected");
	}
	//NameDef ::= Type IDENT | Type Dimension IDENT
	private NameDef nameDef() throws LexicalException, SyntaxException{
		IToken firstToken = currToken;
		IToken type = type();
		IToken name;
		Dimension dimension = null;
		if(check(Kind.LSQUARE)) {
			dimension = dimension();
		}

		if(check(Kind.IDENT)){
			name = currToken;
			consume();
		}
		else{
			throw new SyntaxException(currToken.sourceLocation(), "Ident expected");
		}

		return new NameDef(firstToken, type, dimension, name);
	}
	//ParamList ::= ε | NameDef ( , NameDef ) *
	private List<NameDef> paramList() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		List<NameDef> paramList = new ArrayList<>();
		if(check(predictType)){
			NameDef nameDef = nameDef();
			paramList.add(nameDef);
			while(match(Kind.COMMA)){
				paramList.add(nameDef());
			}
		}
		return paramList;
	}
	//Dimension ::= [Expr, Expr]
	private Dimension dimension() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		matchSingle(Kind.LSQUARE);
		Expr width = expr();
		matchSingle(Kind.COMMA);
		Expr height = expr();
		matchSingle(Kind.RSQUARE);
		return new Dimension(firstToken, width, height);
	}
	//Declaration::= NameDef | NameDef = Expr
	private Declaration declaration() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		NameDef nameDef = nameDef();
		Expr initializer = null;
		if(match(Kind.ASSIGN)){
			initializer = expr();
		}
		return new Declaration(firstToken, nameDef, initializer);
	}
	// Statement::=
	private Statement statement() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		if(check(Kind.IDENT)){ //LValue = Expr |
			LValue lvalue = lvalue();
			matchSingle(Kind.ASSIGN);
			Expr expr = expr();
			return new AssignmentStatement(firstToken, lvalue, expr);
		}
		else if(match(Kind.RES_write)){ //write Expr
			consume();
			Expr expr = expr();
			return new WriteStatement(firstToken, expr);
		}
		else if(match(Kind.RES_do)) { //do GuardedBlock [] GuardedBlock* od
			List<GuardedBlock> guardedBlockList = new ArrayList<>();
			GuardedBlock guardedBlock1 = guardedBlock();
			guardedBlockList.add(guardedBlock1);
			while(matchSingle(Kind.LSQUARE) && matchSingle(Kind.RSQUARE)){
				GuardedBlock guardedBlock = guardedBlock();
				guardedBlockList.add(guardedBlock);
			}
			matchSingle(Kind.RES_od);
			return new DoStatement(firstToken, guardedBlockList);
		}
		else if(match(Kind.RES_if)){ //if GuardedBlock [] GuardedBlock* fi
			List<GuardedBlock> guardedBlockList = new ArrayList<>();
			GuardedBlock guardedBlock1 = guardedBlock();
			guardedBlockList.add(guardedBlock1);
			while(matchSingle(Kind.LSQUARE) && matchSingle(Kind.RSQUARE)){
				GuardedBlock guardedBlock = guardedBlock();
				guardedBlockList.add(guardedBlock);
			}
			matchSingle(Kind.RES_fi);
			return new IfStatement(firstToken, guardedBlockList);
		}
		else if(match(Kind.RETURN)){ //^ Expr
			Expr expr = expr();
			return new ReturnStatement(firstToken, expr);
		}
		else if(check(Kind.BLOCK_OPEN)) { //BlockStatement
			Block block = block();
			return new StatementBlock(firstToken, block);
		}
		throw new SyntaxException(currToken.sourceLocation(), "No valid token detected for Statement");
	}
	//LValue ::= IDENT (PixelSelectorIn | ε ) (ChannelSelector | ε )
	private LValue lvalue() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		consume();
		PixelSelector pixelSelector = null;
		ChannelSelector channelSelector = null;
		if(check(Kind.LSQUARE))
			pixelSelector = pixelSelector();
		if(check(Kind.COLON))
			channelSelector = channelSelector();
		return new LValue(firstToken, firstToken, pixelSelector, channelSelector);
	}
	//GuardedBlock := Expr -> Block
	private GuardedBlock guardedBlock() throws SyntaxException, LexicalException {
		IToken firstToken = currToken;
		Expr guard = expr();
		matchSingle(Kind.RARROW);
		Block block = block();
		return new GuardedBlock(firstToken, guard, block);
	}

}
