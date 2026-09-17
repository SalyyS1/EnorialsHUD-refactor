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
import sk.enorials.minimap.map.Minimap;

public final class EnomapExpansion
extends PlaceholderExpansion {
    private final EnorialsMinimap plugin;

    public EnomapExpansion(EnorialsMinimap enorialsMinimap) {
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
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        String string2 = string.toLowerCase();
        if ("size".equals(string2)) {
            return minimap != null ? minimap.size().name() : "";
        }
        if ("shape".equals(string2)) {
            return minimap != null ? minimap.shape().name() : "";
        }
        if ("x".equals(string2)) {
            return Integer.toString(player.getLocation().getBlockX());
        }
        if ("y".equals(string2)) {
            return Integer.toString(player.getLocation().getBlockY());
        }
        if ("z".equals(string2)) {
            return Integer.toString(player.getLocation().getBlockZ());
        }
        return "xyz".equals(string2) ? "X:" + player.getLocation().getBlockX() + " Y:" + player.getLocation().getBlockY() + " Z:" + player.getLocation().getBlockZ() : null;
    }
}

