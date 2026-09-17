/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 *  org.bukkit.scoreboard.Scoreboard
 *  org.bukkit.scoreboard.Team
 */
package sk.enorials.partyhud.hud;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class HudLineCache {
    private static final Map<UUID, Integer> LAST_LINES = new ConcurrentHashMap<UUID, Integer>();
    private static final ChatColor[] ENTRY_COLORS = new ChatColor[]{ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA, ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY, ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA, ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE};

    private HudLineCache() {
    }

    public static Team ensureTeam(Scoreboard scoreboard, String string) {
        if (scoreboard != null && string != null) {
            Team team = scoreboard.getTeam(string);
            if (team != null) {
                return team;
            }
            try {
                return scoreboard.registerNewTeam(string);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                return scoreboard.getTeam(string);
            }
        }
        return null;
    }

    public static void trim(Player player, Scoreboard scoreboard, int n) {
        if (player != null && scoreboard != null) {
            UUID uUID;
            int n2;
            if (n < 0) {
                n = 0;
            }
            if ((n2 = LAST_LINES.getOrDefault(uUID = player.getUniqueId(), 0).intValue()) > n) {
                for (int i = n; i < n2; ++i) {
                    scoreboard.resetScores(HudLineCache.uniqueEntry(i));
                }
            }
            LAST_LINES.put(uUID, n);
        }
    }

    public static void clear(Player player) {
        if (player != null) {
            LAST_LINES.remove(player.getUniqueId());
        }
    }

    public static void clearAll() {
        LAST_LINES.clear();
    }

    private static String uniqueEntry(int n) {
        int n2 = n % ENTRY_COLORS.length;
        int n3 = n / ENTRY_COLORS.length % ENTRY_COLORS.length;
        return ENTRY_COLORS[n3].toString() + ENTRY_COLORS[n2].toString() + String.valueOf(ChatColor.RESET);
    }
}

