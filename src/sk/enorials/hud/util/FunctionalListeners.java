/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.PlaceholderAPI
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.util;

import java.text.DecimalFormat;
import java.util.Locale;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sk.enorials.hud.compat.HudAttributes;

public final class FunctionalListeners {
    private FunctionalListeners() {
    }

    public static double getCurrent(Player player, String string) {
        return switch (string.toLowerCase(Locale.ROOT)) {
            case "health" -> player.getHealth();
            case "food" -> player.getFoodLevel();
            case "armor" -> {
                if (player.getAttribute(HudAttributes.ARMOR) != null) {
                    yield player.getAttribute(HudAttributes.ARMOR).getValue();
                }
                yield 0.0;
            }
            case "air" -> player.getRemainingAir();
            default -> Double.NaN;
        };
    }

    public static double getMax(Player player, String string) {
        return switch (string.toLowerCase(Locale.ROOT)) {
            case "health" -> {
                if (player.getAttribute(HudAttributes.MAX_HEALTH) != null) {
                    yield player.getAttribute(HudAttributes.MAX_HEALTH).getValue();
                }
                yield 20.0;
            }
            case "food" -> 20.0;
            case "armor" -> 20.0;
            case "air" -> player.getMaximumAir();
            default -> Double.NaN;
        };
    }

    public static String format(double d, String string) {
        return !Double.isFinite(d) ? "0" : new DecimalFormat(string).format(d);
    }

    public static double[] readBarFromPlaceholders(Player player, String string) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return new double[]{Double.NaN, Double.NaN};
        }
        try {
            String string2 = PlaceholderAPI.setPlaceholders((Player)player, (String)("%" + string + "-bar-value%"));
            String string3 = PlaceholderAPI.setPlaceholders((Player)player, (String)("%" + string + "-bar-max%"));
            return new double[]{FunctionalListeners.parseNumber(string2), FunctionalListeners.parseNumber(string3)};
        }
        catch (Throwable throwable) {
            return new double[]{Double.NaN, Double.NaN};
        }
    }

    private static double parseNumber(String string) {
        if (string == null) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(string.replace(",", "."));
        }
        catch (NumberFormatException numberFormatException) {
            return Double.NaN;
        }
    }
}

