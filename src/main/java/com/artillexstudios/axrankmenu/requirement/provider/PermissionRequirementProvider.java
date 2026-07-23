package com.artillexstudios.axrankmenu.requirement.provider;

import com.artillexstudios.axrankmenu.api.requirement.RankRequirementProvider;
import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import org.bukkit.entity.Player;

public final class PermissionRequirementProvider implements RankRequirementProvider {
    @Override
    public String id() {
        return "permission";
    }

    @Override
    public RequirementResult evaluate(Player player, RequirementContext context) {
        String permission = context.string("permission", "");
        boolean expected = context.bool("has", true);
        boolean current = !permission.isBlank() && player.hasPermission(permission);
        boolean met = current == expected;
        return met
                ? RequirementResult.success(Boolean.toString(current), Boolean.toString(expected), "")
                : RequirementResult.failure("requirements.missing-permission", "Permission state did not match", Boolean.toString(current), Boolean.toString(expected), "");
    }
}
