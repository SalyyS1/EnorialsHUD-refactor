/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextColor
 */
package sk.enorials.hud.compat;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.party.GpsArrowPose;
import sk.enorials.minimap.clientside.gui.GuiMinimapWire;

public final class LegacyGpsPose {
    private LegacyGpsPose() {
    }

    public static int color(long l) {
        return 0xC00000 | (int)(l & 0x3FFFFFL);
    }

    public static int cursor(long l) {
        return ((int)(l >>> 22 & 0xFFL) - 128) * 16384;
    }

    public static Component arrow(String string, long l) {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder stringBuilder2 = new StringBuilder();
        int n = LegacyGpsPose.cursor(l);
        GuiMinimapWire.advance(stringBuilder, n);
        GuiMinimapWire.advance(stringBuilder2, -n);
        Key key = Key.key((String)"enorials_gui:marker_spaces");
        return ((TextComponent)((TextComponent)LegacyText.font((Component)Component.text((String)stringBuilder.toString()), key)).append(((TextComponent)LegacyText.font((Component)Component.text((String)String.valueOf(GpsArrowPose.glyph(l))), Key.key((String)string))).color(TextColor.color((int)LegacyGpsPose.color(l))))).append(LegacyText.font((Component)Component.text((String)stringBuilder2.toString()), key));
    }
}

