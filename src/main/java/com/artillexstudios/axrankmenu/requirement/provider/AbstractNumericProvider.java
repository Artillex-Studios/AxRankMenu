package com.artillexstudios.axrankmenu.requirement.provider;

import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import com.artillexstudios.axrankmenu.requirement.parse.NumericParser;

import java.math.BigDecimal;

abstract class AbstractNumericProvider {

    protected RequirementResult compare(BigDecimal current, RequirementContext context, String failureMessage) {
        BigDecimal required = BigDecimal.valueOf(context.number("amount", context.number("expected", 0)));
        String operator = context.string("operator", ">=");
        boolean successful = switch (operator) {
            case ">" -> current.compareTo(required) > 0;
            case ">=" -> current.compareTo(required) >= 0;
            case "<" -> current.compareTo(required) < 0;
            case "<=" -> current.compareTo(required) <= 0;
            case "==" -> current.compareTo(required) == 0;
            case "!=" -> current.compareTo(required) != 0;
            default -> false;
        };
        String currentText = display(current);
        String requiredText = display(required);
        String remaining = display(required.subtract(current).max(BigDecimal.ZERO));
        return successful
                ? RequirementResult.success(currentText, requiredText, remaining)
                : RequirementResult.failure(failureMessage, "Comparison failed: " + currentText + " " + operator + " " + requiredText, currentText, requiredText, remaining);
    }

    protected String display(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
