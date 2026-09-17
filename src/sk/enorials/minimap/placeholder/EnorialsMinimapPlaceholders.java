/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package sk.enorials.minimap.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.enorials.minimap.EnorialsMinimap;

public final class EnorialsMinimapPlaceholders
extends PlaceholderExpansion {
    private final EnorialsMinimap plugin;

    public EnorialsMinimapPlaceholders(EnorialsMinimap enorialsMinimap) {
        this.plugin = enorialsMinimap;
    }

    @NotNull
    public String getIdentifier() {
        return "enomap";
    }

    @NotNull
    public String getAuthor() {
        return "Enorials";
    }

    @NotNull
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public boolean persist() {
        return true;
    }

    @Nullable
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String string) {
        if (offlinePlayer == null) {
            return "";
        }
        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return "";
        }
        int n = player.getLocation().getBlockX();
        int n2 = player.getLocation().getBlockY();
        int n3 = player.getLocation().getBlockZ();
        return switch (string.toLowerCase()) {
            case "x" -> Integer.toString(n);
            case "y" -> Integer.toString(n2);
            case "z" -> Integer.toString(n3);
            case "xyz" -> "X:" + n + " Y:" + n2 + " Z:" + n3;
            default -> null;
        };
    }
}

