package com.artillexstudios.axrankmenu.hooks.currency;

import org.bukkit.entity.Player;

public interface CurrencyHook {
    void setup();

    String getName();

    boolean isPersistent();

    double getBalance(Player p);

    boolean giveBalance(Player p, double amount);

    boolean takeBalance(Player p, double amount);
}
