package com.stilang.lexer;

public class LexTestMain {
    public static void main(String[] args) {
        String src = """
            fn add(a: int, b: int) -> int {
                let result = a + b;
                let text = "There is a new line\nand a slash \\ and a tab \t"
                return result;
            }
            """;

        Lexer lexer = new Lexer(src);
        for (Token tok : lexer.tokenize()) {
            System.out.println(tok);
        }
    }
}
