/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 */
package sk.enorials.partyhud.util;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import sk.enorials.partyhud.party.Party;

public final class PartyNotify {
    private PartyNotify() {
    }

    public static void memberJoined(Party party, UUID uUID) {
        PartyNotify.broadcast(party, uUID, " joined the party.");
    }

    public static void memberLeft(Party party, UUID uUID) {
        PartyNotify.broadcast(party, uUID, " left the party.");
    }

    private static void broadcast(Party party, UUID uUID, String string) {
        if (party != null && uUID != null) {
            String string2 = PartyNotify.resolveName(uUID);
            String string3 = String.valueOf(ChatColor.GOLD) + "[Party] " + String.valueOf(ChatColor.YELLOW) + string2 + string;
            for (UUID uUID2 : party.getMembers()) {
                Player player;
                if (uUID.equals(uUID2) || (player = Bukkit.getPlayer((UUID)uUID2)) == null || !player.isOnline()) continue;
                player.sendMessage(string3);
            }
        }
    }

    private static String resolveName(UUID uUID) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uUID);
        String string = offlinePlayer.getName();
        return string != null ? string : uUID.toString();
    }
}

