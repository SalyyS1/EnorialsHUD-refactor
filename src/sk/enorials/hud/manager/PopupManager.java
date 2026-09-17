/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.hud.manager;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.model.HudPopup;

public class PopupManager {
    private final Map<String, HudPopup> popups = new HashMap<String, HudPopup>();

    public void reload(EnorialsHUD enorialsHUD) {
        this.popups.clear();
        File[] fileArray = new File(enorialsHUD.getDataFolder(), "popup").listFiles((file, string) -> string.endsWith(".yml"));
        if (fileArray != null) {
            for (File file2 : fileArray) {
                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file2);
                for (String string2 : yamlConfiguration.getKeys(false)) {
                    if (!yamlConfiguration.isConfigurationSection(string2)) continue;
                    try {
                        HudPopup hudPopup = new HudPopup(yamlConfiguration.getConfigurationSection(string2));
                        this.popups.put(hudPopup.getKey(), hudPopup);
                    }
                    catch (IllegalArgumentException illegalArgumentException) {
                        enorialsHUD.getLogger().log(Level.WARNING, "Could not load popup '" + string2 + "': " + illegalArgumentException.getMessage());
                    }
                }
            }
        }
    }

    public boolean has(String string) {
        return string != null && this.popups.containsKey(string.toLowerCase(Locale.ROOT));
    }

    public HudPopup get(String string) {
        return this.popups.get(string.toLowerCase(Locale.ROOT));
    }

    public Collection<HudPopup> getPopups() {
        return this.popups.values();
    }
}

