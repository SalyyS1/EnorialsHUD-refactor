/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.common.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class BetonQuestPack {
    public static final String FILE = "resource-pack/Enorials-BetonQuest.zip";

    public static Snapshot empty() {
        return new Snapshot("", Map.of());
    }

    private BetonQuestPack() {
    }

    public static Snapshot load(Plugin plugin) throws IOException {
        Plugin plugin2 = Bukkit.getPluginManager().getPlugin("BetonQuest");
        if (!plugin.getConfig().getBoolean("resource-pack.betonquest.enabled", true) || plugin2 == null || !plugin2.isEnabled()) {
            return BetonQuestPack.empty();
        }
        Path path = plugin2.getDataFolder().toPath().resolve(FILE);
        if (!Files.exists(path, new LinkOption[0])) {
            return BetonQuestPack.empty();
        }
        return BetonQuestPack.read(path);
    }

    public static Snapshot read(Path path) throws IOException {
        Object object;
        if (Files.size(path) > 0x2000000L) {
            throw new IOException("BetonQuest pack ZIP exceeds 32 MiB");
        }
        byte[] byArray = Files.readAllBytes(path);
        Map<String, byte[]> map = BetonQuestPack.unzip(byArray);
        try {
            object = JsonParser.parseString((String)new String(Objects.requireNonNull(map.get("pack.mcmeta")), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!object.has("pack") || object.has("filter") || object.has("overlays")) {
                throw new IOException("BetonQuest pack needs a flat pack.mcmeta without filters/overlays");
            }
            JsonObject jsonObject = object.getAsJsonObject("pack");
            if (!jsonObject.has("min_format") || !jsonObject.has("max_format") || BetonQuestPack.version(jsonObject.get("min_format")) > 88.0 || BetonQuestPack.version(jsonObject.get("max_format")) < 88.0) {
                throw new IOException("BetonQuest pack must declare support for resource format 88.0");
            }
        }
        catch (IOException iOException) {
            throw iOException;
        }
        catch (RuntimeException runtimeException) {
            throw new IOException("Invalid BetonQuest pack.mcmeta", runtimeException);
        }
        object = new TreeMap();
        for (Map.Entry entry : map.entrySet()) {
            if (!((String)entry.getKey()).startsWith("assets/")) continue;
            object.put((String)entry.getKey(), (byte[])entry.getValue());
        }
        if (object.isEmpty()) {
            throw new IOException("BetonQuest pack contains no assets");
        }
        return new Snapshot("-betonquest-" + BetonQuestPack.sha(byArray), (Map<String, byte[]>)object);
    }

    private static double version(JsonElement jsonElement) {
        return jsonElement.isJsonArray() ? (double)jsonElement.getAsJsonArray().get(0).getAsInt() + (double)jsonElement.getAsJsonArray().get(1).getAsInt() / 1000.0 : jsonElement.getAsDouble();
    }

    private static String sha(byte[] byArray) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(byArray));
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IOException(noSuchAlgorithmException);
        }
    }

    public static Map<String, byte[]> merge(Map<String, byte[]> map, Snapshot snapshot) throws IOException {
        TreeMap<String, byte[]> treeMap = new TreeMap<String, byte[]>(map);
        for (Map.Entry<String, byte[]> entry : snapshot.entries().entrySet()) {
            byte[] byArray = treeMap.putIfAbsent(entry.getKey(), entry.getValue());
            if (byArray == null || Arrays.equals(byArray, entry.getValue())) continue;
            throw new IOException("BetonQuest pack conflicts with another resource: " + entry.getKey());
        }
        return treeMap;
    }

    public static byte[] mergeZip(byte[] byArray, Snapshot snapshot) throws IOException {
        if (!snapshot.present()) {
            return byArray;
        }
        Map<String, byte[]> map = BetonQuestPack.merge(BetonQuestPack.unzip(byArray), snapshot);
        JsonObject jsonObject = JsonParser.parseString((String)new String(map.get("pack.mcmeta"), StandardCharsets.UTF_8)).getAsJsonObject();
        jsonObject.getAsJsonObject("pack").addProperty("description", "Enorials Spells + BetonQuest quest icons");
        map.put("pack.mcmeta", jsonObject.toString().getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);){
            for (Map.Entry<String, byte[]> entry : map.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setTime(0L);
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(entry.getValue());
                zipOutputStream.closeEntry();
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static Map<String, byte[]> unzip(byte[] byArray) throws IOException {
        TreeMap<String, byte[]> treeMap = new TreeMap<String, byte[]>();
        HashSet<String> hashSet = new HashSet<String>();
        long l = 0x8000000L;
        int n = 0;
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(byArray));){
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                int n2;
                String string2 = zipEntry.getName();
                if (zipEntry.isDirectory()) continue;
                if (++n > 20000 || string2.startsWith("/") || string2.contains("\\") || string2.contains(":") || Arrays.stream(string2.split("/", -1)).anyMatch(string -> string.isEmpty() || string.equals(".") || string.equals("..")) || !hashSet.add(string2.toLowerCase(Locale.ROOT))) {
                    throw new IOException("Invalid or duplicate resource pack entry: " + string2);
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] byArray2 = new byte[16384];
                while ((n2 = zipInputStream.read(byArray2)) != -1) {
                    if ((l -= (long)n2) < 0L) {
                        throw new IOException("Resource pack exceeds 128 MiB unpacked");
                    }
                    byteArrayOutputStream.write(byArray2, 0, n2);
                }
                treeMap.put(string2, byteArrayOutputStream.toByteArray());
            }
        }
        return treeMap;
    }

    public record Snapshot(String key, Map<String, byte[]> entries) {
        public Snapshot {
            map = Collections.unmodifiableMap(new TreeMap<String, byte[]>(map));
        }

        public boolean present() {
            return !this.entries.isEmpty();
        }
    }
}

