package com.artillexstudios.axrankmenu.hooks.currency;

import org.bukkit.entity.Player;

public interface CurrencyHook {
    void setup();

    String getName();

    double getBalance(Player p);

    void giveBalance(Player p, double amount);

    void takeBalance(Player p, double amount);
}
