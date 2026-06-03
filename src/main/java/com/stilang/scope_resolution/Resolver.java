package com.stilang.scope_resolution;

import java.util.IdentityHashMap;
import java.util.List;

import com.stilang.ast.Decl;
import com.stilang.ast.Expr;
import com.stilang.ast.Stmt;

public class Resolver {

    private Scope current;

    public final IdentityHashMap<Expr, Symbol> resolutions = new IdentityHashMap<>();

    public Resolver() {
        // Start with an empty global scope
        this.current = new Scope(null);
    }

    private void pushScope() {
        current = new Scope(current);
    }

    private void popScope() {
        current = current.parent;
    }

    private void define(String name, Symbol.Kind kind, String type, int line) {
        current.define(new Symbol(name, kind, type, line));
    }

    private Symbol resolve(String name, int line) {
        Symbol s = current.resolve(name);
        if (s == null)
            throw new ResolveException("undefined name '" + name + "'", line);
        return s;
    }

    public void resolveProgram(List<Decl> decls) {
        // First pass register all function names so they can call each other
        for (Decl decl : decls) {
            if (decl instanceof Decl.Function fn) {
                define(fn.name(), Symbol.Kind.FUNCTION, fn.returnType(), fn.line());
            }
        }
        // Second pass resolve the bodies
        for (Decl decl : decls) {
            resolveDecl(decl);
        }
    }

    private void resolveDecl(Decl decl) {
        switch (decl) {
            case Decl.Function fn -> resolveFunction(fn);
        }
    }

    private void resolveFunction(Decl.Function fn) {
        pushScope();
        for (Decl.Param p : fn.params()) {
            define(p.name(), Symbol.Kind.PARAMETER, p.type(), fn.line());
        }
        resolveBlock(fn.body());
        popScope();
    }

    // -------------------------------------------------------------------------
    // Walking statements

    private void resolveStmt(Stmt stmt) {
        switch (stmt) {
            case Stmt.Block s   -> { pushScope(); resolveBlock(s); popScope(); }
            case Stmt.Let s     -> resolveLet(s);
            case Stmt.If s      -> resolveIf(s);
            case Stmt.While s   -> resolveWhile(s);
            case Stmt.Return s  -> resolveReturn(s);
            case Stmt.ExprStmt s -> resolveExpr(s.expr());
        }
    }

    private void resolveBlock(Stmt.Block block) {
        for (Stmt s : block.stmts()) resolveStmt(s);
    }

    private void resolveLet(Stmt.Let s) {
        // Resolve the initialiser BEFORE defining the name
        // so that  let x = x + 1;  correctly fails if x isn't defined yet
        resolveExpr(s.init());
        define(s.name(), Symbol.Kind.VARIABLE, s.type(), -1);
    }

    private void resolveIf(Stmt.If s) {
        resolveExpr(s.cond());
        pushScope(); resolveBlock(s.then()); popScope();
        if (s.else_() != null) {
            pushScope(); resolveBlock(s.else_()); popScope();
        }
    }

    private void resolveWhile(Stmt.While s) {
        resolveExpr(s.cond());
        pushScope(); resolveBlock(s.body()); popScope();
    }

    private void resolveReturn(Stmt.Return s) {
        if (s.value() != null) resolveExpr(s.value());
    }

    // -------------------------------------------------------------------------
    // Walking expressions

    private void resolveExpr(Expr expr) {
        switch (expr) {
            case Expr.Literal e -> {}  // nothing to resolve
            case Expr.Ident e -> { // the key check
                Symbol s = resolve(e.name(), e.line());
                resolutions.put(e, s); // record it
            } 
            case Expr.Binary e -> { resolveExpr(e.left()); resolveExpr(e.right()); }
            case Expr.Unary e -> resolveExpr(e.operand());
            case Expr.Assign e -> {
                resolveExpr(e.value());
                Symbol s = resolve(e.name(), e.line());
                resolutions.put(e, s);
            }
            case Expr.Call e -> { resolveExpr(e.callee()); e.args().forEach(this::resolveExpr); }
        }
    }
}
