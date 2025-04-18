package com.artillexstudios.axrankmenu.utils;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.reflection.ClassUtils;
import com.artillexstudios.axapi.utils.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ItemBuilderUtil {

    @NotNull
    public static ItemBuilder newBuilder(@NotNull Section section, Player player) {
        return newBuilder(section, Map.of(), player);
    }

    @NotNull
    public static ItemBuilder newBuilder(@NotNull Section section, Map<String, String> replacements, Player player) {
        final ItemBuilder builder = new ItemBuilder(section);

        section.getOptionalString("name").ifPresent((name) -> {
            if (ClassUtils.INSTANCE.classExists("me.clip.placeholderapi.PlaceholderAPI")) {
                name = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, name);
            }
            builder.setName(name, replacements);
        });

        section.getOptionalStringList("lore").ifPresent((lore) -> {
            if (ClassUtils.INSTANCE.classExists("me.clip.placeholderapi.PlaceholderAPI")) {
                lore = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, lore);
            }
            builder.setLore(lore, replacements);
        });

        return builder;
    }

    @NotNull
    @Contract("_ -> new")
    public static ItemBuilder newBuilder(@NotNull ItemStack itemStack) {
        return new ItemBuilder(itemStack);
    }
}
