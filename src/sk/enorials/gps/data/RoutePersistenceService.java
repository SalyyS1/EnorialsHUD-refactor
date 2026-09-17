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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.nav.RouteDescriptor;

public final class RoutePersistenceService {
    private final EnorialsGpsPlugin plugin;
    private final File file;
    private YamlConfiguration cfg;

    public RoutePersistenceService(EnorialsGpsPlugin enorialsGpsPlugin) {
        this.plugin = enorialsGpsPlugin;
        this.file = new File(enorialsGpsPlugin.getDataFolder(), "routes.yml");
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

    public void save(UUID uUID, RouteDescriptor routeDescriptor, NavigationMode navigationMode, String string) {
        String string2 = "players." + String.valueOf(uUID);
        this.cfg.set(string2 + ".type", (Object)routeDescriptor.type().name().toLowerCase());
        this.cfg.set(string2 + ".value", (Object)routeDescriptor.value());
        this.cfg.set(string2 + ".mode", (Object)navigationMode.name().toLowerCase());
        this.cfg.set(string2 + ".source", (Object)string);
        this.flush();
    }

    public void clear(UUID uUID) {
        this.cfg.set("players." + String.valueOf(uUID), null);
        this.flush();
    }

    public Optional<PersistedRoute> get(UUID uUID) {
        String string = "players." + String.valueOf(uUID);
        if (!this.cfg.contains(string + ".type")) {
            return Optional.empty();
        }
        String string2 = this.cfg.getString(string + ".type", "");
        String string3 = this.cfg.getString(string + ".value", "");
        String string4 = this.cfg.getString(string + ".mode", "actionbar");
        String string5 = this.cfg.getString(string + ".source", "resume");
        try {
            RouteDescriptor routeDescriptor = new RouteDescriptor(RouteDescriptor.Type.valueOf(string2.toUpperCase()), string3);
            NavigationMode navigationMode = NavigationMode.fromConfig(string4);
            return Optional.of(new PersistedRoute(routeDescriptor, navigationMode, string5));
        }
        catch (Exception exception) {
            return Optional.empty();
        }
    }

    public Map<UUID, PersistedRoute> getAll() {
        HashMap<UUID, PersistedRoute> hashMap = new HashMap<UUID, PersistedRoute>();
        ConfigurationSection configurationSection = this.cfg.getConfigurationSection("players");
        if (configurationSection == null) {
            return hashMap;
        }
        for (String string : configurationSection.getKeys(false)) {
            try {
                UUID uUID = UUID.fromString(string);
                this.get(uUID).ifPresent(persistedRoute -> hashMap.put(uUID, (PersistedRoute)persistedRoute));
            }
            catch (IllegalArgumentException illegalArgumentException) {}
        }
        return hashMap;
    }

    private void flush() {
        try {
            this.cfg.save(this.file);
        }
        catch (IOException iOException) {
            this.plugin.getLogger().warning("Failed to save routes.yml: " + iOException.getMessage());
        }
    }

    public record PersistedRoute(RouteDescriptor descriptor, NavigationMode mode, String source) {
    }
}

