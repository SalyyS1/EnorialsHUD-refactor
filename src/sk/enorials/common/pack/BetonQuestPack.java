package sk.enorials.common.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class BetonQuestPack {
   public static final String FILE = "resource-pack/Enorials-BetonQuest.zip";

   public static BetonQuestPack.Snapshot empty() {
      return new BetonQuestPack.Snapshot("", Map.of());
   }

   private BetonQuestPack() {
   }

   public static BetonQuestPack.Snapshot load(Plugin var0) throws IOException {
      Plugin var1 = Bukkit.getPluginManager().getPlugin("BetonQuest");
      if (var0.getConfig().getBoolean("resource-pack.betonquest.enabled", true) && var1 != null && var1.isEnabled()) {
         Path var2 = var1.getDataFolder().toPath().resolve("resource-pack/Enorials-BetonQuest.zip");
         return !Files.exists(var2) ? empty() : read(var2);
      } else {
         return empty();
      }
   }

   public static BetonQuestPack.Snapshot read(Path var0) throws IOException {
      if (Files.size(var0) > 33554432L) {
         throw new IOException("BetonQuest pack ZIP exceeds 32 MiB");
      }

      byte[] var1 = Files.readAllBytes(var0);
      Map<String, byte[]> var2 = unzip(var1);

      try {
         JsonObject var3 = JsonParser.parseString(new String(Objects.requireNonNull((byte[])var2.get("pack.mcmeta")), StandardCharsets.UTF_8))
            .getAsJsonObject();
         if (!var3.has("pack") || var3.has("filter") || var3.has("overlays")) {
            throw new IOException("BetonQuest pack needs a flat pack.mcmeta without filters/overlays");
         }

         JsonObject var4 = var3.getAsJsonObject("pack");
         if (!var4.has("min_format") || !var4.has("max_format") || version(var4.get("min_format")) > 88.0 || version(var4.get("max_format")) < 88.0) {
            throw new IOException("BetonQuest pack must declare support for resource format 88.0");
         }
      } catch (IOException var6) {
         throw var6;
      } catch (RuntimeException var7) {
         throw new IOException("Invalid BetonQuest pack.mcmeta", var7);
      }

      TreeMap<String, byte[]> var8 = new TreeMap<>();

      for (Entry<String, byte[]> var5 : var2.entrySet()) {
         if (var5.getKey().startsWith("assets/")) {
            var8.put(var5.getKey(), var5.getValue());
         }
      }

      if (var8.isEmpty()) {
         throw new IOException("BetonQuest pack contains no assets");
      } else {
         return new BetonQuestPack.Snapshot("-betonquest-" + sha(var1), var8);
      }
   }

   private static double version(JsonElement var0) {
      return var0.isJsonArray() ? var0.getAsJsonArray().get(0).getAsInt() + var0.getAsJsonArray().get(1).getAsInt() / 1000.0 : var0.getAsDouble();
   }

   private static String sha(byte[] var0) throws IOException {
      try {
         return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(var0));
      } catch (NoSuchAlgorithmException var2) {
         throw new IOException(var2);
      }
   }

   public static Map<String, byte[]> merge(Map<String, byte[]> var0, BetonQuestPack.Snapshot var1) throws IOException {
      TreeMap var2 = new TreeMap(var0);

      for (Entry var4 : var1.entries().entrySet()) {
         byte[] var5 = (byte[])var2.putIfAbsent((String)var4.getKey(), (byte[])var4.getValue());
         if (var5 != null && !Arrays.equals(var5, (byte[])var4.getValue())) {
            throw new IOException("BetonQuest pack conflicts with another resource: " + (String)var4.getKey());
         }
      }

      return var2;
   }

   public static byte[] mergeZip(byte[] var0, BetonQuestPack.Snapshot var1) throws IOException {
      if (!var1.present()) {
         return var0;
      }

      Map<String, byte[]> var2 = merge(unzip(var0), var1);
      JsonObject var3 = JsonParser.parseString(new String(var2.get("pack.mcmeta"), StandardCharsets.UTF_8)).getAsJsonObject();
      var3.getAsJsonObject("pack").addProperty("description", "Enorials Spells + BetonQuest quest icons");
      var2.put("pack.mcmeta", var3.toString().getBytes(StandardCharsets.UTF_8));
      ByteArrayOutputStream var4 = new ByteArrayOutputStream();

      try (ZipOutputStream var5 = new ZipOutputStream(var4)) {
         for (Entry<String, byte[]> var7 : var2.entrySet()) {
            ZipEntry var8 = new ZipEntry(var7.getKey());
            var8.setTime(0L);
            var5.putNextEntry(var8);
            var5.write(var7.getValue());
            var5.closeEntry();
         }
      }

      return var4.toByteArray();
   }

   private static Map<String, byte[]> unzip(byte[] var0) throws IOException {
      TreeMap var1 = new TreeMap();
      HashSet var2 = new HashSet();
      long var3 = 134217728L;
      int var5 = 0;

      ZipEntry var7;
      try (ZipInputStream var6 = new ZipInputStream(new ByteArrayInputStream(var0))) {
         while ((var7 = var6.getNextEntry()) != null) {
            String var8 = var7.getName();
            if (!var7.isDirectory()) {
               var5++;
               if (var5 > 20000
                  || var8.startsWith("/")
                  || var8.contains("\\")
                  || var8.contains(":")
                  || Arrays.stream(var8.split("/", -1)).anyMatch(var0x -> var0x.isEmpty() || var0x.equals(".") || var0x.equals(".."))
                  || !var2.add(var8.toLowerCase(Locale.ROOT))) {
                  throw new IOException("Invalid or duplicate resource pack entry: " + var8);
               }

               ByteArrayOutputStream var9 = new ByteArrayOutputStream();
               byte[] var10 = new byte[16384];

               int var11;
               while ((var11 = var6.read(var10)) != -1) {
                  if ((var3 -= var11) < 0L) {
                     throw new IOException("Resource pack exceeds 128 MiB unpacked");
                  }

                  var9.write(var10, 0, var11);
               }

               var1.put(var8, var9.toByteArray());
            }
         }
      }

      return var1;
   }

   public record Snapshot(String key, Map<String, byte[]> entries) {
      public Snapshot(String key, Map<String, byte[]> entries) {
         entries = Collections.unmodifiableMap(new TreeMap(entries));
         this.key = key;
         this.entries = entries;
      }

      public boolean present() {
         return !this.entries.isEmpty();
      }
   }
}
