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
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class LegacyFonts {
    private LegacyFonts() {
    }

    static void flatten(Path path2) throws IOException {
        JsonArray jsonArray;
        Path path3;
        LinkedHashMap<String, JsonObject> linkedHashMap = new LinkedHashMap<String, JsonObject>();
        HashMap<JsonElement, Path> hashMap = new HashMap<JsonElement, Path>();
        Path path4 = path2.resolve("assets");
        try (Object object = Files.list(path4);){
            for (Path object2 : object.filter(path -> Files.isDirectory(path, new LinkOption[0])).toList()) {
                path3 = object2.resolve("font");
                if (!Files.isDirectory(path3, new LinkOption[0])) continue;
                jsonArray = Files.walk(path3, new FileVisitOption[0]);
                try {
                    for (Path path5 : jsonArray.filter(path -> path.toString().endsWith(".json")).toList()) {
                        Object object3 = String.valueOf(object2.getFileName()) + ":" + path3.relativize(path5).toString().replace('\\', '/').replaceFirst("\\.json$", "");
                        linkedHashMap.put((String)object3, JsonParser.parseString((String)Files.readString(path5)).getAsJsonObject());
                        hashMap.put((JsonElement)object3, path5);
                    }
                }
                finally {
                    if (jsonArray == null) continue;
                    jsonArray.close();
                }
            }
        }
        object = new HashMap();
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            path3 = (JsonObject)entry.getValue();
            jsonArray = path3.getAsJsonArray("providers");
            if (jsonArray == null) continue;
            boolean bl = false;
            for (Object object3 : jsonArray) {
                if (!LegacyFonts.reference(object3.getAsJsonObject())) continue;
                bl = true;
            }
            if (!bl) continue;
            JsonObject jsonObject = path3.deepCopy();
            jsonObject.add("providers", (JsonElement)LegacyFonts.expand((String)entry.getKey(), linkedHashMap, (Map<String, JsonArray>)object, new LinkedHashSet<String>()));
            Files.writeString((Path)hashMap.get(entry.getKey()), (CharSequence)jsonObject.toString(), new OpenOption[0]);
        }
    }

    private static boolean reference(JsonObject jsonObject) {
        String string = jsonObject.has("type") ? jsonObject.get("type").getAsString() : "";
        return string.equals("reference") || string.equals("minecraft:reference");
    }

    private static JsonArray expand(String string, Map<String, JsonObject> map, Map<String, JsonArray> map2, Set<String> set) throws IOException {
        if (map2.containsKey(string)) {
            return map2.get(string);
        }
        if (!set.add(string)) {
            throw new IOException("Cyclic legacy font reference: " + String.valueOf(set) + " -> " + string);
        }
        JsonObject jsonObject = map.get(string);
        if (jsonObject == null) {
            throw new IOException("Missing legacy font reference: " + string);
        }
        JsonArray jsonArray = new JsonArray();
        for (JsonElement jsonElement : jsonObject.getAsJsonArray("providers")) {
            JsonObject jsonObject2 = jsonElement.getAsJsonObject();
            if (LegacyFonts.reference(jsonObject2)) {
                Object object = jsonObject2.get("id").getAsString();
                if (!((String)object).contains(":")) {
                    object = "minecraft:" + (String)object;
                }
                for (JsonElement jsonElement2 : LegacyFonts.expand((String)object, map, map2, set)) {
                    jsonArray.add(jsonElement2.deepCopy());
                }
                continue;
            }
            jsonArray.add(jsonElement.deepCopy());
        }
        set.remove(string);
        map2.put(string, jsonArray);
        return jsonArray;
    }
}

