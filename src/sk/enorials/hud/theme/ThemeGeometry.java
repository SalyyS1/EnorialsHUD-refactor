/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.theme;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

public final class ThemeGeometry {
    private ThemeGeometry() {
    }

    public static Shape opening(String string, double d, double d2, double d3, double d4) {
        if (string.equals("wildwood") || string.equals("astral")) {
            double d5 = Math.min(d4, string.equals("astral") ? d4 : Math.min(d3, d4) * 0.65);
            return new RoundRectangle2D.Double(d, d2, d3, d4, d5, d5);
        }
        double d6 = Math.min(d3, d4) * (string.equals("inferno") ? 0.27 : (string.equals("corsair") ? 0.17 : 0.22));
        Path2D.Double double_ = new Path2D.Double();
        double_.moveTo(d + d6, d2);
        double_.lineTo(d + d3 - d6, d2);
        double_.lineTo(d + d3, d2 + d6);
        double_.lineTo(d + d3, d2 + d4 - d6);
        double_.lineTo(d + d3 - d6, d2 + d4);
        double_.lineTo(d + d6, d2 + d4);
        double_.lineTo(d, d2 + d4 - d6);
        double_.lineTo(d, d2 + d6);
        double_.closePath();
        return double_;
    }

    public static Shape portrait(String string, double d, double d2, double d3, double d4) {
        return new RoundRectangle2D.Double(d, d2, d3, d4, 16.0, 16.0);
    }
}

