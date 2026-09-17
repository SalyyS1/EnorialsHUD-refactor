/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.Configuration
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.gps.config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.config.PlacesMigration;

public final class SplitConfigManager {
    private static final List<String> FILES = List.of("core.yml", "navigation.yml", "input.yml", "integrations.yml", "poi.yml", "betonquest.yml");
    private static final List<String> LEGACY_FILES = List.of("language.yml", "modes.yml", "effects.yml", "restrictions.yml", "favorites.yml", "gui.yml", "prompts.yml", "chat_suggestions.yml");
    private final EnorialsGpsPlugin plugin;
    private final FileConfiguration legacyConfig;
    private YamlConfiguration merged;

    public SplitConfigManager(EnorialsGpsPlugin enorialsGpsPlugin, FileConfiguration fileConfiguration) {
        this.plugin = enorialsGpsPlugin;
        this.legacyConfig = fileConfiguration;
    }

    public void load() {
        boolean bl = this.hasAnyCanonicalConfig();
        this.ensureFiles();
        PlacesMigration.run(this.plugin, new File(new File(this.plugin.getDataFolder(), "configs"), "poi.yml"));
        this.legacyConfig.set("poi", null);
        this.pruneDeprecatedProtectionSection(new File(new File(this.plugin.getDataFolder(), "configs"), "core.yml"));
        this.merged = new YamlConfiguration();
        File file = new File(this.plugin.getDataFolder(), "configs");
        for (String object : FILES) {
            this.mergeFile(new File(file, object));
        }
        if (!bl) {
            boolean bl2 = false;
            for (String string : LEGACY_FILES) {
                File file2 = new File(file, string);
                if (!file2.exists()) continue;
                this.mergeFile(file2);
                bl2 = true;
            }
            if (bl2) {
                this.migrateMergedValuesToCanonicalFiles(file);
                this.plugin.getLogger().info("Legacy config files were imported into the new compact config layout (core.yml/navigation.yml/input.yml/...).");
            }
        }
        this.merged.set("protection", null);
        this.merged.setDefaults((Configuration)this.legacyConfig);
    }

    public FileConfiguration config() {
        return this.merged == null ? this.legacyConfig : this.merged;
    }

    private void ensureFiles() {
        File file = new File(this.plugin.getDataFolder(), "configs");
        if (!file.exists()) {
            file.mkdirs();
        }
        for (String string : FILES) {
            File file2 = new File(file, string);
            if (file2.exists()) continue;
            this.plugin.saveResource("configs/" + string, false);
        }
    }

    private boolean hasAnyCanonicalConfig() {
        File file = new File(this.plugin.getDataFolder(), "configs");
        if (!file.exists()) {
            return false;
        }
        for (String string : FILES) {
            if (!new File(file, string).exists()) continue;
            return true;
        }
        return false;
    }

    private void mergeFile(File file) {
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file);
        for (String string : yamlConfiguration.getKeys(false)) {
            this.merged.set(string, yamlConfiguration.get(string));
        }
    }

    private void pruneDeprecatedProtectionSection(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file);
        if (!yamlConfiguration.contains("protection")) {
            return;
        }
        yamlConfiguration.set("protection", null);
        try {
            yamlConfiguration.save(file);
        }
        catch (IOException iOException) {
            this.plugin.getLogger().warning("Failed to remove deprecated protection section from core.yml: " + iOException.getMessage());
        }
    }

    private void migrateMergedValuesToCanonicalFiles(File file) {
        this.writeSections(new File(file, "core.yml"), "language", "modes", "gui", "favorites");
        this.writeSections(new File(file, "navigation.yml"), "navigation", "effects", "restrictions");
        this.writeSections(new File(file, "input.yml"), "prompts", "chat_suggestions");
        this.writeSections(new File(file, "integrations.yml"), "integrations");
        this.writeSections(new File(file, "poi.yml"), "poi");
        this.writeSections(new File(file, "betonquest.yml"), "betonquest");
    }

    private void writeSections(File file, String ... stringArray) {
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file);
        for (String string : stringArray) {
            yamlConfiguration.set(string, this.merged.get(string));
        }
        try {
            yamlConfiguration.save(file);
        }
        catch (IOException iOException) {
            this.plugin.getLogger().warning("Failed to migrate config file " + file.getName() + ": " + iOException.getMessage());
        }
    }
}

