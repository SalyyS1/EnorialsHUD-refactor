/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 */
package sk.enorials.partyhud.friends;

import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import sk.enorials.partyhud.patch.FriendRequestBridge;

public final class FriendRequestCommandListener
implements Listener {
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent playerCommandPreprocessEvent) {
        String string;
        String[] stringArray;
        String string2;
        String string3;
        String string4;
        if (!playerCommandPreprocessEvent.isCancelled() && (string4 = playerCommandPreprocessEvent.getMessage()) != null && (string3 = (string2 = string4.trim()).toLowerCase(Locale.ROOT)).startsWith("/enohud party friend ") && (stringArray = string2.split("\\s+")).length >= 4 && ((string = stringArray[2].toLowerCase(Locale.ROOT)).equals("accept") || string.equals("deny"))) {
            boolean bl;
            Player player = playerCommandPreprocessEvent.getPlayer();
            String string5 = stringArray[3];
            boolean bl2 = bl = string.equals("accept") ? FriendRequestBridge.accept(player, string5) : FriendRequestBridge.deny(player, string5);
            if (bl) {
                playerCommandPreprocessEvent.setCancelled(true);
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&cNo valid friend request."));
                playerCommandPreprocessEvent.setCancelled(true);
            }
        }
    }
}

