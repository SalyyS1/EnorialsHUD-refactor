/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 */
package sk.enorials.partyhud.friends;

import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import sk.enorials.partyhud.friends.FriendService;

public final class FriendRemoveCommandListener
implements Listener {
    private final FriendService friendService;

    public FriendRemoveCommandListener(FriendService friendService) {
        this.friendService = friendService;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent playerCommandPreprocessEvent) {
        String string;
        String[] stringArray;
        String string2;
        String string3;
        String string4;
        if (!playerCommandPreprocessEvent.isCancelled() && (string4 = playerCommandPreprocessEvent.getMessage()) != null && (string3 = (string2 = string4.trim()).toLowerCase(Locale.ROOT)).startsWith("/enohud party friend ") && (stringArray = string2.split("\\s+")).length >= 3 && ((string = stringArray[2].toLowerCase(Locale.ROOT)).equals("removeconfirm") || string.equals("removedeny"))) {
            playerCommandPreprocessEvent.setCancelled(true);
            Player player = playerCommandPreprocessEvent.getPlayer();
            if (stringArray.length < 4) {
                FriendRemoveCommandListener.send(player, "&cSpecify a friend.");
            } else {
                String string5 = stringArray[3];
                if (string.equals("removedeny")) {
                    FriendRemoveCommandListener.send(player, "&d[Friends] &7Friend removal cancelled.");
                } else if (this.friendService == null) {
                    FriendRemoveCommandListener.send(player, "&cFriends service unavailable.");
                } else {
                    UUID uUID = FriendRemoveCommandListener.resolveTargetId(string5);
                    if (uUID == null) {
                        FriendRemoveCommandListener.send(player, "&cPlayer not found.");
                    } else {
                        boolean bl = this.friendService.removeFriend(player.getUniqueId(), uUID);
                        if (!bl) {
                            FriendRemoveCommandListener.send(player, "&cNo friend to remove.");
                        } else {
                            String string6 = this.friendService.name(uUID);
                            FriendRemoveCommandListener.send(player, "&d[Friends] &7Removed " + string6 + " from friends.");
                        }
                    }
                }
            }
        }
    }

    private static UUID resolveTargetId(String string) {
        if (string != null && !string.isEmpty()) {
            try {
                return UUID.fromString(string);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                Player player = Bukkit.getPlayerExact((String)string);
                if (player != null) {
                    return player.getUniqueId();
                }
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((String)string);
                return offlinePlayer == null ? null : offlinePlayer.getUniqueId();
            }
        }
        return null;
    }

    private static void send(Player player, String string) {
        if (player != null && string != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)string));
        }
    }
}

