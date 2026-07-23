package com.artillexstudios.axrankmenu.requirement;

import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementPolicyTest {

    @Test
    void allRequiresEveryEntry() {
        assertFalse(RequirementPolicy.isSatisfied("ALL", List.of(entry(true), entry(false))));
        assertTrue(RequirementPolicy.isSatisfied("ALL", List.of(entry(true), entry(true))));
    }

    @Test
    void anyRequiresOneEntry() {
        assertTrue(RequirementPolicy.isSatisfied("ANY", List.of(entry(false), entry(true))));
        assertFalse(RequirementPolicy.isSatisfied("ANY", List.of(entry(false), entry(false))));
    }

    private RequirementEvaluation entry(boolean successful) {
        RequirementResult result = new RequirementResult(successful, "", "", "", "", "", java.util.Map.of());
        return new RequirementEvaluation("test", "test", false, result);
    }
}
