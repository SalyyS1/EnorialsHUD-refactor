/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package sk.enorials.hud.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import sk.enorials.hud.compat.SpacingFont;

final class LegacySpacing {
    private LegacySpacing() {
    }

    static void convert(Path path) throws IOException {
        LegacySpacing.apply(path, true);
    }

    static void aliases(Path path) throws IOException {
        LegacySpacing.apply(path, false);
    }

    private static void apply(Path path2, boolean bl) throws IOException {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        try (Stream<Path> stream = Files.walk(path2.resolve("assets"), new FileVisitOption[0]);){
            for (Path path3 : stream.filter(path -> path.toString().replace('\\', '/').contains("/font/") && path.toString().endsWith(".json")).toList()) {
                JsonObject jsonObject = JsonParser.parseString((String)Files.readString(path3)).getAsJsonObject();
                JsonArray jsonArray = jsonObject.getAsJsonArray("providers");
                if (jsonArray == null) continue;
                JsonArray jsonArray2 = new JsonArray();
                boolean bl2 = false;
                boolean bl3 = false;
                float f = 4.0f;
                for (JsonElement jsonElement : jsonArray) {
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    String string = jsonObject2.get("type").getAsString();
                    if (!string.equals("space") && !string.equals("minecraft:space")) {
                        if (string.equals("bitmap") && jsonObject2.has("chars")) {
                            for (JsonElement jsonElement2 : jsonObject2.getAsJsonArray("chars")) {
                                if (!jsonElement2.getAsString().codePoints().anyMatch(n -> n >= 48 && n <= 122)) continue;
                                bl3 = true;
                            }
                        }
                        jsonArray2.add(jsonElement);
                        continue;
                    }
                    JsonObject jsonObject3 = jsonObject2.getAsJsonObject("advances");
                    if (jsonObject3.has(" ")) {
                        f = jsonObject3.get(" ").getAsFloat();
                        bl3 = true;
                    }
                    if (bl) {
                        LegacySpacing.convert(jsonObject3, jsonArray2, path2, hashMap);
                        bl2 = true;
                        continue;
                    }
                    jsonArray2.add(jsonElement);
                }
                if (bl3 && path2.relativize(path3).toString().replace('\\', '/').startsWith("assets/enorials")) {
                    Iterator iterator = new JsonObject();
                    iterator.addProperty("\uf8ff", Float.valueOf(f));
                    if (bl) {
                        LegacySpacing.convert((JsonObject)iterator, jsonArray2, path2, hashMap);
                    } else {
                        JsonElement jsonElement;
                        jsonElement = new JsonObject();
                        jsonElement.addProperty("type", "space");
                        jsonElement.add("advances", (JsonElement)iterator);
                        jsonArray2.add(jsonElement);
                    }
                    bl2 = true;
                }
                if (!bl2) continue;
                jsonObject.add("providers", (JsonElement)jsonArray2);
                Files.writeString(path3, (CharSequence)jsonObject.toString(), new OpenOption[0]);
            }
        }
    }

    private static void convert(JsonObject jsonObject, JsonArray jsonArray, Path path, Map<String, String> map) throws IOException {
        int n2;
        LinkedHashMap<Float, SortedMap> linkedHashMap = new LinkedHashMap<Float, SortedMap>();
        LinkedHashMap<Integer, StringBuilder> linkedHashMap2 = new LinkedHashMap<Integer, StringBuilder>();
        TreeMap<Integer, Integer> treeMap = new TreeMap<Integer, Integer>();
        TreeMap<Integer, Integer> treeMap2 = new TreeMap<Integer, Integer>();
        for (Map.Entry entry : jsonObject.entrySet()) {
            int n3 = ((String)entry.getKey()).codePointAt(0);
            if (n3 == 32) continue;
            if (n3 > 65534) {
                throw new IOException("Legacy spacing must use BMP glyphs");
            }
            float f2 = ((JsonElement)entry.getValue()).getAsFloat();
            float f6 = Math.abs(f2);
            float f3 = 0.2f;
            if ((double)f2 == Math.rint(f2)) {
                if (f2 >= 1.0f && f2 <= 257.0f) {
                    treeMap.put(n3, (int)f2 - 1);
                    continue;
                }
                if (f2 <= 0.0f && f2 >= -254.0f) {
                    treeMap2.put(n3, 2 - (int)f2);
                    continue;
                }
                linkedHashMap2.computeIfAbsent((int)f2, n -> new StringBuilder()).appendCodePoint(n3);
                continue;
            }
            n2 = Math.round(f6 * 20.0f);
            while (n2 > Short.MAX_VALUE) {
                n2 = Math.round(f6 * 4.0f / (f3 *= 2.0f));
            }
            if (Math.abs((float)n2 * f3 / 4.0f - f6) > 4.0E-7f) {
                f3 = f6 * 4.0f;
                n2 = 1;
            }
            float f4 = Math.copySign(f3, f2);
            linkedHashMap.computeIfAbsent(Float.valueOf(f4), f -> new TreeMap()).put(n3, n2);
        }
        LegacySpacing.bitmapAtlas(treeMap, 1, jsonArray, path, map);
        LegacySpacing.bitmapAtlas(treeMap2, -1, jsonArray, path, map);
        for (Map.Entry entry : linkedHashMap2.entrySet()) {
            String string = ((StringBuilder)entry.getValue()).toString();
            int n4 = string.codePointCount(0, string.length());
            int n5 = (Integer)entry.getKey();
            String string2 = "bitmap-" + n4;
            if (!map.containsKey(string2)) {
                BufferedImage bufferedImage = new BufferedImage(n4, 1, 2);
                for (int i = 0; i < n4; ++i) {
                    bufferedImage.setRGB(i, 0, 16843267);
                }
                Path path2 = path.resolve("assets/enorials/textures/legacy-spacing/" + n4 + ".png");
                Files.createDirectories(path2.getParent(), new FileAttribute[0]);
                ImageIO.write((RenderedImage)bufferedImage, "png", path2.toFile());
                map.put(string2, string2);
            }
            int n6 = n2 = n5 >= 1 ? n5 - 1 : n5 - 2;
            if ((int)(0.5 + (double)n2) + 1 != n5) {
                throw new IOException("Unrepresentable bitmap space: " + n5);
            }
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("type", "bitmap");
            jsonObject2.addProperty("file", "enorials:legacy-spacing/" + n4 + ".png");
            jsonObject2.addProperty("height", (Number)n2);
            jsonObject2.addProperty("ascent", (Number)-1000000000);
            JsonArray jsonArray2 = new JsonArray();
            jsonArray2.add(string);
            jsonObject2.add("chars", (JsonElement)jsonArray2);
            jsonArray.add((JsonElement)jsonObject2);
        }
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            String string = ((SortedMap)entry.getValue()).toString();
            String string3 = map.get(string);
            if (string3 == null) {
                try {
                    string3 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(string.getBytes(StandardCharsets.UTF_8)));
                }
                catch (NoSuchAlgorithmException noSuchAlgorithmException) {
                    throw new IllegalStateException(noSuchAlgorithmException);
                }
                Path path3 = path.resolve("assets/enorials/font/legacy-spacing/" + string3 + ".ttf");
                Files.createDirectories(path3.getParent(), new FileAttribute[0]);
                Files.write(path3, SpacingFont.build((SortedMap)entry.getValue()), new OpenOption[0]);
                map.put(string, string3);
            }
            float f5 = ((Float)entry.getKey()).floatValue();
            JsonObject jsonObject3 = new JsonObject();
            jsonObject3.addProperty("type", "ttf");
            jsonObject3.addProperty("file", "enorials:legacy-spacing/" + string3 + ".ttf");
            jsonObject3.addProperty("size", (Number)Float.valueOf(f5));
            jsonObject3.addProperty("oversample", (Number)Float.valueOf(1.0f / f5));
            jsonArray.add((JsonElement)jsonObject3);
        }
    }

    private static void bitmapAtlas(SortedMap<Integer, Integer> sortedMap, int n, JsonArray jsonArray, Path path, Map<String, String> map) throws IOException {
        int n2;
        Object object;
        if (sortedMap.isEmpty()) {
            return;
        }
        int n3 = Math.max(1, Collections.max(sortedMap.values()));
        int n4 = Math.min(16, sortedMap.size());
        int n5 = (sortedMap.size() + n4 - 1) / n4;
        String string = "atlas-" + String.valueOf(sortedMap);
        String string2 = map.get(string);
        if (string2 == null) {
            try {
                string2 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(string.getBytes(StandardCharsets.UTF_8)));
            }
            catch (NoSuchAlgorithmException noSuchAlgorithmException) {
                throw new IllegalStateException(noSuchAlgorithmException);
            }
            object = new BufferedImage(n3 * n4, n5, 2);
            int n6 = 0;
            Object object2 = sortedMap.values().iterator();
            while (object2.hasNext()) {
                int n7 = object2.next();
                for (n2 = 0; n2 < n7; ++n2) {
                    ((BufferedImage)object).setRGB(n6 % n4 * n3 + n2, n6 / n4, 16843267);
                }
                ++n6;
            }
            object2 = path.resolve("assets/enorials/textures/legacy-spacing/" + string2 + ".png");
            Files.createDirectories(object2.getParent(), new FileAttribute[0]);
            ImageIO.write((RenderedImage)object, "png", object2.toFile());
            map.put(string, string2);
        }
        object = new JsonArray();
        ArrayList<Integer> arrayList = new ArrayList<Integer>(sortedMap.keySet());
        for (int i = 0; i < n5; ++i) {
            StringBuilder stringBuilder = new StringBuilder();
            for (n2 = 0; n2 < n4; ++n2) {
                int n8 = i * n4 + n2;
                stringBuilder.appendCodePoint(n8 < arrayList.size() ? arrayList.get(n8) : 0);
            }
            object.add(stringBuilder.toString());
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "bitmap");
        jsonObject.addProperty("file", "enorials:legacy-spacing/" + string2 + ".png");
        jsonObject.addProperty("height", (Number)n);
        jsonObject.addProperty("ascent", (Number)-1000000000);
        jsonObject.add("chars", (JsonElement)object);
        jsonArray.add((JsonElement)jsonObject);
    }
}

