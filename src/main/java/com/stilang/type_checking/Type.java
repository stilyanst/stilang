package com.stilang.type_checking;

import java.util.LinkedHashMap;
import java.util.List;

public sealed interface Type permits
    Type.Primitive, Type.Function, Type.Void, Type.Array, Type.Struct {

    record Primitive(String name) implements Type {
        public static final Primitive INT   = new Primitive("int");
        public static final Primitive FLOAT = new Primitive("float");
        public static final Primitive BOOL  = new Primitive("bool");
        public static final Primitive STR   = new Primitive("str");

        @Override
        public String toString() {
            return name;
        }
    }

    record Function(List<Type> params, Type returnType) implements Type {}

    record Void() implements Type {
        public static final Void INSTANCE = new Void();
    }

    record Array(Type elementType) implements Type {
        @Override
        public String toString() {
            return elementType + "[]";
        }
    }

    /**
     * A user-defined struct type. Typing is nominal: two struct types are equal
     * when their names match, so equality does not recurse into the field map
     * (which would otherwise loop on self-referential structs and break the
     * `expect(actual, expected)` checks). Fields preserve declaration order.
     */
    record Struct(String name, LinkedHashMap<String, Type> fields) implements Type {
        @Override
        public boolean equals(Object o) {
            return o instanceof Struct s && s.name.equals(this.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // Convenience
    default boolean isNumeric() {
        return this.equals(Primitive.INT) || this.equals(Primitive.FLOAT);
    }
}
