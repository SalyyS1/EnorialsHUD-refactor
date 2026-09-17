/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongCollection
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent$Builder
 *  net.kyori.adventure.text.format.Style
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.minimap.clientside.gui;

import it.unimi.dsi.fastutil.longs.LongCollection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.common.pack.ResourcePackGuard;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.avatar.SkinHead;
import sk.enorials.hud.compat.ClientProfiles;
import sk.enorials.hud.compat.ClientRenderProfile;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.party.PartyMapTracker;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.clientside.AbstractMinimapPacketSender;
import sk.enorials.minimap.clientside.gui.CameraHeadMapWire;
import sk.enorials.minimap.clientside.gui.GuiFullscreenBundles;
import sk.enorials.minimap.clientside.gui.GuiMinimapWire;
import sk.enorials.minimap.clientside.gui.GuiMotion;
import sk.enorials.minimap.clientside.gui.GuiOverlayHost;
import sk.enorials.minimap.clientside.gui.GuiPlayerPose;
import sk.enorials.minimap.clientside.gui.HeadMapWire;
import sk.enorials.minimap.clientside.gui.LegacyFullscreen;
import sk.enorials.minimap.clientside.gui.LegacyMarkers;
import sk.enorials.minimap.clientside.gui.LegacyPlayerPose;
import sk.enorials.minimap.clientside.gui.LegacyTerrain;
import sk.enorials.minimap.clientside.gui.MinimapMotionClock;
import sk.enorials.minimap.clientside.gui.TerrainWindow;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapShape;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.icon.MarkerIcons;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.minimap.map.renderer.SecondaryMinimapLayerRenderer;
import sk.enorials.minimap.map.renderer.encoder.SecondaryMapEncoder;
import sk.enorials.minimap.map.renderer.world.cache.CacheableWorldMinimapRenderer;
import sk.enorials.minimap.radar.RadarLayer;

public final class GuiMinimapPacketSender
extends AbstractMinimapPacketSender
implements AutoCloseable,
Listener {
    private final EnorialsMinimap plugin;
    private final GuiOverlayHost host = new GuiOverlayHost();
    private final LegacyTerrain legacyTerrain;
    private final LegacyMarkers legacyMarkers;
    private final LegacyFullscreen legacyFullscreen;
    private final MinimapMotionClock motionClock;
    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();
    private final BukkitTask flushTask;
    private volatile boolean closed;
    private long submittedFrames;
    private long flushTick;
    private final byte[] iconHeader = new byte[16384];

    public synchronized boolean updateIconLayer(Minimap minimap, SecondaryMinimapLayer secondaryMinimapLayer) {
        Object object;
        block6: {
            block5: {
                object = secondaryMinimapLayer.getRenderer();
                if (!(object instanceof MinimapIconRenderer)) break block5;
                MinimapIconRenderer minimapIconRenderer = (MinimapIconRenderer)object;
                if (secondaryMinimapLayer instanceof RadarLayer || minimapIconRenderer.rotateWithHolderYaw() || MarkerIcons.hasGlyph(minimapIconRenderer.icon().key())) break block6;
            }
            return false;
        }
        if (secondaryMinimapLayer instanceof PartyMapTracker.Layer) {
            LayerState layerState;
            object = (PartyMapTracker.Layer)secondaryMinimapLayer;
            Session session = this.sessions.get(minimap.holder().getUniqueId());
            LayerState layerState2 = layerState = session == null ? null : session.layers.get(secondaryMinimapLayer.getBaseLayer().mapId());
            if (layerState != null && layerState.head != ((PartyMapTracker.Layer)object).head) {
                layerState.head = ((PartyMapTracker.Layer)object).head;
                layerState.cached = null;
                session.dirty = true;
            }
        }
        Arrays.fill(this.iconHeader, 0, 640, (byte)0);
        SecondaryMapEncoder.encodeSecondaryLayer(minimap, secondaryMinimapLayer, this.iconHeader);
        this.updateLayer(minimap.holder(), secondaryMinimapLayer.getBaseLayer(), 0, 0, 128, 5, this.iconHeader);
        return true;
    }

    public GuiMinimapPacketSender(EnorialsMinimap enorialsMinimap) {
        this.plugin = enorialsMinimap;
        this.legacyTerrain = new LegacyTerrain(enorialsMinimap);
        this.legacyMarkers = new LegacyMarkers(enorialsMinimap);
        this.legacyFullscreen = new LegacyFullscreen(enorialsMinimap);
        this.motionClock = new MinimapMotionClock((Plugin)enorialsMinimap.owner());
        if (this.legacyTerrain != null) {
            Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsMinimap.owner());
        }
        FileConfiguration fileConfiguration = enorialsMinimap.owner().getConfig();
        boolean bl = false;
        if (!fileConfiguration.isSet("minimap-terrain-cache.max-idle-mib")) {
            fileConfiguration.set("minimap-terrain-cache.max-idle-mib", (Object)4);
            bl = true;
        }
        if (!fileConfiguration.isSet("minimap-terrain-cache.idle-seconds")) {
            fileConfiguration.set("minimap-terrain-cache.idle-seconds", (Object)5);
            bl = true;
        }
        if (bl) {
            enorialsMinimap.owner().saveConfig();
        }
        this.flushTask = Bukkit.getScheduler().runTaskTimer((Plugin)enorialsMinimap.owner(), this::flush, 1L, 1L);
    }

    @Override
    public synchronized void updateLayer(Player player, MinimapLayer minimapLayer, int n2, int n3, int n4, int n5, byte[] byArray) {
        int n6;
        if (this.closed || n2 < 0 || n3 < 0 || n4 < 0 || n5 < 0 || n2 + n4 > 128 || n3 + n5 > 128 || byArray.length < n4 * n5) {
            return;
        }
        Session session = this.sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        LayerState layerState = session.layers.computeIfAbsent(minimapLayer.mapId(), n -> new LayerState(minimapLayer));
        int n7 = GuiMinimapPacketSender.fullscreenMagic(layerState);
        boolean bl = layerState.raw != null && layerState.raw.length == 20736;
        long l = bl ? GuiMinimapPacketSender.terrainSettings(layerState.metadata) : 0L;
        boolean bl2 = false;
        for (n6 = 0; n6 < n5; ++n6) {
            for (int i = 0; i < n4; ++i) {
                int n8 = (n3 + n6) * 128 + n2 + i;
                byte by = byArray[n6 * n4 + i];
                if (layerState.metadata[n8] == by) continue;
                layerState.metadata[n8] = by;
                bl2 = true;
            }
        }
        if (bl2 && (!bl || l != GuiMinimapPacketSender.terrainSettings(layerState.metadata))) {
            layerState.cached = null;
            session.dirty = true;
        }
        n6 = GuiMinimapPacketSender.fullscreenMagic(layerState);
        if (layerState.fullscreenGeneration == 0 && n6 == 178) {
            layerState.fullscreenGeneration = session.currentFullscreenGeneration;
        }
        if (bl2 && layerState.visible && (n7 == 109 || n7 == 178 || n6 == 109 || n6 == 178)) {
            GuiMinimapPacketSender.reviseFullscreen(session, layerState);
        }
        if (GuiMinimapWire.unsigned(layerState.metadata, 105, 0) == 13 && Bukkit.isPrimaryThread() && this.sampleYaw(layerState, player)) {
            session.dirty = true;
        }
    }

    private static long terrainSettings(byte[] byArray) {
        return (long)GuiMinimapWire.variant(byArray) | (long)GuiMinimapWire.zoomAlias(byArray) << 6 | (long)GuiMinimapWire.unsigned(byArray, 33, 2) << 12 | (long)GuiMinimapWire.unsigned(byArray, 41, 0) << 20 | (long)GuiMinimapWire.unsigned(byArray, 49, 0) << 28;
    }

    static int fullscreenMagic(LayerState layerState) {
        return layerState.metadata[0] == -98 ? 0 : GuiMinimapWire.unsigned(layerState.metadata, 0, 0);
    }

    private static void reviseFullscreen(Session session, LayerState layerState) {
        LayerState layerState2 = session.layers.get(layerState.fullscreenGeneration);
        if (layerState2 != null) {
            ++layerState2.fullscreenRevision;
        }
    }

    @Override
    public synchronized void spawnFixedLayer(Player player, MinimapLayer minimapLayer) {
        this.spawnLayer(player, minimapLayer);
        Session session = this.sessions.get(player.getUniqueId());
        if (session != null) {
            session.currentFullscreenGeneration = minimapLayer.mapId();
            session.layers.get((Object)Integer.valueOf((int)minimapLayer.mapId())).fullscreenGeneration = minimapLayer.mapId();
        }
    }

    @Override
    public synchronized void spawnLayer(Player player, MinimapLayer minimapLayer) {
        if (this.closed) {
            return;
        }
        UUID uUID = player.getWorld().getUID();
        Session session = this.sessions.computeIfAbsent(player.getUniqueId(), uUID2 -> new Session(uUID, ClientProfiles.legacy(player)));
        if (!session.world.equals(uUID)) {
            session.layers.clear();
            session.clearFullscreenCache();
            session.pose.reset();
            session.world = uUID;
        }
        LayerState layerState = session.layers.computeIfAbsent(minimapLayer.mapId(), n -> new LayerState(minimapLayer));
        if (!layerState.visible && layerState.fullscreenGeneration != 0) {
            GuiMinimapPacketSender.reviseFullscreen(session, layerState);
        }
        layerState.visible = true;
        session.dirty = true;
    }

    @Override
    public synchronized void despawnLayer(Player player, MinimapLayer minimapLayer) {
        Session session = this.sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        LayerState layerState = session.layers.get(minimapLayer.mapId());
        if (layerState != null) {
            if (layerState.visible && layerState.fullscreenGeneration != 0) {
                GuiMinimapPacketSender.reviseFullscreen(session, layerState);
            }
            layerState.visible = false;
            layerState.cached = null;
            session.dirty = true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void captureTerrain(Minimap minimap, byte[] byArray) {
        Object object;
        Object object2;
        Object object3;
        Player player = minimap.holder();
        Location location = player.getLocation();
        boolean bl = minimap.primaryLayer().renderer() instanceof CacheableWorldMinimapRenderer;
        if (bl) {
            GuiMinimapPacketSender guiMinimapPacketSender = this;
            synchronized (guiMinimapPacketSender) {
                Session session = this.sessions.get(player.getUniqueId());
                if (session != null && session.pose.initialized && session.world.equals(player.getWorld().getUID())) {
                    location.setX(session.pose.x());
                    location.setZ(session.pose.z());
                }
            }
        }
        int n2 = location.getBlockX() - 72;
        int n3 = location.getBlockZ() - 72;
        if (bl) {
            object3 = this;
            synchronized (object3) {
                object2 = this.sessions.get(player.getUniqueId());
                if (object2 == null || this.closed) {
                    return;
                }
                object = ((Session)object2).layers.get(minimap.primaryLayer().mapId());
                if (((Session)object2).world.equals(player.getWorld().getUID()) && object != null && ((LayerState)object).raw != null && TerrainWindow.contains(location.getX(), location.getZ(), ((LayerState)object).originX, ((LayerState)object).originZ)) {
                    n2 = ((LayerState)object).originX;
                    n3 = ((LayerState)object).originZ;
                }
            }
        }
        object3 = new byte[20736];
        object = minimap.primaryLayer().renderer();
        if (object instanceof CacheableWorldMinimapRenderer) {
            object2 = (CacheableWorldMinimapRenderer)object;
            object = player.getWorld();
            object2.getWorldMapCache().configureIdleCache(this.plugin.owner().getConfig().getInt("minimap-terrain-cache.max-idle-mib", 4), this.plugin.owner().getConfig().getDouble("minimap-terrain-cache.idle-seconds", 5.0));
            object2.getWorldMapCache().setViewerChunks(player.getUniqueId(), (LongCollection)TerrainWindow.keys((World)object, n2, n3));
            TerrainWindow.copy(n2, n3, (byte[])object3, (arg_0, arg_1) -> GuiMinimapPacketSender.lambda$captureTerrain$0((CacheableWorldMinimapRenderer)object2, (World)object, arg_0, arg_1));
        } else {
            for (int i = 0; i < 144; ++i) {
                for (int j = 0; j < 144; ++j) {
                    int n4 = Math.max(0, Math.min(127, j - 8));
                    int n5 = Math.max(0, Math.min(127, i - 8));
                    object3[i * 144 + j] = byArray[(127 - n5) * 128 + (127 - n4)];
                }
            }
        }
        object2 = this;
        synchronized (object2) {
            Session session = this.sessions.get(player.getUniqueId());
            if (session == null || this.closed) {
                return;
            }
            LayerState layerState = session.layers.computeIfAbsent(minimap.primaryLayer().mapId(), n -> new LayerState(minimap.primaryLayer()));
            if (layerState.originX != n2 || layerState.originZ != n3 || !Arrays.equals(layerState.raw, (byte[])object3)) {
                layerState.raw = (byte[])object3;
                layerState.originX = n2;
                layerState.originZ = n3;
                layerState.cached = null;
                session.dirty = true;
            }
        }
    }

    public synchronized void captureFullscreen(Player player, MinimapLayer minimapLayer, byte[] byArray) {
        Session session = this.sessions.get(player.getUniqueId());
        if (session == null || this.closed) {
            return;
        }
        LayerState layerState = session.layers.computeIfAbsent(minimapLayer.mapId(), n -> new LayerState(minimapLayer));
        byte[] byArray2 = new byte[16384];
        for (int i = 0; i < 128; ++i) {
            for (int j = 0; j < 128; ++j) {
                byArray2[i * 128 + j] = byArray[(127 - j) * 128 + i];
            }
        }
        if (!Arrays.equals(layerState.raw, byArray2)) {
            layerState.raw = byArray2;
            layerState.cached = null;
            session.dirty = true;
            if (layerState.visible) {
                GuiMinimapPacketSender.reviseFullscreen(session, layerState);
            }
        }
    }

    private synchronized void flush() {
        if (this.closed) {
            return;
        }
        boolean bl = ++this.flushTick % 20L == 0L;
        for (Map.Entry<UUID, Session> entry : this.sessions.entrySet()) {
            boolean bl2;
            LayerState layerState22;
            Player player = Bukkit.getPlayer((UUID)entry.getKey());
            Session session = entry.getValue();
            if (player == null || !player.isOnline()) continue;
            if (!player.getWorld().getUID().equals(session.world)) {
                session.layers.clear();
                session.clearFullscreenCache();
                session.pose.reset();
                session.world = player.getWorld().getUID();
                session.dirty = true;
            }
            long l = player.getWorld().getGameTime();
            this.preparePose(player, session, l);
            if (session.legacy) {
                this.legacyFullscreen.render(player, session);
            }
            if (session.legacy) {
                this.legacyTerrain.render(player, session);
            }
            for (LayerState layerState22 : session.layers.values()) {
                if (!layerState22.visible || !layerState22.yawInitialized || layerState22.yawSettled || l - layerState22.yawTick <= (long)layerState22.yawDuration) continue;
                layerState22.yawFrom = layerState22.yawTo;
                layerState22.yawSettled = true;
                layerState22.cached = null;
                session.dirty = true;
            }
            boolean bl3 = bl2 = bl && this.host.needsRefresh(player);
            if (!session.dirty && !bl2) continue;
            layerState22 = this.compose(player, session);
            if (!bl2 && layerState22.equals(session.lastSubmitted)) {
                session.dirty = false;
            } else if (layerState22.equals(Component.empty())) {
                this.host.clear(player);
                session.dirty = false;
            } else if (this.host.set(player, (Component)layerState22)) {
                session.dirty = false;
                ++this.submittedFrames;
            }
            if (!session.dirty) {
                session.lastSubmitted = layerState22;
            }
            session.layers.values().removeIf(layerState -> !layerState.visible);
        }
    }

    private void preparePose(Player player, Session session, long l) {
        Location location = player.getLocation();
        boolean bl = session.pose.sample(location.getX(), location.getZ(), l);
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        if (minimap == null) {
            return;
        }
        LayerState layerState = session.layers.get(minimap.primaryLayer().mapId());
        this.motionClock.sample(player, session.pose instanceof LegacyPlayerPose && layerState != null && layerState.visible && !ResourcePackGuard.isLoading(player), session.pose.x() != session.pose.previousX() || session.pose.z() != session.pose.previousZ());
        if (layerState != null && layerState.visible && layerState.raw != null && layerState.raw.length == 20736 && (Math.abs(session.pose.x() - (double)layerState.originX - 72.0) >= 3.0 || Math.abs(session.pose.z() - (double)layerState.originZ - 72.0) >= 3.0)) {
            Object object = new byte[16384];
            if (!(minimap.primaryLayer().renderer() instanceof CacheableWorldMinimapRenderer)) {
                minimap.primaryLayer().renderer().render(minimap, minimap.primaryLayer(), (byte[])object);
            }
            this.captureTerrain(minimap, (byte[])object);
        }
        if (bl) {
            session.dirty = true;
            for (LayerState object : session.layers.values()) {
                int n = GuiMinimapWire.unsigned(object.metadata, 105, 0);
                if (n < 11 || n > 14) continue;
                object.cached = null;
            }
        }
        for (SecondaryMinimapLayer secondaryMinimapLayer : minimap.secondaryLayers().values()) {
            LayerState layerState2;
            SecondaryMinimapLayer secondaryMinimapLayer2;
            if (secondaryMinimapLayer instanceof PartyMapTracker.Layer) {
                secondaryMinimapLayer2 = (PartyMapTracker.Layer)secondaryMinimapLayer;
                if (((PartyMapTracker.Layer)secondaryMinimapLayer2).head != null) {
                    layerState2 = session.layers.get(secondaryMinimapLayer.getBaseLayer().mapId());
                    if (ClientProfiles.of(player).cameraTerrain()) {
                        var13_15 = this.cameraHeadPose(player, (PartyMapTracker.Layer)secondaryMinimapLayer2);
                        HeadMapWire.Pose pose = this.cameraMovingHeadPose(player, (PartyMapTracker.Layer)secondaryMinimapLayer2);
                        if (!(layerState2 == null || ((CameraHeadMapWire.Pose)var13_15).equals(layerState2.cameraHeadPose) && Objects.equals(pose, layerState2.headPose))) {
                            layerState2.cameraHeadPose = var13_15;
                            layerState2.headPose = pose;
                            layerState2.cached = null;
                            session.dirty = true;
                        }
                    } else {
                        var13_15 = this.headPose(player, session, (PartyMapTracker.Layer)secondaryMinimapLayer2);
                        if (layerState2 != null && !((HeadMapWire.Pose)var13_15).equals(layerState2.headPose)) {
                            layerState2.headPose = var13_15;
                            layerState2.cached = null;
                            session.dirty = true;
                        }
                    }
                }
            }
            if (!(secondaryMinimapLayer instanceof RadarLayer) || ((RadarLayer)(secondaryMinimapLayer2 = (RadarLayer)secondaryMinimapLayer)).deltaX() == 0.0 && ((RadarLayer)secondaryMinimapLayer2).deltaZ() == 0.0 || (layerState2 = session.layers.get(secondaryMinimapLayer.getBaseLayer().mapId())) == null || !layerState2.visible || l > ((RadarLayer)secondaryMinimapLayer2).sampleTick() + (long)((RadarLayer)secondaryMinimapLayer2).intervalTicks() + 1L) continue;
            layerState2.cached = null;
            session.dirty = true;
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public synchronized void acceptedMovement(PlayerMoveEvent playerMoveEvent) {
        LegacyPlayerPose legacyPlayerPose;
        GuiPlayerPose guiPlayerPose;
        block5: {
            block4: {
                Session session = this.sessions.get(playerMoveEvent.getPlayer().getUniqueId());
                if (session == null || !((guiPlayerPose = session.pose) instanceof LegacyPlayerPose)) break block4;
                legacyPlayerPose = (LegacyPlayerPose)guiPlayerPose;
                if (!(playerMoveEvent instanceof PlayerTeleportEvent)) break block5;
            }
            return;
        }
        guiPlayerPose = playerMoveEvent.getFrom();
        Location location = playerMoveEvent.getTo();
        if (location != null && guiPlayerPose.getWorld().equals((Object)location.getWorld())) {
            legacyPlayerPose.observe(guiPlayerPose.getX(), guiPlayerPose.getZ(), location.getX(), location.getZ(), playerMoveEvent.getPlayer().getWorld().getGameTime());
        }
    }

    private boolean sampleYaw(LayerState layerState, Player player) {
        long l = player.getWorld().getGameTime();
        double d = player.getLocation().getYaw();
        d = (d % 360.0 + 360.0) % 360.0;
        if (!layerState.yawInitialized) {
            layerState.yawInitialized = true;
            layerState.yawFrom = layerState.yawTo = d;
            layerState.yawTick = l;
            layerState.yawSettled = true;
            layerState.cached = null;
            return true;
        }
        if (Math.abs(GuiMinimapPacketSender.shortestAngle(d - layerState.yawTo)) < 1.0E-5) {
            return false;
        }
        double d2 = Math.max(0.0, Math.min(1.0, (double)(l - layerState.yawTick) / (double)layerState.yawDuration));
        layerState.yawFrom += GuiMinimapPacketSender.shortestAngle(layerState.yawTo - layerState.yawFrom) * d2;
        layerState.yawDuration = (int)Math.max(1L, Math.min(10L, l - layerState.yawTick));
        layerState.yawTo = d;
        layerState.yawTick = l;
        layerState.yawSettled = false;
        layerState.cached = null;
        return true;
    }

    private static double shortestAngle(double d) {
        return ((d + 540.0) % 360.0 + 360.0) % 360.0 - 180.0;
    }

    private Key mapFont(Player player, String string, int n) {
        int n2;
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        int n3 = n2 = minimap == null ? 0 : Math.min(5, minimap.frameStyle());
        if (ClientProfiles.of(player).legacyTerrain() && !string.equals("coords")) {
            n2 = 0;
        }
        return GuiMinimapWire.font(string, n, n2);
    }

    private Component compose(Player player, Session session) {
        LayerState layerState22;
        int n;
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        int n2 = n = minimap == null ? 0 : minimap.frameStyle();
        if (session.layoutFrame != n) {
            for (LayerState layerState22 : session.layers.values()) {
                layerState22.cached = null;
                layerState22.posedTerrain = null;
            }
            session.layoutFrame = n;
        }
        TextComponent.Builder builder = Component.text();
        layerState22 = GuiMinimapPacketSender.fullscreenBackground(session);
        if (layerState22 != null) {
            if (this.legacyMarkers != null) {
                this.legacyMarkers.hide(player);
            }
            return this.composeFullscreen(session, layerState22);
        }
        if (session.legacy) {
            this.legacyMarkers.begin(player, session.pose);
        }
        session.clearFullscreenCache();
        for (LayerState object : session.layers.values()) {
            if (!object.visible || object.raw == null || object.raw.length != 20736 || GuiMinimapWire.unsigned(object.metadata, 105, 0) != 10) continue;
            if (!session.legacy) {
                GuiMinimapWire.TerrainPose layerState3;
                if (object.cached == null) {
                    object.cached = GuiMinimapWire.terrainRuns(object.raw);
                    object.posedTerrain = null;
                }
                GuiMinimapWire.TerrainPose terrainPose = layerState3 = ClientProfiles.of(player).cameraTerrain() ? GuiMinimapWire.cameraTerrainPose(object.originX, object.originZ, object.metadata) : GuiMinimapWire.terrainPose(session.pose.x, session.pose.z, object.originX, object.originZ, session.pose.dx, session.pose.dz, session.pose.tick, object.metadata);
                if (object.posedTerrain == null || object.poseKey != layerState3.key()) {
                    object.posedTerrain = object.posedTerrain != null && (object.poseKey & 0x3FFFL) == (long)layerState3.alias() ? GuiMinimapWire.reposeTerrain(object.posedTerrain, layerState3) : GuiMinimapWire.posedTerrain(object.cached, GuiMinimapWire.style(this.mapFont(player, "pose_raster", GuiMinimapWire.variant(object.metadata)), 0), layerState3);
                    object.poseKey = layerState3.key();
                }
                builder.append(object.posedTerrain);
            }
            int terrainPose = GuiMinimapWire.unsigned(object.metadata, 41, 0) & 7;
            int byArray = Math.min(3, GuiMinimapWire.unsigned(object.metadata, 49, 0));
            if (session.legacy) {
                this.legacyMarkers.frame(player, Math.min(5, terrainPose), GuiMinimapWire.variant(object.metadata));
                continue;
            }
            for (int i = 0; i < 4; ++i) {
                builder.append(GuiMinimapWire.quad(GuiMinimapWire.style(this.mapFont(player, "frame_" + Math.min(5, terrainPose), GuiMinimapWire.variant(object.metadata)), terrainPose | byArray << 3), i));
            }
        }
        Minimap minimap2 = this.plugin.getPlayerMinimap(player);
        if (minimap2 != null) {
            minimap2.secondaryLayers().values().stream().sorted(Comparator.comparingDouble(SecondaryMinimapLayer::getDepth)).forEach(secondaryMinimapLayer -> {
                SecondaryMinimapLayerRenderer secondaryMinimapLayerRenderer;
                if (!HudAccess.layer(player, secondaryMinimapLayer)) {
                    return;
                }
                LayerState layerState = session.layers.get(secondaryMinimapLayer.getBaseLayer().mapId());
                if (layerState == null || !layerState.visible || layerState.metadata[256] != 4 || !((secondaryMinimapLayerRenderer = secondaryMinimapLayer.getRenderer()) instanceof MinimapIconRenderer)) {
                    return;
                }
                MinimapIconRenderer minimapIconRenderer = (MinimapIconRenderer)secondaryMinimapLayerRenderer;
                if (layerState.cached == null || session.legacy) {
                    layerState.cached = this.iconComponent(player, session, (SecondaryMinimapLayer)secondaryMinimapLayer, minimapIconRenderer, layerState);
                }
                builder.append(layerState.cached);
            });
        }
        for (LayerState layerState : session.layers.values()) {
            if (!layerState.visible || layerState.metadata[128] != 4 || GuiMinimapWire.unsigned(layerState.metadata, 1, 1) != 255) continue;
            if (layerState.cached == null) {
                byte[] byArray = (byte[])layerState.metadata.clone();
                Arrays.fill(byArray, 0, 640, (byte)0);
                byArray[16383] = 0;
                byArray[16256] = 0;
                layerState.cached = GuiMinimapWire.raster(byArray, 0, byArray.length, GuiMinimapWire.style(this.mapFont(player, "coords", GuiMinimapWire.variant(layerState.metadata)), 0xFFFFFF), 0, true);
            }
            builder.append(layerState.cached);
        }
        if (session.legacy) {
            this.legacyMarkers.end(player);
        }
        return builder.asComponent();
    }

    static LayerState fullscreenBackground(Session session) {
        LayerState layerState = null;
        for (LayerState layerState3 : session.layers.values()) {
            boolean bl;
            if (!layerState3.visible || GuiMinimapPacketSender.fullscreenMagic(layerState3) != 109 || !(bl = session.layers.values().stream().anyMatch(layerState2 -> layerState2.visible && layerState2.fullscreenGeneration == layerState.fullscreenGeneration && GuiMinimapPacketSender.fullscreenMagic(layerState2) == 178 && GuiMinimapWire.unsigned(layerState2.metadata, 24, 0) > 0 && GuiMinimapWire.unsigned(layerState2.metadata, 32, 0) > 0))) continue;
            layerState = layerState3;
        }
        return layerState;
    }

    /*
     * Unable to fully structure code
     */
    private Component iconComponent(Player var1_1, Session var2_2, SecondaryMinimapLayer var3_3, MinimapIconRenderer var4_4, LayerState var5_5) {
        var6_6 = var5_5.metadata;
        var7_7 = GuiMinimapWire.variant(var6_6);
        var8_8 = GuiMinimapWire.zoomAlias(var6_6);
        var9_9 = GuiMinimapWire.unsigned(var6_6, 33, 2);
        var10_10 = var8_8 & 31;
        var11_11 = var8_8 >>> 5;
        if (!var2_2.legacy && var3_3 instanceof PartyMapTracker.Layer) {
            var12_12 = (PartyMapTracker.Layer)var3_3;
            if (var12_12.head != null) {
                if (ClientProfiles.of(var1_1).cameraTerrain()) {
                    var5_5.cameraHeadPose = var13_13 = this.cameraHeadPose(var1_1, (PartyMapTracker.Layer)var12_12);
                    var5_5.headPose = var14_17 = this.cameraMovingHeadPose(var1_1, (PartyMapTracker.Layer)var12_12);
                    if (var14_17 != null) {
                        return HeadMapWire.cameraComponent(var12_12.head, this.mapFont(var1_1, "skin_head_motion", var7_7 / 3 * 3 + var13_13.zoomLevel()), var14_17);
                    }
                    return CameraHeadMapWire.component(var12_12.head, this.mapFont(var1_1, "skin_head", var7_7 / 3 * 3 + var13_13.zoomLevel()), var13_13);
                }
                var5_5.headPose = var13_14 = this.headPose(var1_1, var2_2, (PartyMapTracker.Layer)var12_12);
                return HeadMapWire.component(var12_12.head, this.mapFont(var1_1, "skin_head", var7_7), var13_14);
            }
        }
        if (var3_3 instanceof RadarLayer) {
            var12_12 = (RadarLayer)var3_3;
            if (ClientProfiles.of(var1_1).cameraTerrain()) {
                return this.dataComponent(var1_1, var3_3, var4_4, this.mapFont(var1_1, "pose_radar", var7_7), GuiMinimapWire.radar(var12_12.exactX(), var12_12.exactZ(), var12_12.deltaX(), var12_12.deltaZ(), var12_12.sampleTick() + 1L, var12_12.intervalTicks(), var9_9, var10_10, var11_11, var12_12.category().ordinal()), 59392);
            }
            var13_15 = GuiMinimapPacketSender.radarRemaining((RadarLayer)var12_12, var2_2.pose.tick);
            var15_20 = GuiMinimapPacketSender.radarRemaining((RadarLayer)var12_12, var2_2.pose.tick - 1L);
            var17_22 = var12_12.exactX() - var12_12.deltaX() * var13_15 - var2_2.pose.x();
            var19_24 = var12_12.exactZ() - var12_12.deltaZ() * var13_15 - var2_2.pose.z();
            var21_26 = var12_12.exactX() - var12_12.deltaX() * var15_20 - var2_2.pose.previousX();
            var23_28 = var12_12.exactZ() - var12_12.deltaZ() * var15_20 - var2_2.pose.previousZ();
            var17_22 = (double)Math.round(var17_22 * 32.0) / 32.0;
            var19_24 = (double)Math.round(var19_24 * 32.0) / 32.0;
            var21_26 = (double)Math.round(var21_26 * 32.0) / 32.0;
            var23_28 = (double)Math.round(var23_28 * 32.0) / 32.0;
            var25_29 = var17_22 - var21_26;
            var27_30 = var19_24 - var23_28;
            return this.dataComponent(var1_1, var3_3, var4_4, this.mapFont(var1_1, "pose_radar", var7_7), GuiMinimapWire.poseRadar(var17_22, var19_24, var25_29, var27_30, var25_29 == 0.0 && var27_30 == 0.0 ? 0L : var2_2.pose.tick, var9_9, var10_10, var11_11, var12_12.category().ordinal()), 59392);
        }
        if (var4_4.rotateWithHolderYaw()) {
            this.sampleYaw(var5_5, var1_1);
            return this.dataComponent(var1_1, var3_3, var4_4, this.mapFont(var1_1, "player", var7_7), GuiMinimapWire.player(var5_5.yawFrom, var5_5.yawTo, var5_5.yawSettled != false ? 0L : var5_5.yawTick, var5_5.yawDuration), 57344);
        }
        var12_12 = var4_4.icon().key();
        int var13_16;
        if (var3_3 instanceof PartyMapTracker.Layer partyLayer && partyLayer.head != null) {
            var13_16 = -1;
        } else {
            var13_16 = MarkerIcons.GLYPHS.indexOf(var12_12);
        }
        boolean var14_19 = MarkerIcons.hasGlyph((String)var12_12);
        if ("quest_!".equals(var12_12)) {
            var12_12 = "quest_exclamation";
        }
        if (!var14_19 && !var2_2.legacy) {
            return this.customIconComponent(var1_1, var2_2, var3_3, var5_5);
        }
        var15_21 = var3_3.getPositionX();
        var17_23 = var3_3.getPositionZ();
        var19_25 = var3_3.isTrackLocation() == false;
        var20_31 = 0;
        var21_27 = 0;
        if (var13_16 >= 0 && ClientProfiles.of(var1_1).cameraTerrain()) {
            if (var19_25) {
                var15_21 -= 64.0;
                var17_23 -= 64.0;
            } else {
                var22_32 = var15_21 - var1_1.getLocation().getX();
                var26_36 = Math.hypot(var22_32, var24_34 = var17_23 - var1_1.getLocation().getZ());
                if (var26_36 > 32000.0) {
                    var15_21 = var1_1.getLocation().getX() + var22_32 / var26_36 * 32000.0;
                    var17_23 = var1_1.getLocation().getZ() + var24_34 / var26_36 * 32000.0;
                }
            }
            return this.dataComponent(var1_1, var3_3, var4_4, this.mapFont(var1_1, "hq_marker", var7_7 / 3 * 3), GuiMinimapWire.cameraMarker(var15_21, var17_23, var9_9, var10_10, var11_11, var3_3.isKeepOnEdge(), var19_25, this.plugin.owner().storage().markerStyle(var1_1.getUniqueId())), 59392 + var13_16 * 16);
        }
        if (var19_25) {
            var15_21 -= 64.0;
            var17_23 -= 64.0;
        } else {
            if (var3_3 instanceof PartyMapTracker.Layer) {
                var26_37 = (PartyMapTracker.Layer)var3_3;
                var15_21 = var26_37.x - var2_2.pose.x();
                var17_23 = var26_37.z - var2_2.pose.z();
                var22_33 = var26_37.previousX - var2_2.pose.previousX();
                var24_35 = var26_37.previousZ - var2_2.pose.previousZ();
            } else {
                var22_33 = (var15_21 -= var2_2.pose.x()) + (double)var2_2.pose.dx / 32.0;
                var24_35 = (var17_23 -= var2_2.pose.z()) + (double)var2_2.pose.dz / 32.0;
            }
            var26_38 = var13_16 >= 0 ? 120.0 : 512.0;
            var28_39 = Math.hypot(var15_21, var17_23);
            if (var3_3.isKeepOnEdge() && var28_39 > var26_38) {
                var15_21 = var15_21 / var28_39 * var26_38;
                var17_23 = var17_23 / var28_39 * var26_38;
            }
            var30_40 = Math.hypot(var22_33, var24_35);
            if (var3_3.isKeepOnEdge() && var30_40 > var26_38) {
                var22_33 = var22_33 / var30_40 * var26_38;
                var24_35 = var24_35 / var30_40 * var26_38;
            }
            if (!GuiMotion.continuous(var20_31 = (int)(Math.round(var22_33 * 32.0) - Math.round(var15_21 * 32.0)), var21_27 = (int)(Math.round(var24_35 * 32.0) - Math.round(var17_23 * 32.0)))) {
                var20_31 = 0;
                var21_27 = 0;
            }
        }
        if (var13_16 >= 0) {
            if (var15_21 < -127.0 || var15_21 > 127.0 || var17_23 < -127.0 || var17_23 > 127.0) {
                return Component.empty();
            }
            return this.dataComponent(var1_1, var3_3, var4_4, this.mapFont(var1_1, "hq_marker", var7_7 / 3 * 3), GuiMinimapWire.hqMarker(var15_21, var17_23, var20_31, var21_27, var2_2.pose.tick, var9_9, var10_10, var11_11, var3_3.isKeepOnEdge(), var19_25, this.plugin.owner().storage().markerStyle(var1_1.getUniqueId())), 59392 + var13_16 * 16);
        }
        return this.dataComponent(var1_1, var3_3, var4_4, this.mapFont(var1_1, "pose_icon/" + (String)var12_12, var7_7), GuiMinimapWire.poseIcon(var15_21, var17_23, var20_31, var21_27, var20_31 == 0 && var21_27 == 0 ? 0L : var2_2.pose.tick, var9_9, var10_10, var11_11, var3_3.isKeepOnEdge(), var19_25), 59392);
    }

    static double radarRemaining(RadarLayer radarLayer, long l) {
        return 1.0 - Math.max(0.0, Math.min(1.0, (double)(l - radarLayer.sampleTick()) / (double)Math.max(1, radarLayer.intervalTicks())));
    }

    private HeadMapWire.Pose headPose(Player player, Session session, PartyMapTracker.Layer layer) {
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        long l = player.getWorld().getGameTime();
        double d = minimap == null ? 1.0 : 1.0 + (double)minimap.zoomLevel() * 0.5;
        double d2 = minimap == null ? 1.0 : d * (double)minimap.renderZoomAt(l);
        double d3 = minimap == null ? 1.0 : d * (double)minimap.renderZoomAt(l - 1L);
        return HeadMapWire.pose(layer.x - session.pose.x(), layer.z - session.pose.z(), layer.previousX - session.pose.previousX(), layer.previousZ - session.pose.previousZ(), d2, d3, minimap != null && minimap.shape() == MinimapShape.SQUARE, l);
    }

    private CameraHeadMapWire.Pose cameraHeadPose(Player player, PartyMapTracker.Layer layer) {
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        Location location = player.getLocation();
        return CameraHeadMapWire.pose(layer.x, layer.z, location.getX(), location.getZ(), minimap == null ? 0 : Math.round((minimap.renderZoomFrom() - 1.0f) * 63.0f), minimap == null ? 0 : minimap.renderZoomStartTick(), minimap != null && minimap.renderZoomTarget() > 1.0f ? 1 : 0, minimap == null ? 0 : minimap.zoomLevel());
    }

    private HeadMapWire.Pose cameraMovingHeadPose(Player player, PartyMapTracker.Layer layer) {
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        long l = player.getWorld().getGameTime();
        if (minimap == null || (double)minimap.renderZoomAt(l) != 1.0 || (double)minimap.renderZoomAt(l - 1L) != 1.0) {
            return null;
        }
        double d = 1.0 + (double)minimap.zoomLevel() * 0.5;
        double d2 = layer.previousX - layer.x;
        double d3 = layer.previousZ - layer.z;
        if (d2 == 0.0 && d3 == 0.0 || Math.hypot(d2, d3) > 8.0) {
            return null;
        }
        Location location = player.getLocation();
        double d4 = Math.max(Math.hypot(layer.x - location.getX(), layer.z - location.getZ()), Math.hypot(layer.previousX - location.getX(), layer.previousZ - location.getZ()));
        if (d4 * d > 48.0) {
            return null;
        }
        return new HeadMapWire.Pose(layer.x * d % 256.0, layer.z * d % 256.0, d2 * d, d3 * d, l + 1L);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public synchronized void legacyTeleport(PlayerTeleportEvent playerTeleportEvent) {
        Session session = this.sessions.get(playerTeleportEvent.getPlayer().getUniqueId());
        if (session != null) {
            session.pose.reset();
        }
        this.relocateLegacy(playerTeleportEvent.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void legacyRespawn(PlayerRespawnEvent playerRespawnEvent) {
        this.relocateLegacy(playerRespawnEvent.getPlayer());
    }

    private void relocateLegacy(Player player) {
        if (!ClientProfiles.legacy(player)) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this.plugin.owner(), () -> {
            if (this.closed || !player.isOnline()) {
                return;
            }
            this.legacyTerrain.relocate(player);
            this.legacyMarkers.relocate(player);
            this.legacyFullscreen.relocate(player);
        });
    }

    private Component dataComponent(Player player, SecondaryMinimapLayer secondaryMinimapLayer, MinimapIconRenderer minimapIconRenderer, Key key, GuiMinimapWire.DataGlyph dataGlyph, int n) {
        if (!ClientProfiles.legacy(player)) {
            return GuiMinimapWire.dataComponent(key, dataGlyph, n);
        }
        this.legacyMarkers.show(player, secondaryMinimapLayer, minimapIconRenderer, key.value(), dataGlyph, n);
        return Component.empty();
    }

    private Component customIconComponent(Player player, Session session, SecondaryMinimapLayer secondaryMinimapLayer, LayerState layerState) {
        boolean bl;
        MinimapIconRenderer minimapIconRenderer = (MinimapIconRenderer)secondaryMinimapLayer.getRenderer();
        byte[] byArray = layerState.metadata;
        int n = GuiMinimapWire.zoomAlias(byArray);
        int n2 = GuiMinimapWire.unsigned(byArray, 33, 2);
        double d = secondaryMinimapLayer.getPositionX();
        double d2 = secondaryMinimapLayer.getPositionZ();
        boolean bl2 = bl = !secondaryMinimapLayer.isTrackLocation();
        if (bl) {
            d += 64.0;
            d2 += 64.0;
        } else {
            double d3;
            double d4;
            Minimap minimap = this.plugin.getPlayerMinimap(player);
            double d5 = minimap == null ? 1.0 : (1.0 + (double)minimap.zoomLevel() * 0.5) * (double)minimap.renderZoomAt(player.getWorld().getGameTime());
            double d6 = (d -= session.pose.x()) * d5;
            double d7 = (d2 -= session.pose.z()) * d5;
            double d8 = (double)minimapIconRenderer.icon().width() / 2.0;
            double d9 = (double)minimapIconRenderer.icon().height() / 2.0;
            if (secondaryMinimapLayer.isKeepOnEdge() && GuiMinimapWire.unsigned(byArray, 33, 0) > 0) {
                double d10 = Math.min(1.0, Math.min(Math.max(1.0, 63.0 - d8) / Math.max(1.0E-6, Math.abs(d6)), Math.max(1.0, 63.0 - d9) / Math.max(1.0E-6, Math.abs(d7))));
                d6 *= d10;
                d7 *= d10;
            } else if (secondaryMinimapLayer.isKeepOnEdge() && (d4 = Math.hypot(d6, d7)) > (d3 = Math.max(1.0, 60.0 - Math.max(d8, d9)))) {
                d6 *= d3 / d4;
                d7 *= d3 / d4;
            }
            d = 128.0 + d6 / d5;
            d2 = 128.0 + d7 / d5;
        }
        if (!bl && ClientProfiles.of(player).cameraTerrain()) {
            d = d - 128.0 + session.pose.x();
            d2 = d2 - 128.0 + session.pose.z();
        }
        int n3 = (int)(Math.round(d * 8.0) & 0x7FFL) | (int)(Math.round(d2 * 8.0) & 0x7FFL) << 11 | n2 << 22 | (bl ? 0x40000000 : 0);
        Style style = TextCompat.shadow(GuiMinimapWire.style(this.mapFont(player, "custom", GuiMinimapWire.variant(byArray)), 0), n3 << 1 | 1);
        byte[] byArray2 = (byte[])byArray.clone();
        Arrays.fill(byArray2, 0, 640, (byte)0);
        byArray2[16383] = 0;
        byArray2[16256] = 0;
        return GuiMinimapWire.raster(byArray2, 0, 16384, style, n, true, -8388608);
    }

    private Component composeFullscreen(Session session, LayerState layerState) {
        int n = 0;
        Component component = session.layers.values().iterator();
        while (component.hasNext()) {
            LayerState layerState2 = component.next();
            if (layerState2.raw == null || layerState2.raw.length != 20736) continue;
            n = Math.min(2, GuiMinimapWire.unsigned(layerState2.metadata, 57, 0));
            break;
        }
        if (session.fullscreenComposite != null && session.cachedFullscreenGeneration == layerState.fullscreenGeneration && session.cachedFullscreenRevision == layerState.fullscreenRevision && session.cachedFullscreenProfile == n) {
            return session.fullscreenComposite;
        }
        session.fullscreenComposite = component = this.buildFullscreen(session, layerState, n);
        session.cachedFullscreenGeneration = layerState.fullscreenGeneration;
        session.cachedFullscreenRevision = layerState.fullscreenRevision;
        session.cachedFullscreenProfile = n;
        ++session.fullscreenCompositeBuilds;
        return component;
    }

    private Component buildFullscreen(Session session, LayerState layerState, int n) {
        int n2 = GuiMinimapWire.unsigned(layerState.metadata, 8, 0);
        TextComponent.Builder builder = Component.text();
        builder.append(GuiMinimapWire.quad(GuiMinimapWire.style(Key.key((String)"enorials_gui:background"), 0xFFFFFF), n2));
        if (session.legacy) {
            return builder.asComponent();
        }
        List<LayerState> list = session.layers.values().stream().filter(layerState2 -> layerState2.visible && layerState2.fullscreenGeneration == layerState.fullscreenGeneration && GuiMinimapPacketSender.fullscreenMagic(layerState2) == 178).toList();
        if (list.isEmpty()) {
            return builder.asComponent();
        }
        int n3 = GuiMinimapWire.unsigned(list.get((int)0).metadata, 24, 0);
        int n4 = GuiMinimapWire.unsigned(list.get((int)0).metadata, 32, 0);
        int n5 = n3 * 128;
        int n6 = n4 * 128;
        if (n5 <= 0 || n6 <= 0 || n5 > 16000 || (long)n5 * (long)n6 > 4000000L) {
            return builder.asComponent();
        }
        int n7 = 16000 / n5;
        if ((n6 + n7 - 1) / n7 > 256) {
            return builder.asComponent();
        }
        byte[] byArray = new byte[n5 * n6];
        for (LayerState layerState3 : list) {
            if (layerState3.raw == null || layerState3.raw.length != 16384) continue;
            int n8 = GuiMinimapWire.unsigned(layerState3.metadata, 8, 0);
            int n9 = GuiMinimapWire.unsigned(layerState3.metadata, 16, 0);
            if (n8 >= n3 || n9 >= n4) continue;
            for (int i = 0; i < 128; ++i) {
                System.arraycopy(layerState3.raw, i * 128, byArray, (n9 * 128 + i) * n5 + n8 * 128, 128);
            }
        }
        builder.append(GuiFullscreenBundles.encode(byArray, n5, n6, n, n2));
        return builder.asComponent();
    }

    public synchronized void refreshPermissions(Player player) {
        Session session = this.sessions.get(player.getUniqueId());
        if (session != null) {
            session.dirty = true;
            for (LayerState layerState : session.layers.values()) {
                layerState.cached = null;
            }
        }
    }

    public synchronized void forget(Player player) {
        this.motionClock.sample(player, false, false);
        if (this.legacyTerrain != null) {
            this.legacyTerrain.forget(player);
        }
        if (this.legacyMarkers != null) {
            this.legacyMarkers.forget(player);
        }
        if (this.legacyFullscreen != null) {
            this.legacyFullscreen.forget(player);
        }
        this.sessions.remove(player.getUniqueId());
        this.host.clear(player);
    }

    public synchronized int visibleLayerCount(Player player) {
        Session session = this.sessions.get(player.getUniqueId());
        return session == null ? 0 : (int)session.layers.values().stream().filter(layerState -> layerState.visible).count();
    }

    public synchronized long submittedFrames() {
        return this.submittedFrames;
    }

    public synchronized long fullscreenCompositeBuildCount(Player player) {
        Session session = this.sessions.get(player.getUniqueId());
        return session == null ? 0L : session.fullscreenCompositeBuilds;
    }

    @Override
    public synchronized void close() {
        this.closed = true;
        this.flushTask.cancel();
        this.motionClock.close();
        HandlerList.unregisterAll((Listener)this);
        if (this.legacyTerrain != null) {
            this.legacyTerrain.close();
        }
        if (this.legacyMarkers != null) {
            this.legacyMarkers.close();
        }
        if (this.legacyFullscreen != null) {
            this.legacyFullscreen.close();
        }
        for (UUID uUID : this.sessions.keySet()) {
            Player player = Bukkit.getPlayer((UUID)uUID);
            if (player == null) continue;
            this.host.clear(player);
        }
        this.sessions.clear();
    }

    private static /* synthetic */ byte[] lambda$captureTerrain$0(CacheableWorldMinimapRenderer cacheableWorldMinimapRenderer, World world, int n, int n2) {
        return cacheableWorldMinimapRenderer.getWorldMapCache().get(world, n, n2);
    }

    static final class Session {
        UUID world;
        int currentFullscreenGeneration;
        int layoutFrame = -1;
        final Map<Integer, LayerState> layers = new LinkedHashMap<Integer, LayerState>();
        final GuiPlayerPose pose;
        boolean dirty = true;
        Component lastSubmitted;
        Component fullscreenComposite;
        int cachedFullscreenGeneration;
        int cachedFullscreenProfile;
        long cachedFullscreenRevision;
        long fullscreenCompositeBuilds;
        final boolean legacy;

        Session(UUID uUID) {
            this(uUID, ClientRenderProfile.nativeProfile().legacyTerrain());
        }

        Session(UUID uUID, boolean bl) {
            this.world = uUID;
            this.legacy = bl;
            this.pose = bl ? new LegacyPlayerPose() : new GuiPlayerPose();
        }

        void clearFullscreenCache() {
            this.fullscreenComposite = null;
        }
    }

    static final class LayerState {
        final MinimapLayer layer;
        final byte[] metadata = new byte[16384];
        byte[] raw;
        int originX;
        int originZ;
        int fullscreenGeneration;
        long fullscreenRevision;
        boolean visible;
        Component cached;
        SkinHead head;
        HeadMapWire.Pose headPose;
        CameraHeadMapWire.Pose cameraHeadPose;
        Component posedTerrain;
        long poseKey;
        boolean yawInitialized;
        boolean yawSettled;
        double yawFrom;
        double yawTo;
        long yawTick;
        int yawDuration = 1;

        LayerState(MinimapLayer minimapLayer) {
            this.layer = minimapLayer;
        }
    }
}

