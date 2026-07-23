package com.artillexstudios.axrankmenu.api.requirement;

public record RollbackResult(boolean successful, String debugReason) {
    public RollbackResult {
        debugReason = debugReason == null ? "" : debugReason;
    }

    public static RollbackResult success() {
        return new RollbackResult(true, "");
    }

    public static RollbackResult failure(String reason) {
        return new RollbackResult(false, reason);
    }
}
