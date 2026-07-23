package com.artillexstudios.axrankmenu.hooks.currency;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Optional;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;

public final class UltraEconomyHook implements CurrencyHook {
    private Object api;
    private Object currency;
    private Object accounts;

    @Override
    public void setup() {
        try {
            Class<?> ultraEconomy = Class.forName("me.TechsCode.UltraEconomy.UltraEconomy");
            api = ultraEconomy.getMethod("getAPI").invoke(null);
            Object currencies = api.getClass().getMethod("getCurrencies").invoke(api);
            Object currencyResult = currencies.getClass().getMethod("name", String.class)
                    .invoke(currencies, CONFIG.getString("hooks.UltraEconomy.currency-name", "coins"));
            currency = ((Optional<?>) currencyResult).orElseThrow(() -> new IllegalStateException("Currency not found"));
            accounts = api.getClass().getMethod("getAccounts").invoke(api);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unsupported UltraEconomy API", exception);
        }
    }

    @Override
    public String getName() {
        return "UltraEconomy";
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public double getBalance(@NotNull Player player) {
        try {
            Object account = account(player).orElse(null);
            if (account == null) return 0;
            Object balance = compatible(account.getClass(), "getBalance", currency.getClass()).invoke(account, currency);
            return ((Number) balance.getClass().getMethod("getOnHand").invoke(balance)).doubleValue();
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    @Override
    public boolean giveBalance(@NotNull Player player, double amount) {
        return amount >= 0 && mutate(player, "addBalance", amount);
    }

    @Override
    public boolean takeBalance(@NotNull Player player, double amount) {
        return amount >= 0 && getBalance(player) + 1.0E-7 >= amount && mutate(player, "removeBalance", amount);
    }

    private boolean mutate(Player player, String method, double amount) {
        try {
            Object account = account(player).orElse(null);
            if (account == null) return false;
            compatible(account.getClass(), method, currency.getClass(), double.class).invoke(account, currency, amount);
            return true;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private Optional<?> account(Player player) throws ReflectiveOperationException {
        return (Optional<?>) accounts.getClass().getMethod("uuid", java.util.UUID.class).invoke(accounts, player.getUniqueId());
    }

    private Method compatible(Class<?> owner, String name, Class<?>... parameters) throws NoSuchMethodException {
        for (Method method : owner.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != parameters.length) continue;
            boolean matches = true;
            for (int index = 0; index < parameters.length; index++) {
                Class<?> declared = method.getParameterTypes()[index];
                if (!declared.isAssignableFrom(parameters[index]) && !(declared.isPrimitive() && declared == parameters[index])) {
                    matches = false;
                    break;
                }
            }
            if (matches) return method;
        }
        throw new NoSuchMethodException(name);
    }
}
