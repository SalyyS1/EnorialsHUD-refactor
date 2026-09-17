/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package sk.enorials.hud.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import sk.enorials.hud.EnorialsHUD;

public final class MarkerAssets {
    private MarkerAssets() {
    }

    public static void installIcons(EnorialsHUD enorialsHUD) {
        try {
            MarkerAssets.install(enorialsHUD, enorialsHUD.getDataFolder().toPath().resolve("minimap/icons"), "icons", "modules/minimap/minimap/");
        }
        catch (IOException iOException) {
            throw new UncheckedIOException("Cannot install marker icons", iOException);
        }
    }

    public static void installPack(EnorialsHUD enorialsHUD, Path path) throws IOException {
        MarkerAssets.install(enorialsHUD, path, "pack", "default/pack/");
    }

    private static void install(EnorialsHUD enorialsHUD, Path path, String string, String string2) throws IOException {
        path = path.toAbsolutePath().normalize();
        try (InputStream inputStream = enorialsHUD.getResource("pack-profiles/marker-art-upgrade.json");){
            if (inputStream == null) {
                throw new IOException("Missing marker art manifest");
            }
            JsonObject jsonObject = JsonParser.parseReader((Reader)new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject(string);
            for (Map.Entry entry : jsonObject.entrySet()) {
                Object object;
                Path path2 = path.resolve((String)entry.getKey()).normalize();
                if (!path2.startsWith(path)) {
                    throw new IOException("Invalid marker asset path");
                }
                if (Files.isRegularFile(path2, new LinkOption[0])) {
                    object = MarkerAssets.hash(Files.readAllBytes(path2));
                    boolean bl = false;
                    for (JsonElement jsonElement : ((JsonElement)entry.getValue()).getAsJsonArray()) {
                        if (!((String)object).equals(jsonElement.getAsString())) continue;
                        bl = true;
                        break;
                    }
                    if (!bl) continue;
                }
                object = enorialsHUD.getResource(string2 + (String)entry.getKey());
                try {
                    if (object == null) {
                        throw new IOException("Missing marker asset " + (String)entry.getKey());
                    }
                    Files.createDirectories(path2.getParent(), new FileAttribute[0]);
                    Files.copy((InputStream)object, path2, StandardCopyOption.REPLACE_EXISTING);
                }
                finally {
                    if (object == null) continue;
                    ((InputStream)object).close();
                }
            }
        }
    }

    private static String hash(byte[] byArray) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(byArray));
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IOException(noSuchAlgorithmException);
        }
    }
}

