package com.artillexstudios.axrankmenu.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook {

    public String parsePlaceholders(@NotNull String str) {
        return PlaceholderAPI.setPlaceholders(null, str);
    }

    public String parsePlaceholders(@NotNull Player player, @NotNull String str) {
        return PlaceholderAPI.setPlaceholders(player, str);
    }
}
