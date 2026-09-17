/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 */
package sk.enorials.minimap.storage;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.UUID;
import org.bukkit.Location;
import sk.enorials.minimap.map.MinimapScreenPosition;
import sk.enorials.minimap.map.MinimapShape;
import sk.enorials.minimap.map.MinimapSize;
import sk.enorials.minimap.storage.DatabaseMarkerModel;
import sk.enorials.minimap.storage.LocationPersister;

@DatabaseTable(tableName="player")
public class DatabasePlayerModel {
    @DatabaseField(id=true, dataType=DataType.UUID)
    private UUID holder;
    @DatabaseField
    private boolean enabled;
    @DatabaseField
    private MinimapScreenPosition position;
    @DatabaseField
    private MinimapSize size;
    @DatabaseField
    private MinimapShape shape;
    @DatabaseField(columnName="danger_ring_enabled")
    private Boolean dangerRingEnabled;
    @DatabaseField(columnName="fog_styling_enabled")
    private Boolean fogStylingEnabled;
    @DatabaseField(columnName="radar_aggressive_enabled")
    private Boolean radarAggressiveEnabled;
    @DatabaseField(columnName="radar_neutral_enabled")
    private Boolean radarNeutralEnabled;
    @DatabaseField(columnName="radar_friendly_enabled")
    private Boolean radarFriendlyEnabled;
    @DatabaseField(columnName="coordinates_enabled")
    private Boolean coordinatesEnabled;
    @DatabaseField(columnName="frame_style")
    private Integer frameStyle;
    @DatabaseField(columnName="color_profile")
    private Integer colorProfile;
    @DatabaseField(columnName="zoom_level")
    private Integer zoomLevel;
    @DatabaseField(columnName="combat_zoom_enabled")
    private Boolean combatZoomEnabled;
    @DatabaseField(columnName="radar_master_enabled")
    private Boolean radarMasterEnabled;
    @DatabaseField(columnName="preset_1")
    private String preset1;
    @DatabaseField(columnName="preset_2")
    private String preset2;
    @DatabaseField(columnName="preset_3")
    private String preset3;
    @DatabaseField(columnName="last_seen_at")
    private Long lastSeenAt;
    @DatabaseField(persisterClass=LocationPersister.class)
    private Location deathLocation;
    @ForeignCollectionField
    private ForeignCollection<DatabaseMarkerModel> markers;

    public UUID getHolder() {
        return this.holder;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public MinimapScreenPosition getPosition() {
        return this.position;
    }

    public MinimapSize getSize() {
        return this.size;
    }

    public MinimapShape getShape() {
        return this.shape;
    }

    public Boolean getDangerRingEnabled() {
        return this.dangerRingEnabled;
    }

    public Boolean getFogStylingEnabled() {
        return this.fogStylingEnabled;
    }

    public Boolean getRadarAggressiveEnabled() {
        return this.radarAggressiveEnabled;
    }

    public Boolean getRadarNeutralEnabled() {
        return this.radarNeutralEnabled;
    }

    public Boolean getRadarFriendlyEnabled() {
        return this.radarFriendlyEnabled;
    }

    public void setRadarAggressiveEnabled(Boolean bl) {
        this.radarAggressiveEnabled = bl;
    }

    public void setRadarNeutralEnabled(Boolean bl) {
        this.radarNeutralEnabled = bl;
    }

    public void setRadarFriendlyEnabled(Boolean bl) {
        this.radarFriendlyEnabled = bl;
    }

    public Boolean getCoordinatesEnabled() {
        return this.coordinatesEnabled;
    }

    public void setCoordinatesEnabled(Boolean bl) {
        this.coordinatesEnabled = bl;
    }

    public Integer getFrameStyle() {
        return this.frameStyle;
    }

    public void setFrameStyle(Integer n) {
        this.frameStyle = n;
    }

    public Integer getColorProfile() {
        return this.colorProfile;
    }

    public Integer getZoomLevel() {
        return this.zoomLevel;
    }

    public void setZoomLevel(Integer n) {
        this.zoomLevel = n;
    }

    public void setColorProfile(Integer n) {
        this.colorProfile = n;
    }

    public Boolean getCombatZoomEnabled() {
        return this.combatZoomEnabled;
    }

    public void setCombatZoomEnabled(Boolean bl) {
        this.combatZoomEnabled = bl;
    }

    public Boolean getRadarMasterEnabled() {
        return this.radarMasterEnabled;
    }

    public void setRadarMasterEnabled(Boolean bl) {
        this.radarMasterEnabled = bl;
    }

    public String getPreset1() {
        return this.preset1;
    }

    public String getPreset2() {
        return this.preset2;
    }

    public String getPreset3() {
        return this.preset3;
    }

    public Long getLastSeenAt() {
        return this.lastSeenAt;
    }

    public Location getDeathLocation() {
        return this.deathLocation;
    }

    public ForeignCollection<DatabaseMarkerModel> getMarkers() {
        return this.markers;
    }

    public void setHolder(UUID uUID) {
        this.holder = uUID;
    }

    public void setEnabled(boolean bl) {
        this.enabled = bl;
    }

    public void setPosition(MinimapScreenPosition minimapScreenPosition) {
        this.position = minimapScreenPosition;
    }

    public void setSize(MinimapSize minimapSize) {
        this.size = minimapSize;
    }

    public void setShape(MinimapShape minimapShape) {
        this.shape = minimapShape;
    }

    public void setDangerRingEnabled(Boolean bl) {
        this.dangerRingEnabled = bl;
    }

    public void setFogStylingEnabled(Boolean bl) {
        this.fogStylingEnabled = bl;
    }

    public void setPreset1(String string) {
        this.preset1 = string;
    }

    public void setPreset2(String string) {
        this.preset2 = string;
    }

    public void setPreset3(String string) {
        this.preset3 = string;
    }

    public void setLastSeenAt(Long l) {
        this.lastSeenAt = l;
    }

    public void setDeathLocation(Location location) {
        this.deathLocation = location;
    }

    public void setMarkers(ForeignCollection<DatabaseMarkerModel> foreignCollection) {
        this.markers = foreignCollection;
    }

    public DatabasePlayerModel() {
    }

    public DatabasePlayerModel(UUID uUID, boolean bl, MinimapScreenPosition minimapScreenPosition, MinimapSize minimapSize, MinimapShape minimapShape, Boolean bl2, Boolean bl3, String string, String string2, String string3, Long l, Location location, ForeignCollection<DatabaseMarkerModel> foreignCollection) {
        this.holder = uUID;
        this.enabled = bl;
        this.position = minimapScreenPosition;
        this.size = minimapSize;
        this.shape = minimapShape;
        this.dangerRingEnabled = bl2;
        this.fogStylingEnabled = bl3;
        this.preset1 = string;
        this.preset2 = string2;
        this.preset3 = string3;
        this.lastSeenAt = l;
        this.deathLocation = location;
        this.markers = foreignCollection;
    }
}

