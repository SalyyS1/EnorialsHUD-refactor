/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map;

import sk.enorials.minimap.clientside.EntityHandle;
import sk.enorials.minimap.map.renderer.MinimapLayerRenderer;

public record MinimapLayer(int mapId, EntityHandle<?> lowerFrame, EntityHandle<?> upperFrame, MinimapLayerRenderer renderer) {
}

