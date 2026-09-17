/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.ArmorStand
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerChangedWorldEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.projectiles.ProjectileSource
 */
package sk.enorials.minimap.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapProvider;
import sk.enorials.minimap.radar.RadarCategory;
import sk.enorials.minimap.radar.RadarClassifier;

public final class VisualSettingsManager
implements Listener {
    private final EnorialsMinimap plugin;
    private final RadarClassifier classifier = new RadarClassifier();
    private final Map<UUID, CoordinateSample> coordinates = new HashMap<UUID, CoordinateSample>();

    public VisualSettingsManager(EnorialsMinimap enorialsMinimap) {
        this.plugin = enorialsMinimap;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onCombat(EntityDamageByEntityEvent entityDamageByEntityEvent) {
        Entity entity;
        Projectile projectile;
        ProjectileSource projectileSource;
        if (entityDamageByEntityEvent.getFinalDamage() <= 0.0) {
            return;
        }
        Entity entity2 = entityDamageByEntityEvent.getDamager();
        if (entity2 instanceof Projectile && (projectileSource = (projectile = (Projectile)entity2).getShooter()) instanceof Entity) {
            entity2 = entity = (Entity)projectileSource;
        }
        if (!(entity2 instanceof LivingEntity) || entity2 instanceof ArmorStand || entity2.equals((Object)entityDamageByEntityEvent.getEntity())) {
            return;
        }
        entity = entityDamageByEntityEvent.getEntity();
        if (entity instanceof Player) {
            projectile = (Player)entity;
            this.mark((Player)projectile);
        }
        if (entity2 instanceof Player) {
            projectile = (Player)entity2;
            if (entityDamageByEntityEvent.getEntity() instanceof Player || this.classifier.classify((Player)projectile, entityDamageByEntityEvent.getEntity()) == RadarCategory.AGGRESSIVE) {
                this.mark((Player)projectile);
            }
        }
    }

    private void mark(Player player) {
        Minimap minimap = this.plugin.minimapListener().getPlayerMinimaps().get(player.getUniqueId());
        if (minimap != null && minimap.markCombat(player.getWorld().getGameTime())) {
            minimap.update(this.plugin);
        }
    }

    public void tick() {
        Map<UUID, Minimap> map = this.plugin.minimapListener().getPlayerMinimaps();
        this.coordinates.keySet().retainAll(map.keySet());
        for (Minimap minimap : List.copyOf(map.values())) {
            Player player = minimap.holder();
            if (!player.isOnline()) continue;
            long l = player.getWorld().getGameTime();
            boolean bl = minimap.expireCombat(l);
            if (bl |= minimap.settleCombatZoom(l)) {
                minimap.update(this.plugin);
            }
            if (!minimap.coordinatesEnabled()) {
                this.coordinates.remove(player.getUniqueId());
                continue;
            }
            Location location = player.getLocation();
            CoordinateSample coordinateSample = new CoordinateSample(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
            if (coordinateSample.equals(this.coordinates.put(player.getUniqueId(), coordinateSample))) continue;
            minimap.updateSecondaryLayer((MinimapProvider)this.plugin, "__coords__");
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent playerChangedWorldEvent) {
        Minimap minimap = this.plugin.minimapListener().getPlayerMinimaps().get(playerChangedWorldEvent.getPlayer().getUniqueId());
        if (minimap != null) {
            minimap.resetCombatZoom(playerChangedWorldEvent.getPlayer().getWorld().getGameTime());
            minimap.update(this.plugin);
        }
        this.coordinates.remove(playerChangedWorldEvent.getPlayer().getUniqueId());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent playerDeathEvent) {
        Minimap minimap = this.plugin.minimapListener().getPlayerMinimaps().get(playerDeathEvent.getEntity().getUniqueId());
        if (minimap != null && minimap.endCombat(playerDeathEvent.getEntity().getWorld().getGameTime())) {
            minimap.update(this.plugin);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        this.coordinates.remove(playerQuitEvent.getPlayer().getUniqueId());
    }

    private record CoordinateSample(UUID world, int x, int y, int z) {
    }
}

