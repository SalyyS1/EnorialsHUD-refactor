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
import sk.enorials.gps.nav.RouteDescriptor;

public final class GpsNavigationPreStartEvent
extends GpsPlayerEvent
implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private NavigationMode mode;
    private String source;
    private final String targetId;
    private final String targetDisplayName;
    private final RouteDescriptor descriptor;

    public GpsNavigationPreStartEvent(Player player, NavigationMode navigationMode, String string, String string2, String string3, RouteDescriptor routeDescriptor) {
        super(player);
        this.mode = navigationMode;
        this.source = string;
        this.targetId = string2;
        this.targetDisplayName = string3;
        this.descriptor = routeDescriptor;
    }

    public NavigationMode getMode() {
        return this.mode;
    }

    public void setMode(NavigationMode navigationMode) {
        if (navigationMode != null) {
            this.mode = navigationMode;
        }
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String string) {
        if (string != null) {
            this.source = string;
        }
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

