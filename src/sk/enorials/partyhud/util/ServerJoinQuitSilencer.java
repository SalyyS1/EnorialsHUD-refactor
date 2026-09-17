/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 */
package sk.enorials.partyhud.util;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ServerJoinQuitSilencer
implements Listener {
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        playerJoinEvent.setJoinMessage(null);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        playerQuitEvent.setQuitMessage(null);
    }
}

