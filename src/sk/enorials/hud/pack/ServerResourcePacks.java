/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package sk.enorials.hud.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import sk.enorials.hud.compat.ClientRenderProfile;

public final class ServerResourcePacks {
    public static final String FOLDER = "server-resource-packs";
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final byte[] EMPTY = "{}".getBytes(StandardCharsets.UTF_8);

    private ServerResourcePacks() {
    }

    public static void initialize(Path path) throws IOException {
        Path path2 = path.resolve(FOLDER);
        Files.createDirectories(path2, new FileAttribute[0]);
        Path path3 = path2.resolve("README.txt");
        if (!Files.exists(path3, new LinkOption[0])) {
            Files.writeString(path3, (CharSequence)"SERVER RESOURCE PACKS - ENORIALS HUD\nPut Java Edition resource-pack ZIPs here, or one unpacked folder per pack.\nEach pack needs pack.mcmeta at its root and must support this server's Minecraft version and resource format.\nRun /enohud hud pack to rebuild and send the combined pack, or restart the server.\nInputs stay untouched. Removing an input and rebuilding removes it from the combined pack.\nZIPs/folders load alphabetically; later owner packs have higher priority.\nTextures, models, sounds, languages and font providers are included. Applicable version overlays are flattened.\nShared fonts/sounds/languages are merged; other colliding HUD/Spells assets (including shaders) stop the build.\nResource filters are rejected: they could hide required HUD or Spells resources. Remove filters from a copy first.\nCheck ../pack-merge-report.json for the result and exact conflict path. The last successful pack is retained on failure.\nConfigure server-resource-packs in config.yml (enabled, max-uncompressed-mb, max-files).\nLeave resource-pack in server.properties empty when HUD sends your combined pack, to avoid a second server download.\nHUD's visibility toggle restores your own hearts, food, armor and air textures.\n", StandardCharsets.UTF_8, new OpenOption[0]);
        }
    }

    public static Result merge(Path path, Map<String, byte[]> map, Map<String, byte[]> map2, boolean bl, long l, int n) throws IOException {
        return ServerResourcePacks.merge(path, map, map2, bl, l, n, Map.of());
    }

    public static Result merge(Path path, Map<String, byte[]> map, Map<String, byte[]> map2, boolean bl, long l, int n, Map<String, byte[]> map3) throws IOException {
        return ServerResourcePacks.merge(path, map, map2, bl, l, n, map3, ClientRenderProfile.nativeProfile());
    }

    public static Result merge(Path path2, Map<String, byte[]> map, Map<String, byte[]> map2, boolean bl, long l, int n, Map<String, byte[]> map3, ClientRenderProfile clientRenderProfile) throws IOException {
        ServerResourcePacks.initialize(path2);
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        JsonArray jsonArray2 = new JsonArray();
        jsonObject.add("sources", (JsonElement)jsonArray);
        jsonObject.add("ownerOverrides", (JsonElement)jsonArray2);
        try {
            Object object;
            Object object2;
            Object object422;
            Object object5;
            TreeMap<String, byte[]> treeMap = new TreeMap<String, byte[]>(map);
            TreeMap<String, byte[]> treeMap2 = new TreeMap<String, byte[]>(map2);
            TreeMap<String, byte[]> treeMap3 = new TreeMap<String, byte[]>();
            JsonObject jsonObject2 = new JsonObject();
            if (!map3.isEmpty()) {
                ServerResourcePacks.checkFormat(ServerResourcePacks.object(map3.get("pack.mcmeta"), "generated pack.mcmeta"), "generated server pack", clientRenderProfile);
                treeMap3.putAll(ServerResourcePacks.flatten(map3, ServerResourcePacks.object(map3.get("pack.mcmeta"), "generated pack.mcmeta"), clientRenderProfile));
                object5 = new JsonObject();
                object5.addProperty("name", "generated-resource-packs/current.zip");
                object5.addProperty("files", (Number)treeMap3.size());
                object5.addProperty("sha256", ServerResourcePacks.fingerprint(map3));
                jsonArray.add((JsonElement)object5);
            }
            if (bl) {
                List<Path> object32;
                object5 = path2.resolve(FOLDER);
                if (Files.isSymbolicLink((Path)object5)) {
                    throw new IOException("Input folder must not be a symbolic link");
                }
                object422 = Files.list((Path)object5);
                try {
                    object32 = object422.filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) || path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")).sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
                }
                finally {
                    if (object422 != null) {
                        object422.close();
                    }
                }
                object422 = new Budget(l - map3.values().stream().mapToLong(byArray -> ((byte[])byArray).length).sum(), n - map3.size());
                object2 = object32.iterator();
                while (object2.hasNext()) {
                    String string2;
                    object = (Path)object2.next();
                    if (Files.isSymbolicLink((Path)object)) {
                        throw new IOException("Symbolic link input: " + String.valueOf(object.getFileName()));
                    }
                    Map<String, byte[]> map4 = ServerResourcePacks.read((Path)object, (Budget)object422);
                    JsonObject jsonObject3 = ServerResourcePacks.object(map4.get("pack.mcmeta"), String.valueOf(object.getFileName()) + "/pack.mcmeta");
                    ServerResourcePacks.checkFormat(jsonObject3, object.getFileName().toString(), clientRenderProfile);
                    if (jsonObject3.has("filter")) {
                        throw new IOException(String.valueOf(object.getFileName()) + ": resource filters need manual reconciliation; no files were published");
                    }
                    Map<String, byte[]> map5 = ServerResourcePacks.flatten(map4, jsonObject3, clientRenderProfile);
                    for (Map.Entry<String, byte[]> entry : map5.entrySet()) {
                        string2 = entry.getKey();
                        byte[] byArray2 = treeMap3.get(string2);
                        if (byArray2 != null && !Arrays.equals(byArray2, entry.getValue())) {
                            jsonArray2.add(String.valueOf(object.getFileName()) + ": " + string2);
                        }
                        treeMap3.put(string2, byArray2 != null && ServerResourcePacks.sharedJson(string2) ? ServerResourcePacks.combine(string2, byArray2, entry.getValue(), false) : entry.getValue());
                    }
                    for (Map.Entry<String, Object> entry : jsonObject3.entrySet()) {
                        if (Set.of("pack", "overlays", "filter").contains(entry.getKey())) continue;
                        if (jsonObject2.has(entry.getKey()) && jsonObject2.get(entry.getKey()).isJsonObject() && ((JsonElement)entry.getValue()).isJsonObject()) {
                            string2 = jsonObject2.getAsJsonObject(entry.getKey());
                            ((JsonElement)entry.getValue()).getAsJsonObject().entrySet().forEach(arg_0 -> ServerResourcePacks.lambda$merge$3((JsonObject)string2, arg_0));
                            continue;
                        }
                        jsonObject2.add(entry.getKey(), (JsonElement)entry.getValue());
                    }
                    Iterator<Map.Entry<String, Object>> iterator = new JsonObject();
                    iterator.addProperty("name", object.getFileName().toString());
                    iterator.addProperty("files", map5.size());
                    iterator.addProperty("sha256", ServerResourcePacks.fingerprint(map4));
                    jsonArray.add(iterator);
                }
            }
            for (Map.Entry entry : treeMap3.entrySet()) {
                boolean bl2;
                object422 = (String)entry.getKey();
                object2 = (byte[])entry.getValue();
                object = map.get(object422);
                boolean bl3 = bl2 = map2.containsKey(object422) || ((String)object422).endsWith(".mcmeta") && map2.containsKey(((String)object422).substring(0, ((String)object422).length() - 7));
                if (!(object == null || Arrays.equals((byte[])object, object2) || bl2 || ((String)object422).equals("pack.png"))) {
                    if (ServerResourcePacks.sharedJson((String)object422)) {
                        if (((String)object422).contains("/font/")) {
                            ServerResourcePacks.checkReferences((String)object422, map, treeMap3);
                        }
                        object2 = ServerResourcePacks.combine((String)object422, (byte[])object, object2, true);
                    } else {
                        throw new IOException("Conflicting Enorials resource: " + (String)object422 + ". Rename/remap this asset in your pack; HUD/Spells cannot safely overwrite it.");
                    }
                }
                if (object == null && !bl2 && (((String)object422).startsWith("assets/enorialshud/") || ((String)object422).startsWith("assets/enorials_gui/"))) {
                    throw new IOException("Reserved Enorials namespace: " + (String)object422 + ". Use your server's own namespace.");
                }
                treeMap.put((String)object422, (byte[])object2);
                if (!bl2) continue;
                treeMap2.put((String)object422, (byte[])object2);
            }
            for (String string3 : new ArrayList<String>(treeMap2.keySet())) {
                if (!string3.endsWith(".png")) continue;
                treeMap2.putIfAbsent(string3 + ".mcmeta", EMPTY);
            }
            object5 = ServerResourcePacks.object(map.get("pack.mcmeta"), "Enorials pack.mcmeta");
            for (Object object422 : jsonObject2.entrySet()) {
                object5.add((String)object422.getKey(), (JsonElement)object422.getValue());
            }
            if (!jsonArray.isEmpty()) {
                object5.getAsJsonObject("pack").addProperty("description", "Server resource pack + Enorials HUD" + (map.keySet().stream().anyMatch(string -> string.startsWith("assets/spelleffects/")) ? " + Spells" : ""));
            }
            treeMap.put("pack.mcmeta", ServerResourcePacks.encode((JsonElement)object5));
            String string4 = treeMap3.isEmpty() && jsonObject2.size() == 0 ? "" : ServerResourcePacks.fingerprint(treeMap3) + ServerResourcePacks.fingerprint(Map.of("metadata", ServerResourcePacks.encode((JsonElement)jsonObject2)));
            jsonObject.addProperty("status", "success");
            jsonObject.addProperty("mergedFiles", (Number)treeMap.size());
            jsonObject.addProperty("uncompressedBytes", (Number)treeMap.values().stream().mapToLong(byArray -> ((byte[])byArray).length).sum());
            Files.writeString(path2.resolve((String)(clientRenderProfile.sameResources(ClientRenderProfile.nativeProfile()) ? "pack-merge-report.json" : "pack-merge-report-" + clientRenderProfile.key() + ".json")), (CharSequence)JSON.toJson((JsonElement)jsonObject), StandardCharsets.UTF_8, new OpenOption[0]);
            return new Result(treeMap, treeMap2, (String)(string4.isEmpty() ? "" : "-server-" + ServerResourcePacks.sha(string4.getBytes(StandardCharsets.UTF_8))));
        }
        catch (IOException | RuntimeException exception) {
            jsonObject.addProperty("status", "failed");
            jsonObject.addProperty("error", exception.getMessage());
            Files.writeString(path2.resolve((String)(clientRenderProfile.sameResources(ClientRenderProfile.nativeProfile()) ? "pack-merge-report.json" : "pack-merge-report-" + clientRenderProfile.key() + ".json")), (CharSequence)JSON.toJson((JsonElement)jsonObject), StandardCharsets.UTF_8, new OpenOption[0]);
            if (exception instanceof IOException) {
                IOException iOException = (IOException)exception;
                throw iOException;
            }
            throw new IOException("Invalid server resource pack: " + exception.getMessage(), exception);
        }
    }

    private static Map<String, byte[]> read(Path path, Budget budget) throws IOException {
        TreeMap<String, byte[]> treeMap = new TreeMap<String, byte[]>();
        HashSet<String> hashSet = new HashSet<String>();
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (Stream<Path> stream = Files.walk(path, new FileVisitOption[0]);){
                Iterator iterator = stream.iterator();
                while (iterator.hasNext()) {
                    Path path2 = (Path)iterator.next();
                    if (Files.isSymbolicLink(path2)) {
                        throw new IOException("Symbolic link inside " + String.valueOf(path.getFileName()));
                    }
                    if (!Files.isRegularFile(path2, LinkOption.NOFOLLOW_LINKS)) continue;
                    String string = path.relativize(path2).toString().replace('\\', '/');
                    ServerResourcePacks.validatePath(string, hashSet);
                    InputStream inputStream = Files.newInputStream(path2, new OpenOption[0]);
                    try {
                        treeMap.put(string, budget.read(inputStream));
                    }
                    finally {
                        if (inputStream == null) continue;
                        inputStream.close();
                    }
                }
            }
        }
        try (ZipFile zipFile = new ZipFile(path.toFile());){
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                String string = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    ServerResourcePacks.validatePath(string.replaceAll("/$", ""), new HashSet<String>());
                    continue;
                }
                ServerResourcePacks.validatePath(string, hashSet);
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                try {
                    treeMap.put(string, budget.read(inputStream));
                }
                finally {
                    if (inputStream == null) continue;
                    inputStream.close();
                }
            }
        }
        if (!treeMap.containsKey("pack.mcmeta")) {
            throw new IOException(String.valueOf(path.getFileName()) + ": pack.mcmeta must be at the pack root");
        }
        return treeMap;
    }

    private static void validatePath(String string2, Set<String> set) throws IOException {
        if (string2.isEmpty() || string2.startsWith("/") || string2.contains("\\") || string2.contains(":") || string2.indexOf(0) >= 0 || Arrays.stream(string2.split("/", -1)).anyMatch(string -> string.isEmpty() || string.equals(".") || string.equals(".."))) {
            throw new IOException("Unsafe pack entry: " + string2);
        }
        if (!set.add(string2.toLowerCase(Locale.ROOT))) {
            throw new IOException("Duplicate/case-colliding pack entry: " + string2);
        }
        if (string2.startsWith("assets/") && !string2.matches("[a-z0-9_./-]+")) {
            throw new IOException("Invalid Minecraft resource path: " + string2);
        }
    }

    private static Map<String, byte[]> flatten(Map<String, byte[]> map, JsonObject jsonObject) throws IOException {
        return ServerResourcePacks.flatten(map, jsonObject, ClientRenderProfile.nativeProfile());
    }

    private static Map<String, byte[]> flatten(Map<String, byte[]> map, JsonObject jsonObject, ClientRenderProfile clientRenderProfile) throws IOException {
        TreeMap<String, byte[]> treeMap = new TreeMap<String, byte[]>();
        map.forEach((string, byArray) -> {
            if (string.startsWith("assets/") || string.equals("pack.png")) {
                treeMap.put((String)string, (byte[])byArray);
            }
        });
        if (jsonObject.has("overlays")) {
            for (JsonElement jsonElement : jsonObject.getAsJsonObject("overlays").getAsJsonArray("entries")) {
                JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                String string2 = jsonObject2.get("directory").getAsString();
                ServerResourcePacks.validatePath(string2, new HashSet<String>());
                if (!ServerResourcePacks.supports(jsonObject2, clientRenderProfile)) continue;
                String string3 = string2 + "/";
                for (Map.Entry<String, byte[]> entry : map.entrySet()) {
                    if (!entry.getKey().startsWith(string3 + "assets/")) continue;
                    treeMap.put(entry.getKey().substring(string3.length()), entry.getValue());
                }
            }
        }
        return treeMap;
    }

    private static void checkFormat(JsonObject jsonObject, String string, ClientRenderProfile clientRenderProfile) throws IOException {
        if (!jsonObject.has("pack") || !ServerResourcePacks.supports(jsonObject.getAsJsonObject("pack"), clientRenderProfile)) {
            throw new IOException(string + ": pack does not declare support for resource format " + clientRenderProfile.key());
        }
    }

    private static boolean supports(JsonObject jsonObject, ClientRenderProfile clientRenderProfile) throws IOException {
        JsonElement jsonElement;
        int n = clientRenderProfile.resourceMajor();
        if (jsonObject.has("min_format") && jsonObject.has("max_format")) {
            return ServerResourcePacks.format(jsonObject.get("min_format"), false) <= (long)n * 10000L + (long)clientRenderProfile.resourceMinor() && ServerResourcePacks.format(jsonObject.get("max_format"), true) >= (long)n * 10000L + (long)clientRenderProfile.resourceMinor();
        }
        JsonElement jsonElement2 = jsonElement = jsonObject.has("supported_formats") ? jsonObject.get("supported_formats") : jsonObject.get("formats");
        if (jsonElement != null) {
            if (jsonElement.isJsonPrimitive()) {
                return jsonElement.getAsInt() == n;
            }
            if (jsonElement.isJsonArray()) {
                return jsonElement.getAsJsonArray().get(0).getAsInt() <= n && jsonElement.getAsJsonArray().get(1).getAsInt() >= n;
            }
            JsonObject jsonObject2 = jsonElement.getAsJsonObject();
            return jsonObject2.get("min_inclusive").getAsInt() <= n && jsonObject2.get("max_inclusive").getAsInt() >= n;
        }
        return jsonObject.has("pack_format") && jsonObject.get("pack_format").getAsInt() == n;
    }

    private static long format(JsonElement jsonElement, boolean bl) throws IOException {
        if (jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsLong() * 10000L + (long)(bl ? 9999 : 0);
        }
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        if (jsonArray.size() < 1 || jsonArray.size() > 2) {
            throw new IOException("Invalid resource format version");
        }
        return jsonArray.get(0).getAsLong() * 10000L + (jsonArray.size() == 2 ? jsonArray.get(1).getAsLong() : (bl ? 9999L : 0L));
    }

    public static Set<String> externalResources(Path path2, long l, int n) throws IOException {
        ServerResourcePacks.initialize(path2);
        HashSet<String> hashSet = new HashSet<String>();
        Budget budget = new Budget(l, n);
        try (Stream<Path> stream = Files.list(path2.resolve(FOLDER));){
            for (Path path3 : stream.filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) || path.toString().endsWith(".zip")).toList()) {
                if (Files.isSymbolicLink(path3)) {
                    throw new IOException("Symbolic link owner input");
                }
                Map<String, byte[]> map = ServerResourcePacks.read(path3, budget);
                hashSet.addAll(ServerResourcePacks.flatten(map, ServerResourcePacks.object(map.get("pack.mcmeta"), "owner metadata")).keySet());
            }
        }
        return hashSet;
    }

    private static boolean sharedJson(String string) {
        return string.matches("assets/[^/]+/(font|lang|items|atlases)/.+\\.json") || string.matches("assets/[^/]+/sounds\\.json");
    }

    private static byte[] combine(String string, byte[] byArray, byte[] byArray2, boolean bl) throws IOException {
        JsonObject jsonObject;
        JsonObject jsonObject2 = ServerResourcePacks.object(byArray, string);
        if (jsonObject2.equals((Object)(jsonObject = ServerResourcePacks.object(byArray2, string)))) {
            return byArray;
        }
        if (string.contains("/atlases/")) {
            JsonArray jsonArray = new JsonArray();
            for (JsonElement jsonElement : jsonObject2.getAsJsonArray("sources")) {
                if (ServerResourcePacks.contains(jsonArray, jsonElement)) continue;
                jsonArray.add(jsonElement);
            }
            for (JsonElement jsonElement : jsonObject.getAsJsonArray("sources")) {
                if (ServerResourcePacks.contains(jsonArray, jsonElement)) continue;
                jsonArray.add(jsonElement);
            }
            jsonObject2.add("sources", (JsonElement)jsonArray);
            return ServerResourcePacks.encode((JsonElement)jsonObject2);
        }
        if (string.contains("/items/")) {
            JsonObject jsonObject3 = jsonObject2.getAsJsonObject("model");
            JsonObject jsonObject4 = jsonObject.getAsJsonObject("model");
            if (!ServerResourcePacks.cmdDispatch(jsonObject3) || !ServerResourcePacks.cmdDispatch(jsonObject4)) {
                throw new IOException("Incompatible item definitions: " + string + "; remap this material explicitly");
            }
            TreeMap<Double, JsonObject> treeMap = new TreeMap<Double, JsonObject>();
            for (JsonElement jsonElement : jsonObject3.getAsJsonArray("entries")) {
                treeMap.put(jsonElement.getAsJsonObject().get("threshold").getAsDouble(), jsonElement.getAsJsonObject());
            }
            for (JsonElement jsonElement : jsonObject4.getAsJsonArray("entries")) {
                JsonObject jsonObject5 = jsonElement.getAsJsonObject();
                double d = jsonObject5.get("threshold").getAsDouble();
                JsonObject jsonObject6 = (JsonObject)treeMap.get(d);
                if (jsonObject6 != null && !jsonObject6.get("model").equals(jsonObject5.get("model"))) {
                    throw new IOException("Custom model data conflict: " + string + " at " + d);
                }
                treeMap.put(d, jsonObject5);
            }
            if (!Objects.equals(jsonObject3.get("fallback"), jsonObject4.get("fallback"))) {
                throw new IOException("Different item fallbacks: " + string);
            }
            Iterator iterator = new JsonArray();
            treeMap.values().forEach(arg_0 -> ((JsonArray)iterator).add(arg_0));
            jsonObject3.add("entries", (JsonElement)iterator);
            return ServerResourcePacks.encode((JsonElement)jsonObject2);
        }
        if (string.contains("/font/")) {
            if (!jsonObject2.has("providers") || !jsonObject.has("providers")) {
                throw new IOException("Missing font providers: " + string);
            }
            JsonArray jsonArray = new JsonArray();
            JsonArray jsonArray2 = jsonObject2.getAsJsonArray("providers");
            JsonArray jsonArray3 = jsonObject.getAsJsonArray("providers");
            if (bl) {
                HashSet hashSet = new HashSet();
                for (JsonElement jsonElement : jsonArray2) {
                    hashSet.addAll(ServerResourcePacks.glyphs(jsonElement.getAsJsonObject()));
                }
                for (JsonElement jsonElement : jsonArray3) {
                    if (ServerResourcePacks.contains(jsonArray2, jsonElement)) continue;
                    Set<Integer> set = ServerResourcePacks.glyphs(jsonElement.getAsJsonObject());
                    set.retainAll(hashSet);
                    if (!set.isEmpty()) {
                        throw new IOException("Font glyph conflict in " + string + " at U+" + Integer.toHexString(set.iterator().next()).toUpperCase(Locale.ROOT));
                    }
                    String string2 = jsonElement.getAsJsonObject().get("type").getAsString().replace("minecraft:", "");
                    if (hashSet.isEmpty() || Set.of("bitmap", "space", "reference").contains(string2)) continue;
                    throw new IOException("Cannot verify HUD glyph compatibility for " + string2 + " provider in " + string + "; use a separate named font");
                }
            }
            for (Iterator iterator : jsonArray3) {
                if (ServerResourcePacks.contains(jsonArray, (JsonElement)iterator)) continue;
                jsonArray.add((JsonElement)iterator);
            }
            for (Iterator iterator : jsonArray2) {
                if (ServerResourcePacks.contains(jsonArray, (JsonElement)iterator)) continue;
                jsonArray.add((JsonElement)iterator);
            }
            jsonObject.add("providers", (JsonElement)jsonArray);
            return ServerResourcePacks.encode((JsonElement)jsonObject);
        }
        for (Map.Entry entry : jsonObject.entrySet()) {
            if (bl && jsonObject2.has((String)entry.getKey()) && !jsonObject2.get((String)entry.getKey()).equals(entry.getValue())) {
                throw new IOException("Conflicting Enorials JSON key: " + string + " -> " + (String)entry.getKey());
            }
            if (!bl && string.endsWith("/sounds.json") && jsonObject2.has((String)entry.getKey()) && ((JsonElement)entry.getValue()).isJsonObject()) {
                JsonObject jsonObject7 = ((JsonElement)entry.getValue()).getAsJsonObject();
                JsonObject jsonObject8 = jsonObject2.getAsJsonObject((String)entry.getKey());
                if (!jsonObject7.has("replace") || !jsonObject7.get("replace").getAsBoolean()) {
                    JsonArray jsonArray = new JsonArray();
                    if (jsonObject8.has("sounds")) {
                        jsonArray.addAll(jsonObject8.getAsJsonArray("sounds"));
                    }
                    if (jsonObject7.has("sounds")) {
                        jsonArray.addAll(jsonObject7.getAsJsonArray("sounds"));
                    }
                    jsonObject7.add("sounds", (JsonElement)jsonArray);
                }
            }
            jsonObject2.add((String)entry.getKey(), (JsonElement)entry.getValue());
        }
        return ServerResourcePacks.encode((JsonElement)jsonObject2);
    }

    private static boolean cmdDispatch(JsonObject jsonObject) {
        return !(jsonObject == null || !jsonObject.has("type") || !jsonObject.get("type").getAsString().replace("minecraft:", "").equals("range_dispatch") || !jsonObject.has("property") || !jsonObject.get("property").getAsString().replace("minecraft:", "").equals("custom_model_data") || jsonObject.has("index") && jsonObject.get("index").getAsInt() != 0 || jsonObject.has("scale") && jsonObject.get("scale").getAsDouble() != 1.0);
    }

    private static Set<Integer> glyphs(JsonObject jsonObject) {
        HashSet<Integer> hashSet = new HashSet<Integer>();
        if (jsonObject.has("chars")) {
            for (Object object : jsonObject.getAsJsonArray("chars")) {
                object.getAsString().codePoints().filter(n -> n != 0 && n != 32).forEach(hashSet::add);
            }
        }
        if (jsonObject.has("advances")) {
            for (Object object : jsonObject.getAsJsonObject("advances").keySet()) {
                ((String)object).codePoints().forEach(hashSet::add);
            }
        }
        return hashSet;
    }

    private static void checkReferences(String string, Map<String, byte[]> map, Map<String, byte[]> map2) throws IOException {
        JsonElement jsonElement2;
        HashSet<Integer> hashSet = new HashSet<Integer>();
        for (JsonElement jsonElement2 : ServerResourcePacks.object(map.get(string), string).getAsJsonArray("providers")) {
            hashSet.addAll(ServerResourcePacks.glyphs(jsonElement2.getAsJsonObject()));
        }
        Iterator iterator = ServerResourcePacks.object(map2.get(string), string).getAsJsonArray("providers");
        jsonElement2 = ServerResourcePacks.object(map.get(string), string).getAsJsonArray("providers");
        Iterator iterator2 = iterator.iterator();
        while (iterator2.hasNext()) {
            JsonElement jsonElement3 = (JsonElement)iterator2.next();
            if (ServerResourcePacks.contains((JsonArray)jsonElement2, jsonElement3)) continue;
            ServerResourcePacks.validateReferencedProvider(string, jsonElement3.getAsJsonObject(), hashSet, map, map2, new HashSet<String>(Set.of(string)));
        }
    }

    private static void validateReferencedProvider(String string, JsonObject jsonObject, Set<Integer> set, Map<String, byte[]> map, Map<String, byte[]> map2, Set<String> set2) throws IOException {
        String string2 = jsonObject.get("type").getAsString().replace("minecraft:", "");
        Set<Integer> set3 = ServerResourcePacks.glyphs(jsonObject);
        set3.retainAll(set);
        if (!set3.isEmpty()) {
            throw new IOException("Referenced font glyph conflict in " + string + " at U+" + Integer.toHexString(set3.iterator().next()).toUpperCase(Locale.ROOT));
        }
        if (string2.equals("reference")) {
            String string3;
            Object object = jsonObject.get("id").getAsString();
            if (!((String)object).contains(":")) {
                object = "minecraft:" + (String)object;
            }
            if (!set2.add(string3 = "assets/" + ((String)object).replace(":", "/font/") + ".json")) {
                throw new IOException("Font reference cycle: " + string3);
            }
            byte[] byArray = map2.getOrDefault(string3, map.get(string3));
            if (byArray == null) {
                if (!Set.of("minecraft:include/default", "minecraft:include/space", "minecraft:include/unifont").contains(object)) {
                    throw new IOException("Cannot verify font reference: " + (String)object);
                }
            } else {
                for (JsonElement jsonElement : ServerResourcePacks.object(byArray, string3).getAsJsonArray("providers")) {
                    ServerResourcePacks.validateReferencedProvider(string3, jsonElement.getAsJsonObject(), set, map, map2, set2);
                }
            }
            set2.remove(string3);
        } else if (!set.isEmpty() && !Set.of("bitmap", "space").contains(string2)) {
            throw new IOException("Cannot verify HUD glyph compatibility for " + string2 + " provider in " + string + "; use a separate named font");
        }
    }

    private static boolean contains(JsonArray jsonArray, JsonElement jsonElement) {
        for (JsonElement jsonElement2 : jsonArray) {
            if (!jsonElement2.equals(jsonElement)) continue;
            return true;
        }
        return false;
    }

    private static JsonObject object(byte[] byArray, String string) throws IOException {
        try {
            if (byArray == null) {
                throw new IllegalArgumentException("missing");
            }
            return JsonParser.parseString((String)new String(byArray, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        catch (RuntimeException runtimeException) {
            throw new IOException("Invalid JSON: " + string, runtimeException);
        }
    }

    private static byte[] encode(JsonElement jsonElement) {
        return JSON.toJson(jsonElement).getBytes(StandardCharsets.UTF_8);
    }

    private static String fingerprint(Map<String, byte[]> map) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            for (Map.Entry<String, byte[]> entry : new TreeMap<String, byte[]>(map).entrySet()) {
                messageDigest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                messageDigest.update((byte)0);
                messageDigest.update(MessageDigest.getInstance("SHA-256").digest(entry.getValue()));
            }
            return HexFormat.of().formatHex(messageDigest.digest());
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }

    private static String sha(byte[] byArray) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(byArray));
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }

    private static /* synthetic */ void lambda$merge$3(JsonObject jsonObject, Map.Entry entry) {
        jsonObject.add((String)entry.getKey(), (JsonElement)entry.getValue());
    }

    public record Result(Map<String, byte[]> entries, Map<String, byte[]> visibleBars, String fingerprint) {
    }

    private static final class Budget {
        long remaining;
        int files;

        Budget(long l, int n) {
            if (l < 1L || n < 1) {
                throw new IllegalArgumentException("Pack limits must be positive");
            }
            this.remaining = l;
            this.files = n;
        }

        byte[] read(InputStream inputStream) throws IOException {
            int n;
            if (--this.files < 0) {
                throw new IOException("Owner resource packs exceed max-files");
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] byArray = new byte[16384];
            while ((n = inputStream.read(byArray)) != -1) {
                this.remaining -= (long)n;
                if (this.remaining < 0L) {
                    throw new IOException("Owner resource packs exceed max-uncompressed-mb");
                }
                byteArrayOutputStream.write(byArray, 0, n);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }
}

