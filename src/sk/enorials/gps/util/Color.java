/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.ChatColor
 */
package sk.enorials.gps.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public final class Color {
    private Color() {
    }

    public static String colorize(String string) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(string == null ? "" : string));
    }

    public static String strip(String string) {
        return ChatColor.stripColor((String)(string == null ? "" : string));
    }

    public static String strip(Component component) {
        if (component == null) {
            return "";
        }
        return Color.strip(LegacyComponentSerializer.legacySection().serialize(component));
    }

    public static Component component(String string) {
        return LegacyComponentSerializer.legacySection().deserialize(Color.colorize(string));
    }
}

