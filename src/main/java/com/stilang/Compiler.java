package com.stilang;

import com.stilang.ast.Decl;
import com.stilang.ast.Parser;
import com.stilang.emitter.Emitter;
import com.stilang.scope_resolution.Resolver;
import com.stilang.type_checking.TypeChecker;
import com.stilang.lexer.Lexer;
import com.stilang.lexer.Token;

import java.util.List;

public class Compiler {

    /**
     * Run the full pipeline on a stilang source string.
     * Returns the generated C source code as a String.
     *
     * Throws:
     *   ParseException   — syntax errors
     *   ResolveException — undefined or redefined names
     *   TypeExcaption    — type mismatches
     *   EmitException    — code generation errors
     */
    public String compile(String source) {

        // ── 1. Lex ────────────────────────────────────────────────────────────
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        // ── 2. Parse ──────────────────────────────────────────────────────────
        Parser parser = new Parser(tokens);
        List<Decl> ast = parser.parse();

        // ── 3. Resolve names ──────────────────────────────────────────────────
        Resolver resolver = new Resolver();
        resolver.resolveProgram(ast);

        // ── 4. Type check ─────────────────────────────────────────────────────
        TypeChecker checker = new TypeChecker(resolver.resolutions);
        checker.checkProgram(ast);

        // ── 5. Emit C ─────────────────────────────────────────────────────────
        Emitter emitter = new Emitter(checker.types);
        return emitter.emit(ast);
    }
}
