/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package sk.enorials.hud.pack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class GeneratedPackCompiler {
    private final Set<String> vanilla;
    private final JsonObject vanillaItems;

    public GeneratedPackCompiler(InputStream inputStream, InputStream inputStream2) throws IOException {
        if (inputStream == null || inputStream2 == null) {
            throw new IOException("Missing vanilla resource compatibility registry");
        }
        try (InputStream inputStream3 = inputStream;
             InputStream inputStream4 = inputStream2;){
            this.vanilla = new HashSet<String>(Arrays.asList(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).split("\\R")));
            this.vanillaItems = JsonParser.parseReader((Reader)new InputStreamReader(inputStream2, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    public static Map<String, byte[]> readZip(byte[] byArray, long l, int n) throws IOException {
        TreeMap<String, byte[]> treeMap = new TreeMap<String, byte[]>();
        HashSet<String> hashSet = new HashSet<String>();
        long l2 = 0L;
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(byArray));){
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                int n2;
                String string = zipEntry.getName();
                if (zipEntry.isDirectory()) continue;
                GeneratedPackCompiler.validPath(string);
                if (!hashSet.add(string.toLowerCase(Locale.ROOT))) {
                    throw new IOException("Duplicate generated path: " + string);
                }
                if (treeMap.size() >= n) {
                    throw new IOException("Generated pack exceeds max-files");
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] byArray2 = new byte[16384];
                while ((n2 = zipInputStream.read(byArray2)) != -1) {
                    if ((l2 += (long)n2) > l) {
                        throw new IOException("Generated pack exceeds byte limit");
                    }
                    byteArrayOutputStream.write(byArray2, 0, n2);
                }
                byte[] byArray3 = byteArrayOutputStream.toByteArray();
                if (zipEntry.getCrc() != -1L) {
                    CRC32 cRC32 = new CRC32();
                    cRC32.update(byArray3);
                    if (cRC32.getValue() != zipEntry.getCrc()) {
                        throw new IOException("Generated ZIP CRC mismatch: " + string);
                    }
                }
                treeMap.put(string, byArray3);
            }
        }
        if (!treeMap.containsKey("pack.mcmeta")) {
            throw new IOException("Generated pack has no root pack.mcmeta");
        }
        return treeMap;
    }

    public Output compile(Map<String, byte[]> map, Set<String> set) throws IOException {
        Object object8;
        Object object22;
        Object object3;
        Iterator iterator;
        Object object42;
        Object object5;
        JsonObject jsonObject;
        Object object62;
        JsonObject jsonObject2 = GeneratedPackCompiler.object(map.get("pack.mcmeta"), "pack.mcmeta");
        if (jsonObject2.has("filter") || jsonObject2.has("overlays")) {
            throw new IOException("Generated filters/overlays require explicit conversion");
        }
        TreeMap<Object, byte[]> treeMap = new TreeMap<Object, byte[]>();
        for (Map.Entry<String, byte[]> arrayDeque2 : map.entrySet()) {
            object62 = arrayDeque2.getKey();
            GeneratedPackCompiler.validPath((String)object62);
            if (!((String)object62).startsWith("assets/")) continue;
            if (((String)object62).startsWith("assets/enorialshud/") || ((String)object62).startsWith("assets/enorials_gui/")) {
                throw new IOException("Generator contains reserved HUD resources: " + (String)object62);
            }
            if (((String)object62).contains("/shaders/")) {
                if (((String)object62).matches("assets/minecraft/shaders/core/rendertype_entity_translucent\\.(vsh|fsh|json)")) continue;
                throw new IOException("Generated shader needs a reviewed 26.2 port: " + (String)object62);
            }
            treeMap.put(object62, arrayDeque2.getValue());
        }
        int n = 0;
        for (Object object62 : new ArrayList(treeMap.keySet())) {
            if (!((String)object62).contains("/models/") || !((String)object62).endsWith(".json")) continue;
            jsonObject = GeneratedPackCompiler.object((byte[])treeMap.get(object62), (String)object62);
            if (jsonObject.has("overrides")) {
                if (!((String)object62).contains("/models/item/")) {
                    throw new IOException("Unsupported legacy override location: " + (String)object62);
                }
                object5 = jsonObject.getAsJsonArray("overrides");
                jsonObject.remove("overrides");
                if (!object5.isEmpty() && !treeMap.containsKey(object42 = ((String)object62).replace("/models/item/", "/items/"))) {
                    double d;
                    Object object7;
                    iterator = this.vanillaItems.has((String)object42) ? this.vanillaItems.getAsJsonObject((String)object42).deepCopy() : new JsonObject();
                    object3 = iterator.has("model") ? iterator.getAsJsonObject("model").deepCopy() : GeneratedPackCompiler.modelNode(GeneratedPackCompiler.id((String)object62, "models", ".json"));
                    object22 = new TreeSet();
                    for (JsonElement jsonElement : object5) {
                        object7 = jsonElement.getAsJsonObject().getAsJsonObject("predicate");
                        if (object7 == null || !object7.keySet().equals(Set.of("custom_model_data"))) {
                            throw new IOException("Unsupported legacy predicate in " + (String)object62 + ": " + String.valueOf(object7));
                        }
                        d = object7.get("custom_model_data").getAsDouble();
                        if (!Double.isFinite(d)) {
                            throw new IOException("Invalid custom model data in " + (String)object62);
                        }
                        ((TreeSet)object22).add(d);
                    }
                    object8 = new JsonArray();
                    int object9 = -1;
                    object7 = ((TreeSet)object22).iterator();
                    while (object7.hasNext()) {
                        d = (Double)object7.next();
                        int n2 = -1;
                        for (int i = 0; i < object5.size(); ++i) {
                            if (!(object5.get(i).getAsJsonObject().getAsJsonObject("predicate").get("custom_model_data").getAsDouble() <= d)) continue;
                            n2 = i;
                        }
                        if (n2 == object9) continue;
                        object9 = n2;
                        String string = object5.get(n2).getAsJsonObject().get("model").getAsString();
                        JsonObject jsonObject3 = GeneratedPackCompiler.modelNode(string);
                        if (((String)object42).equals("assets/minecraft/items/player_head.json") && string.contains("custom/entities/player/")) {
                            jsonObject3 = object3.deepCopy();
                            jsonObject3.addProperty("base", string);
                        }
                        if (object3.has("tints")) {
                            jsonObject3.add("tints", object3.get("tints").deepCopy());
                        }
                        JsonObject jsonObject4 = new JsonObject();
                        jsonObject4.addProperty("threshold", (Number)d);
                        jsonObject4.add("model", (JsonElement)jsonObject3);
                        object8.add((JsonElement)jsonObject4);
                    }
                    object7 = new JsonObject();
                    object7.addProperty("type", "minecraft:range_dispatch");
                    object7.addProperty("property", "minecraft:custom_model_data");
                    object7.addProperty("index", (Number)0);
                    object7.add("entries", (JsonElement)object8);
                    object7.add("fallback", (JsonElement)object3);
                    iterator.add("model", (JsonElement)object7);
                    iterator.addProperty("oversized_in_gui", true);
                    treeMap.put(object42, GeneratedPackCompiler.encode(iterator));
                    ++n;
                }
            }
            if (jsonObject.has("textures") && (object5 = jsonObject.getAsJsonObject("textures")).size() > 0 && !object5.has("particle")) {
                object5.addProperty("particle", "#" + (String)object5.keySet().stream().sorted().findFirst().orElseThrow());
            }
            if (jsonObject.has("parent") && jsonObject.get("parent").getAsString().equals("builtin/entity") && ((String)object62).contains("/custom/entities/player/")) {
                jsonObject.addProperty("parent", "minecraft:item/template_skull");
            }
            treeMap.put(object62, GeneratedPackCompiler.encode((JsonElement)jsonObject));
        }
        ArrayDeque<String> arrayDeque = new ArrayDeque<String>();
        object62 = new TreeSet();
        jsonObject = new TreeSet();
        for (Object object42 : treeMap.keySet()) {
            if (!((String)object42).matches("assets/[^/]+/(items|font|blockstates|equipment|particles|lang)/.+") && !((String)object42).matches("assets/[^/]+/sounds.json") && !((String)object42).startsWith("assets/minecraft/textures/")) continue;
            arrayDeque.add((String)object42);
        }
        while (!arrayDeque.isEmpty()) {
            object5 = (String)arrayDeque.removeFirst();
            if (((TreeSet)object62).contains(object5)) continue;
            if (!treeMap.containsKey(object5)) {
                if (this.vanilla.contains(object5) || set.contains(object5)) continue;
                throw new IOException("Missing generated resource dependency: " + (String)object5);
            }
            ((TreeSet)object62).add(object5);
            if (treeMap.containsKey((String)object5 + ".mcmeta")) {
                arrayDeque.add((String)object5 + ".mcmeta");
            }
            if (!object5.endsWith(".json")) continue;
            object42 = GeneratedPackCompiler.object((byte[])treeMap.get(object5), (String)object5);
            if (object5.contains("/models/")) {
                if (object42.has("parent")) {
                    GeneratedPackCompiler.dependency(arrayDeque, object42.get("parent").getAsString(), "models", ".json");
                }
                if (object42.has("textures")) {
                    iterator = object42.getAsJsonObject("textures").entrySet().stream().map(Map.Entry::getValue).toList().iterator();
                    while (iterator.hasNext()) {
                        object3 = iterator.next();
                        object22 = object3.getAsString();
                        GeneratedPackCompiler.dependency(arrayDeque, (String)object22, "textures", ".png");
                        if (((String)object22).startsWith("#")) continue;
                        jsonObject.add(GeneratedPackCompiler.qualified((String)object22));
                    }
                }
            }
            if (object5.contains("/items/") || object5.contains("/models/") || object5.contains("/blockstates/")) {
                GeneratedPackCompiler.modelRefs((JsonElement)object42, arrayDeque);
            }
            if (object5.contains("/font/")) {
                iterator = object42.getAsJsonArray("providers").iterator();
                while (iterator.hasNext()) {
                    object3 = iterator.next();
                    object22 = object3.getAsJsonObject();
                    switch (object8 = object22.get("type").getAsString().replace("minecraft:", "")) {
                        case "bitmap": {
                            GeneratedPackCompiler.dependency(arrayDeque, object22.get("file").getAsString(), "textures", ".png");
                            break;
                        }
                        case "reference": {
                            GeneratedPackCompiler.dependency(arrayDeque, object22.get("id").getAsString(), "font", ".json");
                            break;
                        }
                        case "ttf": {
                            GeneratedPackCompiler.dependency(arrayDeque, object22.get("file").getAsString(), "font", ".ttf");
                        }
                    }
                }
            }
            if (object5.endsWith("/sounds.json")) {
                iterator = object42.entrySet().stream().map(Map.Entry::getValue).toList().iterator();
                while (iterator.hasNext()) {
                    object3 = (JsonElement)iterator.next();
                    for (Object object8 : object3.getAsJsonObject().getAsJsonArray("sounds")) {
                        if (object8.isJsonObject() && object8.getAsJsonObject().has("type") && object8.getAsJsonObject().get("type").getAsString().equals("event")) continue;
                        Object object9 = object8.isJsonPrimitive() ? object8.getAsString() : object8.getAsJsonObject().get("name").getAsString();
                        GeneratedPackCompiler.dependency(arrayDeque, (String)(((String)object9).contains(":") ? object9 : object5.split("/")[1] + ":" + (String)object9), "sounds", ".ogg");
                    }
                }
            }
            if (object5.contains("/particles/")) {
                iterator = object42.getAsJsonArray("textures").iterator();
                while (iterator.hasNext()) {
                    object3 = (JsonElement)iterator.next();
                    GeneratedPackCompiler.dependency(arrayDeque, object3.getAsString(), "textures/particle", ".png");
                }
            }
            if (!object5.contains("/equipment/")) continue;
            iterator = object42.getAsJsonObject("layers").entrySet().iterator();
            while (iterator.hasNext()) {
                object3 = (Map.Entry)iterator.next();
                for (Object object8 : ((JsonElement)object3.getValue()).getAsJsonArray()) {
                    GeneratedPackCompiler.dependency(arrayDeque, object8.getAsJsonObject().get("texture").getAsString(), "textures/entity/equipment/" + (String)object3.getKey(), ".png");
                }
            }
        }
        object5 = new TreeMap();
        object42 = ((TreeSet)object62).iterator();
        while (object42.hasNext()) {
            object5.put(iterator, ((String)((Object)(iterator = (String)object42.next()))).endsWith(".json") ? GeneratedPackCompiler.encode((JsonElement)GeneratedPackCompiler.object((byte[])treeMap.get(iterator), (String)((Object)iterator))) : (byte[])treeMap.get(iterator));
        }
        object42 = new JsonObject();
        iterator = new JsonArray();
        for (Object object22 : jsonObject) {
            object8 = new JsonObject();
            object8.addProperty("type", "minecraft:single");
            object8.addProperty("resource", (String)object22);
            iterator.add((JsonElement)object8);
        }
        object42.add("sources", (JsonElement)iterator);
        if (!iterator.isEmpty()) {
            object5.put("assets/minecraft/atlases/blocks.json", GeneratedPackCompiler.encode((JsonElement)object42));
        }
        object3 = new JsonObject();
        object3.addProperty("description", "Generated server models and items + Enorials compatibility");
        object3.add("min_format", JsonParser.parseString((String)"[88,0]"));
        object3.add("max_format", JsonParser.parseString((String)"[88,0]"));
        object22 = new JsonObject();
        object22.add("pack", (JsonElement)object3);
        object5.put("pack.mcmeta", GeneratedPackCompiler.encode((JsonElement)object22));
        object8 = new TreeSet<String>(map.keySet());
        ((AbstractSet)object8).removeAll(object5.keySet());
        return new Output(Collections.unmodifiableMap(object5), GeneratedPackCompiler.hash((Map<String, byte[]>)object5), map.size(), n, List.copyOf(object8));
    }

    private static void modelRefs(JsonElement jsonElement, Deque<String> deque) throws IOException {
        block4: {
            block3: {
                if (!jsonElement.isJsonArray()) break block3;
                for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
                    GeneratedPackCompiler.modelRefs(jsonElement2, deque);
                }
                break block4;
            }
            if (!jsonElement.isJsonObject()) break block4;
            for (Map.Entry entry : jsonElement.getAsJsonObject().entrySet()) {
                if ((((String)entry.getKey()).equals("model") || ((String)entry.getKey()).equals("base")) && ((JsonElement)entry.getValue()).isJsonPrimitive()) {
                    GeneratedPackCompiler.dependency(deque, ((JsonElement)entry.getValue()).getAsString(), "models", ".json");
                    continue;
                }
                if (!((JsonElement)entry.getValue()).isJsonObject() && !((JsonElement)entry.getValue()).isJsonArray()) continue;
                GeneratedPackCompiler.modelRefs((JsonElement)entry.getValue(), deque);
            }
        }
    }

    private static void dependency(Deque<String> deque, String string, String string2, String string3) throws IOException {
        if (string.startsWith("#") || string.startsWith("builtin/") || string.startsWith("minecraft:builtin/")) {
            return;
        }
        String string4 = GeneratedPackCompiler.qualified(string);
        String[] stringArray = string4.split(":", 2);
        String string5 = "assets/" + stringArray[0] + "/" + string2 + "/" + stringArray[1] + (stringArray[1].endsWith(string3) ? "" : string3);
        GeneratedPackCompiler.validPath(string5);
        deque.add(string5);
    }

    private static String qualified(String string) {
        return string.contains(":") ? string : "minecraft:" + string;
    }

    private static String id(String string, String string2, String string3) {
        return string.split("/")[1] + ":" + string.substring(string.indexOf("/" + string2 + "/") + string2.length() + 2, string.length() - string3.length());
    }

    private static JsonObject modelNode(String string) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "minecraft:model");
        jsonObject.addProperty("model", string);
        return jsonObject;
    }

    public static void validPath(String string2) throws IOException {
        if (string2.isBlank() || string2.startsWith("/") || string2.contains("\\") || string2.contains(":") || string2.indexOf(0) >= 0 || Arrays.stream(string2.split("/", -1)).anyMatch(string -> string.isEmpty() || string.equals(".") || string.equals("..")) || string2.startsWith("assets/") && !string2.matches("[a-z0-9_./-]+")) {
            throw new IOException("Unsafe generated path: " + string2);
        }
    }

    public static JsonObject object(byte[] byArray, String string) throws IOException {
        try {
            return JsonParser.parseString((String)new String(byArray, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        catch (RuntimeException runtimeException) {
            throw new IOException("Invalid generated JSON: " + string, runtimeException);
        }
    }

    public static byte[] encode(JsonElement jsonElement) {
        return new Gson().toJson(GeneratedPackCompiler.canonical(jsonElement)).getBytes(StandardCharsets.UTF_8);
    }

    private static JsonElement canonical(JsonElement jsonElement2) {
        if (jsonElement2.isJsonObject()) {
            JsonObject jsonObject = new JsonObject();
            new TreeSet(jsonElement2.getAsJsonObject().keySet()).forEach(string -> jsonObject.add(string, GeneratedPackCompiler.canonical(jsonElement2.getAsJsonObject().get(string))));
            return jsonObject;
        }
        if (jsonElement2.isJsonArray()) {
            JsonArray jsonArray = new JsonArray();
            jsonElement2.getAsJsonArray().forEach(jsonElement -> jsonArray.add(GeneratedPackCompiler.canonical(jsonElement)));
            return jsonArray;
        }
        return jsonElement2;
    }

    public static String hash(Map<String, byte[]> map) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            for (Map.Entry<String, byte[]> entry : new TreeMap<String, byte[]>(map).entrySet()) {
                messageDigest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                messageDigest.update((byte)0);
                messageDigest.update(entry.getValue());
                messageDigest.update((byte)0);
            }
            return HexFormat.of().formatHex(messageDigest.digest());
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void writeZip(Path path, Map<String, byte[]> map) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent(), new FileAttribute[0]);
        Path path2 = Files.createTempFile(path.toAbsolutePath().getParent(), "generated-", ".tmp", new FileAttribute[0]);
        try {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(path2, new OpenOption[0]));){
                for (Map.Entry<String, byte[]> entry : new TreeMap<String, byte[]>(map).entrySet()) {
                    ZipEntry zipEntry = new ZipEntry(entry.getKey());
                    zipEntry.setTime(0L);
                    zipOutputStream.putNextEntry(zipEntry);
                    zipOutputStream.write(entry.getValue());
                    zipOutputStream.closeEntry();
                }
            }
            try {
                Files.move(path2, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
                Files.move(path2, path, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally {
            Files.deleteIfExists(path2);
        }
    }

    public record Output(Map<String, byte[]> files, String hash, int sourceFiles, int convertedItems, List<String> removed) {
    }
}

