package com.artillexstudios.axrankmenu.gui;

import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axapi.utils.NumberUtils;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axrankmenu.hooks.HookManager;
import com.artillexstudios.axrankmenu.hooks.currency.CurrencyHook;
import com.artillexstudios.axrankmenu.utils.PlaceholderUtils;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;
import static com.artillexstudios.axrankmenu.AxRankMenu.LANG;
import static com.artillexstudios.axrankmenu.AxRankMenu.MESSAGEUTILS;

public class RankGui {

    public void openFor(@NotNull Player player) {
        final Gui menu = Gui.gui()
                .title(StringUtils.format(CONFIG.getString("menu.title")))
                .rows(CONFIG.getInt("menu.rows", 6))
                .disableAllInteractions()
                .create();

        for (String str : CONFIG.getSection("menu").getRoutesAsStrings(false)) {
            if (str.equals("title") || str.equals("rows")) continue;

            if (CONFIG.getString("menu." + str + ".rank") == null) {
                final ItemStack it = new ItemBuilder(CONFIG.getSection("menu." + str + ".item")).get();
                final GuiItem item = new GuiItem(it);

                final List<Integer> slots = CONFIG.getBackingDocument().getIntList("menu." + str + ".slot");

                if (slots.isEmpty())
                    menu.setItem(CONFIG.getInt("menu." + str + ".slot"), item);
                else
                    menu.setItem(slots, item);
            } else {
                final LuckPerms lpApi = LuckPermsProvider.get();
                final String groupName = CONFIG.getString("menu." + str + ".rank");
                final Group group = lpApi.getGroupManager().getGroup(groupName);
                if (group == null) {
                    Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF0000[AxRankMenu] The group %group% does not exist!".replace("%group%", groupName)));
                    continue;
                }

                final List<String> lore = new ArrayList<>();

                for (String line : CONFIG.getStringList("menu." + str + ".item.lore")) {
                    if (line.contains("%permission%")) {
                        LANG.getBackingDocument().setGeneralSettings(GeneralSettings.builder().setRouteSeparator('倀').build());
                        for (Node node : group.getNodes()) {
                            if (node.isNegated()) continue;

                            ImmutableContextSet set = lpApi.getContextManager().getStaticContext();
                            if (!CONFIG.getString("menu." + str + ".server", "").isEmpty()) {
                                set = ImmutableContextSet.of("server", CONFIG.getString("menu." + str + ".server"));
                            }

                            if (!CONFIG.getBoolean("include-global-permissions") && !node.getContexts().equals(set)) continue;
                            if (CONFIG.getBoolean("include-global-permissions") && !node.getContexts().isEmpty() && !node.getContexts().equals(set)) continue;
                            String permission = node.getKey();

                            Integer number = null;
                            for (String t1 : permission.split("\\.")) {
                                if (!NumberUtils.isInt(t1)) continue;
                                number = Integer.parseInt(t1);
                            }

                            permission = permission.replace("" + number, "#");

                            if (LANG.getString("permissions倀" + permission) == null) {
                                LANG.set("permissions倀" + permission, permission);
                                LANG.save();
                            }

                            String tName = LANG.getString("permissions倀" + permission);
                            if (tName.isEmpty()) continue;
                            lore.add(PlaceholderUtils.parsePlaceholders(line.replace("%permission%", tName.replace("#", "" + number)), CONFIG.getSection("menu." + str)));
                        }
                        LANG.getBackingDocument().setGeneralSettings(GeneralSettings.builder().setRouteSeparator('.').build());
                    } else {
                        lore.add(PlaceholderUtils.parsePlaceholders(line, CONFIG.getSection("menu." + str)));
                    }
                }

                final ItemStack it = new ItemBuilder(CONFIG.getSection("menu." + str + ".item")).get();
                final ItemMeta meta = it.getItemMeta();
                meta.setLore(lore);
                it.setItemMeta(meta);

                final GuiItem item = new GuiItem(it, event -> {
                    double price = CONFIG.getDouble("menu." + str + ".price", -1);
                    final String currency = CONFIG.getString("menu." + str + ".currency", "Vault");

                    if (price == -1) return;

                    final CurrencyHook hook = HookManager.getCurrencyHook(currency);
                    if (hook == null) return;

                    if (hook.getBalance(player) < price) {
                        MESSAGEUTILS.sendLang(player, "buy.no-currency");
                        return;
                    }

                    hook.takeBalance(player, price);

                    for (String action : CONFIG.getStringList("menu." + str + ".buy-actions")) {
                        final String[] type = action.split(" ");
                        String ac = action.replace(type[0] + " ", "");
                        ac = ac.replace("%player%", player.getName());
                        ac = ac.replace("%name%", CONFIG.getString("menu." + str + ".item.name"));
                        ac = ac.replace("%rank%", CONFIG.getString("menu." + str + ".rank"));
                        ac = ac.replace("%price%", CONFIG.getString("menu." + str + ".price", "---"));
                        ac = ac.replace("%server%", CONFIG.getString("menu." + str + ".server"));

                        switch (type[0]) {
                            case "[MESSAGE]": {
                                player.sendMessage(StringUtils.formatToString(ac));
                                break;
                            }
                            case "[CONSOLE]": {
                                String finalAc = ac;
                                Scheduler.get().execute(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalAc));
                                break;
                            }
                            case "[CLOSE]": {
                                player.closeInventory();
                                break;
                            }
                        }
                    }
                });

//                menu.setItem(CONFIG.getInt("menu." + str + ".slot"), item);

                final List<Integer> slots = CONFIG.getBackingDocument().getIntList("menu." + str + ".slot");

                if (slots.isEmpty())
                    menu.setItem(CONFIG.getInt("menu." + str + ".slot"), item);
                else
                    menu.setItem(slots, item);
            }
        }

        menu.open(player);
    }
}
