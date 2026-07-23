package com.artillexstudios.axrankmenu.rank;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.libs.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.utils.NumberUtils;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axrankmenu.AxRankMenu;
import com.artillexstudios.axrankmenu.requirement.RankPurchaseService;
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
    private static final RankPurchaseService purchaseService = new RankPurchaseService(AxRankMenu.getRequirementService(), luckPerms);
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
            var user = luckPerms.getUserManager().getUser(requester.getUniqueId());
            if (user == null || group == null) {
                MESSAGEUTILS.sendLang(requester, "requirements.rank-grant-failed");
                return;
            }
            final String cGroupName = user.getPrimaryGroup();
            final Group cGroup = luckPerms.getGroupManager().getGroup(cGroupName);
            if (cGroup == null) {
                MESSAGEUTILS.sendLang(requester, "requirements.rank-grant-failed");
                return;
            }
            if (CONFIG.getBoolean("prevent-downgrading", true) && cGroup.getWeight().isPresent() && group.getWeight().isPresent() && group.getWeight().getAsInt() <= cGroup.getWeight().getAsInt()) {
                MESSAGEUTILS.sendLang(requester, "error.downgrade-disabled");
                return;
            }

            if (CONFIG.getBoolean("force-buy-order.enabled", false)) {
                final Track track = luckPerms.getTrackManager().getTrack(CONFIG.getString("force-buy-order.track"));
                if (track == null || track.getGroups().isEmpty()) {
                    MESSAGEUTILS.sendLang(requester, "error.buy-order");
                    return;
                }
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

            double finalPrice = price;
            purchaseService.purchase(
                    requester,
                    AxRankMenu.getRequirementService().rankIdForGroup(section.getString("rank")),
                    section,
                    group,
                    finalPrice,
                    currency,
                    this::runBuyActions
            );
        });
    }

    private void runBuyActions() {
        var actions = section.getStringList("buy-actions");
        if (actions.isEmpty()) {
            MESSAGEUTILS.sendLang(requester, "requirements.success", java.util.Map.of("%rank%", section.getString("rank")));
            requester.closeInventory();
            return;
        }
        for (String action : actions) {
            final String[] split = action.trim().split("\\s+", 2);
            if (split.length == 0) continue;
            String type = split[0];
            String command = split.length == 2 ? split[1] : "";
            command = command.replace("%player%", requester.getName());
            command = command.replace("%name%", section.getString("item.name"));
            command = command.replace("%rank%", section.getString("rank"));
            command = command.replace("%price%", section.getString("price", "---"));
            command = command.replace("%server%", section.getString("server"));
            if (command.contains("\n") || command.contains("\r")) continue;

            switch (type) {
                case "[MESSAGE]" -> requester.sendMessage(StringUtils.formatToString(command));
                case "[CONSOLE]" -> {
                    if (!command.isBlank()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
                case "[CLOSE]" -> requester.closeInventory();
                default -> Bukkit.getLogger().warning("[AxRankMenu] Unknown buy action type " + type + " for rank " + section.getString("rank"));
            }
        }
    }

    @Nullable
    public Group getGroup() {
        return group;
    }

    public Section getSection() {
        return section;
    }
}
