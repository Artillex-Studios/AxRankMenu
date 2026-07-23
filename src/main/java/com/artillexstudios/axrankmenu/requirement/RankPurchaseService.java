package com.artillexstudios.axrankmenu.requirement;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axrankmenu.AxRankMenu;
import com.artillexstudios.axrankmenu.api.requirement.ConsumptionResult;
import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RollbackResult;
import com.artillexstudios.axrankmenu.requirement.provider.CurrencyRequirementProvider;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.NodeType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.artillexstudios.axrankmenu.AxRankMenu.MESSAGEUTILS;

public final class RankPurchaseService {
    private static final Set<UUID> ACTIVE_PURCHASES = ConcurrentHashMap.newKeySet();

    private final RequirementService requirements;
    private final LuckPerms luckPerms;

    public RankPurchaseService(RequirementService requirements, LuckPerms luckPerms) {
        this.requirements = requirements;
        this.luckPerms = luckPerms;
    }

    public void purchase(Player player, String rankId, Section rankSection, Group targetGroup, double legacyPrice, String legacyCurrency, Runnable successActions) {
        UUID playerId = player.getUniqueId();
        if (!ACTIVE_PURCHASES.add(playerId)) {
            MESSAGEUTILS.sendLang(player, "requirements.transaction-in-progress");
            return;
        }

        ConsumptionTransaction transaction = new ConsumptionTransaction();
        try {
            RequirementSetEvaluation evaluation = requirements.evaluate(player, rankId, true);
            if (!evaluation.successful()) {
                RequirementEvaluation failure = evaluation.firstFailure();
                sendFailure(player, rankId, failure);
                ACTIVE_PURCHASES.remove(playerId);
                return;
            }

            List<PendingConsumption> pending = new ArrayList<>();
            if (legacyPrice >= 0) {
                RequirementContext legacyContext = new RequirementContext(rankId, "legacy-price", Map.of(
                        "currency", legacyCurrency,
                        "amount", legacyPrice,
                        "operator", ">="
                ));
                CurrencyRequirementProvider currencyProvider = new CurrencyRequirementProvider();
                var priceResult = currencyProvider.evaluate(player, legacyContext);
                if (!priceResult.successful()) {
                    MESSAGEUTILS.sendLang(player, "buy.no-currency");
                    ACTIVE_PURCHASES.remove(playerId);
                    return;
                }
                if (legacyPrice > 0) pending.add(new PendingConsumption(currencyProvider, legacyContext));
            }

            for (RequirementDefinition definition : requirements.consumableDefinitions(rankId, evaluation)) {
                if (!definition.provider().supportsConsumption()) {
                    MESSAGEUTILS.sendLang(player, "requirements.consume-unsupported", replacements(rankId, definition.id(), definition.type()));
                    ACTIVE_PURCHASES.remove(playerId);
                    return;
                }
                pending.add(new PendingConsumption(definition.provider(), definition.context()));
            }

            for (PendingConsumption consumption : pending) {
                transaction.add(consumption.provider(), consumption.context());
            }
            ConsumptionTransaction.ExecutionResult transactionResult = transaction.execute(player);
            if (!transactionResult.successful()) {
                ConsumptionResult result = transactionResult.consumptionResult();
                logRollbackFailures(player, transactionResult.rollbackResults());
                MESSAGEUTILS.sendLang(player, result.userMessage().isBlank() ? "requirements.consume-failed" : result.userMessage(),
                        replacements(rankId, transactionResult.failedContext().requirementId(), "transaction"));
                ACTIVE_PURCHASES.remove(playerId);
                return;
            }

            User user = luckPerms.getUserManager().getUser(playerId);
            if (user == null) {
                rollback(player, transaction);
                MESSAGEUTILS.sendLang(player, "requirements.rank-grant-failed");
                ACTIVE_PURCHASES.remove(playerId);
                return;
            }
            String server = rankSection.getString("server", "");
            List<InheritanceNode> previousNodes = new ArrayList<>(user.getNodes(NodeType.INHERITANCE));
            applyTargetGroup(user, targetGroup, server);
            luckPerms.getUserManager().saveUser(user).whenComplete((ignored, throwable) ->
                    Scheduler.get().execute(() -> {
                        try {
                            if (throwable != null) {
                                restoreGroups(user, previousNodes);
                                luckPerms.getUserManager().saveUser(user);
                                rollback(player, transaction);
                                MESSAGEUTILS.sendLang(player, "requirements.rank-grant-rolled-back");
                                return;
                            }
                            requirements.invalidate(playerId);
                            successActions.run();
                        } catch (Throwable completionError) {
                            restoreGroups(user, previousNodes);
                            luckPerms.getUserManager().saveUser(user);
                            rollback(player, transaction);
                            MESSAGEUTILS.sendLang(player, "requirements.rank-grant-rolled-back");
                            Bukkit.getLogger().severe("[AxRankMenu] Failed to complete purchase for " + player.getName() + ": " + completionError.getMessage());
                        } finally {
                            ACTIVE_PURCHASES.remove(playerId);
                        }
                    })
            );
        } catch (Throwable throwable) {
            rollback(player, transaction);
            ACTIVE_PURCHASES.remove(playerId);
            MESSAGEUTILS.sendLang(player, "requirements.consume-failed");
            Bukkit.getLogger().severe("[AxRankMenu] Purchase failed safely for " + player.getName() + ": " + throwable.getMessage());
        }
    }

    private void applyTargetGroup(User user, Group targetGroup, String server) {
        ImmutableContextSet targetContext = server.isBlank() ? ImmutableContextSet.empty() : ImmutableContextSet.of("server", server);
        for (InheritanceNode node : new ArrayList<>(user.getNodes(NodeType.INHERITANCE))) {
            if (server.isBlank() ? node.getContexts().isEmpty() : node.getContexts().equals(targetContext)) {
                user.data().remove(node);
            }
        }
        InheritanceNode.Builder builder = InheritanceNode.builder(targetGroup);
        if (!server.isBlank()) builder.withContext("server", server);
        user.data().add(builder.build());
    }

    private void restoreGroups(User user, List<InheritanceNode> previousNodes) {
        for (InheritanceNode node : new ArrayList<>(user.getNodes(NodeType.INHERITANCE))) user.data().remove(node);
        previousNodes.forEach(node -> user.data().add(node));
    }

    private void rollback(Player player, ConsumptionTransaction transaction) {
        logRollbackFailures(player, transaction.rollback(player));
        requirements.invalidate(player.getUniqueId());
    }

    private void logRollbackFailures(Player player, List<RollbackResult> results) {
        results.stream().filter(result -> !result.successful()).forEach(result ->
                Bukkit.getLogger().severe("[AxRankMenu] Rollback failed for " + player.getName() + ": " + result.debugReason()));
    }

    private void sendFailure(Player player, String rankId, RequirementEvaluation failure) {
        if (failure == null) {
            MESSAGEUTILS.sendLang(player, "requirements.not-met");
            return;
        }
        String path = failure.result().userMessage().isBlank() ? "requirements.not-met" : failure.result().userMessage();
        Map<String, String> replacements = replacements(rankId, failure.id(), failure.type());
        replacements.put("%current%", failure.result().current());
        replacements.put("%required%", failure.result().required());
        replacements.put("%remaining%", failure.result().remaining());
        MESSAGEUTILS.sendLang(player, path, replacements);
    }

    private Map<String, String> replacements(String rankId, String requirementId, String type) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("%rank%", rankId);
        replacements.put("%requirement%", requirementId);
        replacements.put("%type%", type);
        return replacements;
    }

    private record PendingConsumption(
            com.artillexstudios.axrankmenu.api.requirement.RankRequirementProvider provider,
            RequirementContext context
    ) {
    }

}
