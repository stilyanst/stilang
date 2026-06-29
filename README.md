# Stilang

Stilang is a statically typed, compiled programming language built from scratch in Java. It features a hand-written lexer, recursive descent parser, name resolver, type checker, and a C code emitter - a complete compiler pipeline with no external dependencies.

Source files (`.stil`) compile to C, which is then compiled to a native binary via GCC.

## Features

- Static typing with type inference
- Functions, recursion, and nested calls
- `if`/`else`, `while` loops
- `int`, `float`, `bool`, `str` primitives
- Arrays with element access and iteration
- Built-in print functions for all primitive types
- Clear compile-time error messages with line numbers
- Single self-contained jar - runs anywhere Java is installed

## Requirements

- Java 17+
- GCC (or any C99-compatible compiler)

## Usage

```bash
java -jar stilang.jar hello.stil hello.c
gcc hello.c runtime.c -o hello
./hello
```

The compiler extracts `runtime.c` automatically and prints the exact gcc command to run.

## Example

```
fn sum(arr: int[], len: int) -> int {
    let total: int = 0;
    let i: int = 0;
    while (i < len) {
        total = total + arr[i];
        i = i + 1;
    }
    return total;
}

fn main() -> int {
    let nums: int[] = [10, 20, 30, 40, 50];
    print_int(sum(nums, 5));   // 150
    return 0;
}
```

## Compiler Pipeline

```
Source (.stil)  →  Lexer  →  Parser  →  Resolver  →  Type Checker  →  C Emitter  →  output.c
```

Each stage is implemented independently and communicates through well-defined data structures (token list, AST, symbol table).

## Documentation

See [DOCS.md](doc/DOCS.md) for the full language reference.
