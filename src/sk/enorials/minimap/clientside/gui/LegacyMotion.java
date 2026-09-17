/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.clientside.gui;

final class LegacyMotion {
    private LegacyMotion() {
    }

    static void write(byte[] byArray, double d, double d2, double d3, double d4, long l) {
        int n;
        int n2;
        double d5 = d - d3;
        double d6 = d2 - d4;
        if (Math.abs(d5) > 8.0 || Math.abs(d6) > 8.0) {
            d5 = 0.0;
            d6 = 0.0;
        }
        int[] nArray = new int[]{(int)Math.round(d * 4096.0), (int)Math.round(d2 * 4096.0), (int)Math.round(d5 * 4096.0), (int)Math.round(d6 * 4096.0)};
        for (n2 = 0; n2 < 4; ++n2) {
            for (n = 0; n < 32; ++n) {
                byArray[512 + n2 * 32 + n] = (nArray[n2] & 1 << n) == 0 ? 0 : 4;
            }
        }
        n2 = d5 == 0.0 && d6 == 0.0 ? 0 : (int)Math.floorMod(l + 1L, 24000L);
        for (n = 0; n < 16; ++n) {
            byArray[640 + n] = (n2 & 1 << n) == 0 ? 0 : 4;
        }
        for (n = 0; n < 8; ++n) {
            byArray[656 + n] = (0x4D & 1 << n) == 0 ? 0 : 4;
        }
    }
}

