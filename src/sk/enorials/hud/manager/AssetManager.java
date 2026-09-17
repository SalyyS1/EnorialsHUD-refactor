/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.hud.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.model.HudAsset;

public class AssetManager {
    private final Map<String, HudAsset> assets = new HashMap<String, HudAsset>();

    public void reload(EnorialsHUD enorialsHUD) {
        this.assets.clear();
        File file = enorialsHUD.getDataFolder();
        this.loadFromDir(enorialsHUD, new File(file, "assets/hud"));
        this.loadFromDir(enorialsHUD, new File(file, "assets/assets/hud"));
        if (!this.has("enorialshud-panel")) {
            this.loadRecursively(enorialsHUD, new File(file, "assets").toPath());
        }
        if (!this.has("enorialshud-panel")) {
            enorialsHUD.getLogger().warning("Core asset 'enorialshud-panel' is still missing after fallback scan.");
        }
    }

    private void loadFromDir(EnorialsHUD enorialsHUD, File file2) {
        File[] fileArray;
        if (file2.exists() && file2.isDirectory() && (fileArray = file2.listFiles((file, string) -> string.endsWith(".yml"))) != null) {
            for (File file3 : fileArray) {
                this.loadFromFile(enorialsHUD, file3);
            }
        }
    }

    private void loadRecursively(EnorialsHUD enorialsHUD, Path path2) {
        if (path2 != null && Files.exists(path2, new LinkOption[0])) {
            try {
                Files.walk(path2, new FileVisitOption[0]).filter(path -> Files.isRegularFile(path, new LinkOption[0])).filter(path -> path.getFileName().toString().endsWith(".yml")).forEach(path -> this.loadFromFile(enorialsHUD, path.toFile()));
            }
            catch (IOException iOException) {
                enorialsHUD.getLogger().log(Level.WARNING, "Could not scan assets recursively: " + iOException.getMessage());
            }
        }
    }

    private void loadFromFile(EnorialsHUD enorialsHUD, File file) {
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file);
        for (String string : yamlConfiguration.getKeys(false)) {
            if (!yamlConfiguration.isConfigurationSection(string)) continue;
            try {
                HudAsset hudAsset = new HudAsset(enorialsHUD, yamlConfiguration.getConfigurationSection(string));
                if (this.assets.containsKey(hudAsset.getKey())) continue;
                this.assets.put(hudAsset.getKey(), hudAsset);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                enorialsHUD.getLogger().log(Level.WARNING, "Could not load asset '" + string + "': " + illegalArgumentException.getMessage());
            }
        }
    }

    public HudAsset get(String string) {
        return this.assets.get(string.toLowerCase(Locale.ROOT));
    }

    public boolean has(String string) {
        return this.assets.containsKey(string.toLowerCase(Locale.ROOT));
    }

    public Collection<HudAsset> getAssets() {
        return this.assets.values();
    }
}

