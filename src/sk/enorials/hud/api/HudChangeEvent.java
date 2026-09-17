/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package sk.enorials.hud.api;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class HudChangeEvent
extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Kind kind;
    private final UUID playerId;
    private final String key;

    public HudChangeEvent(Kind kind, UUID uUID, String string) {
        this.kind = kind;
        this.playerId = uUID;
        this.key = string;
    }

    public Kind kind() {
        return this.kind;
    }

    public UUID playerId() {
        return this.playerId;
    }

    public String key() {
        return this.key;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public static enum Kind {
        SETTING,
        MARKER_PUT,
        MARKER_REMOVE,
        PARTY_CREATED,
        PARTY_JOINED,
        PARTY_LEFT,
        PARTY_DISBANDED,
        PARTY_UPDATED,
        NAVIGATION_STARTED,
        NAVIGATION_STOPPED;

    }
}

