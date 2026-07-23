package com.artillexstudios.axrankmenu.requirement;

import com.artillexstudios.axrankmenu.api.requirement.ConsumptionResult;
import com.artillexstudios.axrankmenu.api.requirement.RankRequirementProvider;
import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import com.artillexstudios.axrankmenu.api.requirement.RollbackResult;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class ConsumptionTransactionTest {

    @Test
    void secondFailureRollsBackFirstDeductionInReverseOrder() {
        Player player = mock(Player.class);
        List<String> calls = new ArrayList<>();
        ConsumptionTransaction transaction = new ConsumptionTransaction();
        transaction.add(provider("money", true, calls), context("money"));
        transaction.add(provider("xp", false, calls), context("xp"));

        ConsumptionTransaction.ExecutionResult result = transaction.execute(player);

        assertFalse(result.successful());
        assertEquals(List.of("consume-money", "consume-xp", "rollback-money"), calls);
        assertEquals(0, transaction.appliedCount());
    }

    private RankRequirementProvider provider(String id, boolean consumeResult, List<String> calls) {
        return new RankRequirementProvider() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public RequirementResult evaluate(Player player, RequirementContext context) {
                return RequirementResult.success("", "", "");
            }

            @Override
            public boolean supportsConsumption() {
                return true;
            }

            @Override
            public ConsumptionResult consume(Player player, RequirementContext context) {
                calls.add("consume-" + id);
                return consumeResult ? ConsumptionResult.success(id) : ConsumptionResult.failure("", "failed");
            }

            @Override
            public RollbackResult rollback(Player player, RequirementContext context, Object rollbackState) {
                calls.add("rollback-" + id);
                return RollbackResult.success();
            }
        };
    }

    private RequirementContext context(String id) {
        return new RequirementContext("rank", id, Map.of());
    }
}
