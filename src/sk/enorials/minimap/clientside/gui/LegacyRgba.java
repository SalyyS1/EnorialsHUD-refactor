/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.clientside.gui;

public final class LegacyRgba {
    public static final int HEADER_ROWS = 8;
    public static final int MAX_PIXELS = 1920;

    private LegacyRgba() {
    }

    public static byte[] encode(int[] nArray) {
        if (nArray.length > 1920) {
            throw new IllegalArgumentException("RGBA tile exceeds map capacity");
        }
        byte[] byArray = new byte[(1024 + nArray.length * 8 + 127) / 128 * 128];
        for (int i = 0; i < nArray.length; ++i) {
            for (int j = 0; j < 8; ++j) {
                byArray[1024 + i * 8 + j] = (byte)(4 + (nArray[i] >>> (7 - j) * 4 & 0xF));
            }
        }
        return byArray;
    }

    public static int nibble(int n) {
        int n2;
        int n3;
        int n4;
        int n5;
        int n6;
        int n7 = n >>> 16 & 0xFF;
        int n8 = n >>> 8 & 0xFF;
        int n9 = n & 0xFF;
        if (n7 == n8 && n8 == n9) {
            n6 = 2;
            n5 = n7;
            n4 = 122;
            n3 = 155;
            n2 = 185;
        } else if (n8 == 0 && n9 == 0) {
            n6 = 3;
            n5 = n7;
            n4 = 157;
            n3 = 200;
            n2 = 237;
        } else if (n7 < n8) {
            n6 = 0;
            n5 = n8;
            n4 = 109;
            n3 = 139;
            n2 = 165;
        } else {
            n6 = 1;
            n5 = n7;
            n4 = 152;
            n3 = 193;
            n2 = 230;
        }
        return n6 * 4 + (n5 <= n4 ? 3 : (n5 <= n3 ? 0 : (n5 <= n2 ? 1 : 2)));
    }
}

