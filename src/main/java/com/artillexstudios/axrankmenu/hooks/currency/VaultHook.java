package com.artillexstudios.axrankmenu.hooks.currency;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

public class VaultHook implements CurrencyHook {
    private Economy econ = null;

    @Override
    public void setup() {
        final RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;

        econ = rsp.getProvider();
    }

    @Override
    public String getName() {
        return "Vault";
    }

    @Override
    public double getBalance(@NotNull Player p) {
        return econ.getBalance(p);
    }

    @Override
    public void giveBalance(@NotNull Player p, double amount) {
        econ.depositPlayer(p, amount);
    }

    @Override
    public void takeBalance(@NotNull Player p, double amount) {
        econ.withdrawPlayer(p, amount);
    }
}