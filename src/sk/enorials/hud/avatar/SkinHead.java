/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.avatar;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import sk.enorials.minimap.map.icon.MinimapIcon;

public final class SkinHead {
    public static final int SIZE = 32;
    public static final double MAP_SIZE = 12.0;
    public static final double CELL = 0.375;
    private final BufferedImage image;
    private final List<Pixel> pixels;
    private final MinimapIcon fallback;

    private SkinHead(BufferedImage bufferedImage) {
        this.image = bufferedImage;
        ArrayList<Pixel> arrayList = new ArrayList<Pixel>();
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 32; ++j) {
                int n = bufferedImage.getRGB(j, i);
                if (n >>> 24 < 128) continue;
                arrayList.add(new Pixel(j, i, n & 0xFFFFFF));
            }
        }
        this.pixels = List.copyOf(arrayList);
        BufferedImage bufferedImage2 = new BufferedImage(12, 12, 2);
        Graphics2D graphics2D = bufferedImage2.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(bufferedImage, 0, 0, 12, 12, null);
        graphics2D.dispose();
        this.fallback = MinimapIcon.fromBufferedImage("__skin_head", bufferedImage2);
    }

    public List<Pixel> pixels() {
        return this.pixels;
    }

    public int[] rgba() {
        return this.image.getRGB(0, 0, 32, 32, null, 0, 32);
    }

    public BufferedImage image() {
        BufferedImage bufferedImage = new BufferedImage(32, 32, 2);
        bufferedImage.setRGB(0, 0, 32, 32, this.rgba(), 0, 32);
        return bufferedImage;
    }

    public MinimapIcon icon(String string) {
        return new MinimapIcon(string, this.fallback.data(), this.fallback.width(), this.fallback.height());
    }

    public static SkinHead render(BufferedImage bufferedImage) {
        if (bufferedImage.getWidth() != 64 || bufferedImage.getHeight() != 32 && bufferedImage.getHeight() != 64) {
            throw new IllegalArgumentException("Expected vanilla skin");
        }
        BufferedImage bufferedImage2 = new BufferedImage(32, 32, 2);
        double[] dArray = SkinHead.unit(new double[]{0.62, 0.48, 1.0});
        double[] dArray2 = SkinHead.unit(new double[]{dArray[2], 0.0, -dArray[0]});
        double[] dArray3 = new double[]{dArray[1] * dArray2[2], dArray[2] * dArray2[0] - dArray[0] * dArray2[2], -dArray[1] * dArray2[0]};
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 32; ++j) {
                double d = 0.0;
                double d2 = 0.0;
                double d3 = 0.0;
                double d4 = 0.0;
                for (int k = 0; k < 2; ++k) {
                    for (int i2 = 0; i2 < 2; ++i2) {
                        double d5 = ((double)j + ((double)i2 + 0.5) / 2.0 - 16.0) * 13.2 / 32.0;
                        double d6 = (16.0 - (double)i - ((double)k + 0.5) / 2.0) * 13.2 / 32.0;
                        double[] dArray4 = new double[]{dArray2[0] * d5 + dArray3[0] * d6 + dArray[0] * 30.0, dArray2[1] * d5 + dArray3[1] * d6 + dArray[1] * 30.0, dArray2[2] * d5 + dArray3[2] * d6 + dArray[2] * 30.0};
                        int n = SkinHead.sample(bufferedImage, dArray4, dArray, 4.0, false);
                        int n2 = SkinHead.sample(bufferedImage, dArray4, dArray, 4.5, true);
                        double d7 = (double)(n2 >>> 24) / 255.0;
                        double d8 = (double)(n >>> 24) / 255.0;
                        double d9 = d7 + d8 * (1.0 - d7);
                        d += d9;
                        d2 += (double)(n2 >> 16 & 0xFF) * d7 + (double)(n >> 16 & 0xFF) * d8 * (1.0 - d7);
                        d3 += (double)(n2 >> 8 & 0xFF) * d7 + (double)(n >> 8 & 0xFF) * d8 * (1.0 - d7);
                        d4 += (double)(n2 & 0xFF) * d7 + (double)(n & 0xFF) * d8 * (1.0 - d7);
                    }
                }
                if (!(d > 0.0)) continue;
                bufferedImage2.setRGB(j, i, (int)Math.round(d * 255.0 / 4.0) << 24 | (int)Math.round(d2 / d) << 16 | (int)Math.round(d3 / d) << 8 | (int)Math.round(d4 / d));
            }
        }
        return new SkinHead(bufferedImage2);
    }

    private static double[] unit(double[] dArray) {
        double d = Math.sqrt(dArray[0] * dArray[0] + dArray[1] * dArray[1] + dArray[2] * dArray[2]);
        return new double[]{dArray[0] / d, dArray[1] / d, dArray[2] / d};
    }

    private static int sample(BufferedImage bufferedImage, double[] dArray, double[] dArray2, double d, boolean bl) {
        double d2;
        int n;
        int n2;
        double d3;
        double d4;
        double d5 = -1.7976931348623157E308;
        double d6 = Double.MAX_VALUE;
        int n3 = 0;
        for (int i = 0; i < 3; ++i) {
            double d7 = (dArray[i] - d) / dArray2[i];
            double d8 = (dArray[i] + d) / dArray2[i];
            if (d7 > d5) {
                d5 = d7;
                n3 = i;
            }
            d6 = Math.min(d6, d8);
        }
        if (d5 > d6) {
            return 0;
        }
        double d9 = dArray[0] - dArray2[0] * d5;
        double d10 = dArray[1] - dArray2[1] * d5;
        double d11 = dArray[2] - dArray2[2] * d5;
        if (n3 == 2) {
            d4 = d9 / d * 0.5 + 0.5;
            d3 = 0.5 - d10 / d * 0.5;
            n2 = 8;
            n = 8;
            d2 = 0.96;
        } else if (n3 == 0) {
            d4 = 0.5 - d11 / d * 0.5;
            d3 = 0.5 - d10 / d * 0.5;
            n2 = 16;
            n = 8;
            d2 = 0.74;
        } else {
            d4 = d9 / d * 0.5 + 0.5;
            d3 = d11 / d * 0.5 + 0.5;
            n2 = 8;
            n = 0;
            d2 = 1.0;
        }
        int n4 = bufferedImage.getRGB(n2 + (bl ? 32 : 0) + Math.max(0, Math.min(7, (int)(d4 * 8.0))), n + Math.max(0, Math.min(7, (int)(d3 * 8.0))));
        int n5 = bl ? n4 >>> 24 : 255;
        return n5 << 24 | (int)Math.round((double)(n4 >> 16 & 0xFF) * d2) << 16 | (int)Math.round((double)(n4 >> 8 & 0xFF) * d2) << 8 | (int)Math.round((double)(n4 & 0xFF) * d2);
    }

    public record Pixel(int x, int y, int rgb) {
    }
}

