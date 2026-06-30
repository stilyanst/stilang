package com.stilang.emitter;

import com.stilang.ast.*;
import com.stilang.type_checking.Type;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

public class Emitter {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final StringBuilder         out       = new StringBuilder();
    private final IdentityHashMap<Expr, Type> types;   // from TypeChecker
    private int                         indent    = 0;
    private int                         tempCount = 0;

    // Names of declared struct types, collected at the start of emit().
    private final Set<String> structNames = new LinkedHashSet<>();

    // Reserved C keywords — if a user variable matches one, we mangle it
    private static final java.util.Set<String> C_KEYWORDS = java.util.Set.of(
        "auto", "break", "case", "char", "const", "continue", "default",
        "do", "double", "else", "enum", "extern", "float", "for", "goto",
        "if", "inline", "int", "long", "register", "restrict", "return",
        "short", "signed", "sizeof", "static", "struct", "switch", "typedef",
        "union", "unsigned", "void", "volatile", "while",
        "_Bool", "_Complex", "_Imaginary", "bool", "true", "false"
    );

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public Emitter(IdentityHashMap<Expr, Type> types) {
        this.types = types;
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    /**
     * Emit a full C translation unit from a list of top-level declarations.
     * Returns the generated C source as a String.
     */
    public String emit(List<Decl> program) {
        // Standard headers
        writeLine("#include <stdio.h>");
        writeLine("#include <stdlib.h>");
        writeLine("#include <stdbool.h>");
        writeLine("#include <string.h>");
        writeLine("");

        // Runtime declarations
        writeLine("void print_int(int x);");
        writeLine("void print_float(double x);");
        writeLine("void print_bool(bool x);");
        writeLine("void print_str(char* x);");
        writeLine("");

        // Struct typedefs — must precede any function that uses them.
        for (Decl d : program)
            if (d instanceof Decl.Struct s) structNames.add(s.name());
        emitStructs(program);

        // Forward-declare every function so call order doesn't matter
        for (Decl d : program) writeForwardDecl(d);
        writeLine("");

        // Emit full definitions
        for (Decl d : program) emitDecl(d);

        return out.toString();
    }

    // -------------------------------------------------------------------------
    // Forward declarations
    // -------------------------------------------------------------------------

    private void writeForwardDecl(Decl decl) {
        if (!(decl instanceof Decl.Function fn)) return;
        String ret    = mapType(fn.returnType());
        String params = fn.params().isEmpty()
            ? "void"
            : fn.params().stream()
                .map(p -> mapType(p.type()) + " " + mangle(p.name()))
                .collect(Collectors.joining(", "));
        writeLine(ret + " " + mangle(fn.name()) + "(" + params + ");");
    }

    // -------------------------------------------------------------------------
    // Structs
    // -------------------------------------------------------------------------

    /**
     * Emit every struct as a C typedef. By-value struct fields require the
     * nested struct to be fully defined first, so definitions are emitted in
     * dependency (topological) order. The type checker has already rejected
     * cycles, so this terminates.
     */
    private void emitStructs(List<Decl> program) {
        Map<String, Decl.Struct> structs = new LinkedHashMap<>();
        for (Decl d : program)
            if (d instanceof Decl.Struct s) structs.put(s.name(), s);

        Set<String> emitted = new LinkedHashSet<>();
        for (Decl.Struct s : structs.values()) emitStructDef(s, structs, emitted);
        if (!structs.isEmpty()) writeLine("");
    }

    private void emitStructDef(Decl.Struct s, Map<String, Decl.Struct> structs, Set<String> emitted) {
        if (!emitted.add(s.name())) return;
        // Emit by-value struct dependencies first.
        for (Decl.Field f : s.fields()) {
            if (!f.type().endsWith("[]") && structs.containsKey(f.type()))
                emitStructDef(structs.get(f.type()), structs, emitted);
        }
        writeLine("typedef struct {");
        indent++;
        for (Decl.Field f : s.fields())
            writeLine(mapType(f.type()) + " " + mangle(f.name()) + ";");
        indent--;
        writeLine("} " + mangle(s.name()) + ";");
        writeLine("");
    }

    // -------------------------------------------------------------------------
    // Declarations
    // -------------------------------------------------------------------------

    private void emitDecl(Decl decl) {
        switch (decl) {
            case Decl.Function fn -> emitFunction(fn);
            case Decl.Struct s   -> {} // emitted up front by emitStructs
        }
    }

    private void emitFunction(Decl.Function fn) {
        String ret    = mapType(fn.returnType());
        String params = fn.params().isEmpty()
            ? "void"
            : fn.params().stream()
                .map(p -> mapType(p.type()) + " " + mangle(p.name()))
                .collect(Collectors.joining(", "));

        writeLine(ret + " " + mangle(fn.name()) + "(" + params + ") {");
        indent++;
        emitBlock(fn.body());
        indent--;
        writeLine("}");
        writeLine("");
    }

    // -------------------------------------------------------------------------
    // Statements
    // -------------------------------------------------------------------------

    private void emitBlock(Stmt.Block block) {
        for (Stmt s : block.stmts()) emitStmt(s);
    }

    private void emitStmt(Stmt stmt) {
        switch (stmt) {
            case Stmt.Let      s -> emitLet(s);
            case Stmt.If       s -> emitIf(s);
            case Stmt.While    s -> emitWhile(s);
            case Stmt.Return   s -> emitReturn(s);
            case Stmt.ExprStmt s -> writeLine(emitExpr(s.expr()) + ";");
            case Stmt.Block    s -> {
                writeLine("{");
                indent++;
                emitBlock(s);
                indent--;
                writeLine("}");
            }
        }
    }

    private void emitLet(Stmt.Let s) {
        // Determine the C type — prefer the explicit annotation, fall back to
        // the type the checker inferred for the initialiser expression.
        String cType = s.type() != null
            ? mapType(s.type())
            : inferCType(s.init());
        writeLine(cType + " " + mangle(s.name()) + " = " + emitExpr(s.init()) + ";");
    }

    private void emitIf(Stmt.If s) {
        writeLine("if (" + emitExpr(s.cond()) + ") {");
        indent++;
        emitBlock(s.then());
        indent--;
        if (s.else_() != null) {
            writeLine("} else {");
            indent++;
            emitBlock(s.else_());
            indent--;
        }
        writeLine("}");
    }

    private void emitWhile(Stmt.While s) {
        writeLine("while (" + emitExpr(s.cond()) + ") {");
        indent++;
        emitBlock(s.body());
        indent--;
        writeLine("}");
    }

    private void emitReturn(Stmt.Return s) {
        if (s.value() == null) {
            writeLine("return;");
        } else {
            writeLine("return " + emitExpr(s.value()) + ";");
        }
    }

    // -------------------------------------------------------------------------
    // Expressions
    // -------------------------------------------------------------------------

    /**
     * Emit an expression as a C expression string.
     * Every binary expression is wrapped in parentheses — this is always
     * correct and avoids any precedence mismatch with C.
     */
    private String emitExpr(Expr expr) {
        return switch (expr) {

            case Expr.Literal e -> switch (e.value()) {
                case Integer v -> v.toString();
                case Double  v -> {
                    // Always include a decimal point so C treats it as double,
                    // not an integer constant.
                    String s = Double.toString(v);
                    yield s.contains(".") ? s : s + ".0";
                }
                case Boolean v -> v ? "true" : "false";
                case String  v -> "\"" + escapeString(v) + "\"";
                default        -> throw new EmitException("unknown literal type");
            };

            case Expr.Ident  e -> mangle(e.name());

            case Expr.Binary e -> "("
                + emitExpr(e.left())
                + " " + e.op() + " "
                + emitExpr(e.right())
                + ")";

            case Expr.Unary  e -> "(" + e.op() + emitExpr(e.operand()) + ")";

            case Expr.Assign e -> "(" + mangle(e.name()) + " = " + emitExpr(e.value()) + ")";

            case Expr.Call   e -> {
                if (!(e.callee() instanceof Expr.Ident id))
                    throw new EmitException("only direct function calls are supported");
                String args = e.args().stream()
                    .map(this::emitExpr)
                    .collect(Collectors.joining(", "));
                yield mangle(id.name()) + "(" + args + ")";
            }

            case Expr.ArrayLiteral e -> {
                // infer the element C type from the first element's annotation
                Type firstType = types.get(e.elements().get(0));
                String cElemType = firstType != null ? inferCType(e.elements().get(0)) : "int";
                String elems = e.elements().stream()
                    .map(this::emitExpr)
                    .collect(Collectors.joining(", "));
                // C compound literal: (int[]){1, 2, 3}
                yield "(" + cElemType + "[]){" + elems + "}";
            }

            case Expr.Index e -> {
                yield emitExpr(e.array()) + "[" + emitExpr(e.index()) + "]";
            }

            case Expr.StructLiteral e -> {
                String inits = e.fields().stream()
                    .map(fi -> "." + mangle(fi.name()) + " = " + emitExpr(fi.value()))
                    .collect(Collectors.joining(", "));
                yield "(" + mangle(e.typeName()) + "){" + inits + "}";
            }

            case Expr.FieldAccess e -> emitExpr(e.target()) + "." + mangle(e.field());

            case Expr.FieldAssign e -> "("
                + emitExpr(e.target()) + "." + mangle(e.field())
                + " = " + emitExpr(e.value()) + ")";
        };
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Map a stilang type name to the corresponding C type.
     * Extend this as you add more types to the language.
     */
    private String mapType(String type) {
        if (type == null) return "void";
        return switch (type) {
            case "int"   -> "int";
            case "float" -> "double";
            case "bool"  -> "bool";
            case "str"   -> "char*";
            case "void"  -> "void";
            default -> {
                // handles "int[]", "float[]", "Point[]", etc.
                if (type.endsWith("[]")) {
                    yield mapType(type.substring(0, type.length() - 2)) + "*";
                }
                // user-defined struct type
                if (structNames.contains(type)) yield mangle(type);
                throw new EmitException("unknown type: '" + type + "'");
            }
        };
    }
    /**
     * Look up the C type for an expression using the TypeChecker's annotations.
     * Used when a let binding has no explicit type annotation.
     */
    private String inferCType(Expr expr) {
        Type t = types.get(expr);
        if (t == null)
            throw new EmitException(
                "internal: no type annotation for expression — did the type checker run?");
        return cTypeOf(t);
    }

    /** Map a resolved Type to its C type string. */
    private String cTypeOf(Type t) {
        return switch (t) {
            case Type.Primitive p -> mapType(p.name());
            case Type.Void v -> "void";
            case Type.Struct st -> mangle(st.name());
            case Type.Array a -> cTypeOf(a.elementType()) + "*";
            case Type.Function f ->
                throw new EmitException("cannot use a function type as a variable type");
        };
    }

    /**
     * Escape special characters inside a string literal so the C compiler
     * sees the correct bytes.
     */
    private String escapeString(String s) {
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * Mangle a stilang identifier that collides with a C reserved word.
     * For example, a variable named "register" becomes "register_".
     */
    private String mangle(String name) {
        return C_KEYWORDS.contains(name) ? name + "_" : name;
    }

    /** Generate a fresh temporary variable name. */
    private String freshTemp() {
        return "_t" + tempCount++;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private void writeLine(String text) {
        if (!text.isBlank()) {
            out.append("    ".repeat(indent));
        }
        out.append(text).append("\n");
    }
}
