/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.hud.manager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.theme.HudTheme;

public class StorageManager {
    private final EnorialsHUD plugin;
    private File file;
    private FileConfiguration config;

    public StorageManager(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
        this.reload();
    }

    public void reload() {
        this.file = new File(this.plugin.getDataFolder(), "data.yml");
        if (!this.file.exists()) {
            try {
                this.plugin.getDataFolder().mkdirs();
                this.file.createNewFile();
            }
            catch (IOException iOException) {
                iOException.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration((File)this.file);
        ConfigurationSection configurationSection = this.config.getConfigurationSection("players");
        boolean bl = false;
        if (configurationSection != null) {
            for (String string : configurationSection.getKeys(false)) {
                for (String string2 : List.of("offset", "layers")) {
                    if (!configurationSection.contains(string + "." + string2)) continue;
                    configurationSection.set(string + "." + string2, null);
                    bl = true;
                }
            }
        }
        if (bl) {
            this.save();
        }
    }

    public boolean preference(UUID uUID, String string, boolean bl) {
        return this.config.getBoolean("players." + String.valueOf(uUID) + ".preferences." + string, bl);
    }

    public HudTheme theme(UUID uUID) {
        return HudTheme.stored(this.config.getString("players." + String.valueOf(uUID) + ".preferences.theme", "original"));
    }

    public void setTheme(UUID uUID, String string) {
        HudTheme hudTheme = HudTheme.parse(string);
        this.config.set("players." + String.valueOf(uUID) + ".preferences.theme", (Object)hudTheme.id());
        this.config.set("players." + String.valueOf(uUID) + ".preferences.map-frame-theme", (Object)true);
        this.save();
    }

    public void setPreference(UUID uUID, String string, boolean bl) {
        this.config.set("players." + String.valueOf(uUID) + ".preferences." + string, (Object)bl);
        this.save();
    }

    public int markerSize(UUID uUID) {
        int n = this.config.getInt("players." + String.valueOf(uUID) + ".preferences.marker-size", 100);
        return Set.of(Integer.valueOf(75), Integer.valueOf(100), Integer.valueOf(125), Integer.valueOf(150)).contains(n) ? n : 100;
    }

    public void setMarkerSize(UUID uUID, int n) {
        if (!Set.of(Integer.valueOf(75), Integer.valueOf(100), Integer.valueOf(125), Integer.valueOf(150)).contains(n)) {
            throw new IllegalArgumentException("Marker size: 75, 100, 125 or 150");
        }
        this.config.set("players." + String.valueOf(uUID) + ".preferences.marker-size", (Object)n);
        this.save();
    }

    public int markerStyle(UUID uUID) {
        int n = switch (this.markerSize(uUID)) {
            case 75 -> 1;
            case 125 -> 2;
            case 150 -> 3;
            default -> 0;
        };
        return n | (this.preference(uUID, "marker-scaling", true) ? 0 : 4);
    }

    public int questLogSize(UUID uUID) {
        int n = this.config.getInt("players." + String.valueOf(uUID) + ".preferences.quest-log-size", 100);
        return Set.of(Integer.valueOf(50), Integer.valueOf(75), Integer.valueOf(100), Integer.valueOf(125)).contains(n) ? n : 100;
    }

    public void setQuestLogSize(UUID uUID, int n) {
        if (!Set.of(Integer.valueOf(50), Integer.valueOf(75), Integer.valueOf(100), Integer.valueOf(125)).contains(n)) {
            throw new IllegalArgumentException("Quest log size: 50, 75, 100 or 125");
        }
        this.config.set("players." + String.valueOf(uUID) + ".preferences.quest-log-size", (Object)n);
        this.save();
    }

    public String partySize(UUID uUID) {
        String string = this.config.getString("players." + String.valueOf(uUID) + ".preferences.party-size", "small");
        return Set.of("small", "medium", "large").contains(string) ? string : "small";
    }

    public void setPartySize(UUID uUID, String string) {
        if (!Set.of("small", "medium", "large").contains(string)) {
            throw new IllegalArgumentException("Party size");
        }
        this.config.set("players." + String.valueOf(uUID) + ".preferences.party-size", (Object)string);
        this.save();
    }

    public String getLayout(UUID uUID) {
        String string = this.config.getString("players." + String.valueOf(uUID) + ".layout");
        return string != null && !string.isBlank() ? string : null;
    }

    public void setLayout(UUID uUID, String string) {
        this.config.set("players." + String.valueOf(uUID) + ".layout", (Object)string);
        this.save();
    }

    public void clearLayout(UUID uUID) {
        this.config.set("players." + String.valueOf(uUID) + ".layout", null);
        this.save();
    }

    public String getPackUrl(UUID uUID) {
        String string = this.config.getString("players." + String.valueOf(uUID) + ".pack-url");
        return string != null && !string.isBlank() ? string : null;
    }

    public void setPackUrl(UUID uUID, String string) {
        if (string != null && !string.isBlank()) {
            this.config.set("players." + String.valueOf(uUID) + ".pack-url", (Object)string);
        } else {
            this.config.set("players." + String.valueOf(uUID) + ".pack-url", null);
        }
        this.save();
    }

    public int getAvatarGlyph(UUID uUID) {
        return this.config.getInt("avatars." + String.valueOf(uUID) + ".glyph", 0);
    }

    public void setAvatarGlyph(UUID uUID, int n) {
        this.config.set("avatars." + String.valueOf(uUID) + ".glyph", (Object)n);
        this.save();
    }

    public int getNextAvatarGlyph() {
        return this.config.getInt("avatars.next-glyph", 0);
    }

    public void setNextAvatarGlyph(int n) {
        this.config.set("avatars.next-glyph", (Object)n);
        this.save();
    }

    public void resetHudStateForAllPlayers() {
        ConfigurationSection configurationSection = this.config.getConfigurationSection("players");
        if (configurationSection != null) {
            boolean bl = false;
            for (String string : configurationSection.getKeys(false)) {
                String string2 = "players." + string;
                if (this.config.contains(string2 + ".layout")) {
                    this.config.set(string2 + ".layout", null);
                    bl = true;
                }
                if (this.config.contains(string2 + ".offset")) {
                    this.config.set(string2 + ".offset", null);
                    bl = true;
                }
                if (!this.config.contains(string2 + ".layers")) continue;
                this.config.set(string2 + ".layers", null);
                bl = true;
            }
            if (bl) {
                this.save();
            }
        }
    }

    private void save() {
        try {
            this.config.save(this.file);
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }
}

