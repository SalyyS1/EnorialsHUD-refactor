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
import sk.enorials.gps.nav.RouteDescriptor;

public final class GpsNavigationStopEvent
extends GpsPlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final NavigationMode mode;
    private final String source;
    private final String reason;
    private final String targetId;
    private final String targetDisplayName;
    private final RouteDescriptor descriptor;

    public GpsNavigationStopEvent(Player player, NavigationMode navigationMode, String string, String string2, String string3, String string4, RouteDescriptor routeDescriptor) {
        super(player);
        this.mode = navigationMode;
        this.source = string;
        this.reason = string2;
        this.targetId = string3;
        this.targetDisplayName = string4;
        this.descriptor = routeDescriptor;
    }

    public NavigationMode getMode() {
        return this.mode;
    }

    public String getSource() {
        return this.source;
    }

    public String getReason() {
        return this.reason;
    }

    public String getTargetId() {
        return this.targetId;
    }

    public String getTargetDisplayName() {
        return this.targetDisplayName;
    }

    public RouteDescriptor getDescriptor() {
        return this.descriptor;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

