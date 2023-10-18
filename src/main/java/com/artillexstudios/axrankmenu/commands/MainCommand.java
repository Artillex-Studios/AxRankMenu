package com.artillexstudios.axrankmenu.commands;

import com.artillexstudios.axrankmenu.gui.RankGui;
import com.artillexstudios.axrankmenu.hooks.HookManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.artillexstudios.axrankmenu.AxRankMenu.CONFIG;
import static com.artillexstudios.axrankmenu.AxRankMenu.LANG;
import static com.artillexstudios.axrankmenu.AxRankMenu.MESSAGEUTILS;

public class MainCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("axrankmenu.reload")) {

            if (!CONFIG.reload()) {
                MESSAGEUTILS.sendLang(sender, "reload.failed", Map.of("%file%", "tiers.yml"));
                return true;
            }

            if (!LANG.reload()) {
                MESSAGEUTILS.sendLang(sender, "reload.failed", Map.of("%file%", "lang.yml"));
                return true;
            }

            new HookManager().updateHooks();

            MESSAGEUTILS.sendLang(sender, "reload.success");
            return true;
        }

        new RankGui().openFor((Player) sender);
        return true;
    }
}
