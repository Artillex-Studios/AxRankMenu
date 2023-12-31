package com.artillexstudios.axrankmenu;

import com.artillexstudios.axapi.AxPlugin;
import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.dvs.versioning.BasicVersioning;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.libs.org.snakeyaml.engine.v2.common.ScalarStyle;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.route.Route;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axapi.scheduler.ScheduledTask;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.MessageUtils;
import com.artillexstudios.axrankmenu.commands.MainCommand;
import com.artillexstudios.axrankmenu.commands.TabComplete;
import com.artillexstudios.axrankmenu.hooks.HookManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.function.Consumer;

public final class AxRankMenu extends AxPlugin {
    private static AxPlugin instance;
    public static Config CONFIG;
    public static Config LANG;
    public static MessageUtils MESSAGEUTILS;

    public static AxPlugin getInstance() {
        return instance;
    }

    public void enable() {
        instance = this;

        int pluginId = 20079;
        new Metrics(this, pluginId);

        CONFIG = new Config(new File(getDataFolder(), "config.yml"), getResource("config.yml"), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().addIgnoredRoute("2", Route.from("menu")).setVersioning(new BasicVersioning("version")).build());
        LANG = new Config(new File(getDataFolder(), "lang.yml"), getResource("lang.yml"), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.builder().setScalarStyle(ScalarStyle.DOUBLE_QUOTED).build(), UpdaterSettings.builder().setVersioning(new BasicVersioning("version")).build());

        MESSAGEUTILS = new MessageUtils(LANG.getBackingDocument(), "prefix", CONFIG.getBackingDocument());

        final MainCommand command = new MainCommand();
        this.getCommand("axrankmenu").setExecutor(command);
        Bukkit.getPluginCommand("axrankmenu").setTabCompleter(new TabComplete());

        Scheduler.get().run(task -> new HookManager().updateHooks());
    }

    public void disable() {
    }
}
