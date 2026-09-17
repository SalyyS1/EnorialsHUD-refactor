/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.compat;

public final class Numbers {
    private Numbers() {
    }

    public static int clamp(int n, int n2, int n3) {
        if (n2 > n3) {
            throw new IllegalArgumentException("min > max");
        }
        return Math.max(n2, Math.min(n, n3));
    }

    public static double clamp(double d, double d2, double d3) {
        if (!(d2 <= d3)) {
            throw new IllegalArgumentException("invalid bounds");
        }
        return Math.max(d2, Math.min(d, d3));
    }
}

