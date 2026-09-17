/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.party;

import sk.enorials.hud.compat.Numbers;

public final class GpsArrowPose {
    public static final int FIRST_GLYPH = 16384;
    private static final long LOW_BITS = 0x3FFFFFFFL;
    private static final double TAU = Math.PI * 2;

    private GpsArrowPose() {
    }

    public static Angles direction(double d, double d2, double d3, float f, float f2, Angles angles) {
        if (!Double.isFinite(d + d2 + d3 + (double)f + (double)f2) || d * d + d2 * d2 + d3 * d3 < 1.0E-8) {
            return new Angles(0, 128, false);
        }
        double d4 = Math.toRadians(f);
        double d5 = Math.toRadians(f2);
        double d6 = -d * Math.cos(d4) - d3 * Math.sin(d4);
        double d7 = -d * Math.sin(d4) + d3 * Math.cos(d4);
        double d8 = d7 * Math.cos(d5) - d2 * Math.sin(d5);
        double d9 = d2 * Math.cos(d5) + d7 * Math.sin(d5);
        double d10 = Math.hypot(d6, d8);
        int n = d10 < 1.0E-7 && angles != null ? angles.yaw() : Math.floorMod((int)Math.round(Math.atan2(d6, d8) / (Math.PI * 2) * 512.0), 512);
        int n2 = (int)Math.round((Math.atan2(d9, d10) / Math.PI + 0.5) * 255.0);
        return new Angles(n, Numbers.clamp(n2, 0, 255), true);
    }

    public static long encode(Sample sample, Angles angles, long l) {
        Angles angles2 = sample != null && sample.angles().valid() && angles.valid() && l - sample.tick() == 1L ? sample.angles() : angles;
        long l2 = angles2.equals(angles) ? 0L : l + 1L & 0x3FL;
        return (long)angles2.yaw() | (long)angles2.elevation() << 9 | (long)angles.yaw() << 17 | (long)angles.elevation() << 26 | l2 << 34 | (angles.valid() ? 0x10000000000L : 0L);
    }

    public static char glyph(long l) {
        return (char)(16384L + (l >>> 30));
    }

    public static int shadow(long l) {
        return 0x80000001 | (int)(l & 0x3FFFFFFFL) << 1;
    }

    public record Angles(int yaw, int elevation, boolean valid) {
    }

    public record Sample(Angles angles, long tick) {
    }
}

