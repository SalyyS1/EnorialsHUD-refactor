/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map.renderer;

import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.fullscreen.FullscreenMinimap;
import sk.enorials.minimap.map.fullscreen.FullscreenSecondaryMinimapLayer;

public interface SecondaryMinimapLayerRenderer {
    public void render(Minimap var1, SecondaryMinimapLayer var2, byte[] var3);

    public void renderFullscreen(FullscreenMinimap var1, FullscreenSecondaryMinimapLayer var2, int var3, int var4, byte[] var5);
}

