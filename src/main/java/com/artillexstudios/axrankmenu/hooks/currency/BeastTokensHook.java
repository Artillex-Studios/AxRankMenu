package com.artillexstudios.axrankmenu.hooks.currency;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * BeastTokens does not publish a stable Maven API. Reflection is isolated here so
 * the optional plugin can evolve or be absent without preventing AxRankMenu from loading.
 */
public final class BeastTokensHook implements CurrencyHook {
    private Object manager;
    private Method getTokens;
    private Method addTokens;
    private Method removeTokens;

    @Override
    public void setup() {
        try {
            Class<?> api = Class.forName("me.mraxetv.beasttokens.api.BeastTokensAPI");
            manager = api.getMethod("getTokensManager").invoke(null);
            getTokens = find(manager.getClass(), "getTokens", Player.class);
            addTokens = find(manager.getClass(), "addTokens", Player.class, double.class);
            removeTokens = find(manager.getClass(), "removeTokens", Player.class, double.class);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unsupported BeastTokens API", exception);
        }
    }

    @Override
    public String getName() {
        return "BeastTokens";
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public double getBalance(@NotNull Player player) {
        try {
            return ((Number) getTokens.invoke(manager, player)).doubleValue();
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    @Override
    public boolean giveBalance(@NotNull Player player, double amount) {
        return amount >= 0 && invoke(addTokens, player, amount);
    }

    @Override
    public boolean takeBalance(@NotNull Player player, double amount) {
        return amount >= 0 && getBalance(player) + 1.0E-7 >= amount && invoke(removeTokens, player, amount);
    }

    private boolean invoke(Method method, Player player, double amount) {
        try {
            Object numericAmount = method.getParameterTypes()[1] == int.class ? (int) Math.round(amount) : amount;
            method.invoke(manager, player, numericAmount);
            return true;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private Method find(Class<?> type, String name, Class<?>... parameters) throws NoSuchMethodException {
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException exception) {
            if (parameters.length == 2 && parameters[1] == double.class) {
                return type.getMethod(name, parameters[0], int.class);
            }
            throw exception;
        }
    }
}
