/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.radar;

import java.awt.Color;
import java.awt.image.BufferedImage;
import sk.enorials.minimap.map.icon.MinimapIcon;

public enum RadarCategory {
    AGGRESSIVE("Aggressive mobs", 0xFF3535),
    NEUTRAL("Neutral mobs", 16768565),
    FRIENDLY("Friendly mobs / NPCs", 0x55FF55);

    private final String label;
    private final MinimapIcon icon;

    private RadarCategory(String string2, int n2) {
        this.label = string2;
        BufferedImage bufferedImage = new BufferedImage(3, 3, 2);
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                bufferedImage.setRGB(j, i, new Color(n2).getRGB());
            }
        }
        this.icon = MinimapIcon.fromBufferedImage("__radar_" + this.name().toLowerCase(), bufferedImage);
    }

    public String label() {
        return this.label;
    }

    public MinimapIcon icon() {
        return this.icon;
    }
}

