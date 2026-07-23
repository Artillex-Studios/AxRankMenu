package com.artillexstudios.axrankmenu.requirement.provider;

import com.artillexstudios.axrankmenu.api.requirement.RankRequirementProvider;
import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class StatisticRequirementProvider extends AbstractNumericProvider implements RankRequirementProvider {
    @Override
    public String id() {
        return "statistic";
    }

    @Override
    public RequirementResult evaluate(Player player, RequirementContext context) {
        String statisticName = context.string("statistic", "PLAY_ONE_MINUTE");
        if (!statisticName.equalsIgnoreCase("PLAY_ONE_MINUTE") && !statisticName.equalsIgnoreCase("PLAY_ONE_TICK")) {
            return RequirementResult.failure("requirements.invalid-statistic", "Only PLAY_ONE_MINUTE is supported", "0", "", "");
        }
        long ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        BigDecimal divisor = switch (context.string("unit", "ticks").toLowerCase()) {
            case "minutes" -> BigDecimal.valueOf(20L * 60L);
            case "hours" -> BigDecimal.valueOf(20L * 60L * 60L);
            case "days" -> BigDecimal.valueOf(20L * 60L * 60L * 24L);
            default -> BigDecimal.ONE;
        };
        BigDecimal current = BigDecimal.valueOf(ticks).divide(divisor, 6, RoundingMode.DOWN);
        return compare(current, context, "requirements.insufficient-playtime");
    }
}
