package com.stilang.ast;

import java.util.List;

public sealed interface Stmt permits
    Stmt.ExprStmt, Stmt.Let, Stmt.Return,
    Stmt.If, Stmt.While, Stmt.Block {
    
    record ExprStmt(Expr expr) implements Stmt {}

    record Let(String name, String type, Expr init) implements Stmt {}
    
    record Return(Expr value, int line) implements Stmt {}

    record If(Expr cond, Stmt.Block then, Stmt.Block else_) implements Stmt {} 

    record While(Expr cond, Stmt.Block body) implements Stmt {}

    record Block(List<Stmt> stmts) implements Stmt {}
}
