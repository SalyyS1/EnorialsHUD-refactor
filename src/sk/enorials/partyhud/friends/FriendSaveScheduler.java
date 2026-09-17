/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.partyhud.friends;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.partyhud.friends.FriendConfig;
import sk.enorials.partyhud.friends.FriendService;

public final class FriendSaveScheduler {
    private static final long SAVE_DELAY_TICKS = 40L;
    private static final Map<FriendService, BukkitTask> TASKS = new ConcurrentHashMap<FriendService, BukkitTask>();
    private static volatile Field pluginField;

    private FriendSaveScheduler() {
    }

    public static void queue(FriendService friendService) {
        Plugin plugin;
        if (friendService != null && (plugin = FriendSaveScheduler.getPlugin(friendService)) != null && plugin.isEnabled() && FriendConfig.enabled(plugin)) {
            TASKS.compute(friendService, (friendService2, bukkitTask) -> bukkitTask != null && !bukkitTask.isCancelled() ? bukkitTask : Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                TASKS.remove(friendService2);
                try {
                    friendService.save();
                }
                catch (Throwable throwable) {
                    plugin.getLogger().warning("Failed to save friends asynchronously: " + throwable.getMessage());
                }
            }, 40L));
        }
    }

    private static Plugin getPlugin(FriendService friendService) {
        Field field = pluginField;
        if (field == null) {
            pluginField = field = FriendSaveScheduler.resolvePluginField(friendService.getClass());
        }
        if (field == null) {
            return null;
        }
        try {
            Object object = field.get(friendService);
            return object instanceof Plugin ? (Plugin)object : null;
        }
        catch (IllegalAccessException illegalAccessException) {
            return null;
        }
    }

    private static Field resolvePluginField(Class<?> clazz) {
        try {
            Field field = clazz.getDeclaredField("plugin");
            field.setAccessible(true);
            return field;
        }
        catch (Throwable throwable) {
            return null;
        }
    }
}

