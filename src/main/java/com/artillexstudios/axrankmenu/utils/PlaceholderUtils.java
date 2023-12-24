package com.artillexstudios.axrankmenu.utils;

import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axrankmenu.hooks.HookManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ImmutableContextSet;
import org.jetbrains.annotations.NotNull;

public class PlaceholderUtils {

    @NotNull
    public static String parsePlaceholders(@NotNull String string, @NotNull Section section) {
        string = string.replace("%price%", StringUtils.formatNumber("#,###.##", section.getDouble("price")));

        final LuckPerms lpApi = LuckPermsProvider.get();
        final ImmutableContextSet set = lpApi.getContextManager().getStaticContext();
        string = string.replace("%server%", (section.getString("server").equals("") && set.getAnyValue("server").isPresent()) ? set.getAnyValue("server").get() : section.getString("server"));

        if (HookManager.getPapi() != null)
            string = HookManager.getPapi().parsePlaceholders(string);

        return StringUtils.formatToString(string);
    }
}
