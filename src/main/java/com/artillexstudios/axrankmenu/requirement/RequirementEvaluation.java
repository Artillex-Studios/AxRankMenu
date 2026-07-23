package com.artillexstudios.axrankmenu.requirement;

import com.artillexstudios.axrankmenu.api.requirement.RequirementResult;

public record RequirementEvaluation(String id, String type, boolean consumable, RequirementResult result) {
}
