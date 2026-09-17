/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import sk.enorials.hud.EnorialsHUD;

public final class ObsoleteArtwork {
    private ObsoleteArtwork() {
    }

    public static void clean(EnorialsHUD enorialsHUD) throws IOException {
        Object object;
        Object object2;
        Properties properties = new Properties();
        try (Object object3 = enorialsHUD.getResource("obsolete-textures.properties");){
            if (object3 == null) {
                return;
            }
            properties.load((InputStream)object3);
        }
        object3 = enorialsHUD.getDataFolder().toPath();
        Path path2 = object3.resolve("textures").toAbsolutePath().normalize();
        StringBuilder stringBuilder = new StringBuilder();
        for (String object4 : List.of("assets", "layout", "popup", "display", "pack")) {
            Path path3 = object3.resolve(object4);
            if (!Files.isDirectory(path3, new LinkOption[0])) continue;
            object2 = Files.walk(path3, new FileVisitOption[0]);
            try {
                object = object2.filter(path -> Files.isRegularFile(path, new LinkOption[0])).filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".json")).toList().iterator();
                while (object.hasNext()) {
                    Path path4 = object.next();
                    stringBuilder.append(Files.readString(path4)).append('\n');
                }
            }
            finally {
                if (object2 == null) continue;
                object2.close();
            }
        }
        int n = 0;
        for (String string : properties.stringPropertyNames()) {
            object2 = path2.resolve(string).normalize();
            if (!object2.startsWith(path2) || !Files.isRegularFile((Path)object2, new LinkOption[0]) || stringBuilder.indexOf(string) >= 0 || !((String)(object = string.replaceFirst("_\\d+\\.png$", ".png"))).equals(string) && !Files.isRegularFile(path2.resolve((String)object), new LinkOption[0]) && stringBuilder.indexOf((String)object) >= 0) continue;
            try {
                if (!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes((Path)object2))).equals(properties.getProperty(string))) continue;
                Files.delete((Path)object2);
                ++n;
            }
            catch (NoSuchAlgorithmException noSuchAlgorithmException) {
                throw new AssertionError((Object)noSuchAlgorithmException);
            }
        }
        if (n > 0) {
            enorialsHUD.getLogger().info("Removed " + n + " obsolete default textures.");
        }
    }
}

