/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 */
package sk.enorials.gps.command;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.hud.access.HudAccess;

public final class PlacesCommand {
    private PlacesCommand() {
    }

    public static boolean execute(EnorialsGpsPlugin enorialsGpsPlugin, CommandSender commandSender, String[] stringArray) {
        if (!HudAccess.has(commandSender, "enorials.gps.admin")) {
            commandSender.sendMessage("You do not have permission to manage Places.");
            return true;
        }
        try {
            String string;
            File file = new File(enorialsGpsPlugin.getDataFolder(), "configs/poi.yml");
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file);
            String string2 = string = stringArray.length == 0 ? "list" : stringArray[0].toLowerCase(Locale.ROOT);
            if (string.equals("reload")) {
                enorialsGpsPlugin.reloadPlaces();
                commandSender.sendMessage("Places reloaded.");
                return true;
            }
            if (string.equals("list")) {
                ConfigurationSection configurationSection = yamlConfiguration.getConfigurationSection("poi.points");
                commandSender.sendMessage("Places: " + (configurationSection == null ? "none" : String.join((CharSequence)", ", configurationSection.getKeys(false))));
                return true;
            }
            if (stringArray.length < 2 || !stringArray[1].matches("[A-Za-z0-9_-]{1,64}")) {
                throw new IllegalArgumentException("Use a place ID with 1-64 letters, digits, _ or -.");
            }
            String string3 = stringArray[1].toLowerCase(Locale.ROOT);
            String string4 = "poi.points." + string3;
            if (string.equals("remove")) {
                if (!yamlConfiguration.isConfigurationSection(string4)) {
                    throw new IllegalArgumentException("Place not found.");
                }
                yamlConfiguration.set(string4, null);
            } else if (string.equals("add")) {
                String string5;
                Location location;
                if (yamlConfiguration.contains(string4)) {
                    throw new IllegalArgumentException("That place ID already exists. Remove it first or use another ID.");
                }
                int n = 2;
                if (stringArray.length >= 6 && Bukkit.getWorld((String)stringArray[2]) != null) {
                    location = new Location(Bukkit.getWorld((String)stringArray[2]), Double.parseDouble(stringArray[3]), Double.parseDouble(stringArray[4]), Double.parseDouble(stringArray[5]));
                    n = 6;
                } else if (commandSender instanceof Player) {
                    Player player = (Player)commandSender;
                    location = player.getLocation();
                } else {
                    throw new IllegalArgumentException("Console: /enohud gps place add <id> <world> <x> <y> <z> [display name]");
                }
                if (!Double.isFinite(location.getX()) || !Double.isFinite(location.getY()) || !Double.isFinite(location.getZ()) || Math.abs(location.getX()) >= 3.0E7 || Math.abs(location.getZ()) >= 3.0E7 || location.getY() < (double)location.getWorld().getMinHeight() || location.getY() >= (double)location.getWorld().getMaxHeight()) {
                    throw new IllegalArgumentException("Location is outside world bounds.");
                }
                String string6 = string5 = stringArray.length > n ? String.join((CharSequence)" ", Arrays.copyOfRange(stringArray, n, stringArray.length)) : string3;
                if (string5.length() > 64) {
                    throw new IllegalArgumentException("Display name must be at most 64 characters.");
                }
                yamlConfiguration.set(string4 + ".display_name", (Object)string5);
                yamlConfiguration.set(string4 + ".world", (Object)location.getWorld().getName());
                yamlConfiguration.set(string4 + ".x", (Object)location.getX());
                yamlConfiguration.set(string4 + ".y", (Object)location.getY());
                yamlConfiguration.set(string4 + ".z", (Object)location.getZ());
            } else {
                throw new IllegalArgumentException("Use /enohud gps place <add|remove|list|reload>.");
            }
            yamlConfiguration.save(file);
            enorialsGpsPlugin.reloadPlaces();
            commandSender.sendMessage("Place " + string3 + (string.equals("add") ? " added for all players." : " removed."));
        }
        catch (Exception exception) {
            commandSender.sendMessage("Places: " + exception.getMessage());
        }
        return true;
    }
}

