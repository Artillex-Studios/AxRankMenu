package com.artillexstudios.axrankmenu.hooks.currency;

import me.mraxetv.beasttokens.api.BeastTokensAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BeastTokensHook implements CurrencyHook {

    @Override
    public void setup() {
    }

    @Override
    public String getName() {
        return "BeastTokens";
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public double getBalance(@NotNull Player p) {
        return BeastTokensAPI.getTokensManager().getTokens(p);
    }

    @Override
    public void giveBalance(@NotNull Player p, double amount) {
        BeastTokensAPI.getTokensManager().addTokens(p, amount);
    }

    @Override
    public void takeBalance(@NotNull Player p, double amount) {
        BeastTokensAPI.getTokensManager().removeTokens(p, amount);
    }
}