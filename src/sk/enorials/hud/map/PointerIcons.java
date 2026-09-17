/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.map;

import java.awt.image.BufferedImage;
import sk.enorials.minimap.map.icon.MinimapIcon;

public final class PointerIcons {
    private PointerIcons() {
    }

    public static MinimapIcon create(String string) {
        boolean bl = string.equals("player");
        if (!bl && !string.equals("offscreen_player")) {
            return null;
        }
        int n = bl ? 5 : 6;
        int n2 = bl ? 7 : 6;
        BufferedImage bufferedImage = new BufferedImage(n, n2, 2);
        for (int i = 0; i < n2; ++i) {
            for (int j = 0; j < n; ++j) {
                boolean bl2;
                boolean bl3 = bl ? Math.abs(j - 2) <= (i == 6 ? 1 : Math.min(2, i)) : (bl2 = j != 0 && j != 5 || i != 0 && i != 5);
                if (!bl2) continue;
                boolean bl4 = bl ? i > 0 && i < 6 && Math.abs(j - 2) < Math.min(2, i) : j > 0 && j < 5 && i > 0 && i < 5;
                int n3 = 0;
                if (bl4) {
                    int n4 = bl ? Math.abs(j - 2) : Math.max(0, Math.max(2 - j, j - 3));
                    int n5 = bl ? Math.max(0, Math.max(3 - i, i - 4)) : Math.max(0, Math.max(2 - i, i - 3));
                    n3 = switch (n4 + n5) {
                        case 0 -> 255;
                        case 1 -> 224;
                        default -> 188;
                    };
                }
                bufferedImage.setRGB(j, i, 0xFF000000 | n3 * 65793);
            }
        }
        return MinimapIcon.fromBufferedImage(string, bufferedImage);
    }
}

