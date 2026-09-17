/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 */
package sk.enorials.gps.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import sk.enorials.gps.nav.NavigationManager;

public final class PlayerSessionListener
implements Listener {
    private final NavigationManager navigationManager;

    public PlayerSessionListener(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        this.navigationManager.tryResume(playerJoinEvent.getPlayer());
    }
}

