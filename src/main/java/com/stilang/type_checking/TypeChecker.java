package com.stilang.type_checking;

import com.stilang.ast.Decl;
import com.stilang.ast.Expr;
import com.stilang.scope_resolution.Symbol;
import com.stilang.ast.Stmt;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class TypeChecker {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    // Symbol resolutions produced by the Resolver - maps each Ident/Assign Expr to its Symbol. 
    private final IdentityHashMap<Expr, Symbol> resolutions;

    // Type annotations produced by this checker - maps each Expr to its Type.
    public final IdentityHashMap<Expr, Type> types = new IdentityHashMap<>();

    // Function signatures, keyed by function name.
    private final Map<String, Type.Function> functionTypes = new HashMap<>();

    // Types of resolved symbols (variables, parameters). 
    private final IdentityHashMap<Symbol, Type> symbolTypes = new IdentityHashMap<>();

    // Return type of the function currently being checked. Null at top level.
    private Type currentReturnType = null;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public TypeChecker(IdentityHashMap<Expr, Symbol> resolutions) {
        this.resolutions = resolutions;
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    /**
     * Type-check an entire program.
     * Throws TypeException on the first type error encountered.
     */
    public void checkProgram(List<Decl> decls) {        
        // Built-in functions
        functionTypes.put("print_int", new Type.Function(List.of(Type.Primitive.INT), Type.Void.INSTANCE));
        functionTypes.put("print_float", new Type.Function(List.of(Type.Primitive.FLOAT), Type.Void.INSTANCE));
        functionTypes.put("print_bool", new Type.Function(List.of(Type.Primitive.BOOL), Type.Void.INSTANCE));
        functionTypes.put("print_str", new Type.Function(List.of(Type.Primitive.STR), Type.Void.INSTANCE));

        // First pass: register all function signatures so functions can call each other
        for (Decl decl : decls) {
            if (decl instanceof Decl.Function fn) {
                List<Type> paramTypes = fn.params().stream()
                        .map(p -> typeOf(p.type()))
                        .toList();
                Type retType = fn.returnType().equals("void")
                        ? Type.Void.INSTANCE
                        : typeOf(fn.returnType());
                functionTypes.put(fn.name(), new Type.Function(paramTypes, retType));
            }
        }
        // Second pass: check bodies
        for (Decl decl : decls) {
            checkDecl(decl);
        }
    }

    // -------------------------------------------------------------------------
    // Declarations
    // -------------------------------------------------------------------------

    private void checkDecl(Decl decl) {
        switch (decl) {
            case Decl.Function fn -> checkFunction(fn);
        }
    }

    private void checkFunction(Decl.Function fn) {
        Type.Function fnType = functionTypes.get(fn.name());

        // Register each parameter's type so the body can look them up
        for (int i = 0; i < fn.params().size(); i++) {
            Decl.Param p = fn.params().get(i);
            Symbol sym = findParamSymbol(fn.name(), p.name());
            symbolTypes.put(sym, fnType.params().get(i));
        }

        currentReturnType = fnType.returnType();
        checkBlock(fn.body());
        currentReturnType = null;
    }

    /**
     * Find the Symbol the Resolver created for a parameter.
     * This works because the Resolver stores one Symbol per (function, param-name) pair.
     *
     * TIP: A cleaner long-term approach is to store the Symbol directly on
     * the Decl.Param record so you don't need this search.
     */
    private Symbol findParamSymbol(String fnName, String paramName) {
        return resolutions.values().stream()
                .filter(s -> s.name().equals(paramName)
                          && s.kind() == Symbol.Kind.PARAMETER)
                .findFirst()
                .orElseThrow(() -> new TypeException(
                        "internal: no symbol found for param '" + paramName
                        + "' in fn '" + fnName + "'", -1));
    }

    // -------------------------------------------------------------------------
    // Statements
    // -------------------------------------------------------------------------

    private void checkStmt(Stmt stmt) {
        switch (stmt) {
            case Stmt.Block    s -> checkBlock(s);
            case Stmt.Let      s -> checkLet(s);
            case Stmt.If       s -> checkIf(s);
            case Stmt.While    s -> checkWhile(s);
            case Stmt.Return   s -> checkReturn(s);
            case Stmt.ExprStmt s -> checkExpr(s.expr());
        }
    }

    private void checkBlock(Stmt.Block block) {
        for (Stmt s : block.stmts()) checkStmt(s);
    }

    private void checkLet(Stmt.Let s) {
        Type initType = checkExpr(s.init());

        if (s.type() != null) {
            // Type annotation present - verify the initialiser matches
            Type declared = typeOf(s.type());
            expect(initType, declared,
                    "declared '" + s.type() + "' but initialiser has type '" + initType + "'", -1);
            // Store the declared type (not the inferred one) for the variable's symbol
            // NOTE: if you attach Symbol directly to Stmt.Let, replace the line below with:
            //   symbolTypes.put(s.symbol(), declared);
            Symbol sym = findLetSymbol(s.name());
            if (sym != null) symbolTypes.put(sym, declared);
        } else {
            // No annotation - infer type from the initialiser
            // NOTE: if you attach Symbol directly to Stmt.Let, replace the lines below with:
            //   symbolTypes.put(s.symbol(), initType);
            Symbol sym = findLetSymbol(s.name());
            if (sym != null) symbolTypes.put(sym, initType);
        }
    }

    /**
     * Find the Symbol the Resolver created for a let-binding by variable name.
     *
     * TIP: Same as findParamSymbol - attaching the Symbol directly to Stmt.Let
     * is cleaner and avoids this search entirely.
     */
    private Symbol findLetSymbol(String name) {
        return resolutions.values().stream()
                .filter(s -> s.name().equals(name)
                          && s.kind() == Symbol.Kind.VARIABLE)
                .findFirst()
                .orElse(null);
    }

    private void checkIf(Stmt.If s) {
        Type cond = checkExpr(s.cond());
        expect(cond, Type.Primitive.BOOL, "if condition must be 'bool'", -1);
        checkBlock(s.then());
        if (s.else_() != null) checkBlock(s.else_());
    }

    private void checkWhile(Stmt.While s) {
        Type cond = checkExpr(s.cond());
        expect(cond, Type.Primitive.BOOL, "while condition must be 'bool'", -1);
        checkBlock(s.body());
    }

    private void checkReturn(Stmt.Return s) {
        if (s.value() == null) {
            if (!(currentReturnType instanceof Type.Void))
                throw new TypeException(
                        "missing return value - expected '" + currentReturnType + "'",
                        s.line());
            return;
        }
        Type actual = checkExpr(s.value());
        expect(actual, currentReturnType,
                "return type mismatch: expected '" + currentReturnType
                + "' but got '" + actual + "'",
                s.line());
    }

    // -------------------------------------------------------------------------
    // Expressions
    // -------------------------------------------------------------------------

    /**
     * Check an expression and return its type.
     * Also annotates the expression node in the `types` map for later passes.
     */
    private Type checkExpr(Expr expr) {
        Type type = switch (expr) {

            case Expr.Literal e -> switch (e.value()) {
                case Integer v -> Type.Primitive.INT;
                case Double v -> Type.Primitive.FLOAT;
                case Boolean v -> Type.Primitive.BOOL;
                case String v -> Type.Primitive.STR;
                default -> throw new TypeException("unknown literal type", e.line());
            };

            case Expr.Ident e -> {
                Symbol s = resolutions.get(e);
                if (s == null)
                    throw new TypeException(
                            "internal: no resolution for ident '" + e.name() + "'", e.line());
                yield symbolTypes.containsKey(s) ? symbolTypes.get(s) : typeOf(s.type());
            }

            case Expr.Binary e -> checkBinary(e);
            case Expr.Unary e -> checkUnary(e);
            case Expr.Assign e -> checkAssign(e);
            case Expr.Call e -> checkCall(e);
            case Expr.ArrayLiteral e -> {
                if (e.elements().isEmpty())
                    throw new TypeException("cannot infer type of empty array literal", e.line());
                Type elemType = checkExpr(e.elements().get(0));
                for (int i = 1; i < e.elements().size(); i++) {
                    Type t = checkExpr(e.elements().get(i));
                    expect(t, elemType,
                        "array element " + i + " has type '" + t +
                        "' but expected '" + elemType + "'", e.line());
                }
                yield new Type.Array(elemType);
            }

            case Expr.Index e -> {
                Type arrayType = checkExpr(e.array());
                if (!(arrayType instanceof Type.Array a))
                    throw new TypeException(
                        "cannot index into non-array type '" + arrayType + "'", e.line());
                Type indexType = checkExpr(e.index());
                expect(indexType, Type.Primitive.INT,
                    "array index must be 'int', got '" + indexType + "'", e.line());
                yield a.elementType();
            }
        };

        types.put(expr, type);
        return type;
    }

    private Type checkBinary(Expr.Binary e) {
        Type left  = checkExpr(e.left());
        Type right = checkExpr(e.right());

        return switch (e.op()) {
            case "+", "-", "*", "/" -> {
                if (!left.isNumeric())
                    throw new TypeException(
                            "operator '" + e.op() + "' requires numeric operands, got '"
                            + left + "'", e.line());
                expect(left, right,
                        "operands of '" + e.op() + "' must have the same type: '"
                        + left + "' vs '" + right + "'", e.line());
                yield left; // int op int -> int,  float op float -> float
            }
            case "==", "!=" -> {
                expect(left, right,
                        "can only compare values of the same type: '"
                        + left + "' vs '" + right + "'", e.line());
                yield Type.Primitive.BOOL;
            }
            case "<", ">", "<=", ">=" -> {
                if (!left.isNumeric())
                    throw new TypeException(
                            "comparison '" + e.op() + "' requires numeric operands, got '"
                            + left + "'", e.line());
                expect(left, right,
                        "comparison operands must have the same type: '"
                        + left + "' vs '" + right + "'", e.line());
                yield Type.Primitive.BOOL;
            }
            case "&&", "||" -> {
                expect(left,  Type.Primitive.BOOL,
                        "left side of '" + e.op() + "' must be 'bool', got '" + left + "'",
                        e.line());
                expect(right, Type.Primitive.BOOL,
                        "right side of '" + e.op() + "' must be 'bool', got '" + right + "'",
                        e.line());
                yield Type.Primitive.BOOL;
            }
            default -> throw new TypeException(
                    "unknown binary operator '" + e.op() + "'", e.line());
        };
    }

    private Type checkUnary(Expr.Unary e) {
        Type operand = checkExpr(e.operand());
        return switch (e.op()) {
            case "-" -> {
                if (!operand.isNumeric())
                    throw new TypeException(
                            "unary '-' requires a numeric operand, got '" + operand + "'",
                            e.line());
                yield operand;
            }
            case "!" -> {
                expect(operand, Type.Primitive.BOOL,
                        "unary '!' requires 'bool', got '" + operand + "'", e.line());
                yield Type.Primitive.BOOL;
            }
            default -> throw new TypeException(
                    "unknown unary operator '" + e.op() + "'", e.line());
        };
    }

    private Type checkAssign(Expr.Assign e) {
        Symbol s = resolutions.get(e);
        if (s == null)
            throw new TypeException(
                    "internal: no resolution for assignment to '" + e.name() + "'", e.line());
        Type expected = symbolTypes.containsKey(s) ? symbolTypes.get(s) : typeOf(s.type());
        Type actual   = checkExpr(e.value());
        expect(actual, expected,
                "cannot assign '" + actual + "' to variable '"
                + e.name() + "' of type '" + expected + "'", e.line());
        return expected;
    }

    private Type checkCall(Expr.Call e) {
        if (!(e.callee() instanceof Expr.Ident id))
            throw new TypeException(
                    "only direct function calls are supported for now", e.line());

        Type.Function fnType = functionTypes.get(id.name());
        if (fnType == null)
            throw new TypeException(
                    "'" + id.name() + "' is not a defined function", e.line());

        if (e.args().size() != fnType.params().size())
            throw new TypeException(
                    "'" + id.name() + "' expects " + fnType.params().size()
                    + " argument(s) but got " + e.args().size(), e.line());

        for (int i = 0; i < e.args().size(); i++) {
            Type argType   = checkExpr(e.args().get(i));
            Type paramType = fnType.params().get(i);
            expect(argType, paramType,
                    "argument " + (i + 1) + " of '" + id.name()
                    + "': expected '" + paramType + "' but got '" + argType + "'",
                    e.line());
        }

        return fnType.returnType();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Convert a type name string from source code into a Type object. */
    private Type typeOf(String name) {
        if (name == null) return Type.Primitive.INT;
        // handle array types like "int[]"
        if (name.endsWith("[]")) {
            Type inner = typeOf(name.substring(0, name.length() - 2));
            return new Type.Array(inner);
        }
        return switch (name) {
            case "int"   -> Type.Primitive.INT;
            case "float" -> Type.Primitive.FLOAT;
            case "bool"  -> Type.Primitive.BOOL;
            case "str"   -> Type.Primitive.STR;
            case "void"  -> Type.Void.INSTANCE;
            default -> throw new TypeException("unknown type '" + name + "'", -1);
        };
    }

    /** Assert that actual == expected, throwing a TypeException if not. */
    private void expect(Type actual, Type expected, String message, int line) {
        if (!actual.equals(expected))
            throw new TypeException(message, line);
    }
}
