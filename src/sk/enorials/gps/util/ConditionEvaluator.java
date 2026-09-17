/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.gps.util;

import java.lang.reflect.Method;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.integration.BetonQuestBridge;
import sk.enorials.hud.access.HudAccess;

public final class ConditionEvaluator {
    private Method placeholderMethod;
    private boolean placeholderApiChecked;
    private final BetonQuestBridge betonQuest;

    public ConditionEvaluator(Object object) {
        BetonQuestBridge betonQuestBridge;
        if (object instanceof EnorialsGpsPlugin) {
            EnorialsGpsPlugin enorialsGpsPlugin = (EnorialsGpsPlugin)object;
            betonQuestBridge = new BetonQuestBridge(enorialsGpsPlugin);
        } else {
            betonQuestBridge = null;
        }
        this.betonQuest = betonQuestBridge;
    }

    public boolean checkAll(Player player, List<String> list) {
        for (String string : list) {
            if (this.check(player, string)) continue;
            return false;
        }
        return true;
    }

    private boolean check(Player player, String string) {
        if (string == null || string.isBlank()) {
            return true;
        }
        if (string.startsWith("betonquest:")) {
            return this.betonQuest != null && this.betonQuest.test(player, string.substring("betonquest:".length()));
        }
        if (string.startsWith("permission:")) {
            return HudAccess.has((CommandSender)player, string.substring("permission:".length()));
        }
        if (string.startsWith("world:")) {
            return player.getWorld().getName().equalsIgnoreCase(string.substring("world:".length()));
        }
        if (string.startsWith("placeholder_true:")) {
            String string2 = string.substring("placeholder_true:".length());
            String string3 = this.resolvePlaceholder(player, string2);
            return "true".equalsIgnoreCase(string3) || "yes".equalsIgnoreCase(string3) || "1".equals(string3);
        }
        if (string.startsWith("placeholder_equals:")) {
            String string4 = string.substring("placeholder_equals:".length());
            int n = string4.indexOf("==");
            if (n == -1) {
                return false;
            }
            String string5 = string4.substring(0, n);
            String string6 = string4.substring(n + 2);
            return this.resolvePlaceholder(player, string5).equalsIgnoreCase(string6);
        }
        if (string.startsWith("near:")) {
            try {
                String[] stringArray = string.substring("near:".length()).split(",");
                if (stringArray.length < 5) {
                    return false;
                }
                String string7 = stringArray[0];
                double d = Double.parseDouble(stringArray[1]);
                double d2 = Double.parseDouble(stringArray[2]);
                double d3 = Double.parseDouble(stringArray[3]);
                double d4 = Double.parseDouble(stringArray[4]);
                if (Bukkit.getWorld((String)string7) == null) {
                    return false;
                }
                if (!player.getWorld().getName().equalsIgnoreCase(string7)) {
                    return false;
                }
                Location location = new Location(Bukkit.getWorld((String)string7), d, d2, d3);
                return player.getLocation().distance(location) <= d4;
            }
            catch (Exception exception) {
                return false;
            }
        }
        return false;
    }

    private String resolvePlaceholder(Player player, String string) {
        Method method = this.getPlaceholderMethod();
        if (method == null) {
            return string;
        }
        try {
            Object object = method.invoke(null, player, string);
            return String.valueOf(object);
        }
        catch (Exception exception) {
            return string;
        }
    }

    private Method getPlaceholderMethod() {
        if (this.placeholderApiChecked) {
            return this.placeholderMethod;
        }
        this.placeholderApiChecked = true;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (plugin == null || !plugin.isEnabled()) {
            this.placeholderMethod = null;
            return null;
        }
        try {
            Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            this.placeholderMethod = clazz.getMethod("setPlaceholders", Player.class, String.class);
        }
        catch (Exception exception) {
            this.placeholderMethod = null;
        }
        return this.placeholderMethod;
    }
}

