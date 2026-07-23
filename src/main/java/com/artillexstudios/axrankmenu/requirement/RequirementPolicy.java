package com.artillexstudios.axrankmenu.requirement;

import java.util.List;

public final class RequirementPolicy {
    private RequirementPolicy() {
    }

    public static boolean isSatisfied(String mode, List<RequirementEvaluation> entries) {
        return "ANY".equalsIgnoreCase(mode)
                ? entries.stream().anyMatch(entry -> entry.result().successful())
                : entries.stream().allMatch(entry -> entry.result().successful());
    }
}
