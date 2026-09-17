/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.scoreboard.Scoreboard
 */
package sk.enorials.partyhud.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

public final class ScoreboardUtil {
    private static final Map<UUID, Scoreboard> previous = new ConcurrentHashMap<UUID, Scoreboard>();

    private ScoreboardUtil() {
    }

    public static void apply(Player player, Scoreboard scoreboard) {
        if (player != null && scoreboard != null) {
            UUID uUID = player.getUniqueId();
            previous.putIfAbsent(uUID, player.getScoreboard());
            if (player.getScoreboard() != scoreboard) {
                player.setScoreboard(scoreboard);
            }
        }
    }

    public static void restore(Player player) {
        UUID uUID;
        Scoreboard scoreboard;
        if (player != null && (scoreboard = previous.remove(uUID = player.getUniqueId())) != null) {
            player.setScoreboard(scoreboard);
        }
    }

    public static void clear(Player player) {
        if (player != null) {
            previous.remove(player.getUniqueId());
        }
    }

    public static void restoreAll() {
        for (Map.Entry<UUID, Scoreboard> entry : previous.entrySet()) {
            Player player = Bukkit.getPlayer((UUID)entry.getKey());
            if (player == null || !player.isOnline()) continue;
            player.setScoreboard(entry.getValue());
        }
        previous.clear();
    }
}

