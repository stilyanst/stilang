package com.stilang.ast;

import java.util.List;

import com.stilang.lexer.Lexer;

class TestParser {
    public static void main(String[] args) {
        String src = """
            fn add(a: int, b: int) -> int {
                let result = a + b;
                return result;
            }
            fn variables(a: int, b: int) {
                let variable = a;
                variable = b;
            }
        """;

        Lexer lexer   = new Lexer(src);
        Parser parser = new Parser(lexer.tokenize());
        List<Decl> ast = parser.parse();
        System.out.println(ast);
    }
}
