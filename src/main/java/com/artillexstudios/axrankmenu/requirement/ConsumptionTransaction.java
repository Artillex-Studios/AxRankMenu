package com.artillexstudios.axrankmenu.requirement;

import com.artillexstudios.axrankmenu.api.requirement.ConsumptionResult;
import com.artillexstudios.axrankmenu.api.requirement.RankRequirementProvider;
import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RollbackResult;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConsumptionTransaction {
    private final List<Step> steps = new ArrayList<>();
    private final List<AppliedStep> applied = new ArrayList<>();

    public void add(RankRequirementProvider provider, RequirementContext context) {
        steps.add(new Step(provider, context));
    }

    public ExecutionResult execute(Player player) {
        if (!applied.isEmpty()) throw new IllegalStateException("Transaction was already executed");
        for (Step step : steps) {
            ConsumptionResult result = step.provider().consume(player, step.context());
            if (!result.successful()) {
                List<RollbackResult> rollback = rollback(player);
                return new ExecutionResult(false, step.context(), result, rollback);
            }
            applied.add(new AppliedStep(step.provider(), step.context(), result.rollbackState()));
        }
        return new ExecutionResult(true, null, ConsumptionResult.success(null), List.of());
    }

    public List<RollbackResult> rollback(Player player) {
        List<RollbackResult> results = new ArrayList<>();
        for (int index = applied.size() - 1; index >= 0; index--) {
            AppliedStep step = applied.get(index);
            try {
                results.add(step.provider().rollback(player, step.context(), step.rollbackState()));
            } catch (RuntimeException exception) {
                results.add(RollbackResult.failure(exception.getMessage()));
            }
        }
        applied.clear();
        return Collections.unmodifiableList(results);
    }

    public int appliedCount() {
        return applied.size();
    }

    private record Step(RankRequirementProvider provider, RequirementContext context) {
    }

    private record AppliedStep(RankRequirementProvider provider, RequirementContext context, Object rollbackState) {
    }

    public record ExecutionResult(
            boolean successful,
            RequirementContext failedContext,
            ConsumptionResult consumptionResult,
            List<RollbackResult> rollbackResults
    ) {
    }
}
