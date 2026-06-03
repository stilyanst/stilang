package com.stilang.scope_resolution;

public class Symbol {
    public enum Kind { VARIABLE, FUNCTION, PARAMETER }

    public final String name;
    public final Kind kind;
    public final String type; // int, float, ... 
    public final int line; // where it was declared

    public Symbol(String name, Kind kind, String type, int line) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.line = line;
    }
}
