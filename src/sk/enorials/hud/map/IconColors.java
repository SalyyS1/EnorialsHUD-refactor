/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.map.MapPalette
 */
package sk.enorials.hud.map;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.bukkit.map.MapPalette;

public final class IconColors {
    private static final int FIRST = 4;
    private static final int END = 248;
    private static final int[] RED;
    private static final int[] GREEN;
    private static final int[] BLUE;
    private static final byte[] LOOKUP;

    private IconColors() {
    }

    public static byte match(int n) {
        if (n >>> 24 < 128) {
            return 0;
        }
        int n2 = (n >>> 20 & 0xF) * 256 + (n >>> 12 & 0xF) * 16 + (n >>> 4 & 0xF);
        return LOOKUP[n2];
    }

    public static byte nearest(int n) {
        int n2 = n >>> 16 & 0xFF;
        int n3 = n >>> 8 & 0xFF;
        int n4 = n & 0xFF;
        int n5 = Integer.MAX_VALUE;
        int n6 = 4;
        for (int i = 4; i < 248; ++i) {
            int n7 = n2 + RED[i];
            int n8 = n2 - RED[i];
            int n9 = n3 - GREEN[i];
            int n10 = n4 - BLUE[i];
            int n11 = (1024 + n7) * n8 * n8 + 2048 * n9 * n9 + (1534 - n7) * n10 * n10;
            if (n11 >= n5) continue;
            n5 = n11;
            n6 = i;
        }
        return (byte)n6;
    }

    public static byte[] pixels(BufferedImage bufferedImage) {
        int n = bufferedImage.getWidth();
        int n2 = bufferedImage.getHeight();
        byte[] byArray = new byte[Math.multiplyExact(n, n2)];
        int[] nArray = new int[n];
        for (int i = 0; i < n2; ++i) {
            bufferedImage.getRGB(0, i, n, 1, nArray, 0, n);
            for (int j = 0; j < n; ++j) {
                byArray[i * n + j] = IconColors.match(nArray[j]);
            }
        }
        return byArray;
    }

    static {
        int n;
        RED = new int[248];
        GREEN = new int[248];
        BLUE = new int[248];
        LOOKUP = new byte[4096];
        for (n = 4; n < 248; ++n) {
            Color color = MapPalette.getColor((byte)((byte)n));
            IconColors.RED[n] = color.getRed();
            IconColors.GREEN[n] = color.getGreen();
            IconColors.BLUE[n] = color.getBlue();
        }
        for (n = 0; n < LOOKUP.length; ++n) {
            IconColors.LOOKUP[n] = IconColors.nearest(n >>> 8 << 20 | (n >>> 4 & 0xF) << 12 | (n & 0xF) << 4);
        }
    }
}

