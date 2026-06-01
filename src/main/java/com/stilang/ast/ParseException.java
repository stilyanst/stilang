package com.stilang.ast;

public class ParseException extends RuntimeException {
    public final int line;
    public ParseException(String message, int line) {
        super("Line " + line + ": " + message);
        this.line = line;
    }
}
