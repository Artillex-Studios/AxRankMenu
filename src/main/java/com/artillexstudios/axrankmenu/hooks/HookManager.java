package com.artillexstudios.axrankmenu.hooks;

import com.artillexstudios.axapi.reflection.ClassUtils;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axrankmenu.hooks.currency.BeastTokensHook;
import com.artillexstudios.axrankmenu.hooks.currency.CoinsEngineHook;
import com.artillexstudios.axrankmenu.hooks.currency.CurrencyHook;
import com.artillexstudios.axrankmenu.hooks.currency.ExcellentEconomyHook;
import com.artillexstudios.axrankmenu.hooks.currency.PlayerPointsHook;
import com.artillexstudios.axrankmenu.hooks.currency.RoyaleEconomyHook;
import com.artillexstudios.axrankmenu.hooks.currency.UltraEconomyHook;
import com.artillexstudios.axrankmenu.hooks.currency.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Locale;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;

public class HookManager {
    private static final ArrayList<CurrencyHook> currency = new ArrayList<>();
    private static PlaceholderAPIHook papi = null;
    private static AxRankMenuExpansion expansion = null;

    public void updateHooks() {
        currency.removeIf(currencyHook -> !currencyHook.isPersistent());

        if (CONFIG.getBoolean("hooks.Vault.register", true) && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            currency.add(new VaultHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into Vault!"));
        }
        
        if (CONFIG.getBoolean("hooks.PlayerPoints.register", true) && Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
            currency.add(new PlayerPointsHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into PlayerPoints!"));
        }

        if (ClassUtils.INSTANCE.classExists("su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI")) {
            if (CONFIG.getBoolean("hooks.ExcellentEconomy.register", true)) {
                currency.add(new ExcellentEconomyHook());
                Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into ExcellentEconomy!"));
            }
        } else {
            if (CONFIG.getBoolean("hooks.CoinsEngine.register", true) && Bukkit.getPluginManager().getPlugin("CoinsEngine") != null) {
                currency.add(new CoinsEngineHook());
                Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into CoinsEngine!"));
            }
        }

        if (CONFIG.getBoolean("hooks.RoyaleEconomy.register", true) && Bukkit.getPluginManager().getPlugin("RoyaleEconomy") != null) {
            currency.add(new RoyaleEconomyHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into RoyaleEconomy!"));
        }

        if (CONFIG.getBoolean("hooks.UltraEconomy.register", true) && Bukkit.getPluginManager().getPlugin("UltraEconomy") != null) {
            currency.add(new UltraEconomyHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into UltraEconomy!"));
        }

        if (CONFIG.getBoolean("hooks.BeastTokens.register", true) && Bukkit.getPluginManager().getPlugin("BeastTokens") != null) {
            currency.add(new BeastTokensHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into BeastTokens!"));
        }
        
        currency.removeIf(hook -> {
            try {
                hook.setup();
                return false;
            } catch (RuntimeException exception) {
                Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF3333[AxRankMenu] Failed to initialize " + hook.getName() + ": " + exception.getMessage()));
                return true;
            }
        });

        if (currency.isEmpty()) {
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF3333[AxRankMenu] Currency hook not found! Please check your config.yml!"));
        }

        if (CONFIG.getBoolean("hooks.PlaceholderAPI.register", true) && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papi = new PlaceholderAPIHook();
            if (expansion == null) {
                expansion = new AxRankMenuExpansion(com.artillexstudios.axrankmenu.AxRankMenu.getRequirementService());
                expansion.register();
            }
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into PlaceholderAPI!"));
        } else {
            papi = null;
        }
    }
    
    @SuppressWarnings("unused")
    public static void registerCurrencyHook(@NotNull Plugin plugin, @NotNull CurrencyHook currencyHook) {
        currency.add(currencyHook);
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxRankMenu] Hooked into " + plugin.getName() + "! Note: You must set the currency provider to CUSTOM or it will be overridden after reloading!"));
    }

    @NotNull
    public static ArrayList<CurrencyHook> getCurrency() {
        return currency;
    }

    @Nullable
    public static CurrencyHook getCurrencyHook(@NotNull String name) {
        for (CurrencyHook hook : currency) {
            if (!hook.getName().equalsIgnoreCase(name)) continue;
            return hook;
        }

        CurrencyHook dynamic = createDynamicCurrency(name);
        if (dynamic != null) {
            try {
                dynamic.setup();
                currency.add(dynamic);
                return dynamic;
            } catch (RuntimeException exception) {
                Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF3333[AxRankMenu] Failed to initialize currency " + name + ": " + exception.getMessage()));
            }
        }
        return null;
    }

    @Nullable
    private static CurrencyHook createDynamicCurrency(String name) {
        int separator = name.indexOf('-');
        if (separator <= 0 || separator == name.length() - 1) return null;
        String provider = name.substring(0, separator).toLowerCase(Locale.ROOT);
        String currencyId = name.substring(separator + 1);
        return switch (provider) {
            case "excellenteconomy" -> Bukkit.getPluginManager().isPluginEnabled("ExcellentEconomy") ? new ExcellentEconomyHook(currencyId) : null;
            case "coinsengine" -> Bukkit.getPluginManager().isPluginEnabled("CoinsEngine") ? new CoinsEngineHook(currencyId) : null;
            default -> null;
        };
    }

    @Nullable
    public static PlaceholderAPIHook getPapi() {
        return papi;
    }
}
