/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 */
package sk.enorials.partyhud.friends;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import sk.enorials.partyhud.friends.FriendService;

public final class FriendPresenceListener
implements Listener {
    private final FriendService friendService;

    public FriendPresenceListener(FriendService friendService) {
        this.friendService = friendService;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        this.notifyFriends(playerJoinEvent.getPlayer(), true);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        this.notifyFriends(playerQuitEvent.getPlayer(), false);
    }

    private void notifyFriends(Player player, boolean bl) {
        Set<UUID> set;
        if (player != null && this.friendService != null && (set = this.friendService.getFriends(player.getUniqueId())) != null && !set.isEmpty()) {
            Object object2;
            HashMap<UUID, Player> hashMap = new HashMap<UUID, Player>();
            for (Object object2 : Bukkit.getOnlinePlayers()) {
                hashMap.put(object2.getUniqueId(), (Player)object2);
            }
            String string = player.getName();
            object2 = bl ? String.valueOf(ChatColor.AQUA) + "[FRIENDS] " + String.valueOf(ChatColor.WHITE) + (String)string + String.valueOf(ChatColor.GREEN) + " joined the server." : String.valueOf(ChatColor.AQUA) + "[FRIENDS] " + String.valueOf(ChatColor.WHITE) + (String)string + String.valueOf(ChatColor.GRAY) + " left the server.";
            for (UUID uUID : set) {
                Player player2;
                if (uUID == null || uUID.equals(player.getUniqueId()) || (player2 = (Player)hashMap.get(uUID)) == null) continue;
                player2.sendMessage((String)object2);
            }
        }
    }
}

