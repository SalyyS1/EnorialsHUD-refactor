/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.clientside.gui;

final class GuiMotion {
    static final int LIMIT_Q32 = 256;

    private GuiMotion() {
    }

    static boolean continuous(long l, long l2) {
        return l >= -256L && l <= 256L && l2 >= -256L && l2 <= 256L;
    }

    static int encode(int n) {
        double d = Math.abs((double)n) / 32.0;
        int n2 = d < 2.0 ? (int)Math.round(d * 8.0) : (d < 5.0 ? 16 + (int)Math.round((d - 2.0) * 4.0) : 28 + (int)Math.round(d - 5.0));
        n2 = Math.min(31, n2);
        return n < 0 ? -n2 : n2;
    }

    static double decode(int n) {
        int n2 = Math.abs(n);
        double d = n2 < 16 ? (double)n2 / 8.0 : (n2 < 28 ? 2.0 + (double)(n2 - 16) / 4.0 : 5.0 + (double)n2 - 28.0);
        return n < 0 ? -d : d;
    }
}

