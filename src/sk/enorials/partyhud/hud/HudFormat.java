/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.hud;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.module.PartyConfig;
import sk.enorials.partyhud.util.PlaceholderResolver;

public final class HudFormat {
    private HudFormat() {
    }

    public static String formatMemberLine(Plugin plugin, Player player, String string, String string2, String string3, String string4, String string5) {
        if (plugin != null && player != null) {
            String string6 = PartyConfig.get(plugin).getString("hud.member-format", "");
            if (string6 != null && !string6.isBlank()) {
                String string7 = string6;
                string7 = string7.replace("{race}", HudFormat.safe(string));
                string7 = string7.replace("{leader}", HudFormat.safe(string2));
                string7 = string7.replace("{color}", HudFormat.safe(string3));
                string7 = string7.replace("{name}", HudFormat.safe(string4));
                string7 = ChatColor.translateAlternateColorCodes((char)'&', (String)string7);
                return PlaceholderResolver.resolve(player, string7);
            }
            return string5;
        }
        return string5;
    }

    private static String safe(String string) {
        return string == null ? "" : string;
    }
}

