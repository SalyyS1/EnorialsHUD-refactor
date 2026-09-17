/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 */
package sk.enorials.minimap.map.renderer.encoder;

import org.bukkit.Location;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapScreenPosition;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.marker.MarkerMinimapLayer;
import sk.enorials.minimap.map.renderer.CoordinatesOverlayRenderer;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.minimap.map.renderer.SecondaryMinimapLayerRenderer;
import sk.enorials.minimap.map.renderer.encoder.MapEncoderUtils;
import sk.enorials.minimap.map.renderer.encoder.PrimaryMapEncoder;
import sk.enorials.minimap.radar.RadarLayer;

public class SecondaryMapEncoder {
    private static Projection position(Location location, SecondaryMinimapLayer secondaryMinimapLayer) {
        if (!secondaryMinimapLayer.isTrackLocation()) {
            return new Projection(secondaryMinimapLayer.getPositionX(), secondaryMinimapLayer.getPositionZ(), 0.0, 0.0);
        }
        double d = location.getZ() - (double)secondaryMinimapLayer.getPositionZ();
        double d2 = location.getX() - (double)secondaryMinimapLayer.getPositionX();
        double d3 = location.getX() - Math.floor(location.getX());
        double d4 = location.getZ() - Math.floor(location.getZ());
        double d5 = d * d + d2 * d2;
        if (secondaryMinimapLayer.isKeepOnEdge() && d5 > 3600.0) {
            double d6 = Math.sqrt(d5);
            d = d / d6 * 60.0;
            d2 = d2 / d6 * 60.0;
            d3 = 0.0;
            d4 = 0.0;
        }
        return new Projection(d + 64.0, d2 + 64.0, d3, d4);
    }

    private static boolean visible(Minimap minimap, SecondaryMinimapLayer secondaryMinimapLayer, Location location) {
        MarkerMinimapLayer markerMinimapLayer;
        Integer n;
        double d;
        double d2;
        if (!HudAccess.layer(minimap.holder(), secondaryMinimapLayer)) {
            return false;
        }
        if (secondaryMinimapLayer instanceof RadarLayer) {
            RadarLayer radarLayer = (RadarLayer)secondaryMinimapLayer;
            v0 = radarLayer.exactX();
        } else {
            v0 = d2 = (double)secondaryMinimapLayer.getPositionX();
        }
        if (secondaryMinimapLayer instanceof RadarLayer) {
            RadarLayer radarLayer = (RadarLayer)secondaryMinimapLayer;
            d = radarLayer.exactZ();
        } else {
            d = secondaryMinimapLayer.getPositionZ();
        }
        double d3 = d;
        double d4 = location.getX() - d2;
        double d5 = location.getZ() - d3;
        double d6 = d4 * d4 + d5 * d5;
        if (secondaryMinimapLayer.isTrackLocation() && !secondaryMinimapLayer.isKeepOnEdge() && d6 >= 4096.0) {
            return false;
        }
        return !(secondaryMinimapLayer instanceof MarkerMinimapLayer) || (n = (markerMinimapLayer = (MarkerMinimapLayer)secondaryMinimapLayer).getMaxVisibleDistance()) == null || n <= 0 || !(d6 > (double)n.intValue() * (double)n.intValue());
    }

    private static void number(byte[] byArray, int n, int n2, int n3, int n4) {
        for (int i = 0; i < n4; ++i) {
            MapEncoderUtils.encodeUnsigned(byArray, n + 8 * i, n2, n3 >>> i * 8);
        }
    }

    public static void encodeSecondaryLayer(Minimap minimap, SecondaryMinimapLayer secondaryMinimapLayer, byte[] byArray) {
        SecondaryMinimapLayerRenderer secondaryMinimapLayerRenderer;
        Location location = minimap.holder().getLocation();
        Projection projection = SecondaryMapEncoder.position(location, secondaryMinimapLayer);
        PrimaryMapEncoder.encodePrimaryLayer(minimap.screenPosition() == MinimapScreenPosition.RIGHT, minimap.size(), minimap.shape(), minimap.dangerRingEnabled(), minimap.fogStylingEnabled(), projection.fractionX, projection.fractionZ, byArray);
        PrimaryMapEncoder.encodeSettings(minimap, byArray);
        boolean bl = SecondaryMapEncoder.visible(minimap, secondaryMinimapLayer, location);
        boolean bl2 = !Config.instance().resourcePack.fallbackMode();
        boolean bl3 = projection.x >= 0.0 && projection.x < 128.0 && projection.z >= 0.0 && projection.z < 128.0;
        byArray[128] = 4;
        byArray[256] = (byte)(bl && bl3 ? 4 : 0);
        if (bl && bl3) {
            MapEncoderUtils.encodeFixedPoint(byArray, 1, 1, secondaryMinimapLayer.getDepth());
            MapEncoderUtils.encodeFixedPoint(byArray, 9, 1, projection.x / 128.0);
            MapEncoderUtils.encodeFixedPoint(byArray, 1, 2, projection.z / 128.0);
            byArray[265] = (byte)(secondaryMinimapLayer.isKeepOnEdge() ? 4 : 0);
        }
        if (!bl2) {
            return;
        }
        if (secondaryMinimapLayer.getRenderer() instanceof CoordinatesOverlayRenderer) {
            MapEncoderUtils.encodeUnsigned(byArray, 1, 1, 255);
        }
        if (!((secondaryMinimapLayerRenderer = secondaryMinimapLayer.getRenderer()) instanceof MinimapIconRenderer)) {
            return;
        }
        MinimapIconRenderer minimapIconRenderer = (MinimapIconRenderer)secondaryMinimapLayerRenderer;
        MapEncoderUtils.encodeUnsigned(byArray, 105, 0, minimapIconRenderer.rotateWithHolderYaw() ? 13 : (secondaryMinimapLayer.isTrackLocation() ? 11 : 12));
        SecondaryMapEncoder.number(byArray, 1, 3, secondaryMinimapLayer.getPositionX(), 4);
        SecondaryMapEncoder.number(byArray, 33, 3, secondaryMinimapLayer.getPositionZ(), 4);
        MapEncoderUtils.encodeUnsigned(byArray, 65, 3, minimapIconRenderer.icon().width());
        MapEncoderUtils.encodeUnsigned(byArray, 73, 3, minimapIconRenderer.icon().height());
        double d = ((double)location.getYaw() % 360.0 + 360.0) % 360.0;
        MapEncoderUtils.encodeUnsigned(byArray, 81, 3, (int)Math.round(d * 255.0 / 360.0));
        byArray[256] = (byte)(bl ? 4 : 0);
        byArray[265] = (byte)(secondaryMinimapLayer.isKeepOnEdge() ? 4 : 0);
        MapEncoderUtils.encodeFixedPoint(byArray, 1, 1, secondaryMinimapLayer.getDepth());
        if (secondaryMinimapLayer instanceof RadarLayer) {
            RadarLayer radarLayer = (RadarLayer)secondaryMinimapLayer;
            MapEncoderUtils.encodeUnsigned(byArray, 105, 0, 14);
            int[] nArray = new int[]{(int)Math.floor((radarLayer.exactX() - Math.floor(radarLayer.exactX())) * 65536.0), (int)Math.floor((radarLayer.exactZ() - Math.floor(radarLayer.exactZ())) * 65536.0), (int)Math.round(radarLayer.deltaX() * 256.0), (int)Math.round(radarLayer.deltaZ() * 256.0), (int)Math.floorMod(radarLayer.sampleTick(), 24000L)};
            for (int i = 0; i < nArray.length; ++i) {
                SecondaryMapEncoder.number(byArray, 1 + i * 16, 4, nArray[i], 2);
            }
            MapEncoderUtils.encodeUnsigned(byArray, 81, 4, radarLayer.intervalTicks());
        }
    }

    private record Projection(double x, double z, double fractionX, double fractionZ) {
    }
}

