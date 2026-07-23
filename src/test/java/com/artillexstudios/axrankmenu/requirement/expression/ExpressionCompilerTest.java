package com.artillexstudios.axrankmenu.requirement.expression;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionCompilerTest {

    @Test
    void appliesComparisonAndLogicalPrecedence() {
        assertTrue(evaluate("1 < 2 || 3 < 2 && false", Map.of(), true));
        assertFalse(evaluate("(1 < 2 || 3 < 2) && false", Map.of(), true));
    }

    @Test
    void supportsNestedParenthesesAndNot() {
        assertTrue(evaluate("!((1 > 2) || (3 <= 2)) && true", Map.of(), true));
    }

    @Test
    void supportsNumericStringAndBooleanFunctions() {
        String expression = "contains(\"skyblock\", \"sky\") && equalsIgnoreCase(\"ADMIN\", \"admin\") && true != false";
        assertTrue(evaluate(expression, Map.of(), true));
    }

    @Test
    void variablesAndDirectPlaceholdersProduceSameResult() {
        Map<String, String> values = Map.of("balance", "50,000", "%vault_eco_balance%", "50,000");
        assertEquals(
                evaluate("balance >= 50000", values, true),
                evaluate("%vault_eco_balance% >= 50000", values, true)
        );
    }

    @Test
    void resolvesQuotedPlaceholderArgumentsAsValues() {
        assertTrue(evaluate("equalsIgnoreCase(\"%luckperms_primary_group_name%\", \"admin\")",
                Map.of("%luckperms_primary_group_name%", "ADMIN"), true));
    }

    @Test
    void strictModeRejectsMissingValues() {
        CompiledExpression expression = ExpressionCompiler.compile("missing >= 0");
        assertThrows(IllegalStateException.class, () -> expression.evaluate((name, placeholder) -> CompiledExpression.ResolvedValue.missing(), true));
        assertTrue(expression.evaluate((name, placeholder) -> CompiledExpression.ResolvedValue.missing(), false));
    }

    @Test
    void malformedTokenReportsExactPosition() {
        ExpressionException exception = assertThrows(ExpressionException.class, () -> ExpressionCompiler.compile("true && @bad"));
        assertEquals(8, exception.position());
    }

    @Test
    void rejectsPotentiallyCatastrophicRegex() {
        CompiledExpression expression = ExpressionCompiler.compile("matches(\"aaaaaaaa\", \"(a+)+\")");
        assertThrows(ExpressionException.class, () -> expression.evaluate((name, placeholder) -> CompiledExpression.ResolvedValue.missing(), true));
    }

    private boolean evaluate(String source, Map<String, String> values, boolean strict) {
        return ExpressionCompiler.compile(source).evaluate((name, placeholder) -> {
            String value = values.get(name);
            return value == null ? CompiledExpression.ResolvedValue.missing() : CompiledExpression.ResolvedValue.of(value);
        }, strict);
    }
}
