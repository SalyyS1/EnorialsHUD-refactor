/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
 */
package sk.enorials.minimap.map;

import org.bukkit.World;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.renderer.SecondaryMinimapLayerRenderer;

public class SecondaryMinimapLayer {
    private final MinimapLayer baseLayer;
    private SecondaryMinimapLayerRenderer renderer;
    private boolean trackLocation;
    private boolean keepOnEdge;
    private World world;
    private int positionX;
    private int positionZ;
    private float depth;

    public MinimapLayer getBaseLayer() {
        return this.baseLayer;
    }

    public SecondaryMinimapLayerRenderer getRenderer() {
        return this.renderer;
    }

    public boolean isTrackLocation() {
        return this.trackLocation;
    }

    public boolean isKeepOnEdge() {
        return this.keepOnEdge;
    }

    public World getWorld() {
        return this.world;
    }

    public int getPositionX() {
        return this.positionX;
    }

    public int getPositionZ() {
        return this.positionZ;
    }

    public float getDepth() {
        return this.depth;
    }

    public void setRenderer(SecondaryMinimapLayerRenderer secondaryMinimapLayerRenderer) {
        this.renderer = secondaryMinimapLayerRenderer;
    }

    public void setTrackLocation(boolean bl) {
        this.trackLocation = bl;
    }

    public void setKeepOnEdge(boolean bl) {
        this.keepOnEdge = bl;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public void setPositionX(int n) {
        this.positionX = n;
    }

    public void setPositionZ(int n) {
        this.positionZ = n;
    }

    public void setDepth(float f) {
        this.depth = f;
    }

    public SecondaryMinimapLayer(MinimapLayer minimapLayer, SecondaryMinimapLayerRenderer secondaryMinimapLayerRenderer, boolean bl, boolean bl2, World world, int n, int n2, float f) {
        this.baseLayer = minimapLayer;
        this.renderer = secondaryMinimapLayerRenderer;
        this.trackLocation = bl;
        this.keepOnEdge = bl2;
        this.world = world;
        this.positionX = n;
        this.positionZ = n2;
        this.depth = f;
    }
}

