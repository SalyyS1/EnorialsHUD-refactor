/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 */
package sk.enorials.minimap.map.marker;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.marker.MarkerCategory;
import sk.enorials.minimap.map.renderer.SecondaryMinimapLayerRenderer;

public class MarkerMinimapLayer
extends SecondaryMinimapLayer {
    private Integer maxVisibleDistance;
    private MarkerCategory category;
    private Long createdAt;
    private Long expiresAt;
    private boolean publicMarker;
    private UUID storedWorldId;

    public UUID getWorldId() {
        World world = super.getWorld();
        return this.storedWorldId != null ? this.storedWorldId : (world == null ? null : world.getUID());
    }

    public void setWorldId(UUID uUID) {
        this.storedWorldId = uUID;
    }

    @Override
    public World getWorld() {
        UUID uUID = this.getWorldId();
        return uUID == null ? null : Bukkit.getWorld((UUID)uUID);
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        this.storedWorldId = world == null ? null : world.getUID();
    }

    public boolean hasUnavailableWorld() {
        return this.getWorldId() != null && this.getWorld() == null;
    }

    public boolean isPublic() {
        return this.publicMarker;
    }

    public void setPublic(boolean bl) {
        this.publicMarker = bl;
    }

    public MarkerMinimapLayer(MinimapLayer minimapLayer, SecondaryMinimapLayerRenderer secondaryMinimapLayerRenderer, boolean bl, boolean bl2, World world, int n, int n2, float f) {
        super(minimapLayer, secondaryMinimapLayerRenderer, bl, bl2, world, n, n2, f);
        this.maxVisibleDistance = null;
        this.category = MarkerCategory.GENERAL;
    }

    public MarkerMinimapLayer(MinimapLayer minimapLayer, SecondaryMinimapLayerRenderer secondaryMinimapLayerRenderer, boolean bl, boolean bl2, World world, int n, int n2, float f, Integer n3) {
        this(minimapLayer, secondaryMinimapLayerRenderer, bl, bl2, world, n, n2, f, n3, MarkerCategory.GENERAL);
    }

    public MarkerMinimapLayer(MinimapLayer minimapLayer, SecondaryMinimapLayerRenderer secondaryMinimapLayerRenderer, boolean bl, boolean bl2, World world, int n, int n2, float f, Integer n3, MarkerCategory markerCategory) {
        this(minimapLayer, secondaryMinimapLayerRenderer, bl, bl2, world, n, n2, f, n3, markerCategory, System.currentTimeMillis(), null);
    }

    public MarkerMinimapLayer(MinimapLayer minimapLayer, SecondaryMinimapLayerRenderer secondaryMinimapLayerRenderer, boolean bl, boolean bl2, World world, int n, int n2, float f, Integer n3, MarkerCategory markerCategory, Long l, Long l2) {
        super(minimapLayer, secondaryMinimapLayerRenderer, bl, bl2, world, n, n2, f);
        this.maxVisibleDistance = n3;
        this.category = markerCategory == null ? MarkerCategory.GENERAL : markerCategory;
        this.createdAt = l != null ? l : System.currentTimeMillis();
        this.expiresAt = l2;
    }

    public Integer getMaxVisibleDistance() {
        return this.maxVisibleDistance;
    }

    public void setMaxVisibleDistance(Integer n) {
        this.maxVisibleDistance = n;
    }

    public MarkerCategory getCategory() {
        return this.category;
    }

    public void setCategory(MarkerCategory markerCategory) {
        this.category = markerCategory == null ? MarkerCategory.GENERAL : markerCategory;
    }

    public Long getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Long l) {
        this.createdAt = l;
    }

    public Long getExpiresAt() {
        return this.expiresAt;
    }

    public void setExpiresAt(Long l) {
        this.expiresAt = l;
    }

    public boolean isExpired(long l) {
        return this.expiresAt != null && this.expiresAt > 0L && l >= this.expiresAt;
    }
}

