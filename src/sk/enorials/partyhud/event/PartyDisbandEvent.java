/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package sk.enorials.partyhud.event;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PartyDisbandEvent
extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID partyId;

    public PartyDisbandEvent(UUID uUID) {
        this.partyId = uUID;
    }

    public UUID getPartyId() {
        return this.partyId;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

