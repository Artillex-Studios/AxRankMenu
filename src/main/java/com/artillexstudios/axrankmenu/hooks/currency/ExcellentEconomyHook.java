package com.artillexstudios.axrankmenu.hooks.currency;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;

/**
 * Calls ExcellentEconomy's public service API while keeping AxRankMenu compatible
 * with both the Java 21 (2.7) and newer API bytecode lines.
 */
public final class ExcellentEconomyHook implements CurrencyHook {
    private final String currencyName;
    private final String hookName;
    private Object api;
    private Object currency;

    public ExcellentEconomyHook() {
        this(null);
    }

    public ExcellentEconomyHook(String currencyName) {
        this.currencyName = currencyName;
        this.hookName = currencyName == null ? "ExcellentEconomy" : "ExcellentEconomy-" + currencyName;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setup() {
        try {
            Class apiClass = Class.forName("su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI");
            RegisteredServiceProvider<?> registration = Bukkit.getServer().getServicesManager().getRegistration(apiClass);
            if (registration == null) throw new IllegalStateException("ExcellentEconomy service is unavailable");
            api = registration.getProvider();
            String id = currencyName == null ? CONFIG.getString("hooks.ExcellentEconomy.currency-name", "coins") : currencyName;
            currency = api.getClass().getMethod("getCurrency", String.class).invoke(api, id);
            if (currency == null) throw new IllegalStateException("ExcellentEconomy currency not found: " + id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unsupported ExcellentEconomy API", exception);
        }
    }

    @Override
    public String getName() {
        return hookName;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public double getBalance(@NotNull Player player) {
        try {
            Object result = compatible("getBalance", Player.class, currency.getClass()).invoke(api, player, currency);
            return ((Number) result).doubleValue();
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    @Override
    public boolean giveBalance(@NotNull Player player, double amount) {
        return amount >= 0 && mutate("deposit", player, amount);
    }

    @Override
    public boolean takeBalance(@NotNull Player player, double amount) {
        return amount >= 0 && getBalance(player) + 1.0E-7 >= amount && mutate("withdraw", player, amount);
    }

    private boolean mutate(String name, Player player, double amount) {
        try {
            Object result = compatible(name, Player.class, currency.getClass(), double.class).invoke(api, player, currency, amount);
            return !(result instanceof Boolean bool) || bool;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private Method compatible(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        for (Method method : api.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != parameterTypes.length) continue;
            boolean compatible = true;
            for (int index = 0; index < parameterTypes.length; index++) {
                Class<?> declared = method.getParameterTypes()[index];
                Class<?> supplied = parameterTypes[index];
                if (!declared.isAssignableFrom(supplied) && !(declared.isPrimitive() && declared == supplied)) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) return method;
        }
        throw new NoSuchMethodException(name);
    }
}
