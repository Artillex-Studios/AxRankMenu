package com.artillexstudios.axrankmenu.api.requirement;

import org.bukkit.entity.Player;

public interface RankRequirementProvider {
    String id();

    RequirementResult evaluate(Player player, RequirementContext context);

    default boolean supportsConsumption() {
        return false;
    }

    default ConsumptionResult consume(Player player, RequirementContext context) {
        return ConsumptionResult.failure("requirements.consume-failed", "Provider does not support consumption");
    }

    default RollbackResult rollback(Player player, RequirementContext context, Object rollbackState) {
        return RollbackResult.failure("Provider does not support rollback");
    }
}
