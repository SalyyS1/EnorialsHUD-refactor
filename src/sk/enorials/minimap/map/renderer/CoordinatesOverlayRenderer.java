/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.map.MapPalette
 */
package sk.enorials.minimap.map.renderer;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.map.MapPalette;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.fullscreen.FullscreenMinimap;
import sk.enorials.minimap.map.fullscreen.FullscreenSecondaryMinimapLayer;
import sk.enorials.minimap.map.renderer.SecondaryMinimapLayerRenderer;

public final class CoordinatesOverlayRenderer
implements SecondaryMinimapLayerRenderer {
    private static final int GLYPH_WIDTH = 3;
    private static final int GLYPH_HEIGHT = 5;
    private static final int GLYPH_SPACING = 1;
    private static final byte LABEL_COLOR = MapPalette.matchColor((Color)Color.YELLOW);
    private static final byte VALUE_COLOR = MapPalette.matchColor((Color)Color.WHITE);
    private static final byte SHADOW_COLOR = MapPalette.matchColor((Color)new Color(20, 20, 20));
    private static final Map<Character, int[]> GLYPHS = CoordinatesOverlayRenderer.buildGlyphs();
    private final int textY;

    public CoordinatesOverlayRenderer(int n) {
        this.textY = Math.max(0, Math.min(121, n));
    }

    @Override
    public void render(Minimap minimap, SecondaryMinimapLayer secondaryMinimapLayer, byte[] byArray) {
        int n;
        if (!HudAccess.has((CommandSender)minimap.holder(), "enorials.minimap.coordinates") || !minimap.coordinatesEnabled()) {
            return;
        }
        Location location = minimap.holder().getLocation();
        String string = "X:" + location.getBlockX() + " Y:" + location.getBlockY() + " Z:" + location.getBlockZ();
        int n2 = CoordinatesOverlayRenderer.textWidth(string);
        int n3 = n = Math.max(2, Math.min(126 - n2, 64 - n2 / 2));
        for (char c : string.toCharArray()) {
            char c2 = Character.toUpperCase(c);
            int[] nArray = GLYPHS.get(Character.valueOf(c2));
            if (nArray == null) {
                nArray = GLYPHS.get(Character.valueOf(' '));
            }
            byte by = CoordinatesOverlayRenderer.isLabelChar(c2) ? LABEL_COLOR : VALUE_COLOR;
            boolean bl = !Config.instance().resourcePack.fallbackMode();
            int n4 = bl ? 8 : this.textY;
            int n5 = bl ? 4 : 1;
            CoordinatesOverlayRenderer.drawGlyph(byArray, n3 + 1, n4 + n5, nArray, SHADOW_COLOR, n5, bl);
            CoordinatesOverlayRenderer.drawGlyph(byArray, n3, n4, nArray, by, n5, bl);
            n3 += 4;
        }
    }

    @Override
    public void renderFullscreen(FullscreenMinimap fullscreenMinimap, FullscreenSecondaryMinimapLayer fullscreenSecondaryMinimapLayer, int n, int n2, byte[] byArray) {
    }

    private static boolean isLabelChar(char c) {
        return c == 'X' || c == 'Y' || c == 'Z' || c == ':';
    }

    private static int textWidth(String string) {
        return string.isEmpty() ? 0 : string.length() * 4 - 1;
    }

    private static void drawGlyph(byte[] byArray, int n, int n2, int[] nArray, byte by, int n3, boolean bl) {
        for (int i = 0; i < 5; ++i) {
            int n4 = nArray[i];
            for (int j = 0; j < 3; ++j) {
                if ((n4 & 1 << 2 - j) == 0) continue;
                int n5 = n + j;
                for (int k = 0; k < n3; ++k) {
                    int n6 = n2 + i * n3 + k;
                    if (n5 < 0 || n5 >= 128 || n6 < 0 || n6 >= 128) continue;
                    byArray[bl ? n6 * 128 + n5 : n5 * 128 + n6] = by;
                }
            }
        }
    }

    private static Map<Character, int[]> buildGlyphs() {
        HashMap<Character, int[]> hashMap = new HashMap<Character, int[]>();
        hashMap.put(Character.valueOf(' '), CoordinatesOverlayRenderer.rows("000", "000", "000", "000", "000"));
        hashMap.put(Character.valueOf(':'), CoordinatesOverlayRenderer.rows("000", "010", "000", "010", "000"));
        hashMap.put(Character.valueOf('-'), CoordinatesOverlayRenderer.rows("000", "000", "111", "000", "000"));
        hashMap.put(Character.valueOf('0'), CoordinatesOverlayRenderer.rows("111", "101", "101", "101", "111"));
        hashMap.put(Character.valueOf('1'), CoordinatesOverlayRenderer.rows("010", "110", "010", "010", "111"));
        hashMap.put(Character.valueOf('2'), CoordinatesOverlayRenderer.rows("111", "001", "111", "100", "111"));
        hashMap.put(Character.valueOf('3'), CoordinatesOverlayRenderer.rows("111", "001", "111", "001", "111"));
        hashMap.put(Character.valueOf('4'), CoordinatesOverlayRenderer.rows("101", "101", "111", "001", "001"));
        hashMap.put(Character.valueOf('5'), CoordinatesOverlayRenderer.rows("111", "100", "111", "001", "111"));
        hashMap.put(Character.valueOf('6'), CoordinatesOverlayRenderer.rows("111", "100", "111", "101", "111"));
        hashMap.put(Character.valueOf('7'), CoordinatesOverlayRenderer.rows("111", "001", "001", "001", "001"));
        hashMap.put(Character.valueOf('8'), CoordinatesOverlayRenderer.rows("111", "101", "111", "101", "111"));
        hashMap.put(Character.valueOf('9'), CoordinatesOverlayRenderer.rows("111", "101", "111", "001", "111"));
        hashMap.put(Character.valueOf('X'), CoordinatesOverlayRenderer.rows("101", "101", "010", "101", "101"));
        hashMap.put(Character.valueOf('Y'), CoordinatesOverlayRenderer.rows("101", "101", "010", "010", "010"));
        hashMap.put(Character.valueOf('Z'), CoordinatesOverlayRenderer.rows("111", "001", "010", "100", "111"));
        return hashMap;
    }

    private static int[] rows(String ... stringArray) {
        int[] nArray = new int[5];
        for (int i = 0; i < 5; ++i) {
            nArray[i] = Integer.parseInt(stringArray[i], 2);
        }
        return nArray;
    }
}

