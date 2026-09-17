/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.TextComponent$Builder
 *  net.kyori.adventure.text.format.Style
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 */
package sk.enorials.minimap.clientside.gui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.minimap.clientside.gui.GuiMotion;

public final class GuiMinimapWire {
    public static final int TERRAIN_WIDTH = 144;
    public static final int CHUNK_ROWS = 100;
    public static final int ALIAS_BASE = 57344;
    public static final int DELTA_BASE = 57600;
    public static final int POSITIVE_SPACE_BASE = 58368;
    public static final int NEGATIVE_SPACE_BASE = 58400;
    public static final int RADAR_ALIAS_BASE = 59392;
    public static final int RUN_FLAG_SPACE_BASE = 58624;
    public static final int RUN_STEP_SPACE_BASE = 58640;
    public static final int POSE_ALIAS_BASE = 16384;
    public static final int POSE_RENDER_TICK_OFFSET = 1;
    static final int STRING_CHARACTER_LIMIT = 19000;

    private GuiMinimapWire() {
    }

    public static int unsigned(byte[] byArray, int n, int n2) {
        int n3 = 0;
        for (int i = 0; i < 8; ++i) {
            if (byArray[n2 * 128 + n + i] != 4) continue;
            n3 |= 1 << i;
        }
        return n3;
    }

    public static int integer(byte[] byArray, int n, int n2) {
        return GuiMinimapWire.unsigned(byArray, n, n2) | GuiMinimapWire.unsigned(byArray, n + 8, n2) << 8 | GuiMinimapWire.unsigned(byArray, n + 16, n2) << 16 | GuiMinimapWire.unsigned(byArray, n + 24, n2) << 24;
    }

    public static int shortUnsigned(byte[] byArray, int n, int n2) {
        return GuiMinimapWire.unsigned(byArray, n, n2) | GuiMinimapWire.unsigned(byArray, n + 8, n2) << 8;
    }

    public static int variant(byte[] byArray) {
        int n = Math.min(2, GuiMinimapWire.unsigned(byArray, 25, 0));
        int n2 = byArray[17] == 4 ? 1 : 0;
        int n3 = GuiMinimapWire.unsigned(byArray, 33, 0) > 0 ? 1 : 0;
        int n4 = Math.min(2, GuiMinimapWire.unsigned(byArray, 57, 0));
        return ((n * 2 + n2) * 2 + n3) * 3 + n4;
    }

    public static int layout(byte[] byArray) {
        return GuiMinimapWire.variant(byArray) / 3;
    }

    public static int zoomAlias(byte[] byArray) {
        int n = GuiMinimapWire.unsigned(byArray, 41, 2);
        int n2 = GuiMinimapWire.unsigned(byArray, 33, 2) == n ? 0 : GuiMinimapWire.shortUnsigned(byArray, 17, 2) & 0x1F;
        return n2 | ((n & 0x3F) > 31 ? 32 : 0);
    }

    public static int terrainColor(int n, int n2, byte[] byArray) {
        return n & 0xFF | (n2 & 0xFF) << 8 | GuiMinimapWire.unsigned(byArray, 33, 2) << 16;
    }

    public static Key font(String string, int n) {
        return Key.key((String)"enorials_gui", (String)(string + "/v" + (n < 10 ? "0" : "") + n));
    }

    public static Key font(String string, int n, int n2) {
        return GuiMinimapWire.font((String)(n2 == 0 ? string : "t" + Math.max(0, Math.min(5, n2)) + "/" + string), n);
    }

    public static Style style(Key key, int n) {
        return TextCompat.shadow(Style.style().font(key).color(TextColor.color((int)n)), 0).decoration(TextDecoration.BOLD, false).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.UNDERLINED, false).decoration(TextDecoration.STRIKETHROUGH, false).decoration(TextDecoration.OBFUSCATED, false).build();
    }

    public static Component quad(Style style, int n) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((char)(57344 + n));
        GuiMinimapWire.advance(stringBuilder, -5);
        return Component.text((String)stringBuilder.toString(), (Style)style);
    }

    public static DataGlyph dataGlyph(long l, int n) {
        int n2 = (int)(l & Integer.MAX_VALUE) << 1 | 1;
        int n3 = (int)(l >>> 31 & 0xFFFFFFL) - 0x800000;
        int n4 = (int)(l >>> 55) | (n & 1) << 9;
        return new DataGlyph(n2, n3, n4);
    }

    public static Component dataComponent(Key key, DataGlyph dataGlyph, int n) {
        StringBuilder stringBuilder = new StringBuilder(52);
        GuiMinimapWire.advance(stringBuilder, dataGlyph.cursor());
        stringBuilder.append((char)(n + dataGlyph.alias()));
        GuiMinimapWire.advance(stringBuilder, -dataGlyph.cursor() - 5);
        Style style = TextCompat.shadow(GuiMinimapWire.style(key, 0), dataGlyph.shadowArgb());
        return Component.text((String)stringBuilder.toString(), (Style)style);
    }

    public static DataGlyph radar(double d, double d2, double d3, double d4, long l, int n, int n2, int n3, int n4, int n5) {
        long l2 = Math.round(d * 32.0) & 0x1FFFL;
        long l3 = Math.round(d2 * 32.0) & 0x1FFFL;
        long l4 = Math.max(-64L, Math.min(63L, Math.round(d3 * 16.0))) & 0x7FL;
        long l5 = Math.max(-64L, Math.min(63L, Math.round(d4 * 16.0))) & 0x7FL;
        long l6 = l2 | l3 << 13 | l4 << 26 | l5 << 33 | (l & 0x1FL) << 40 | (long)Math.max(1, Math.min(10, n)) << 45 | (long)(n2 & 0xFF) << 49 | (long)(n3 & 0x1F) << 57 | (long)(n4 & 1) << 62 | (long)(n5 & 1) << 63;
        return GuiMinimapWire.dataGlyph(l6, n5 >>> 1 & 1);
    }

    public static DataGlyph icon(double d, double d2, int n, int n2, int n3, boolean bl, boolean bl2) {
        long l = Math.round(d * 16.0) & 0xFFFFL | (Math.round(d2 * 16.0) & 0xFFFFL) << 16 | (long)(n & 0xFF) << 32 | (long)(n2 & 0x1F) << 40 | (long)(n3 & 1) << 45 | (bl ? 0x400000000000L : 0L) | (bl2 ? 0x800000000000L : 0L);
        return GuiMinimapWire.dataGlyph(l, 0);
    }

    public static DataGlyph poseIcon(double d, double d2, int n, int n2, long l, int n3, int n4, int n5, boolean bl, boolean bl2) {
        n = GuiMotion.encode(n);
        n2 = GuiMotion.encode(n2);
        long l2 = n == 0 && n2 == 0 ? 0L : l + 1L;
        long l3 = Math.round((d + 1024.0) * 32.0) & 0xFFFFL | (Math.round((d2 + 1024.0) * 32.0) & 0xFFFFL) << 16 | (long)(n3 & 0xFF) << 32 | (long)(n4 & 0x1F) << 40 | (long)(n5 & 1) << 45 | (bl ? 0x400000000000L : 0L) | (bl2 ? 0x800000000000L : 0L) | (long)(n & 0x3F) << 48 | (long)(n2 & 0x3F) << 54 | (l2 & 0xFL) << 60;
        return GuiMinimapWire.dataGlyph(l3, (int)(l2 >>> 4 & 1L));
    }

    public static DataGlyph hqMarker(double d, double d2, int n, int n2, long l, int n3, int n4, int n5, boolean bl, boolean bl2) {
        return GuiMinimapWire.hqMarker(d, d2, n, n2, l, n3, n4, n5, bl, bl2, 0);
    }

    public static DataGlyph hqMarker(double d, double d2, int n, int n2, long l, int n3, int n4, int n5, boolean bl, boolean bl2, int n6) {
        long l2 = Math.round((d + 128.0) * 32.0);
        long l3 = Math.round((d2 + 128.0) * 32.0);
        if (l2 < 0L || l2 > 8191L || l3 < 0L || l3 > 8191L) {
            throw new IllegalArgumentException("marker outside guard band");
        }
        n = GuiMotion.encode(n);
        n2 = GuiMotion.encode(n2);
        long l4 = n == 0 && n2 == 0 ? 0L : l + 1L;
        int n7 = Math.min(2, n3 >>> 6) << 3 | Math.round((float)(n3 & 0x3F) / 9.0f);
        int n8 = n7 | (n6 & 7) << 5;
        long l5 = l2 | l3 << 13 | (long)n8 << 26 | (long)(n4 & 0x1F) << 34 | (long)(n5 & 1) << 39 | (bl ? 0x10000000000L : 0L) | (bl2 ? 0x20000000000L : 0L) | (long)(n & 0x3F) << 42 | (long)(n2 & 0x3F) << 48 | (l4 & 0x1FL) << 54;
        return GuiMinimapWire.dataGlyph(l5, 0);
    }

    public static DataGlyph cameraMarker(double d, double d2, int n, int n2, int n3, boolean bl, boolean bl2, int n4) {
        long l = Math.round(d * 32.0) & 0x1FFFFFL;
        long l2 = Math.round(d2 * 32.0) & 0x1FFFFFL;
        int n5 = Math.min(2, n >>> 6) << 3 | Math.round((float)(n & 0x3F) / 9.0f);
        long l3 = l | l2 << 21 | (long)(n5 | (n4 & 7) << 5) << 42 | (long)(n2 & 0x1F) << 50 | (long)(n3 & 1) << 55 | (bl ? 0x100000000000000L : 0L) | (bl2 ? 0x200000000000000L : 0L);
        return GuiMinimapWire.dataGlyph(l3, 0);
    }

    public static DataGlyph poseRadar(double d, double d2, double d3, double d4, long l, int n, int n2, int n3, int n4) {
        long l2 = Math.round((d + 128.0) * 32.0) & 0x1FFFL;
        long l3 = Math.round((d2 + 128.0) * 32.0) & 0x1FFFL;
        long l4 = Math.round(d3 * 32.0);
        long l5 = Math.round(d4 * 32.0);
        long l6 = 0L;
        long l7 = 0L;
        if (GuiMotion.continuous(l4, l5)) {
            l6 = GuiMotion.encode((int)l4);
            l7 = GuiMotion.encode((int)l5);
        }
        long l8 = l6 == 0L && l7 == 0L ? 0L : l + 1L;
        long l9 = l2 | l3 << 13 | (l6 & 0x7FL) << 26 | (l7 & 0x7FL) << 33 | (l8 & 0x1FL) << 40 | 0x200000000000L | (long)(n & 0xFF) << 49 | (long)(n2 & 0x1F) << 57 | (long)(n3 & 1) << 62 | (long)(n4 & 1) << 63;
        return GuiMinimapWire.dataGlyph(l9, n4 >>> 1 & 1);
    }

    public static TerrainPose cameraTerrainPose(int n, int n2, byte[] byArray) {
        int n3 = n + 72 & 0x1FFF | (n2 + 72 & 0x1FFF) << 13;
        return new TerrainPose(n3 << 1 | 1, GuiMinimapWire.terrainZoomAlias(byArray));
    }

    private static int terrainZoomAlias(byte[] byArray) {
        int n = GuiMinimapWire.zoomAlias(byArray);
        int n2 = GuiMinimapWire.unsigned(byArray, 33, 2);
        int n3 = (n2 >>> 6) * 32 + ((n2 & 0x3F) * 31 + 31) / 63;
        return n3 | (n & 0x1F) << 7 | n >>> 5 << 12;
    }

    public static TerrainPose terrainPose(long l, long l2, int n, int n2, int n3, int n4, long l3, byte[] byArray) {
        long l4 = Math.round((double)(l - ((long)n + 72L) * 32L) / 4.0);
        long l5 = Math.round((double)(l2 - ((long)n2 + 72L) * 32L) / 4.0);
        if (l4 < -64L || l4 > 63L || l5 < -64L || l5 > 63L) {
            throw new IllegalArgumentException("terrain pose outside guard band");
        }
        n3 = GuiMotion.encode(n3);
        n4 = GuiMotion.encode(n4);
        long l6 = n3 == 0 && n4 == 0 ? 0L : l3 + 1L;
        int n5 = GuiMinimapWire.terrainZoomAlias(byArray);
        int n6 = (int)(l4 & 0x7FL) | (int)(l5 & 0x7FL) << 7 | (n3 & 0x3F) << 14 | (n4 & 0x3F) << 20 | ((int)l6 & 0x1F) << 26;
        return new TerrainPose(n6 << 1 | 1, n5);
    }

    public static Component posedTerrain(Component component, Style style, TerrainPose terrainPose) {
        Component component2 = GuiMinimapWire.replacePoseAlias(component, (char)(16384 + terrainPose.alias()));
        return component2.style(TextCompat.shadow(style.color(TextColor.color((int)0)), terrainPose.shadowArgb()));
    }

    public static Component reposeTerrain(Component component, TerrainPose terrainPose) {
        return component.style(TextCompat.shadow(component.style(), terrainPose.shadowArgb()));
    }

    private static Component replacePoseAlias(Component component2, char c) {
        Component component3 = component2;
        if (component2 instanceof TextComponent) {
            TextComponent textComponent = (TextComponent)component2;
            component3 = textComponent.content(textComponent.content().replace('\u4000', c));
        }
        if (!component2.children().isEmpty()) {
            component3 = component3.children(component2.children().stream().map(component -> GuiMinimapWire.replacePoseAlias(component, c)).toList());
        }
        return component3;
    }

    public static Component terrainRuns(byte[] byArray) {
        int n;
        if (byArray.length != 20736) {
            throw new IllegalArgumentException("pose terrain dimensions");
        }
        TextComponent.Builder builder = Component.text();
        StringBuilder stringBuilder = new StringBuilder(19000);
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        for (int i = 0; i < byArray.length; i += n) {
            n = i % 144 < 143 && byArray[i] == byArray[i + 1] ? 2 : 1;
            int n5 = Byte.toUnsignedInt(byArray[i]);
            int n6 = ((i | n - 1 << 15) << 8 | n5) - 0x800000;
            if (n3 == 0) {
                GuiMinimapWire.advance(stringBuilder, n6 - n2);
            } else {
                if (n != n3) {
                    GuiMinimapWire.advance(stringBuilder, (n - n3) * 0x800000);
                }
                if (n3 == 2) {
                    stringBuilder.append('\ue511');
                }
                stringBuilder.append((char)(57851 + n5 - n4 + 4));
            }
            stringBuilder.append('\u4000');
            n2 = n6 + 5;
            n3 = n;
            n4 = n5;
            if (stringBuilder.length() < 18968) continue;
            builder.append((Component)Component.text((String)stringBuilder.toString()));
            stringBuilder.setLength(0);
        }
        GuiMinimapWire.advance(stringBuilder, -n2);
        if (!stringBuilder.isEmpty()) {
            builder.append((Component)Component.text((String)stringBuilder.toString()));
        }
        return builder.asComponent();
    }

    public static DataGlyph player(double d, double d2, long l, int n) {
        long l2 = Math.round(d * 512.0 / 360.0) & 0x1FFL;
        long l3 = Math.round(d2 * 512.0 / 360.0) & 0x1FFL;
        return GuiMinimapWire.dataGlyph(l2 | l3 << 9 | (l & 0x1FL) << 18 | (long)Math.max(1, Math.min(10, n)) << 23, 0);
    }

    public static Component raster(byte[] byArray, int n, int n2, Style style, int n3, boolean bl) {
        return GuiMinimapWire.raster(byArray, n, n2, style, n3, bl, 0);
    }

    public static Component raster(byte[] byArray, int n, int n2, Style style, int n3, boolean bl, int n4) {
        if (n2 < 0 || n < 0 || n + n2 > byArray.length || n2 > 16384) {
            throw new IllegalArgumentException("raster chunk");
        }
        TextComponent.Builder builder = Component.text();
        StringBuilder stringBuilder = new StringBuilder(Math.min(19000, n2 * 2 + 24));
        int n5 = 0;
        for (int i = 0; i < n2; ++i) {
            int n6 = Byte.toUnsignedInt(byArray[n + i]);
            if (bl && n6 < 4) continue;
            int n7 = n4 + i * 256 + n6;
            int n8 = n7 - n5;
            if (n8 >= -4 && n8 <= 506) {
                stringBuilder.append((char)(57600 + n8 + 4));
            } else {
                GuiMinimapWire.advance(stringBuilder, n8);
            }
            stringBuilder.append((char)(57344 + n3));
            n5 = n7 + 5;
            if (stringBuilder.length() < 18968) continue;
            builder.append((Component)Component.text((String)stringBuilder.toString(), (Style)style));
            stringBuilder.setLength(0);
        }
        GuiMinimapWire.advance(stringBuilder, -n5);
        if (!stringBuilder.isEmpty()) {
            builder.append((Component)Component.text((String)stringBuilder.toString(), (Style)style));
        }
        return builder.asComponent();
    }

    public static void advance(StringBuilder stringBuilder, int n) {
        long l = Math.abs((long)n);
        if (l >= 0x1000000L) {
            throw new IllegalArgumentException("glyph advance outside exact float range");
        }
        int n2 = n < 0 ? 58400 : 58368;
        for (int i = 23; i >= 0; --i) {
            if ((l & 1L << i) == 0L) continue;
            stringBuilder.append((char)(n2 + i));
        }
    }

    public static Component rasterRuns(byte[] byArray, int n, int n2, Style style, int n3, int n4, int n5, boolean bl) {
        int n6;
        if (n2 < 0 || n2 > 16000 || n < 0 || n + n2 > byArray.length || n4 < 1 || n % n4 != 0 || n2 % n4 != 0) {
            throw new IllegalArgumentException("RLE raster chunk");
        }
        TextComponent.Builder builder = Component.text();
        StringBuilder stringBuilder = new StringBuilder(Math.min(19000, n2 * 2 + 24));
        int n7 = 0;
        int n8 = 0;
        int n9 = 0;
        for (int i = 0; i < n2; i += n6) {
            boolean bl2;
            int n10 = Byte.toUnsignedInt(byArray[n + i]);
            int n11 = i % n4;
            int n12 = (n + i) / n4;
            boolean bl3 = bl2 = bl && (n11 < 3 || n11 >= n4 - 3 || n12 < 3 || n12 >= n5 - 3);
            for (n6 = 1; !(bl2 || n6 >= 4 || i + n6 >= n2 || n11 + n6 >= n4 || bl && n11 + n6 >= n4 - 3 || byArray[n + i + n6] != byArray[n + i]); ++n6) {
            }
            int n13 = (i | n6 - 1 << 14) << 8 | n10;
            if (n8 == 0) {
                GuiMinimapWire.advance(stringBuilder, n13 - n7);
            } else {
                int n14 = n6 - n8;
                if (n14 != 0) {
                    stringBuilder.append((char)(58624 + n14 + 3));
                }
                if (n8 > 1) {
                    stringBuilder.append((char)(58640 + n8 - 1));
                }
                stringBuilder.append((char)(57851 + n10 - n9 + 4));
            }
            stringBuilder.append((char)(57344 + n3));
            n7 = n13 + 5;
            n8 = n6;
            n9 = n10;
            if (stringBuilder.length() < 18968) continue;
            builder.append((Component)Component.text((String)stringBuilder.toString(), (Style)style));
            stringBuilder.setLength(0);
        }
        GuiMinimapWire.advance(stringBuilder, -n7);
        if (!stringBuilder.isEmpty()) {
            builder.append((Component)Component.text((String)stringBuilder.toString(), (Style)style));
        }
        return builder.asComponent();
    }

    public static int advanceFor(int n) {
        if (n >= 57600 && n <= 58110) {
            return n - 57600 - 4;
        }
        if (n >= 58368 && n < 58392) {
            return 1 << n - 58368;
        }
        if (n >= 58400 && n < 58424) {
            return -(1 << n - 58400);
        }
        if (n >= 58624 && n < 58631) {
            return (n - 58624 - 3) * 0x400000;
        }
        if (n >= 58640 && n < 58644) {
            return (n - 58640) * 256;
        }
        return 5;
    }

    public record DataGlyph(int shadowArgb, int cursor, int alias) {
    }

    public record TerrainPose(int shadowArgb, int alias) {
        public long key() {
            return Integer.toUnsignedLong(this.shadowArgb) << 14 | (long)this.alias;
        }
    }
}

