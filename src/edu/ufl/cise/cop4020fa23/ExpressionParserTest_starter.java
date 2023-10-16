package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpressionParserTest_starter {

	/**
	 * Constructs a scanner and parser for the given input string, scans and parses
	 * the input and returns and AST.
	 *
	 * @param input String representing program to be tested
	 * @return AST representing the program
	 * @throws PLCCompilerException
	 */
	AST getAST(String input) throws PLCCompilerException {
		return ComponentFactory.makeExpressionParser(input).parse();
	}

	/**
	 * Checks that the given AST e has type NumLitExpr with the indicated value.
	 * Returns the given AST cast to NumLitExpr.
	 *
	 * @param e
	 * @param value
	 * @return
	 */
	NumLitExpr checkNumLitExpr(AST e, int value) {
		assertThat("", e, instanceOf(NumLitExpr.class));
		NumLitExpr ne = (NumLitExpr) e;
		assertEquals(Integer.toString(value), ne.getText());
		return ne;
	}

	/**
	 * Checks that the given AST e has type StringLitExpr with the given String
	 * value. For convenience, we do not require the value to include the enclosing
	 * quotes. So we can write checkStringLitExpr(ast, "hello") rather than
	 * checkStringLitExpr(ast,""hello"");
	 *
	 * Returns the given AST cast to StringLitExpr.
	 *
	 * @param e
	 * @param nameToken
	 * @return
	 */
	StringLitExpr checkStringLitExpr(AST e, String value) {
		assertThat("", e, instanceOf(StringLitExpr.class));
		StringLitExpr se = (StringLitExpr) e;
		String s = se.getText();
		assertEquals('"', s.charAt(0)); // check that first char is "
		assertEquals('"', s.charAt(s.length() - 1));
		assertEquals(value, s.substring(1, s.length() - 1));
		return se;
	}

	/**
	 * Checks that the given AST has type BooleanLitExpr and represents the given boolean value
	 *
	 * @param e
	 * @param value
	 * @return
	 */
	BooleanLitExpr checkBooleanLitExpr(AST e, boolean value) {
		assertThat("", e, instanceOf(BooleanLitExpr.class));
		BooleanLitExpr be = (BooleanLitExpr) e;
		assertEquals(Boolean.toString(value), be.getText());
		return be;
	}

	/**
	 * Checks that the given AST e has type UnaryExpr with the given operator.
	 * Returns the given AST cast to UnaryExpr.
	 *
	 * @param e
	 * @param op Kind of expected operator
	 * @return
	 */
	private UnaryExpr checkUnaryExpr(AST e, Kind op) {
		assertThat("", e, instanceOf(UnaryExpr.class));
		assertEquals(op, ((UnaryExpr) e).getOp());
		return (UnaryExpr) e;
	}

	/**
	 * Checks that the given AST e has type ConditionalExpr. Returns the given AST
	 * cast to ConditionalExpr.
	 *
	 * @param e
	 * @return
	 */
	private ConditionalExpr checkConditionalExpr(AST e) {
		assertThat("", e, instanceOf(ConditionalExpr.class));
		return (ConditionalExpr) e;
	}

	/**
	 * Checks that the given AST e has type BinaryExpr with the given operator.
	 * Returns the given AST cast to BinaryExpr.
	 *
	 * @param e
	 * @param op Kind of expected operator
	 * @return
	 */
	BinaryExpr checkBinaryExpr(AST e, Kind expectedOp) {
		assertThat("", e, instanceOf(BinaryExpr.class));
		BinaryExpr be = (BinaryExpr) e;
		assertEquals(expectedOp, be.getOp().kind());
		return be;
	}

	/**
	 * Checks that the given AST e has type IdentExpr with the given name. Returns
	 * the given AST cast to IdentExpr.
	 *
	 * @param e
	 * @param name
	 * @return
	 */
	IdentExpr checkIdentExpr(AST e, String name) {
		assertThat("", e, instanceOf(IdentExpr.class));
		IdentExpr ident = (IdentExpr) e;
		assertEquals(name, ident.getName());
		return ident;
	}

	/**
	 * Checks that the given AST e has type ChannelSelector with the indicated color
	 *
	 * @param e
	 * @param expectedColor
	 * @return
	 */
	ChannelSelector checkChannelSelector(AST e, Kind expectedColor) {
		assertThat("", e, instanceOf(ChannelSelector.class));
		ChannelSelector chan = (ChannelSelector) e;
		assertEquals(expectedColor, chan.color());
		return chan;
	}

	/**
	 * Checks that given AST e has type PostfixExpr, and checks whether or not
	 * ChannelSelectors and PixelSelectors exist
	 *
	 * @param e
	 * @param hasChannelSelector
	 * @param hasPixelSelector
	 * @return
	 */
	PostfixExpr checkPostfixExpr(AST e, boolean hasPixelSelector, boolean hasChannelSelector) {
		assertThat("", e, instanceOf(PostfixExpr.class));
		PostfixExpr pfe = (PostfixExpr) e;
		AST channel = pfe.channel();
		assertEquals(hasChannelSelector, channel != null);
		AST pixel = pfe.pixel();
		assertEquals(hasPixelSelector, pixel != null);
		return pfe;
	}

	@Test
	void test0() throws PLCCompilerException {
		String input = """
				""

				""";
		AST ast = getAST(input);
		checkStringLitExpr(ast, "");
	}

	@Test
	void test1() throws PLCCompilerException {
		String input = """
				3

				""";
		AST ast = getAST(input);
		checkNumLitExpr(ast, 3);
	}

	@Test
	void test2() throws PLCCompilerException {
		String input = """
				b

				""";
		AST ast = getAST(input);
		checkIdentExpr(ast, "b");
	}

	@Test
	void test3() throws PLCCompilerException {
		String input = """
				"hello"

				""";
		AST ast = getAST(input);
		checkStringLitExpr(ast, "hello");
	}

	@Test
	void test4() throws PLCCompilerException {
		String input = """
				a:red

				""";
		AST ast = getAST(input);
		checkPostfixExpr(ast, false, true);
		Expr v0 = ((PostfixExpr) ast).primary();
		checkIdentExpr(v0, "a");
		ChannelSelector v1 = ((PostfixExpr) ast).channel();
		checkChannelSelector(v1, Kind.RES_red);

	}

	@Test
	void test5() throws PLCCompilerException {
		String input = """
				(a+b):green

				""";
		AST ast = getAST(input);
		checkPostfixExpr(ast, false, true);
		Expr v0 = ((PostfixExpr) ast).primary();
		checkBinaryExpr(v0, Kind.PLUS);
		Expr v1 = ((BinaryExpr) v0).getLeftExpr();
		checkIdentExpr(v1, "a");
		Expr v2 = ((BinaryExpr) v0).getRightExpr();
		checkIdentExpr(v2, "b");
		ChannelSelector v3 = ((PostfixExpr) ast).channel();
		checkChannelSelector(v3, Kind.RES_green);

	}

	@Test
	void test6() throws PLCCompilerException {
		String input = """
				x[x,y]

				""";
		AST ast = getAST(input);
		checkPostfixExpr(ast, true, false);
		Expr v0 = ((PostfixExpr) ast).primary();
		checkIdentExpr(v0, "x");
		PixelSelector v1 = ((PostfixExpr) ast).pixel();
		Expr v2 = v1.xExpr();
		checkIdentExpr(v2, "x");
		Expr v3 = v1.yExpr();
		checkIdentExpr(v3, "y");
	}

	@Test
	void test7() throws PLCCompilerException {
		String input = """
				 (3)
				""";
		AST ast = getAST(input);
		checkNumLitExpr(ast, 3);
	}

	@Test
	void test8() throws PLCCompilerException {
		String input = """
				 (!3)
				""";
		AST ast = getAST(input);
		checkUnaryExpr(ast, Kind.BANG);
		Expr v0 = ((UnaryExpr) ast).getExpr();
		checkNumLitExpr(v0, 3);
	}

	@Test
	void test9() throws PLCCompilerException {
		String input = """
				 -3
				""";
		AST ast = getAST(input);
		checkUnaryExpr(ast, Kind.MINUS);
		Expr v0 = ((UnaryExpr) ast).getExpr();
		checkNumLitExpr(v0, 3);
	}

	@Test
	void test10() throws PLCCompilerException {
		String input = """
				 (-3)
				""";
		AST ast = getAST(input);
		checkUnaryExpr(ast, Kind.MINUS);
		Expr v0 = ((UnaryExpr) ast).getExpr();
		checkNumLitExpr(v0, 3);
	}

	@Test
	void test11() throws PLCCompilerException {
		String input = """
				a[x+1,y-2]:blue

				""";
		AST ast = getAST(input);
		checkPostfixExpr(ast, true, true);
		Expr v0 = ((PostfixExpr) ast).primary();
		checkIdentExpr(v0, "a");
		PixelSelector v1 = ((PostfixExpr) ast).pixel();
		Expr v2 = v1.xExpr();
		checkBinaryExpr(v2, Kind.PLUS);
		Expr v3 = ((BinaryExpr) v2).getLeftExpr();
		checkIdentExpr(v3, "x");
		Expr v4 = ((BinaryExpr) v2).getRightExpr();
		checkNumLitExpr(v4, 1);
		Expr v5 = v1.yExpr();
		checkBinaryExpr(v5, Kind.MINUS);
		Expr v6 = ((BinaryExpr) v5).getLeftExpr();
		checkIdentExpr(v6, "y");
		Expr v7 = ((BinaryExpr) v5).getRightExpr();
		checkNumLitExpr(v7, 2);
		ChannelSelector v8 = ((PostfixExpr) ast).channel();
		checkChannelSelector(v8, Kind.RES_blue);
	}


	@Test
	void test13() throws PLCCompilerException {
		String input = """
				1-2+3*4/5%6
				""";
		AST ast = getAST(input);
		checkBinaryExpr(ast, Kind.PLUS);
		Expr v0 = ((BinaryExpr) ast).getLeftExpr();
		checkBinaryExpr(v0, Kind.MINUS);
		Expr v1 = ((BinaryExpr) v0).getLeftExpr();
		checkNumLitExpr(v1, 1);
		Expr v2 = ((BinaryExpr) v0).getRightExpr();
		checkNumLitExpr(v2, 2);
		Expr v3 = ((BinaryExpr) ast).getRightExpr();
		checkBinaryExpr(v3, Kind.MOD);
		Expr v4 = ((BinaryExpr) v3).getLeftExpr();
		checkBinaryExpr(v4, Kind.DIV);
		Expr v5 = ((BinaryExpr) v4).getLeftExpr();
		checkBinaryExpr(v5, Kind.TIMES);
		Expr v6 = ((BinaryExpr) v5).getLeftExpr();
		checkNumLitExpr(v6, 3);
		Expr v7 = ((BinaryExpr) v5).getRightExpr();
		checkNumLitExpr(v7, 4);
		Expr v8 = ((BinaryExpr) v4).getRightExpr();
		checkNumLitExpr(v8, 5);
		Expr v9 = ((BinaryExpr) v3).getRightExpr();
		checkNumLitExpr(v9, 6);
	}

	@Test
	void test14() throws PLCCompilerException {
		String input = """
				MAGENTA
				""";
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(ConstExpr.class));
	}

	@Test
	void test15() throws PLCCompilerException {
		String input = """
				 Z
				""";
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(ConstExpr.class));
	}

	@Test
	void test18() throws PLCCompilerException {
		String input = """
				f+g
				""";
		AST ast = getAST(input);
		checkBinaryExpr(ast, Kind.PLUS);
		Expr v0 = ((BinaryExpr) ast).getLeftExpr();
		checkIdentExpr(v0, "f");
		Expr v1 = ((BinaryExpr) ast).getRightExpr();
		checkIdentExpr(v1, "g");
	}

	@Test
	void test19() throws PLCCompilerException {
		String input = """
				 ? d -> e , f
				""";
		AST ast = getAST(input);
		checkConditionalExpr(ast);
		Expr v0 = ((ConditionalExpr) ast).getGuardExpr();
		checkIdentExpr(v0, "d");
		Expr v1 = ((ConditionalExpr) ast).getTrueExpr();
		checkIdentExpr(v1, "e");
		Expr v2 = ((ConditionalExpr) ast).getFalseExpr();
		checkIdentExpr(v2, "f");
	}

	@Test
	void test29() throws PLCCompilerException {
		String input = """
				b+2
				""";
		AST ast = getAST(input);
		checkBinaryExpr(ast, Kind.PLUS);
		Expr v0 = ((BinaryExpr) ast).getLeftExpr();
		checkIdentExpr(v0, "b");
		Expr v1 = ((BinaryExpr) ast).getRightExpr();
		checkNumLitExpr(v1, 2);
	}

	@Test
	void test30() throws PLCCompilerException {
		String input = """

				""";
		assertThrows(SyntaxException.class, () -> {
			@SuppressWarnings("unused")
			AST ast = getAST(input);
		});
	}

	@Test
	void test31() throws PLCCompilerException {
		String input = """
				b + + 2
				""";
		assertThrows(SyntaxException.class, () -> {
			@SuppressWarnings("unused")
			AST ast = getAST(input);
		});
	}

	@Test
	void test32() throws PLCCompilerException {
		String input = """
				3 @ 4
				""";
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			AST ast = getAST(input);
		});
	}
	@Test
	void unitTestLogicOrExpression() throws PLCCompilerException {
		String input = """
         x || y
         """;
		AST ast = getAST(input);
		BinaryExpr expr = checkBinaryExpr(ast, Kind.OR);
		checkIdentExpr(expr.getLeftExpr(), "x");
		checkIdentExpr(expr.getRightExpr(), "y");
	}


	@Test
	void unitTestLogicBitOrExpression() throws PLCCompilerException {
		String input = """
         x | y
         """;
		AST ast = getAST(input);
		BinaryExpr expr = checkBinaryExpr(ast, Kind.BITOR);
		checkIdentExpr(expr.getLeftExpr(), "x");
		checkIdentExpr(expr.getRightExpr(), "y");
	}


	@Test
	void unitTestLogicAndExpression() throws PLCCompilerException {
		String input = """
         x && y
         """;
		AST ast = getAST(input);
		BinaryExpr expr = checkBinaryExpr(ast, Kind.AND);
		checkIdentExpr(expr.getLeftExpr(), "x");
		checkIdentExpr(expr.getRightExpr(), "y");
	}


	@Test
	void unitTestLogicBitAndExpression() throws PLCCompilerException {
		String input = """
         x & y
         """;
		AST ast = getAST(input);
		BinaryExpr expr = checkBinaryExpr(ast, Kind.BITAND);
		checkIdentExpr(expr.getLeftExpr(), "x");
		checkIdentExpr(expr.getRightExpr(), "y");
	}


	@Test
	void unitTestNestedAndOrExpression() throws PLCCompilerException {
		String input = """
         x || y && z
         """;
		AST ast = getAST(input);
		BinaryExpr expr = checkBinaryExpr(ast, Kind.OR);
		checkIdentExpr(expr.getLeftExpr(), "x");
		BinaryExpr innerExpr = checkBinaryExpr(expr.getRightExpr(), Kind.AND);
		checkIdentExpr(innerExpr.getLeftExpr(), "y");
		checkIdentExpr(innerExpr.getRightExpr(), "z");
	}


	@Test
	void unitTestLtExpression() throws PLCCompilerException {
		String input = """
         x < y
         """;
		AST ast = getAST(input);
		BinaryExpr expr = checkBinaryExpr(ast, Kind.LT);
		checkIdentExpr(expr.getLeftExpr(), "x");
		checkIdentExpr(expr.getRightExpr(), "y");
	}


	@Test
	void unitTestPowExpression() throws PLCCompilerException {
		String input = """
         x ** y
         """;
		AST ast = getAST(input);
		BinaryExpr expr = checkBinaryExpr(ast, Kind.EXP);
		checkIdentExpr(expr.getLeftExpr(), "x");
		checkIdentExpr(expr.getRightExpr(), "y");
	}


	@Test
	void unitTestWidthInUnaryExpression() throws PLCCompilerException {
		String input = """
         width 42
         """;
		AST ast = getAST(input);
		UnaryExpr unaryWidth = checkUnaryExpr(ast, Kind.RES_width);
		checkNumLitExpr(unaryWidth.getExpr(), 42);
	}


	@Test
	void unitTestHeightInUnaryExpression() throws PLCCompilerException {
		String input = """
         height 42
         """;
		AST ast = getAST(input);
		UnaryExpr unaryWidth = checkUnaryExpr(ast, Kind.RES_height);
		checkNumLitExpr(unaryWidth.getExpr(), 42);
	}


	@Test
	void unitTestNestedUnaryExpression() throws PLCCompilerException {
		String input = """
         width -42
         """;
		AST ast = getAST(input);
		Expr unaryWidth = checkUnaryExpr(ast, Kind.RES_width).getExpr();
		Expr unaryNegation = checkUnaryExpr(unaryWidth, Kind.MINUS).getExpr();
		checkNumLitExpr(unaryNegation, 42);
	}


	@Test
	void unitTestExpandedPixelExpression() throws PLCCompilerException {
		String input = """
         [1, 2, 3]
         """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(ExpandedPixelExpr.class));
		Expr red = ((ExpandedPixelExpr) ast).getRed();
		Expr green = ((ExpandedPixelExpr) ast).getGreen();
		Expr blue = ((ExpandedPixelExpr) ast).getBlue();
		checkNumLitExpr(red, 1);
		checkNumLitExpr(green, 2);
		checkNumLitExpr(blue, 3);
	}
	/* My tests*/
	@Test
	void unitTestConditionalExpression() throws PLCCompilerException {
		String input = """
		? x&5 -> y * 2 , y - 2
		""";
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(ConditionalExpr.class));
		Expr guard = ((ConditionalExpr) ast).getGuardExpr();
		Expr left = ((ConditionalExpr) ast).getTrueExpr();
		Expr right = ((ConditionalExpr) ast).getFalseExpr();
		checkBinaryExpr(guard, Kind.BITAND);
		checkBinaryExpr(left, Kind.TIMES);
		checkBinaryExpr(right, Kind.MINUS);
	}
	@Test
	void unitTestUnaryExprOps() throws PLCCompilerException {
		String input = """
		!width-height(test)		
		""";
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(UnaryExpr.class));
		Expr unary2 = ((UnaryExpr) ast).getExpr();
		Expr unary3 = ((UnaryExpr) unary2).getExpr();
		Expr unary4 = ((UnaryExpr) unary3).getExpr();
		checkUnaryExpr(ast, Kind.BANG);
		checkUnaryExpr(unary2, Kind.RES_width);
		checkUnaryExpr(unary3, Kind.MINUS);
		checkUnaryExpr(unary4, Kind.RES_height);
	}



}
