/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.map;

public final class TileWindow {
    private TileWindow() {
    }

    public static void compose(byte[] byArray, byte[] byArray2, byte[] byArray3, byte[] byArray4, int n, int n2, byte[] byArray5) {
        for (int i = 0; i < 128; ++i) {
            int n3 = (i - n2 & 0x7F) * 128;
            byte[] byArray6 = i < n2 ? byArray4 : byArray2;
            byte[] byArray7 = i < n2 ? byArray3 : byArray;
            System.arraycopy(byArray6, n3 + 128 - n, byArray5, i * 128, n);
            System.arraycopy(byArray7, n3, byArray5, i * 128 + n, 128 - n);
        }
    }
}

