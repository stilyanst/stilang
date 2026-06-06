#include <stdio.h>

void print_int(int x) {
    printf("%d\n", x);
}

void print_float(double x) {
    printf("%f\n", x);
}

void print_bool(int x) {
    printf("%s\n", x ? "true" : "false");
}

void print_str(char* x) {
    printf("%s\n", x);
}
