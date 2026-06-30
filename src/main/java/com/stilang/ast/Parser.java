package com.stilang.ast;

import java.util.ArrayList;
import java.util.List;
import com.stilang.lexer.Token;
import com.stilang.lexer.TokenType;

public class Parser {

    private final List<Token> tokens;
    private int pos;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    public List<Decl> parse() {
        List<Decl> decls = new ArrayList<>();
        while (!isAtEnd()) {
            decls.add(parseDecl());
        }
        return decls;
    }


    private Decl parseDecl() {
        if (match(TokenType.FN))     return parseFn();
        if (match(TokenType.STRUCT)) return parseStruct();
        throw new ParseException("expected declaration", peek().line());
    }

    private Decl.Struct parseStruct() {
        Token name = expect(TokenType.IDENT, "expected struct name");
        expect(TokenType.LBRACE, "expected '{'");

        List<Decl.Field> fields = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            Token fname = expect(TokenType.IDENT, "expected field name");
            expect(TokenType.COLON, "expected ':'");
            Token ftype = expect(TokenType.IDENT, "expected field type");
            String ftypeStr = ftype.value();
            if (match(TokenType.LBRACKET)) {
                expect(TokenType.RBRACKET, "expected ']'");
                ftypeStr = ftypeStr + "[]";
            }
            expect(TokenType.SEMICOLON, "expected ';'");
            fields.add(new Decl.Field(fname.value(), ftypeStr));
        }

        expect(TokenType.RBRACE, "expected '}'");
        // An optional trailing ';' after the struct body is accepted.
        match(TokenType.SEMICOLON);
        return new Decl.Struct(name.value(), fields, name.line());
    }

    private Decl.Function parseFn() {
        Token name = expect(TokenType.IDENT, "expected function name");
        expect(TokenType.LPAREN, "expected '('");

        List<Decl.Param> params = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                Token pname = expect(TokenType.IDENT, "expected parameter name");
                expect(TokenType.COLON, "expected ':'");
                Token ptype = expect(TokenType.IDENT, "expected type");
                String ptypeStr = ptype.value();
                if (match(TokenType.LBRACKET)) {
                    expect(TokenType.RBRACKET, "expected ']'");
                    ptypeStr = ptypeStr + "[]";
                }
                params.add(new Decl.Param(pname.value(), ptypeStr)); 
            } while (match(TokenType.COMMA));
        }

        expect(TokenType.RPAREN, "expected ')'");

        // If a return type is not specified void is assumed
        String returnType = "void";
        if (match(TokenType.ARROW)) {
            Token retTok = expect(TokenType.IDENT, "expected return type");
            returnType = retTok.value();
            if (match(TokenType.LBRACKET)) {
                expect(TokenType.RBRACKET, "expected ']'");
                returnType = returnType + "[]";
            }
        }

        Stmt.Block body = parseBlock();
        return new Decl.Function(name.value(), params, returnType, body, name.line());
    }

    private Stmt parseStmt() {
        if (match(TokenType.LET))    return parseLet();
        if (match(TokenType.RETURN)) return parseReturn();
        if (match(TokenType.IF))     return parseIf();
        if (match(TokenType.WHILE))  return parseWhile();
        if (check(TokenType.LBRACE)) return parseBlock();
        return parseExprStmt();
    }

    private Stmt.Block parseBlock() {
        expect(TokenType.LBRACE, "expected '{'");
        List<Stmt> stmts = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            stmts.add(parseStmt());
        }
        expect(TokenType.RBRACE, "expected '}'");
        return new Stmt.Block(stmts);
    }

    private Stmt parseLet() {
        Token name = expect(TokenType.IDENT, "expected variable name");
        String type = null;
        if (match(TokenType.COLON)) {
            Token typeName = expect(TokenType.IDENT, "expected type");
            // check for [] suffix
            if (match(TokenType.LBRACKET)) {
                expect(TokenType.RBRACKET, "expected ']'");
                type = typeName.value() + "[]";
            } else {
                type = typeName.value();
            }
        }
        expect(TokenType.EQ, "expected '='");
        Expr init = parseExpr();
        expect(TokenType.SEMICOLON, "expected ';'");
        return new Stmt.Let(name.value(), type, init);
}

    private Stmt parseReturn() {
        int line = previous().line();
        Expr value = check(TokenType.SEMICOLON) ? null : parseExpr();
        expect(TokenType.SEMICOLON, "expected ';'");
        return new Stmt.Return(value, line);
    }

    private Stmt parseIf() {
        expect(TokenType.LPAREN, "expected '('");
        Expr cond = parseExpr();
        expect(TokenType.RPAREN, "expected ')'");
        Stmt.Block then = parseBlock();
        Stmt.Block else_ = null;
        if (match(TokenType.ELSE)) else_ = parseBlock();
        return new Stmt.If(cond, then, else_);
    }

    private Stmt parseWhile() {
        expect(TokenType.LPAREN, "expected '('");
        Expr cond = parseExpr();
        expect(TokenType.RPAREN, "expected ')'");
        Stmt.Block body = parseBlock();
        return new Stmt.While(cond, body);
    }

    private Stmt parseExprStmt() {
        Expr expr = parseExpr();
        expect(TokenType.SEMICOLON, "expected ';'");
        return new Stmt.ExprStmt(expr);
    }

    // Parese expressions in order using  cascading to preserve precedence.
    private Expr parseExpr()       { return parseAssign(); }

    private Expr parseAssign() {
        Expr left = parseOr();
        if (match(TokenType.EQ)) {
            Expr value = parseAssign(); // right-associative
            if (left instanceof Expr.Ident id)
                return new Expr.Assign(id.name(), value, id.line());
            if (left instanceof Expr.FieldAccess fa)
                return new Expr.FieldAssign(fa.target(), fa.field(), value, fa.line());
            throw new ParseException("invalid assignment target", previous().line());
        }
        return left;
    }

    private Expr parseOr() {
        Expr left = parseAnd();
        while (match(TokenType.PIPEPIPE)) {
            String op = previous().value();
            left = new Expr.Binary(left, op, parseAnd(), previous().line());
        }
        return left;
    }

    private Expr parseAnd() {
        Expr left = parseEquality();
        while (match(TokenType.AMPAMP)) {
            String op = previous().value();
            left = new Expr.Binary(left, op, parseEquality(), previous().line());
        }
        return left;
    }

    private Expr parseEquality() {
        Expr left = parseComparison();
        while (match(TokenType.EQEQ, TokenType.BANGEQ)) {
            String op = previous().value();
            left = new Expr.Binary(left, op, parseComparison(), previous().line());
        }
        return left;
    }

    private Expr parseComparison() {
        Expr left = parseAddSub();
        while (match(TokenType.LT, TokenType.GT, TokenType.LTEQ, TokenType.GTEQ)) {
            String op = previous().value();
            left = new Expr.Binary(left, op, parseAddSub(), previous().line());
        }
        return left;
    }

    private Expr parseAddSub() {
        Expr left = parseMulDiv();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            String op = previous().value();
            left = new Expr.Binary(left, op, parseMulDiv(), previous().line());
        }
        return left;
    }

    private Expr parseMulDiv() {
        Expr left = parseUnary();
        while (match(TokenType.STAR, TokenType.SLASH)) {
            String op = previous().value();
            left = new Expr.Binary(left, op, parseUnary(), previous().line());
        }
        return left;
    }

    private Expr parseUnary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            String op = previous().value();
            return new Expr.Unary(op, parseUnary(), previous().line());
        }
        return parseCall();
    }

    private Expr parseCall() {
    Expr expr = parsePrimary();
    while (true) {
        if (match(TokenType.LPAREN)) {
            List<Expr> args = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    args.add(parseExpr());
                }
                while (match(TokenType.COMMA));
            }
            expect(TokenType.RPAREN, "expected ')'");
            expr = new Expr.Call(expr, args, previous().line());
        } else if (match(TokenType.LBRACKET)) {
            int line = previous().line();
            Expr index = parseExpr();
            expect(TokenType.RBRACKET, "expected ']'");
            expr = new Expr.Index(expr, index, line);
        } else if (match(TokenType.DOT)) {
            int line = previous().line();
            Token field = expect(TokenType.IDENT, "expected field name after '.'");
            expr = new Expr.FieldAccess(expr, field.value(), line);
        } else {
            break;
        }
    }
    return expr;
}

    private Expr parsePrimary() {
        if (match(TokenType.INT))    return new Expr.Literal(Integer.parseInt(previous().value()), previous().line());
        if (match(TokenType.FLOAT))  return new Expr.Literal(Double.parseDouble(previous().value()), previous().line());
        if (match(TokenType.STRING)) return new Expr.Literal(previous().value(), previous().line());
        if (match(TokenType.TRUE))   return new Expr.Literal(true, previous().line());
        if (match(TokenType.FALSE))  return new Expr.Literal(false, previous().line());

        if (check(TokenType.IDENT)) {
            Token id = advance();
            // Struct literal:  Point { x = 3, y = 4 }
            if (check(TokenType.LBRACE)) {
                return parseStructLiteral(id);
            }
            return new Expr.Ident(id.value(), id.line());
        }

        if (match(TokenType.LPAREN)) {
            Expr inner = parseExpr();
            expect(TokenType.RPAREN, "expected ')'");
            return inner;
        }
        // Array literal: [1, 2, 3]
        if (match(TokenType.LBRACKET)) {
            int line = previous().line();
            List<Expr> elements = new ArrayList<>();
            if (!check(TokenType.RBRACKET)) {
                do {
                    elements.add(parseExpr());
                } while (match(TokenType.COMMA));
            }
            expect(TokenType.RBRACKET, "expected ']'");
            return new Expr.ArrayLiteral(elements, line);
        }

        throw new ParseException("expected expression", peek().line());
    }

    private Expr parseStructLiteral(Token typeName) {
        expect(TokenType.LBRACE, "expected '{'");
        List<Expr.FieldInit> inits = new ArrayList<>();
        if (!check(TokenType.RBRACE)) {
            do {
                Token fname = expect(TokenType.IDENT, "expected field name");
                expect(TokenType.EQ, "expected '='");
                Expr value = parseExpr();
                inits.add(new Expr.FieldInit(fname.value(), value));
            } while (match(TokenType.COMMA));
        }
        expect(TokenType.RBRACE, "expected '}'");
        return new Expr.StructLiteral(typeName.value(), inits, typeName.line());
    }

    // Helpers
    private Token peek() {
        return tokens.get(pos);
    }

    private Token peekNext() {
        return tokens.get(Math.min(pos + 1, tokens.size() - 1 ));
    }

    private Token previous() {
        return tokens.get(pos - 1);
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token advance() {
        if (!isAtEnd()) pos++;
        return previous();
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token expect(TokenType type, String message) {
        if (check(type)) return advance();
        throw new ParseException(message + ", got " + peek(), peek().line());
    }
}
