/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map.renderer.world.cache;

import sk.enorials.minimap.map.renderer.world.WorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.cache.WorldMapCache;

public interface CacheableWorldMinimapRenderer
extends WorldMinimapRenderer {
    public WorldMapCache<?> getWorldMapCache();
}

