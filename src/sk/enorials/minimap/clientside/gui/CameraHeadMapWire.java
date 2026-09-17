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

final class CameraHeadMapWire {
    CameraHeadMapWire() {
    }

    static Pose pose(double d, double d2, double d3, double d4, int n, int n2, int n3, int n4) {
        double d5 = d - d3;
        double d6 = d2 - d4;
        double d7 = Math.hypot(d5, d6);
        if (d7 > 120.0) {
            d = d3 + d5 / d7 * 120.0;
            d2 = d4 + d6 / d7 * 120.0;
        }
        return new Pose((int)(Math.round(d * 32.0) & 0x1FFFL), (int)(Math.round(d2 * 32.0) & 0x1FFFL), Math.max(0, Math.min(7, Math.round((float)n / 9.0f))), n2 & 0x1F, n3 & 1, n4);
    }

    static GuiMinimapWire.DataGlyph cell(int n, int n2, int n3, Pose pose) {
        long l = (long)n & 0xFFFFFFL | (long)pose.x << 24 | (long)pose.z << 37 | (long)n2 << 50 | (long)n3 << 55 | (long)pose.zoomFrom << 60 | (long)(pose.zoomTick & 1) << 63;
        int n4 = pose.zoomTick >>> 1 | pose.zoomTarget << 4;
        return new GuiMinimapWire.DataGlyph((int)(l & Integer.MAX_VALUE) << 1 | 1, (int)(l >>> 31 & 0xFFFFFFL) - 0x800000, (int)(l >>> 55) | n4 << 9);
    }

    static Component component(SkinHead skinHead, Key key, Pose pose) {
        int n = 0;
        int n2 = 0;
        boolean bl = true;
        TextComponent.Builder builder = (TextComponent.Builder)Component.text().style(GuiMinimapWire.style(key, 0));
        StringBuilder stringBuilder = new StringBuilder();
        for (SkinHead.Pixel pixel : skinHead.pixels()) {
            GuiMinimapWire.DataGlyph dataGlyph = CameraHeadMapWire.cell(pixel.rgb(), pixel.x(), pixel.y(), pose);
            if (!bl && n2 != dataGlyph.shadowArgb()) {
                builder.append(Component.text((String)stringBuilder.toString()).style(TextCompat.shadow(GuiMinimapWire.style(key, 0), n2)));
                stringBuilder.setLength(0);
            }
            bl = false;
            n2 = dataGlyph.shadowArgb();
            GuiMinimapWire.advance(stringBuilder, dataGlyph.cursor() - n);
            stringBuilder.append((char)(16384 + dataGlyph.alias()));
            n = dataGlyph.cursor() + 5;
        }
        GuiMinimapWire.advance(stringBuilder, -n);
        if (!stringBuilder.isEmpty()) {
            builder.append(Component.text((String)stringBuilder.toString()).style(TextCompat.shadow(GuiMinimapWire.style(key, 0), n2)));
        }
        return builder.asComponent();
    }

    record Pose(int x, int z, int zoomFrom, int zoomTick, int zoomTarget, int zoomLevel) {
    }
}

