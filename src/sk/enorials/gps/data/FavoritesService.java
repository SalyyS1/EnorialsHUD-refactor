/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.gps.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.gps.EnorialsGpsPlugin;

public final class FavoritesService {
    private final EnorialsGpsPlugin plugin;
    private final File file;
    private YamlConfiguration cfg;

    public FavoritesService(EnorialsGpsPlugin enorialsGpsPlugin) {
        this.plugin = enorialsGpsPlugin;
        this.file = new File(enorialsGpsPlugin.getDataFolder(), "favorites.yml");
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

    public Set<String> getFavorites(UUID uUID) {
        return new LinkedHashSet<String>(this.cfg.getStringList(this.path(uUID)));
    }

    public ToggleResult toggle(UUID uUID, String string, int n) {
        Set<String> set = this.getFavorites(uUID);
        if (set.remove(string)) {
            this.save(uUID, set);
            return ToggleResult.REMOVED;
        }
        if (set.size() >= n) {
            return ToggleResult.LIMIT_REACHED;
        }
        set.add(string);
        this.save(uUID, set);
        return ToggleResult.ADDED;
    }

    public boolean remove(UUID uUID, String string) {
        Set<String> set = this.getFavorites(uUID);
        boolean bl = set.remove(string);
        if (bl) {
            this.save(uUID, set);
        }
        return bl;
    }

    private void save(UUID uUID, Set<String> set) {
        this.cfg.set(this.path(uUID), new ArrayList<String>(set));
        try {
            this.cfg.save(this.file);
        }
        catch (IOException iOException) {
            this.plugin.getLogger().warning("Failed to save favorites.yml: " + iOException.getMessage());
        }
    }

    private String path(UUID uUID) {
        return "players." + String.valueOf(uUID);
    }

    public static enum ToggleResult {
        ADDED,
        REMOVED,
        LIMIT_REACHED;

    }
}

