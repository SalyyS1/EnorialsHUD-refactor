/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.hud.pack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.hud.EnorialsHUD;

final class GpsEqualFields {
    GpsEqualFields() {
    }

    static void install(EnorialsHUD enorialsHUD) {
        Path path = enorialsHUD.getDataFolder().toPath();
        Path path2 = path.resolve("gps-equal-fields-25.txt");
        if (Files.exists(path2, new LinkOption[0])) {
            return;
        }
        try {
            Path path3 = path.resolve("assets/hud/gps-panel.yml");
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)path3.toFile());
            for (boolean bl : new boolean[]{false, true}) {
                Object object;
                String string = bl ? "_compact" : "";
                String string2 = (bl ? "gps-compact" : "gps") + ".layers.";
                String string3 = "assets/enorialshud/gps_frame" + string + "_equal_split.png";
                Path path4 = path.resolve("textures/" + string3);
                Files.createDirectories(path4.getParent(), new FileAttribute[0]);
                if (!Files.exists(path4, new LinkOption[0])) {
                    object = enorialsHUD.getResource("default/textures/" + string3);
                    try {
                        if (object == null) {
                            throw new IOException("Missing GPS frame");
                        }
                        Files.copy((InputStream)object, path4, new CopyOption[0]);
                    }
                    finally {
                        if (object != null) {
                            ((InputStream)object).close();
                        }
                    }
                }
                if (!((String)(object = yamlConfiguration.getString(string2 + "frame-split.texture.path", ""))).equals("assets/enorialshud/gps_frame" + string + "_split.png") && !((String)object).equals(string3)) continue;
                yamlConfiguration.set(string2 + "frame-split.texture.path", (Object)string3);
                yamlConfiguration.set(string2 + "distance.offset.x", (Object)(bl ? 49 : 58));
                yamlConfiguration.set(string2 + "height.offset.x", (Object)(bl ? 86 : 113));
            }
            yamlConfiguration.save(path3.toFile());
            Files.writeString(path2, (CharSequence)"Equal GPS fields installed; subsequent layout edits are preserved.\n", new OpenOption[0]);
        }
        catch (IOException iOException) {
            throw new UncheckedIOException("Cannot install equal GPS fields", iOException);
        }
    }
}

