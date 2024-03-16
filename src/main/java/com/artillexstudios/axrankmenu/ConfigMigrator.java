package com.artillexstudios.axrankmenu;

import com.artillexstudios.axapi.utils.NumberUtils;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;
import static com.artillexstudios.axrankmenu.AxRankMenu.RANKS;

public class ConfigMigrator {

    public ConfigMigrator() {
        RANKS.getBackingDocument().remove("VIP");
        RANKS.getBackingDocument().remove("MVP");

        for (String route : CONFIG.getSection("menu").getRoutesAsStrings(true)) {

            if (!CONFIG.getStringList("menu." + route).isEmpty()) {
                System.out.println(CONFIG.getStringList("menu." + route));
                RANKS.set(route, CONFIG.getStringList("menu." + route));
                continue;
            }

            if (NumberUtils.isInt(CONFIG.getString("menu." + route))) {
                RANKS.set(route, CONFIG.getLong("menu." + route));
                continue;
            }

            if (NumberUtils.isDouble(CONFIG.getString("menu." + route))) {
                RANKS.set(route, CONFIG.getDouble("menu." + route));
                continue;
            }

            RANKS.set(route, CONFIG.getString("menu." + route));
        }

        CONFIG.getBackingDocument().remove("menu");

        CONFIG.save();
        RANKS.save();
    }
}
