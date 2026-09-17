/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 */
package sk.enorials.minimap.storage;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.bukkit.Location;
import sk.enorials.minimap.storage.DatabasePlayerModel;
import sk.enorials.minimap.storage.LocationPersister;

@DatabaseTable(tableName="marker")
public class DatabaseMarkerModel {
    @DatabaseField(generatedId=true)
    private int id;
    @DatabaseField
    private String name;
    @DatabaseField(persisterClass=LocationPersister.class)
    private Location location;
    @DatabaseField
    private String icon;
    @DatabaseField(columnName="max_visible_distance", canBeNull=true)
    private Integer maxVisibleDistance;
    @DatabaseField(columnName="category")
    private String category = "GENERAL";
    @DatabaseField(columnName="created_at")
    private Long createdAt;
    @DatabaseField(columnName="expires_at")
    private Long expiresAt;
    @DatabaseField(columnName="public_marker")
    private Boolean publicMarker;
    @DatabaseField(foreign=true, foreignAutoCreate=true, foreignAutoRefresh=true)
    private DatabasePlayerModel player;

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Location getLocation() {
        return this.location;
    }

    public String getIcon() {
        return this.icon;
    }

    public Integer getMaxVisibleDistance() {
        return this.maxVisibleDistance;
    }

    public String getCategory() {
        return this.category;
    }

    public Long getCreatedAt() {
        return this.createdAt;
    }

    public Long getExpiresAt() {
        return this.expiresAt;
    }

    public Boolean getPublicMarker() {
        return this.publicMarker;
    }

    public void setPublicMarker(Boolean bl) {
        this.publicMarker = bl;
    }

    public DatabasePlayerModel getPlayer() {
        return this.player;
    }

    public void setId(int n) {
        this.id = n;
    }

    public void setName(String string) {
        this.name = string;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setIcon(String string) {
        this.icon = string;
    }

    public void setMaxVisibleDistance(Integer n) {
        this.maxVisibleDistance = n;
    }

    public void setCategory(String string) {
        this.category = string;
    }

    public void setCreatedAt(Long l) {
        this.createdAt = l;
    }

    public void setExpiresAt(Long l) {
        this.expiresAt = l;
    }

    public void setPlayer(DatabasePlayerModel databasePlayerModel) {
        this.player = databasePlayerModel;
    }

    public DatabaseMarkerModel() {
    }

    public DatabaseMarkerModel(int n, String string, Location location, String string2, Integer n2, String string3, Long l, Long l2, DatabasePlayerModel databasePlayerModel) {
        this.id = n;
        this.name = string;
        this.location = location;
        this.icon = string2;
        this.maxVisibleDistance = n2;
        this.category = string3;
        this.createdAt = l;
        this.expiresAt = l2;
        this.player = databasePlayerModel;
    }
}

