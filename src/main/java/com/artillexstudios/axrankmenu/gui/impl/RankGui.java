package com.artillexstudios.axrankmenu.gui.impl;

import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axrankmenu.gui.GuiFrame;
import com.artillexstudios.axrankmenu.rank.Rank;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;
import static com.artillexstudios.axrankmenu.AxRankMenu.RANKS;

public class RankGui extends GuiFrame {
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
        for (String route : RANKS.getBackingDocument().getRoutesAsStrings(false)) {
            if (RANKS.getString(route + ".rank", null) == null) continue;

            final Rank rank = new Rank(RANKS.getSection(route), player);
            if (rank.getGroup() == null) {
                Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF0000[AxRankMenu] The group %group% does not exist!".replace("%group%", CONFIG.getString(route + ".rank"))));
                continue;
            }

            super.addItem(rank.getItem(), route);
        }

        gui.open(player);
    }
}
