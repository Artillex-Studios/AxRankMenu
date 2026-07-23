package com.artillexstudios.axrankmenu.hooks.currency;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;

public class CoinsEngineHook implements CurrencyHook {
    private final String currencyName;
    private final String hookName;
    private Currency currency = null;

    public CoinsEngineHook() {
        this(null);
    }

    public CoinsEngineHook(String currencyName) {
        this.currencyName = currencyName;
        this.hookName = currencyName == null ? "CoinsEngine" : "CoinsEngine-" + currencyName;
    }

    @Override
    public void setup() {
        currency = CoinsEngineAPI.getCurrency(currencyName == null ? CONFIG.getString("hooks.CoinsEngine.currency-name", "coins") : currencyName);
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
    public double getBalance(@NotNull Player p) {
        if (currency == null) return 0;
        return CoinsEngineAPI.getBalance(p, currency);
    }

    @Override
    public boolean giveBalance(@NotNull Player p, double amount) {
        if (currency == null || amount < 0) return false;
        CoinsEngineAPI.addBalance(p, currency, amount);
        return true;
    }

    @Override
    public boolean takeBalance(@NotNull Player p, double amount) {
        if (currency == null || amount < 0 || getBalance(p) + 1.0E-7 < amount) return false;
        CoinsEngineAPI.removeBalance(p, currency, amount);
        return true;
    }
}
