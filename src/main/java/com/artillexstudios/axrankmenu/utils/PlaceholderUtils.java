package com.artillexstudios.axrankmenu.utils;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.reflection.ClassUtils;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axrankmenu.hooks.HookManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;
import static com.artillexstudios.axrankmenu.AxRankMenu.RANKS;

public class PlaceholderUtils {

    @NotNull
    public static String parsePlaceholders(@NotNull Player player, @NotNull String string, @NotNull Section section) {
        final LuckPerms lpApi = LuckPermsProvider.get();

        final User user = lpApi.getUserManager().getUser(player.getUniqueId());
        final Group playerRank = lpApi.getGroupManager().getGroup(user.getPrimaryGroup());

        if (ClassUtils.INSTANCE.classExists("me.clip.placeholderapi.PlaceholderAPI")) {
            string = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, string);
        }

        double price = section.getDouble("price");
        if (CONFIG.getBoolean("discount-ranks", false)) {
            double currentPrice = Math.max(RANKS.getDouble(playerRank.getName() + ".price", -1), RANKS.getDouble(playerRank.getName().toUpperCase() + ".price", -1));
            if (currentPrice != -1) {
                price -= currentPrice;
                price = Math.max(0, price);
            }
        }
        string = string.replace("%price%", StringUtils.formatNumber("#,###.##", price));

        final ImmutableContextSet set = lpApi.getContextManager().getStaticContext();
        string = string.replace("%server%", (section.getString("server").isEmpty() && set.getAnyValue("server").isPresent()) ? set.getAnyValue("server").get() : section.getString("server"));

        if (HookManager.getPapi() != null)
            string = HookManager.getPapi().parsePlaceholders(string);

        return string;
    }
}
