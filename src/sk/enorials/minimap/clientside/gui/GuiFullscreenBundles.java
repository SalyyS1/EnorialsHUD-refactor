/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent$Builder
 *  net.kyori.adventure.text.format.Style
 */
package sk.enorials.minimap.clientside.gui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.minimap.clientside.gui.GuiMinimapWire;

public final class GuiFullscreenBundles {
    public static final int ALIAS_BASE = 983040;
    public static final int DELTA_BASE = 58880;
    public static final int MAX_RUN = 128;

    private GuiFullscreenBundles() {
    }

    public static int aliasCodepoint(int n) {
        if (n < 0 || n > 65535) {
            throw new IllegalArgumentException("fullscreen alias");
        }
        return n < 65534 ? 983040 + n : 0x100000 + n - 65534;
    }

    public static Component encode(byte[] byArray, int n, int n2, int n3, int n4) {
        if (n <= 0 || n > 16000 || n2 <= 0 || n % 128 != 0 || n2 % 128 != 0 || n / 128 > 255 || n2 / 128 > 255 || (long)n * (long)n2 > 4000000L || (long)byArray.length != (long)n * (long)n2 || n3 < 0 || n3 > 2 || n4 < 0 || n4 > 255) {
            throw new IllegalArgumentException("fullscreen canvas");
        }
        int n5 = 16000 / n;
        if ((n2 + n5 - 1) / n5 > 256) {
            throw new IllegalArgumentException("fullscreen chunks");
        }
        TextComponent.Builder builder = Component.text();
        Key key = Key.key((String)("enorials_gui:fullscreen_bundle/v" + n3));
        int n6 = 0;
        int n7 = 0;
        while (n6 < n2) {
            int n8 = Math.min(n5, n2 - n6) * n;
            int n9 = n4 << 24 | n7 << 16 | n2 / 128 << 8 | n / 128;
            Style style = TextCompat.shadow(GuiMinimapWire.style(key, 0), n9);
            builder.append(GuiFullscreenBundles.chunk(byArray, n6 * n, n8, n, n2, style));
            n6 += n5;
            ++n7;
        }
        return builder.asComponent();
    }

    private static Component chunk(byte[] byArray, int n, int n2, int n3, int n4, Style style) {
        int n5;
        TextComponent.Builder builder = Component.text();
        StringBuilder stringBuilder = new StringBuilder(19000);
        int n6 = 0;
        int n7 = -1;
        int n8 = 0;
        int n9 = 0;
        for (int i = 0; i < n2; i += n5) {
            int n10;
            int n11;
            int n12;
            int n13 = n + i;
            n5 = GuiFullscreenBundles.uniformLength(byArray, n13, n3, n4);
            if (n5 >= 4) {
                n12 = 3;
                n11 = n5 - 1;
                n10 = Byte.toUnsignedInt(byArray[n13]);
            } else {
                for (n5 = 1; n5 < 3 && i + n5 < n2 && (n13 + n5) / n3 == n13 / n3 && !GuiFullscreenBundles.border(n13, n3, n4) && !GuiFullscreenBundles.border(n13 + n5, n3, n4) && GuiFullscreenBundles.uniformLength(byArray, n13 + n5, n3, n4) < 4; ++n5) {
                }
                n12 = n5 - 1;
                n11 = Byte.toUnsignedInt(byArray[n13]);
                if (n5 > 1) {
                    n11 |= Byte.toUnsignedInt(byArray[n13 + 1]) << 8;
                }
                n10 = n5 > 2 ? Byte.toUnsignedInt(byArray[n13 + 2]) : 0;
            }
            int n14 = (i | n12 << 14) << 8 | n10;
            if (n7 < 0) {
                GuiMinimapWire.advance(stringBuilder, n14 - n6);
            } else {
                int n15;
                if (n12 != n7) {
                    stringBuilder.append((char)(58624 + n12 - n7 + 3));
                }
                if ((n15 = n8 * 256 + n10 - n9 - 5) >= -4 && n15 <= 1018) {
                    stringBuilder.append((char)(58880 + n15 + 4));
                } else {
                    stringBuilder.append((char)(58640 + n8 - 1));
                    stringBuilder.append((char)(57851 + n10 - n9 + 4));
                }
            }
            stringBuilder.appendCodePoint(GuiFullscreenBundles.aliasCodepoint(n11));
            n6 = n14 + 5;
            n7 = n12;
            n8 = n5;
            n9 = n10;
            if (stringBuilder.length() < 18968) continue;
            builder.append((Component)Component.text((String)stringBuilder.toString(), (Style)style));
            stringBuilder.setLength(0);
        }
        GuiMinimapWire.advance(stringBuilder, -n6);
        if (!stringBuilder.isEmpty()) {
            builder.append((Component)Component.text((String)stringBuilder.toString(), (Style)style));
        }
        return builder.asComponent();
    }

    private static boolean border(int n, int n2, int n3) {
        int n4 = n % n2;
        int n5 = n / n2;
        return n4 < 3 || n4 >= n2 - 3 || n5 < 3 || n5 >= n3 - 3;
    }

    private static int uniformLength(byte[] byArray, int n, int n2, int n3) {
        int n4;
        if (GuiFullscreenBundles.border(n, n2, n3)) {
            return 1;
        }
        int n5 = Math.min(128, n2 - 3 - n % n2);
        for (n4 = 1; n4 < n5 && byArray[n + n4] == byArray[n]; ++n4) {
        }
        return n4;
    }
}

