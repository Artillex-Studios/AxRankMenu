package com.artillexstudios.axrankmenu.requirement.parse;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class NumericParser {
    private static final Pattern LEGACY_COLOR = Pattern.compile("(?i)[&§][0-9A-FK-ORX]");
    private static final Pattern HEX_COLOR = Pattern.compile("(?i)(?:[&§]#[0-9A-F]{6}|<#[0-9A-F]{6}>|<[^>]{1,64}>)");
    private static final Pattern ALLOWED = Pattern.compile("[+-]?\\d+(?:\\.\\d+)?");

    private NumericParser() {
    }

    public static Optional<BigDecimal> parse(String input, boolean abbreviations) {
        if (input == null) return Optional.empty();
        String value = HEX_COLOR.matcher(LEGACY_COLOR.matcher(input).replaceAll("")).replaceAll("").trim();
        if (value.isEmpty()) return Optional.empty();

        BigDecimal multiplier = BigDecimal.ONE;
        char suffix = Character.toUpperCase(value.charAt(value.length() - 1));
        if (abbreviations && (suffix == 'K' || suffix == 'M' || suffix == 'B' || suffix == 'T')) {
            value = value.substring(0, value.length() - 1).trim();
            multiplier = switch (suffix) {
                case 'K' -> BigDecimal.valueOf(1_000L);
                case 'M' -> BigDecimal.valueOf(1_000_000L);
                case 'B' -> BigDecimal.valueOf(1_000_000_000L);
                default -> BigDecimal.valueOf(1_000_000_000_000L);
            };
        }

        value = normalizeSeparators(value);
        if (!ALLOWED.matcher(value).matches()) return Optional.empty();
        try {
            return Optional.of(new BigDecimal(value).multiply(multiplier));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static String stripFormatting(String input) {
        if (input == null) return "";
        return HEX_COLOR.matcher(LEGACY_COLOR.matcher(input).replaceAll("")).replaceAll("").trim();
    }

    private static String normalizeSeparators(String input) {
        String value = input.replace(" ", "").replace("_", "").replace("'", "");
        int comma = value.lastIndexOf(',');
        int dot = value.lastIndexOf('.');
        if (comma >= 0 && dot >= 0) {
            char decimal = comma > dot ? ',' : '.';
            char grouping = decimal == ',' ? '.' : ',';
            return value.replace(String.valueOf(grouping), "").replace(decimal, '.');
        }
        if (comma >= 0) {
            if (isGrouping(value, ',')) return value.replace(",", "");
            return value.replace(',', '.');
        }
        if (dot >= 0 && isGrouping(value, '.')) return value.replace(".", "");
        return value;
    }

    private static boolean isGrouping(String value, char separator) {
        String unsigned = value.toLowerCase(Locale.ROOT).replaceFirst("^[+-]", "");
        String[] groups = unsigned.split(Pattern.quote(String.valueOf(separator)), -1);
        if (groups.length <= 1 || groups[0].isEmpty() || groups[0].length() > 3) return false;
        for (int i = 1; i < groups.length; i++) {
            if (groups[i].length() != 3) return false;
        }
        return true;
    }
}
