/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package sk.enorials.partyhud.util;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlaceholderResolver {
    private static boolean checked = false;
    private static Method setPlaceholdersMethod = null;

    private PlaceholderResolver() {
    }

    private static void check() {
        if (!checked) {
            checked = true;
            try {
                if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    return;
                }
                Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                setPlaceholdersMethod = clazz.getMethod("setPlaceholders", Player.class, String.class);
            }
            catch (Throwable throwable) {
                setPlaceholdersMethod = null;
            }
        }
    }

    public static String resolve(Player player, String string) {
        if (string == null) {
            return "";
        }
        PlaceholderResolver.check();
        if (setPlaceholdersMethod == null) {
            return string;
        }
        try {
            Object object = setPlaceholdersMethod.invoke(null, player, string);
            return object != null ? String.valueOf(object) : string;
        }
        catch (Throwable throwable) {
            return string;
        }
    }
}

