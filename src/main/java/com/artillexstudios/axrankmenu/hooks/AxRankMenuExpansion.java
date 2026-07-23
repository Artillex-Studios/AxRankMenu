package com.artillexstudios.axrankmenu.hooks;

import com.artillexstudios.axrankmenu.AxRankMenu;
import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import com.artillexstudios.axrankmenu.requirement.RequirementEvaluation;
import com.artillexstudios.axrankmenu.requirement.RequirementService;
import com.artillexstudios.axrankmenu.requirement.RequirementSetEvaluation;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public final class AxRankMenuExpansion extends PlaceholderExpansion {
    private final RequirementService service;

    public AxRankMenuExpansion(RequirementService service) {
        this.service = service;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "axrankmenu";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Artillex Studios";
    }

    @Override
    public @NotNull String getVersion() {
        return AxRankMenu.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (!(offlinePlayer instanceof Player player)) return "";
        if (params.startsWith("requirements_")) return aggregate(player, params.substring("requirements_".length()));
        if (!params.startsWith("requirement_")) return null;
        return individual(player, params.substring("requirement_".length()));
    }

    private String aggregate(Player player, String input) {
        String[] suffixes = {"_met_count", "_total_count", "_all_met"};
        for (String suffix : suffixes) {
            if (!input.endsWith(suffix)) continue;
            String rank = matchRank(input.substring(0, input.length() - suffix.length()));
            if (rank == null) return "";
            RequirementSetEvaluation evaluation = service.cachedOrEvaluate(player, rank);
            return switch (suffix) {
                case "_met_count" -> Long.toString(evaluation.metCount());
                case "_total_count" -> Integer.toString(evaluation.entries().size());
                default -> Boolean.toString(evaluation.successful());
            };
        }
        return "";
    }

    private String individual(Player player, String input) {
        String rank = service.rankIds().stream()
                .filter(candidate -> input.regionMatches(true, 0, candidate + "_", 0, candidate.length() + 1))
                .max(Comparator.comparingInt(String::length))
                .orElse(null);
        if (rank == null) return "";
        String remainder = input.substring(rank.length() + 1);
        String requirement = service.requirementIds(rank).stream()
                .filter(candidate -> remainder.regionMatches(true, 0, candidate + "_", 0, candidate.length() + 1))
                .max(Comparator.comparingInt(String::length))
                .orElse(null);
        if (requirement == null) return "";
        String field = remainder.substring(requirement.length() + 1);
        RequirementEvaluation evaluation = service.result(player, rank, requirement);
        if (evaluation == null) return "";
        RequirementResult result = evaluation.result();
        if (field.startsWith("variable_")) return result.details().getOrDefault(field.substring("variable_".length()), "");
        return switch (field) {
            case "current" -> result.current();
            case "required" -> result.required();
            case "remaining" -> result.remaining();
            case "met", "result" -> Boolean.toString(result.successful());
            case "status" -> result.successful()
                    ? AxRankMenu.LANG.getString("requirements.status.met", "Met")
                    : AxRankMenu.LANG.getString("requirements.status.not-met", "Not met");
            case "expression" -> player.hasPermission("axrankmenu.requirements.debug")
                    ? result.details().getOrDefault("expression", "")
                    : "";
            case "failed_part" -> result.details().getOrDefault("failed-part", "");
            default -> "";
        };
    }

    private String matchRank(String input) {
        return service.rankIds().stream().filter(input::equalsIgnoreCase).findFirst().orElse(null);
    }
}
