/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 */
package sk.enorials.gps.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.data.FavoritesService;
import sk.enorials.gps.input.ChatInputService;
import sk.enorials.gps.input.TargetInputType;
import sk.enorials.gps.integration.IntegrationManager;
import sk.enorials.gps.nav.NavigationManager;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.gps.nav.target.PlayerTarget;
import sk.enorials.gps.nav.target.TargetLocator;
import sk.enorials.gps.util.Color;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.betonquest.HudBetonQuest;
import sk.enorials.hud.betonquest.QuestLogWindow;
import sk.enorials.hud.menu.GraphicalHudMenu;
import sk.enorials.hud.menu.MenuGeometry;
import sk.enorials.partyhud.party.Party;

public final class GpsGuiService {
    private static final int[] CONTENT_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
    private final EnorialsGpsPlugin plugin;
    private final NavigationManager navigationManager;
    private final ChatInputService chatInputService;
    private final IntegrationManager integrationManager;
    private final FavoritesService favoritesService;
    private final Map<UUID, Integer> poiPage = new HashMap<UUID, Integer>();
    private final Map<UUID, String> poiSearch = new HashMap<UUID, String>();
    private final Map<UUID, Map<Integer, String>> poiSlotToKey = new HashMap<UUID, Map<Integer, String>>();
    private final Map<UUID, Integer> favoritePage = new HashMap<UUID, Integer>();
    private final Map<UUID, Map<Integer, String>> favoriteSlotToKey = new HashMap<UUID, Map<Integer, String>>();
    private final Map<UUID, Integer> playerListPage = new HashMap<UUID, Integer>();
    private final Map<UUID, Map<Integer, UUID>> playerListSlotToTarget = new HashMap<UUID, Map<Integer, UUID>>();

    public GpsGuiService(EnorialsGpsPlugin enorialsGpsPlugin, NavigationManager navigationManager, ChatInputService chatInputService, IntegrationManager integrationManager, FavoritesService favoritesService) {
        this.plugin = enorialsGpsPlugin;
        this.navigationManager = navigationManager;
        this.chatInputService = chatInputService;
        this.integrationManager = integrationManager;
        this.favoritesService = favoritesService;
    }

    public void openMain(Player player) {
        if (!HudAccess.has((CommandSender)player, "enorials.gps.use")) {
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        arrayList.add(this.button("markers", "Destinations", () -> this.openTarget(player)));
        arrayList.add(this.button("save", "Favorites", () -> this.openFavorites(player)));
        if (this.navigationManager.hasActiveNavigation(player)) {
            arrayList.add(this.button("close", "Stop GPS", () -> {
                this.navigationManager.stopNavigation(player, "manual");
                this.openMain(player);
            }).as(MenuGeometry.Kind.ACTION));
        }
        this.plugin.owner().menu().graphical().open(player, "GPS navigation", arrayList, () -> this.plugin.owner().menu().open(player));
    }

    private GraphicalHudMenu.Button button(String string, String string2, Runnable runnable) {
        return new GraphicalHudMenu.Button(string, string2, runnable);
    }

    public boolean samePartyTarget(Player player, TargetLocator targetLocator) {
        Party party = this.plugin.owner().getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
        return party != null && party.getMembers().stream().anyMatch(uUID -> !uUID.equals(player.getUniqueId()) && Bukkit.getPlayer((UUID)uUID) != null && player.canSee(Bukkit.getPlayer((UUID)uUID)) && targetLocator.id().equals("player:" + String.valueOf(uUID)));
    }

    public void openParty(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.gps.party")) {
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        Party party = this.plugin.owner().getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
        if (party != null) {
            for (UUID uUID2 : party.getMembers().stream().sorted(Comparator.comparing(uUID -> Objects.toString(Bukkit.getOfflinePlayer((UUID)uUID).getName(), ""))).toList()) {
                Player player2 = Bukkit.getPlayer((UUID)uUID2);
                if (player2 == null || uUID2.equals(player.getUniqueId()) || !player.canSee(player2)) continue;
                arrayList.add(this.button("head:" + String.valueOf(uUID2), player2.getName(), () -> {
                    PlayerTarget playerTarget = new PlayerTarget(uUID2, player2.getName());
                    if (HudAccess.has((CommandSender)player, "enorials.gps.use") && this.samePartyTarget(player, playerTarget)) {
                        this.navigationManager.startNavigation(player, playerTarget, NavigationMode.HUD, "hud-party", RouteDescriptor.player(uUID2.toString()));
                        player.closeInventory();
                    } else {
                        this.openParty(player);
                    }
                }));
            }
        }
        this.plugin.owner().menu().graphical().open(player, "Find party", arrayList, () -> this.plugin.owner().menu().party(player));
    }

    public void openMode(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.gps.use")) {
            return;
        }
        NavigationMode navigationMode = this.navigationManager.getCurrentOrDefaultMode(player);
        List<GraphicalHudMenu.Button> list = List.of(this.button("hud", "HUD" + (navigationMode != NavigationMode.COMPASS ? "\nSelected" : ""), () -> {
            this.navigationManager.setMode(player, NavigationMode.HUD);
            this.openMode(player);
        }).as(MenuGeometry.Kind.ACTION), this.button("map", "HUD + compass" + (navigationMode == NavigationMode.COMPASS ? "\nSelected" : ""), () -> {
            this.navigationManager.setMode(player, NavigationMode.COMPASS);
            this.openMode(player);
        }).as(MenuGeometry.Kind.ACTION));
        this.plugin.owner().menu().graphical().open(player, "GPS mode", list, () -> this.openOptions(player));
    }

    public void openTarget(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.gps.use")) {
            return;
        }
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new Holder(GuiKind.TARGET), (int)45, (Component)this.title("gui.target_title", "&8Choose Target Type"));
        this.fill(inventory);
        inventory.setItem(10, this.named(Material.PLAYER_HEAD, "&aPlayer", this.withPermissionLore(player, "enorials.gps.track.player", List.of("&7Click: enter player name in chat", "&eShift-click: open online player heads"))));
        inventory.setItem(12, this.named(Material.ROTTEN_FLESH, "&6Mob", this.withPermissionLore(player, "enorials.gps.track.mob", List.of("&7Enter mob type or custom name"))));
        inventory.setItem(14, this.named(Material.MAP, "&eLocation", List.of("&7Enter X Y Z [world]")));
        inventory.setItem(16, this.named(Material.NAME_TAG, "&bNPC", this.withPermissionLore(player, "enorials.gps.track.npc", List.of("&7Enter NPC id or name"))));
        inventory.setItem(30, this.named(Material.ENDER_PEARL, "&dPoints Of Interest", List.of("&7Open searchable POI list")));
        inventory.setItem(32, this.named(Material.GRASS_BLOCK, "&2Block", this.withPermissionLore(player, "enorials.gps.track.block", List.of("&7Enter block id (vanilla/custom)"))));
        inventory.setItem(40, this.named(Material.OAK_DOOR, "&7Back", List.of("&7Return to main menu")));
        this.show(player, inventory);
    }

    public void openOptions(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.gps.use")) {
            return;
        }
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new Holder(GuiKind.OPTIONS), (int)27, (Component)this.title("gui.options_title", "&8GPS Options"));
        this.fill(inventory);
        boolean bl = this.navigationManager.isDistanceDisplayEnabled(player);
        boolean bl2 = this.navigationManager.isVerticalDisplayEnabled(player);
        boolean bl3 = this.navigationManager.isTargetNameDisplayEnabled(player);
        boolean bl4 = this.navigationManager.isCompactHudEnabled(player);
        boolean bl5 = this.navigationManager.isTargetMarkerEnabled(player);
        inventory.setItem(10, this.named(bl ? Material.LIME_DYE : Material.GRAY_DYE, "&eShow Distance: " + (bl ? "&aON" : "&cOFF"), List.of("&7Show distance in meters in HUD")));
        inventory.setItem(12, this.named(bl2 ? Material.LIME_DYE : Material.GRAY_DYE, "&eShow High/Low: " + (bl2 ? "&aON" : "&cOFF"), List.of("&7Show if target is above or below you")));
        inventory.setItem(14, this.named(bl3 ? Material.LIME_DYE : Material.GRAY_DYE, "&eShow Target Name: " + (bl3 ? "&aON" : "&cOFF"), List.of("&7Show target name in navigation HUD")));
        inventory.setItem(16, this.named(bl4 ? Material.LIME_DYE : Material.GRAY_DYE, "&eCompact HUD: " + (bl4 ? "&aON" : "&cOFF"), List.of("&7Use shorter minimal HUD display")));
        inventory.setItem(20, this.named(bl5 ? Material.BEACON : Material.GRAY_DYE, "&eTarget Beacon: " + (bl5 ? "&aON" : "&cOFF"), List.of("&7Shows a vertical marker at tracked target", "&7Visible only to you")));
        inventory.setItem(22, this.named(Material.OAK_DOOR, "&7Back", List.of("&7Return to main menu")));
        this.show(player, inventory);
    }

    public void openPlayerList(Player player) {
        this.openPlayerList(player, this.playerListPage.getOrDefault(player.getUniqueId(), 0));
    }

    public void openPlayers(Player player) {
        if (!HudAccess.has((CommandSender)player, "enorials.gps.track.player")) {
            this.plugin.msg().send((CommandSender)player, "&cYou do not have permission: &fenorials.gps.track.player");
            return;
        }
        this.plugin.owner().menu().graphical().open(player, "Find player", List.of(this.button("party", "Online players", () -> this.openPlayerList(player)), this.button("edit", "Player name", () -> {
            this.chatInputService.request(player, TargetInputType.PLAYER);
            player.closeInventory();
        })), () -> this.openTarget(player));
    }

    private void openPlayerList(Player player3, int n) {
        int n2;
        if (!HudAccess.has((CommandSender)player3, "enorials.gps.track.player")) {
            this.plugin.msg().send((CommandSender)player3, "&cYou do not have permission: &fenorials.gps.track.player");
            return;
        }
        if (!this.integrationManager.isPlayerTargetsEnabled()) {
            this.plugin.msg().send((CommandSender)player3, "&cPlayer targets are disabled.");
            return;
        }
        ArrayList<Player> arrayList = new ArrayList<Player>(Bukkit.getOnlinePlayers());
        arrayList.removeIf(player2 -> !player3.canSee(player2) || player2.equals((Object)player3));
        arrayList.sort((player, player2) -> player.getName().compareToIgnoreCase(player2.getName()));
        int n3 = this.totalPages(arrayList.size());
        int n4 = Math.max(0, Math.min(n, n3 - 1));
        this.playerListPage.put(player3.getUniqueId(), n4);
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new Holder(GuiKind.PLAYER_LIST), (int)54, (Component)this.title("gui.player_list_title", "&8Online Players"));
        this.fill(inventory);
        HashMap<Integer, UUID> hashMap = new HashMap<Integer, UUID>();
        int n5 = n4 * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length && (n2 = n5 + i) < arrayList.size(); ++i) {
            Player player4 = (Player)arrayList.get(n2);
            ItemStack itemStack = this.playerHead(player4, "&a" + player4.getName(), List.of("&7World: &f" + player4.getWorld().getName(), "&7Click to start tracking this player"));
            inventory.setItem(CONTENT_SLOTS[i], itemStack);
            hashMap.put(CONTENT_SLOTS[i], player4.getUniqueId());
        }
        this.playerListSlotToTarget.put(player3.getUniqueId(), hashMap);
        this.renderPagerControls(inventory, n4, n3, "", "PLAYER_LIST");
        this.show(player3, inventory);
    }

    public void openPoiList(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.gps.places")) {
            return;
        }
        this.openPoiList(player, this.poiPage.getOrDefault(player.getUniqueId(), 0), this.poiSearch.getOrDefault(player.getUniqueId(), ""));
    }

    private void openPoiList(Player player, int n, String string) {
        int n2;
        Map<String, String> map = this.integrationManager.getVisiblePoiDisplayNames(player);
        ArrayList<String> arrayList = new ArrayList<String>(map.keySet());
        if (!string.isBlank()) {
            String string3 = string.toLowerCase();
            arrayList.removeIf(string2 -> !string2.toLowerCase().contains(string3));
        }
        Collections.sort(arrayList);
        int n3 = this.totalPages(arrayList.size());
        int n4 = Math.max(0, Math.min(n, n3 - 1));
        this.poiPage.put(player.getUniqueId(), n4);
        this.poiSearch.put(player.getUniqueId(), string);
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new Holder(GuiKind.POI), (int)54, (Component)this.title("gui.poi_title", "&8Points Of Interest"));
        this.fill(inventory);
        HashMap<Integer, String> hashMap = new HashMap<Integer, String>();
        int n5 = n4 * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length && (n2 = n5 + i) < arrayList.size(); ++i) {
            String string4 = arrayList.get(n2);
            String string5 = map.getOrDefault(string4, string4);
            ArrayList<String> arrayList2 = new ArrayList<String>();
            arrayList2.add("&7Target: &f" + string5);
            arrayList2.add("&7Click to start navigation");
            arrayList2.add("&eShift-click to toggle favorite");
            inventory.setItem(CONTENT_SLOTS[i], this.named(Material.LODESTONE, "&a" + string4, arrayList2));
            hashMap.put(CONTENT_SLOTS[i], string4);
        }
        this.poiSlotToKey.put(player.getUniqueId(), hashMap);
        this.renderPagerControls(inventory, n4, n3, string, "POI");
        this.show(player, inventory);
    }

    public void openFavorites(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.gps.places")) {
            return;
        }
        this.openFavorites(player, this.favoritePage.getOrDefault(player.getUniqueId(), 0));
    }

    private void openFavorites(Player player, int n) {
        int n2;
        Set<String> set = this.favoritesService.getFavorites(player.getUniqueId());
        ArrayList<String> arrayList = new ArrayList<String>(set);
        Collections.sort(arrayList);
        int n3 = this.totalPages(arrayList.size());
        int n4 = Math.max(0, Math.min(n, n3 - 1));
        this.favoritePage.put(player.getUniqueId(), n4);
        Inventory inventory = Bukkit.createInventory((InventoryHolder)new Holder(GuiKind.FAVORITES), (int)54, (Component)this.title("gui.favorites_title", "&8Favorite Destinations"));
        this.fill(inventory);
        HashMap<Integer, String> hashMap = new HashMap<Integer, String>();
        int n5 = n4 * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length && (n2 = n5 + i) < arrayList.size(); ++i) {
            String string = arrayList.get(n2);
            String string2 = this.integrationManager.getPoiDisplayName(string);
            ArrayList<String> arrayList2 = new ArrayList<String>();
            arrayList2.add("&7Target: &f" + string2);
            arrayList2.add("&7Click to start navigation");
            arrayList2.add("&eShift-click to remove from favorites");
            inventory.setItem(CONTENT_SLOTS[i], this.named(Material.NETHER_STAR, "&d" + string, arrayList2));
            hashMap.put(CONTENT_SLOTS[i], string);
        }
        this.favoriteSlotToKey.put(player.getUniqueId(), hashMap);
        this.renderPagerControls(inventory, n4, n3, "", "FAVORITES");
        this.show(player, inventory);
    }

    public void openQuestRoutes(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.gps.quests")) {
            return;
        }
        HudBetonQuest hudBetonQuest = this.plugin.owner().betonQuest();
        if (hudBetonQuest != null && hudBetonQuest.questLog() != null) {
            try {
                hudBetonQuest.questLog().refresh(player);
            }
            catch (Exception exception) {
                this.plugin.getLogger().warning("Cannot refresh accepted quests: " + exception.getMessage());
            }
            ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
            for (QuestLogWindow.Entry entry : hudBetonQuest.questLog().entries(player)) {
                arrayList.add(this.button("atlas:accepted", entry.title() + (entry.tracked() ? "\nTracking" : ""), () -> {
                    try {
                        hudBetonQuest.questLog().select(player, entry.key());
                        player.closeInventory();
                    }
                    catch (Exception exception) {
                        player.sendMessage(exception.getMessage());
                    }
                }).as(MenuGeometry.Kind.ACTION));
            }
            this.plugin.owner().menu().graphical().open(player, "Accepted quests", arrayList, () -> this.plugin.owner().menu().quests(player));
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        this.integrationManager.getAutoRoutes(player).values().stream().sorted(Comparator.comparing(IntegrationManager.AutoRoute::displayName, String.CASE_INSENSITIVE_ORDER)).forEach(autoRoute -> arrayList.add(this.button("marker:quest_questionmark", autoRoute.displayName(), () -> this.startQuestRoute(player, autoRoute.key())).as(MenuGeometry.Kind.ACTION)));
        this.plugin.owner().menu().graphical().open(player, "Quests", arrayList, () -> this.openMain(player));
    }

    public void handleClick(Player player, GuiKind guiKind, int n, ItemStack itemStack, boolean bl) {
        if (!this.plugin.config().getBoolean("gui.enabled", true)) {
            String string = this.plugin.config().getString("gui.disabled_message", "&cGPS menu is disabled by server settings.");
            this.plugin.msg().send((CommandSender)player, string);
            player.closeInventory();
            return;
        }
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }
        switch (guiKind.ordinal()) {
            case 0: {
                if (n == 10) {
                    this.openMode(player);
                    break;
                }
                if (n == 12) {
                    this.openTarget(player);
                    break;
                }
                if (n == 14) {
                    this.openFavorites(player);
                    break;
                }
                if (n == 16) {
                    this.openQuestRoutes(player);
                    break;
                }
                if (n == 31) {
                    this.openOptions(player);
                    break;
                }
                if (n != 30 || !this.navigationManager.hasActiveNavigation(player)) break;
                this.navigationManager.stopNavigation(player, "manual");
                player.closeInventory();
                break;
            }
            case 1: {
                if (n == 11) {
                    this.navigationManager.setMode(player, NavigationMode.ACTIONBAR);
                    player.closeInventory();
                    break;
                }
                if (n == 13) {
                    this.navigationManager.setMode(player, NavigationMode.BOSSBAR);
                    player.closeInventory();
                    break;
                }
                if (n == 15) {
                    this.navigationManager.setMode(player, NavigationMode.COMPASS);
                    player.closeInventory();
                    break;
                }
                if (n != 22) break;
                this.openMain(player);
                break;
            }
            case 2: {
                if (n == 10) {
                    this.navigationManager.toggleDistanceDisplay(player);
                    this.openOptions(player);
                    break;
                }
                if (n == 12) {
                    this.navigationManager.toggleVerticalDisplay(player);
                    this.openOptions(player);
                    break;
                }
                if (n == 14) {
                    this.navigationManager.toggleTargetNameDisplay(player);
                    this.openOptions(player);
                    break;
                }
                if (n == 16) {
                    this.navigationManager.toggleCompactHud(player);
                    this.openOptions(player);
                    break;
                }
                if (n == 20) {
                    this.navigationManager.toggleTargetMarker(player);
                    this.openOptions(player);
                    break;
                }
                if (n != 22) break;
                this.openMain(player);
                break;
            }
            case 3: {
                if (n == 10) {
                    if (!HudAccess.has((CommandSender)player, "enorials.gps.track.player")) {
                        this.plugin.msg().send((CommandSender)player, "&cYou do not have permission: &fenorials.gps.track.player");
                        return;
                    }
                    if (!bl) {
                        this.openPlayerList(player);
                        break;
                    }
                    this.chatInputService.request(player, TargetInputType.PLAYER);
                    player.closeInventory();
                    break;
                }
                if (n == 12) {
                    if (!HudAccess.has((CommandSender)player, "enorials.gps.track.mob")) {
                        this.plugin.msg().send((CommandSender)player, "&cYou do not have permission: &fenorials.gps.track.mob");
                        return;
                    }
                    this.chatInputService.request(player, TargetInputType.MOB);
                    player.closeInventory();
                    break;
                }
                if (n == 14) {
                    this.chatInputService.request(player, TargetInputType.LOCATION);
                    player.closeInventory();
                    break;
                }
                if (n == 16) {
                    if (!HudAccess.has((CommandSender)player, "enorials.gps.track.npc")) {
                        this.plugin.msg().send((CommandSender)player, "&cYou do not have permission: &fenorials.gps.track.npc");
                        return;
                    }
                    this.chatInputService.request(player, TargetInputType.NPC);
                    player.closeInventory();
                    break;
                }
                if (n == 32) {
                    if (!HudAccess.has((CommandSender)player, "enorials.gps.track.block")) {
                        this.plugin.msg().send((CommandSender)player, "&cYou do not have permission: &fenorials.gps.track.block");
                        return;
                    }
                    this.chatInputService.request(player, TargetInputType.BLOCK);
                    player.closeInventory();
                    break;
                }
                if (n == 30) {
                    this.openPoiList(player);
                    break;
                }
                if (n != 40) break;
                this.openMain(player);
                break;
            }
            case 4: {
                this.handlePoiClick(player, n, bl);
                break;
            }
            case 5: {
                this.handleFavoritesClick(player, n, bl);
                break;
            }
            case 6: {
                this.handleQuestClick(player, n);
                break;
            }
            case 7: {
                this.handlePlayerListClick(player, n);
            }
        }
    }

    private void handlePoiClick(Player player, int n, boolean bl) {
        if (n == 45) {
            this.openPoiList(player, this.poiPage.getOrDefault(player.getUniqueId(), 0) - 1, this.poiSearch.getOrDefault(player.getUniqueId(), ""));
            return;
        }
        if (n == 46) {
            List<String> list = this.integrationManager.getVisiblePoiDisplayNames(player).keySet().stream().limit(8L).toList();
            this.chatInputService.requestCustom(player, "&eType POI search text (or 'cancel').", list, string -> {
                this.poiSearch.put(player.getUniqueId(), string.trim());
                this.poiPage.put(player.getUniqueId(), 0);
                this.openPoiList(player, 0, string.trim());
            });
            player.closeInventory();
            return;
        }
        if (n == 47) {
            this.openPoiList(player, 0, "");
            return;
        }
        if (n == 49) {
            this.openTarget(player);
            return;
        }
        if (n == 52) {
            this.openPoiList(player, this.poiPage.getOrDefault(player.getUniqueId(), 0) + 1, this.poiSearch.getOrDefault(player.getUniqueId(), ""));
            return;
        }
        String string2 = (String)this.poiSlotToKey.getOrDefault(player.getUniqueId(), Collections.emptyMap()).get(n);
        if (string2 == null) {
            return;
        }
        if (bl) {
            int n2 = this.plugin.config().getInt("favorites.max", 30);
            FavoritesService.ToggleResult toggleResult = this.favoritesService.toggle(player.getUniqueId(), string2, n2);
            if (toggleResult == FavoritesService.ToggleResult.ADDED) {
                this.plugin.msg().send((CommandSender)player, "&aAdded to favorites: &f" + string2);
            } else if (toggleResult == FavoritesService.ToggleResult.LIMIT_REACHED) {
                this.plugin.msg().send((CommandSender)player, "&cFavorites limit reached (&f" + n2 + "&c). Remove one first.");
            } else {
                this.plugin.msg().send((CommandSender)player, "&eRemoved from favorites: &f" + string2);
            }
            this.openPoiList(player);
            return;
        }
        Optional<TargetLocator> optional = this.integrationManager.resolvePoi(player, string2);
        if (optional.isEmpty()) {
            this.plugin.msg().send((CommandSender)player, "&cThat POI is not currently available.");
            return;
        }
        this.navigationManager.startNavigation(player, optional.get(), this.navigationManager.getCurrentOrDefaultMode(player), "poi", RouteDescriptor.poi(string2));
        player.closeInventory();
    }

    private void handleFavoritesClick(Player player, int n, boolean bl) {
        if (n == 45) {
            this.openFavorites(player, this.favoritePage.getOrDefault(player.getUniqueId(), 0) - 1);
            return;
        }
        if (n == 49) {
            this.openMain(player);
            return;
        }
        if (n == 52) {
            this.openFavorites(player, this.favoritePage.getOrDefault(player.getUniqueId(), 0) + 1);
            return;
        }
        String string = (String)this.favoriteSlotToKey.getOrDefault(player.getUniqueId(), Collections.emptyMap()).get(n);
        if (string == null) {
            return;
        }
        if (bl) {
            this.favoritesService.remove(player.getUniqueId(), string);
            this.plugin.msg().send((CommandSender)player, "&eRemoved favorite: &f" + string);
            this.openFavorites(player);
            return;
        }
        Optional<TargetLocator> optional = this.integrationManager.resolvePoi(player, string);
        if (optional.isEmpty()) {
            this.plugin.msg().send((CommandSender)player, "&cThat favorite POI is currently unavailable.");
            return;
        }
        this.navigationManager.startNavigation(player, optional.get(), this.navigationManager.getCurrentOrDefaultMode(player), "favorite", RouteDescriptor.poi(string));
        player.closeInventory();
    }

    private void handleQuestClick(Player player, int n) {
        this.openQuestRoutes(player);
    }

    private void startQuestRoute(Player player, String string) {
        IntegrationManager.AutoRoute autoRoute = this.integrationManager.getAutoRoutes(player).get(string);
        if (autoRoute == null) {
            this.plugin.msg().send((CommandSender)player, "&cThat quest route is no longer available.");
            this.openQuestRoutes(player);
            return;
        }
        Optional<TargetLocator> optional = this.integrationManager.resolveAutoRoute(player, autoRoute);
        if (optional.isEmpty()) {
            this.plugin.msg().send((CommandSender)player, "&cCould not resolve quest route target.");
            return;
        }
        this.navigationManager.startNavigation(player, optional.get(), autoRoute.mode(), autoRoute.source(), autoRoute.descriptor());
        player.closeInventory();
    }

    private void handlePlayerListClick(Player player, int n) {
        if (n == 45) {
            this.openPlayerList(player, this.playerListPage.getOrDefault(player.getUniqueId(), 0) - 1);
            return;
        }
        if (n == 49) {
            this.openTarget(player);
            return;
        }
        if (n == 52) {
            this.openPlayerList(player, this.playerListPage.getOrDefault(player.getUniqueId(), 0) + 1);
            return;
        }
        UUID uUID = (UUID)this.playerListSlotToTarget.getOrDefault(player.getUniqueId(), Collections.emptyMap()).get(n);
        if (uUID == null) {
            return;
        }
        if (!HudAccess.has((CommandSender)player, "enorials.gps.track.player")) {
            this.plugin.msg().send((CommandSender)player, "&cYou do not have permission: &fenorials.gps.track.player");
            player.closeInventory();
            return;
        }
        if (!this.integrationManager.isPlayerTargetsEnabled()) {
            this.plugin.msg().send((CommandSender)player, "&cPlayer targets are disabled.");
            player.closeInventory();
            return;
        }
        Player player2 = Bukkit.getPlayer((UUID)uUID);
        if (player2 == null || !player2.isOnline()) {
            this.plugin.msg().send((CommandSender)player, "&cThat player is no longer online.");
            this.openPlayerList(player);
            return;
        }
        this.navigationManager.startNavigation(player, new PlayerTarget(player2.getUniqueId(), player2.getName()), this.navigationManager.getCurrentOrDefaultMode(player), "gui-player-list", RouteDescriptor.player(player2.getUniqueId().toString()));
        player.closeInventory();
    }

    private void renderPagerControls(Inventory inventory, int n, int n2, String string, String string2) {
        inventory.setItem(45, this.named(Material.ARROW, "&ePrevious", List.of("&7Page " + (n + 1) + "/" + n2)));
        inventory.setItem(49, this.named(Material.OAK_DOOR, "&7Back", List.of("&7Return")));
        inventory.setItem(52, this.named(Material.ARROW, "&eNext", List.of("&7Page " + (n + 1) + "/" + n2)));
        if ("POI".equals(string2)) {
            inventory.setItem(46, this.named(Material.OAK_SIGN, "&bSearch", List.of("&7Type search text in chat", "&7Current: &f" + (string.isBlank() ? "<none>" : string))));
            inventory.setItem(47, this.named(Material.BARRIER, "&cClear Search", List.of("&7Show all POIs")));
            inventory.setItem(51, this.named(Material.NETHER_STAR, "&dFavorites", List.of("&7Shift-click a POI to toggle favorite")));
        } else if ("FAVORITES".equals(string2)) {
            inventory.setItem(46, this.named(Material.NETHER_STAR, "&dFavorites", List.of("&7Click favorite to navigate", "&eShift-click to remove")));
        } else if ("QUEST".equals(string2)) {
            inventory.setItem(46, this.named(Material.BOOK, "&6Quest Routes", List.of("&7Routes shown only when conditions pass")));
        } else if ("PLAYER_LIST".equals(string2)) {
            inventory.setItem(46, this.named(Material.PLAYER_HEAD, "&aOnline Players", List.of("&7Click a head to start tracking")));
        }
    }

    private void show(Player player, Inventory inventory) {
        GuiKind guiKind = ((Holder)inventory.getHolder()).kind();
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        Runnable runnable = guiKind == GuiKind.OPTIONS ? () -> this.plugin.owner().menu().navigationCustom(player) : (guiKind == GuiKind.PLAYER_LIST ? () -> this.openPlayers(player) : (guiKind == GuiKind.POI ? () -> this.openTarget(player) : () -> this.openMain(player)));
        for (int i = 0; i < inventory.getSize(); ++i) {
            String string;
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack == null || itemStack.getType().name().endsWith("STAINED_GLASS_PANE") || itemStack.getType() == Material.OAK_DOOR) continue;
            int n = i;
            String string2 = ChatColor.stripColor((String)PlainTextComponentSerializer.plainText().serialize(itemStack.getItemMeta().displayName()));
            if (string2 == null || string2.isBlank() || i == 46 && guiKind != GuiKind.POI) continue;
            String string3 = string2.replace("Show Distance: ", "Distance\n").replace("Show High/Low: ", "Height\n").replace("Show Target Name: ", "Target name\n").replace("Compact HUD: ", "Compact\n").replace("Target Beacon: ", "Beacon\n").replace("Points Of Interest", "Places");
            switch (itemStack.getType()) {
                case PLAYER_HEAD: {
                    Object object;
                    if (guiKind == GuiKind.PLAYER_LIST && this.playerListSlotToTarget.getOrDefault(player.getUniqueId(), Map.of()).containsKey(n)) {
                        object = "head:" + String.valueOf(this.playerListSlotToTarget.get(player.getUniqueId()).get(n));
                        break;
                    }
                    object = "party";
                    break;
                }
                case BARRIER: {
                    Object object = "close";
                    break;
                }
                case NETHER_STAR: {
                    Object object = "save";
                    break;
                }
                case BOOK: 
                case ROTTEN_FLESH: {
                    Object object = "effects";
                    break;
                }
                case NAME_TAG: {
                    Object object = "friends";
                    break;
                }
                case MAP: 
                case LODESTONE: 
                case GRASS_BLOCK: 
                case ENDER_PEARL: {
                    Object object = "map";
                    break;
                }
                case ARROW: {
                    Object object = "back";
                    break;
                }
                case OAK_SIGN: {
                    Object object = "edit";
                    break;
                }
                default: {
                    Object object = string = "settings";
                }
            }
            if (guiKind == GuiKind.TARGET && i == 10) {
                arrayList.add(this.button("party", "Players", () -> this.openPlayers(player)));
                continue;
            }
            if (i < 45 && (guiKind == GuiKind.POI || guiKind == GuiKind.FAVORITES)) {
                String string4 = string3;
                arrayList.add(this.button(string, string3, () -> this.plugin.owner().menu().graphical().open(player, string4, List.of(this.button("markers", "Navigate", () -> this.handleClick(player, guiKind, n, itemStack, false)), this.button("save", guiKind == GuiKind.FAVORITES ? "Remove favorite" : "Toggle favorite", () -> this.handleClick(player, guiKind, n, itemStack, true))), () -> {
                    if (guiKind == GuiKind.POI) {
                        this.openPoiList(player);
                    } else {
                        this.openFavorites(player);
                    }
                })));
                continue;
            }
            arrayList.add(new GraphicalHudMenu.Button(string, string3, () -> this.handleClick(player, guiKind, n, itemStack, false), () -> this.handleClick(player, guiKind, n, itemStack, true)).as(guiKind == GuiKind.OPTIONS ? MenuGeometry.Kind.TOGGLE : (guiKind == GuiKind.PLAYER_LIST || guiKind == GuiKind.QUEST_ROUTES ? MenuGeometry.Kind.ACTION : MenuGeometry.Kind.CATEGORY)));
        }
        String string = switch (guiKind.ordinal()) {
            case 2 -> "GPS display";
            case 3 -> "GPS destinations";
            case 7 -> "Online players";
            case 4 -> "Places";
            case 5 -> "Favorites";
            case 6 -> "Quests";
            default -> "GPS navigation";
        };
        this.plugin.owner().menu().graphical().open(player, string, arrayList, runnable);
    }

    private int totalPages(int n) {
        if (n <= 0) {
            return 1;
        }
        return (n + CONTENT_SLOTS.length - 1) / CONTENT_SLOTS.length;
    }

    private void fill(Inventory inventory) {
        Material material = Material.BLACK_STAINED_GLASS_PANE;
        String string = this.plugin.config().getString("gui.filler", "BLACK_STAINED_GLASS_PANE");
        try {
            material = Material.valueOf((String)string.toUpperCase());
        }
        catch (IllegalArgumentException illegalArgumentException) {
            // empty catch block
        }
        ItemStack itemStack = this.named(material, " ", List.of());
        for (int i = 0; i < inventory.getSize(); ++i) {
            if (inventory.getItem(i) != null) continue;
            inventory.setItem(i, itemStack);
        }
    }

    private ItemStack named(Material material, String string, List<String> list) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName((Component)Component.text((String)Color.colorize(string)));
        if (!list.isEmpty()) {
            ArrayList<TextComponent> arrayList = new ArrayList<TextComponent>();
            for (String string2 : list) {
                arrayList.add(Component.text((String)Color.colorize(string2)));
            }
            itemMeta.lore(arrayList);
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private ItemStack playerHead(Player player, String string, List<String> list) {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (!(itemMeta instanceof SkullMeta)) {
            return this.named(Material.PLAYER_HEAD, string, list);
        }
        SkullMeta skullMeta = (SkullMeta)itemMeta;
        skullMeta.setOwningPlayer((OfflinePlayer)player);
        skullMeta.displayName((Component)Component.text((String)Color.colorize(string)));
        if (!list.isEmpty()) {
            ArrayList<TextComponent> arrayList = new ArrayList<TextComponent>();
            for (String string2 : list) {
                arrayList.add(Component.text((String)Color.colorize(string2)));
            }
            skullMeta.lore(arrayList);
        }
        itemStack.setItemMeta((ItemMeta)skullMeta);
        return itemStack;
    }

    private List<String> withPermissionLore(Player player, String string, List<String> list) {
        if (HudAccess.has((CommandSender)player, string)) {
            return list;
        }
        ArrayList<String> arrayList = new ArrayList<String>(list);
        arrayList.add("&cRequires: &f" + string);
        return arrayList;
    }

    private Component title(String string, String string2) {
        String string3 = this.plugin.config().getString(string, string2);
        return Component.text((String)Color.colorize(string3));
    }

    public record Holder(GuiKind kind) implements InventoryHolder
    {
        public Inventory getInventory() {
            return null;
        }
    }

    public static enum GuiKind {
        MAIN,
        MODE,
        OPTIONS,
        TARGET,
        POI,
        FAVORITES,
        QUEST_ROUTES,
        PLAYER_LIST;

    }
}

