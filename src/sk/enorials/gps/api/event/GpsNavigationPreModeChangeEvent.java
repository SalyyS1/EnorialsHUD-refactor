/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.HandlerList
 */
package sk.enorials.gps.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.api.event.GpsPlayerEvent;

public final class GpsNavigationPreModeChangeEvent
extends GpsPlayerEvent
implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private final NavigationMode oldMode;
    private NavigationMode newMode;

    public GpsNavigationPreModeChangeEvent(Player player, NavigationMode navigationMode, NavigationMode navigationMode2) {
        super(player);
        this.oldMode = navigationMode;
        this.newMode = navigationMode2;
    }

    public NavigationMode getOldMode() {
        return this.oldMode;
    }

    public NavigationMode getNewMode() {
        return this.newMode;
    }

    public void setNewMode(NavigationMode navigationMode) {
        if (navigationMode != null) {
            this.newMode = navigationMode;
        }
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean bl) {
        this.cancelled = bl;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

