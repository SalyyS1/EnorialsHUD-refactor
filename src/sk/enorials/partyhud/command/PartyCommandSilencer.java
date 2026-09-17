/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.command;

import java.util.Arrays;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import sk.enorials.partyhud.patch.FriendRequestBridge;

public final class PartyCommandSilencer
implements Listener {
    private final Plugin plugin;

    public PartyCommandSilencer(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent playerCommandPreprocessEvent) {
        String[] stringArray;
        String string;
        String string2;
        if (!playerCommandPreprocessEvent.isCancelled() && (string2 = playerCommandPreprocessEvent.getMessage()) != null && PartyCommandSilencer.startsWithParty(string = string2.trim()) && (stringArray = string.substring(1).split("\\s+")).length != 0) {
            Object object;
            String[] stringArray2;
            String[] stringArray3 = stringArray2 = stringArray.length > 1 ? Arrays.copyOfRange(stringArray, 1, stringArray.length) : new String[]{};
            if (stringArray2.length < 3 || !"friend".equalsIgnoreCase(stringArray2[0]) || !(object = stringArray2[1].toLowerCase(Locale.ROOT)).equals("accept") && !((String)object).equals("deny")) {
                object = Bukkit.getPluginCommand((String)"enorialsparty:party");
                if (object != null) {
                    playerCommandPreprocessEvent.setCancelled(true);
                    object.execute((CommandSender)playerCommandPreprocessEvent.getPlayer(), "party", stringArray2);
                }
            } else {
                Player player = playerCommandPreprocessEvent.getPlayer();
                boolean bl = ((String)object).equals("accept") ? FriendRequestBridge.accept(player, stringArray2[2]) : FriendRequestBridge.deny(player, stringArray2[2]);
                playerCommandPreprocessEvent.setCancelled(true);
                if (!bl) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&cNo valid friend request."));
                }
            }
        }
    }

    private static boolean startsWithParty(String string) {
        if (string == null) {
            return false;
        }
        String string2 = string.toLowerCase(Locale.ROOT);
        if (!string2.startsWith("/enohud party")) {
            return false;
        }
        return string2.length() == 6 ? true : Character.isWhitespace(string2.charAt(6));
    }
}

