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

        while (chars.has(0)) {
            // Ignore whitespace characters as they should not be added to the list
            while (peek("[ \b\n\r\t]")) {
                match("[ \b\n\r\t]");
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
        // method to check what type of characters each one is in the sequence

        if (peek("[A-Za-z_]")) {
            // If the character is an identifier, send to identifier method
            return lexIdentifier();
        }

        else if (peek("[+-]?") && peek("[0-9]")) {
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

        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexCharacter() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexString() {
        throw new UnsupportedOperationException(); //TODO
    }

    public void lexEscape() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        throw new UnsupportedOperationException(); //TODO
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
