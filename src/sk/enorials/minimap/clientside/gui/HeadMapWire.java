/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent$Builder
 */
package sk.enorials.minimap.clientside.gui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import sk.enorials.hud.avatar.SkinHead;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.minimap.clientside.gui.GuiMinimapWire;

public final class HeadMapWire {
    private static final double[] SPEED = new double[]{0.0, 0.03125, 0.0625, 0.125, 0.1875, 0.25, 0.375, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0, 6.0, 8.0};

    private HeadMapWire() {
    }

    public static int velocity(double d) {
        int n = 0;
        for (int i = 1; i < SPEED.length; ++i) {
            if (!(Math.abs(Math.abs(d) - SPEED[i]) < Math.abs(Math.abs(d) - SPEED[n]))) continue;
            n = i;
        }
        return (d < 0.0 ? -n : n) & 0x1F;
    }

    public static double velocity(int n) {
        int n2 = (n + 16) % 32 - 16;
        return Math.copySign(SPEED[Math.min(15, Math.abs(n2))], (double)n2);
    }

    public static GuiMinimapWire.DataGlyph cell(int n, int n2, int n3, int n4, int n5, long l) {
        if (n2 < 0 || n2 > 8191 || n3 < 0 || n3 > 8191) {
            throw new IllegalArgumentException("Head outside map guard band");
        }
        long l2 = (long)n & 0xFFFFFFL | (l & 0x1FL) << 24 | (long)(n4 & 3) << 29 | (long)n2 << 31 | (long)n3 << 44 | (long)((n4 & 0x1F) >>> 2) << 57 | (long)(n5 & 0x1F) << 60;
        return GuiMinimapWire.dataGlyph(l2, n5 >>> 4 & 1);
    }

    public static Pose pose(double d, double d2, double d3, double d4, double d5, double d6, boolean bl, long l) {
        double[] dArray = HeadMapWire.clamp(d * d5, d2 * d5, bl);
        double[] dArray2 = HeadMapWire.clamp(d3 * d6, d4 * d6, bl);
        double d7 = dArray2[0] - dArray[0];
        double d8 = dArray2[1] - dArray[1];
        if (Math.abs(d7) > 8.0 || Math.abs(d8) > 8.0) {
            d7 = 0.0;
            d8 = 0.0;
        }
        long l2 = d7 == 0.0 && d8 == 0.0 ? 0L : l + 1L;
        return new Pose(dArray[0], dArray[1], d7, d8, l2);
    }

    private static double[] clamp(double d, double d2, boolean bl) {
        double d3 = 7.0;
        double d4 = 60.8;
        double d5 = 1.0;
        if (bl) {
            d5 = Math.min(1.0, Math.min((d4 - d3) / Math.max(1.0E-6, Math.abs(d)), (d4 - d3) / Math.max(1.0E-6, Math.abs(d2))));
        } else if (Math.hypot(Math.abs(d) + d3, Math.abs(d2) + d3) > d4) {
            double d6 = d * d + d2 * d2;
            double d7 = (Math.abs(d) + Math.abs(d2)) * d3;
            double d8 = 2.0 * d3 * d3 - d4 * d4;
            d5 = Math.max(0.0, Math.min(1.0, (-d7 + Math.sqrt(Math.max(0.0, d7 * d7 - d6 * d8))) / Math.max(1.0E-6, d6)));
        }
        return new double[]{d * d5, d2 * d5};
    }

    public static Component component(SkinHead skinHead, Key key, Pose pose) {
        return HeadMapWire.component(skinHead, key, pose, false);
    }

    static Component cameraComponent(SkinHead skinHead, Key key, Pose pose) {
        return HeadMapWire.component(skinHead, key, pose, true);
    }

    private static Component component(SkinHead skinHead, Key key, Pose pose, boolean bl) {
        int n = HeadMapWire.velocity(pose.dx);
        int n2 = HeadMapWire.velocity(pose.dz);
        int n3 = 0;
        int n4 = 0;
        TextComponent.Builder builder = (TextComponent.Builder)Component.text().style(GuiMinimapWire.style(key, 0));
        StringBuilder stringBuilder = new StringBuilder();
        boolean bl2 = true;
        for (SkinHead.Pixel pixel : skinHead.pixels()) {
            int n5 = (int)Math.round((pose.x - 6.0 + (double)pixel.x() * 0.375 + 128.0) * 32.0);
            int n6 = (int)Math.round((pose.z - 6.0 + (double)pixel.y() * 0.375 + 128.0) * 32.0);
            if (bl) {
                n5 = n5 - 4096 & 0x1FFF;
                n6 = n6 - 4096 & 0x1FFF;
            }
            GuiMinimapWire.DataGlyph dataGlyph = HeadMapWire.cell(pixel.rgb(), n5, n6, n, n2, pose.tick);
            if (!bl2 && n4 != dataGlyph.shadowArgb()) {
                builder.append(Component.text((String)stringBuilder.toString()).style(TextCompat.shadow(GuiMinimapWire.style(key, 0), n4)));
                stringBuilder.setLength(0);
            }
            bl2 = false;
            n4 = dataGlyph.shadowArgb();
            GuiMinimapWire.advance(stringBuilder, dataGlyph.cursor() - n3);
            stringBuilder.append((char)(59392 + dataGlyph.alias()));
            n3 = dataGlyph.cursor() + 5;
        }
        GuiMinimapWire.advance(stringBuilder, -n3);
        if (!stringBuilder.isEmpty()) {
            builder.append(Component.text((String)stringBuilder.toString()).style(TextCompat.shadow(GuiMinimapWire.style(key, 0), n4)));
        }
        return builder.asComponent();
    }

    public record Pose(double x, double z, double dx, double dz, long tick) {
    }
}

