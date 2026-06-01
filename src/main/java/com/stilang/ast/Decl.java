package com.stilang.ast;

import java.util.List;

// It is interesting to observe that a program is nothing
// more than a List<Decl>
public sealed interface Decl permits Decl.Function {

    record Param(String name, String type) {}

    record Function(
        String name,
        List<Param> params,
        String returnType,
        Stmt.Block body,
        int line
    ) implements Decl {}
}
