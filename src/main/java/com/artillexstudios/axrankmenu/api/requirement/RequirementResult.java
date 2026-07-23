package com.artillexstudios.axrankmenu.api.requirement;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record RequirementResult(
        boolean successful,
        String userMessage,
        String debugReason,
        String current,
        String required,
        String remaining,
        Map<String, String> details
) {
    public RequirementResult {
        userMessage = userMessage == null ? "" : userMessage;
        debugReason = debugReason == null ? "" : debugReason;
        current = current == null ? "" : current;
        required = required == null ? "" : required;
        remaining = remaining == null ? "" : remaining;
        details = details == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public static RequirementResult success(String current, String required, String remaining) {
        return new RequirementResult(true, "", "", current, required, remaining, Map.of());
    }

    public static RequirementResult failure(String message, String reason, String current, String required, String remaining) {
        return new RequirementResult(false, message, reason, current, required, remaining, Map.of());
    }

    public RequirementResult withDetails(Map<String, String> values) {
        return new RequirementResult(successful, userMessage, debugReason, current, required, remaining, values);
    }
}
