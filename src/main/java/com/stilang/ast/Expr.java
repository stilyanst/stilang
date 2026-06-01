package com.stilang.ast;

import java.util.List;

public sealed interface Expr permits
    Expr.Literal, Expr.Ident, Expr.Binary,
    Expr.Unary, Expr.Call, Expr.Assign {

    // It is Interger, Double, String, Boolean
    record Literal(Object value, int line) implements Expr {}
    
    record Ident(String name, int line) implements Expr {}

    record Binary(Expr left, String op, Expr right, int line) implements Expr {}

    record Unary(String op, Expr operand, int line) implements Expr {}

    record Call(Expr callee, List<Expr> args, int line) implements Expr {}

    record Assign(String name, Expr value, int line) implements Expr {}
}
