package com.artillexstudios.axrankmenu.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook {

    public String parsePlaceholders(@NotNull String str) {
        return PlaceholderAPI.setPlaceholders(null, str);
    }
}
