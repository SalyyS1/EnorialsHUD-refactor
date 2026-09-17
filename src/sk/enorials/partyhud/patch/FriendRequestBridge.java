/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 */
package sk.enorials.partyhud.patch;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import sk.enorials.partyhud.friends.FriendRequestChat;
import sk.enorials.partyhud.friends.FriendService;
import sk.enorials.partyhud.patch.FriendRequestStore;

public final class FriendRequestBridge {
    private static volatile FriendRequestStore requests;
    private static volatile FriendService friendService;
    private static volatile Field requestsField;
    private static volatile Field friendServiceField;
    private static volatile Object listenerRef;

    private FriendRequestBridge() {
    }

    public static void init(FriendRequestStore friendRequestStore, FriendService friendService) {
        if (friendRequestStore != null) {
            requests = friendRequestStore;
        }
        if (friendService != null) {
            FriendRequestBridge.friendService = friendService;
        }
    }

    public static void attachListener(Object object) {
        if (object != null) {
            listenerRef = object;
            if (requests == null || friendService == null) {
                FriendRequestBridge.resolveFromListener(object);
            }
        }
    }

    public static Set<UUID> incoming(UUID uUID) {
        FriendRequestStore friendRequestStore = FriendRequestBridge.getRequests();
        return friendRequestStore == null ? Set.of() : friendRequestStore.getIncoming(uUID);
    }

    public static void send(Player player, Player player2) {
        FriendRequestStore friendRequestStore = FriendRequestBridge.getRequests();
        FriendService friendService = FriendRequestBridge.getFriendService();
        if (friendRequestStore == null || friendService == null || player2 == null || player.equals((Object)player2)) {
            return;
        }
        if (friendService.areFriends(player.getUniqueId(), player2.getUniqueId())) {
            player.sendMessage("Already friends.");
            return;
        }
        friendRequestStore.send(player.getUniqueId(), player2.getUniqueId());
        player.sendMessage("Friend request sent to " + player2.getName() + ".");
        FriendRequestChat.sendRequest(player2, player.getName());
    }

    public static boolean accept(Player player, String string) {
        return FriendRequestBridge.handle(player, string, true);
    }

    public static boolean deny(Player player, String string) {
        return FriendRequestBridge.handle(player, string, false);
    }

    private static boolean handle(Player player, String string, boolean bl) {
        if (player != null && string != null && !string.isEmpty()) {
            FriendRequestStore friendRequestStore = FriendRequestBridge.getRequests();
            FriendService friendService = FriendRequestBridge.getFriendService();
            if (friendRequestStore != null && friendService != null) {
                UUID uUID = player.getUniqueId();
                UUID uUID2 = FriendRequestBridge.resolveRequesterId(uUID, string, friendRequestStore);
                if (uUID2 == null) {
                    player.sendMessage(FriendRequestBridge.color("&cNo valid friend request."));
                    return true;
                }
                boolean bl2 = friendRequestStore.remove(uUID, uUID2);
                if (!bl2) {
                    player.sendMessage(FriendRequestBridge.color("&cNo valid friend request."));
                    return true;
                }
                if (bl) {
                    friendService.addFriend(uUID, uUID2);
                    friendService.addFriend(uUID2, uUID);
                    player.sendMessage(FriendRequestBridge.color("&aFriend request accepted."));
                    FriendRequestBridge.notifyRequester(uUID2, player.getName(), true);
                } else {
                    player.sendMessage(FriendRequestBridge.color("&cFriend request declined."));
                    FriendRequestBridge.notifyRequester(uUID2, player.getName(), false);
                }
                return true;
            }
            player.sendMessage(FriendRequestBridge.color("&cFriend requests are not available."));
            return true;
        }
        return false;
    }

    private static void notifyRequester(UUID uUID, String string, boolean bl) {
        Player player;
        if (uUID != null && string != null && (player = Bukkit.getPlayer((UUID)uUID)) != null && player.isOnline()) {
            String string2 = bl ? "&a" + string + " accepted your friend request." : "&c" + string + " declined your friend request.";
            player.sendMessage(FriendRequestBridge.color(string2));
        }
    }

    private static UUID resolveRequesterId(UUID uUID, String string, FriendRequestStore friendRequestStore) {
        if (uUID != null && string != null && !string.isEmpty() && friendRequestStore != null) {
            Player player = Bukkit.getPlayerExact((String)string);
            if (player != null) {
                return player.getUniqueId();
            }
            for (UUID uUID2 : friendRequestStore.getIncoming(uUID)) {
                String string2;
                OfflinePlayer offlinePlayer;
                if (uUID2 == null || (offlinePlayer = Bukkit.getOfflinePlayer((UUID)uUID2)) == null || (string2 = offlinePlayer.getName()) == null || !string2.equalsIgnoreCase(string)) continue;
                return uUID2;
            }
            try {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((String)string);
                return offlinePlayer == null ? null : offlinePlayer.getUniqueId();
            }
            catch (Throwable throwable) {
                return null;
            }
        }
        return null;
    }

    private static FriendRequestStore getRequests() {
        FriendRequestStore friendRequestStore = requests;
        if (friendRequestStore != null) {
            return friendRequestStore;
        }
        Object object = listenerRef;
        if (object != null) {
            FriendRequestBridge.resolveFromListener(object);
            return requests;
        }
        return null;
    }

    private static FriendService getFriendService() {
        FriendService friendService = FriendRequestBridge.friendService;
        if (friendService != null) {
            return friendService;
        }
        Object object = listenerRef;
        if (object != null) {
            FriendRequestBridge.resolveFromListener(object);
            return FriendRequestBridge.friendService;
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void resolveFromListener(Object object) {
        if (object != null) {
            try {
                Object object2;
                Object object3;
                Field field = requestsField;
                Field field2 = friendServiceField;
                if (field == null || field2 == null) {
                    object3 = object.getClass();
                    field = ((Class)object3).getDeclaredField("requests");
                    field.setAccessible(true);
                    field2 = ((Class)object3).getDeclaredField("friendService");
                    field2.setAccessible(true);
                    requestsField = field;
                    friendServiceField = field2;
                }
                if ((object3 = field.get(object)) instanceof FriendRequestStore) {
                    requests = (FriendRequestStore)object3;
                }
                if ((object2 = field2.get(object)) instanceof FriendService) {
                    friendService = (FriendService)object2;
                }
                return;
            }
            catch (Throwable throwable) {
                return;
            }
        }
    }

    private static String color(String string) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)string);
    }
}

