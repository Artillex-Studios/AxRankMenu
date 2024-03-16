package com.artillexstudios.axrankmenu.gui;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axrankmenu.utils.ItemBuilderUtil;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class GuiFrame {
    protected final Config file;
    protected BaseGui gui;
    protected Player player;

    public GuiFrame(Config file, Player player) {
        this.file = file;
        this.player = player;
    }

    public void setGui(BaseGui gui) {
        this.gui = gui;
        for (String str : file.getBackingDocument().getRoutesAsStrings(false)) {
            if (file.getString(str + ".rank", null) != null) continue;
            createItem(str);
        }
    }

    @NotNull
    public Config getFile() {
        return file;
    }

    protected ItemStack buildItem(@NotNull String key) {
        return ItemBuilderUtil.newBuilder(file.getSection(key)).get();
    }

    protected ItemStack buildItem(@NotNull String key, Map<String, String> replacements) {
        return ItemBuilderUtil.newBuilder(file.getSection(key), replacements).get();
    }

    protected void createItem(@NotNull String route) {
        createItem(route, null, Map.of());
    }

    protected void createItem(@NotNull String route, @Nullable GuiAction<InventoryClickEvent> action) {
        createItem(route, action, Map.of());
    }

    protected void createItem(@NotNull String route, @Nullable GuiAction<InventoryClickEvent> action, Map<String, String> replacements) {
        if (file.getString(route + ".item.type") == null && file.getString(route + ".item.material") == null) return;
        final GuiItem guiItem = new GuiItem(buildItem(route + ".item", replacements), action);
        final List<Integer> slots = file.getBackingDocument().getIntList(route + ".slot");
        if (slots.isEmpty()) gui.setItem(file.getInt(route + ".slot"), guiItem);
        else gui.setItem(slots, guiItem);
    }

    protected void addItem(@NotNull GuiItem guiItem, @NotNull String route) {
        final List<Integer> slots = file.getBackingDocument().getIntList(route + ".slot");
        if (slots.isEmpty()) gui.setItem(file.getInt(route + ".slot"), guiItem);
        else gui.setItem(slots, guiItem);
    }
}
