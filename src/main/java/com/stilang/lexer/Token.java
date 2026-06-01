package com.stilang.lexer;

public record Token(TokenType type, String value, int line) {
    @Override
    public String toString() {
        return type + "(" + value + ")@" + line;
    }
}
