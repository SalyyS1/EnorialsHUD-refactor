/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.hud.manager;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.condition.ConditionHolder;
import sk.enorials.hud.model.HudLayout;

public class DisplayManager {
    private final Map<String, DisplayEntry> displays = new HashMap<String, DisplayEntry>();

    public void reload(EnorialsHUD enorialsHUD) {
        this.displays.clear();
        File file2 = new File(enorialsHUD.getDataFolder(), "displays");
        File[] fileArray = file2.listFiles((file, string) -> string.endsWith(".yml"));
        if (fileArray != null) {
            for (File file3 : fileArray) {
                try {
                    YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file3);
                    for (String string2 : yamlConfiguration.getKeys(false)) {
                        ConfigurationSection configurationSection = yamlConfiguration.getConfigurationSection(string2);
                        if (configurationSection == null) continue;
                        String string3 = string2.toLowerCase();
                        String string4 = configurationSection.getString("layout");
                        HudLayout hudLayout = null;
                        if (string4 != null) {
                            hudLayout = enorialsHUD.layouts().get(string4);
                            if (hudLayout == null) {
                                enorialsHUD.getLogger().warning("Display '" + string3 + "' references missing layout '" + string4 + "'.");
                                continue;
                            }
                            string4 = hudLayout.getKey();
                        } else {
                            if (!configurationSection.isConfigurationSection("elements")) {
                                enorialsHUD.getLogger().warning("Display '" + string3 + "' has no layout or elements.");
                                continue;
                            }
                            hudLayout = new HudLayout(configurationSection);
                            string4 = hudLayout.getKey();
                        }
                        ConditionHolder conditionHolder = new ConditionHolder(enorialsHUD, "display:" + string3, null, configurationSection.getStringList("conditions"));
                        this.displays.put(string3, new DisplayEntry(string3, string4, hudLayout, conditionHolder));
                    }
                }
                catch (Exception exception) {
                    enorialsHUD.getLogger().log(Level.WARNING, "Could not load display file: " + file3.getName(), exception);
                }
            }
        }
    }

    public Collection<DisplayEntry> getDisplays() {
        return this.displays.values();
    }

    public boolean hasDisplays() {
        return !this.displays.isEmpty();
    }

    public record DisplayEntry(String key, String layoutKey, HudLayout layout, ConditionHolder conditions) {
    }
}

