/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.gps.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.gps.EnorialsGpsPlugin;

public final class PlacesMigration {
    private PlacesMigration() {
    }

    private static Map<String, Object> values(ConfigurationSection configurationSection) {
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        if (configurationSection != null) {
            configurationSection.getValues(true).forEach((string, object) -> {
                if (!(object instanceof ConfigurationSection)) {
                    hashMap.put((String)string, object);
                }
            });
        }
        return hashMap;
    }

    public static void run(EnorialsGpsPlugin enorialsGpsPlugin, File file) {
        try (InputStream inputStream = enorialsGpsPlugin.getResource("legacy-places.yml");){
            if (inputStream == null || !file.exists()) {
                return;
            }
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            YamlConfiguration yamlConfiguration2 = YamlConfiguration.loadConfiguration((File)file);
            boolean bl = false;
            ConfigurationSection configurationSection = yamlConfiguration.getConfigurationSection("poi.points");
            for (String string : configurationSection.getKeys(false)) {
                String string2 = "poi.points." + string;
                if (!yamlConfiguration2.isConfigurationSection(string2) || !PlacesMigration.values(yamlConfiguration2.getConfigurationSection(string2)).equals(PlacesMigration.values(yamlConfiguration.getConfigurationSection(string2)))) continue;
                yamlConfiguration2.set(string2, null);
                bl = true;
                if (!string.equals("spawn")) continue;
                yamlConfiguration2.set(string2 + ".display_name", (Object)"&aSpawn");
                yamlConfiguration2.set(string2 + ".spawn", (Object)true);
            }
            if (bl) {
                yamlConfiguration2.save(file);
            }
        }
        catch (IOException iOException) {
            throw new IllegalStateException("Could not migrate Places", iOException);
        }
    }
}

