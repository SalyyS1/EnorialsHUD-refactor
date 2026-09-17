/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 */
package sk.enorials.minimap.map.renderer.encoder;

import org.bukkit.Location;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapScreenPosition;
import sk.enorials.minimap.map.MinimapShape;
import sk.enorials.minimap.map.MinimapSize;
import sk.enorials.minimap.map.renderer.encoder.MapEncoderUtils;

public class PrimaryMapEncoder {
    private static void field(byte[] byArray, int n, int n2, int n3) {
        MapEncoderUtils.encodeUnsigned(byArray, n, n2, n3);
    }

    public static void encodePrimaryLayer(Minimap minimap, byte[] byArray) {
        Location location = minimap.holder().getLocation();
        double d = location.getX() - (double)location.getBlockX();
        double d2 = location.getZ() - (double)location.getBlockZ();
        PrimaryMapEncoder.encodePrimaryLayer(minimap.screenPosition() == MinimapScreenPosition.RIGHT, minimap.size(), minimap.shape(), minimap.dangerRingEnabled(), minimap.fogStylingEnabled(), d, d2, byArray);
        PrimaryMapEncoder.encodeSettings(minimap, byArray);
        PrimaryMapEncoder.field(byArray, 89, 0, location.getBlockX() & 0xFF);
        PrimaryMapEncoder.field(byArray, 97, 0, location.getBlockZ() & 0xFF);
        PrimaryMapEncoder.field(byArray, 105, 0, 10);
    }

    public static void encodeSettings(Minimap minimap, byte[] byArray) {
        float f = minimap.renderZoomTarget();
        boolean bl = minimap.renderZoomAt(minimap.holder().getWorld().getGameTime()) == f;
        int n = bl ? 0 : minimap.renderZoomStartTick();
        float f2 = bl ? f : minimap.renderZoomFrom();
        int n2 = minimap.zoomLevel() * 64;
        PrimaryMapEncoder.field(byArray, 41, 0, minimap.frameStyle());
        PrimaryMapEncoder.field(byArray, 57, 0, minimap.colorProfile());
        PrimaryMapEncoder.field(byArray, 65, 0, f > 1.0f ? 1 : 0);
        PrimaryMapEncoder.field(byArray, 17, 2, n & 0xFF);
        PrimaryMapEncoder.field(byArray, 25, 2, n >>> 8 & 0xFF);
        PrimaryMapEncoder.field(byArray, 33, 2, n2 + Math.round((f2 - 1.0f) * 63.0f));
        PrimaryMapEncoder.field(byArray, 41, 2, n2 + Math.round((f - 1.0f) * 63.0f));
        PrimaryMapEncoder.field(byArray, 49, 2, 167);
    }

    public static void encodePrimaryLayer(boolean bl, MinimapSize minimapSize, MinimapShape minimapShape, boolean bl2, boolean bl3, double d, double d2, byte[] byArray) {
        MapEncoderUtils.markCorners(byArray);
        MapEncoderUtils.encodeFixedPoint(byArray, 1, 0, d);
        MapEncoderUtils.encodeFixedPoint(byArray, 9, 0, d2);
        byArray[17] = bl ? 4 : 0;
        PrimaryMapEncoder.field(byArray, 25, 0, minimapSize.ordinal());
        PrimaryMapEncoder.field(byArray, 33, 0, minimapShape.ordinal());
        PrimaryMapEncoder.field(byArray, 41, 0, bl2 ? 1 : 0);
        PrimaryMapEncoder.field(byArray, 49, 0, 1);
        PrimaryMapEncoder.field(byArray, 57, 0, bl3 ? 1 : 0);
        PrimaryMapEncoder.field(byArray, 65, 0, 0);
        PrimaryMapEncoder.field(byArray, 73, 0, 165);
        PrimaryMapEncoder.field(byArray, 81, 0, 90);
        byArray[128] = 0;
    }
}

