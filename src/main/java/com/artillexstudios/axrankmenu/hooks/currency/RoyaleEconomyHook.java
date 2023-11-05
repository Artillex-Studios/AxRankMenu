package com.artillexstudios.axrankmenu.hooks.currency;

import me.qKing12.RoyaleEconomy.RoyaleEconomy;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RoyaleEconomyHook implements CurrencyHook {

    @Override
    public void setup() {
    }

    @Override
    public String getName() {
        return "RoyaleEconomy";
    }

    @Override
    public double getBalance(@NotNull Player p) {
        return RoyaleEconomy.apiHandler.balance.getBalance(p.getUniqueId().toString());
    }

    @Override
    public void giveBalance(@NotNull Player p, double amount) {
        RoyaleEconomy.apiHandler.balance.addBalance(p.getUniqueId().toString(), amount);
    }

    @Override
    public void takeBalance(@NotNull Player p, double amount) {
        RoyaleEconomy.apiHandler.balance.removeBalance(p.getUniqueId().toString(), amount);
    }
}