package com.artillexstudios.axrankmenu.requirement.provider;

import com.artillexstudios.axrankmenu.api.requirement.ConsumptionResult;
import com.artillexstudios.axrankmenu.api.requirement.RankRequirementProvider;
import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import com.artillexstudios.axrankmenu.api.requirement.RollbackResult;
import com.artillexstudios.axrankmenu.hooks.HookManager;
import com.artillexstudios.axrankmenu.hooks.currency.CurrencyHook;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

public final class CurrencyRequirementProvider extends AbstractNumericProvider implements RankRequirementProvider {
    @Override
    public String id() {
        return "currency";
    }

    @Override
    public RequirementResult evaluate(Player player, RequirementContext context) {
        CurrencyHook hook = HookManager.getCurrencyHook(context.string("currency", "Vault"));
        if (hook == null) {
            return RequirementResult.failure("requirements.currency-unavailable", "Currency hook is unavailable", "0", display(BigDecimal.valueOf(context.number("amount", 0))), "");
        }
        return compare(BigDecimal.valueOf(hook.getBalance(player)), context, "requirements.insufficient-currency");
    }

    @Override
    public boolean supportsConsumption() {
        return true;
    }

    @Override
    public ConsumptionResult consume(Player player, RequirementContext context) {
        double amount = context.number("amount", 0);
        CurrencyHook hook = HookManager.getCurrencyHook(context.string("currency", "Vault"));
        if (hook == null || amount < 0 || hook.getBalance(player) + 1.0E-7 < amount) {
            return ConsumptionResult.failure("requirements.consume-failed", "Currency is unavailable or balance changed");
        }
        return hook.takeBalance(player, amount)
                ? ConsumptionResult.success(amount)
                : ConsumptionResult.failure("requirements.consume-failed", "Currency provider rejected withdrawal");
    }

    @Override
    public RollbackResult rollback(Player player, RequirementContext context, Object rollbackState) {
        CurrencyHook hook = HookManager.getCurrencyHook(context.string("currency", "Vault"));
        double amount = rollbackState instanceof Number number ? number.doubleValue() : context.number("amount", 0);
        return hook != null && hook.giveBalance(player, amount)
                ? RollbackResult.success()
                : RollbackResult.failure("Currency provider rejected refund");
    }
}
