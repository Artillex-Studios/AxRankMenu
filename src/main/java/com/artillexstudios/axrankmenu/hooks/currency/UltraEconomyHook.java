package com.artillexstudios.axrankmenu.hooks.currency;

import me.TechsCode.UltraEconomy.UltraEconomy;
import me.TechsCode.UltraEconomy.objects.Account;
import me.TechsCode.UltraEconomy.objects.Currency;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;

public class UltraEconomyHook implements CurrencyHook {
    private Currency currency = null;

    @Override
    public void setup() {
        final Optional<Currency> currencyOptional = UltraEconomy.getAPI().getCurrencies().name(CONFIG.getString("hook-settings.UltraEconomy.currency-name", "coins"));
        if (!currencyOptional.isPresent()) throw new RuntimeException("Currency not found!");
        currency = currencyOptional.get();
    }

    @Override
    public String getName() {
        return "UltraEconomy";
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public double getBalance(@NotNull Player p) {
        final Optional<Account> account = UltraEconomy.getAPI().getAccounts().uuid(p.getUniqueId());
        if (!account.isPresent()) return 0.0D;
        return account.get().getBalance(currency).getOnHand();
    }

    @Override
    public void giveBalance(@NotNull Player p, double amount) {
        final Optional<Account> account = UltraEconomy.getAPI().getAccounts().uuid(p.getUniqueId());
        if (!account.isPresent()) return;
        account.get().addBalance(currency, amount);
    }

    @Override
    public void takeBalance(@NotNull Player p, double amount) {
        final Optional<Account> account = UltraEconomy.getAPI().getAccounts().uuid(p.getUniqueId());
        if (!account.isPresent()) return;
        account.get().removeBalance(currency, amount);
    }
}