/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 */
package sk.enorials.minimap.radar;

import org.bukkit.Location;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.minimap.radar.RadarCategory;

public final class RadarLayer
extends SecondaryMinimapLayer {
    public static final String PREFIX = "__radar::";
    private RadarCategory category;
    private double exactX;
    private double exactZ;
    private double deltaX;
    private double deltaZ;
    private long sampleTick;
    private int intervalTicks = 2;
    private boolean fullTextureNeeded = true;

    public RadarLayer(MinimapLayer minimapLayer, Location location, RadarCategory radarCategory, long l) {
        super(minimapLayer, new MinimapIconRenderer(radarCategory.icon()), true, false, location.getWorld(), location.getBlockX(), location.getBlockZ(), 0.035f);
        this.category = radarCategory;
        this.exactX = location.getX();
        this.exactZ = location.getZ();
        this.sampleTick = l;
    }

    public void sample(Location location, RadarCategory radarCategory, long l) {
        if (radarCategory != this.category) {
            this.category = radarCategory;
            this.setRenderer(new MinimapIconRenderer(radarCategory.icon()));
            this.fullTextureNeeded = true;
        }
        if (l != this.sampleTick) {
            long l2 = l - this.sampleTick;
            double d = location.getX() - this.exactX;
            double d2 = location.getZ() - this.exactZ;
            boolean bl = l2 > 0L && l2 <= 10L && d * d + d2 * d2 < 16.0;
            this.deltaX = bl ? d : 0.0;
            this.deltaZ = bl ? d2 : 0.0;
            this.intervalTicks = bl ? (int)l2 : 2;
            this.sampleTick = l;
        }
        this.exactX = location.getX();
        this.exactZ = location.getZ();
        this.setPositionX(location.getBlockX());
        this.setPositionZ(location.getBlockZ());
        this.setWorld(location.getWorld());
    }

    public RadarCategory category() {
        return this.category;
    }

    public void reuse(Location location, RadarCategory radarCategory, long l) {
        this.category = radarCategory;
        this.setRenderer(new MinimapIconRenderer(radarCategory.icon()));
        this.exactX = location.getX();
        this.exactZ = location.getZ();
        this.deltaZ = 0.0;
        this.deltaX = 0.0;
        this.sampleTick = l;
        this.intervalTicks = 2;
        this.setPositionX(location.getBlockX());
        this.setPositionZ(location.getBlockZ());
        this.setWorld(location.getWorld());
        this.invalidateTexture();
    }

    public double exactX() {
        return this.exactX;
    }

    public double exactZ() {
        return this.exactZ;
    }

    public double deltaX() {
        return this.deltaX;
    }

    public double deltaZ() {
        return this.deltaZ;
    }

    public long sampleTick() {
        return this.sampleTick;
    }

    public int intervalTicks() {
        return this.intervalTicks;
    }

    public void invalidateTexture() {
        this.fullTextureNeeded = true;
    }

    public boolean consumeFullTexture() {
        boolean bl = this.fullTextureNeeded;
        this.fullTextureNeeded = false;
        return bl;
    }
}

