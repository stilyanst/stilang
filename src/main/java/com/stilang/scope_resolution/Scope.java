package com.stilang.scope_resolution;

import java.util.LinkedHashMap;
import java.util.Map;

public class Scope {
    private final Map<String, Symbol> symbols = new LinkedHashMap<>();
    public final Scope parent; // null for global scope

    public Scope(Scope parent) {
        this.parent = parent;
    }

    // Define a new name in THIS scope only
    public void define(Symbol symbol) {
        if (symbols.containsKey(symbol.name))
            throw new ResolveException(
                "'" + symbol.name + "' is already defined in this scope",
                symbol.line);
        symbols.put(symbol.name, symbol);
    }

    // Look up a name by walking up the chain until found
    // TODO: Use Optional
    public Symbol resolve(String name) {
        Symbol s = symbols.get(name);
        if (s != null) return s;
        if (parent != null) return parent.resolve(name);
        return null; // not found
    }

    public boolean has(String name) {
        return symbols.containsKey(name);
    }
}
