/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.radar;

import java.lang.invoke.CallSite;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapProvider;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.radar.RadarCategory;
import sk.enorials.minimap.radar.RadarClassifier;
import sk.enorials.minimap.radar.RadarLayer;

public final class EntityRadar {
    private final EnorialsMinimap plugin;
    private final RadarClassifier classifier = new RadarClassifier();
    private final Map<UUID, Pool> pools = new HashMap<UUID, Pool>();

    public EntityRadar(EnorialsMinimap enorialsMinimap) {
        this.plugin = enorialsMinimap;
    }

    public RadarClassifier classifier() {
        return this.classifier;
    }

    public void tick() {
        this.pools.keySet().removeIf(uUID -> Bukkit.getPlayer((UUID)uUID) == null);
        for (Minimap minimap : List.copyOf(this.plugin.minimapListener().getPlayerMinimaps().values())) {
            this.refresh(minimap);
        }
    }

    public void refresh(Minimap minimap) {
        SecondaryMinimapLayer secondaryMinimapLayer;
        Player player = minimap.holder();
        HashSet<CallSite> hashSet = new HashSet<CallSite>();
        ArrayList<Candidate> arrayList = new ArrayList<Candidate>();
        Pool pool = this.pools.computeIfAbsent(player.getUniqueId(), uUID -> new Pool());
        if (pool.owner != minimap) {
            pool.available.clear();
            pool.available.addAll(pool.allocated);
            pool.owner = minimap;
        }
        Config.Radar radar = Config.instance().radar;
        if (player.isOnline() && radar.enabled && HudAccess.map(player) && HudAccess.anyRadar(player) && minimap.radarMasterEnabled() && minimap.anyRadarEnabled()) {
            Iterator<String> iterator = player.getLocation();
            double d = Math.max(1, Math.min(64, radar.radiusBlocks));
            double d2 = Math.max(1, Math.min(128, radar.verticalRangeBlocks));
            int n = Math.max(1, Math.min(96, radar.maxEntities));
            ArrayList<Nearby> arrayList2 = new ArrayList<Nearby>();
            for (Entity entity : player.getNearbyEntities(d, d2, d)) {
                if (!(entity instanceof LivingEntity)) continue;
                Location location = entity.getLocation();
                arrayList2.add(new Nearby(entity, location, location.distanceSquared(iterator)));
            }
            arrayList2.sort(Comparator.comparingDouble(Nearby::distanceSquared));
            long l = player.getWorld().getGameTime();
            for (Nearby nearby : arrayList2) {
                RadarCategory radarCategory;
                double d3;
                if (hashSet.size() >= n) break;
                Entity entity = nearby.entity();
                Location location = nearby.location();
                double d4 = location.getX() - iterator.getX();
                if (d4 * d4 + (d3 = location.getZ() - iterator.getZ()) * d3 > d * d || (radarCategory = this.classifier.classify(player, entity)) == null || !HudAccess.has((CommandSender)player, HudAccess.radar(radarCategory)) || !minimap.radarEnabled(radarCategory)) continue;
                String string = "__radar::" + String.valueOf(entity.getUniqueId());
                hashSet.add((CallSite)((Object)string));
                arrayList.add(new Candidate(string, location, radarCategory, l));
            }
        }
        for (String string : List.copyOf(minimap.secondaryLayers().keySet())) {
            if (!string.startsWith("__radar::") || hashSet.contains(string)) continue;
            secondaryMinimapLayer = (SecondaryMinimapLayer)minimap.secondaryLayers().remove(string);
            this.plugin.packetSender().despawnLayer(player, secondaryMinimapLayer.getBaseLayer());
            if (!(secondaryMinimapLayer instanceof RadarLayer)) continue;
            RadarLayer radarLayer = (RadarLayer)secondaryMinimapLayer;
            pool.available.addLast(radarLayer);
        }
        for (Candidate candidate : arrayList) {
            secondaryMinimapLayer = minimap.secondaryLayers().get(candidate.key());
            if (secondaryMinimapLayer instanceof RadarLayer) {
                RadarLayer radarLayer;
                var10_13 = radarLayer = (RadarLayer)secondaryMinimapLayer;
                if (!candidate.location().getWorld().equals((Object)var10_13.getWorld())) {
                    this.plugin.packetSender().despawnLayer(player, var10_13.getBaseLayer());
                    var10_13.reuse(candidate.location(), candidate.category(), candidate.tick());
                    this.plugin.packetSender().spawnLayer(player, var10_13.getBaseLayer());
                } else {
                    var10_13.sample(candidate.location(), candidate.category(), candidate.tick());
                }
            } else {
                var10_13 = pool.available.pollFirst();
                if (var10_13 == null) {
                    if (pool.allocated.size() >= 96) continue;
                    MinimapLayer minimapLayer = this.plugin.clientsideMinimapFactory().createMinimapLayer(player.getWorld(), null);
                    var10_13 = new RadarLayer(minimapLayer, candidate.location(), candidate.category(), candidate.tick());
                    pool.allocated.add(var10_13);
                } else {
                    var10_13.reuse(candidate.location(), candidate.category(), candidate.tick());
                }
                minimap.secondaryLayers().put(candidate.key(), var10_13);
                this.plugin.packetSender().spawnLayer(player, var10_13.getBaseLayer());
            }
            minimap.updateSecondaryLayer((MinimapProvider)this.plugin, candidate.key());
        }
    }

    public int allocatedLayers(Player player) {
        Pool pool = this.pools.get(player.getUniqueId());
        return pool == null ? 0 : pool.allocated.size();
    }

    private static final class Pool {
        Minimap owner;
        final List<RadarLayer> allocated = new ArrayList<RadarLayer>();
        final ArrayDeque<RadarLayer> available = new ArrayDeque();

        private Pool() {
        }
    }

    private record Nearby(Entity entity, Location location, double distanceSquared) {
    }

    private record Candidate(String key, Location location, RadarCategory category, long tick) {
    }
}

