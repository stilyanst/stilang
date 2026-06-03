package com.stilang.type_checking;

public class TypeException extends RuntimeException {
    public final int line;
    public TypeException(String msg, int line) {
        super(line > 0 ? "Line " + line + ": " + msg : msg);
        this.line = line;
    }
}
