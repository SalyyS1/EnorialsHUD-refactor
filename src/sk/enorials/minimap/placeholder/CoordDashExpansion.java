/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package sk.enorials.minimap.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.enorials.minimap.EnorialsMinimap;

public final class CoordDashExpansion
extends PlaceholderExpansion {
    private final EnorialsMinimap plugin;
    private final Axis axis;
    private final String identifier;

    public CoordDashExpansion(EnorialsMinimap enorialsMinimap, Axis axis) {
        this.plugin = enorialsMinimap;
        this.axis = axis;
        this.identifier = "coord-" + axis.name().toLowerCase();
    }

    @NotNull
    public String getIdentifier() {
        return this.identifier;
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
        Location location = player.getLocation();
        return switch (this.axis.ordinal()) {
            default -> throw new IncompatibleClassChangeError();
            case 0 -> Integer.toString(location.getBlockX());
            case 1 -> Integer.toString(location.getBlockY());
            case 2 -> Integer.toString(location.getBlockZ());
        };
    }

    public static enum Axis {
        X,
        Y,
        Z;

    }
}

