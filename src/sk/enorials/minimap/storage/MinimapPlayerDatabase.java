/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.storage;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.field.DataPersisterManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.listener.MinimapListener;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapPreset;
import sk.enorials.minimap.map.MinimapProvider;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.marker.MarkerCategory;
import sk.enorials.minimap.map.marker.MarkerMinimapLayer;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.minimap.radar.RadarCategory;
import sk.enorials.minimap.storage.DatabaseMarkerModel;
import sk.enorials.minimap.storage.DatabasePlayerModel;
import sk.enorials.minimap.storage.LocationPersister;

public class MinimapPlayerDatabase {
    private final JdbcPooledConnectionSource connectionSource;
    private final Dao<DatabasePlayerModel, UUID> playerDao;
    private final Dao<DatabaseMarkerModel, Integer> markerDao;
    private final Object writes = new Object();
    private final Object queueLock = new Object();
    private final ConcurrentMap<UUID, SavedState> latest = new ConcurrentHashMap<UUID, SavedState>();
    private final LinkedHashMap<UUID, SavedState> pending = new LinkedHashMap();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "EnorialsHUD-minimap-save");
        thread.setDaemon(true);
        return thread;
    });
    private boolean draining;
    private boolean closing;

    public MinimapPlayerDatabase(Path path) throws SQLException {
        DataPersisterManager.registerDataPersisters(LocationPersister.getSingleton());
        this.connectionSource = new JdbcPooledConnectionSource("jdbc:sqlite:" + String.valueOf(path.toAbsolutePath()));
        TableUtils.createTableIfNotExists((ConnectionSource)this.connectionSource, DatabasePlayerModel.class);
        TableUtils.createTableIfNotExists((ConnectionSource)this.connectionSource, DatabaseMarkerModel.class);
        this.playerDao = DaoManager.createDao((ConnectionSource)this.connectionSource, DatabasePlayerModel.class);
        this.markerDao = DaoManager.createDao((ConnectionSource)this.connectionSource, DatabaseMarkerModel.class);
        this.ensureSchema();
    }

    private void ensureSchema() throws SQLException {
        for (String string : new String[]{"coordinates_enabled", "combat_zoom_enabled", "radar_master_enabled"}) {
            if (this.hasColumn("player", string)) continue;
            this.playerDao.executeRaw("ALTER TABLE player ADD COLUMN " + string + " BOOLEAN", new String[0]);
        }
        for (String string : new String[]{"frame_style", "color_profile", "zoom_level"}) {
            if (this.hasColumn("player", string)) continue;
            this.playerDao.executeRaw("ALTER TABLE player ADD COLUMN " + string + " INTEGER", new String[0]);
        }
        if (!this.hasColumn("marker", "public_marker")) {
            this.markerDao.executeRaw("ALTER TABLE marker ADD COLUMN public_marker BOOLEAN", new String[0]);
        }
        for (String string : new String[]{"aggressive", "neutral", "friendly"}) {
            if (this.hasColumn("player", "radar_" + string + "_enabled")) continue;
            this.playerDao.executeRaw("ALTER TABLE player ADD COLUMN radar_" + string + "_enabled BOOLEAN", new String[0]);
        }
        if (!this.hasColumn("player", "size")) {
            this.playerDao.executeRaw("ALTER TABLE player ADD COLUMN size VARCHAR DEFAULT 'LARGE'", new String[0]);
        }
        if (!this.hasColumn("player", "shape")) {
            this.playerDao.executeRaw("ALTER TABLE player ADD COLUMN shape VARCHAR DEFAULT 'CIRCLE'", new String[0]);
        }
        if (!this.hasColumn("player", "danger_ring_enabled")) {
            this.playerDao.executeRaw("ALTER TABLE player ADD COLUMN danger_ring_enabled BOOLEAN DEFAULT 0", new String[0]);
        }
        if (!this.hasColumn("player", "fog_styling_enabled")) {
            this.playerDao.executeRaw("ALTER TABLE player ADD COLUMN fog_styling_enabled BOOLEAN DEFAULT 1", new String[0]);
        }
        if (!this.hasColumn("player", "preset_1")) {
            this.playerDao.executeRaw("ALTER TABLE player ADD COLUMN preset_1 VARCHAR", new String[0]);
        }
        if (!this.hasColumn("player", "preset_2")) {
            this.playerDao.executeRaw("ALTER TABLE player ADD COLUMN preset_2 VARCHAR", new String[0]);
        }
        if (!this.hasColumn("player", "preset_3")) {
            this.playerDao.executeRaw("ALTER TABLE player ADD COLUMN preset_3 VARCHAR", new String[0]);
        }
        if (!this.hasColumn("player", "last_seen_at")) {
            this.playerDao.executeRaw("ALTER TABLE player ADD COLUMN last_seen_at BIGINT", new String[0]);
        }
        if (!this.hasColumn("marker", "max_visible_distance")) {
            this.playerDao.executeRaw("ALTER TABLE marker ADD COLUMN max_visible_distance INTEGER", new String[0]);
        }
        if (!this.hasColumn("marker", "category")) {
            this.playerDao.executeRaw("ALTER TABLE marker ADD COLUMN category VARCHAR DEFAULT 'GENERAL'", new String[0]);
        }
        if (!this.hasColumn("marker", "created_at")) {
            this.playerDao.executeRaw("ALTER TABLE marker ADD COLUMN created_at BIGINT", new String[0]);
        }
        if (!this.hasColumn("marker", "expires_at")) {
            this.playerDao.executeRaw("ALTER TABLE marker ADD COLUMN expires_at BIGINT", new String[0]);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean hasColumn(String string, String string2) throws SQLException {
        GenericRawResults<String[]> genericRawResults = null;
        try {
            genericRawResults = this.playerDao.queryRaw("PRAGMA table_info(" + string + ")", new String[0]);
            Iterator<String[]> iterator = genericRawResults.getResults().iterator();
            while (true) {
                if (iterator.hasNext()) {
                    String[] stringArray = iterator.next();
                    if (stringArray.length <= 1 || !string2.equalsIgnoreCase(stringArray[1])) continue;
                    boolean bl = true;
                    return bl;
                    continue;
                }
                break;
            }
        }
        finally {
            if (genericRawResults != null) {
                try {
                    genericRawResults.close();
                }
                catch (Exception exception) {}
            }
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public DatabasePlayerModel queryPlayer(UUID uUID) throws SQLException {
        Object object = this.writes;
        synchronized (object) {
            this.flush(uUID);
            return this.playerDao.queryForId(uUID);
        }
    }

    public DatabasePlayerModel queryPlayer(Player player) throws SQLException {
        return this.queryPlayer(player.getUniqueId());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int clearDeathLocation(UUID uUID) throws SQLException {
        Object object = this.writes;
        synchronized (object) {
            this.flush(uUID);
            UpdateBuilder<DatabasePlayerModel, UUID> updateBuilder = this.playerDao.updateBuilder();
            updateBuilder.updateColumnValue("deathLocation", null);
            updateBuilder.where().eq("holder", uUID);
            return updateBuilder.update();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<DatabaseMarkerModel> publicMarkers() throws SQLException {
        Object object = this.writes;
        synchronized (object) {
            this.flushAll();
            long l = System.currentTimeMillis();
            return this.markerDao.queryForEq("public_marker", true).stream().filter(databaseMarkerModel -> databaseMarkerModel.getExpiresAt() == null || databaseMarkerModel.getExpiresAt() <= 0L || databaseMarkerModel.getExpiresAt() > l).toList();
        }
    }

    public void restore(MinimapProvider minimapProvider, Player player) throws Exception {
        if (!HudAccess.map(player)) {
            return;
        }
        MinimapListener minimapListener = minimapProvider.minimapListener();
        minimapListener.disableMinimap(player);
        DatabasePlayerModel databasePlayerModel = this.queryPlayer(player);
        if (databasePlayerModel == null) {
            if (!Config.instance().enabledByDefault) {
                return;
            }
        } else {
            if (!databasePlayerModel.isEnabled()) {
                return;
            }
            this.removeExpiredMarkersForPlayer(player.getUniqueId(), System.currentTimeMillis());
            databasePlayerModel = this.queryPlayer(player);
            if (databasePlayerModel == null) {
                return;
            }
        }
        Minimap minimap = minimapListener.enableMinimap(player);
        if (databasePlayerModel != null) {
            this.applySettings(minimap, databasePlayerModel);
            if (databasePlayerModel.getDeathLocation() != null) {
                minimap.setDeathPoint(minimapProvider, databasePlayerModel.getDeathLocation());
            }
            long l = System.currentTimeMillis();
            for (DatabaseMarkerModel databaseMarkerModel : this.markerDao.queryForEq("player_id", player.getUniqueId())) {
                Long l2 = databaseMarkerModel.getExpiresAt();
                if (l2 != null && l2 > 0L && l2 <= l) continue;
                this.restoreMarker(minimapProvider, minimap, databaseMarkerModel);
            }
            minimap.update(minimapProvider);
        }
        if (minimapProvider instanceof EnorialsMinimap) {
            EnorialsMinimap enorialsMinimap = (EnorialsMinimap)minimapProvider;
            enorialsMinimap.menuListener().applyPublicMarkers(minimap);
            enorialsMinimap.menuListener().applyHiddenMarkers(minimap);
        }
    }

    private void restoreMarker(MinimapProvider minimapProvider, Minimap minimap, DatabaseMarkerModel databaseMarkerModel) {
        MinimapIcon minimapIcon = minimapProvider.iconProvider().getIcon(databaseMarkerModel.getIcon());
        if (minimapIcon == null) {
            return;
        }
        Location location = databaseMarkerModel.getLocation();
        Player player = minimap.holder();
        MinimapLayer minimapLayer = minimapProvider.clientsideMinimapFactory().createMinimapLayer(player.getWorld(), null);
        MarkerMinimapLayer markerMinimapLayer = new MarkerMinimapLayer(minimapLayer, new MinimapIconRenderer(minimapIcon), true, Config.instance().markers.customMarkers.stickToBorder, location.getWorld(), location.getBlockX(), location.getBlockZ(), 0.05f + (float)minimap.secondaryLayers().size() * 0.01f, databaseMarkerModel.getMaxVisibleDistance(), MarkerCategory.fromString(databaseMarkerModel.getCategory()), databaseMarkerModel.getCreatedAt(), databaseMarkerModel.getExpiresAt());
        markerMinimapLayer.setWorldId(LocationPersister.worldIdentity(location));
        markerMinimapLayer.setPublic(Boolean.TRUE.equals(databaseMarkerModel.getPublicMarker()));
        minimap.secondaryLayers().put(databaseMarkerModel.getName(), markerMinimapLayer);
        if (!markerMinimapLayer.hasUnavailableWorld() && (markerMinimapLayer.getWorld() == null || player.getWorld().equals((Object)markerMinimapLayer.getWorld()))) {
            minimapProvider.packetSender().spawnLayer(player, minimapLayer);
        }
    }

    private static <T> T orDefault(T t, T t2) {
        return t == null ? t2 : t;
    }

    private void applySettings(Minimap minimap, DatabasePlayerModel databasePlayerModel) {
        Config config = Config.instance();
        minimap.screenPosition(databasePlayerModel.getPosition());
        minimap.size(MinimapPlayerDatabase.orDefault(databasePlayerModel.getSize(), config.defaultSize));
        minimap.shape(MinimapPlayerDatabase.orDefault(databasePlayerModel.getShape(), config.defaultShape));
        minimap.restoreDangerRing(MinimapPlayerDatabase.orDefault(databasePlayerModel.getDangerRingEnabled(), config.defaultDangerRingEnabled));
        minimap.fogStylingEnabled(MinimapPlayerDatabase.orDefault(databasePlayerModel.getFogStylingEnabled(), config.defaultFogStylingEnabled));
        minimap.coordinatesEnabled(MinimapPlayerDatabase.orDefault(databasePlayerModel.getCoordinatesEnabled(), config.coordinates.enabled));
        if (databasePlayerModel.getFrameStyle() != null) {
            minimap.restoreFrameStyle(databasePlayerModel.getFrameStyle());
        }
        if (databasePlayerModel.getColorProfile() != null) {
            minimap.colorProfile(databasePlayerModel.getColorProfile());
        }
        minimap.combatZoomEnabled(MinimapPlayerDatabase.orDefault(databasePlayerModel.getCombatZoomEnabled(), config.defaultCombatZoomEnabled));
        minimap.zoomLevel(MinimapPlayerDatabase.orDefault(databasePlayerModel.getZoomLevel(), 0));
        minimap.radarMasterEnabled(MinimapPlayerDatabase.orDefault(databasePlayerModel.getRadarMasterEnabled(), true));
        minimap.radarEnabled(RadarCategory.AGGRESSIVE, MinimapPlayerDatabase.orDefault(databasePlayerModel.getRadarAggressiveEnabled(), config.radar.defaultAggressiveEnabled));
        minimap.radarEnabled(RadarCategory.NEUTRAL, MinimapPlayerDatabase.orDefault(databasePlayerModel.getRadarNeutralEnabled(), config.radar.defaultNeutralEnabled));
        minimap.radarEnabled(RadarCategory.FRIENDLY, MinimapPlayerDatabase.orDefault(databasePlayerModel.getRadarFriendlyEnabled(), config.radar.defaultFriendlyEnabled));
        String[] stringArray = new String[]{databasePlayerModel.getPreset1(), databasePlayerModel.getPreset2(), databasePlayerModel.getPreset3()};
        for (int i = 0; i < stringArray.length; ++i) {
            minimap.preset(i, MinimapPreset.deserialize(stringArray[i]));
        }
    }

    private SavedState capture(Minimap minimap) {
        DatabasePlayerModel databasePlayerModel = new DatabasePlayerModel();
        databasePlayerModel.setHolder(minimap.holder().getUniqueId());
        databasePlayerModel.setEnabled(true);
        databasePlayerModel.setPosition(minimap.screenPosition());
        databasePlayerModel.setSize(minimap.size());
        databasePlayerModel.setShape(minimap.shape());
        databasePlayerModel.setDangerRingEnabled(minimap.dangerRingEnabled());
        databasePlayerModel.setFogStylingEnabled(minimap.fogStylingEnabled());
        databasePlayerModel.setCoordinatesEnabled(minimap.coordinatesEnabled());
        databasePlayerModel.setFrameStyle(minimap.frameStyle());
        databasePlayerModel.setColorProfile(minimap.colorProfile());
        databasePlayerModel.setCombatZoomEnabled(minimap.combatZoomEnabled());
        databasePlayerModel.setZoomLevel(minimap.zoomLevel());
        databasePlayerModel.setRadarMasterEnabled(minimap.radarMasterEnabled());
        databasePlayerModel.setRadarAggressiveEnabled(minimap.radarEnabled(RadarCategory.AGGRESSIVE));
        databasePlayerModel.setRadarNeutralEnabled(minimap.radarEnabled(RadarCategory.NEUTRAL));
        databasePlayerModel.setRadarFriendlyEnabled(minimap.radarEnabled(RadarCategory.FRIENDLY));
        databasePlayerModel.setPreset1(minimap.preset(0) == null ? null : minimap.preset(0).serialize());
        databasePlayerModel.setPreset2(minimap.preset(1) == null ? null : minimap.preset(1).serialize());
        databasePlayerModel.setPreset3(minimap.preset(2) == null ? null : minimap.preset(2).serialize());
        databasePlayerModel.setLastSeenAt(System.currentTimeMillis());
        Location location = Config.instance().markers.deathMarker.enabled ? minimap.getDeathPoint() : null;
        databasePlayerModel.setDeathLocation(location == null ? null : location.clone());
        ArrayList<DatabaseMarkerModel> arrayList = new ArrayList<DatabaseMarkerModel>();
        for (Map.Entry<String, SecondaryMinimapLayer> entry : minimap.secondaryLayers().entrySet()) {
            MarkerMinimapLayer markerMinimapLayer;
            Object object;
            String string = entry.getKey();
            if (string.equals("player") || string.equals("death_point") || string.equals("__coords__") || string.startsWith("public::") || !((object = entry.getValue()) instanceof MarkerMinimapLayer) || !((object = (markerMinimapLayer = (MarkerMinimapLayer)object).getRenderer()) instanceof MinimapIconRenderer)) continue;
            MinimapIconRenderer minimapIconRenderer = (MinimapIconRenderer)object;
            object = LocationPersister.atWorld(markerMinimapLayer.getWorldId(), markerMinimapLayer.getPositionX(), 0.0, markerMinimapLayer.getPositionZ());
            DatabaseMarkerModel databaseMarkerModel = new DatabaseMarkerModel(0, string, (Location)object, minimapIconRenderer.icon().key(), markerMinimapLayer.getMaxVisibleDistance(), markerMinimapLayer.getCategory().name(), markerMinimapLayer.getCreatedAt(), markerMinimapLayer.getExpiresAt(), databasePlayerModel);
            databaseMarkerModel.setPublicMarker(markerMinimapLayer.isPublic());
            arrayList.add(databaseMarkerModel);
        }
        return new SavedState(databasePlayerModel, List.copyOf(arrayList));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void save(Minimap minimap) throws Exception {
        SavedState savedState = this.capture(minimap);
        Object object = this.queueLock;
        synchronized (object) {
            if (this.closing) {
                throw new IllegalStateException("Minimap storage is closed");
            }
            this.latest.put(savedState.player().getHolder(), savedState);
        }
        object = this.writes;
        synchronized (object) {
            this.write(savedState);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void queueSave(Minimap minimap, Logger logger) {
        SavedState savedState = this.capture(minimap);
        UUID uUID = savedState.player().getHolder();
        Object object = this.queueLock;
        synchronized (object) {
            if (this.closing) {
                return;
            }
            this.latest.put(uUID, savedState);
            this.pending.put(uUID, savedState);
            if (!this.draining) {
                this.draining = true;
                this.writer.execute(() -> this.drain(logger));
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void drain(Logger logger) {
        while (true) {
            SavedState savedState;
            Object object = this.queueLock;
            synchronized (object) {
                if (this.pending.isEmpty()) {
                    this.draining = false;
                    return;
                }
                Iterator<Map.Entry<UUID, SavedState>> iterator = this.pending.entrySet().iterator();
                savedState = iterator.next().getValue();
                iterator.remove();
            }
            try {
                object = this.writes;
                synchronized (object) {
                    this.write(savedState);
                    continue;
                }
            }
            catch (Exception exception) {
                logger.log(Level.WARNING, "Could not save minimap for " + String.valueOf(savedState.player().getHolder()), exception);
                continue;
            }
            break;
        }
    }

    private void write(SavedState savedState) throws SQLException {
        UUID uUID = savedState.player().getHolder();
        if (this.latest.get(uUID) != savedState) {
            return;
        }
        TransactionManager.callInTransaction(this.connectionSource, () -> {
            DeleteBuilder<DatabaseMarkerModel, Integer> deleteBuilder = this.markerDao.deleteBuilder();
            deleteBuilder.where().eq("player_id", uUID);
            deleteBuilder.delete();
            this.playerDao.createOrUpdate(savedState.player());
            long l = System.currentTimeMillis();
            for (DatabaseMarkerModel databaseMarkerModel : savedState.markers()) {
                if (databaseMarkerModel.getExpiresAt() != null && databaseMarkerModel.getExpiresAt() > 0L && databaseMarkerModel.getExpiresAt() <= l) continue;
                this.markerDao.create(databaseMarkerModel);
            }
            return null;
        });
        this.latest.remove(uUID, savedState);
    }

    private void flush(UUID uUID) throws SQLException {
        SavedState savedState = (SavedState)this.latest.get(uUID);
        if (savedState != null) {
            this.write(savedState);
        }
    }

    private void flushAll() throws SQLException {
        for (SavedState savedState : List.copyOf(this.latest.values())) {
            this.write(savedState);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void disableMinimap(Player player) throws SQLException {
        Object object = this.writes;
        synchronized (object) {
            this.flush(player.getUniqueId());
            DatabasePlayerModel databasePlayerModel = this.queryPlayer(player);
            if (databasePlayerModel == null) {
                databasePlayerModel = new DatabasePlayerModel();
                databasePlayerModel.setHolder(player.getUniqueId());
                databasePlayerModel.setPosition(Config.instance().defaultPosition);
                databasePlayerModel.setSize(Config.instance().defaultSize);
                databasePlayerModel.setShape(Config.instance().defaultShape);
                databasePlayerModel.setDangerRingEnabled(Config.instance().defaultDangerRingEnabled);
                databasePlayerModel.setFogStylingEnabled(Config.instance().defaultFogStylingEnabled);
            }
            databasePlayerModel.setEnabled(false);
            databasePlayerModel.setLastSeenAt(System.currentTimeMillis());
            this.playerDao.createOrUpdate(databasePlayerModel);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void enableMinimap(Player player) throws SQLException {
        Object object = this.writes;
        synchronized (object) {
            this.flush(player.getUniqueId());
            DatabasePlayerModel databasePlayerModel = this.queryPlayer(player);
            if (databasePlayerModel == null) {
                databasePlayerModel = new DatabasePlayerModel();
                databasePlayerModel.setHolder(player.getUniqueId());
                databasePlayerModel.setPosition(Config.instance().defaultPosition);
                databasePlayerModel.setSize(Config.instance().defaultSize);
                databasePlayerModel.setShape(Config.instance().defaultShape);
                databasePlayerModel.setDangerRingEnabled(Config.instance().defaultDangerRingEnabled);
                databasePlayerModel.setFogStylingEnabled(Config.instance().defaultFogStylingEnabled);
            }
            databasePlayerModel.setEnabled(true);
            databasePlayerModel.setLastSeenAt(System.currentTimeMillis());
            this.playerDao.createOrUpdate(databasePlayerModel);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int removeExpiredMarkersForPlayer(UUID uUID, long l) throws SQLException {
        Object object = this.writes;
        synchronized (object) {
            this.flush(uUID);
            DeleteBuilder<DatabaseMarkerModel, Integer> deleteBuilder = this.markerDao.deleteBuilder();
            deleteBuilder.where().eq("player_id", uUID).and().isNotNull("expires_at").and().le("expires_at", l);
            int n = deleteBuilder.delete();
            DatabasePlayerModel databasePlayerModel = this.queryPlayer(uUID);
            if (databasePlayerModel != null) {
                databasePlayerModel.setLastSeenAt(l);
                this.playerDao.update(databasePlayerModel);
            }
            return n;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int cleanupExpiredData(long l) throws SQLException {
        Object object = this.writes;
        synchronized (object) {
            this.flushAll();
            DeleteBuilder<DatabaseMarkerModel, Integer> deleteBuilder = this.markerDao.deleteBuilder();
            deleteBuilder.where().isNotNull("expires_at").and().le("expires_at", l);
            int n = deleteBuilder.delete();
            int n2 = Config.instance().dataRetention.playerDataDays;
            if (n2 > 0) {
                long l2 = l - (long)n2 * 24L * 60L * 60L * 1000L;
                for (DatabasePlayerModel databasePlayerModel : this.playerDao.queryForAll()) {
                    Long l3 = databasePlayerModel.getLastSeenAt();
                    if (l3 == null || l3 >= l2) continue;
                    DeleteBuilder<DatabaseMarkerModel, Integer> deleteBuilder2 = this.markerDao.deleteBuilder();
                    deleteBuilder2.where().eq("player_id", databasePlayerModel.getHolder());
                    deleteBuilder2.delete();
                    this.playerDao.deleteById(databasePlayerModel.getHolder());
                    ++n;
                }
            }
            return n;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void close() throws Exception {
        Object object = this.queueLock;
        synchronized (object) {
            this.closing = true;
            this.writer.shutdown();
        }
        if (!this.writer.awaitTermination(10L, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Minimap writer did not finish before shutdown");
        }
        object = this.writes;
        synchronized (object) {
            this.flushAll();
            this.connectionSource.close();
        }
    }

    private record SavedState(DatabasePlayerModel player, List<DatabaseMarkerModel> markers) {
    }
}

