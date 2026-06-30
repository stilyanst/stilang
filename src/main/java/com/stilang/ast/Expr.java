package com.stilang.ast;

import java.util.List;

public sealed interface Expr permits
    Expr.Literal, Expr.Ident, Expr.Binary,
    Expr.Unary, Expr.Call, Expr.Assign, Expr.ArrayLiteral,
    Expr.Index, Expr.StructLiteral, Expr.FieldAccess, Expr.FieldAssign {

    // It is Interger, Double, String, Boolean
    record Literal(Object value, int line) implements Expr {}
    
    record Ident(String name, int line) implements Expr {}

    record Binary(Expr left, String op, Expr right, int line) implements Expr {}

    record Unary(String op, Expr operand, int line) implements Expr {}

    record Call(Expr callee, List<Expr> args, int line) implements Expr {}

    record Assign(String name, Expr value, int line) implements Expr {}
    
    record ArrayLiteral(List<Expr> elements, int line) implements Expr {}

    record Index(Expr array, Expr index, int line) implements Expr {}

    // Point { x = 3, y = 4 }
    record StructLiteral(String typeName, List<FieldInit> fields, int line) implements Expr {}

    // A single  name = value  inside a struct literal. Not an Expr itself.
    record FieldInit(String name, Expr value) {}

    // p.x
    record FieldAccess(Expr target, String field, int line) implements Expr {}

    // p.x = value
    record FieldAssign(Expr target, String field, Expr value, int line) implements Expr {}
}
