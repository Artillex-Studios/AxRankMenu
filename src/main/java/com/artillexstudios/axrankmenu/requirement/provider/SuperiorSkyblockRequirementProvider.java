package com.artillexstudios.axrankmenu.requirement.provider;

import com.artillexstudios.axrankmenu.api.requirement.ConsumptionResult;
import com.artillexstudios.axrankmenu.api.requirement.RankRequirementProvider;
import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import com.artillexstudios.axrankmenu.api.requirement.RollbackResult;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.bank.BankTransaction;
import com.bgsoftware.superiorskyblock.api.upgrades.Upgrade;
import com.bgsoftware.superiorskyblock.api.upgrades.UpgradeLevel;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.List;

public final class SuperiorSkyblockRequirementProvider extends AbstractNumericProvider implements RankRequirementProvider {
    @Override
    public String id() {
        return "superior-skyblock";
    }

    @Override
    public RequirementResult evaluate(Player player, RequirementContext context) {
        if (Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2") == null) {
            return RequirementResult.failure("requirements.superior-unavailable", "SuperiorSkyblock2 is not installed", "0", "", "");
        }
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        Island island = superiorPlayer == null ? null : superiorPlayer.getIsland();
        if (island == null) {
            String message = context.bool("fail-if-no-island", false) ? "requirements.no-island" : "requirements.not-met";
            return compare(BigDecimal.ZERO, context, message);
        }
        String metric = context.string("metric", "island-level").toLowerCase();
        BigDecimal value;
        try {
            value = switch (metric) {
                case "island-level" -> island.getIslandLevel();
                case "island-worth" -> island.getWorth();
                case "island-bank" -> island.getIslandBank().getBalance();
                case "island-members" -> BigDecimal.valueOf(island.getIslandMembers(true).size());
                case "island-upgrades", "island-upgrade" -> BigDecimal.valueOf(upgradeLevel(island, context.string("upgrade", "")));
                default -> throw new IllegalArgumentException("Unknown SuperiorSkyblock metric: " + metric);
            };
        } catch (RuntimeException exception) {
            return RequirementResult.failure("requirements.superior-unavailable", exception.getMessage(), "0", "", "");
        }
        return compare(value == null ? BigDecimal.ZERO : value, context, "requirements.insufficient-island");
    }

    @Override
    public boolean supportsConsumption() {
        return true;
    }

    @Override
    public ConsumptionResult consume(Player player, RequirementContext context) {
        if (!context.string("metric", "").equalsIgnoreCase("island-bank")) {
            return ConsumptionResult.failure("requirements.consume-failed", "Only the island-bank metric is consumable");
        }
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        Island island = superiorPlayer == null ? null : superiorPlayer.getIsland();
        BigDecimal amount = BigDecimal.valueOf(context.number("amount", 0));
        if (island == null || amount.signum() < 0 || island.getIslandBank().getBalance().compareTo(amount) < 0) {
            return ConsumptionResult.failure("requirements.consume-failed", "Island or bank balance changed");
        }
        BigDecimal before = island.getIslandBank().getBalance();
        BankTransaction transaction = island.getIslandBank().withdrawMoney(superiorPlayer, amount, List.of());
        BigDecimal after = island.getIslandBank().getBalance();
        boolean successful = transaction != null
                && (transaction.getFailureReason() == null || transaction.getFailureReason().isBlank())
                && after.compareTo(before.subtract(amount)) <= 0;
        return successful
                ? ConsumptionResult.success(new BankState(island.getUniqueId(), amount))
                : ConsumptionResult.failure("requirements.consume-failed", transaction == null ? "No bank transaction returned" : transaction.getFailureReason());
    }

    @Override
    public RollbackResult rollback(Player player, RequirementContext context, Object rollbackState) {
        if (!(rollbackState instanceof BankState state)) return RollbackResult.failure("Missing island bank rollback state");
        Island island = SuperiorSkyblockAPI.getIslandByUUID(state.islandId());
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        if (island == null || superiorPlayer == null || !island.getIslandBank().canDepositMoney(state.amount())) {
            return RollbackResult.failure("Island bank refund is unavailable");
        }
        BankTransaction transaction = island.getIslandBank().depositMoney(superiorPlayer, state.amount());
        return transaction != null && (transaction.getFailureReason() == null || transaction.getFailureReason().isBlank())
                ? RollbackResult.success()
                : RollbackResult.failure(transaction == null ? "No bank refund transaction returned" : transaction.getFailureReason());
    }

    private int upgradeLevel(Island island, String upgradeName) {
        if (upgradeName.isBlank()) throw new IllegalArgumentException("An upgrade name is required");
        Upgrade upgrade = SuperiorSkyblockAPI.getUpgrades().getUpgrade(upgradeName);
        if (upgrade == null) return 0;
        UpgradeLevel level = island.getUpgradeLevel(upgrade);
        return level == null ? 0 : level.getLevel();
    }

    private record BankState(java.util.UUID islandId, BigDecimal amount) {
    }
}
