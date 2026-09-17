/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map.icon;

import java.awt.image.BufferedImage;
import sk.enorials.hud.map.IconColors;

public record MinimapIcon(String key, byte[] data, int width, int height) {
    public static MinimapIcon fromBufferedImage(String string, BufferedImage bufferedImage) {
        return new MinimapIcon(string, IconColors.pixels(bufferedImage), bufferedImage.getWidth(), bufferedImage.getHeight());
    }
}

