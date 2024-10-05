package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the invalid character.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        // Initializing list to store tokens in
        //System.out.println("Starting Lexing..."); // debug print statement

        while (chars.has(0)) {
            // Ignore whitespace characters as they should not be added to the list
            while (peek("[ \b\n\r\t]")) {
                match("[ \b\n\r\t]");
                chars.skip(); // restarting token count once whitespace is skipped
            }

            if (chars.has(0)) {
                tokens.add(lexToken());
                // add characters which are NOT whitespace to the list
            }
        }
        return tokens;


        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        //System.out.println("Lexing Token at index " + chars.index + ": " + chars.get(0)); // print statements for debugging
        // method to check what type of characters each one is in the sequence

        if (peek("[A-Za-z_]")) {
            // If the character is an identifier, send to identifier method
            return lexIdentifier();
        }

        else if (peek("[+-]?") || peek("[0-9]")) {
            // check for integer signage, or integer itself
            return lexNumber();

        }

        else if (peek("'")) {
            // check for character literal in the case of a single '
            return lexCharacter();
        }


        else if (peek("\"")) {
            // check for a string literal in the case of a "
            return lexString();
        }

        else if (peek("[<>!=]") || peek("[&|]") || peek("[^\\w\\s]")) {
            // check for special character or operator
            return lexOperator();
        }

        // throw parse exception is characters does nt match any of the above specified
        throw new ParseException("Unexpected character: " + chars.get(0), chars.index);
        //throw new UnsupportedOperationException(); //TODO
    }

    public Token lexIdentifier() {
        // we can start by checking if the beginning character is valid
        if (!peek("[A-Za-z_]")) {
            throw new ParseException("Incorrect beginning to identifier", chars.index);
        }

        // as long as the identifier is valid, continue going through the characters
        while (peek("[A-Za-z0-9_-]")) {
            chars.advance();  // as long as the identifier is valid, continue going through the characters
        }

        // test with and without this
//        String identifier = chars.input.substring(chars.index - chars.length, chars.index);
//        if (identifier.startsWith("-")) {
//            throw new ParseException("Identifiers cannot start with a hyphen", chars.index - chars.length);
//        }

        return chars.emit(Token.Type.IDENTIFIER);
        //throw new UnsupportedOperationException(); //TODO
    }

    public Token lexNumber() {
        // account for the signage of the number (this will also help consider the hyphen in other cases)
        if (peek("[+-]")) {
            chars.advance();
        }
        // ensure zero is allowed
        if (peek("0")) {
            chars.advance();
            if (peek("[0-9]") && !peek("\\.")) {
                // Ensure leading 0 is not allowed
                throw new ParseException("There CANNOT be a leading zero", chars.index);
            }
        } // try all but 0 before all ints
        else if (peek("[1-9]")) {
            // allow other integers (not 0)
            while (peek("[0-9]")) {
                chars.advance();  // Consume digits of the integer
            }
        } else {
            throw new ParseException("Number cannot be formatted this way", chars.index);
        }

        // allow decimal literal
        if (peek("\\.")) {
            chars.advance();
            if (!peek("[0-9]")) {
                // throw exception in case of no number (int) after decimal
                throw new ParseException("Invalid decimal format", chars.index);
            }
            // allow values after decimal
            while (peek("[0-9]")) {
                chars.advance();
            }
            return chars.emit(Token.Type.DECIMAL);
            // send decimal to DECIMAL
        }

        // In case of no decimal - permit
        return chars.emit(Token.Type.INTEGER);
        //throw new UnsupportedOperationException(); //TODO
    }

    public Token lexCharacter() {
        // character must have single quote
        if (!peek("'")) {
            throw new ParseException("Character literal not initialized with single quote", chars.index);
        }
        chars.advance();
        // ensure escape sequence is accounted for
        if (peek("\\\\")) {
            // account for backslash/escape sequence
            chars.advance();
            if (peek("[bnrt'\"\\\\]")) {
                // cover escape characters
                chars.advance();

            } else {

                throw new ParseException("This escape sequence is not covered", chars.index);
            }
        } else if (peek("[^'\n\r\\\\]")) {
            // individual valid character

            chars.advance();
        } else {

            throw new ParseException("This is an invalid character literal", chars.index);
        }
        // account for single quote closing character
        if (!peek("'")) {

            throw new ParseException("Character literal not terminated with single quote", chars.index);
        }
        chars.advance();

        return chars.emit(Token.Type.CHARACTER);
        //throw new UnsupportedOperationException(); //TODO
    }

    public Token lexString() {
        // Make sure string starts with double quote "
        if (!peek("\"")) {
            throw new ParseException("String literal must start with a double quote ", chars.index);
        }
        chars.advance();

        // Read string contents until double quote to terminate
        while (!peek("\"") && !peek("[\n\r]")) {
            // Terminate at double quote or newline
            if (peek("\\\\")) {
                // account for backspace sequence
                chars.advance();
                if (peek("[bnrt'\"\\\\]")) {
                    chars.advance();
                } else {
                    throw new ParseException("Invalid escape sequence in string", chars.index);
                }
            } else if (peek("[^\"]")) {
                // all characters except unescaped double quote
                chars.advance();
            } else {
                throw new ParseException("Invalid character in string", chars.index);
            }
        }

        // Ensure there is a closing double quote
        if (!peek("\"")) {
            throw new ParseException("Unterminated string literal", chars.index);
        }
        chars.advance();

        return chars.emit(Token.Type.STRING);
        //throw new UnsupportedOperationException(); //TODO
    }

    public void lexEscape() {
        // Make sure backslash starts the escape sequence
        if (!peek("\\\\")) {
            throw new ParseException("Backslash must appear at beginning of escape sequence", chars.index);
        }
        chars.advance();

        // Ensure valid escape character option follows backslash
        if (peek("[bnrt'\"\\\\]")) {
            chars.advance();
        } else {
            throw new ParseException("Escape sequence is invalid", chars.index);
        }
        // throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        // First, check for two-character operators like &&, ||, ==, !=, <=, >=.
        if (peek("&", "&") || peek("|", "|") || peek("=", "=") || peek("!", "=") || peek("<", "=") || peek(">", "=")) {
            chars.advance();  // consume the first character
            chars.advance();  // consume the second character
            return chars.emit(Token.Type.OPERATOR);  // emit the two-character operator
        }

        // If we encounter a single '&' or '|' we need to check the next character before emitting.
        if (peek("&") || peek("|")) {
            char currentChar = chars.get(0);
            chars.advance();  // move past the first character

            // After encountering a single & or |, if the next character is also & or |, it should be combined.
            if (chars.has(0) && chars.get(0) == currentChar) {
                chars.advance();  // move past the second character
                return chars.emit(Token.Type.OPERATOR);  // emit the two-character operator like '||' or '&&'
            }

            // If no second & or | follows, emit just the single character operator.
            return chars.emit(Token.Type.OPERATOR);  // emit single operator like '&' or '|'
        }

        // If none of the special two-character operators matched, check for any other single operator.
        if (peek("[^\\w\\s]")) {
            chars.advance();  // consume the single character
            return chars.emit(Token.Type.OPERATOR);  // emit the single character operator
        }

        // If nothing matches, throw an exception.
        throw new ParseException("Operator is not valid", chars.index);
    }


    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {

        for ( int i = 0; i < patterns.length; i++) {
            if ( !chars.has(i) ||
                 !String.valueOf(chars.get(i)).matches(patterns[i]) ) {
                return false;
            }
        }

        return true;
        //throw new UnsupportedOperationException(); //TODO (in Lecture)
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if (peek) {

            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
       //throw new UnsupportedOperationException(); //TODO (in Lecture)
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
