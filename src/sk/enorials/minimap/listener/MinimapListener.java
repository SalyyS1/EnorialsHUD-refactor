/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
 *  it.unimi.dsi.fastutil.ints.IntIntImmutablePair
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerChangedWorldEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 *  org.jetbrains.annotations.Nullable
 */
package sk.enorials.minimap.listener;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.compat.OptionalPlayerEvents;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.clientside.ClientsideMinimapFactory;
import sk.enorials.minimap.clientside.SteerableLockedView;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapProvider;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.fullscreen.FullscreenMinimap;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.marker.MarkerMinimapLayer;
import sk.enorials.minimap.map.renderer.CoordinatesOverlayRenderer;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.minimap.map.renderer.MinimapLayerRenderer;
import sk.enorials.minimap.map.renderer.SecondaryMinimapLayerRenderer;
import sk.enorials.minimap.map.renderer.world.WorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.cache.CacheableWorldMinimapRenderer;

public class MinimapListener
implements Listener {
    private final Map<UUID, Minimap> playerMinimaps = new HashMap<UUID, Minimap>();
    private final Map<UUID, FullscreenMinimap> fullscreenMinimaps = new HashMap<UUID, FullscreenMinimap>();
    private final Map<UUID, Location> fullscreenOrigins = new HashMap<UUID, Location>();
    private final Map<UUID, SteerableLockedView> lockedViews = new HashMap<UUID, SteerableLockedView>();
    private final Map<UUID, IntIntImmutablePair> playerSections = new HashMap<UUID, IntIntImmutablePair>();
    private final Map<UUID, Long> nextPositionUpdateAt = new HashMap<UUID, Long>();
    private final Set<UUID> queuedPositionUpdates = new HashSet<UUID>();
    private final Map<UUID, Long> nextOrientationUpdateAt = new HashMap<UUID, Long>();
    private final Set<UUID> requestedUpdates = new HashSet<UUID>();
    private final Set<UUID> requestedOrientationUpdates = new HashSet<UUID>();
    private final Map<UUID, BukkitTask> pendingCarrierRespawns = new HashMap<UUID, BukkitTask>();
    private final EnorialsMinimap plugin;

    public MinimapListener(EnorialsMinimap enorialsMinimap) {
        this.plugin = enorialsMinimap;
        OptionalPlayerEvents.register((Plugin)enorialsMinimap.owner(), this, "io.papermc.paper.event.player.PlayerClientLoadedWorldEvent", this::queueCarrierRespawn);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        if (Config.instance().resourcePack.enabled) {
            return;
        }
        Player player = playerJoinEvent.getPlayer();
        Bukkit.getScheduler().runTask((Plugin)this.plugin.owner(), () -> {
            if (!player.isOnline()) {
                return;
            }
            try {
                this.plugin.playerDataStorage().restore(this.plugin, player);
            }
            catch (Exception exception) {
                this.plugin.getLogger().log(Level.WARNING, "Could not restore minimap for " + String.valueOf(player.getUniqueId()), exception);
            }
        });
    }

    public Minimap enableMinimap(Player player) {
        Object object;
        HudAccess.require((CommandSender)player, "enorials.minimap.use");
        UUID uUID = player.getUniqueId();
        Minimap minimap = this.playerMinimaps.get(uUID);
        if (minimap != null) {
            return minimap;
        }
        Config config = Config.instance();
        WorldMinimapRenderer worldMinimapRenderer = this.plugin.worldRenderer();
        ClientsideMinimapFactory clientsideMinimapFactory = this.plugin.clientsideMinimapFactory();
        Minimap minimap2 = clientsideMinimapFactory.createMinimap(player, config.defaultPosition, worldMinimapRenderer);
        minimap2.size(config.defaultSize);
        minimap2.shape(config.defaultShape);
        minimap2.restoreDangerRing(config.defaultDangerRingEnabled);
        minimap2.fogStylingEnabled(config.defaultFogStylingEnabled);
        this.playerMinimaps.put(uUID, minimap2);
        this.plugin.packetSender().spawnMinimap(minimap2);
        if (config.playerIconEnabled && (object = this.plugin.iconProvider().getIcon("player")) != null) {
            this.addBuiltin(minimap2, "player", new MinimapIconRenderer((MinimapIcon)object, this.plugin.iconProvider().getIcon("offscreen_player"), true), 0.4f);
        }
        if (config.coordinates.enabled) {
            this.addBuiltin(minimap2, "__coords__", new CoordinatesOverlayRenderer(config.coordinates.textY), 0.45f);
        }
        if (worldMinimapRenderer instanceof CacheableWorldMinimapRenderer) {
            object = (CacheableWorldMinimapRenderer)worldMinimapRenderer;
            object.getWorldMapCache().setCallback(uUID, vector4ic -> {
                boolean bl;
                Location location = player.getLocation();
                boolean bl2 = bl = (double)vector4ic.x() < location.getX() + 64.0 && (double)vector4ic.y() < location.getZ() + 64.0 && (double)(vector4ic.x() + vector4ic.z()) > location.getX() - 64.0 && (double)(vector4ic.y() + vector4ic.w()) > location.getZ() - 64.0;
                if (bl && this.requestedUpdates.add(uUID)) {
                    Bukkit.getScheduler().runTask((Plugin)this.plugin.owner(), () -> {
                        if (this.requestedUpdates.remove(uUID) && this.playerMinimaps.get(uUID) == minimap2 && player.isOnline()) {
                            Location location = player.getLocation();
                            minimap2.update(this.plugin, location.getX(), location.getZ(), false);
                        }
                    });
                }
            });
        }
        object = player.getLocation();
        minimap2.update(this.plugin, object.getX(), object.getZ(), true);
        this.plugin.menuListener().applyPublicMarkers(minimap2);
        this.plugin.menuListener().applyHiddenMarkers(minimap2);
        return minimap2;
    }

    @Nullable
    public Minimap disableMinimap(Player player) {
        BukkitTask bukkitTask = this.pendingCarrierRespawns.remove(player.getUniqueId());
        if (bukkitTask != null) {
            bukkitTask.cancel();
        }
        this.requestedUpdates.remove(player.getUniqueId());
        this.playerSections.remove(player.getUniqueId());
        this.nextPositionUpdateAt.remove(player.getUniqueId());
        this.queuedPositionUpdates.remove(player.getUniqueId());
        this.nextOrientationUpdateAt.remove(player.getUniqueId());
        this.requestedOrientationUpdates.remove(player.getUniqueId());
        Minimap minimap = this.playerMinimaps.remove(player.getUniqueId());
        if (minimap != null) {
            MinimapLayerRenderer minimapLayerRenderer = minimap.primaryLayer().renderer();
            if (minimapLayerRenderer instanceof CacheableWorldMinimapRenderer) {
                CacheableWorldMinimapRenderer cacheableWorldMinimapRenderer = (CacheableWorldMinimapRenderer)minimapLayerRenderer;
                cacheableWorldMinimapRenderer.getWorldMapCache().releaseViewer(player.getUniqueId());
            }
            this.plugin.packetSender().despawnMinimap(minimap);
        }
        this.closeFullscreen(player);
        return minimap;
    }

    public SteerableLockedView openFullscreen(FullscreenMinimap fullscreenMinimap) {
        HudAccess.require((CommandSender)fullscreenMinimap.getHolder(), "enorials.minimap.use");
        HudAccess.require((CommandSender)fullscreenMinimap.getHolder(), "enorials.minimap.fullscreen");
        SteerableLockedView steerableLockedView = this.plugin.steerableViewFactory().lockedView(fullscreenMinimap.getHolder());
        fullscreenMinimap.spawn(this.plugin);
        this.fullscreenMinimaps.put(fullscreenMinimap.getHolder().getUniqueId(), fullscreenMinimap);
        this.fullscreenOrigins.put(fullscreenMinimap.getHolder().getUniqueId(), fullscreenMinimap.getHolder().getLocation().clone());
        this.lockedViews.put(fullscreenMinimap.getHolder().getUniqueId(), steerableLockedView);
        return steerableLockedView;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void closeFullscreen(Player player) {
        UUID uUID = player.getUniqueId();
        this.fullscreenOrigins.remove(uUID);
        FullscreenMinimap fullscreenMinimap = this.fullscreenMinimaps.remove(uUID);
        SteerableLockedView steerableLockedView = this.lockedViews.remove(uUID);
        try {
            if (steerableLockedView != null) {
                steerableLockedView.destroy();
            }
        }
        finally {
            if (fullscreenMinimap != null) {
                fullscreenMinimap.despawn(this.plugin, null);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent playerDeathEvent) {
        if (!Config.instance().markers.deathMarker.enabled) {
            return;
        }
        Player player = playerDeathEvent.getPlayer();
        Minimap minimap = this.playerMinimaps.get(player.getUniqueId());
        if (minimap == null) {
            return;
        }
        minimap.setDeathPoint(this.plugin, player.getLocation());
        this.closeFullscreen(player);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent playerMoveEvent) {
        UUID uUID;
        Minimap minimap;
        boolean bl = playerMoveEvent.hasChangedPosition();
        boolean bl2 = playerMoveEvent.hasChangedOrientation();
        if ((bl || bl2) && (minimap = this.playerMinimaps.get(uUID = playerMoveEvent.getPlayer().getUniqueId())) != null) {
            if (!bl) {
                this.queuePlayerIconUpdate(uUID, true);
            } else {
                Location location = playerMoveEvent.getTo().clone().subtract(playerMoveEvent.getFrom());
                location.setY(0.0);
                if (location.lengthSquared() != 0.0) {
                    if (this.shouldRunPositionUpdateNow(uUID)) {
                        this.runPositionUpdate(playerMoveEvent.getPlayer(), minimap);
                    } else if (this.queuedPositionUpdates.add(uUID)) {
                        long l = Math.max(1L, this.remainingPositionDelayTicks(uUID));
                        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin.owner(), () -> {
                            this.queuedPositionUpdates.remove(uUID);
                            Minimap minimap = this.playerMinimaps.get(uUID);
                            Player player = Bukkit.getPlayer((UUID)uUID);
                            if (minimap != null && player != null && player.isOnline()) {
                                this.runPositionUpdate(player, minimap);
                            }
                        }, l);
                    }
                    if (bl2) {
                        this.queuePlayerIconUpdate(uUID, true);
                    }
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onDeathMarkerMove(PlayerMoveEvent playerMoveEvent) {
        if (!playerMoveEvent.hasChangedPosition() || playerMoveEvent.getPlayer().isDead()) {
            return;
        }
        Minimap minimap = this.playerMinimaps.get(playerMoveEvent.getPlayer().getUniqueId());
        if (minimap == null) {
            return;
        }
        try {
            minimap.removeDeathPointIfReached(this.plugin, playerMoveEvent.getTo());
        }
        catch (SQLException sQLException) {
            this.plugin.getLogger().warning("Could not remove recovered death marker for " + String.valueOf(playerMoveEvent.getPlayer().getUniqueId()) + ": " + sQLException.getMessage());
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onFullscreenMove(PlayerMoveEvent playerMoveEvent) {
        this.closeFullscreenAfterMovement(playerMoveEvent.getPlayer(), playerMoveEvent.getTo());
    }

    private void closeFullscreenAfterMovement(Player player, @Nullable Location location) {
        Location location2 = this.fullscreenOrigins.get(player.getUniqueId());
        if (location2 != null && MinimapListener.hasLeftFullscreenOrigin(location2, location)) {
            this.closeFullscreen(player);
        }
    }

    static boolean hasLeftFullscreenOrigin(Location location, @Nullable Location location2) {
        if (location2 == null) {
            return false;
        }
        if (!Objects.equals(location.getWorld(), location2.getWorld())) {
            return true;
        }
        return location.distanceSquared(location2) > 1.0E-4;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onTeleport(PlayerTeleportEvent playerTeleportEvent) {
        if (playerTeleportEvent.getTo() != null) {
            this.closeFullscreenAfterMovement(playerTeleportEvent.getPlayer(), playerTeleportEvent.getTo());
            this.queueCarrierRespawn(playerTeleportEvent.getPlayer());
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onRespawn(PlayerPostRespawnEvent playerPostRespawnEvent) {
        this.queueCarrierRespawn(playerPostRespawnEvent.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent playerChangedWorldEvent) {
        this.closeFullscreen(playerChangedWorldEvent.getPlayer());
        this.playerSections.remove(playerChangedWorldEvent.getPlayer().getUniqueId());
        this.queueCarrierRespawn(playerChangedWorldEvent.getPlayer());
    }

    private void queueCarrierRespawn(Player player) {
        UUID uUID = player.getUniqueId();
        Minimap minimap = this.playerMinimaps.get(uUID);
        if (minimap == null || this.pendingCarrierRespawns.containsKey(uUID)) {
            return;
        }
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater((Plugin)this.plugin.owner(), () -> {
            this.pendingCarrierRespawns.remove(uUID);
            Player player = Bukkit.getPlayer((UUID)uUID);
            if (player == null || !player.isOnline() || this.playerMinimaps.get(uUID) != minimap) {
                return;
            }
            this.playerSections.remove(uUID);
            this.requestedUpdates.remove(uUID);
            minimap.respawn(this.plugin);
            this.plugin.menuListener().applyPublicMarkers(minimap);
            this.plugin.menuListener().applyHiddenMarkers(minimap);
            this.plugin.entityRadar().refresh(minimap);
        }, 2L);
        this.pendingCarrierRespawns.put(uUID, bukkitTask);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        try {
            Minimap minimap = this.disableMinimap(playerQuitEvent.getPlayer());
            if (minimap != null) {
                this.plugin.playerDataStorage().save(minimap);
            }
        }
        catch (Throwable throwable) {
            throw new IllegalStateException("Minimap player data operation failed", throwable);
        }
    }

    private boolean shouldRunPositionUpdateNow(UUID uUID) {
        long l = System.currentTimeMillis();
        Long l2 = this.nextPositionUpdateAt.get(uUID);
        if (l2 != null && l < l2) {
            return false;
        }
        long l3 = (long)Math.max(1, Config.instance().performance.movementUpdateIntervalTicks) * 50L;
        this.nextPositionUpdateAt.put(uUID, l + l3);
        return true;
    }

    private long remainingPositionDelayTicks(UUID uUID) {
        long l = System.currentTimeMillis();
        long l2 = this.nextPositionUpdateAt.getOrDefault(uUID, l);
        long l3 = Math.max(50L, l2 - l);
        return (l3 + 49L) / 50L;
    }

    private void runPositionUpdate(Player player, Minimap minimap) {
        UUID uUID = player.getUniqueId();
        Location location = player.getLocation();
        IntIntImmutablePair intIntImmutablePair = this.playerSections.get(uUID);
        int n = location.getBlockX() >> 7;
        int n2 = location.getBlockZ() >> 7;
        boolean bl = intIntImmutablePair == null || n2 != intIntImmutablePair.rightInt() || n != intIntImmutablePair.leftInt();
        minimap.update(this.plugin, location.getX(), location.getZ(), bl);
        this.requestedUpdates.remove(uUID);
        if (bl) {
            this.playerSections.put(uUID, IntIntImmutablePair.of((int)n, (int)n2));
        }
    }

    private boolean shouldRunOrientationUpdateNow(UUID uUID) {
        long l = System.currentTimeMillis();
        Long l2 = this.nextOrientationUpdateAt.get(uUID);
        if (l2 != null && l < l2) {
            return false;
        }
        long l3 = (long)Math.max(1, Config.instance().performance.orientationUpdateIntervalTicks) * 50L;
        this.nextOrientationUpdateAt.put(uUID, l + l3);
        return true;
    }

    private long remainingOrientationDelayTicks(UUID uUID) {
        long l = System.currentTimeMillis();
        long l2 = this.nextOrientationUpdateAt.getOrDefault(uUID, l);
        long l3 = Math.max(50L, l2 - l);
        return (l3 + 49L) / 50L;
    }

    private void queuePlayerIconUpdate(UUID uUID, boolean bl) {
        if (bl && !this.shouldRunOrientationUpdateNow(uUID)) {
            if (this.requestedOrientationUpdates.add(uUID)) {
                long l = Math.max(1L, this.remainingOrientationDelayTicks(uUID));
                Bukkit.getScheduler().runTaskLater((Plugin)this.plugin.owner(), () -> {
                    this.requestedOrientationUpdates.remove(uUID);
                    Minimap minimap = this.playerMinimaps.get(uUID);
                    if (minimap != null) {
                        minimap.updateSecondaryLayer((MinimapProvider)this.plugin, "player");
                    }
                }, l);
            }
        } else if (this.requestedOrientationUpdates.add(uUID)) {
            Bukkit.getScheduler().runTask((Plugin)this.plugin.owner(), () -> {
                this.requestedOrientationUpdates.remove(uUID);
                Minimap minimap = this.playerMinimaps.get(uUID);
                if (minimap != null) {
                    minimap.updateSecondaryLayer((MinimapProvider)this.plugin, "player");
                }
            });
        }
    }

    public int cleanupExpiredMarkers() {
        int n = 0;
        long l = System.currentTimeMillis();
        for (Minimap minimap : this.playerMinimaps.values()) {
            n += minimap.removeExpiredTimedMarkers(this.plugin, l);
        }
        return n;
    }

    public Map<UUID, Minimap> getPlayerMinimaps() {
        return this.playerMinimaps;
    }

    public Map<UUID, FullscreenMinimap> getFullscreenMinimaps() {
        return this.fullscreenMinimaps;
    }

    private void addBuiltin(Minimap minimap, String string, SecondaryMinimapLayerRenderer secondaryMinimapLayerRenderer, float f) {
        MinimapLayer minimapLayer = this.plugin.clientsideMinimapFactory().createMinimapLayer(minimap.holder().getWorld(), null);
        SecondaryMinimapLayer secondaryMinimapLayer = string.equals("player") ? new MarkerMinimapLayer(minimapLayer, secondaryMinimapLayerRenderer, false, false, null, 64, 64, f) : new SecondaryMinimapLayer(minimapLayer, secondaryMinimapLayerRenderer, false, true, null, 64, 64, f);
        minimap.secondaryLayers().put(string, secondaryMinimapLayer);
        this.plugin.packetSender().spawnLayer(minimap.holder(), minimapLayer);
    }
}

