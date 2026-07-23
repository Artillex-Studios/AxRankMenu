package com.artillexstudios.axrankmenu.api.requirement;

import java.util.Collection;

public interface RankRequirementRegistry {
    void register(RankRequirementProvider provider);

    boolean unregister(String id);

    RankRequirementProvider provider(String id);

    Collection<RankRequirementProvider> providers();
}
