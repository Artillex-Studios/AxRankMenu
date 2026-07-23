package com.artillexstudios.axrankmenu.requirement.provider;

import com.artillexstudios.axrankmenu.api.requirement.RequirementContext;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class ExperienceRequirementProviderTest {

    @Test
    void checksAndConsumesLevelsWithoutChangingProgress() {
        Player player = mock(Player.class);
        when(player.getLevel()).thenReturn(30);
        when(player.getExp()).thenReturn(0.5F);
        when(player.getTotalExperience()).thenReturn(1395);
        ExperienceRequirementProvider provider = new ExperienceRequirementProvider(true);
        RequirementContext context = new RequirementContext("rank", "xp", Map.of("amount", 25, "operator", ">="));

        assertTrue(provider.evaluate(player, context).successful());
        assertTrue(provider.consume(player, context).successful());
        verify(player).setLevel(5);
        verify(player).setTotalExperience(anyInt());
    }

    @Test
    void totalExperienceFormulaIncludesProgress() {
        Player player = mock(Player.class);
        when(player.getLevel()).thenReturn(10);
        when(player.getExp()).thenReturn(0.5F);
        assertEquals(174, ExperienceRequirementProvider.totalExperience(player));
    }
}
