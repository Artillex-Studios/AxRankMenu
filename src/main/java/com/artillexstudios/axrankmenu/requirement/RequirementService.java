package com.artillexstudios.axrankmenu.requirement;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axrankmenu.api.requirement.RankRequirementProvider;
import com.artillexstudios.axrankmenu.api.requirement.RankRequirementRegistry;
import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import com.artillexstudios.axrankmenu.requirement.expression.CompiledExpression;
import com.artillexstudios.axrankmenu.requirement.expression.ExpressionCompiler;
import com.artillexstudios.axrankmenu.requirement.expression.ExpressionException;
import com.artillexstudios.axrankmenu.requirement.provider.CurrencyRequirementProvider;
import com.artillexstudios.axrankmenu.requirement.provider.ExperienceRequirementProvider;
import com.artillexstudios.axrankmenu.requirement.provider.ExpressionRequirementProvider;
import com.artillexstudios.axrankmenu.requirement.provider.LuckPermsGroupRequirementProvider;
import com.artillexstudios.axrankmenu.requirement.provider.PermissionRequirementProvider;
import com.artillexstudios.axrankmenu.requirement.provider.PlaceholderRequirementProvider;
import com.artillexstudios.axrankmenu.requirement.provider.StatisticRequirementProvider;
import com.artillexstudios.axrankmenu.requirement.provider.SuperiorSkyblockRequirementProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.artillexstudios.axrankmenu.AxRankMenu.RANKS;

public final class RequirementService implements RankRequirementRegistry {
    private static final long CACHE_MILLIS = 500L;

    private final Map<String, RankRequirementProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, RequirementSet> sets = new ConcurrentHashMap<>();
    private final Map<String, String> groupsToRanks = new ConcurrentHashMap<>();
    private final Map<CacheKey, CachedEvaluation> cache = new ConcurrentHashMap<>();
    private final Set<String> warnings = ConcurrentHashMap.newKeySet();

    public RequirementService() {
        register(new CurrencyRequirementProvider());
        register(new ExperienceRequirementProvider(true));
        register(new ExperienceRequirementProvider(false));
        register(new StatisticRequirementProvider());
        register(new PermissionRequirementProvider());
        register(new LuckPermsGroupRequirementProvider());
        register(new PlaceholderRequirementProvider());
        register(new ExpressionRequirementProvider());
        if (Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2") != null) {
            register(new SuperiorSkyblockRequirementProvider());
        }
    }

    @Override
    public void register(RankRequirementProvider provider) {
        if (provider == null || provider.id() == null || provider.id().isBlank()) {
            throw new IllegalArgumentException("Requirement provider id cannot be empty");
        }
        String id = provider.id().toLowerCase(Locale.ROOT);
        RankRequirementProvider previous = providers.putIfAbsent(id, provider);
        if (previous != null && previous != provider) {
            throw new IllegalStateException("Requirement provider id is already registered: " + id);
        }
        if (!sets.isEmpty()) reload();
    }

    @Override
    public boolean unregister(String id) {
        boolean removed = id != null && providers.remove(id.toLowerCase(Locale.ROOT)) != null;
        if (removed && !sets.isEmpty()) reload();
        return removed;
    }

    @Override
    public RankRequirementProvider provider(String id) {
        return id == null ? null : providers.get(id.toLowerCase(Locale.ROOT));
    }

    @Override
    public Collection<RankRequirementProvider> providers() {
        return Collections.unmodifiableCollection(providers.values());
    }

    public void reload() {
        sets.clear();
        groupsToRanks.clear();
        cache.clear();
        warnings.clear();
        for (String route : RANKS.getBackingDocument().getRoutesAsStrings(false)) {
            Section rank = RANKS.getSection(route);
            if (rank == null || rank.getString("rank", null) == null) continue;
            loadSet(route, rank.getSection("requirements"));
            groupsToRanks.put(normalize(rank.getString("rank")), normalize(route));
        }
    }

    public RequirementSetEvaluation evaluate(Player player, String rankId, boolean fresh) {
        RequirementSet set = sets.get(normalize(rankId));
        if (set == null || set.entries().isEmpty()) return new RequirementSetEvaluation(true, "ALL", List.of());
        CacheKey key = new CacheKey(player.getUniqueId(), normalize(rankId));
        CachedEvaluation cached = cache.get(key);
        long now = System.currentTimeMillis();
        if (!fresh && cached != null && now - cached.createdAt() <= CACHE_MILLIS) return cached.evaluation();

        List<RequirementEvaluation> entries = new ArrayList<>();
        for (RequirementDefinition definition : set.entries()) {
            RequirementResult result;
            try {
                result = definition.evaluate(player);
            } catch (Throwable throwable) {
                result = RequirementResult.failure("requirements.unavailable", throwable.getClass().getSimpleName() + ": " + throwable.getMessage(), "", "", "");
            }
            entries.add(new RequirementEvaluation(definition.id(), definition.type(), definition.consume(), result));
        }
        boolean successful = RequirementPolicy.isSatisfied(set.mode(), entries);
        RequirementSetEvaluation evaluation = new RequirementSetEvaluation(successful, set.mode(), entries);
        if (!fresh) cache.put(key, new CachedEvaluation(now, evaluation));
        return evaluation;
    }

    public List<RequirementDefinition> consumableDefinitions(String rankId, RequirementSetEvaluation evaluation) {
        RequirementSet set = sets.get(normalize(rankId));
        if (set == null || !evaluation.successful()) return List.of();
        if (set.mode().equals("ANY")) {
            for (int index = 0; index < set.entries().size(); index++) {
                if (!evaluation.entries().get(index).result().successful()) continue;
                RequirementDefinition definition = set.entries().get(index);
                return definition.consume() ? List.of(definition) : List.of();
            }
            return List.of();
        }
        List<RequirementDefinition> result = new ArrayList<>();
        for (int index = 0; index < set.entries().size(); index++) {
            RequirementDefinition definition = set.entries().get(index);
            RequirementEvaluation entry = evaluation.entries().get(index);
            if (!definition.consume() || !entry.result().successful()) continue;
            result.add(definition);
        }
        return result;
    }

    public RequirementSetEvaluation cachedOrEvaluate(Player player, String rankId) {
        return evaluate(player, rankId, false);
    }

    public RequirementEvaluation result(Player player, String rankId, String requirementId) {
        return cachedOrEvaluate(player, rankId).entries().stream()
                .filter(entry -> entry.id().equalsIgnoreCase(requirementId))
                .findFirst()
                .orElse(null);
    }

    public void invalidate(UUID playerId) {
        cache.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    public String resolveRankId(String input) {
        if (input == null) return null;
        return sets.keySet().stream().filter(id -> id.equalsIgnoreCase(input)).findFirst().orElse(null);
    }

    public Set<String> rankIds() {
        return Collections.unmodifiableSet(sets.keySet());
    }

    public String rankIdForGroup(String group) {
        return groupsToRanks.getOrDefault(normalize(group), normalize(group));
    }

    public Set<String> requirementIds(String rankId) {
        RequirementSet set = sets.get(normalize(rankId));
        if (set == null) return Set.of();
        Set<String> ids = ConcurrentHashMap.newKeySet();
        set.entries().forEach(entry -> ids.add(entry.id()));
        return ids;
    }

    private void loadSet(String rankId, Section requirements) {
        if (requirements == null) {
            sets.put(normalize(rankId), new RequirementSet("ALL", List.of()));
            return;
        }
        String mode = requirements.getString("mode", "ALL").toUpperCase(Locale.ROOT);
        if (!mode.equals("ALL") && !mode.equals("ANY")) {
            warnOnce(rankId + ":mode", "Rank " + rankId + " has invalid requirements mode '" + mode + "'; ALL is used.");
            mode = "ALL";
        }
        Section entries = requirements.getSection("entries");
        if (entries == null) entries = requirements;
        List<RequirementDefinition> definitions = new ArrayList<>();
        for (String requirementId : entries.getRoutesAsStrings(false)) {
            if (requirementId.equals("mode")) continue;
            Section section = entries.getSection(requirementId);
            if (section == null) continue;
            String type = section.getString("type", "").toLowerCase(Locale.ROOT);
            RankRequirementProvider provider = provider(type);
            if (provider == null) {
                warnOnce(rankId + ":" + requirementId, "Rank " + rankId + " requirement " + requirementId + " uses unavailable provider '" + type + "'.");
                provider = new UnavailableProvider(type);
            }
            Map<String, Object> values = readValues(section);
            Object attachment = null;
            if (type.equals("expression")) {
                try {
                    attachment = ExpressionCompiler.compile(section.getString("expression", ""));
                } catch (ExpressionException exception) {
                    warnOnce(rankId + ":" + requirementId + ":expression",
                            "Invalid expression in rank " + rankId + ", requirement " + requirementId + ": " + exception.getMessage());
                }
            }
            boolean consume = section.getBoolean("consume", false) || !section.getStringList("consume-actions").isEmpty();
            RequirementContext context = new RequirementContext(rankId, requirementId, values, attachment);
            definitions.add(new RequirementDefinition(requirementId, type, consume, context, provider));
        }
        sets.put(normalize(rankId), new RequirementSet(mode, List.copyOf(definitions)));
    }

    private Map<String, Object> readValues(Section section) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : section.getRoutesAsStrings(false)) {
            Section child = section.getSection(key);
            if (child != null) {
                Map<String, String> nested = new LinkedHashMap<>();
                for (String nestedKey : child.getRoutesAsStrings(false)) {
                    nested.put(nestedKey, child.getString(nestedKey, ""));
                }
                values.put(key, nested);
                continue;
            }
            List<String> strings = section.getStringList(key);
            if (!strings.isEmpty()) {
                values.put(key, List.copyOf(strings));
                continue;
            }
            String value = section.getString(key, null);
            if (value != null) values.put(key, value);
        }
        return values;
    }

    private void warnOnce(String key, String message) {
        if (!warnings.add(key)) return;
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FFAA33[AxRankMenu] " + message));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private record RequirementSet(String mode, List<RequirementDefinition> entries) {
    }

    private record CacheKey(UUID playerId, String rankId) {
    }

    private record CachedEvaluation(long createdAt, RequirementSetEvaluation evaluation) {
    }

    private static final class UnavailableProvider implements RankRequirementProvider {
        private final String id;

        private UnavailableProvider(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public RequirementResult evaluate(Player player, RequirementContext context) {
            return RequirementResult.failure("requirements.unavailable", "Requirement provider is unavailable: " + id, "", "", "");
        }
    }
}
