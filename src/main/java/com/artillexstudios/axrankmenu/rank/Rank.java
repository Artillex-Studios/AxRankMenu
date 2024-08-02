package com.artillexstudios.axrankmenu.rank;

import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.NumberUtils;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axrankmenu.hooks.HookManager;
import com.artillexstudios.axrankmenu.hooks.currency.CurrencyHook;
import com.artillexstudios.axrankmenu.utils.ItemBuilderUtil;
import com.artillexstudios.axrankmenu.utils.PlaceholderUtils;
import dev.triumphteam.gui.guis.GuiItem;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;
import static com.artillexstudios.axrankmenu.AxRankMenu.LANG;
import static com.artillexstudios.axrankmenu.AxRankMenu.MESSAGEUTILS;
import static com.artillexstudios.axrankmenu.AxRankMenu.RANKS;

public class Rank {
    private static final LuckPerms luckPerms = LuckPermsProvider.get();
    private final Group group;
    private final Section section;
    private final Player requester;

    public Rank(@NotNull Section section, @NotNull Player requester) {
        this.section = section;
        this.requester = requester;
        group = luckPerms.getGroupManager().getGroup(section.getString("rank"));
    }

    public Node[] getNodes() {
        return group.getNodes().stream().filter(node -> !node.isNegated()).toArray(Node[]::new);
    }

    public GuiItem getItem() {
        final List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("item.lore")) {
            if (line.contains("%permission%")) {
                LANG.getBackingDocument().setGeneralSettings(GeneralSettings.builder().setRouteSeparator('倀').build());
                for (Node node : getNodes()) {
                    ImmutableContextSet set = luckPerms.getContextManager().getStaticContext();
                    if (!section.getString("server", "").isEmpty()) {
                        set = ImmutableContextSet.of("server", section.getString("server"));
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
                    lore.add(PlaceholderUtils.parsePlaceholders(requester, line.replace("%permission%", tName.replace("#", "" + number)), section));
                }
                LANG.getBackingDocument().setGeneralSettings(GeneralSettings.builder().setRouteSeparator('.').build());
            } else {
                lore.add(PlaceholderUtils.parsePlaceholders(requester, line, section));
            }
        }

        final ItemStack it = ItemBuilderUtil.newBuilder(section.getSection("item"), requester).setLore(lore).get();

        return new GuiItem(it, event -> {
            final String cGroupName = luckPerms.getUserManager().getUser(requester.getUniqueId()).getPrimaryGroup();
            final Group cGroup = luckPerms.getGroupManager().getGroup(cGroupName);
            if (CONFIG.getBoolean("prevent-downgrading", true) && cGroup.getWeight().isPresent() && group.getWeight().isPresent() && group.getWeight().getAsInt() <= cGroup.getWeight().getAsInt()) {
                MESSAGEUTILS.sendLang(requester, "error.downgrade-disabled");
                return;
            }

            if (CONFIG.getBoolean("force-buy-order.enabled", false)) {
                final Track track = luckPerms.getTrackManager().getTrack(CONFIG.getString("force-buy-order.track"));
                final String nextGroup = track.getNext(cGroup);
                if (nextGroup == null && !track.containsGroup(cGroup) && !track.getGroups().get(0).equals(group.getName())) {
                    MESSAGEUTILS.sendLang(requester, "error.buy-order");
                    return;
                }
                if (nextGroup != null && !group.getName().equals(nextGroup)) {
                    MESSAGEUTILS.sendLang(requester, "error.buy-order");
                    return;
                }
            }

            double price = section.getDouble("price", -1.0D);
            final String currency = section.getString("currency", "Vault");

            if (price == -1) return;

            if (CONFIG.getBoolean("discount-ranks", false)) {
                double currentPrice = Math.max(RANKS.getDouble(cGroup.getName() + ".price", -1), RANKS.getDouble(cGroup.getName().toUpperCase() + ".price", -1));
                if (currentPrice != -1) {
                    price -= currentPrice;
                    price = Math.max(0, price);
                }
            }

            final CurrencyHook hook = HookManager.getCurrencyHook(currency);
            if (hook == null) return;

            if (hook.getBalance(requester) < price) {
                MESSAGEUTILS.sendLang(requester, "buy.no-currency");
                return;
            }

            hook.takeBalance(requester, price);

            var actions = section.getStringList("buy-actions");
            if (actions.isEmpty()) {
                Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#FF0000[AxRankMenu] The buy-actions section is missing from the " + section.getString("rank") + " rank, this will cause issues!"));
            }
            for (String action : actions) { // todo: add a warning if missing
                final String[] type = action.split(" ");
                String ac = action.replace(type[0] + " ", "");
                ac = ac.replace("%player%", requester.getName());
                ac = ac.replace("%name%", section.getString("item.name"));
                ac = ac.replace("%rank%", section.getString("rank"));
                ac = ac.replace("%price%", section.getString("price", "---"));
                ac = ac.replace("%server%", section.getString("server"));

                switch (type[0]) {
                    case "[MESSAGE]": {
                        requester.sendMessage(StringUtils.formatToString(ac));
                        break;
                    }
                    case "[CONSOLE]": {
                        String finalAc = ac;
                        Scheduler.get().execute(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalAc));
                        break;
                    }
                    case "[CLOSE]": {
                        requester.closeInventory();
                        break;
                    }
                }
            }
        });
    }

    @Nullable
    public Group getGroup() {
        return group;
    }

    public Section getSection() {
        return section;
    }
}
