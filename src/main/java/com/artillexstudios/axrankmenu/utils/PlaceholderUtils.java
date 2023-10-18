package com.artillexstudios.axrankmenu.utils;

import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.utils.StringUtils;
import org.jetbrains.annotations.NotNull;

public class PlaceholderUtils {

    @NotNull
    public static String parsePlaceholders(@NotNull String string, @NotNull Section section) {
        string = string.replace("%price%", StringUtils.formatNumber("#,###.##", section.getDouble("price")));

        return StringUtils.formatToString(string);
    }
}
