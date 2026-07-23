package com.artillexstudios.axrankmenu.requirement;

import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import com.artillexstudios.axrankmenu.api.requirement.RankRequirementProvider;

final class RequirementDefinition {
    private final String id;
    private final String type;
    private final boolean consume;
    private final RequirementContext context;
    private final RankRequirementProvider provider;

    RequirementDefinition(String id, String type, boolean consume, RequirementContext context, RankRequirementProvider provider) {
        this.id = id;
        this.type = type;
        this.consume = consume;
        this.context = context;
        this.provider = provider;
    }

    String id() {
        return id;
    }

    String type() {
        return type;
    }

    boolean consume() {
        return consume;
    }

    RequirementContext context() {
        return context;
    }

    RankRequirementProvider provider() {
        return provider;
    }

    RequirementResult evaluate(org.bukkit.entity.Player player) {
        return provider.evaluate(player, context);
    }
}
