package plc.project;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
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
        //throw new UnsupportedOperationException(); //TODO

        // First expect LET
        match("LET");

        // Check for keywork CONST
        boolean isConstant = match("CONST");

        // Parse identifier
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Identifier Expected", tokens.get(0).getIndex());
        }
        String name = tokens.get(-1).getLiteral();

        // IF = is present, then parse initialization expression
        Optional<Ast.Expression> expression = Optional.empty();
        if (match(Token.Type.OPERATOR, "=")) {
            expression = Optional.of(parseExpression());
        }

        // Throw expression if no semicolon
        if (!match(Token.Type.OPERATOR, ";")) {
            throw new ParseException("There is an expected semicolon - ';'", tokens.get(0).getIndex());
        }

        // Return parsed field
        return new Ast.Field(name, isConstant, expression);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        // Expect DEF keyword
        match("DEF");

        // Parse method identifier
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Method name was expected", tokens.get(0).getIndex());
        }
        String name = tokens.get(-1).getLiteral();
        // Parse parameter list in parenthesis
        if (!match(Token.Type.OPERATOR, "(")) {
            throw new ParseException("Expected '('", tokens.get(0).getIndex());
        }
        List<String> parameters = new ArrayList<>();

        if (!peek(Token.Type.OPERATOR, ")")) {


            do {
                if (!match(Token.Type.IDENTIFIER)) {


                    throw new ParseException("Parameter identifier expected", tokens.get(0).getIndex());
                }
                parameters.add(tokens.get(-1).getLiteral());
            } while (match(Token.Type.OPERATOR, ","));
        }

        if (!match(Token.Type.OPERATOR, ")")) {


            throw new ParseException("')' expected", tokens.get(0).getIndex());
        }
        // Expect DO keyword
        if (!match("DO")) {


            throw new ParseException("'DO' Expected", tokens.get(0).getIndex());
        }

        // Parse statement list
        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {
            statements.add(parseStatement());
        }

        // Step 6: Expect the keyword END
        if (!match("END")) {
            throw new ParseException("'END' expected", tokens.get(0).getIndex());
        }

        // Parsed method return
        return new Ast.Method(name, parameters, statements);
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        // we will check for each keyword in this part of the statement section

        // LET check
        if (peek("LET")) {
            Ast.Field field = parseField();
            // Use parseField to parse the declaration
            return new Ast.Statement.Declaration(field.getName(), field.getValue());
            // Wrap it in a Declaration
        }

        // IF check
        if (peek("IF")) {
            return parseIfStatement();
        }

        // FOR check
        if (peek("FOR")) {
            return parseForStatement();
        }

        // WHILE check
        if (peek("WHILE")) {
            return parseWhileStatement();
        }

        // RETURN check
        if (peek("RETURN")) {
            match("RETURN");
            Ast.Expression expression = parseExpression();
            if (!match(Token.Type.OPERATOR, ";")) {
                throw new ParseException("Semicolon expected after statement ';'", tokens.get(0).getIndex());
            }
            return new Ast.Statement.Return(expression);
        }

        // Handle assignment or expression statement
        Ast.Expression expression = parseExpression();
        if (match(Token.Type.OPERATOR, "=")) {
            Ast.Expression value = parseExpression();
            if (!match(Token.Type.OPERATOR, ";")) {
                throw new ParseException("Semicolon expected after assignment ';'", tokens.get(0).getIndex());
            }
            return new Ast.Statement.Assignment(expression, value);
        } else if (match(Token.Type.OPERATOR, ";")) {
            return new Ast.Statement.Expression(expression);
        } else {
            throw new ParseException("Semicolon expected after expression ';'", tokens.get(0).getIndex());
        }

        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        // LET keyword
        match("LET");

        // Parse variable name identifier
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Identifier expected after LET", tokens.get(0).getIndex());
        }
        String name = tokens.get(-1).getLiteral();  // Get the identifier name

        //  if =, parse initialization expression
        Optional<Ast.Expression> value = Optional.empty();
        if (match(Token.Type.OPERATOR, "=")) {
            value = Optional.of(parseExpression());
        }

        // Semicolon expected to close declaration
        if (!match(Token.Type.OPERATOR, ";")) {
            throw new ParseException("semicolon - ; - expected at end of declaration", tokens.get(0).getIndex());
        }

        return new Ast.Statement.Declaration(name, value);

        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        // expect IF
        match("IF");

        // parse condition
        Ast.Expression condition = parseExpression();
        // Expect DO
        if (!match("DO")) {
            throw new ParseException("DO expected after IF condition", tokens.get(0).getIndex());
        }

        // Parse what follows - ELSE END
        List<Ast.Statement> thenStatements = new ArrayList<>();
        while (!peek("ELSE") && !peek("END")) {
            thenStatements.add(parseStatement());
        }

        // Parse ELSE in case of IF
        List<Ast.Statement> elseStatements = new ArrayList<>();
        if (match("ELSE")) {
            while (!peek("END")) {
                elseStatements.add(parseStatement());
            }
        }

        // Expect END to close IF
        if (!match("END")) {
            throw new ParseException("Expect END to close IF", tokens.get(0).getIndex());
        }

        return new Ast.Statement.If(condition, thenStatements, elseStatements);

        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        // Expect FOR
        match("FOR");

        // Expect (
        if (!match(Token.Type.OPERATOR, "(")) {
            throw new ParseException("Expect ( after FOR", tokens.get(0).getIndex());
        }

        // parse initialization if necessary
        Optional<Ast.Statement> initialization = Optional.empty();
        if (peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);//parse identifier

            if (!match(Token.Type.OPERATOR, "=")) {
                throw new ParseException("Expect = after identifier", tokens.get(0).getIndex());
            }
            Ast.Expression value = parseExpression();
            initialization = Optional.of(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), name), value));
        }

        // Expect first ;
        if (!match(Token.Type.OPERATOR, ";")) {
            throw new ParseException("Expect ; after initialization", tokens.get(0).getIndex());
        }

        // Parse loop cond
        Ast.Expression condition = parseExpression();

        // Second ;
        if (!match(Token.Type.OPERATOR, ";")) {
            throw new ParseException("Expected ';' after condition", tokens.get(0).getIndex());
        }

        // Parse increm
        Optional<Ast.Statement> increment = Optional.empty();
        if (peek(Token.Type.IDENTIFIER)) {

            String name = tokens.get(0).getLiteral();

            // parse identifier
            match(Token.Type.IDENTIFIER);

            if (!match(Token.Type.OPERATOR, "=")) {
                throw new ParseException("Expect = after", tokens.get(0).getIndex());
            }
            Ast.Expression value = parseExpression();
            increment = Optional.of(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), name), value));
        }

        // Expect )
        if (!match(Token.Type.OPERATOR, ")")) {
            throw new ParseException("After increment expect )", tokens.get(0).getIndex());
        }

        // Parse loop body
        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {
            statements.add(parseStatement());
        }

        // Expect END to close for loop
        if (!match("END")) {
            throw new ParseException("END expected to close FOR loop", tokens.get(0).getIndex());
        }

        // Parsed FOR return
        return new Ast.Statement.For(initialization.orElse(null), condition, increment.orElse(null), statements);

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

        // Parse the initial primary expression (e.g., obj in obj.field)
        Ast.Expression expression = parsePrimaryExpression();
        System.out.println("Initial primary expression: " + expression);  // Debugging

        // Handle the '.' operator for field access
        while (match(Token.Type.OPERATOR, ".")) {
            System.out.println("Matched '.' for field access.");  // Debugging
            if (match(Token.Type.IDENTIFIER)) {
                String fieldName = tokens.get(-1).getLiteral();
                System.out.println("Matched field name: " + fieldName);  // Debugging

                // Check if current expression has a receiver
                Optional<Ast.Expression> receiver = Optional.of(expression);
                expression = new Ast.Expression.Access(receiver, fieldName);

                System.out.println("Updated expression: " + expression);  // Debugging
            } else {
                throw new ParseException("Expected field name after '.'", tokens.get(0).getIndex());
            }
        }

        return expression;
//        Ast.Expression expression = parsePrimaryExpression();
//
//
//        while (match(Token.Type.OPERATOR, ".")) {
//
//            if (!match(Token.Type.IDENTIFIER)) {
//                throw new ParseException("Expected identifier after '.'", tokens.get(0).getIndex());
//            }
//            String name = tokens.get(-1).getLiteral();
//
//
//            if (match(Token.Type.OPERATOR, "(")) {
//
//                List<Ast.Expression> arguments = new ArrayList<>();
//                if (!peek(Token.Type.OPERATOR, ")")) {
//                    do {
//                        arguments.add(parseExpression());
//                    } while (match(Token.Type.OPERATOR, ","));
//                }
//                match(Token.Type.OPERATOR, ")");
//
//                expression = new Ast.Expression.Function(Optional.of(expression), name, arguments);
//            } else {
//                expression = new Ast.Expression.Access(Optional.of(expression), name);
//            }
//        }
//
//        return expression;
//        // throw new UnsupportedOperationException(); //TODO
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
