/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongArrayList
 *  it.unimi.dsi.fastutil.longs.LongCollection
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.map;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.map.TileWindow;
import sk.enorials.minimap.clientside.MinimapPacketSender;
import sk.enorials.minimap.clientside.gui.GuiMinimapPacketSender;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.map.DeathMarkerController;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapPreset;
import sk.enorials.minimap.map.MinimapProvider;
import sk.enorials.minimap.map.MinimapScreenPosition;
import sk.enorials.minimap.map.MinimapShape;
import sk.enorials.minimap.map.MinimapSize;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.marker.MarkerMinimapLayer;
import sk.enorials.minimap.map.renderer.CoordinatesOverlayRenderer;
import sk.enorials.minimap.map.renderer.MinimapLayerRenderer;
import sk.enorials.minimap.map.renderer.encoder.PrimaryMapEncoder;
import sk.enorials.minimap.map.renderer.encoder.SecondaryMapEncoder;
import sk.enorials.minimap.map.renderer.world.cache.CacheableWorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.cache.WorldMapCache;
import sk.enorials.minimap.radar.RadarCategory;
import sk.enorials.minimap.radar.RadarLayer;
import sk.enorials.minimap.storage.LocationPersister;

public final class Minimap {
    public static final String PLAYER_LAYER_KEY = "player";
    public static final String DEATH_LAYER_KEY = "death_point";
    public static final String COORDINATES_LAYER_KEY = "__coords__";
    private final Player holder;
    private final MinimapLayer primaryLayer;
    private final LinkedHashMap<String, SecondaryMinimapLayer> secondaryLayers;
    private MinimapScreenPosition screenPosition;
    private MinimapSize size;
    private MinimapShape shape;
    private boolean dangerRingEnabled;
    private boolean fogStylingEnabled;
    private boolean coordinatesEnabled;
    private int frameStyle;
    private int colorProfile;
    private boolean combatZoomEnabled;
    private int zoomLevel;
    private boolean radarMasterEnabled = true;
    private float renderZoomFrom = 1.0f;
    private float renderZoomTarget = 1.0f;
    private long renderZoomStartTick;
    private long combatUntilTick;
    private final EnumMap<RadarCategory, Boolean> radarFilters = new EnumMap(RadarCategory.class);
    private final MinimapPreset[] presets;
    private Location persistedDeathPoint;
    private final byte[] guiTerrainHeader = new byte[16384];

    public Minimap(Player player, MinimapScreenPosition minimapScreenPosition, MinimapLayer minimapLayer, LinkedHashMap<String, SecondaryMinimapLayer> linkedHashMap) {
        this.holder = player;
        this.screenPosition = minimapScreenPosition;
        this.size = MinimapSize.LARGE;
        this.shape = MinimapShape.CIRCLE;
        this.dangerRingEnabled = Config.instance().defaultDangerRingEnabled;
        this.fogStylingEnabled = Config.instance().defaultFogStylingEnabled;
        this.coordinatesEnabled = Config.instance().coordinates.enabled;
        this.frameStyle = this.dangerRingEnabled ? 1 : 0;
        this.colorProfile = this.fogStylingEnabled ? 1 : 0;
        this.combatZoomEnabled = Config.instance().defaultCombatZoomEnabled;
        this.presets = new MinimapPreset[3];
        this.primaryLayer = minimapLayer;
        this.secondaryLayers = linkedHashMap;
        this.radarFilters.put(RadarCategory.AGGRESSIVE, Config.instance().radar.defaultAggressiveEnabled);
        this.radarFilters.put(RadarCategory.NEUTRAL, Config.instance().radar.defaultNeutralEnabled);
        this.radarFilters.put(RadarCategory.FRIENDLY, Config.instance().radar.defaultFriendlyEnabled);
    }

    public boolean radarEnabled(RadarCategory radarCategory) {
        return this.radarFilters.getOrDefault((Object)radarCategory, true);
    }

    public void radarEnabled(RadarCategory radarCategory, boolean bl) {
        this.radarFilters.put(radarCategory, bl);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean anyRadarEnabled() {
        if (!this.radarMasterEnabled) return false;
        if (!this.radarFilters.values().stream().anyMatch(Boolean.TRUE::equals)) return false;
        return true;
    }

    public boolean radarMasterEnabled() {
        return this.radarMasterEnabled;
    }

    public void radarMasterEnabled(boolean bl) {
        this.radarMasterEnabled = bl;
    }

    public boolean coordinatesEnabled() {
        return this.coordinatesEnabled;
    }

    public void coordinatesEnabled(boolean bl) {
        this.coordinatesEnabled = bl;
    }

    public boolean frameFollowsTheme() {
        EnorialsHUD enorialsHUD = EnorialsHUD.getInstance();
        return enorialsHUD != null && enorialsHUD.storage() != null && enorialsHUD.storage().preference(this.holder.getUniqueId(), "map-frame-theme", true);
    }

    public void frameFollowsTheme(boolean bl) {
        EnorialsHUD enorialsHUD = EnorialsHUD.getInstance();
        if (enorialsHUD != null && enorialsHUD.storage() != null) {
            enorialsHUD.storage().setPreference(this.holder.getUniqueId(), "map-frame-theme", bl);
        }
    }

    public int frameStyle() {
        return this.frameFollowsTheme() ? EnorialsHUD.getInstance().storage().theme(this.holder.getUniqueId()).minimapFrame() : this.frameStyle;
    }

    public void restoreFrameStyle(int n) {
        this.frameStyle = Math.max(0, Math.min(5, n));
        this.dangerRingEnabled = this.frameStyle == 1;
    }

    public void frameStyle(int n) {
        this.restoreFrameStyle(n);
        this.frameFollowsTheme(false);
    }

    public int colorProfile() {
        return this.colorProfile;
    }

    public void colorProfile(int n) {
        this.colorProfile = Math.max(0, Math.min(2, n));
        this.fogStylingEnabled = this.colorProfile != 0;
    }

    public boolean combatZoomEnabled() {
        return this.combatZoomEnabled;
    }

    public int zoomLevel() {
        return this.zoomLevel;
    }

    public void zoomLevel(int n) {
        this.zoomLevel = Math.max(0, Math.min(2, n));
    }

    public void combatZoomEnabled(boolean bl) {
        this.combatZoomEnabled = bl;
        if (!bl) {
            this.endCombat(this.holder.getWorld().getGameTime());
        }
    }

    public float renderZoomFrom() {
        return this.renderZoomFrom;
    }

    public float renderZoomTarget() {
        return this.renderZoomTarget;
    }

    public int renderZoomStartTick() {
        return (int)Math.floorMod(this.renderZoomStartTick, 24000L);
    }

    public float renderZoomAt(long l) {
        float f = Math.max(0.0f, Math.min(1.0f, (float)(l - this.renderZoomStartTick) / 8.0f));
        f *= f * (3.0f - 2.0f * f);
        return this.renderZoomFrom + (this.renderZoomTarget - this.renderZoomFrom) * f;
    }

    public boolean markCombat(long l) {
        if (!this.combatZoomEnabled) {
            return false;
        }
        this.combatUntilTick = l + 160L;
        return this.zoomTarget(2.0f, l);
    }

    public boolean expireCombat(long l) {
        return this.combatUntilTick > 0L && l >= this.combatUntilTick && this.endCombat(l);
    }

    public boolean endCombat(long l) {
        this.combatUntilTick = 0L;
        return this.zoomTarget(1.0f, l);
    }

    public boolean settleCombatZoom(long l) {
        if (this.renderZoomFrom == this.renderZoomTarget || l - this.renderZoomStartTick < 8L) {
            return false;
        }
        this.renderZoomFrom = this.renderZoomTarget;
        this.renderZoomStartTick = l;
        return true;
    }

    public void resetCombatZoom(long l) {
        this.combatUntilTick = 0L;
        this.renderZoomTarget = 1.0f;
        this.renderZoomFrom = 1.0f;
        this.renderZoomStartTick = l;
    }

    private boolean zoomTarget(float f, long l) {
        if (this.renderZoomTarget == f) {
            return false;
        }
        this.renderZoomFrom = this.renderZoomAt(l);
        this.renderZoomTarget = f;
        this.renderZoomStartTick = l;
        return true;
    }

    private void ensureCoordinatesLayer(MinimapProvider minimapProvider) {
        SecondaryMinimapLayer secondaryMinimapLayer = this.secondaryLayers.get(COORDINATES_LAYER_KEY);
        if (!this.coordinatesEnabled) {
            if (secondaryMinimapLayer != null) {
                this.secondaryLayers.remove(COORDINATES_LAYER_KEY);
                minimapProvider.packetSender().despawnLayer(this.holder, secondaryMinimapLayer.getBaseLayer());
            }
        } else if (secondaryMinimapLayer == null) {
            MinimapLayer minimapLayer = minimapProvider.clientsideMinimapFactory().createMinimapLayer(this.holder.getWorld(), null);
            SecondaryMinimapLayer secondaryMinimapLayer2 = new SecondaryMinimapLayer(minimapLayer, new CoordinatesOverlayRenderer(Config.instance().coordinates.textY), false, true, null, 64, 64, 0.45f);
            this.secondaryLayers.put(COORDINATES_LAYER_KEY, secondaryMinimapLayer2);
            minimapProvider.packetSender().spawnLayer(this.holder, minimapLayer);
        }
    }

    public void update(MinimapProvider minimapProvider, double d, double d2, boolean bl) {
        Object object;
        byte[] byArray;
        boolean bl2;
        this.ensureCoordinatesLayer(minimapProvider);
        this.removeExpiredTimedMarkers(minimapProvider, System.currentTimeMillis());
        MinimapLayerRenderer minimapLayerRenderer = this.primaryLayer.renderer();
        MinimapPacketSender minimapPacketSender = minimapProvider.packetSender();
        boolean bl3 = bl2 = minimapPacketSender instanceof GuiMinimapPacketSender && minimapLayerRenderer instanceof CacheableWorldMinimapRenderer;
        if (bl2) {
            byArray = this.guiTerrainHeader;
            Arrays.fill(byArray, 0, 640, (byte)0);
        } else {
            byArray = new byte[16384];
            if (minimapLayerRenderer instanceof CacheableWorldMinimapRenderer) {
                object = (CacheableWorldMinimapRenderer)minimapLayerRenderer;
                World world = this.holder.getWorld();
                Location object2 = this.holder.getLocation();
                int n = object2.getBlockX() >> 7 << 7;
                int n2 = object2.getBlockZ() >> 7 << 7;
                WorldMapCache<?> worldMapCache = object.getWorldMapCache();
                TileWindow.compose(worldMapCache.get(world, n, n2), worldMapCache.get(world, n + 128, n2), worldMapCache.get(world, n, n2 + 128), worldMapCache.get(world, n + 128, n2 + 128), object2.getBlockX() & 0x7F, object2.getBlockZ() & 0x7F, byArray);
                if (bl) {
                    int n3 = (int)Math.floor(d) >> 7 << 7;
                    int n4 = (int)Math.floor(d2) >> 7 << 7;
                    LongArrayList longArrayList = new LongArrayList(4);
                    for (int i = 0; i < 2; ++i) {
                        for (int j = 0; j < 2; ++j) {
                            longArrayList.add(WorldMapCache.getKey(world, n3 + j * 128, n4 + i * 128));
                        }
                    }
                    worldMapCache.setViewerChunks(this.holder.getUniqueId(), (LongCollection)longArrayList);
                }
            } else {
                minimapLayerRenderer.render(this, this.primaryLayer, byArray);
            }
        }
        if (minimapPacketSender instanceof GuiMinimapPacketSender) {
            object = (GuiMinimapPacketSender)minimapPacketSender;
            ((GuiMinimapPacketSender)object).captureTerrain(this, byArray);
        }
        PrimaryMapEncoder.encodePrimaryLayer(this, byArray);
        minimapPacketSender.updateLayer(this.holder, this.primaryLayer, 0, 0, 128, bl2 ? 5 : 128, byArray);
        boolean bl4 = Config.instance().resourcePack.fallbackMode();
        for (SecondaryMinimapLayer secondaryMinimapLayer : this.secondaryLayers.values()) {
            if (!bl4 && secondaryMinimapLayer instanceof RadarLayer) continue;
            this.updateSecondaryLayer(minimapProvider, secondaryMinimapLayer);
        }
    }

    public void update(MinimapProvider minimapProvider) {
        Location location = this.holder.getLocation();
        this.update(minimapProvider, location.getX(), location.getZ(), false);
    }

    public boolean updateSecondaryLayer(MinimapProvider minimapProvider, String string) {
        SecondaryMinimapLayer secondaryMinimapLayer = this.secondaryLayers.get(string);
        return secondaryMinimapLayer == null ? false : this.updateSecondaryLayer(minimapProvider, secondaryMinimapLayer);
    }

    /*
     * Enabled aggressive block sorting
     */
    private boolean updateSecondaryLayer(MinimapProvider minimapProvider, SecondaryMinimapLayer secondaryMinimapLayer) {
        Object object;
        if (secondaryMinimapLayer.getWorld() != null && !secondaryMinimapLayer.getWorld().equals((Object)this.holder.getWorld())) {
            return false;
        }
        Object object2 = minimapProvider.packetSender();
        if (object2 instanceof GuiMinimapPacketSender && ((GuiMinimapPacketSender)(object = (GuiMinimapPacketSender)object2)).updateIconLayer(this, secondaryMinimapLayer)) {
            return true;
        }
        object = new byte[16384];
        if (secondaryMinimapLayer.getRenderer() != null) {
            secondaryMinimapLayer.getRenderer().render(this, secondaryMinimapLayer, (byte[])object);
        } else if (secondaryMinimapLayer.getBaseLayer().renderer() != null) {
            secondaryMinimapLayer.getBaseLayer().renderer().render(this, secondaryMinimapLayer.getBaseLayer(), (byte[])object);
        }
        SecondaryMapEncoder.encodeSecondaryLayer(this, secondaryMinimapLayer, (byte[])object);
        if (secondaryMinimapLayer instanceof RadarLayer) {
            object2 = (RadarLayer)secondaryMinimapLayer;
            if (!Config.instance().resourcePack.fallbackMode() && !((RadarLayer)object2).consumeFullTexture()) {
                minimapProvider.packetSender().updateLayer(this.holder, secondaryMinimapLayer.getBaseLayer(), 0, 0, 128, 5, Arrays.copyOf((byte[])object, 640));
                return true;
            }
        }
        minimapProvider.packetSender().updateLayer(this.holder, secondaryMinimapLayer.getBaseLayer(), 0, 0, 128, 128, (byte[])object);
        return true;
    }

    public void respawn(MinimapProvider minimapProvider) {
        SecondaryMinimapLayer secondaryMinimapLayer2;
        for (SecondaryMinimapLayer secondaryMinimapLayer2 : this.secondaryLayers.values()) {
            if (!(secondaryMinimapLayer2 instanceof RadarLayer)) continue;
            RadarLayer radarLayer = (RadarLayer)secondaryMinimapLayer2;
            radarLayer.invalidateTexture();
        }
        MinimapPacketSender minimapPacketSender = minimapProvider.packetSender();
        minimapPacketSender.despawnMinimap(this);
        minimapPacketSender.spawnMinimap(this);
        secondaryMinimapLayer2 = this.holder.getLocation();
        this.update(minimapProvider, secondaryMinimapLayer2.getX(), secondaryMinimapLayer2.getZ(), true);
    }

    public void resetDeathPoint(MinimapProvider minimapProvider) {
        this.persistedDeathPoint = null;
        DeathMarkerController.clear(this, minimapProvider);
    }

    public boolean removeDeathPointIfReached(MinimapProvider minimapProvider, Location location) throws SQLException {
        Config.Markers.DeathMarker deathMarker = Config.instance().markers.deathMarker;
        if (!deathMarker.enabled || !Minimap.isNearDeathPoint(this.persistedDeathPoint, location, deathMarker.removeRadiusBlocks, deathMarker.removeVerticalRangeBlocks)) {
            return false;
        }
        minimapProvider.playerDataStorage().clearDeathLocation(this.holder.getUniqueId());
        this.resetDeathPoint(minimapProvider);
        return true;
    }

    static boolean isNearDeathPoint(Location location, Location location2, double d, double d2) {
        if (location == null || location2 == null || !Double.isFinite(d) || d <= 0.0 || !Double.isFinite(d2) || d2 < 0.0) {
            return false;
        }
        World world = location.getWorld();
        World world2 = location2.getWorld();
        if (world == null || world2 == null || !world.getUID().equals(world2.getUID())) {
            return false;
        }
        double d3 = location2.getX() - location.getX();
        double d4 = location2.getZ() - location.getZ();
        return Math.abs(location2.getY() - location.getY()) <= d2 && d3 * d3 + d4 * d4 <= d * d;
    }

    public void setDeathPoint(MinimapProvider minimapProvider, Location location) {
        this.persistedDeathPoint = LocationPersister.preserveWorldIdentity(location);
        DeathMarkerController.show(this, minimapProvider, location);
    }

    public Location getDeathPoint() {
        return this.persistedDeathPoint == null ? null : this.persistedDeathPoint.clone();
    }

    public Player holder() {
        return this.holder;
    }

    public MinimapScreenPosition screenPosition() {
        return this.screenPosition;
    }

    public void screenPosition(MinimapScreenPosition minimapScreenPosition) {
        this.screenPosition = minimapScreenPosition;
    }

    public MinimapSize size() {
        return this.size;
    }

    public void size(MinimapSize minimapSize) {
        this.size = minimapSize;
    }

    public MinimapShape shape() {
        return this.shape;
    }

    public void shape(MinimapShape minimapShape) {
        this.shape = minimapShape;
    }

    public boolean dangerRingEnabled() {
        return this.dangerRingEnabled;
    }

    public void dangerRingEnabled(boolean bl) {
        this.restoreDangerRing(bl);
        this.frameFollowsTheme(false);
    }

    public void restoreDangerRing(boolean bl) {
        this.dangerRingEnabled = bl;
        this.frameStyle = bl ? 1 : 0;
    }

    public boolean fogStylingEnabled() {
        return this.fogStylingEnabled;
    }

    public void fogStylingEnabled(boolean bl) {
        this.fogStylingEnabled = bl;
        this.colorProfile = bl ? 1 : 0;
    }

    public MinimapPreset preset(int n) {
        return n >= 0 && n < this.presets.length ? this.presets[n] : null;
    }

    public void preset(int n, MinimapPreset minimapPreset) {
        if (n >= 0 && n < this.presets.length) {
            this.presets[n] = minimapPreset;
        }
    }

    public int removeExpiredTimedMarkers(MinimapProvider minimapProvider, long l) {
        int n = 0;
        Iterator<Map.Entry<String, SecondaryMinimapLayer>> iterator = this.secondaryLayers.entrySet().iterator();
        while (iterator.hasNext()) {
            MarkerMinimapLayer markerMinimapLayer;
            Map.Entry<String, SecondaryMinimapLayer> entry = iterator.next();
            SecondaryMinimapLayer secondaryMinimapLayer = entry.getValue();
            if (!(secondaryMinimapLayer instanceof MarkerMinimapLayer) || !(markerMinimapLayer = (MarkerMinimapLayer)secondaryMinimapLayer).isExpired(l)) continue;
            minimapProvider.packetSender().despawnLayer(this.holder, markerMinimapLayer.getBaseLayer());
            iterator.remove();
            ++n;
        }
        return n;
    }

    public MinimapLayer primaryLayer() {
        return this.primaryLayer;
    }

    public LinkedHashMap<String, SecondaryMinimapLayer> secondaryLayers() {
        return this.secondaryLayers;
    }

    public String toString() {
        return "Minimap(holder=" + String.valueOf(this.holder) + ", primaryLayer=" + String.valueOf(this.primaryLayer) + ", secondaryLayers=" + String.valueOf(this.secondaryLayers) + ", screenPosition=" + String.valueOf((Object)this.screenPosition) + ", size=" + String.valueOf((Object)this.size) + ", shape=" + String.valueOf((Object)this.shape) + ", dangerRingEnabled=" + this.dangerRingEnabled + ", fogStylingEnabled=" + this.fogStylingEnabled + ", presets=" + Arrays.deepToString(this.presets) + ")";
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Minimap)) {
            return false;
        }
        Minimap minimap = (Minimap)object;
        if (this.dangerRingEnabled != minimap.dangerRingEnabled) {
            return false;
        }
        if (this.fogStylingEnabled != minimap.fogStylingEnabled) {
            return false;
        }
        Player player = this.holder;
        Player player2 = minimap.holder;
        if (player == null ? player2 == null : player.equals(player2)) {
            MinimapLayer minimapLayer = this.primaryLayer;
            MinimapLayer minimapLayer2 = minimap.primaryLayer;
            if (minimapLayer == null ? minimapLayer2 == null : ((Object)minimapLayer).equals(minimapLayer2)) {
                LinkedHashMap<String, SecondaryMinimapLayer> linkedHashMap = this.secondaryLayers;
                LinkedHashMap<String, SecondaryMinimapLayer> linkedHashMap2 = minimap.secondaryLayers;
                if (linkedHashMap == null ? linkedHashMap2 == null : ((Object)linkedHashMap).equals(linkedHashMap2)) {
                    MinimapScreenPosition minimapScreenPosition = this.screenPosition;
                    MinimapScreenPosition minimapScreenPosition2 = minimap.screenPosition;
                    if (minimapScreenPosition == null ? minimapScreenPosition2 == null : ((Object)((Object)minimapScreenPosition)).equals((Object)minimapScreenPosition2)) {
                        MinimapSize minimapSize = this.size;
                        MinimapSize minimapSize2 = minimap.size;
                        if (minimapSize == null ? minimapSize2 == null : ((Object)((Object)minimapSize)).equals((Object)minimapSize2)) {
                            MinimapShape minimapShape = this.shape;
                            MinimapShape minimapShape2 = minimap.shape;
                            return (minimapShape == null ? minimapShape2 == null : ((Object)((Object)minimapShape)).equals((Object)minimapShape2)) ? Arrays.deepEquals(this.presets, minimap.presets) : false;
                        }
                        return false;
                    }
                    return false;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    public int hashCode() {
        int n = 59;
        int n2 = 1;
        n2 = n2 * 59 + (this.dangerRingEnabled ? 79 : 97);
        n2 = n2 * 59 + (this.fogStylingEnabled ? 79 : 97);
        Player player = this.holder;
        n2 = n2 * 59 + (player == null ? 43 : player.hashCode());
        MinimapLayer minimapLayer = this.primaryLayer;
        n2 = n2 * 59 + (minimapLayer == null ? 43 : ((Object)minimapLayer).hashCode());
        LinkedHashMap<String, SecondaryMinimapLayer> linkedHashMap = this.secondaryLayers;
        n2 = n2 * 59 + (linkedHashMap == null ? 43 : ((Object)linkedHashMap).hashCode());
        MinimapScreenPosition minimapScreenPosition = this.screenPosition;
        n2 = n2 * 59 + (minimapScreenPosition == null ? 43 : ((Object)((Object)minimapScreenPosition)).hashCode());
        MinimapSize minimapSize = this.size;
        n2 = n2 * 59 + (minimapSize == null ? 43 : ((Object)((Object)minimapSize)).hashCode());
        MinimapShape minimapShape = this.shape;
        n2 = n2 * 59 + (minimapShape == null ? 43 : ((Object)((Object)minimapShape)).hashCode());
        return n2 * 59 + Arrays.deepHashCode(this.presets);
    }
}

