/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.module;

import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;

public final class PartyConfig {
    private PartyConfig() {
    }

    public static FileConfiguration get(Plugin plugin) {
        return ((EnorialsHUD)plugin).party().getConfig();
    }

    public static File folder(Plugin plugin) {
        return ((EnorialsHUD)plugin).party().getDataFolder();
    }

    public static void saveConfig(Plugin plugin) {
        ((EnorialsHUD)plugin).party().saveConfig();
    }

    public static void reloadConfig(Plugin plugin) {
        ((EnorialsHUD)plugin).party().reloadConfig();
    }
}

