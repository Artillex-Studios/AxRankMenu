package com.artillexstudios.axrankmenu.hooks;

import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axrankmenu.hooks.currency.CoinsEngineHook;
import com.artillexstudios.axrankmenu.hooks.currency.CurrencyHook;
import com.artillexstudios.axrankmenu.hooks.currency.PlayerPointsHook;
import com.artillexstudios.axrankmenu.hooks.currency.RoyaleEconomyHook;
import com.artillexstudios.axrankmenu.hooks.currency.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;

public class HookManager {
    private static CurrencyHook currency = null;

    public void updateHooks() {
        final String eco = CONFIG.getString("hooks.economy-plugin").toUpperCase();
        switch (eco) {
            case "VAULT" -> {
                if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                    currency = new VaultHook();
                    Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into Vault!"));
                } else {
                    Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF3333[AxRankMenu] Vault is set in config.yml, but it isn't installed, please download it or change it in the config to stop errors!"));
                }
            }

            case "PLAYERPOINTS" -> {
                if (Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
                    currency = new PlayerPointsHook();
                    Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into PlayerPoints!"));
                } else {
                    Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF3333[AxRankMenu] PlayerPoints is set in config.yml, but it isn't installed, please download it or change it in the config to stop errors!"));
                }
            }

            case "COINSENGINE" -> {
                if (Bukkit.getPluginManager().getPlugin("CoinsEngine") != null) {
                    currency = new CoinsEngineHook();
                    Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into CoinsEngine!"));
                } else {
                    Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF3333[AxRankMenu] CoinsEngine is set in config.yml, but it isn't installed, please download it or change it in the config to stop errors!"));
                }
            }

            case "ROYALEECONOMY" -> {
                if (Bukkit.getPluginManager().getPlugin("RoyaleEconomy") != null) {
                    currency = new RoyaleEconomyHook();
                    Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into RoyaleEconomy!"));
                } else {
                    Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF3333[AxRankMenu] RoyaleEconomy is set in config.yml, but it isn't installed, please download it or change it in the config to stop errors!"));
                }
            }
        }
        if (currency != null)
            currency.setup();

        if (getCurrency() == null) {
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF3333[AxRankMenu] Currency hook not found! Please check your config.yml!"));
        }
    }
    
    @SuppressWarnings("unused")
    public static void registerCurrencyHook(@NotNull Plugin plugin, @NotNull CurrencyHook currencyHook) {
        currency = currencyHook;
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into " + plugin.getName() + "! Note: You must set the currency provider to CUSTOM or it will be overridden after reloading!"));
    }

    @Nullable
    public static CurrencyHook getCurrency() {
        return currency;
    }
}
