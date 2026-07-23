package com.artillexstudios.axrankmenu.gui.impl;

import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axrankmenu.gui.GuiFrame;
import com.artillexstudios.axrankmenu.rank.Rank;
import dev.triumphteam.gui.builder.gui.BaseGuiBuilder;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.artillexstudios.axrankmenu.AxRankMenu.RANKS;

public class RankGui extends GuiFrame {
    private static final Set<RankGui> openMenus = ConcurrentHashMap.newKeySet();

    private final Player player;
    private final Gui gui;

    public RankGui(@NotNull Player player) {
        super(RANKS, player);
        this.player = player;

        GuiType guiType = GuiType.valueOf(RANKS.getString("type", "CHEST"));

        BaseGuiBuilder<?, ?> builder;
        if (guiType == GuiType.CHEST) {
            builder = Gui.gui().rows(RANKS.getInt("rows", 1));
        } else {
            builder = Gui.gui(guiType);
        }

        gui = (Gui) builder.disableAllInteractions()
                .title(StringUtils.format(RANKS.getString("title")))
                .create();
        gui.setCloseGuiAction(event -> {
            openMenus.remove(this);
        });

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

    public void refresh() {
        if (!player.isOnline()) {
            openMenus.remove(this);
            return;
        }
        Scheduler.get().execute(player, this::open, () -> openMenus.remove(this), 1L);
    }

    public static void closeAll() {
        for (RankGui menu : openMenus) {
            if (menu.player.isOnline()) menu.player.closeInventory();
        }
        openMenus.clear();
    }
}
