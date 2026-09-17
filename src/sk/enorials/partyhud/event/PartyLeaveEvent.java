/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package sk.enorials.partyhud.event;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PartyLeaveEvent
extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID partyId;
    private final Player player;

    public PartyLeaveEvent(UUID uUID, Player player) {
        this.partyId = uUID;
        this.player = player;
    }

    public UUID getPartyId() {
        return this.partyId;
    }

    public Player getPlayer() {
        return this.player;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

