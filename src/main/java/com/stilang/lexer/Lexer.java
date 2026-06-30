package com.stilang.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Lexer {
    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
        Map.entry("fn", TokenType.FN), 
        Map.entry("let", TokenType.LET),
        Map.entry("if", TokenType.IF),
        Map.entry("else", TokenType.ELSE),
        Map.entry("return", TokenType.RETURN),
        Map.entry("while", TokenType.WHILE),
        Map.entry("true", TokenType.TRUE),
        Map.entry("false",  TokenType.FALSE),
        Map.entry("struct", TokenType.STRUCT)
    );

    private final String source; // Code to lex
    private int start; // Start of curr token
    private int current; // Curr read position
    private int line;

    public Lexer(String source) {
        this.source = source;
        this.start = 0;
        this.current = 0;
        this.line = 1;
    }
    
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        Token tok;
        do {
            tok = nextToken();
            tokens.add(tok);
        } while (tok.type() != TokenType.EOF && tok.type() != TokenType.ERROR);
        return tokens;
    }

    private Token nextToken() {
        skipWhitespaceAndComments();
        start = current;

        if (isAtEnd()) return makeToken(TokenType.EOF);

        char c = advance();

        if (Character.isDigit(c)) return lexNumber();
        if (Character.isLetterOrDigit(c)) return lexIdentOrKeyword();
        if (c == '"') return lexString();

        return switch (c) {
            case '+' -> makeToken(TokenType.PLUS);
            case '*' -> makeToken(TokenType.STAR);
            case '/' -> makeToken(TokenType.SLASH);
            case '(' -> makeToken(TokenType.LPAREN);
            case ')' -> makeToken(TokenType.RPAREN);
            case '{' -> makeToken(TokenType.LBRACE);
            case '}' -> makeToken(TokenType.RBRACE);
            case '[' -> makeToken(TokenType.LBRACKET);
            case ']' -> makeToken(TokenType.RBRACKET);
            case ';' -> makeToken(TokenType.SEMICOLON);
            case ':' -> makeToken(TokenType.COLON);
            case ',' -> makeToken(TokenType.COMMA);
            case '.' -> makeToken(TokenType.DOT);
            case '-' -> makeToken(match('>') ? TokenType.ARROW : TokenType.MINUS);
            case '=' -> makeToken(match('=') ? TokenType.EQEQ : TokenType.EQ);
            case '!' -> makeToken(match('=') ? TokenType.BANGEQ : TokenType.BANG);
            case '<' -> makeToken(match('=') ? TokenType.LTEQ : TokenType.LT);
            case '>' -> makeToken(match('=') ? TokenType.GTEQ : TokenType.GT);
            case '&' -> match('&') ? makeToken(TokenType.AMPAMP)
                                   : errorToken("expected '&&'");
            case '|' -> match('|') ? makeToken(TokenType.PIPEPIPE)
                                   : errorToken("expected '||'");
            default -> errorToken("unexpected character: '" + c + "'");
        };
    }

    private Token lexNumber() {
        while (!isAtEnd() && Character.isDigit(peek())) advance();

        boolean isFloat = (peek() == '.' && Character.isDigit(peekNext()));
        if (isFloat) {
            advance(); // consume the dot
            while (!isAtEnd() && Character.isDigit(peek())) advance();
        }

        return makeToken(isFloat ? TokenType.FLOAT : TokenType.INT);
    }

    private Token lexIdentOrKeyword() {
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) advance();

        String word = source.substring(start, current);
        TokenType type = KEYWORDS.getOrDefault(word, TokenType.IDENT);
        return makeToken(type);
    }

    private Token lexString() {
        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\n') line++;
            if (peek() == '\\') advance();
            advance();
        }
        if (isAtEnd()) return errorToken("unterminated string");
        advance(); // Consime the closing "

        String value = source.substring(start + 1, current - 1);
        return new Token(TokenType.STRING, value, line);
    }

    // Helpers
    private void skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            char c = peek();
            switch (c) {
                case ' ', '\t', '\r' -> advance();
                case '\n' -> {
                    line++;
                    advance();
                }
                case '/' -> {
                    if (peekNext() == '/') {
                        while (!isAtEnd() && peek() != '\n') {
                            advance();
                        }
                    } else {
                        return;
                    }
                }
                default -> {return;}
            }
        }
    }

    private char advance() {
        return source.charAt(current++);
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(current);
    }

    private char peekNext() {
        return (current + 1 >= source.length()) ? '\0' : source.charAt(current + 1);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private Token makeToken(TokenType type) {
        return new Token(type, source.substring(start, current), line);
    }

    private Token errorToken(String message) {
        return new Token(TokenType.ERROR, message, line);
    }
}
