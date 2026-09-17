/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 */
package sk.enorials.hud.pack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import sk.enorials.hud.avatar.AvatarFont;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.model.FunctionData;
import sk.enorials.hud.util.HudFontMetrics;
import sk.enorials.hud.util.SpaceUtil;

public class PackRegistry {
    private final Map<Key, AvatarFont.Definition> avatarFonts = new HashMap<Key, AvatarFont.Definition>();
    private HudFontMetrics metrics = HudFontMetrics.empty();
    private final Map<String, GlyphInfo> textureGlyphs = new HashMap<String, GlyphInfo>();
    private final Map<String, TextureSteps> textureSteps = new HashMap<String, TextureSteps>();
    private final Map<String, String> textFonts = new HashMap<String, String>();

    public Map<Key, AvatarFont.Definition> avatarFonts() {
        return this.avatarFonts;
    }

    public HudFontMetrics metrics() {
        return this.metrics;
    }

    public void loadMetrics(Path path) throws IOException {
        this.metrics = HudFontMetrics.load(path);
    }

    private static String key(String string, String string2, String string3, String string4) {
        return string + ":" + string2 + ":" + string3 + ":" + string4;
    }

    private static String key(String string, String string2, String string3, String string4, String string5) {
        return string5 != null && !string5.isEmpty() ? string + ":" + string2 + ":" + string3 + ":" + string4 + "|" + string5 : PackRegistry.key(string, string2, string3, string4);
    }

    public void registerTexture(String string, String string2, String string3, String string4, char c, String string5, int n) {
        this.textureGlyphs.put(PackRegistry.key(string, string2, string3, string4), new GlyphInfo(c, string5, n));
    }

    public void registerTexture(String string, String string2, String string3, String string4, String string5, char c, String string6, int n) {
        this.textureGlyphs.put(PackRegistry.key(string, string2, string3, string4, string5), new GlyphInfo(c, string6, n));
    }

    public void registerTextureSteps(String string, String string2, String string3, String string4, List<GlyphInfo> list, FunctionData functionData) {
        this.textureSteps.put(PackRegistry.key(string, string2, string3, string4), new TextureSteps(list, functionData));
    }

    public void registerTextureSteps(String string, String string2, String string3, String string4, String string5, List<GlyphInfo> list, FunctionData functionData) {
        this.textureSteps.put(PackRegistry.key(string, string2, string3, string4, string5), new TextureSteps(list, functionData));
    }

    public void registerText(String string, String string2, String string3, String string4, String string5) {
        this.textFonts.put(PackRegistry.key(string, string2, string3, string4), string5);
    }

    public GlyphInfo getTexture(String string, String string2, String string3, String string4) {
        return this.textureGlyphs.get(PackRegistry.key(string, string2, string3, string4));
    }

    public GlyphInfo getTexture(String string, String string2, String string3, String string4, String string5) {
        return this.textureGlyphs.get(PackRegistry.key(string, string2, string3, string4, string5));
    }

    public TextureSteps getTextureSteps(String string, String string2, String string3, String string4) {
        return this.textureSteps.get(PackRegistry.key(string, string2, string3, string4));
    }

    public TextureSteps getTextureSteps(String string, String string2, String string3, String string4, String string5) {
        return this.textureSteps.get(PackRegistry.key(string, string2, string3, string4, string5));
    }

    public String getTextFont(String string, String string2, String string3, String string4) {
        return this.textFonts.get(PackRegistry.key(string, string2, string3, string4));
    }

    public void registerTextureInfo(String string, String string2, String string3, String string4, String string5, GlyphInfo glyphInfo) {
        this.textureGlyphs.put(PackRegistry.key(string, string2, string3, string4, string5), glyphInfo);
    }

    public static class GlyphInfo {
        public final char glyph;
        public final String fontKey;
        public final int width;
        private List<GlyphInfo> tiles;
        private int advance;

        public GlyphInfo(char c, String string, int n) {
            this.glyph = c;
            this.fontKey = string;
            this.width = n;
        }

        public GlyphInfo(List<GlyphInfo> list, int n, int n2) {
            this(list.get((int)0).glyph, list.get((int)0).fontKey, n);
            this.tiles = List.copyOf(list);
            this.advance = n2;
        }

        public Component component(PackRegistry packRegistry) {
            if (this.tiles == null) {
                return LegacyText.font((Component)Component.text((char)this.glyph), Key.key((String)this.fontKey));
            }
            TextComponent textComponent = Component.empty();
            for (GlyphInfo glyphInfo : this.tiles) {
                textComponent = textComponent.append(packRegistry.metrics().neutral(glyphInfo.component(packRegistry)));
            }
            return textComponent.append(SpaceUtil.space(this.advance));
        }
    }

    public record TextureSteps(List<GlyphInfo> steps, FunctionData function) {
    }
}

