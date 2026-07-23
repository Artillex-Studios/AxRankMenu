package com.artillexstudios.axrankmenu.requirement.provider;

import com.artillexstudios.axrankmenu.api.requirement.ConsumptionResult;
import com.artillexstudios.axrankmenu.api.requirement.RankRequirementProvider;
import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import com.artillexstudios.axrankmenu.api.requirement.RollbackResult;
import com.artillexstudios.axrankmenu.requirement.parse.NumericParser;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class PlaceholderRequirementProvider extends AbstractNumericProvider implements RankRequirementProvider {
    private static final int MAX_REGEX_LENGTH = 256;
    private static final Pattern UNSAFE_REGEX = Pattern.compile("(?:\\\\[1-9]|\\(\\?[=!<]|\\([^)]*(?:\\*|\\+|\\{\\d+(?:,\\d*)?})[^)]*\\)(?:\\*|\\+|\\{\\d+(?:,\\d*)?}))");

    @Override
    public String id() {
        return "placeholder";
    }

    @Override
    public RequirementResult evaluate(Player player, RequirementContext context) {
        String placeholder = context.string("placeholder", "");
        String resolved = resolve(player, placeholder);
        if (resolved == null) {
            return RequirementResult.failure("requirements.placeholder-unreadable", "PlaceholderAPI is unavailable or returned an unresolved/empty value", "", context.string("expected", ""), "");
        }

        String type = context.string("value-type", "string").toLowerCase(Locale.ROOT);
        String operator = context.string("operator", type.equals("number") ? ">=" : "equals");
        String expected = context.string("expected", "");
        boolean successful;
        String remaining = "";
        try {
            successful = switch (type) {
                case "number" -> {
                    var currentNumber = NumericParser.parse(resolved, context.bool("abbreviations", true));
                    var expectedNumber = NumericParser.parse(expected, context.bool("abbreviations", true));
                    if (currentNumber.isEmpty() || expectedNumber.isEmpty()) {
                        throw new IllegalArgumentException("Numeric placeholder value could not be parsed");
                    }
                    BigDecimal current = currentNumber.get();
                    BigDecimal required = expectedNumber.get();
                    remaining = display(required.subtract(current).max(BigDecimal.ZERO));
                    yield numeric(current, required, operator);
                }
                case "boolean" -> bool(resolved, operator);
                default -> text(resolved, expected, operator);
            };
        } catch (RuntimeException exception) {
            return RequirementResult.failure("requirements.placeholder-unreadable", exception.getMessage(), resolved, expected, "");
        }
        return new RequirementResult(
                successful,
                successful ? "" : "requirements.not-met",
                successful ? "" : "Placeholder comparison failed",
                NumericParser.stripFormatting(resolved),
                expected,
                remaining,
                Map.of()
        );
    }

    @Override
    public boolean supportsConsumption() {
        return true;
    }

    @Override
    public ConsumptionResult consume(Player player, RequirementContext context) {
        List<String> actions = context.strings("consume-actions");
        List<String> rollback = context.strings("rollback-actions");
        if (actions.isEmpty() || rollback.isEmpty()) {
            return ConsumptionResult.failure("requirements.consume-failed", "Placeholder consumption requires both consume-actions and rollback-actions");
        }
        for (String action : actions) {
            if (!dispatch(player, action)) {
                return ConsumptionResult.failure("requirements.consume-failed", "A consume action was not accepted by the command dispatcher");
            }
        }
        return ConsumptionResult.success(List.copyOf(rollback));
    }

    @Override
    @SuppressWarnings("unchecked")
    public RollbackResult rollback(Player player, RequirementContext context, Object rollbackState) {
        if (!(rollbackState instanceof List<?> actions)) return RollbackResult.failure("Missing placeholder rollback actions");
        boolean successful = true;
        for (Object action : actions) successful &= dispatch(player, String.valueOf(action));
        return successful ? RollbackResult.success() : RollbackResult.failure("A rollback action was not accepted by the command dispatcher");
    }

    public static String resolve(Player player, String placeholder) {
        if (placeholder == null || placeholder.isBlank() || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return null;
        String resolved = PlaceholderAPI.setPlaceholders(player, placeholder);
        if (resolved == null || resolved.isBlank() || resolved.equals(placeholder)) return null;
        return resolved;
    }

    private static boolean numeric(BigDecimal current, BigDecimal expected, String operator) {
        return switch (operator) {
            case ">" -> current.compareTo(expected) > 0;
            case ">=" -> current.compareTo(expected) >= 0;
            case "<" -> current.compareTo(expected) < 0;
            case "<=" -> current.compareTo(expected) <= 0;
            case "==" -> current.compareTo(expected) == 0;
            case "!=" -> current.compareTo(expected) != 0;
            default -> throw new IllegalArgumentException("Unsupported numeric operator: " + operator);
        };
    }

    private static boolean text(String current, String expected, String operator) {
        String clean = NumericParser.stripFormatting(current);
        return switch (operator.toLowerCase(Locale.ROOT)) {
            case "equals" -> clean.equals(expected);
            case "equals-ignore-case" -> clean.equalsIgnoreCase(expected);
            case "not-equals" -> !clean.equals(expected);
            case "contains" -> clean.contains(expected);
            case "starts-with" -> clean.startsWith(expected);
            case "ends-with" -> clean.endsWith(expected);
            case "matches" -> safeMatches(clean, expected);
            default -> throw new IllegalArgumentException("Unsupported string operator: " + operator);
        };
    }

    private static boolean bool(String value, String operator) {
        boolean current;
        if (value.equalsIgnoreCase("true")) current = true;
        else if (value.equalsIgnoreCase("false")) current = false;
        else throw new IllegalArgumentException("Boolean placeholder value is neither true nor false");
        return switch (operator.toLowerCase(Locale.ROOT)) {
            case "is-true" -> current;
            case "is-false" -> !current;
            default -> throw new IllegalArgumentException("Unsupported boolean operator: " + operator);
        };
    }

    private static boolean safeMatches(String value, String regex) {
        if (regex.length() > MAX_REGEX_LENGTH || value.length() > 2048 || UNSAFE_REGEX.matcher(regex).find()) {
            throw new IllegalArgumentException("Regex is too long or potentially unsafe");
        }
        try {
            return Pattern.compile(regex).matcher(value).matches();
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("Regex is invalid");
        }
    }

    private static boolean dispatch(Player player, String configuredAction) {
        String command = configuredAction.trim();
        if (command.startsWith("[CONSOLE]")) command = command.substring("[CONSOLE]".length()).trim();
        command = command.replace("%player%", player.getName());
        if (command.isBlank() || command.contains("\n") || command.contains("\r")) return false;
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
