package com.artillexstudios.axrankmenu.requirement.provider;

import com.artillexstudios.axrankmenu.api.requirement.RankRequirementProvider;
import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import com.artillexstudios.axrankmenu.requirement.expression.CompiledExpression;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ExpressionRequirementProvider implements RankRequirementProvider {
    @Override
    public String id() {
        return "expression";
    }

    @Override
    @SuppressWarnings("unchecked")
    public RequirementResult evaluate(Player player, RequirementContext context) {
        if (!(context.attachment() instanceof CompiledExpression expression)) {
            return RequirementResult.failure("requirements.invalid-expression", "Expression was not compiled", "", "", "");
        }
        Map<String, String> variablePlaceholders = context.values().get("variables") instanceof Map<?, ?> map
                ? (Map<String, String>) map
                : Map.of();
        Map<String, String> resolvedVariables = new LinkedHashMap<>();
        try {
            boolean successful = expression.evaluate((name, placeholder) -> {
                String token = placeholder ? name : variablePlaceholders.get(name);
                if (token == null) return CompiledExpression.ResolvedValue.missing();
                String resolved = PlaceholderRequirementProvider.resolve(player, token);
                if (resolved == null) return CompiledExpression.ResolvedValue.missing();
                if (!placeholder) resolvedVariables.put(name, resolved);
                return CompiledExpression.ResolvedValue.of(resolved);
            }, context.bool("strict", true));
            Map<String, String> details = new LinkedHashMap<>(resolvedVariables);
            details.put("expression", expression.source());
            details.put("failed-part", successful ? "" : context.string("failed-part", context.requirementId()));
            return new RequirementResult(
                    successful,
                    successful ? "" : "requirements.not-met",
                    successful ? "" : "Expression evaluated to false",
                    Boolean.toString(successful),
                    "true",
                    "",
                    details
            );
        } catch (RuntimeException exception) {
            Map<String, String> details = new LinkedHashMap<>(resolvedVariables);
            details.put("expression", expression.source());
            details.put("failed-part", context.string("failed-part", context.requirementId()));
            return RequirementResult.failure("requirements.placeholder-unreadable", exception.getMessage(), "false", "true", "").withDetails(details);
        }
    }
}
