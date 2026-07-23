package com.artillexstudios.axrankmenu.api.requirement;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RequirementContext {
    private final String rankId;
    private final String requirementId;
    private final Map<String, Object> values;
    private final Object attachment;

    public RequirementContext(String rankId, String requirementId, Map<String, Object> values) {
        this(rankId, requirementId, values, null);
    }

    public RequirementContext(String rankId, String requirementId, Map<String, Object> values, Object attachment) {
        this.rankId = rankId;
        this.requirementId = requirementId;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        this.attachment = attachment;
    }

    public String rankId() {
        return rankId;
    }

    public String requirementId() {
        return requirementId;
    }

    public Map<String, Object> values() {
        return values;
    }

    public String string(String key, String fallback) {
        Object value = values.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public double number(String key, double fallback) {
        Object value = values.get(key);
        if (value instanceof Number number) return number.doubleValue();
        if (value == null) return fallback;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public boolean bool(String key, boolean fallback) {
        Object value = values.get(key);
        if (value instanceof Boolean bool) return bool;
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    public List<String> strings(String key) {
        Object value = values.get(key);
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().map(String::valueOf).toList();
    }

    public Object attachment() {
        return attachment;
    }
}
