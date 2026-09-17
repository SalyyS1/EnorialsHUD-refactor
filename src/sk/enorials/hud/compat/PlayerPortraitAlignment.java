/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.hud.compat;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class PlayerPortraitAlignment {
    private PlayerPortraitAlignment() {
    }

    public static void migrate(File file) throws IOException {
        if (!file.isFile()) {
            return;
        }
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file);
        boolean bl = false;
        for (String string : new String[]{"player", "player-compact"}) {
            String string2 = string + ".layers.head.";
            if (!"player-avatar".equals(yamlConfiguration.getString(string2 + "font")) || yamlConfiguration.getDouble(string2 + "offset.x", -999.0) != 8.0 || yamlConfiguration.getDouble(string2 + "offset.y", -999.0) != 9.0 || yamlConfiguration.getDouble(string2 + "scale.x", 1.0) != 1.0 || yamlConfiguration.getDouble(string2 + "scale.y", 1.0) != 1.0) continue;
            yamlConfiguration.set(string2 + "offset.x", (Object)7);
            yamlConfiguration.set(string2 + "scale.x", (Object)1.025);
            bl = true;
        }
        if (bl) {
            yamlConfiguration.save(file);
        }
    }
}

