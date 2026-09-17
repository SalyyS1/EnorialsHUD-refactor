/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.manager;

import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import sk.enorials.common.pack.BoundedWorkQueue;
import sk.enorials.hud.EnorialsHUD;

public final class PackBuildQueue
implements Listener,
AutoCloseable {
    private final EnorialsHUD plugin;
    private final BoundedWorkQueue<Object> queue;
    private volatile boolean closed;

    public PackBuildQueue(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
        this.queue = new BoundedWorkQueue(this.maximum(), throwable -> {
            if (!this.closed) {
                enorialsHUD.getLogger().log(Level.WARNING, "Resource pack job failed", (Throwable)throwable);
            }
        });
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
    }

    private int maximum() {
        return Math.max(1, this.plugin.cfg().getInt("pack-build.max-concurrent-builds", 1));
    }

    public void reload() {
        this.queue.limit(this.maximum());
    }

    public boolean isClosed() {
        return this.closed;
    }

    public int activeCount() {
        return this.queue.activeCount();
    }

    public int waitingCount() {
        return this.queue.waitingCount();
    }

    public boolean submit(Object object, Player player, Runnable runnable) {
        return this.queue.submit(object, () -> {
            try {
                runnable.run();
            }
            catch (RuntimeException runtimeException) {
                this.sync(() -> {
                    if (player != null && player.isOnline()) {
                        this.plugin.packLoadGuard().fail(player, "Resource pack preparation failed. Please reconnect.");
                    }
                });
                throw runtimeException;
            }
        }, (int n) -> this.sync(() -> {
            if (player != null && player.isOnline()) {
                this.plugin.packLoadGuard().queuePosition(player, n);
            }
        }));
    }

    private void sync(Runnable runnable) {
        if (this.closed || !this.plugin.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (!this.closed) {
                    runnable.run();
                }
            });
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        this.queue.cancel(playerQuitEvent.getPlayer());
    }

    @Override
    public void close() {
        this.closed = true;
        HandlerList.unregisterAll((Listener)this);
        this.queue.close();
    }
}

