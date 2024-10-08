package plc.project;

import javax.swing.text.html.Option;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import java.math.BigInteger;
import java.math.BigDecimal;


/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling those functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();

        while (tokens.has(0)) {
            // check for all valid words

            //first LET
            if (peek("LET")) {

                fields.add(parseField());

                //check for DEF
            } else if (peek("DEF")) {

                methods.add(parseMethod());

            } else {

                // Throw parse exception if neither
                throw new ParseException("Neither LET nor DEF", tokens.get(0).getIndex());
            }
        }

        // Return source with lists/fields methods

        return new Ast.Source(fields, methods);
        //throw new UnsupportedOperationException(); //TODO

    }


    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        // first we declare empty string - strNM
        String strNM = "";

        // Then we can have a boolean to track whether there is a constant
        boolean constInStatement = false;

        // we define an expression for expression variables to null
        Ast.Expression exprAst = null;

        tokens.advance();

        // Check for keyword CONST and flip value if detected
        if (peek("CONST")){
            constInStatement = true;
            tokens.advance();
        }

        // Check for IDENTIFIER
        if (! peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Identifier Expected - ", tokens.get(0).getIndex());

        } else {

            strNM = tokens.get(0).getLiteral();

            tokens.advance();
        }

        // IF = is present, then parse initialization expression
        if (peek("=")) {
            tokens.advance();
            exprAst = parseExpression();
        }

        // Throw expression if no semicolon
        if (!match(";")) {
            throw new ParseException("There is an expected semicolon - ';'", tokens.get(0).getIndex());
        }

        return new Ast.Field(strNM, constInStatement, Optional.ofNullable(exprAst));
    }


        //FIRST IMPLEMENTATION
//        // First expect LET
//        match("LET");
//
//        // Check for keyword CONST
//        boolean isConstant = match("CONST");
//
//        // Parse identifier
//        if (!match(Token.Type.IDENTIFIER)) {
//            throw new ParseException("Identifier Expected", tokens.get(0).getIndex());
//        }
//        String name = tokens.get(-1).getLiteral();
//
//        // IF = is present, then parse initialization expression
//        Optional<Ast.Expression> expression = Optional.empty();
//        if (match(Token.Type.OPERATOR, "=")) {
//            expression = Optional.of(parseExpression());
//        }
//
//        // Throw expression if no semicolon
//        if (!match(Token.Type.OPERATOR, ";")) {
//            throw new ParseException("There is an expected semicolon - ';'", tokens.get(0).getIndex());
//        }
//
//        // Return parsed field
//        return new Ast.Field(name, isConstant, expression);
//    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        // first we declare empty string - strNM
        String strNM = "";

        // We create a list of the parameters which will be passed
        List<String> parameters = new ArrayList<>();

        // We create a list of AST statements which will be kept track of
        List<Ast.Statement> stmAST = new ArrayList<>();

        tokens.advance();

        // Parse method identifier
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Method name was expected", tokens.get(0).getIndex());
        }

        strNM = tokens.get(0).getLiteral();
        tokens.advance();

        // Parse parameter list in parentheses
        if (!match("(")) {
            throw new ParseException("Expected '(' - ", tokens.get(0).getIndex());
        }

        // Check if there are other optional function arguments
        // Then add arguments to parameters if needed
        if (peek(Token.Type.IDENTIFIER)) {

            parameters.add(tokens.get(0).getLiteral());

            tokens.advance();
            while (peek(",", Token.Type.IDENTIFIER)) {
                parameters.add(tokens.get(1).getLiteral());
                tokens.advance();
                tokens.advance();
                // twice accounting for ,
            }
        }

        if (!match(")")) {

            throw new ParseException("')' expected", tokens.get(0).getIndex());
        }
        // Expect DO keyword
        if (!match("DO")) {

            throw new ParseException("'DO' Expected", tokens.get(0).getIndex());
        }

        // Statement check

        if (tokens.has(1) && (!peek("END")))
        {
            stmAST.add(parseStatement());
        }
        else {throw new ParseException("There was a Statement expected", tokens.get(0).getIndex());}


        // Lastly expect END
        if (!match("END")) {
            throw new ParseException("'END' expected", tokens.get(0).getIndex());
        }

        // Parsed method return
        return new Ast.Method(strNM, parameters, stmAST);
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        // we will check for each keyword in this part of the statement section
        if (tokens.has(0)) {
            // LET check
            if (match("LET")) {
                Ast.Field field = parseField();
                return parseDeclarationStatement();
            }

            // IF check
            else if (match("IF")) {
                return parseIfStatement();
            }

            // FOR check
            else if (match("FOR")) {
                return parseForStatement();
            }

            // WHILE check
            else if (match("WHILE")) {
                return parseWhileStatement();
            } else if (match("RETURN")) {
                return parseReturnStatement();
            } else {
                Ast.Expression exprAST = null;

                exprAST = parseExpression();


                if (match("=") && tokens.has(1)) {
                    Ast.Expression valAST = parseExpression();

                    if (!match(";")) {
                        throw new ParseException("Semicolon expected after assignment ';'", tokens.get(0).getIndex());
                    }
                    return new Ast.Statement.Assignment(exprAST, valAST);
                }
                if (!match(";")) {
                    throw new ParseException("Semicolon expected after assignment ';'", tokens.get(0).getIndex());
                }
                return new Ast.Statement.Expression(exprAST);
            }
        }
         throw new ParseException("There was a statement expected - ", tokens.get(0).getIndex());
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        String strNM = "";
        Ast.Expression exprAST = null;

        // Parse variable name identifier
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Identifier expected after LET", tokens.get(0).getIndex());
        } else{
            strNM = tokens.get(0).getLiteral();
            tokens.advance();
        }

        if (tokens.has(1) && peek("="))
        {
            tokens.advance();
            exprAST = parseExpression();
        }

        // End semicolon
        if (!match(";"))
        {
            throw new ParseException("semicolon - ; - expected at end of declaration", tokens.get(0).getIndex());
        }

        return new Ast.Statement.Declaration(strNM, Optional.ofNullable(exprAST));

        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        Ast.Expression conAST = null;
        List<Ast.Statement> stmThen = new ArrayList<>();

        List<Ast.Statement> stmElse = new ArrayList<>();

        if (tokens.has(0)) {
            conAST = parseExpression();
        }

        // Expect DO
        if (!match("DO")) {
            throw new ParseException("DO expected after IF condition", tokens.get(0).getIndex());
        }

        if (tokens.has(0)) {
            stmThen.add(parseStatement());
        }

        // Optional ELSE check
        if (tokens.has(1) && match("ELSE")) {
            stmElse.add(parseStatement());
        }

        // END to finish
        if (!match("END")) {
            throw new ParseException("Expect END to close IF", tokens.get(0).getIndex());
        }

        return new Ast.Statement.If(conAST, stmThen, stmElse);


        //throw new UnsupportedOperationException(); //TODO
    }



    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        Ast.Statement initAST = null;
        Ast.Expression conAST = null;
        Ast.Statement incAST = null;
        List<Ast.Statement> stmAST = new ArrayList<>();

        // Opening ( parenthesis
        if (!match("("))
        {
            throw new ParseException("Expect ( after FOR", tokens.get(0).getIndex());
        }

        if (peek(Token.Type.IDENTIFIER, "="))
        {
            String strNM = tokens.get(0).getLiteral();
            tokens.advance();
            tokens.advance();


            Ast.Expression valExprAST = parseExpression();
            initAST = new Ast.Statement.Declaration(strNM, Optional.ofNullable(valExprAST));
        }

        // Expect first ;
        if (!match(";"))
        {
            throw new ParseException("Expect ; after initialization", tokens.get(0).getIndex());
        }

        // Parse loop cond
        conAST = parseExpression();

        // Second ;
        if (!match(";"))
        {
            throw new ParseException("Expected ';' after condition", tokens.get(0).getIndex());
        }


        if (peek(Token.Type.IDENTIFIER, "="))
        {
            Ast.Expression recordAST = parseExpression();
            tokens.advance();
            Ast.Expression valAST = parseExpression();
            incAST = new Ast.Statement.Assignment(recordAST, valAST);
        }

        if (tokens.has(0))
        {
            stmAST.add(parseStatement());
        }


        // Expect )
        if (!match(")"))
        {
            throw new ParseException("After increment expect )", tokens.get(0).getIndex());
        }

        // Parse loop body
        if(match("END"))
        {
            throw new ParseException("END expected to close FOR loop", tokens.get(0).getIndex());
        }



        // Parsed FOR return
        return new Ast.Statement.For(initAST, conAST, incAST, stmAST);

        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        // WHILE
        match("WHILE");

        Ast.Expression condition = parseExpression();

        // DO
        if (!match("DO")) {
            throw new ParseException("DO after WHILE", tokens.get(0).getIndex());
        }

        // LOOP BODY
        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {
            statements.add(parseStatement());
        }

        // END TO CLOSE WHILE
        if (!match("END")) {
            throw new ParseException("END to close WHILE loop", tokens.get(0).getIndex());
        }

        // Return WHILE with cond and statements
        return new Ast.Statement.While(condition, statements);
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        // RETURN
        match("RETURN");

        // parse
        Ast.Expression value = parseExpression();

        // ; to end
        if (!match(Token.Type.OPERATOR, ";")) {
            throw new ParseException("After return expression - ;", tokens.get(0).getIndex());
        }



        // expression value to return
        return new Ast.Statement.Return(value);

        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {

        return parseLogicalExpression();
        // throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression left = parseEqualityExpression(); // parse left

        while (match("&&") || match("||")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseEqualityExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseEqualityExpression() throws ParseException {
        Ast.Expression left = parseAdditiveExpression();

        while (match("==") || match("!=")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseAdditiveExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression left = parseMultiplicativeExpression();

        while (match("+") || match("-")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseMultiplicativeExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;
        // throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression left = parseSecondaryExpression();

        while (match("*") || match("/")) {
            String operator = tokens.get(-1).getLiteral();

            Ast.Expression right = parseSecondaryExpression();



            left = new Ast.Expression.Binary(operator, left, right);
        }
        return left;

        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expression parseSecondaryExpression() throws ParseException {


        Ast.Expression expression = parsePrimaryExpression();


        while (match(Token.Type.OPERATOR, ".")) {

            if (!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected identifier after '.'", tokens.get(0).getIndex());
            }
            String name = tokens.get(-1).getLiteral();


            if (match(Token.Type.OPERATOR, "(")) {

                List<Ast.Expression> arguments = new ArrayList<>();
                if (!peek(Token.Type.OPERATOR, ")")) {
                    do {
                        arguments.add(parseExpression());
                    } while (match(Token.Type.OPERATOR, ","));
                }
                match(Token.Type.OPERATOR, ")");

                expression = new Ast.Expression.Function(Optional.of(expression), name, arguments);
            } else {
                expression = new Ast.Expression.Access(Optional.of(expression), name);
            }
        }

        return expression;
        // throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {

        if (match("NIL")) {
            return new Ast.Expression.Literal(null);
        } else if (match("TRUE")) {
            return new Ast.Expression.Literal(true);
        } else if (match("FALSE")) {
            return new Ast.Expression.Literal(false);
        } else if (match(Token.Type.INTEGER)) {
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.CHARACTER)) {
            return new Ast.Expression.Literal(tokens.get(-1).getLiteral().charAt(1));
        } else if (match(Token.Type.STRING)) {
            return new Ast.Expression.Literal(tokens.get(-1).getLiteral().substring(1, tokens.get(-1).getLiteral().length() - 1));
        }


        else if (match(Token.Type.OPERATOR, "(")) {
            Ast.Expression expression = parseExpression();
            if (!match(Token.Type.OPERATOR, ")")) {
                throw new ParseException("Expected ')' to close grouped expression", tokens.get(0).getIndex());
            }
            return new Ast.Expression.Group(expression);
        }


        else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();


            if (match(Token.Type.OPERATOR, "(")) {
                List<Ast.Expression> arguments = new ArrayList<>();
                if (!peek(Token.Type.OPERATOR, ")")) {
                    do {
                        arguments.add(parseExpression());
                    } while (match(Token.Type.OPERATOR, ","));
                }
                match(Token.Type.OPERATOR, ")");
                return new Ast.Expression.Function(Optional.empty(), name, arguments);
            }


            return new Ast.Expression.Access(Optional.empty(), name);
        }


        else {
            throw new ParseException("Expected a primary expression", tokens.get(0).getIndex());
        }
        // throw new UnsupportedOperationException(); //TODO
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    public boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

// Peek with debugging print
//    private boolean peek(Object... patterns) {
//        System.out.println("Peeking: " + Arrays.toString(patterns));  // Debugging
//        for (int i = 0; i < patterns.length; i++) {
//            if (!tokens.has(i)) {
//                return false;
//            } else if (patterns[i] instanceof Token.Type) {
//                if (patterns[i] != tokens.get(i).getType()) {
//                    return false;
//                }
//            } else if (patterns[i] instanceof String) {
//                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
//                    return false;
//                }
//            } else {
//                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
//            }
//        }
//        return true;
//    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    public boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }


// Match with debugging print
//    private boolean match(Object... patterns) {
//        System.out.println("Matching: " + Arrays.toString(patterns));  // Debugging
//        boolean peek = peek(patterns);
//        if (peek) {
//            for (int i = 0; i < patterns.length; i++) {
//                System.out.println("Advancing token: " + tokens.get(0));  // Debugging
//                tokens.advance();
//            }
//        }
//        return peek;
//    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}

