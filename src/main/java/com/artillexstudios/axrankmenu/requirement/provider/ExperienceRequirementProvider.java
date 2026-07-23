package com.artillexstudios.axrankmenu.requirement.provider;

import com.artillexstudios.axrankmenu.api.requirement.ConsumptionResult;
import com.artillexstudios.axrankmenu.api.requirement.RankRequirementProvider;
import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import com.artillexstudios.axrankmenu.api.requirement.RollbackResult;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

public final class ExperienceRequirementProvider extends AbstractNumericProvider implements RankRequirementProvider {
    private final boolean levels;

    public ExperienceRequirementProvider(boolean levels) {
        this.levels = levels;
    }

    @Override
    public String id() {
        return levels ? "minecraft-xp-level" : "minecraft-xp-points";
    }

    @Override
    public RequirementResult evaluate(Player player, RequirementContext context) {
        int current = levels ? player.getLevel() : totalExperience(player);
        return compare(BigDecimal.valueOf(current), context, levels ? "requirements.insufficient-xp-level" : "requirements.insufficient-xp-points");
    }

    @Override
    public boolean supportsConsumption() {
        return true;
    }

    @Override
    public ConsumptionResult consume(Player player, RequirementContext context) {
        int amount = (int) Math.ceil(context.number("amount", 0));
        if (amount < 0) return ConsumptionResult.failure("requirements.consume-failed", "XP amount cannot be negative");
        XpState state = new XpState(player.getLevel(), player.getExp(), player.getTotalExperience());
        if (levels) {
            if (player.getLevel() < amount) return ConsumptionResult.failure("requirements.insufficient-xp-level", "XP level changed");
            player.setLevel(player.getLevel() - amount);
            player.setTotalExperience(totalExperience(player));
        } else {
            int total = totalExperience(player);
            if (total < amount) return ConsumptionResult.failure("requirements.insufficient-xp-points", "XP points changed");
            setTotalExperience(player, total - amount);
        }
        return ConsumptionResult.success(state);
    }

    @Override
    public RollbackResult rollback(Player player, RequirementContext context, Object rollbackState) {
        if (!(rollbackState instanceof XpState state)) return RollbackResult.failure("Missing XP rollback state");
        player.setLevel(state.level());
        player.setExp(state.progress());
        player.setTotalExperience(state.total());
        return RollbackResult.success();
    }

    static int totalExperience(Player player) {
        int level = player.getLevel();
        int base;
        if (level <= 16) {
            base = level * level + 6 * level;
        } else if (level <= 31) {
            base = (int) Math.floor(2.5 * level * level - 40.5 * level + 360);
        } else {
            base = (int) Math.floor(4.5 * level * level - 162.5 * level + 2220);
        }
        int next = level <= 15 ? 2 * level + 7 : level <= 30 ? 5 * level - 38 : 9 * level - 158;
        return Math.max(0, base + Math.round(player.getExp() * next));
    }

    static void setTotalExperience(Player player, int total) {
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);
        if (total > 0) player.giveExp(total);
    }

    private record XpState(int level, float progress, int total) {
    }
}
