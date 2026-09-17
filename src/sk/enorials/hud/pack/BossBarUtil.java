/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.pack;

public final class BossBarUtil {
    private BossBarUtil() {
    }

    public static int getOffset(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("position must be >= 1");
        }
        return (n - 1) * 19 + 10;
    }
}

