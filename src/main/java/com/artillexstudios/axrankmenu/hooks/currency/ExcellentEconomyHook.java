package com.artillexstudios.axrankmenu.hooks.currency;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;
import su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;

public class ExcellentEconomyHook implements CurrencyHook {
    private ExcellentEconomyAPI api;
    private ExcellentCurrency currency = null;

    @Override
    public void setup() {
        api = Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class).getProvider();
        currency = api.getCurrency(CONFIG.getString("hooks.ExcellentEconomy.currency-name", "coins"));
    }

    @Override
    public String getName() {
        return "ExcellentEconomy";
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public double getBalance(@NotNull Player p) {
        if (currency == null) return 0;
        return api.getBalance(p, currency);
    }

    @Override
    public void giveBalance(@NotNull Player p, double amount) {
        if (currency == null) return;
        api.deposit(p, currency, amount);
    }

    @Override
    public void takeBalance(@NotNull Player p, double amount) {
        if (currency == null) return;
        api.withdraw(p, currency, amount);
    }
}