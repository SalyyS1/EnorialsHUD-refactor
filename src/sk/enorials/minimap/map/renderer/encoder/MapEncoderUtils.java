/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map.renderer.encoder;

public class MapEncoderUtils {
    public static void encodeUnsigned(byte[] byArray, int n, int n2, int n3) {
        int n4 = n2 * 128 + n;
        for (int i = 0; i != 8; ++i) {
            byArray[n4 + i] = (byte)((n3 >>> i & 1) * 4);
        }
    }

    public static void encodeFixedPoint(byte[] byArray, int n, int n2, double d) {
        MapEncoderUtils.encodeUnsigned(byArray, n, n2, d < 0.0 ? 255 : (int)Math.min(255.0, Math.abs(d) * 255.0));
    }

    public static void markCorners(byte[] byArray) {
        for (int n : new int[]{0, 127}) {
            for (int n2 : new int[]{0, 127}) {
                byArray[n * 128 + n2] = -98;
            }
        }
    }
}

