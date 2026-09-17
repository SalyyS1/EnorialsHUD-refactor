/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerExpChangeEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.sharedexp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.plugin.Plugin;
import sk.enorials.partyhud.party.PartyService;
import sk.enorials.partyhud.sharedexp.SharedExpHandler;

public class SharedExpService
implements Listener {
    private final Plugin plugin;
    private final PartyService partyService;
    private final Set<UUID> awardingGuard = Collections.synchronizedSet(new HashSet());

    public SharedExpService(Plugin plugin, PartyService partyService) {
        this.plugin = plugin;
        this.partyService = partyService;
    }

    @EventHandler
    public void onExp(PlayerExpChangeEvent playerExpChangeEvent) {
        SharedExpHandler.handle(this.plugin, this.partyService, playerExpChangeEvent, this.awardingGuard);
    }
}

