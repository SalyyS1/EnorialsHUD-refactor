/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.gps.data;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.nav.RouteDescriptor;

public final class QuestRouteService {
    private final EnorialsGpsPlugin plugin;
    private final File file;
    private YamlConfiguration cfg;

    public QuestRouteService(EnorialsGpsPlugin enorialsGpsPlugin) {
        this.plugin = enorialsGpsPlugin;
        this.file = new File(enorialsGpsPlugin.getDataFolder(), "quest-routes.yml");
        this.load();
    }

    private void load() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }
        if (!this.file.exists()) {
            try {
                this.file.createNewFile();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        this.cfg = YamlConfiguration.loadConfiguration((File)this.file);
    }

    public Map<String, StoredRoute> getRoutes(UUID uUID) {
        String string = "players." + String.valueOf(uUID);
        ConfigurationSection configurationSection = this.cfg.getConfigurationSection(string);
        if (configurationSection == null) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, StoredRoute> linkedHashMap = new LinkedHashMap<String, StoredRoute>();
        for (String string2 : configurationSection.getKeys(false)) {
            String string3 = string + "." + string2;
            String string4 = this.cfg.getString(string3 + ".type", "").toUpperCase(Locale.ROOT);
            String string5 = this.cfg.getString(string3 + ".value", "");
            String string6 = this.cfg.getString(string3 + ".display", string2);
            String string7 = this.cfg.getString(string3 + ".mode", "actionbar");
            String string8 = this.cfg.getString(string3 + ".source", "bq-dynamic");
            try {
                RouteDescriptor routeDescriptor = new RouteDescriptor(RouteDescriptor.Type.valueOf(string4), string5);
                NavigationMode navigationMode = NavigationMode.fromConfig(string7);
                linkedHashMap.put(string2, new StoredRoute(string2, string6, routeDescriptor, navigationMode, string8));
            }
            catch (Exception exception) {}
        }
        return linkedHashMap;
    }

    public void putRoute(UUID uUID, String string, String string2, RouteDescriptor routeDescriptor, NavigationMode navigationMode, String string3) {
        String string4 = this.normalizeKey(string);
        if (string4.isEmpty()) {
            return;
        }
        String string5 = "players." + String.valueOf(uUID) + "." + string4;
        this.cfg.set(string5 + ".display", (Object)string2);
        this.cfg.set(string5 + ".type", (Object)routeDescriptor.type().name().toLowerCase(Locale.ROOT));
        this.cfg.set(string5 + ".value", (Object)routeDescriptor.value());
        this.cfg.set(string5 + ".mode", (Object)navigationMode.name().toLowerCase(Locale.ROOT));
        this.cfg.set(string5 + ".source", (Object)string3);
        this.flush();
    }

    public boolean removeRoute(UUID uUID, String string) {
        String string2 = this.normalizeKey(string);
        if (string2.isEmpty()) {
            return false;
        }
        String string3 = "players." + String.valueOf(uUID) + "." + string2;
        if (!this.cfg.contains(string3)) {
            return false;
        }
        this.cfg.set(string3, null);
        this.flush();
        return true;
    }

    public int clearRoutes(UUID uUID) {
        String string = "players." + String.valueOf(uUID);
        ConfigurationSection configurationSection = this.cfg.getConfigurationSection(string);
        if (configurationSection == null) {
            return 0;
        }
        int n = configurationSection.getKeys(false).size();
        this.cfg.set(string, null);
        this.flush();
        return n;
    }

    private String normalizeKey(String string) {
        if (string == null) {
            return "";
        }
        return string.trim().toLowerCase(Locale.ROOT);
    }

    private void flush() {
        try {
            this.cfg.save(this.file);
        }
        catch (IOException iOException) {
            this.plugin.getLogger().warning("Failed to save quest-routes.yml: " + iOException.getMessage());
        }
    }

    public record StoredRoute(String key, String displayName, RouteDescriptor descriptor, NavigationMode mode, String source) {
    }
}

