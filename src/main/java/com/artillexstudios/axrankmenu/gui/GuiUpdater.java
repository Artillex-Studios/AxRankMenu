package com.artillexstudios.axrankmenu.gui;

import com.artillexstudios.axapi.scheduler.ScheduledTask;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axrankmenu.gui.impl.RankGui;

public final class GuiUpdater {
    private static ScheduledTask task;

    private GuiUpdater() {
    }

    public static void start() {
        stop();
        task = Scheduler.get().runTimer(() -> {
            for (RankGui gui : RankGui.getOpenMenus()) gui.refresh();
        }, 20L, 20L);
    }

    public static void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }
}
