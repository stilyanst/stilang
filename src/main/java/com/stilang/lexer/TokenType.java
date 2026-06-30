package com.stilang.lexer;

public enum TokenType {
    // Literals
    INT, FLOAT, STRING, IDENT,

    // Keywords
    FN, LET, IF, ELSE, RETURN, WHILE, TRUE, FALSE, STRUCT,

    // Operators
    PLUS, MINUS, STAR, SLASH, EQ, EQEQ, BANG,
    BANGEQ, LT, GT, LTEQ, GTEQ, AMPAMP, PIPEPIPE,

    // Punnctuation
    LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET,
    SEMICOLON, COLON, COMMA, ARROW, DOT,

    EOF, ERROR
}
