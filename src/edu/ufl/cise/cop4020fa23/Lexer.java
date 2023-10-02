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

import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;

import java.util.HashMap;
import java.util.Vector;


public class Lexer implements ILexer {

	/*========== MAIN VARIABLES ==========*/
	String input;
	char[] charInput;	//char array of input
	//util variables
	char ch;
	Kind kind;
	int pos;	//tracks current pos in the array
	int tokenPos;	//tracks starting position of a token;
	int line;
	int column;
	SourceLocation location;
	HashMap<String, Kind> reservedWords = new HashMap<String, Kind>();
	Vector<String> constants = new Vector<>();

	public Lexer(String input) {
		this.input = input;
		this.charInput = input.toCharArray();
		this.pos = 0;
		this.line = 1;
		this.column = 1;

		//add reserved words to hash map
		reservedWords.put("image", Kind.RES_image);
		reservedWords.put("pixel", Kind.RES_pixel);
		reservedWords.put("int", Kind.RES_int);
		reservedWords.put("string", Kind.RES_string);
		reservedWords.put("void", Kind.RES_void);
		reservedWords.put("boolean", Kind.RES_boolean);
		reservedWords.put("write", Kind.RES_write);
		reservedWords.put("height", Kind.RES_height);
		reservedWords.put("width", Kind.RES_width);
		reservedWords.put("if", Kind.RES_if);
		reservedWords.put("fi", Kind.RES_fi);
		reservedWords.put("do", Kind.RES_do);
		reservedWords.put("od", Kind.RES_od);
		reservedWords.put("red", Kind.RES_red);
		reservedWords.put("green", Kind.RES_green);
		reservedWords.put("blue", Kind.RES_blue);

		constants.add("Z");
		constants.add("BLACK");
		constants.add("BLUE");
		constants.add("CYAN");
		constants.add("GRAY");
		constants.add("DARK_GRAY");
		constants.add("GREEN");
		constants.add("LIGHT_GRAY");
		constants.add("MAGENTA");
		constants.add("ORANGE");
		constants.add("PINK");
		constants.add("RED");
		constants.add("WHITE");
		constants.add("YELLOW");
	}
	private enum State{
		START,
		IN_IDENT,
		IN_NUM_LIT,
		IN_STRING_LIT,
		HAS_AMP,
		HAS_COLON,
		HAS_ASTERISK,
		HAS_EQ,
		HAS_LINE,
		HAS_LT,
		HAS_GT,
		HAS_BRACKET,
		HAS_MINUS,
		COMMENT,
		HAS_POUND
	}

	@Override
	public IToken next() throws LexicalException {
		return scanToken();
	}

	//advances pos, special case if newline occurs
	private void nextChar(){
		column++;
		pos++;
	}
	public IToken scanToken() throws LexicalException{
		State state = State.START;
		IToken token;
		while(true){
			try{
				ch = charInput[pos];
			}
			catch(ArrayIndexOutOfBoundsException e){
				ch = 0;
			}
			switch(state){
				case START -> {
					tokenPos = pos;	//sets first char of new token;
					location = new SourceLocation(line, column);
					//large range of potential chars (e.g. a-z) --> if statements
					//small amount of chars --> switch case
					if(isLetter(ch) || ch == '_'){
						state = State.IN_IDENT;
						nextChar();
					}
					else if(isNonZeroDigit(ch)){
						state = State.IN_NUM_LIT;
						nextChar();
					}
					switch(ch){
						case '"' -> {
							state = State.IN_STRING_LIT;
							nextChar();
						}
						case '<' -> {
							state = State.HAS_LT;
							nextChar();
						}
						case '>' -> {
							state = State.HAS_GT;
							nextChar();
						}
						case '=' -> {
							state = State.HAS_EQ;
							nextChar();
						}
						case ':' -> {
							state = State.HAS_COLON;
							nextChar();
						}
						case '&' -> {
							state = State.HAS_AMP;
							nextChar();
						}
						case '|' -> {
							state = State.HAS_LINE;
							nextChar();
						}
						case '*' -> {
							state = State.HAS_ASTERISK;
							nextChar();
						}
						case '[' -> {
							state = State.HAS_BRACKET;
							nextChar();
						}
						case '-' -> {
							state = State.HAS_MINUS;
							nextChar();
						}
						case '#' -> {
							state = State.HAS_POUND;
							nextChar();
						}
						//single char kinds
						case ',' -> {
							nextChar();
							token = new Token(Kind.COMMA, tokenPos, 1, charInput, location);
							return token;
						}
						case ';' -> {
							nextChar();
							token = new Token(Kind.SEMI, tokenPos, 1, charInput, location);
							return token;
						}
						case '?' -> {
							nextChar();
							token = new Token(Kind.QUESTION, tokenPos, 1, charInput, location);
							return token;
						}
						case '!' -> {
							nextChar();
							token = new Token(Kind.BANG, tokenPos, 1, charInput, location);
							return token;
						}
						case '+' -> {
							nextChar();
							token = new Token(Kind.PLUS, tokenPos, 1, charInput, location);
							return token;
						}
						case '/' -> {
							nextChar();
							token = new Token(Kind.DIV, tokenPos, 1, charInput, location);
							return token;
						}
						case '%' -> {
							nextChar();
							token = new Token(Kind.MOD, tokenPos, 1, charInput, location);
							return token;
						}
						case '^' -> {
							nextChar();
							token = new Token(Kind.RETURN, tokenPos, 1, charInput, location);
							return token;
						}
						case ']' -> {
							nextChar();
							token = new Token(Kind.RSQUARE, tokenPos, 1, charInput, location);
							return token;
						}
						case '0' -> {
							nextChar();
							token = new Token(Kind.NUM_LIT, tokenPos, 1, charInput, location);
							return token;
						}
						case '(' -> {
							nextChar();
							token = new Token(Kind.LPAREN, tokenPos, 1, charInput, location);
							return token;
						}
						case ')' -> {
							nextChar();
							token = new Token(Kind.RPAREN, tokenPos, 1, charInput, location);
							return token;
						}
						case '\r', ' ' -> {		//whitespace
							nextChar();
						}
						case '\n' -> {
							line++;
							column = 1;
							pos++;
						}
						case 0 -> {
							token = new Token(Kind.EOF, tokenPos, 0, charInput, location);
							return token;
						}
						default -> {
							if(!(isLetter(ch) || isNonZeroDigit(ch) || ch == '_'))
								throw new LexicalException("Character not recognized in language");
						}
					}
				}
				case IN_IDENT -> {
					if(isNonZeroDigit(ch) || ch == '0' || ch =='_' || isLetter(ch)){
						nextChar();
					}
					else{	//reached end of ident/reserved word
						int length = pos - tokenPos;
						String identStr = new String(charInput, tokenPos, length);
						//determine kind of ident
						if(reservedWords.containsKey(identStr)){	//check for reserved word/const
							kind = reservedWords.get(identStr);
						}
						else if(constants.contains(identStr)){
							kind = Kind.CONST;
						}
						else if(identStr.equals("TRUE") || identStr.equals("FALSE")){
							kind = Kind.BOOLEAN_LIT;
						}
						else{
							kind = Kind.IDENT;
						}
						token = new Token(kind, tokenPos, length, charInput, location);
						return token;
					}
				}
				case IN_STRING_LIT -> {
					if(isPrintableChar(ch) && ch != '"'){	//printable char
						nextChar();
					}
					else if(ch == '"'){
						nextChar();
						int length = pos - tokenPos;
						token = new Token(Kind.STRING_LIT, tokenPos, length, charInput, location);
						state = State.START;
						return token;
					}
					else{
						throw new LexicalException("STRING_LIT: Printable char or double quote expected");
					}
				}
				case IN_NUM_LIT -> {
					if(isNonZeroDigit(ch) || ch == '0'){
						nextChar();
					}
					else{
						int length = pos - tokenPos;
						String numLit = new String(charInput, tokenPos, length);
						long numLitLong = 0;
						try{
							numLitLong = Long.parseLong(numLit);
						}
						catch(NumberFormatException e){
							throw new LexicalException("NUM_LIT: Cannot convert to an integer");
						}
						if(numLitLong > 2147483647){
							throw new LexicalException("Numeric Literal exceeded max value");
						}
						token = new Token(Kind.NUM_LIT, tokenPos, length, charInput, location);
						state = State.START;
						return token;
					}
				}
				case HAS_LT -> {
					int length = 0;
					switch(ch){
						case '=' -> {
							nextChar();
							length = 2;
							kind = Kind.LE;
						}
						case ':' -> {
							nextChar();
							length = 2;
							kind = Kind.BLOCK_OPEN;
						}
						default -> {
							length = 1;
							kind = Kind.LT;
						}
					}
					token = new Token(kind, tokenPos, length, charInput, location);
					state = State.START;
					return token;
				}
				case HAS_GT -> {
					int length;
					switch(ch){
						case '=' -> {
							nextChar();
							length = 2;
							kind = Kind.GE;
						}
						default -> {
							length = 1;
							kind = Kind.GT;
						}
					}
					token = new Token(kind, tokenPos, length, charInput, location);
					state = State.START;
					return token;
				}
				case HAS_EQ -> {
					int length;
					switch(ch){
						case('=') -> {
							nextChar();
							length = 2;
							kind = Kind.EQ;
						}
						default -> {
							length = 1;
							kind = Kind.ASSIGN;
						}
					}
					token = new Token(kind, tokenPos, length, charInput, location);
					state = State.START;
					return token;
				}
				case HAS_COLON -> {
					int length;
					if(ch == '>'){
						nextChar();
						length = 2;
						kind = Kind.BLOCK_CLOSE;
					}
					else{
						length = 1;
						kind = Kind.COLON;
					}
					token = new Token(kind, tokenPos, length, charInput, location);
					state = State.START;
					return token;
				}
				case HAS_AMP -> {
					int length;
					if(ch == '&'){
						nextChar();
						length = 2;
						kind = Kind.AND;
					}
					else{
						length = 1;
						kind = Kind.BITAND;
					}
					token = new Token(kind, tokenPos, length, charInput, location);
					state = State.START;
					return token;
				}
				case HAS_ASTERISK -> {
					int length;
					if(ch == '*'){
						nextChar();
						length = 2;
						kind = Kind.EXP;
					}
					else{
						length = 1;
						kind = Kind.TIMES;
					}
					token = new Token(kind, tokenPos, length, charInput, location);
					state = State.START;
					return token;
				}
				case HAS_LINE -> {
					int length;
					if(ch == '|'){
						nextChar();
						length = 2;
						kind = Kind.OR;
					}
					else{
						length = 1;
						kind = Kind.BITOR;
					}
					token = new Token(kind, tokenPos, length, charInput, location);
					state = State.START;
					return token;
				}
				case HAS_POUND -> {
					if(ch == '#'){
						state = State.COMMENT;
						nextChar();
					}
					else{
						throw new LexicalException("Double pound signs expected for comments.");
					}
				}
				case COMMENT -> {	//##
					if(isPrintableChar(ch))
						nextChar();
					else{
						state = State.START;	//what would cause comment to break?
					}
					if(column == 1)	//new line, reset to start state
						state = State.START;
				}
				case HAS_BRACKET -> {	//[
					int length;
					if(ch == ']'){
						nextChar();
						length = 2;
						kind = Kind.BOX;
					}
					else {
						length = 1;
						kind = Kind.LSQUARE;
					}
					token = new Token(kind, tokenPos, length, charInput, location);
					return token;
				}
				case HAS_MINUS -> {
					int length;
					if(ch == '>'){
						nextChar();
						length = 2;
						kind = Kind.RARROW;
					}
					else{
						length = 1;
						kind = Kind.MINUS;
					}
					token = new Token(kind, tokenPos, length, charInput, location);
					state = State.START;
					return token;
				}
			}
		}
	}

	//helper methods
	boolean isNonZeroDigit(char ch){
		 return (ch >= '1' && ch <= '9');
	}
	boolean isLetter(char ch){
		return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
	}
	boolean isPrintableChar(char ch){
		return (ch >= 32 && ch <= 127);
	}
}
