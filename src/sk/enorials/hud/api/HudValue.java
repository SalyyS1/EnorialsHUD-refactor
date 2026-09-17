/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.api;

public record HudValue(double current, double maximum) {
    public HudValue(double d, double d2) {
        if (!Double.isFinite(d) || !Double.isFinite(d2) || d2 <= 0.0) {
            throw new IllegalArgumentException("Finite values and positive maximum required");
        }
        this.current = d = Math.max(0.0, Math.min(d, d2));
        this.maximum = d2;
    }
}

