package com.stilang.type_checking;

import java.util.List;

public sealed interface Type permits
    Type.Primitive, Type.Function, Type.Void {

    record Primitive(String name) implements Type {
        public static final Primitive INT   = new Primitive("int");
        public static final Primitive FLOAT = new Primitive("float");
        public static final Primitive BOOL  = new Primitive("bool");
        public static final Primitive STR   = new Primitive("str");
    }

    record Function(List<Type> params, Type returnType) implements Type {}

    record Void() implements Type {
        public static final Void INSTANCE = new Void();
    }

    // Convenience
    default boolean isNumeric() {
        return this.equals(Primitive.INT) || this.equals(Primitive.FLOAT);
    }
}
