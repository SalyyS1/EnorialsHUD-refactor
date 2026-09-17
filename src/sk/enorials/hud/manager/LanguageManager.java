/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.hud.manager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.hud.EnorialsHUD;

public class LanguageManager {
    private final Map<String, YamlConfiguration> languages = new HashMap<String, YamlConfiguration>();

    public void reload(EnorialsHUD enorialsHUD) {
        this.languages.clear();
        File file2 = new File(enorialsHUD.getDataFolder(), "language");
        File[] fileArray = file2.listFiles((file, string) -> string.endsWith(".yml"));
        if (fileArray != null) {
            for (File file3 : fileArray) {
                try {
                    this.languages.put(file3.getName().toLowerCase(), YamlConfiguration.loadConfiguration((File)file3));
                }
                catch (Exception exception) {
                    enorialsHUD.getLogger().log(Level.WARNING, "Could not load language file: " + file3.getName(), exception);
                }
            }
        }
    }

    public Map<String, YamlConfiguration> getLanguages() {
        return this.languages;
    }
}

