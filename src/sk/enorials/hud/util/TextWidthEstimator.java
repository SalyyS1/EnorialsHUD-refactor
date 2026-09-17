/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.util;

public final class TextWidthEstimator {
    private TextWidthEstimator() {
    }

    public static int estimate(String string) {
        if (string != null && !string.isEmpty()) {
            String string2 = string.replaceAll("\u00a7[0-9A-FK-ORa-fk-or]", "");
            string2 = string2.replaceAll("<#[0-9a-fA-F]{6}>", "");
            string2 = string2.replaceAll("<[^>]+>", "");
            return string2.length() * 6;
        }
        return 0;
    }
}

