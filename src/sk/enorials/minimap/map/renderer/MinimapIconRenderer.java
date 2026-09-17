/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.jetbrains.annotations.Nullable
 */
package sk.enorials.minimap.map.renderer;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapShape;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.fullscreen.FullscreenMinimap;
import sk.enorials.minimap.map.fullscreen.FullscreenSecondaryMinimapLayer;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.marker.MarkerMinimapLayer;
import sk.enorials.minimap.map.renderer.FullscreenMarkerPainter;
import sk.enorials.minimap.map.renderer.SecondaryMinimapLayerRenderer;

public record MinimapIconRenderer(MinimapIcon icon, @Nullable MinimapIcon fullscreenIcon, boolean rotateWithHolderYaw) implements SecondaryMinimapLayerRenderer
{
    public MinimapIconRenderer(MinimapIcon minimapIcon) {
        this(minimapIcon, minimapIcon, false);
    }

    public MinimapIconRenderer(MinimapIcon minimapIcon, @Nullable MinimapIcon minimapIcon2) {
        this(minimapIcon, minimapIcon2, false);
    }

    private static boolean unavailable(SecondaryMinimapLayer secondaryMinimapLayer) {
        MarkerMinimapLayer markerMinimapLayer;
        return secondaryMinimapLayer instanceof MarkerMinimapLayer && (markerMinimapLayer = (MarkerMinimapLayer)secondaryMinimapLayer).hasUnavailableWorld();
    }

    @Override
    public void render(Minimap minimap, SecondaryMinimapLayer secondaryMinimapLayer, byte[] byArray) {
        if (!HudAccess.layer(minimap.holder(), secondaryMinimapLayer) || MinimapIconRenderer.unavailable(secondaryMinimapLayer)) {
            return;
        }
        if (!Config.instance().resourcePack.fallbackMode()) {
            MinimapIconRenderer.paint(this.icon, byArray, 64, 64, 0.0f, 0);
            return;
        }
        Location location = minimap.holder().getLocation();
        double d = secondaryMinimapLayer.getPositionX();
        double d2 = secondaryMinimapLayer.getPositionZ();
        if (secondaryMinimapLayer.isTrackLocation()) {
            d = location.getZ() - (double)secondaryMinimapLayer.getPositionZ();
            d2 = location.getX() - (double)secondaryMinimapLayer.getPositionX();
            if (secondaryMinimapLayer.isKeepOnEdge()) {
                int n = Math.max(0, this.icon.width() / 2);
                int n2 = Math.max(0, this.icon.height() / 2);
                if (minimap.shape() == MinimapShape.SQUARE) {
                    double d3 = Math.max(1, 63 - n);
                    double d4 = Math.max(1, 63 - n2);
                    if (Math.abs(d) > d3 || Math.abs(d2) > d4) {
                        double d5 = Math.min(d3 / Math.max(1.0E-6, Math.abs(d)), d4 / Math.max(1.0E-6, Math.abs(d2)));
                        d = Math.round(d * d5);
                        d2 = Math.round(d2 * d5);
                    }
                } else {
                    double d6 = d * d + d2 * d2;
                    double d7 = Math.max(1, 60 - Math.max(n, n2));
                    if (d6 > d7 * d7) {
                        double d8 = Math.sqrt(d6);
                        d = d / d8 * d7;
                        d2 = d2 / d8 * d7;
                    }
                }
            }
            d += 64.0;
            d2 += 64.0;
        }
        MinimapIconRenderer.paint(this.icon, byArray, (int)Math.round(d), (int)Math.round(d2), this.rotateWithHolderYaw ? 180.0f - location.getYaw() : 0.0f, 1);
    }

    @Override
    public void renderFullscreen(FullscreenMinimap fullscreenMinimap, FullscreenSecondaryMinimapLayer fullscreenSecondaryMinimapLayer, int n, int n2, byte[] byArray) {
        if (this.fullscreenIcon == null || !HudAccess.layer(fullscreenMinimap.getHolder(), fullscreenSecondaryMinimapLayer.base()) || MinimapIconRenderer.unavailable(fullscreenSecondaryMinimapLayer.base())) {
            return;
        }
        FullscreenMarkerPainter.draw(fullscreenSecondaryMinimapLayer.base(), fullscreenMinimap.getHolder().getLocation(), n, n2, this.icon, this.fullscreenIcon, this.rotateWithHolderYaw, byArray);
    }

    static void paint(MinimapIcon minimapIcon, byte[] byArray, int n, int n2, float f, int n3) {
        int n4 = minimapIcon.width();
        int n5 = minimapIcon.height();
        int n6 = n - n4 / 2;
        int n7 = n2 - n5 / 2;
        int n8 = Math.max(0, -n6);
        int n9 = Math.max(0, -n7);
        int n10 = Math.min(n4, 128 - n6);
        int n11 = Math.min(n5, 128 - n7);
        double d = Math.toRadians(f);
        double d2 = Math.sin(d);
        double d3 = Math.cos(d);
        double d4 = (double)(n4 - 1) / 2.0;
        double d5 = (double)(n5 - 1) / 2.0;
        for (int i = n9; i < n11; ++i) {
            for (int j = n8; j < n10; ++j) {
                int n12 = j;
                int n13 = i;
                if (f != 0.0f) {
                    double d6 = (double)j - d4;
                    double d7 = (double)i - d5;
                    n12 = (int)Math.round(d3 * d6 + d2 * d7 + d4);
                    n13 = (int)Math.round(-d2 * d6 + d3 * d7 + d5);
                    if (n12 < 0 || n12 >= n4 || n13 < 0 || n13 >= n5) continue;
                }
                int n14 = n13 * n4 + (n3 == 1 ? n4 - 1 - n12 : n12);
                byte by = minimapIcon.data()[n14];
                if (by == 0) continue;
                int n15 = n6 + j;
                int n16 = n7 + i;
                int n17 = switch (n3) {
                    case 1 -> n15 * 128 + n16;
                    case 2 -> (127 - n15) * 128 + n16;
                    default -> n16 * 128 + n15;
                };
                byArray[n17] = by;
            }
        }
    }
}

