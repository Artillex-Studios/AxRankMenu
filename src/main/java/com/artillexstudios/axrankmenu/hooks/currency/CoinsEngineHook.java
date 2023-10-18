package com.artillexstudios.axrankmenu.hooks.currency;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;

public class CoinsEngineHook implements CurrencyHook {
    private Currency currency = null;

    @Override
    public void setup() {
        currency = CoinsEngineAPI.getCurrency(CONFIG.getString("hook-settings.CoinsEngine.currency-name", "coins"));
    }

    @Override
    public double getBalance(@NotNull Player p) {
        if (currency == null) return 0;
        return CoinsEngineAPI.getBalance(p, currency);
    }

    @Override
    public void giveBalance(@NotNull Player p, double amount) {
        if (currency == null) return;
        CoinsEngineAPI.addBalance(p, currency, amount);
    }

    @Override
    public void takeBalance(@NotNull Player p, double amount) {
        if (currency == null) return;
        CoinsEngineAPI.removeBalance(p, currency, amount);
    }
}