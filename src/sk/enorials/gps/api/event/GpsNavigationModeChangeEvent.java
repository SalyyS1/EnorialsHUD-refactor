/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.HandlerList
 */
package sk.enorials.gps.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.api.event.GpsPlayerEvent;

public final class GpsNavigationModeChangeEvent
extends GpsPlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final NavigationMode oldMode;
    private final NavigationMode newMode;

    public GpsNavigationModeChangeEvent(Player player, NavigationMode navigationMode, NavigationMode navigationMode2) {
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

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

