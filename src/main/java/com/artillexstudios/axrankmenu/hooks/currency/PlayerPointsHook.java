package com.artillexstudios.axrankmenu.hooks.currency;

import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerPointsHook implements CurrencyHook {
    private PlayerPointsAPI econ = null;

    @Override
    public void setup() {
        econ = org.black_ixx.playerpoints.PlayerPoints.getInstance().getAPI();
    }

    @Override
    public String getName() {
        return "PlayerPoints";
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public double getBalance(@NotNull Player p) {
        return econ.look(p.getUniqueId());
    }

    @Override
    public void giveBalance(@NotNull Player p, double amount) {
        econ.give(p.getUniqueId(), (int) amount);
    }

    @Override
    public void takeBalance(@NotNull Player p, double amount) {
        econ.take(p.getUniqueId(), (int) Math.round(amount));
    }
}