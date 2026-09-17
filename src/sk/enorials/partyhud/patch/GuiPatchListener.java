/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryOpenEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.patch;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.partyhud.friends.FriendRequestChat;
import sk.enorials.partyhud.friends.FriendService;
import sk.enorials.partyhud.invite.InviteService;
import sk.enorials.partyhud.locator.LocatorService;
import sk.enorials.partyhud.party.Party;
import sk.enorials.partyhud.party.PartyService;
import sk.enorials.partyhud.patch.FriendRequestBridge;
import sk.enorials.partyhud.patch.FriendRequestStore;
import sk.enorials.partyhud.patch.PatchHolder;
import sk.enorials.partyhud.patch.PatchMenu;
import sk.enorials.partyhud.ui.GuiHolder;
import sk.enorials.partyhud.ui.GuiService;
import sk.enorials.partyhud.ui.GuiType;
import sk.enorials.partyhud.util.ItemUtil;

public final class GuiPatchListener
implements Listener {
    private static final int CMD_CREATE_PRIVATE_W3 = 13009;
    private static final int CMD_CREATE_PUBLIC_W3 = 13010;
    private final FriendRequestStore requests = new FriendRequestStore();
    private final Map<UUID, Map<Integer, UUID>> inviteFriendSlots = new ConcurrentHashMap<UUID, Map<Integer, UUID>>();
    private final Plugin plugin;
    private final GuiService guiService;
    private final FriendService friendService;
    private final PartyService partyService;
    private final InviteService inviteService;
    private final Set<UUID> swappingMyParty = Collections.newSetFromMap(new ConcurrentHashMap());
    private final Set<UUID> redirectingSelectSource = Collections.newSetFromMap(new ConcurrentHashMap());

    public GuiPatchListener(Plugin plugin, GuiService guiService, FriendService friendService) {
        this.plugin = plugin;
        this.guiService = guiService;
        this.friendService = friendService;
        FriendRequestBridge.init(this.requests, this.friendService);
        FriendRequestBridge.attachListener(this);
        this.partyService = GuiPatchListener.reflectField(plugin, "partyService", PartyService.class);
        this.inviteService = GuiPatchListener.reflectField(plugin, "inviteService", InviteService.class);
    }

    private static <T> T reflectField(Object object, String string, Class<T> clazz) {
        try {
            Object object2;
            if (object instanceof EnorialsHUD) {
                object2 = (EnorialsHUD)((Object)object);
                object = ((EnorialsHUD)((Object)object2)).party();
            }
            object2 = object.getClass().getDeclaredField(string);
            ((Field)object2).setAccessible(true);
            Object object3 = ((Field)object2).get(object);
            return clazz.cast(object3);
        }
        catch (Throwable throwable) {
            throw new IllegalStateException("Cannot access field " + string + " on " + object.getClass().getName(), throwable);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onGuiOpen(InventoryOpenEvent inventoryOpenEvent) {
        Inventory inventory = inventoryOpenEvent.getInventory();
        InventoryHolder inventoryHolder = inventory.getHolder();
        Object object = inventoryOpenEvent.getPlayer();
        if (object instanceof Player) {
            Player player = (Player)object;
            if (inventoryHolder instanceof GuiHolder && (object = (GuiHolder)inventoryHolder).getType() == GuiType.SELECT_SOURCE) {
                UUID uUID = player.getUniqueId();
                if (this.redirectingSelectSource.add(uUID)) {
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        try {
                            this.openMyPartyInviteFrom(player);
                        }
                        finally {
                            this.redirectingSelectSource.remove(uUID);
                        }
                    });
                }
            } else if (inventoryHolder instanceof GuiHolder && (object = (GuiHolder)inventoryHolder).getType() == GuiType.MY_PARTY) {
                UUID uUID = player.getUniqueId();
                if (this.swappingMyParty.add(uUID)) {
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        try {
                            this.openMyPartyV2(player);
                        }
                        finally {
                            this.swappingMyParty.remove(uUID);
                        }
                    });
                }
            } else {
                if (inventoryHolder instanceof GuiHolder) {
                    GuiHolder guiHolder = (GuiHolder)inventoryHolder;
                    if (guiHolder.getType() == GuiType.INVITES) {
                        this.injectFriendRequests(player, inventory);
                    }
                    if (guiHolder.getType() == GuiType.FRIENDS) {
                        Bukkit.getScheduler().runTask(this.plugin, () -> GuiPatchListener.patchAddFriendDescription(inventory));
                    }
                }
                if (inventoryHolder instanceof GuiHolder && (object = (GuiHolder)inventoryHolder).getType() == GuiType.CREATE_MODE) {
                    Bukkit.getScheduler().runTask(this.plugin, () -> this.patchCreatePartyMenu(inventory));
                }
                if (inventoryHolder instanceof GuiHolder && (object = (GuiHolder)inventoryHolder).getType() == GuiType.OPEN_PARTIES) {
                    Bukkit.getScheduler().runTask(this.plugin, () -> this.patchPublicPartiesMenu(inventory));
                }
            }
        }
    }

    private static void patchAddFriendDescription(Inventory inventory) {
        for (int n : new int[]{48, 49, 50}) {
            ItemMeta itemMeta;
            ItemStack itemStack = inventory.getItem(n);
            if (itemStack == null || (itemMeta = itemStack.getItemMeta()) == null || !itemMeta.hasDisplayName() || !GuiPatchListener.stripColor(itemMeta.getDisplayName()).equalsIgnoreCase("Add Friend")) continue;
            itemMeta.setLore(Arrays.asList("\u00a77Click to open a menu of online players, ", "\u00a77and add them by clicking their head.         "));
            itemStack.setItemMeta(itemMeta);
            inventory.setItem(n, itemStack);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onGuiClose(InventoryCloseEvent inventoryCloseEvent) {
        HumanEntity humanEntity;
        InventoryHolder inventoryHolder = inventoryCloseEvent.getInventory().getHolder();
        if (inventoryHolder instanceof GuiHolder && (humanEntity = inventoryCloseEvent.getPlayer()) instanceof Player) {
            Player player = (Player)humanEntity;
            this.inviteFriendSlots.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPreClick(InventoryClickEvent inventoryClickEvent) {
        InventoryHolder inventoryHolder = inventoryClickEvent.getInventory().getHolder();
        if (inventoryHolder instanceof GuiHolder) {
            GuiHolder guiHolder = (GuiHolder)inventoryHolder;
            HumanEntity humanEntity = inventoryClickEvent.getWhoClicked();
            if (humanEntity instanceof Player) {
                Map<Integer, UUID> map;
                inventoryHolder = (Player)humanEntity;
                int n = inventoryClickEvent.getSlot();
                if (guiHolder.getType() == GuiType.INVITES && (map = this.inviteFriendSlots.get(inventoryHolder.getUniqueId())) != null && map.containsKey(n)) {
                    inventoryClickEvent.setCurrentItem(null);
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onGuiClick(InventoryClickEvent inventoryClickEvent) {
        Inventory inventory = inventoryClickEvent.getInventory();
        InventoryHolder inventoryHolder = inventory.getHolder();
        HumanEntity humanEntity = inventoryClickEvent.getWhoClicked();
        if (humanEntity instanceof Player) {
            Player player = (Player)humanEntity;
            if (inventoryClickEvent.getClickedInventory() != null && inventoryClickEvent.getClickedInventory().equals((Object)inventoryClickEvent.getView().getTopInventory())) {
                int n = inventoryClickEvent.getSlot();
                ClickType clickType = inventoryClickEvent.getClick();
                ItemStack itemStack = inventoryClickEvent.getCurrentItem();
                if (inventoryHolder instanceof PatchHolder) {
                    PatchHolder patchHolder = (PatchHolder)inventoryHolder;
                    inventoryClickEvent.setCancelled(true);
                    this.handlePatchMenuClick(player, patchHolder, n, clickType, itemStack);
                } else if (inventoryHolder instanceof GuiHolder) {
                    GuiHolder guiHolder = (GuiHolder)inventoryHolder;
                    if (guiHolder.getType() == GuiType.SELECT_SOURCE) {
                        if (n >= 10 && n <= 12) {
                            inventoryClickEvent.setCancelled(true);
                            this.openInviteFriendsFromSelectSource(player, 0);
                            return;
                        }
                        if (n >= 14 && n <= 16) {
                            inventoryClickEvent.setCancelled(true);
                            this.openInviteOnlineFromSelectSource(player, 0);
                            return;
                        }
                    }
                    if (guiHolder.getType() == GuiType.CREATE_MODE) {
                        if (n == 10 || n == 12) {
                            inventoryClickEvent.setCancelled(true);
                            this.guiService.handleClick(player, guiHolder, 11, clickType, inventory.getItem(11));
                            return;
                        }
                        if (n == 14 || n == 16) {
                            inventoryClickEvent.setCancelled(true);
                            this.guiService.handleClick(player, guiHolder, 15, clickType, inventory.getItem(15));
                            return;
                        }
                    }
                    if (guiHolder.getType() == GuiType.FRIENDS && (n == 48 || n == 49 || n == 50)) {
                        inventoryClickEvent.setCancelled(true);
                        this.openAddFriendMenu(player, guiHolder.getPage(), 0);
                    } else if (guiHolder.getType() == GuiType.INVITES) {
                        UUID uUID;
                        Map<Integer, UUID> map = this.inviteFriendSlots.get(player.getUniqueId());
                        if (map != null && (uUID = map.get(n)) != null) {
                            inventoryClickEvent.setCancelled(true);
                            if (clickType.isRightClick()) {
                                this.requests.remove(player.getUniqueId(), uUID);
                                player.sendMessage(GuiPatchListener.color("&cFriend request declined."));
                            } else if (clickType.isLeftClick() && this.requests.remove(player.getUniqueId(), uUID)) {
                                this.friendService.addFriend(player.getUniqueId(), uUID);
                                this.friendService.addFriend(uUID, player.getUniqueId());
                                player.sendMessage(GuiPatchListener.color("&aFriend request accepted."));
                                Player player2 = Bukkit.getPlayer((UUID)uUID);
                                if (player2 != null && player2.isOnline()) {
                                    player2.sendMessage(GuiPatchListener.color("&a" + player.getName() + " accepted your friend request."));
                                }
                            }
                            Bukkit.getScheduler().runTask(this.plugin, () -> this.guiService.openInvites(player));
                        }
                    } else if (guiHolder.getType() != GuiType.ONLINE) {
                        // empty if block
                    }
                }
            }
        }
    }

    private void handlePatchMenuClick(Player player, PatchHolder patchHolder, int n, ClickType clickType, ItemStack itemStack) {
        Party party;
        Optional<Party> optional;
        UUID uUID;
        UUID uUID2;
        if (this.isBackButton(itemStack)) {
            switch (patchHolder.getMenu()) {
                case MY_PARTY_V2: {
                    this.guiService.openMain(player);
                    return;
                }
                case MY_PARTY_INVITE_FROM: {
                    this.openMyPartyV2(player);
                    return;
                }
                case MY_PARTY_INVITE_ONLINE: 
                case MY_PARTY_INVITE_FRIENDS: {
                    this.openMyPartyInviteFrom(player);
                    return;
                }
                case INVITE_ONLINE_FROM_SELECT_SOURCE: 
                case INVITE_FRIENDS_FROM_SELECT_SOURCE: {
                    this.guiService.openSelectSource(player);
                    return;
                }
                case FRIENDS_ADD_FRIEND: {
                    this.guiService.openFriends(player, 0);
                    return;
                }
            }
        }
        if (patchHolder.getMenu() == PatchMenu.MY_PARTY_V2 && itemStack != null && itemStack.getType() == Material.PLAYER_HEAD && (uUID2 = GuiPatchListener.getHeadOwner(itemStack)) != null && !uUID2.equals(uUID = player.getUniqueId()) && !(optional = this.partyService.getPartyOf(uUID)).isEmpty() && (party = optional.get()).getMembers().contains(uUID2)) {
            if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
                LocatorService locatorService = GuiPatchListener.reflectField(this.plugin, "locatorService", LocatorService.class);
                locatorService.toggleTracking(player, uUID2);
                Optional<UUID> optional2 = locatorService.getTrackedTarget(uUID);
                String string = optional2.isPresent() ? GuiPatchListener.color("&aTracking: &f" + GuiPatchListener.nameOf(optional2.get())) : GuiPatchListener.color("&7Tracking disabled.");
                player.sendMessage(string);
                return;
            }
            if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
                if (!party.getLeader().equals(uUID)) {
                    player.sendMessage(GuiPatchListener.color("&cOnly the leader can promote members."));
                    return;
                }
                party.setLeader(uUID2);
                String string = GuiPatchListener.color("&6[Party] &e" + GuiPatchListener.nameOf(uUID2) + " &7is now the leader.");
                for (UUID uUID3 : party.getMembers()) {
                    Player player2 = Bukkit.getPlayer((UUID)uUID3);
                    if (player2 == null || !player2.isOnline()) continue;
                    player2.sendMessage(string);
                }
                return;
            }
        }
        switch (patchHolder.getMenu()) {
            case MY_PARTY_V2: {
                this.handleMyPartyV2Click(player, patchHolder, n, itemStack);
                break;
            }
            case MY_PARTY_INVITE_FROM: {
                this.handleMyPartyInviteFromClick(player, patchHolder, n, itemStack);
                break;
            }
            case MY_PARTY_INVITE_ONLINE: {
                this.handleInviteOnlineClick(player, patchHolder, n, itemStack);
                break;
            }
            case MY_PARTY_INVITE_FRIENDS: {
                this.handleInviteFriendsClick(player, patchHolder, n, itemStack);
                break;
            }
            case INVITE_ONLINE_FROM_SELECT_SOURCE: {
                this.handleInviteOnlineClick(player, patchHolder, n, itemStack);
                break;
            }
            case INVITE_FRIENDS_FROM_SELECT_SOURCE: {
                this.handleInviteFriendsClick(player, patchHolder, n, itemStack);
                break;
            }
            case FRIENDS_ADD_FRIEND: {
                this.handleAddFriendClick(player, patchHolder, n, itemStack);
            }
        }
    }

    private void handleAddFriendClick(Player player, PatchHolder patchHolder, int n, ItemStack itemStack) {
        UUID uUID;
        UUID uUID2;
        ItemMeta itemMeta;
        if (itemStack != null && itemStack.hasItemMeta() && (itemMeta = itemStack.getItemMeta()) != null && itemMeta.hasCustomModelData()) {
            int n2 = itemMeta.getCustomModelData();
            if (n2 == 1004) {
                this.openAddFriendMenu(player, 0, Math.max(0, patchHolder.getPage() - 1));
                return;
            }
            if (n2 == 1005) {
                this.openAddFriendMenu(player, 0, patchHolder.getPage() + 1);
                return;
            }
        }
        if ((uUID2 = GuiPatchListener.getHeadOwner(itemStack)) != null && !(uUID = player.getUniqueId()).equals(uUID2)) {
            Set<UUID> set = this.friendService.getFriends(uUID);
            if (set != null && set.contains(uUID2)) {
                player.sendMessage(GuiPatchListener.color("&eYou are already friends with &f" + GuiPatchListener.nameOf(uUID2) + "&e."));
            } else {
                this.requests.send(uUID, uUID2);
                player.sendMessage(GuiPatchListener.color("&aFriend request sent to &f" + GuiPatchListener.nameOf(uUID2) + "&a."));
                Player player2 = Bukkit.getPlayer((UUID)uUID2);
                if (player2 != null && player2.isOnline()) {
                    player2.sendMessage(GuiPatchListener.color("&e" + player.getName() + " &asent you a friend request. Open &fInvites &ato accept/decline."));
                    FriendRequestChat.sendRequest(player2, player.getName());
                }
            }
        }
    }

    private void handleMyPartyV2Click(Player player, PatchHolder patchHolder, int n, ItemStack itemStack) {
        if (n == 0 || n == 1) {
            this.guiService.openVoteKick(player);
        } else if (n >= 48 && n <= 50) {
            this.openMyPartyInviteFrom(player);
        } else if (n == 53 || n == 52) {
            Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
            if (optional.isEmpty()) {
                player.sendMessage(GuiPatchListener.color("&cYou're not in a party."));
                this.guiService.openMain(player);
            } else {
                this.partyService.leaveParty(player.getUniqueId(), true);
                player.sendMessage(GuiPatchListener.color("&cYou left the party."));
                this.guiService.openMain(player);
            }
        }
    }

    private void handleMyPartyInviteFromClick(Player player, PatchHolder patchHolder, int n, ItemStack itemStack) {
        if (n >= 10 && n <= 12) {
            this.openMyPartyInviteFriends(player, 0);
        } else if (n >= 14 && n <= 16) {
            this.openMyPartyInviteOnline(player, 0);
        }
    }

    private void handleInviteOnlineClick(Player player, PatchHolder patchHolder, int n, ItemStack itemStack) {
        UUID uUID;
        if (itemStack != null && itemStack.getType() != Material.AIR && (uUID = GuiPatchListener.getHeadOwner(itemStack)) != null && !uUID.equals(player.getUniqueId())) {
            Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
            if (optional.isEmpty()) {
                player.sendMessage(GuiPatchListener.color("&cYou're not in a party."));
            } else {
                Party party = optional.get();
                if (party.isMember(uUID)) {
                    player.sendMessage(GuiPatchListener.color("&eThat player is already in your party."));
                } else {
                    Player player2 = Bukkit.getPlayer((UUID)uUID);
                    if (player2 != null && player2.isOnline()) {
                        this.inviteService.sendInvite(player, player2);
                        player.sendMessage(GuiPatchListener.color("&aParty invite sent to &f" + player2.getName() + "&a."));
                    } else {
                        player.sendMessage(GuiPatchListener.color("&cThat player is not online."));
                    }
                }
            }
        }
    }

    private void handleInviteFriendsClick(Player player, PatchHolder patchHolder, int n, ItemStack itemStack) {
        this.handleInviteOnlineClick(player, patchHolder, n, itemStack);
    }

    private void openInviteOnlineFromSelectSource(Player player, int n) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new PatchHolder(PatchMenu.INVITE_ONLINE_FROM_SELECT_SOURCE, n), (int)54, (String)"Invite From: Online");
        this.fillBackground(inventory);
        this.renderOnlinePlayers(inventory, player, n, false);
        this.placeBack(inventory, 49);
        player.openInventory(inventory);
    }

    private void openInviteFriendsFromSelectSource(Player player, int n) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new PatchHolder(PatchMenu.INVITE_FRIENDS_FROM_SELECT_SOURCE, n), (int)54, (String)"Invite From: Friends");
        this.fillBackground(inventory);
        this.renderOnlinePlayers(inventory, player, n, true);
        this.placeBack(inventory, 49);
        player.openInventory(inventory);
    }

    private void openMyPartyV2(Player player) {
        Serializable serializable;
        Party party;
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new PatchHolder(PatchMenu.MY_PARTY_V2, 0), (int)54, (String)"My Party");
        this.fillBackground(inventory);
        this.clearSlots(inventory, new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43, 48, 49, 50, 1, 53});
        inventory.setItem(8, null);
        Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
        if (optional.isPresent()) {
            party = optional.get();
            inventory.setItem(7, ItemUtil.icon(party.isOpen() ? 13013 : 13014, party.isOpen() ? "\u00a7aPUBLIC" : "\u00a7cPRIVATE", "\u00a77Party type"));
            inventory.setItem(4, ItemUtil.icon(1011, "\u00a7aParty Members", "\u00a77Members: \u00a7f" + party.getMembers().size()));
            serializable = party.getLeader();
            inventory.setItem(6, this.headItem((UUID)serializable, "\u00a7eLeader", "\u00a77" + GuiPatchListener.nameOf((UUID)serializable)));
        } else {
            inventory.setItem(4, ItemUtil.icon(1011, "\u00a7cNo Party", "\u00a77You are not in a party"));
        }
        inventory.setItem(0, ItemUtil.icon(13011, "\u00a7cVOTE KICK", "\u00a77Vote to kick a member"));
        inventory.setItem(52, ItemUtil.icon(13012, "\u00a7cLEAVE", "\u00a77Leave your party"));
        inventory.setItem(49, ItemUtil.icon(13008, "\u00a7bInvite", "\u00a77Invite players to your party"));
        this.placeBack(inventory, 45);
        if (optional.isPresent()) {
            party = optional.get();
            serializable = new ArrayList<UUID>(party.getMembers());
            serializable.sort(Comparator.comparing(GuiPatchListener::nameOf, String.CASE_INSENSITIVE_ORDER));
            int[] nArray = this.memberGridSlots();
            int n = 0;
            Iterator iterator = serializable.iterator();
            while (iterator.hasNext()) {
                UUID uUID = (UUID)iterator.next();
                if (n >= nArray.length) break;
                inventory.setItem(nArray[n++], this.headItem(uUID, "\u00a7f" + GuiPatchListener.nameOf(uUID), "\u00a77Member"));
            }
        }
        player.openInventory(inventory);
    }

    private void openMyPartyInviteFrom(Player player) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new PatchHolder(PatchMenu.MY_PARTY_INVITE_FROM, 0), (int)54, (String)"My Party: Invite");
        this.fillBackground(inventory);
        inventory.setItem(11, ItemUtil.icon(13005, "\u00a7aFriends", "\u00a77Invite online friends"));
        inventory.setItem(15, ItemUtil.icon(13006, "\u00a7bOnline", "\u00a77Invite online players"));
        this.placeBack(inventory, 45);
        player.openInventory(inventory);
    }

    private void openMyPartyInviteOnline(Player player, int n) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new PatchHolder(PatchMenu.MY_PARTY_INVITE_ONLINE, n), (int)54, (String)"My Party: Invite Online");
        this.fillBackground(inventory);
        this.renderOnlinePlayers(inventory, player, n, false);
        this.placeBack(inventory, 49);
        player.openInventory(inventory);
    }

    private void openMyPartyInviteFriends(Player player, int n) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new PatchHolder(PatchMenu.MY_PARTY_INVITE_FRIENDS, n), (int)54, (String)"My Party: Invite Friends");
        this.fillBackground(inventory);
        this.renderOnlinePlayers(inventory, player, n, true);
        this.placeBack(inventory, 49);
        player.openInventory(inventory);
    }

    private void patchCreatePartyMenu(Inventory inventory) {
        inventory.setItem(11, ItemUtil.icon(13009, "\u00a7cPrivate Party", "\u00a77Invite-only party"));
        inventory.setItem(15, ItemUtil.icon(13010, "\u00a7aPublic Party", "\u00a77Anyone can join"));
    }

    private void patchPublicPartiesMenu(Inventory inventory) {
    }

    private void openAddFriendMenu(Player player, int n, int n2) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new PatchHolder(PatchMenu.FRIENDS_ADD_FRIEND, n2), (int)54, (String)"Friends: Add Friend");
        this.fillBackground(inventory);
        this.renderOnlinePlayersForFriendRequests(inventory, player, n2);
        this.placeBack(inventory, 45);
        player.openInventory(inventory);
    }

    private void renderOnlinePlayersForFriendRequests(Inventory inventory, Player player2, int n) {
        int n2;
        UUID uUID = player2.getUniqueId();
        Set<UUID> set = this.friendService.getFriends(uUID);
        Set<UUID> set2 = set != null ? set : Collections.emptySet();
        ArrayList<Player> arrayList = new ArrayList<Player>(Bukkit.getOnlinePlayers());
        arrayList.removeIf(player -> player.getUniqueId().equals(uUID));
        arrayList.removeIf(player -> set2.contains(player.getUniqueId()));
        arrayList.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        int n3 = 45;
        int n4 = Math.max(0, n) * n3;
        int n5 = Math.min(arrayList.size(), n4 + n3);
        for (n2 = 0; n2 < 45; ++n2) {
            inventory.setItem(n2, null);
        }
        n2 = 0;
        for (int i = n4; i < n5; ++i) {
            Player player3 = (Player)arrayList.get(i);
            inventory.setItem(n2++, this.headItem(player3.getUniqueId(), "&f" + player3.getName(), "&aClick: send friend request"));
        }
        if (n4 > 0) {
            inventory.setItem(45, ItemUtil.icon(1004, "\u00a77Previous", new String[0]));
        }
        if (n5 < arrayList.size()) {
            inventory.setItem(53, ItemUtil.icon(1005, "\u00a77Next", new String[0]));
        }
    }

    private void fillBackground(Inventory inventory) {
        ItemStack itemStack = ItemUtil.bgTile(2001);
        for (int i = 0; i < inventory.getSize(); ++i) {
            inventory.setItem(i, itemStack);
        }
    }

    private void clearSlots(Inventory inventory, int[] nArray) {
        for (int n : nArray) {
            if (n < 0 || n >= inventory.getSize()) continue;
            inventory.setItem(n, null);
        }
    }

    private void placeBack(Inventory inventory, int n) {
        inventory.setItem(n, ItemUtil.icon(1006, "\u00a77Back", new String[0]));
    }

    private boolean isBackButton(ItemStack itemStack) {
        if (itemStack != null && itemStack.getType() != Material.AIR) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            return itemMeta != null && itemMeta.hasCustomModelData() && itemMeta.getCustomModelData() == 1006;
        }
        return false;
    }

    private int[] memberGridSlots() {
        return new int[]{19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    }

    private void renderOnlinePlayers(Inventory inventory, Player player3, int n, boolean bl) {
        int n2;
        Optional<Party> optional = this.partyService.getPartyOf(player3.getUniqueId());
        HashSet<UUID> hashSet = new HashSet<UUID>();
        if (optional.isPresent()) {
            hashSet.addAll(optional.get().getMembers());
        }
        ArrayList<Player> arrayList = new ArrayList<Player>();
        if (bl) {
            for (UUID uUID : this.friendService.getFriends(player3.getUniqueId())) {
                Player player4 = Bukkit.getPlayer((UUID)uUID);
                if (player4 == null || !player4.isOnline()) continue;
                arrayList.add(player4);
            }
        } else {
            arrayList.addAll(Bukkit.getOnlinePlayers());
        }
        arrayList.removeIf(player2 -> player2.getUniqueId().equals(player3.getUniqueId()));
        arrayList.removeIf(player -> hashSet.contains(player.getUniqueId()));
        arrayList.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        int n3 = 45;
        int n4 = Math.max(0, n) * n3;
        int n5 = Math.min(arrayList.size(), n4 + n3);
        for (n2 = 0; n2 < 45; ++n2) {
            inventory.setItem(n2, null);
        }
        n2 = 0;
        for (int i = n4; i < n5; ++i) {
            Player player5 = (Player)arrayList.get(i);
            inventory.setItem(n2++, this.headItem(player5.getUniqueId(), "&f" + player5.getName(), "&7Click to invite"));
        }
        if (n4 > 0) {
            inventory.setItem(45, ItemUtil.icon(1004, "\u00a77Previous", new String[0]));
        }
        if (n5 < arrayList.size()) {
            inventory.setItem(53, ItemUtil.icon(1005, "\u00a77Next", new String[0]));
        }
    }

    private ItemStack headItem(UUID uUID, String string, String ... stringArray) {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta)itemStack.getItemMeta();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uUID);
        skullMeta.setOwningPlayer(offlinePlayer);
        skullMeta.setDisplayName(GuiPatchListener.color(string));
        if (stringArray != null && stringArray.length > 0) {
            ArrayList<String> arrayList = new ArrayList<String>();
            Object object = stringArray;
            int n = ((String[])object).length;
            for (int i = 0; i < n; ++i) {
                String string2 = object[i];
                arrayList.add(GuiPatchListener.color(string2));
            }
            if (arrayList.size() >= 1 && (((String)(object = (String)arrayList.get(0))).equals("\u00a77Member") || ((String)object).equals("\u00a7eLeader"))) {
                arrayList.add("\u00a78\u2022 \u00a77Left-click: \u00a7ePromote (leader)");
                arrayList.add("\u00a78\u2022 \u00a77Right-click: \u00a7aTrack / untrack");
            }
            skullMeta.setLore(arrayList);
        }
        itemStack.setItemMeta((ItemMeta)skullMeta);
        return itemStack;
    }

    private static UUID getHeadOwner(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (!(itemMeta instanceof SkullMeta)) {
            return null;
        }
        SkullMeta skullMeta = (SkullMeta)itemMeta;
        itemMeta = skullMeta.getOwningPlayer();
        return itemMeta != null ? itemMeta.getUniqueId() : null;
    }

    private static String nameOf(UUID uUID) {
        Player player = Bukkit.getPlayer((UUID)uUID);
        if (player != null) {
            return player.getName();
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uUID);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : uUID.toString();
    }

    private static String stripColor(String string) {
        return ChatColor.stripColor((String)string);
    }

    private static String color(String string) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)string);
    }

    private void injectFriendRequests(Player player, Inventory inventory) {
        Set<UUID> set = this.requests.getIncoming(player.getUniqueId());
        if (set != null && !set.isEmpty()) {
            HashMap<Integer, UUID> hashMap = new HashMap<Integer, UUID>();
            int[] nArray = new int[]{10, 11, 12, 13, 14, 15, 16};
            int n = 0;
            for (UUID uUID : set) {
                if (n >= nArray.length) break;
                int n2 = nArray[n++];
                inventory.setItem(n2, this.headItem(uUID, "&eFriend request", "&7From: &f" + GuiPatchListener.nameOf(uUID), "&aLeft click: accept", "&cRight click: decline"));
                hashMap.put(n2, uUID);
            }
            this.inviteFriendSlots.put(player.getUniqueId(), hashMap);
        }
    }
}

