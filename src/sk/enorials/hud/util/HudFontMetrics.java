/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.format.TextDecoration$State
 */
package sk.enorials.hud.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.util.SpaceUtil;

public final class HudFontMetrics {
    private final Map<String, Map<Integer, Glyph>> fonts;

    private HudFontMetrics(Map<String, Map<Integer, Glyph>> map) {
        this.fonts = Map.copyOf(map);
    }

    public static HudFontMetrics empty() {
        return new HudFontMetrics(Map.of());
    }

    public static HudFontMetrics load(Path path2) throws IOException {
        HashMap<String, Map<Integer, Glyph>> hashMap = new HashMap<String, Map<Integer, Glyph>>();
        HashMap<Object, BufferedImage> hashMap2 = new HashMap<Object, BufferedImage>();
        record BitmapKey(Path texture, String chars, float height) {
        }
        HashMap<BitmapKey, HashMap<Integer, Glyph>> hashMap3 = new HashMap<BitmapKey, HashMap<Integer, Glyph>>();
        Path path3 = path2.resolve("assets");
        if (!Files.isDirectory(path3, new LinkOption[0])) {
            return HudFontMetrics.empty();
        }
        try (Stream<Path> stream = Files.walk(path3, new FileVisitOption[0]);){
            for (Path path4 : stream.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject jsonObject;
                int n;
                String string = path3.relativize(path4).toString().replace('\\', '/');
                if (string.startsWith("enorials_gui/font/") || (n = string.indexOf("/font/")) < 0) continue;
                try (Object object = Files.newBufferedReader(path4);){
                    jsonObject = JsonParser.parseReader((Reader)object).getAsJsonObject();
                }
                if (!jsonObject.has("providers")) continue;
                object = new HashMap();
                for (JsonElement jsonElement : jsonObject.getAsJsonArray("providers")) {
                    Object object2;
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    String string2 = jsonObject2.get("type").getAsString();
                    if (string2.equals("space")) {
                        for (Object object2 : jsonObject2.getAsJsonObject("advances").entrySet()) {
                            object.putIfAbsent(((String)object2.getKey()).codePointAt(0), new Glyph(((JsonElement)object2.getValue()).getAsFloat(), 0.0f, 0.0f, false));
                        }
                        continue;
                    }
                    if (!string2.equals("bitmap")) continue;
                    String[] stringArray = jsonObject2.get("file").getAsString().split(":", 2);
                    object2 = path3.resolve(stringArray.length == 2 ? stringArray[0] : "minecraft").resolve("textures").resolve(stringArray[stringArray.length - 1]);
                    JsonArray jsonArray = jsonObject2.getAsJsonArray("chars");
                    float f = jsonObject2.has("height") ? jsonObject2.get("height").getAsFloat() : 8.0f;
                    BitmapKey bitmapKey = new BitmapKey((Path)object2, jsonArray.toString(), f);
                    HashMap<Integer, Glyph> hashMap4 = (HashMap<Integer, Glyph>)hashMap3.get(bitmapKey);
                    if (hashMap4 != null) {
                        hashMap4.forEach(((Map)object)::putIfAbsent);
                        continue;
                    }
                    hashMap4 = new HashMap<Integer, Glyph>();
                    BufferedImage bufferedImage = (BufferedImage)hashMap2.get(object2);
                    if (bufferedImage == null) {
                        bufferedImage = ImageIO.read(object2.toFile());
                        hashMap2.put(object2, bufferedImage);
                    }
                    int n2 = bufferedImage.getHeight() / jsonArray.size();
                    float f2 = f / (float)n2;
                    for (int i = 0; i < jsonArray.size(); ++i) {
                        int[] nArray = jsonArray.get(i).getAsString().codePoints().toArray();
                        int n3 = bufferedImage.getWidth() / nArray.length;
                        for (int j = 0; j < nArray.length; ++j) {
                            if (nArray[j] == 0) continue;
                            int n4 = 0;
                            int n5 = n3;
                            int n6 = 0;
                            for (int k = 0; k < n2; ++k) {
                                for (int i2 = 0; i2 < n3; ++i2) {
                                    int n7 = bufferedImage.getRGB(j * n3 + i2, i * n2 + k) >>> 24;
                                    if (n7 != 0) {
                                        n4 = Math.max(n4, i2 + 1);
                                    }
                                    if (n7 < 26) continue;
                                    n5 = Math.min(n5, i2);
                                    n6 = Math.max(n6, i2 + 1);
                                }
                            }
                            hashMap4.putIfAbsent(nArray[j], new Glyph((int)((float)n4 * f2 + 0.5f) + 1, n6 == 0 ? 0.0f : (float)n5 * f2, (float)n6 * f2, true));
                        }
                    }
                    hashMap3.put(bitmapKey, hashMap4);
                    hashMap4.forEach(((Map)object)::putIfAbsent);
                }
                hashMap.put(string.substring(0, n) + ":" + string.substring(n + 6, string.length() - 5), Map.copyOf(object));
            }
        }
        return new HudFontMetrics(hashMap);
    }

    public Bounds measure(Component component) {
        float[] fArray = new float[]{0.0f, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN};
        this.visit(component, "minecraft:default", false, fArray, -1);
        return new Bounds(fArray[0], Float.isFinite(fArray[1]) ? fArray[1] : 0.0f, Float.isFinite(fArray[2]) ? fArray[2] : 0.0f);
    }

    public float firstCenter(Component component, int n) {
        float[] fArray = new float[]{0.0f, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN};
        this.visit(component, "minecraft:default", false, fArray, n);
        return fArray[3];
    }

    private void visit(Component component, String string, boolean bl, float[] fArray, int n) {
        boolean bl2;
        String string2 = LegacyText.font(component) == null ? string : LegacyText.font(component).asString();
        TextDecoration.State state = component.decoration(TextDecoration.BOLD);
        boolean bl3 = state == TextDecoration.State.NOT_SET ? bl : (bl2 = state == TextDecoration.State.TRUE);
        if (component instanceof TextComponent) {
            TextComponent object = (TextComponent)component;
            for (int n2 : object.content().codePoints().toArray()) {
                float f;
                Glyph glyph;
                if (n2 == 63743 && string2.startsWith("enorials")) {
                    n2 = 32;
                }
                if ((glyph = (Glyph)this.fonts.getOrDefault(string2, Map.of()).get(n2)) == null && string2.equals("enorialshud:spaces")) {
                    int n3 = n2 >= 61440 && n2 <= 62720 ? -(n2 - 61440) : (n2 >= 57344 && n2 <= 58624 ? n2 - 57344 : 0);
                    glyph = new Glyph(n3, 0.0f, 0.0f, false);
                }
                if (glyph == null && string2.equals("enorials_gui:marker_spaces")) {
                    if (n2 >= 58368 && n2 < 58392) {
                        glyph = new Glyph(1 << n2 - 58368, 0.0f, 0.0f, false);
                    } else if (n2 >= 58400 && n2 < 58424) {
                        glyph = new Glyph(-(1 << n2 - 58400), 0.0f, 0.0f, false);
                    }
                }
                if (glyph == null) {
                    glyph = new Glyph(n2 == 32 ? 4.0f : 6.0f, 0.0f, n2 == 32 ? 0.0f : 5.0f, true);
                }
                float f2 = f = bl2 && glyph.boldable ? 1.0f : 0.0f;
                if (n2 == n && Float.isNaN(fArray[3])) {
                    fArray[3] = fArray[0] + (glyph.left + glyph.right + f) / 2.0f;
                }
                if (glyph.right > glyph.left) {
                    fArray[1] = Math.min(fArray[1], fArray[0] + glyph.left);
                    fArray[2] = Math.max(fArray[2], fArray[0] + glyph.right + f);
                }
                fArray[0] = fArray[0] + (glyph.advance + f);
            }
        }
        for (Component component2 : component.children()) {
            this.visit(component2, string2, bl2, fArray, n);
        }
    }

    public Component field(Component component, Component component2) {
        Bounds bounds = this.measure(component);
        Bounds bounds2 = this.measure(component2);
        int n = Math.round(bounds2.center() - bounds.center());
        int n2 = Math.round(bounds2.advance() - bounds.advance()) - n;
        return ((TextComponent)((TextComponent)Component.empty().append(SpaceUtil.space(n))).append(component)).append(SpaceUtil.space(n2));
    }

    public Component fieldAt(Component component, Component component2, int n) {
        float f = this.firstCenter(component, n);
        float f2 = this.firstCenter(component2, n);
        if (!Float.isFinite(f) || !Float.isFinite(f2)) {
            return this.field(component, component2);
        }
        int n2 = Math.round(f2 - f);
        int n3 = Math.round(this.measure(component2).advance() - this.measure(component).advance()) - n2;
        return ((TextComponent)((TextComponent)Component.empty().append(SpaceUtil.space(n2))).append(component)).append(SpaceUtil.space(n3));
    }

    public Component neutral(Component component) {
        return component.append(SpaceUtil.space(-Math.round(this.measure(component).advance())));
    }

    public record Glyph(float advance, float left, float right, boolean boldable) {
    }

    public record Bounds(float advance, float left, float right) {
        public float center() {
            return (this.left + this.right) / 2.0f;
        }
    }
}

