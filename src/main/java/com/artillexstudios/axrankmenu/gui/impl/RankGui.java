package com.artillexstudios.axrankmenu.gui.impl;

import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axrankmenu.gui.GuiFrame;
import com.artillexstudios.axrankmenu.rank.Rank;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import static com.artillexstudios.axrankmenu.AxRankMenu.RANKS;

public class RankGui extends GuiFrame {
    private static final Set<RankGui> openMenus = Collections.newSetFromMap(new WeakHashMap<>());

    private final Player player;
    private final Gui gui = Gui.gui(GuiType.valueOf(RANKS.getString("type", "CHEST")))
            .disableAllInteractions()
            .title(StringUtils.format(RANKS.getString("title")))
            .rows(RANKS.getInt("rows", 1))
            .create();

    public RankGui(@NotNull Player player) {
        super(RANKS, player);
        this.player = player;
        setGui(gui);
    }

    public void open() {
        for (String str : file.getBackingDocument().getRoutesAsStrings(false)) createItem(str);
        for (String route : RANKS.getBackingDocument().getRoutesAsStrings(false)) {
            if (RANKS.getString(route + ".rank", null) == null) continue;

            final Rank rank = new Rank(RANKS.getSection(route), player);
            if (rank.getGroup() == null) {
                Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF0000[AxRankMenu] The group %group% does not exist!".replace("%group%", RANKS.getString(route + ".rank", "---"))));
                continue;
            }

            super.addItem(rank.getItem(), route);
        }

        if (openMenus.contains(this)) {
            gui.update();
            return;
        }
        openMenus.add(this);

        gui.open(player);
    }

    public static Set<RankGui> getOpenMenus() {
        return openMenus;
    }
}
