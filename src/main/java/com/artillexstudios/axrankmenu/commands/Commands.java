package com.artillexstudios.axrankmenu.commands;

import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.nms.NMSHandlers;
import com.artillexstudios.axapi.reflection.FastFieldAccessor;
import com.artillexstudios.axrankmenu.AxRankMenu;
import com.artillexstudios.axrankmenu.commands.annotations.Groups;
import com.artillexstudios.axrankmenu.gui.impl.RankGui;
import com.artillexstudios.axrankmenu.hooks.HookManager;
import com.artillexstudios.axrankmenu.utils.CommandMessages;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Warning;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.BukkitCommandHandler;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.bukkit.exception.InvalidPlayerException;
import revxrsal.commands.orphan.OrphanCommand;
import revxrsal.commands.orphan.Orphans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;
import static com.artillexstudios.axrankmenu.AxRankMenu.LANG;
import static com.artillexstudios.axrankmenu.AxRankMenu.MESSAGEUTILS;
import static com.artillexstudios.axrankmenu.AxRankMenu.RANKS;

public class Commands implements OrphanCommand {

    @DefaultFor({"~", "~ open"})
    @CommandPermission(value = "axrankmenu.use")
    public void open(@NotNull Player sender) {
        new RankGui(sender).open();
    }

    @Subcommand({"reload"})
    @CommandPermission(value = "axrankmenu.reload")
    public void reload(@NotNull Player sender) {
        if (!CONFIG.reload()) {
            MESSAGEUTILS.sendLang(sender, "reload.failed", Collections.singletonMap("%file%", "tiers.yml"));
            return;
        }

        if (!LANG.reload()) {
            MESSAGEUTILS.sendLang(sender, "reload.failed", Collections.singletonMap("%file%", "lang.yml"));
            return;
        }

        if (!RANKS.reload()) {
            MESSAGEUTILS.sendLang(sender, "reload.failed", Collections.singletonMap("%file%", "ranks.yml"));
            return;
        }

        new HookManager().updateHooks();

        MESSAGEUTILS.sendLang(sender, "reload.success");
    }

    @Subcommand({"addrank"})
    @CommandPermission(value = "axrankmenu.addrank")
    public void addRank(@NotNull Player sender, @Groups String group) {
        final Section section = RANKS.getBackingDocument().createSection(group);
        section.set("rank", group);
        section.set("server", "");
        section.set("price", -1.0);
        section.set("currency", "Vault");
        section.set("slot", getFirstEmptySlot());
        section.set("item.type", "GRAY_BANNER");
        section.set("item.name", "&#00FF00" + group + " &fRANK");
        section.set("item.lore", Arrays.asList(
                " ",
                " &7- &fPrice: &#00AA00$%price%",
                " ",
                "&#00FF00ᴘᴇʀᴍɪssɪᴏɴs",
                " &7- &f%permission%",
                " ",
                "&#00FF00&l(!) &#00FF00Click here to purchase!"
        ));
        section.set("buy-actions", Arrays.asList(
                "[MESSAGE] &#00FF00You have purchased the &f%name%&#00FF00! &7(%rank%)",
                "[CONSOLE] lp user %player% parent set " + group,
                "[CLOSE] menu"
        ));
        RANKS.save();
        MESSAGEUTILS.sendLang(sender, "add-rank", Map.of("%rank%", group));
    }

    private int getFirstEmptySlot() {
        final ArrayList<Integer> filled = new ArrayList<>();
        for (String str : RANKS.getBackingDocument().getRoutesAsStrings(false)) {
            filled.add(RANKS.getInt(str + ".slot", -1));
        }
        for (int i = 0; i < RANKS.getInt("rows", 3) * 9; i++) {
            if (filled.contains(i)) continue;
            return i;
        }
        return -1;
    }

    private static BukkitCommandHandler handler = null;

    public static void registerCommand() {
        if (handler == null) {
            Warning.WarningState prevState = Bukkit.getWarningState();
            FastFieldAccessor accessor = FastFieldAccessor.forClassField(Bukkit.getServer().getClass().getPackage().getName() + ".CraftServer", "warningState");
            accessor.set(Bukkit.getServer(), Warning.WarningState.OFF);
            handler = BukkitCommandHandler.create(AxRankMenu.getInstance());
            accessor.set(Bukkit.getServer(), prevState);

            handler.registerValueResolver(0, OfflinePlayer.class, context -> {
                String value = context.pop();
                if (value.equalsIgnoreCase("self") || value.equalsIgnoreCase("me")) return ((BukkitCommandActor) context.actor()).requirePlayer();
                OfflinePlayer player = NMSHandlers.getNmsHandler().getCachedOfflinePlayer(value);
                if (player == null && !(player = Bukkit.getOfflinePlayer(value)).hasPlayedBefore()) throw new InvalidPlayerException(context.parameter(), value);
                return player;
            });

            handler.getAutoCompleter().registerSuggestionFactory(parameter -> {
                if (parameter.hasAnnotation(Groups.class)) {
                    return (args, sender, command) -> {
                        final LuckPerms luckPerms = LuckPermsProvider.get();
                        final Set<Group> groups = new HashSet<>(luckPerms.getGroupManager().getLoadedGroups());
                        groups.removeIf(group -> {
                            for (String str : RANKS.getBackingDocument().getRoutesAsStrings(false)) {
                                if (RANKS.getString(str + ".rank", "").equalsIgnoreCase(group.getName())) return true;
                            }
                            return false;
                        });

                        return groups.stream().map(Group::getName).collect(Collectors.toList());
                    };
                }
                return null;
            });

            handler.getAutoCompleter().registerParameterSuggestions(OfflinePlayer.class, (args, sender, command) -> {
                return Bukkit.getOnlinePlayers().stream().map(HumanEntity::getName).collect(Collectors.toSet());
            });

            handler.getTranslator().add(new CommandMessages());
            handler.setLocale(new Locale("en", "US"));
        }
        handler.unregisterAllCommands();

        handler.register(Orphans.path(CONFIG.getStringList("command-aliases").toArray(String[]::new)).handler(new Commands()));
        handler.registerBrigadier();
    }
}
