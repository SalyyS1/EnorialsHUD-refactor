/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 */
package sk.enorials.minimap.map.renderer;

import org.bukkit.Location;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;

final class FullscreenMarkerPainter {
    private FullscreenMarkerPainter() {
    }

    static void draw(SecondaryMinimapLayer secondaryMinimapLayer, Location location, int n, int n2, MinimapIcon minimapIcon, MinimapIcon minimapIcon2, boolean bl, byte[] byArray) {
        int n3 = secondaryMinimapLayer.getPositionX() - (n << 7);
        int n4 = secondaryMinimapLayer.getPositionZ() - (n2 << 7);
        if (!secondaryMinimapLayer.isTrackLocation()) {
            n3 += (int)(location.getX() - 64.0);
            n4 += (int)(location.getZ() - 64.0);
        }
        if (FullscreenMarkerPainter.inside(n3, minimapIcon.width()) && FullscreenMarkerPainter.inside(n4, minimapIcon.height())) {
            float f = bl ? location.getYaw() - 180.0f : 0.0f;
            MinimapIconRenderer.paint(minimapIcon2, byArray, n3, n4, f, 2);
        }
    }

    private static boolean inside(int n, int n2) {
        return n >= -n2 && n <= 128 + n2;
    }
}

