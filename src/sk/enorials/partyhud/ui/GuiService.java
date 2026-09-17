/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.ui;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.module.PartyConfig;
import sk.enorials.partyhud.effects.PartyFX;
import sk.enorials.partyhud.friends.FriendRemoveChat;
import sk.enorials.partyhud.friends.FriendService;
import sk.enorials.partyhud.invite.InviteService;
import sk.enorials.partyhud.locator.LocatorService;
import sk.enorials.partyhud.party.Party;
import sk.enorials.partyhud.party.PartyService;
import sk.enorials.partyhud.ui.GuiHolder;
import sk.enorials.partyhud.ui.GuiType;
import sk.enorials.partyhud.util.ItemUtil;
import sk.enorials.partyhud.util.VoteKickMessenger;

public class GuiService {
    private final Plugin plugin;
    private final PartyService partyService;
    private final InviteService inviteService;
    private final FriendService friendService;
    private final LocatorService locatorService;
    private final PartyFX fx;
    private final Map<UUID, Map<Integer, UUID>> openPartySlotMap = new ConcurrentHashMap<UUID, Map<Integer, UUID>>();
    private final Map<UUID, Map<Integer, String>> hubSlotCommands = new ConcurrentHashMap<UUID, Map<Integer, String>>();

    public GuiService(Plugin plugin, PartyService partyService, InviteService inviteService, FriendService friendService, LocatorService locatorService, PartyFX partyFX) {
        this.plugin = plugin;
        this.partyService = partyService;
        this.inviteService = inviteService;
        this.friendService = friendService;
        this.locatorService = locatorService;
        this.fx = partyFX;
    }

    public void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new GuiHolder(GuiType.MAIN, 0), (int)36, (String)"Party");
        ItemStack itemStack = ItemUtil.bgTile(2001);
        for (int i = 0; i < inventory.getSize(); ++i) {
            inventory.setItem(i, itemStack);
        }
        inventory.setItem(0, null);
        inventory.setItem(1, null);
        inventory.setItem(2, null);
        inventory.setItem(6, null);
        inventory.setItem(8, null);
        inventory.setItem(9, null);
        inventory.setItem(11, null);
        inventory.setItem(15, null);
        inventory.setItem(17, null);
        inventory.setItem(18, null);
        inventory.setItem(20, null);
        inventory.setItem(24, null);
        inventory.setItem(26, null);
        inventory.setItem(27, null);
        inventory.setItem(29, null);
        if (!this.partyService.isInParty(player.getUniqueId())) {
            inventory.setItem(10, ItemUtil.icon(13001, "", new String[0]));
        }
        inventory.setItem(7, ItemUtil.icon(13002, "", new String[0]));
        inventory.setItem(16, ItemUtil.icon(13004, "", new String[0]));
        inventory.setItem(28, ItemUtil.icon(13005, "", new String[0]));
        if (this.partyService.isInParty(player.getUniqueId())) {
            inventory.setItem(25, ItemUtil.icon(13003, "", new String[0]));
        }
        inventory.setItem(31, ItemUtil.icon(13007, "", new String[0]));
        this.openMenu(player, GuiType.MAIN, 0, inventory);
    }

    public void openHub(Player player) {
        this.openHubRoot(player);
    }

    public void openCreateMode(Player player) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new GuiHolder(GuiType.CREATE_MODE, 0), (int)27, (String)"Create Party");
        ItemStack itemStack = ItemUtil.bgTile(2001);
        for (int i = 0; i < inventory.getSize(); ++i) {
            inventory.setItem(i, itemStack);
        }
        inventory.setItem(11, ItemUtil.item(Material.RED_WOOL, "\u00a7cPrivate Party", "\u00a77Invite-only party"));
        inventory.setItem(15, ItemUtil.item(Material.LIME_WOOL, "\u00a7aPublic Party", "\u00a77Anyone can join from GUI"));
        this.openMenu(player, GuiType.CREATE_MODE, 0, inventory);
    }

    public void openSelectSource(Player player) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new GuiHolder(GuiType.SELECT_SOURCE, 0), (int)27, (String)"Invite From");
        ItemStack itemStack = ItemUtil.bgTile(2001);
        for (int i = 0; i < inventory.getSize(); ++i) {
            inventory.setItem(i, itemStack);
        }
        inventory.setItem(11, ItemUtil.icon(13005, "\u00a7aFriends", "\u00a77Invite friends"));
        inventory.setItem(15, ItemUtil.icon(13006, "\u00a7bOnline", "\u00a77Invite online players"));
        this.openMenu(player, GuiType.SELECT_SOURCE, 0, inventory);
    }

    public void openOpenParties(Player player, int n) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new GuiHolder(GuiType.OPEN_PARTIES, n), (int)54, (String)"Public Parties");
        ItemStack itemStack = ItemUtil.bgTile(2001);
        for (int i = 0; i < inventory.getSize(); ++i) {
            inventory.setItem(i, itemStack);
        }
        ArrayList<Party> arrayList = new ArrayList<Party>();
        for (Party party2 : this.partyService.getParties()) {
            if (party2 == null || !party2.isOpen()) continue;
            arrayList.add(party2);
        }
        arrayList.sort(Comparator.comparing(party -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)party.getLeader());
            return offlinePlayer.getName() == null ? party.getLeader().toString() : offlinePlayer.getName().toLowerCase(Locale.ROOT);
        }));
        int n2 = 45;
        int n3 = Math.max(0, n) * n2;
        int n4 = Math.min(arrayList.size(), n3 + n2);
        HashMap<Integer, UUID> hashMap = new HashMap<Integer, UUID>();
        for (int i = n3; i < n4; ++i) {
            Party party3 = (Party)arrayList.get(i);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)party3.getLeader());
            String string = offlinePlayer.getName() != null ? offlinePlayer.getName() : party3.getLeader().toString();
            ArrayList<Object> arrayList2 = new ArrayList<Object>();
            arrayList2.add("\u00a77Leader: \u00a7f" + string);
            arrayList2.add("\u00a77Members:");
            for (UUID uUID : party3.getMembers()) {
                OfflinePlayer offlinePlayer2 = Bukkit.getOfflinePlayer((UUID)uUID);
                String string2 = offlinePlayer2.getName() != null ? offlinePlayer2.getName() : uUID.toString();
                arrayList2.add("\u00a78- \u00a7f" + string2);
            }
            arrayList2.add("");
            arrayList2.add("\u00a7aClick to join");
            int n5 = i - n3;
            inventory.setItem(n5, ItemUtil.playerHead(offlinePlayer, "\u00a7ePublic Party", arrayList2.toArray(new String[0])));
            hashMap.put(n5, party3.getId());
        }
        this.openPartySlotMap.put(player.getUniqueId(), hashMap);
        inventory.setItem(45, ItemUtil.icon(1020, "\u00a77Prev", new String[0]));
        inventory.setItem(49, ItemUtil.icon(1006, "\u00a77Back", new String[0]));
        inventory.setItem(53, ItemUtil.icon(1021, "\u00a77Next", new String[0]));
        this.openMenu(player, GuiType.OPEN_PARTIES, n, inventory);
    }

    public void openMyParty(Player player) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new GuiHolder(GuiType.MY_PARTY, 0), (int)45, (String)"My Party");
        Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
        if (optional.isEmpty()) {
            inventory.setItem(22, ItemUtil.item(Material.BARRIER, "\u00a7cYou are not in a party", new String[0]));
        } else {
            Party party = optional.get();
            boolean bl = party.getLeader().equals(player.getUniqueId());
            inventory.setItem(10, ItemUtil.icon(1010, "\u00a7cLeave Party", new String[0]));
            inventory.setItem(12, ItemUtil.icon(1011, "\u00a7eMembers: \u00a7f" + party.size(), new String[0]));
            inventory.setItem(14, ItemUtil.icon(1012, (String)(bl ? "\u00a76You are the leader" : "\u00a77Leader: \u00a7f" + this.name(party.getLeader())), new String[0]));
            inventory.setItem(16, ItemUtil.icon(party.isOpen() ? 1017 : 1016, party.isOpen() ? "\u00a7aPublic Party" : "\u00a7cPrivate Party", new String[0]));
            inventory.setItem(28, ItemUtil.icon(1013, "\u00a7cVote to KICK", "\u00a77Open vote menu"));
            UUID uUID = this.locatorService.getTrackedTarget(player.getUniqueId()).orElse(null);
            int n = 20;
            for (UUID uUID2 : party.getMembers()) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uUID2);
                String string = offlinePlayer.getName() != null ? offlinePlayer.getName() : uUID2.toString();
                String string2 = uUID2.equals(party.getLeader()) ? "\u00a76Leader" : "\u00a77Member";
                boolean bl2 = uUID != null && uUID.equals(uUID2);
                ArrayList<String> arrayList = new ArrayList<String>();
                arrayList.add(string2);
                if (!uUID2.equals(player.getUniqueId())) {
                    arrayList.add(bl2 ? "\u00a7aClick: Untrack" : "\u00a7bClick: Track");
                    arrayList.add("\u00a7eMiddle-click: Vote-kick");
                } else {
                    arrayList.add("\u00a77This is you");
                }
                if (bl && !uUID2.equals(player.getUniqueId())) {
                    arrayList.add("\u00a7eShift-Left-click: Kick");
                    arrayList.add("\u00a76Shift-Right-click: Make Leader");
                }
                inventory.setItem(n++, ItemUtil.playerHead(offlinePlayer, "\u00a7f" + string, arrayList.toArray(new String[0])));
                if (n < 35) continue;
                break;
            }
        }
        this.fillBackground(inventory);
        this.openMenu(player, GuiType.MY_PARTY, 0, inventory);
    }

    public void openVoteKick(Player player) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new GuiHolder(GuiType.VOTE_KICK, 0), (int)27, (String)"Vote to Kick");
        Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
        if (optional.isEmpty()) {
            inventory.setItem(13, ItemUtil.item(Material.BARRIER, "\u00a7cYou are not in a party", new String[0]));
        } else {
            Party party = optional.get();
            int n = 10;
            for (UUID uUID : party.getMembers()) {
                if (uUID.equals(player.getUniqueId())) continue;
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uUID);
                String string = offlinePlayer.getName() != null ? offlinePlayer.getName() : uUID.toString();
                inventory.setItem(n++, ItemUtil.playerHead(offlinePlayer, "\u00a7f" + string, "\u00a7cClick: Start vote to kick"));
                if (n == 17) {
                    n = 19;
                }
                if (n < 26) continue;
                break;
            }
        }
        inventory.setItem(26, ItemUtil.icon(1006, "\u00a77Back", new String[0]));
        this.fillBackground(inventory);
        this.openMenu(player, GuiType.VOTE_KICK, 0, inventory);
    }

    public void openFriends(Player player, int n) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new GuiHolder(GuiType.FRIENDS, n), (int)54, (String)"Friends");
        ArrayList<UUID> arrayList = new ArrayList<UUID>(this.friendService.getFriends(player.getUniqueId()));
        arrayList.sort(Comparator.comparing(this.friendService::name, String.CASE_INSENSITIVE_ORDER));
        int n2 = 45;
        int n3 = n * n2;
        int n4 = Math.min(arrayList.size(), n3 + n2);
        for (int i = n3; i < n4; ++i) {
            UUID uUID = arrayList.get(i);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uUID);
            String string = offlinePlayer.getName() != null ? offlinePlayer.getName() : uUID.toString();
            ArrayList<String> arrayList2 = new ArrayList<String>();
            arrayList2.add("\u00a7aLeft-click: Invite to party");
            arrayList2.add("\u00a7cRight-click: Remove friend");
            String[] stringArray = this.friendExtraStats(offlinePlayer);
            if (stringArray != null) {
                arrayList2.add(" ");
                for (String string2 : stringArray) {
                    arrayList2.add(string2);
                }
            }
            inventory.addItem(new ItemStack[]{ItemUtil.playerHead(offlinePlayer, "\u00a7f" + string, arrayList2.toArray(new String[0]))});
        }
        inventory.setItem(45, ItemUtil.icon(1020, "\u00a77Prev", new String[0]));
        inventory.setItem(49, ItemUtil.icon(1008, "\u00a7aAdd Friend", "\u00a77Shift-click a player in Online list", "\u00a77or use /enohud party friend add <name>"));
        inventory.setItem(53, ItemUtil.icon(1021, "\u00a77Next", new String[0]));
        inventory.setItem(52, ItemUtil.icon(1006, "\u00a77Back", new String[0]));
        this.fillBackground(inventory);
        this.openMenu(player, GuiType.FRIENDS, n, inventory);
    }

    public void openOnline(Player player, int n) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new GuiHolder(GuiType.ONLINE, n), (int)54, (String)"Online Players");
        ArrayList<Player> arrayList = new ArrayList<Player>(Bukkit.getOnlinePlayers());
        arrayList.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        int n2 = 45;
        int n3 = n * n2;
        int n4 = Math.min(arrayList.size(), n3 + n2);
        for (int i = n3; i < n4; ++i) {
            Player player2 = (Player)arrayList.get(i);
            inventory.addItem(new ItemStack[]{ItemUtil.playerHead((OfflinePlayer)player2, "\u00a7f" + player2.getName(), "\u00a7aClick: Invite to party", "\u00a7dShift-click: Add as friend")});
        }
        inventory.setItem(45, ItemUtil.icon(1020, "\u00a77Prev", new String[0]));
        inventory.setItem(53, ItemUtil.icon(1021, "\u00a77Next", new String[0]));
        this.fillBackground(inventory);
        this.openMenu(player, GuiType.ONLINE, n, inventory);
    }

    public void openInvites(Player player) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new GuiHolder(GuiType.INVITES, 0), (int)27, (String)"Invites");
        this.inviteService.getInvite(player.getUniqueId()).ifPresentOrElse(partyInvite -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)partyInvite.getLeader());
            String string = offlinePlayer.getName() != null ? offlinePlayer.getName() : partyInvite.getLeader().toString();
            inventory.setItem(13, ItemUtil.playerHead(offlinePlayer, "\u00a7eInvite from \u00a7f" + string, "\u00a7aClick: Accept", "\u00a7cRight-click: Decline"));
        }, () -> inventory.setItem(13, ItemUtil.item(Material.GLASS_PANE, "\u00a77No pending invites", new String[0])));
        inventory.setItem(26, ItemUtil.icon(1006, "\u00a77Back", new String[0]));
        this.fillBackground(inventory);
        this.openMenu(player, GuiType.INVITES, 0, inventory);
    }

    private void fillBackground(Inventory inventory) {
        ItemStack itemStack = ItemUtil.bgTile(2001);
        for (int i = 0; i < inventory.getSize(); ++i) {
            if (inventory.getItem(i) != null) continue;
            inventory.setItem(i, itemStack);
        }
    }

    private void handleMain(Player player, int n) {
        if (n == 0 || n == 1 || n == 2 || n == 9 || n == 10 || n == 11) {
            if (!this.partyService.isInParty(player.getUniqueId())) {
                this.openCreateMode(player);
            }
        } else if (n == 6 || n == 7 || n == 8) {
            this.openOpenParties(player, 0);
        } else if (n == 15 || n == 16 || n == 17) {
            this.openInvites(player);
        } else if (n == 24 || n == 25 || n == 26) {
            if (this.partyService.isInParty(player.getUniqueId())) {
                this.openMyParty(player);
            }
        } else if (n == 27 || n == 28 || n == 29) {
            this.openFriends(player, 0);
        } else if (n == 31) {
            this.openHubRoot(player);
        }
    }

    private void handleCreateMode(Player player, int n) {
        if (n == 11) {
            this.partyService.createParty(player, false);
            this.fx.partyCreated(player);
            player.sendMessage(Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)"Private party created.", (TextColor)NamedTextColor.YELLOW)));
            this.openSelectSource(player);
        } else if (n == 15) {
            this.partyService.createParty(player, true);
            this.fx.partyCreated(player);
            player.sendMessage(Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)"Public party created.", (TextColor)NamedTextColor.YELLOW)));
            this.openSelectSource(player);
        }
    }

    private void handleOpenParties(Player player, int n, int n2) {
        if (n == 45) {
            this.openOpenParties(player, Math.max(0, n2 - 1));
        } else if (n == 53) {
            this.openOpenParties(player, n2 + 1);
        } else {
            UUID uUID;
            Map<Integer, UUID> map = this.openPartySlotMap.get(player.getUniqueId());
            if (map != null && (uUID = map.get(n)) != null) {
                Party party = null;
                for (Party party2 : this.partyService.getParties()) {
                    if (!party2.getId().equals(uUID)) continue;
                    party = party2;
                    break;
                }
                if (party != null && party.isOpen()) {
                    int n3 = PartyConfig.get(this.plugin).getInt("party.max-size", 4);
                    boolean bl = this.partyService.joinParty(party, player, n3);
                    if (!bl) {
                        player.sendMessage((Component)Component.text((String)"[Party] Could not join (maybe full or already in a party).", (TextColor)NamedTextColor.RED));
                    } else {
                        this.fx.inviteAccepted(player);
                        player.sendMessage(Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)"Joined an open party.", (TextColor)NamedTextColor.YELLOW)));
                        player.closeInventory();
                    }
                } else {
                    player.sendMessage((Component)Component.text((String)"[Party] Party is no longer open.", (TextColor)NamedTextColor.RED));
                    this.openOpenParties(player, n2);
                }
            }
        }
    }

    private void handleInvites(Player player, int n, ClickType clickType) {
        if (n == 13) {
            if (clickType.isRightClick()) {
                this.inviteService.declineInvite(player, null);
            } else {
                this.inviteService.acceptInvite(player, null);
            }
            player.closeInventory();
        }
    }

    private void handleVoteKick(Player player, int n, ClickType clickType, ItemStack itemStack) {
        SkullMeta skullMeta;
        ItemMeta itemMeta;
        if (itemStack != null && (itemMeta = itemStack.getItemMeta()) instanceof SkullMeta && (itemMeta = (skullMeta = (SkullMeta)itemMeta).getOwningPlayer()) != null) {
            UUID uUID = itemMeta.getUniqueId();
            Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
            if (optional.isEmpty()) {
                player.sendMessage((Component)Component.text((String)"[Party] You are not in a party.", (TextColor)NamedTextColor.RED));
                player.closeInventory();
            } else {
                Party party = optional.get();
                PartyService.VoteKickResult voteKickResult = this.partyService.startVoteKick(player, uUID);
                VoteKickMessenger.broadcast(party, uUID);
                if (voteKickResult instanceof PartyService.VoteKickResult.Voted) {
                    PartyService.VoteKickResult.Voted voted = (PartyService.VoteKickResult.Voted)voteKickResult;
                    player.sendMessage(Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)("Vote-kick started: " + voted.votes() + "/" + voted.required()), (TextColor)NamedTextColor.YELLOW)));
                    this.fx.voteCast(player);
                } else if (voteKickResult instanceof PartyService.VoteKickResult.Simple) {
                    PartyService.VoteKickResult.Simple simple = (PartyService.VoteKickResult.Simple)voteKickResult;
                    if (simple.code().equals("KICKED")) {
                        player.sendMessage(Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)"Vote passed. Target was kicked.", (TextColor)NamedTextColor.YELLOW)));
                        this.fx.votePassed(player);
                    } else {
                        player.sendMessage((Component)Component.text((String)"[Party] Vote-kick failed.", (TextColor)NamedTextColor.RED));
                    }
                }
                player.closeInventory();
            }
        }
    }

    private void handleFriends(Player player, int n, ClickType clickType, ItemStack itemStack, int n2) {
        OfflinePlayer offlinePlayer;
        if (n == 45) {
            this.openFriends(player, Math.max(0, n2 - 1));
        } else if (n == 53) {
            this.openFriends(player, n2 + 1);
        } else if (itemStack != null && itemStack.getType() == Material.PLAYER_HEAD && (offlinePlayer = ((SkullMeta)itemStack.getItemMeta()).getOwningPlayer()) != null) {
            UUID uUID = offlinePlayer.getUniqueId();
            if (clickType.isShiftClick()) {
                FriendRemoveChat.sendConfirm(player, this.friendService.name(uUID), uUID.toString());
            } else if (clickType.isRightClick()) {
                FriendRemoveChat.sendShiftHint(player);
            } else {
                Player player2 = Bukkit.getPlayer((UUID)uUID);
                if (player2 == null || !player2.isOnline()) {
                    player.sendMessage((Component)Component.text((String)"[Party] Player is offline.", (TextColor)NamedTextColor.RED));
                } else if (!this.partyService.isInParty(player.getUniqueId())) {
                    player.sendMessage((Component)Component.text((String)"[Party] Create a party first.", (TextColor)NamedTextColor.RED));
                } else {
                    this.inviteService.sendInvite(player, player2);
                }
            }
        }
    }

    private void handleOnline(Player player, int n, ClickType clickType, ItemStack itemStack, int n2) {
        UUID uUID;
        Player player2;
        OfflinePlayer offlinePlayer;
        if (n == 45) {
            this.openOnline(player, Math.max(0, n2 - 1));
        } else if (n == 53) {
            this.openOnline(player, n2 + 1);
        } else if (itemStack != null && itemStack.getType() == Material.PLAYER_HEAD && (offlinePlayer = ((SkullMeta)itemStack.getItemMeta()).getOwningPlayer()) != null && (player2 = Bukkit.getPlayer((UUID)(uUID = offlinePlayer.getUniqueId()))) != null && player2.isOnline()) {
            if (clickType.isShiftClick()) {
                this.friendService.addFriend(player.getUniqueId(), uUID);
                player.sendMessage(Component.text((String)"[Friends] ", (TextColor)NamedTextColor.LIGHT_PURPLE).append((Component)Component.text((String)("Added friend: " + player2.getName()), (TextColor)NamedTextColor.GRAY)));
            } else if (!this.partyService.isInParty(player.getUniqueId())) {
                player.sendMessage((Component)Component.text((String)"[Party] Create a party first.", (TextColor)NamedTextColor.RED));
            } else {
                this.inviteService.sendInvite(player, player2);
            }
        }
    }

    private void handleMyParty(Player player, int n, ClickType clickType, ItemStack itemStack) {
        Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
        if (!optional.isEmpty()) {
            UUID uUID;
            OfflinePlayer offlinePlayer;
            Party party = optional.get();
            boolean bl = party.getLeader().equals(player.getUniqueId());
            if (n == 10) {
                this.partyService.leaveParty(player.getUniqueId(), false);
                this.fx.memberLeft(player);
                player.sendMessage(Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)"You left the party.", (TextColor)NamedTextColor.YELLOW)));
            } else if (n == 28) {
                this.openVoteKick(player);
            } else if (itemStack != null && itemStack.getType() == Material.PLAYER_HEAD && (offlinePlayer = ((SkullMeta)itemStack.getItemMeta()).getOwningPlayer()) != null && !(uUID = offlinePlayer.getUniqueId()).equals(player.getUniqueId())) {
                if (clickType.isRightClick() && bl) {
                    this.partyService.promote(player, uUID);
                    Player player2 = Bukkit.getPlayer((UUID)uUID);
                    if (player2 != null) {
                        this.fx.promoted(player2);
                    }
                    this.fx.promoted(player);
                    player.sendMessage(Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)("New leader: " + this.name(uUID)), (TextColor)NamedTextColor.YELLOW)));
                    this.openMyParty(player);
                } else {
                    boolean bl2;
                    this.locatorService.toggleTracking(player, uUID);
                    boolean bl3 = bl2 = this.locatorService.getTrackedTarget(player.getUniqueId()).orElse(null) != null && ((UUID)this.locatorService.getTrackedTarget(player.getUniqueId()).orElse(null)).equals(uUID);
                    if (bl2) {
                        this.fx.trackingOn(player);
                    } else {
                        this.fx.trackingOff(player);
                    }
                    player.sendMessage(Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)(bl2 ? "Tracking " + this.name(uUID) : "Stopped tracking " + this.name(uUID)), (TextColor)NamedTextColor.YELLOW)));
                    this.openMyParty(player);
                }
            }
        }
    }

    private String name(UUID uUID) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uUID);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : uUID.toString();
    }

    private static void lambda$openMain$0(Inventory inventory, int n) {
        inventory.setItem(n, null);
    }

    public void handleClick(Player player, GuiHolder guiHolder, int n, ClickType clickType, ItemStack itemStack) {
        Object object;
        GuiType guiType = guiHolder.getType();
        int n2 = guiHolder.getPage();
        if (itemStack != null && (object = itemStack.getItemMeta()) != null && object.hasCustomModelData() && object.getCustomModelData() == 1006) {
            if (guiType == GuiType.VOTE_KICK) {
                this.openMyParty(player);
            } else {
                this.openMain(player);
            }
            return;
        }
        if (guiType == GuiType.HUB) {
            String string;
            if (n == 49 || n == 26) {
                player.closeInventory();
                return;
            }
            object = this.hubSlotCommands.get(player.getUniqueId());
            if (object != null && (string = (String)object.get(n)) != null && !string.isBlank()) {
                player.performCommand(string.startsWith("/") ? string.substring(1) : string);
                return;
            }
        }
        if (guiType == GuiType.MAIN) {
            this.handleMain(player, n);
        } else if (guiType == GuiType.CREATE_MODE) {
            this.handleCreateMode(player, n);
        } else if (guiType == GuiType.SELECT_SOURCE) {
            if (n >= 10 && n <= 12) {
                this.openFriends(player, 0);
            } else if (n >= 14 && n <= 16) {
                this.openOnline(player, 0);
            }
        } else if (guiType == GuiType.OPEN_PARTIES) {
            this.handleOpenParties(player, n, n2);
        } else if (guiType == GuiType.MY_PARTY) {
            this.handleMyParty(player, n, clickType, itemStack);
        } else if (guiType == GuiType.FRIENDS) {
            this.handleFriends(player, n, clickType, itemStack, n2);
        } else if (guiType == GuiType.ONLINE) {
            this.handleOnline(player, n, clickType, itemStack, n2);
        } else if (guiType == GuiType.INVITES) {
            this.handleInvites(player, n, clickType);
        } else if (guiType == GuiType.VOTE_KICK) {
            this.handleVoteKick(player, n, clickType, itemStack);
        }
    }

    private void openMenu(Player player, GuiType guiType, int n, Inventory inventory) {
        player.openInventory(inventory);
    }

    public void openHubRoot(Player player) {
        Map<String, String> map = this.collectActiveEnorialsCommands();
        if (map.isEmpty()) {
            this.openMainRoot(player);
        } else {
            Inventory inventory = Bukkit.createInventory((InventoryHolder)new GuiHolder(GuiType.HUB, 0), (int)54, (String)"Main Menu");
            ItemStack itemStack = ItemUtil.bgTile(2001);
            for (int i = 0; i < inventory.getSize(); ++i) {
                inventory.setItem(i, itemStack);
            }
            int[] nArray = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
            HashMap<Integer, String> hashMap = new HashMap<Integer, String>();
            int n = 0;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String string;
                if (n >= nArray.length) break;
                String string2 = entry.getKey();
                String string3 = entry.getValue();
                int n2 = nArray[n++];
                String string4 = string = string2.startsWith("Enorials") ? string2.substring("Enorials".length()) : string2;
                if (string.isBlank()) {
                    string = string2;
                }
                Material material = this.hubMaterialForPlugin(string2);
                inventory.setItem(n2, ItemUtil.item(material, "\u00a7e" + string, "\u00a77Click to open " + string + " menu", "\u00a78/" + string3));
                hashMap.put(n2, string3);
            }
            inventory.setItem(49, ItemUtil.item(Material.BARRIER, "\u00a7cClose", new String[0]));
            this.hubSlotCommands.put(player.getUniqueId(), hashMap);
            this.openMenu(player, GuiType.HUB, 0, inventory);
        }
    }

    public void openMainRoot(Player player) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new GuiHolder(GuiType.MAIN, 0), (int)36, (String)"Party");
        ItemStack itemStack = ItemUtil.bgTile(2001);
        for (int i = 0; i < inventory.getSize(); ++i) {
            inventory.setItem(i, itemStack);
        }
        inventory.setItem(0, null);
        inventory.setItem(1, null);
        inventory.setItem(2, null);
        inventory.setItem(6, null);
        inventory.setItem(8, null);
        inventory.setItem(9, null);
        inventory.setItem(11, null);
        inventory.setItem(15, null);
        inventory.setItem(17, null);
        inventory.setItem(18, null);
        inventory.setItem(20, null);
        inventory.setItem(24, null);
        inventory.setItem(26, null);
        inventory.setItem(27, null);
        inventory.setItem(29, null);
        if (!this.partyService.isInParty(player.getUniqueId())) {
            inventory.setItem(10, ItemUtil.icon(13001, "", new String[0]));
        }
        inventory.setItem(7, ItemUtil.icon(13002, "", new String[0]));
        inventory.setItem(16, ItemUtil.icon(13004, "", new String[0]));
        inventory.setItem(28, ItemUtil.icon(13005, "", new String[0]));
        if (this.partyService.isInParty(player.getUniqueId())) {
            inventory.setItem(25, ItemUtil.icon(13003, "", new String[0]));
        }
        inventory.setItem(31, ItemUtil.icon(13007, "", new String[0]));
        this.openMenu(player, GuiType.MAIN, 0, inventory);
    }

    private Plugin spellsPlugin() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("EnorialsSpells");
        return plugin != null && plugin.isEnabled() ? plugin : null;
    }

    private Map<String, String> collectActiveEnorialsCommands() {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            String string;
            String string2;
            if (plugin == null || !plugin.isEnabled() || (string2 = plugin.getName()) == null || !string2.startsWith("Enorials") || "EnorialsHUD".equalsIgnoreCase(string2) || "EnorialsEvents".equalsIgnoreCase(string2) || (string = this.resolveMainCommand(plugin)) == null || string.isBlank()) continue;
            linkedHashMap.put(string2, string);
        }
        return linkedHashMap;
    }

    private String resolveMainCommand(Plugin plugin) {
        String string = plugin.getName();
        if (string == null) {
            return null;
        }
        if ("EnorialsParty".equalsIgnoreCase(string)) {
            return "party";
        }
        if ("EnorialsSpells".equalsIgnoreCase(string)) {
            return "spells";
        }
        if ("EnorialsMinimap".equalsIgnoreCase(string)) {
            return "enomap";
        }
        if ("EnorialsDungeons".equalsIgnoreCase(string)) {
            return "dungeons";
        }
        if ("EnorialsEvents".equalsIgnoreCase(string)) {
            return "events";
        }
        if ("EnorialsArena".equalsIgnoreCase(string)) {
            return "arena";
        }
        if ("EnorialsGPS".equalsIgnoreCase(string)) {
            return "gps";
        }
        if (plugin.getDescription().getCommands() != null && !plugin.getDescription().getCommands().isEmpty()) {
            ArrayList arrayList = new ArrayList(plugin.getDescription().getCommands().keySet());
            Collections.sort(arrayList);
            for (String string2 : arrayList) {
                if ("menu".equalsIgnoreCase(string2)) continue;
                return string2;
            }
            return (String)arrayList.get(0);
        }
        return null;
    }

    private Material hubMaterialForPlugin(String string) {
        if (string == null) {
            return Material.BOOK;
        }
        switch (string.toLowerCase(Locale.ROOT)) {
            case "enorialsparty": {
                return Material.PLAYER_HEAD;
            }
            case "enorialsspells": {
                return Material.ENCHANTED_BOOK;
            }
            case "enorialsminimap": {
                return Material.FILLED_MAP;
            }
            case "enorialsevents": {
                return Material.CLOCK;
            }
            case "enorialsdungeons": {
                return Material.IRON_SWORD;
            }
            case "enorialsarena": {
                return Material.DIAMOND_SWORD;
            }
            case "enorialsgps": {
                return Material.COMPASS;
            }
        }
        return Material.BOOK;
    }

    private String[] friendExtraStats(OfflinePlayer offlinePlayer) {
        try {
            Player player;
            Plugin plugin = this.spellsPlugin();
            if (plugin == null) {
                return null;
            }
            Player player2 = player = offlinePlayer != null ? Bukkit.getPlayer((UUID)offlinePlayer.getUniqueId()) : null;
            if (player != null && player.isOnline()) {
                Method method = plugin.getClass().getMethod("getPlayerLevel", Player.class);
                Method method2 = plugin.getClass().getMethod("getAverageSpellLevel", Player.class);
                Method method3 = plugin.getClass().getMethod("getPlayerClassName", Player.class);
                int n = ((Number)method.invoke((Object)plugin, player)).intValue();
                int n2 = ((Number)method2.invoke((Object)plugin, player)).intValue();
                String string = String.valueOf(method3.invoke((Object)plugin, player));
                return new String[]{"\u00a77Class: \u00a7f" + string, "\u00a77Player Lv: \u00a7f" + n, "\u00a77Spell Lv: \u00a7f" + n2};
            }
            return null;
        }
        catch (Throwable throwable) {
            return null;
        }
    }
}

