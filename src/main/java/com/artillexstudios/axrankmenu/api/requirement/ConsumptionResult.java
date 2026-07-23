package com.artillexstudios.axrankmenu.api.requirement;

public record ConsumptionResult(boolean successful, String userMessage, String debugReason, Object rollbackState) {
    public ConsumptionResult {
        userMessage = userMessage == null ? "" : userMessage;
        debugReason = debugReason == null ? "" : debugReason;
    }

    public static ConsumptionResult success(Object rollbackState) {
        return new ConsumptionResult(true, "", "", rollbackState);
    }

    public static ConsumptionResult failure(String message, String reason) {
        return new ConsumptionResult(false, message, reason, null);
    }
}
