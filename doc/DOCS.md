# Stilang Language Reference

Stilang is a statically typed, compiled language that targets C. Programs are written in `.stil` files, compiled to C, then to a native binary via any C compiler.

```
fn factorial(n: int) -> int {
    if (n <= 1) { return 1; }
    return n * factorial(n - 1);
}

fn main() -> int {
    let result: int = factorial(10);
    print_int(result);
    return 0;
}
```

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Types](#types)
3. [Variables](#variables)
4. [Functions](#functions)
5. [Expressions](#expressions)
6. [Control Flow](#control-flow)
7. [Operators](#operators)
8. [Built-in Functions](#built-in-functions)
9. [Compiling](#compiling)

---

## Getting Started

**Requirements:** Java 17+, GCC (or any C99-compatible compiler)

```bash
java -jar stilang.jar hello.stil hello.c
# The compiler prints the exact command to run next:
#   gcc hello.c runtime.c -o hello
gcc hello.c runtime.c -o hello
./hello
```

**hello.stil**
```
fn main() -> int {
    print_str("Hello, world!");
    return 0;
}
```

Every program must define a function named `main` returning `int` — this is the entry point where execution begins. There can only be one `main` per program. The value returned from `main` becomes the process exit code.

---

## Types

Stilang has four primitive types and `void` for functions that return no value.

| Type    | Description           | Example literals        |
|---------|-----------------------|-------------------------|
| `int`   | 32-bit signed integer | `0`, `42`, `-7`         |
| `float` | 64-bit floating point | `3.14`, `-0.5`, `1.0`   |
| `bool`  | Boolean               | `true`, `false`         |
| `str`   | Immutable string      | `"hello"`, `"world"`    |
| `void`  | No return value       | —                       |

Types are checked at compile time. There is no implicit conversion between types — `int` and `float` cannot be mixed in the same expression.

```
// ERROR: cannot add int and float
let x: float = 1.0 + 2;

// OK: both operands are float
let x: float = 1.0 + 2.0;
```

---

## Variables

Variables are declared with `let` and must be initialised at the point of declaration. There is no `null` and no uninitialised state.

```
let x: int = 10;
let pi: float = 3.14159;
let greeting: str = "hello";
let active: bool = true;
```

**Type inference** — the type annotation is optional when it can be unambiguously inferred from the initialiser:

```
let x = 42;        // int
let y = 3.14;      // float
let z = true;      // bool
let s = "hello";   // str
```

**Assignment** — variables are mutable. Use `=` to reassign:

```
let count: int = 0;
count = count + 1;   // count is now 1
count = count * 2;   // count is now 2
```

The assigned value must match the variable's type. Reassigning to a different type is a compile error:

```
let x: int = 0;
x = 3.14;   // ERROR: cannot assign float to variable of type int
```

---

## Functions

```
fn name(param1: type1, param2: type2) -> returnType {
    // body
}
```

- All parameters require an explicit type annotation.
- The return type follows `->`. If omitted, the return type is `void`.
- Every non-void function must return a value.
- Functions may call any other function in the program regardless of declaration order.
- Recursion is supported.

**Examples:**

```
fn add(a: int, b: int) -> int {
    return a + b;
}

fn greet() {
    print_str("hello");
}

fn max(a: int, b: int) -> int {
    if (a > b) { return a; }
    return b;
}
```

**Calling functions:**

```
let sum: int = add(3, 4);
let big: int = max(add(1, 2), add(3, 4));
```

Arguments are evaluated left to right. The number and types of arguments must match the function's parameter list exactly.

```
add(1, 2);        // OK
add(1);           // ERROR: expected 2 arguments, got 1
add(1, true);     // ERROR: argument 2 expected 'int', got 'bool'
```

**Recursion:**

```
fn fib(n: int) -> int {
    if (n <= 1) { return n; }
    return fib(n - 1) + fib(n - 2);
}

fn gcd(a: int, b: int) -> int {
    if (b == 0) { return a; }
    return gcd(b, a - (a / b) * b);
}
```

---

## Expressions

### Literals

```
42        // int literal
3.14      // float literal — must contain a decimal point
true      // bool literal
false     // bool literal
"hello"   // string literal
```

Float literals must always include a decimal point. `1` is an `int`; `1.0` is a `float`.

### String escape sequences

| Sequence | Character       |
|----------|-----------------|
| `\\`     | Backslash       |
| `\"`     | Double quote    |
| `\n`     | Newline         |
| `\r`     | Carriage return |
| `\t`     | Tab             |

```
let path: str = "C:\\Users\\name";
let line: str = "first\nsecond";
```

### Arithmetic expressions

Arithmetic operators work on `int` and `float`. Both operands must be the same type.

```
let a: int   = 10 + 3;     // 13
let b: int   = 10 - 3;     // 7
let c: int   = 10 * 3;     // 30
let d: int   = 10 / 3;     // 3  (integer division, truncates)
let e: float = 10.0 / 3.0; // 3.3333...
```

### Parentheses

Use parentheses to group sub-expressions and override default precedence:

```
let a: int = 2 + 3 * 4;    // 14  (multiplication binds tighter)
let b: int = (2 + 3) * 4;  // 20
```

---

## Control Flow

### if / else

```
if (condition) {
    // ...
} else {
    // ...
}
```

- The condition must be a `bool` expression. Other types are not implicitly truthy.
- Braces are required even for single-statement bodies.
- The `else` branch is optional.

```
fn abs(n: int) -> int {
    if (n < 0) {
        return -n;
    } else {
        return n;
    }
}
```

**Chained conditions** use nested `if` inside `else`:

```
fn sign(n: int) -> int {
    if (n < 0) {
        return -1;
    } else {
        if (n > 0) {
            return 1;
        } else {
            return 0;
        }
    }
}
```

### while

```
while (condition) {
    // ...
}
```

- The condition must be `bool`.
- The body executes repeatedly as long as the condition is `true`.
- If the condition is `false` on the first check, the body never runs.

```
fn sum(n: int) -> int {
    let total: int = 0;
    let i: int = 1;
    while (i <= n) {
        total = total + i;
        i = i + 1;
    }
    return total;
}
```

**Infinite loops** can be written by using `true` as the condition. Ensure there is a reachable `return` to exit the function.

---

## Operators

### Arithmetic

| Operator | Description        | Operand types  |
|----------|--------------------|----------------|
| `+`      | Addition           | `int`, `float` |
| `-`      | Subtraction        | `int`, `float` |
| `*`      | Multiplication     | `int`, `float` |
| `/`      | Division           | `int`, `float` |
| `-x`     | Unary negation     | `int`, `float` |

Both operands must have the same type. Integer division truncates toward zero.

### Comparison

All comparison operators return `bool`. Both operands must be the same type.

| Operator | Description           |
|----------|-----------------------|
| `==`     | Equal                 |
| `!=`     | Not equal             |
| `<`      | Less than             |
| `>`      | Greater than          |
| `<=`     | Less than or equal    |
| `>=`     | Greater than or equal |

```
let a: bool = 3 == 3;   // true
let b: bool = 3 != 4;   // true
let c: bool = 3 < 4;    // true
let d: bool = 3 >= 4;   // false
```

### Logical

Logical operators work on `bool` only.

| Operator | Description | Short-circuits                                   |
|----------|-------------|--------------------------------------------------|
| `&&`     | And         | Yes — if left is `false`, right is not evaluated |
| `\|\|`   | Or          | Yes — if left is `true`, right is not evaluated  |
| `!`      | Not         | —                                                |

```
let a: bool = true && false;  // false
let b: bool = true || false;  // true
let c: bool = !true;          // false
```

### Operator precedence

From highest (evaluated first) to lowest:

| Level | Operators               |
|-------|-------------------------|
| 1     | `!`, unary `-`          |
| 2     | `*`, `/`                |
| 3     | `+`, `-`                |
| 4     | `<`, `>`, `<=`, `>=`    |
| 5     | `==`, `!=`              |
| 6     | `&&`                    |
| 7     | `\|\|`                  |
| 8     | `=` (assignment)        |

When in doubt, use parentheses. The compiler treats parenthesised expressions as a single unit regardless of precedence rules.

---

## Built-in Functions

Stilang provides four built-in print functions, one per type. They are available in every program without any import.

Each function prints its argument followed by a newline.

| Function              | Argument type | Output                  |
|-----------------------|---------------|-------------------------|
| `print_int(x)`        | `int`         | Decimal integer         |
| `print_float(x)`      | `float`       | Decimal with 6 digits   |
| `print_bool(x)`       | `bool`        | `true` or `false`       |
| `print_str(x)`        | `str`         | Raw string              |

**Examples:**

```
print_int(42);            // 42
print_int(-7);            // -7
print_float(3.14);        // 3.140000
print_float(1.0 / 3.0);  // 0.333333
print_bool(true);         // true
print_bool(2 > 5);        // false
print_str("hello");       // hello
print_str("a\tb");        // a	b
```

Print calls can appear anywhere a statement is valid — inside functions, loops, and branches:

```
fn printRange(lo: int, hi: int) {
    let i: int = lo;
    while (i <= hi) {
        print_int(i);
        i = i + 1;
    }
}

fn main() -> int {
    printRange(1, 5);
    // prints: 1 2 3 4 5 (each on its own line)
    return 0;
}
```

Passing the wrong type is a compile error:

```
print_int(3.14);    // ERROR: argument 1 expected 'int', got 'float'
print_bool("yes");  // ERROR: argument 1 expected 'bool', got 'str'
```

---

## Compiling

The stilang compiler is distributed as a self-contained jar. It takes a `.stil` source file, produces a `.c` file, and extracts `runtime.c` (which contains the print functions) next to it automatically.

```bash
# Compile stilang source to C
java -jar stilang.jar input.stil output.c
```

Output:
```
Compiled input.stil -> output.c
Run:  gcc output.c runtime.c -o program
```

The compiler prints the exact gcc command to run. Copy and execute it:

```bash
gcc output.c runtime.c -o program
./program
```

`runtime.c` must always be included in the gcc command. It is extracted automatically and will be present in the same directory as `output.c`.

If no output path is given, the generated C is printed to stdout — useful for inspecting what the compiler produces:

```bash
java -jar stilang.jar input.stil
```

**Exit codes:**

| Code | Meaning                              |
|------|--------------------------------------|
| `0`  | Compilation succeeded                |
| `1`  | Error — message printed to stderr    |

**Error messages** include the phase and source line:

```
Syntax error: Line 4: expected ';', got '}'
Name error:   Line 9: undefined name 'counter'
Type error:   Line 7: return type mismatch: expected 'int' but got 'bool'
Type error:   Line 3: operator '+' requires numeric operands, got 'bool'
```

Fix errors from top to bottom — a single mistake early in the file can cause cascading errors on later lines.
