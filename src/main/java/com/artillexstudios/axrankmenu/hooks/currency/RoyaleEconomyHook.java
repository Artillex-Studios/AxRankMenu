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
    public boolean isPersistent() {
        return false;
    }

    @Override
    public double getBalance(@NotNull Player p) {
        return RoyaleEconomy.apiHandler.balance.getBalance(p.getUniqueId().toString());
    }

    @Override
    public boolean giveBalance(@NotNull Player p, double amount) {
        if (amount < 0) return false;
        RoyaleEconomy.apiHandler.balance.addBalance(p.getUniqueId().toString(), amount);
        return true;
    }

    @Override
    public boolean takeBalance(@NotNull Player p, double amount) {
        if (amount < 0 || getBalance(p) + 1.0E-7 < amount) return false;
        RoyaleEconomy.apiHandler.balance.removeBalance(p.getUniqueId().toString(), amount);
        return true;
    }
}
