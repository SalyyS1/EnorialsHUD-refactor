/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.projectiles.ProjectileSource
 */
package sk.enorials.partyhud.party;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import sk.enorials.partyhud.party.Party;
import sk.enorials.partyhud.party.PartyService;

public final class PartyFriendlyFireListener
implements Listener {
    private final PartyService partyService;

    public PartyFriendlyFireListener(PartyService partyService) {
        this.partyService = partyService;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onDamage(EntityDamageByEntityEvent entityDamageByEntityEvent) {
        Entity entity;
        if (this.partyService != null && (entity = entityDamageByEntityEvent.getEntity()) instanceof Player) {
            Optional<Party> optional;
            Optional<Party> optional2;
            Player player = (Player)entity;
            entity = PartyFriendlyFireListener.resolveAttacker(entityDamageByEntityEvent.getDamager());
            if (entity != null && !entity.getUniqueId().equals(player.getUniqueId()) && (optional2 = this.partyService.getPartyOf(entity.getUniqueId())).isPresent() && (optional = this.partyService.getPartyOf(player.getUniqueId())).isPresent()) {
                UUID uUID = optional2.get().getId();
                UUID uUID2 = optional.get().getId();
                if (uUID != null && uUID.equals(uUID2)) {
                    entityDamageByEntityEvent.setCancelled(true);
                }
            }
        }
    }

    private static Player resolveAttacker(Entity entity) {
        ProjectileSource projectileSource;
        if (entity instanceof Player) {
            return (Player)entity;
        }
        return entity instanceof Projectile && (projectileSource = ((Projectile)entity).getShooter()) instanceof Player ? (Player)projectileSource : null;
    }
}

