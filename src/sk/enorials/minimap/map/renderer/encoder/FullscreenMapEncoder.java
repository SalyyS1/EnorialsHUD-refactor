/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map.renderer.encoder;

import sk.enorials.minimap.map.fullscreen.FullscreenMinimap;
import sk.enorials.minimap.map.fullscreen.FullscreenMinimapLayer;
import sk.enorials.minimap.map.renderer.encoder.MapEncoderUtils;

public class FullscreenMapEncoder {
    private static void header(byte[] byArray, int ... nArray) {
        for (int i = 0; i < nArray.length; ++i) {
            MapEncoderUtils.encodeUnsigned(byArray, i * 8, 0, nArray[i]);
        }
    }

    public static void encodePrimaryLayer(FullscreenMinimap fullscreenMinimap, FullscreenMinimapLayer fullscreenMinimapLayer, byte[] byArray) {
        FullscreenMapEncoder.header(byArray, 178, fullscreenMinimapLayer.screenX(), fullscreenMinimapLayer.screenY(), fullscreenMinimap.getWidth(), fullscreenMinimap.getHeight());
        MapEncoderUtils.encodeFixedPoint(byArray, 40, 0, fullscreenMinimap.getTransitionState());
    }

    public static void encodeBackground(FullscreenMinimap fullscreenMinimap, byte[] byArray) {
        FullscreenMapEncoder.header(byArray, 109);
        MapEncoderUtils.encodeFixedPoint(byArray, 8, 0, fullscreenMinimap.getTransitionState());
    }
}

