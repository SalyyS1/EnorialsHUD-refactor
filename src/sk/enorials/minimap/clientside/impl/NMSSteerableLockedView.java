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
 *  org.bukkit.event.player.PlayerToggleSneakEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.minimap.clientside.impl;

import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.clientside.SteerableLockedView;

public class NMSSteerableLockedView
implements SteerableLockedView,
Listener {
    private final Player player;
    private Consumer<Void> sneakCallback;
    private boolean active;

    public NMSSteerableLockedView(Player player) {
        this.player = player;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)EnorialsMinimap.get().owner());
        this.active = true;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent playerToggleSneakEvent) {
        if (this.active && playerToggleSneakEvent.getPlayer().getUniqueId().equals(this.player.getUniqueId()) && playerToggleSneakEvent.isSneaking() && this.sneakCallback != null) {
            this.sneakCallback.accept(null);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        if (playerQuitEvent.getPlayer().getUniqueId().equals(this.player.getUniqueId())) {
            this.destroy();
        }
    }

    @Override
    public void onSneak(Consumer<Void> consumer) {
        this.sneakCallback = consumer;
    }

    @Override
    public void destroy() {
        if (this.active) {
            this.active = false;
            HandlerList.unregisterAll((Listener)this);
        }
    }
}

