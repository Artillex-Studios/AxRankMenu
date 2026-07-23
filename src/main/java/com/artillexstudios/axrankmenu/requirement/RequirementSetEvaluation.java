package com.artillexstudios.axrankmenu.requirement;

import java.util.Collections;
import java.util.List;

public record RequirementSetEvaluation(boolean successful, String mode, List<RequirementEvaluation> entries) {
    public RequirementSetEvaluation {
        entries = Collections.unmodifiableList(entries);
    }

    public RequirementEvaluation firstFailure() {
        return entries.stream().filter(entry -> !entry.result().successful()).findFirst().orElse(null);
    }

    public long metCount() {
        return entries.stream().filter(entry -> entry.result().successful()).count();
    }
}
