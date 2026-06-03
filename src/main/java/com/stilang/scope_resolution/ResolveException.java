package com.stilang.scope_resolution;

public class ResolveException extends RuntimeException {
    public final int line;
    public ResolveException(String msg, int line) {
        super("Line " + line + ": " + msg);
        this.line = line;
    }
}
