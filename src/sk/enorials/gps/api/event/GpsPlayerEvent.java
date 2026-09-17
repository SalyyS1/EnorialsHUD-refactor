/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 */
package sk.enorials.gps.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public abstract class GpsPlayerEvent
extends Event {
    private final Player player;

    protected GpsPlayerEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }
}

