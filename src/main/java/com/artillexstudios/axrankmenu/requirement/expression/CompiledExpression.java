package com.artillexstudios.axrankmenu.requirement.expression;

import java.util.Map;

public interface CompiledExpression {
    boolean evaluate(ValueResolver resolver, boolean strict);

    String source();

    interface ValueResolver {
        ResolvedValue resolve(String name, boolean placeholder);
    }

    record ResolvedValue(boolean present, String value) {
        public static ResolvedValue missing() {
            return new ResolvedValue(false, "");
        }

        public static ResolvedValue of(String value) {
            return new ResolvedValue(value != null, value == null ? "" : value);
        }
    }
}
