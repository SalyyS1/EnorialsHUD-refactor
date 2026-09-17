/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.NotNull
 */
package sk.enorials.minimap.menu;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapPreset;
import sk.enorials.minimap.map.MinimapProvider;
import sk.enorials.minimap.map.MinimapScreenPosition;
import sk.enorials.minimap.map.MinimapShape;
import sk.enorials.minimap.map.MinimapSize;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.marker.MarkerCategory;
import sk.enorials.minimap.map.marker.MarkerMinimapLayer;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.minimap.map.renderer.SecondaryMinimapLayerRenderer;
import sk.enorials.minimap.menu.MarkerWizard;
import sk.enorials.minimap.radar.RadarCategory;
import sk.enorials.minimap.storage.DatabaseMarkerModel;

public class EnomapMenuListener
implements Listener {
    private static final String MAIN_TITLE = String.valueOf(ChatColor.DARK_AQUA) + "Enorials Minimap";
    private static final String REMOVE_TITLE = String.valueOf(ChatColor.DARK_RED) + "Remove Marker";
    private static final String MARKERS_TITLE = String.valueOf(ChatColor.DARK_GREEN) + "My Markers";
    private static final String CATEGORIES_TITLE = String.valueOf(ChatColor.DARK_BLUE) + "Marker Categories";
    private static final String PUBLIC_PREFIX = "public::";
    private final EnorialsMinimap plugin;
    private final MarkerWizard markerWizard;
    private final Map<UUID, Set<String>> hiddenMarkers = new HashMap<UUID, Set<String>>();
    private final Map<UUID, Set<MarkerCategory>> hiddenCategories = new HashMap<UUID, Set<MarkerCategory>>();
    private final Map<PublicMarkerKey, PublicMarkerData> publicMarkers = new HashMap<PublicMarkerKey, PublicMarkerData>();

    public EnomapMenuListener(EnorialsMinimap enorialsMinimap) {
        this.plugin = enorialsMinimap;
        this.markerWizard = new MarkerWizard(enorialsMinimap, this);
    }

    public MarkerWizard markerWizard() {
        return this.markerWizard;
    }

    public static boolean isPublicLayerKey(String string) {
        return string.startsWith(PUBLIC_PREFIX);
    }

    public static String publicLayerKey(UUID uUID, String string) {
        return PUBLIC_PREFIX + String.valueOf(uUID) + "::" + string;
    }

    public void onMarkerRemoved(UUID uUID, String string) {
        this.unpublishMarker(uUID, string);
    }

    public void onMarkerRenamed(UUID uUID, String string, String string2) {
        PublicMarkerKey publicMarkerKey = new PublicMarkerKey(uUID, string);
        PublicMarkerData publicMarkerData = this.publicMarkers.remove(publicMarkerKey);
        if (publicMarkerData != null) {
            for (Minimap object : this.plugin.minimapListener().getPlayerMinimaps().values()) {
                this.removePublicLayer(object, publicMarkerKey);
            }
            PublicMarkerKey publicMarkerKey2 = new PublicMarkerKey(uUID, string2);
            this.publicMarkers.put(publicMarkerKey2, publicMarkerData);
            for (Minimap minimap : this.plugin.minimapListener().getPlayerMinimaps().values()) {
                if (minimap.holder().getUniqueId().equals(uUID)) continue;
                this.addPublicLayer(minimap, publicMarkerKey2, publicMarkerData);
            }
        }
    }

    public void onMarkerCategoryChanged(UUID uUID, String string, MarkerCategory markerCategory) {
        PublicMarkerKey publicMarkerKey = new PublicMarkerKey(uUID, string);
        PublicMarkerData publicMarkerData = this.publicMarkers.get(publicMarkerKey);
        if (publicMarkerData != null) {
            PublicMarkerData publicMarkerData2 = new PublicMarkerData(publicMarkerData.icon(), publicMarkerData.world(), publicMarkerData.x(), publicMarkerData.z(), publicMarkerData.keepOnEdge(), publicMarkerData.depth(), publicMarkerData.maxVisibleDistance(), markerCategory, publicMarkerData.createdAt(), publicMarkerData.expiresAt());
            this.publicMarkers.put(publicMarkerKey, publicMarkerData2);
            for (Minimap minimap : this.plugin.minimapListener().getPlayerMinimaps().values()) {
                if (minimap.holder().getUniqueId().equals(uUID)) continue;
                this.addPublicLayer(minimap, publicMarkerKey, publicMarkerData2);
            }
        }
    }

    private Inventory filledMenu(MenuType menuType, int n, String string) {
        MenuHolder menuHolder = new MenuHolder(menuType);
        Inventory inventory = Bukkit.createInventory((InventoryHolder)menuHolder, (int)n, (String)(String.valueOf(ChatColor.AQUA) + string));
        menuHolder.inventory(inventory);
        for (int i = 0; i < n; ++i) {
            inventory.setItem(i, this.menuItem(Material.GRAY_STAINED_GLASS_PANE, " ", new String[0]));
        }
        return inventory;
    }

    public void openMainMenu(Player player) {
        Inventory inventory = this.filledMenu(MenuType.MAIN, 45, "Enorials Minimap");
        boolean bl = this.plugin.getPlayerMinimap(player) != null;
        inventory.setItem(10, this.menuItem(bl ? Material.LIME_DYE : Material.GRAY_DYE, String.valueOf(ChatColor.AQUA) + "Minimap: " + (bl ? "ON" : "OFF"), String.valueOf(ChatColor.GRAY) + "Click to " + (bl ? "disable" : "enable")));
        inventory.setItem(12, this.menuItem(Material.PAPER, String.valueOf(ChatColor.AQUA) + "Display Settings", String.valueOf(ChatColor.GRAY) + "Layout, appearance, radar and saved views"));
        inventory.setItem(14, this.menuItem(Material.ORANGE_DYE, String.valueOf(ChatColor.AQUA) + "Fullscreen", String.valueOf(ChatColor.GRAY) + "Open the fullscreen map"));
        inventory.setItem(16, this.menuItem(Material.CHEST, String.valueOf(ChatColor.AQUA) + "Marker Menu", String.valueOf(ChatColor.GRAY) + "Create and manage your markers"));
        player.openInventory(inventory);
    }

    public void openDisplayMenu(Player player) {
        try {
            if (this.ensureEnabledMinimap(player) == null) {
                return;
            }
        }
        catch (Exception exception) {
            this.plugin.getLogger().warning("Could not open minimap display settings: " + exception.getMessage());
            player.sendMessage(String.valueOf(ChatColor.RED) + "Could not open display settings.");
            return;
        }
        Inventory inventory = this.filledMenu(MenuType.DISPLAY, 45, "Display Settings");
        inventory.setItem(10, this.menuItem(Material.MAP, String.valueOf(ChatColor.AQUA) + "Map Layout", "Size, shape and screen position"));
        inventory.setItem(12, this.menuItem(Material.PAINTING, String.valueOf(ChatColor.AQUA) + "Appearance", "Map lighting and frame style"));
        inventory.setItem(14, this.menuItem(Material.COMPASS, String.valueOf(ChatColor.AQUA) + "Entity Radar", "Radar visibility and mob / NPC groups"));
        inventory.setItem(16, this.menuItem(Material.SPYGLASS, String.valueOf(ChatColor.AQUA) + "Behavior", "Coordinates and automatic combat zoom"));
        inventory.setItem(22, this.menuItem(Material.BOOK, String.valueOf(ChatColor.AQUA) + "Saved Views", "Save or load your three presets"));
        inventory.setItem(31, this.menuItem(Material.ARROW, String.valueOf(ChatColor.AQUA) + "Back", "Return to minimap menu"));
        inventory.setItem(34, this.menuItem(Material.FILLED_MAP, String.valueOf(ChatColor.AQUA) + "Fullscreen", "Open the fullscreen map"));
        player.openInventory(inventory);
    }

    private void openSettingsMenu(Player player, MenuType menuType) {
        try {
            Minimap minimap = this.ensureEnabledMinimap(player);
            if (minimap == null) {
                return;
            }
            String string = switch (menuType.ordinal()) {
                case 2 -> "Map Layout";
                case 3 -> "Appearance";
                case 4 -> "Entity Radar";
                case 5 -> "Behavior";
                case 6 -> "Saved Views";
                default -> throw new IllegalArgumentException("Not a settings menu: " + String.valueOf((Object)menuType));
            };
            Inventory inventory = this.filledMenu(menuType, 45, string);
            switch (menuType.ordinal()) {
                case 2: {
                    MinimapSize[] minimapSizeArray = new MinimapSize[]{MinimapSize.SMALL, MinimapSize.MEDIUM, MinimapSize.LARGE};
                    for (int i = 0; i < minimapSizeArray.length; ++i) {
                        inventory.setItem(10 + i, this.sizeItem(minimapSizeArray[i], minimap.size()));
                    }
                    inventory.setItem(14, this.menuItem(Material.SUNFLOWER, String.valueOf(ChatColor.AQUA) + "Circle", minimap.shape() == MinimapShape.CIRCLE ? "Selected" : "Click to select"));
                    inventory.setItem(15, this.menuItem(Material.IRON_BLOCK, String.valueOf(ChatColor.AQUA) + "Square", minimap.shape() == MinimapShape.SQUARE ? "Selected" : "Click to select"));
                    inventory.setItem(16, this.menuItem(Material.ARROW, String.valueOf(ChatColor.AQUA) + "Position: " + String.valueOf((Object)minimap.screenPosition()), "Click to switch LEFT / RIGHT"));
                    break;
                }
                case 3: {
                    String[] stringArray = new String[]{"Original", "Natural", "Vivid"};
                    for (int i = 0; i < stringArray.length; ++i) {
                        inventory.setItem(10 + i, this.menuItem(minimap.colorProfile() == i ? Material.LIME_DYE : Material.GRAY_DYE, String.valueOf(ChatColor.AQUA) + "Lighting: " + stringArray[i], minimap.colorProfile() == i ? "Selected" : "Click to select"));
                    }
                    String[] stringArray2 = new String[]{"Enorials", "Frostbound", "Corsair", "Inferno", "Wildwood", "Astral"};
                    Material[] materialArray = new Material[]{Material.IRON_NUGGET, Material.LAPIS_LAZULI, Material.GOLD_NUGGET, Material.EMERALD, Material.REDSTONE, Material.OBSIDIAN};
                    for (int i = 0; i < stringArray2.length; ++i) {
                        inventory.setItem(19 + i, this.menuItem(materialArray[i], String.valueOf(ChatColor.AQUA) + "Frame: " + stringArray2[i], minimap.frameStyle() == i ? "Selected" : "Click to select"));
                    }
                    inventory.setItem(25, this.menuItem(Material.PAINTING, String.valueOf(ChatColor.AQUA) + "Follow HUD theme", minimap.frameFollowsTheme() ? "Selected" : "Click to follow your HUD theme"));
                    break;
                }
                case 4: {
                    inventory.setItem(13, this.menuItem(Material.COMPASS, String.valueOf(ChatColor.AQUA) + "Entity Radar: " + EnomapMenuListener.onOff(minimap.radarMasterEnabled()), "Show or hide all radar dots"));
                    RadarCategory[] radarCategoryArray = RadarCategory.values();
                    Material[] materialArray = new Material[]{Material.RED_DYE, Material.YELLOW_DYE, Material.LIME_DYE};
                    for (int i = 0; i < radarCategoryArray.length; ++i) {
                        inventory.setItem(20 + i * 2, this.menuItem(minimap.radarEnabled(radarCategoryArray[i]) ? materialArray[i] : Material.GRAY_DYE, String.valueOf(ChatColor.AQUA) + radarCategoryArray[i].label() + ": " + EnomapMenuListener.onOff(minimap.radarEnabled(radarCategoryArray[i])), "Click to toggle this group"));
                    }
                    break;
                }
                case 5: {
                    inventory.setItem(11, this.menuItem(Material.MAP, String.valueOf(ChatColor.AQUA) + "Coordinates XYZ: " + EnomapMenuListener.onOff(minimap.coordinatesEnabled()), "Click to toggle"));
                    inventory.setItem(15, this.menuItem(Material.SPYGLASS, String.valueOf(ChatColor.AQUA) + "Combat Zoom: " + EnomapMenuListener.onOff(minimap.combatZoomEnabled()), "Automatically zoom during combat"));
                    break;
                }
                case 6: {
                    for (int i = 0; i < 3; ++i) {
                        inventory.setItem(11 + i * 2, this.presetItem(minimap, i));
                    }
                    break;
                }
            }
            inventory.setItem(31, this.menuItem(Material.ARROW, String.valueOf(ChatColor.AQUA) + "Back", "Return to display settings"));
            inventory.setItem(35, this.menuItem(Material.CHEST, String.valueOf(ChatColor.AQUA) + "Main Menu", "Return to minimap menu"));
            player.openInventory(inventory);
        }
        catch (Exception exception) {
            this.plugin.getLogger().warning("Could not open minimap display settings: " + exception.getMessage());
            player.sendMessage(String.valueOf(ChatColor.RED) + "Could not open display settings.");
        }
    }

    public void openMarkerMenu(Player player) {
        Inventory inventory = this.filledMenu(MenuType.MARKER_HUB, 45, "Marker Menu");
        inventory.setItem(10, this.menuItem(Material.WRITABLE_BOOK, String.valueOf(ChatColor.AQUA) + "Add Marker", "Choose category, icon, visibility and location"));
        inventory.setItem(12, this.menuItem(Material.ENDER_EYE, String.valueOf(ChatColor.AQUA) + "My Markers", "Show / hide your markers", "Shift-click a marker to switch public / private"));
        inventory.setItem(14, this.menuItem(Material.HOPPER, String.valueOf(ChatColor.AQUA) + "Marker Categories", "Show / hide waypoint and quest categories"));
        inventory.setItem(16, this.menuItem(Material.BARRIER, String.valueOf(ChatColor.AQUA) + "Remove Marker", "Remove one of your personal markers"));
        inventory.setItem(31, this.menuItem(Material.ARROW, String.valueOf(ChatColor.AQUA) + "Back", "Return to minimap menu"));
        player.openInventory(inventory);
    }

    private static String onOff(boolean bl) {
        return bl ? "ON" : "OFF";
    }

    private ItemStack presetItem(Minimap minimap, int n) {
        MinimapPreset minimapPreset;
        MinimapPreset minimapPreset2 = minimapPreset = minimap != null ? minimap.preset(n) : null;
        return minimapPreset == null ? this.menuItem(Material.WRITABLE_BOOK, String.valueOf(ChatColor.YELLOW) + "Preset " + (n + 1), String.valueOf(ChatColor.GRAY) + "Left click = load", String.valueOf(ChatColor.GRAY) + "Right click = save current", String.valueOf(ChatColor.DARK_GRAY) + "Status: empty") : this.menuItem(Material.BOOK, String.valueOf(ChatColor.YELLOW) + "Preset " + (n + 1), String.valueOf(ChatColor.GRAY) + minimapPreset.position().name() + ", " + minimapPreset.size().name() + ", " + minimapPreset.shape().name(), String.valueOf(ChatColor.GRAY) + "Danger ring: " + (minimapPreset.dangerRingEnabled() ? String.valueOf(ChatColor.GREEN) + "ON" : String.valueOf(ChatColor.RED) + "OFF"), String.valueOf(ChatColor.GRAY) + "Fog style: " + (minimapPreset.fogStylingEnabled() ? String.valueOf(ChatColor.GREEN) + "ON" : String.valueOf(ChatColor.RED) + "OFF"), String.valueOf(ChatColor.GRAY) + "Left click = load", String.valueOf(ChatColor.GRAY) + "Right click = overwrite");
    }

    private ItemStack sizeItem(MinimapSize minimapSize, MinimapSize minimapSize2) {
        boolean bl = minimapSize == minimapSize2;
        Material material = switch (minimapSize) {
            default -> throw new IncompatibleClassChangeError();
            case MinimapSize.SMALL -> Material.GOLD_NUGGET;
            case MinimapSize.MEDIUM -> Material.GOLD_INGOT;
            case MinimapSize.LARGE -> Material.GOLD_BLOCK;
        };
        ItemStack itemStack = this.menuItem(material, String.valueOf(bl ? ChatColor.GREEN : ChatColor.WHITE) + "Size: " + minimapSize.name(), bl ? String.valueOf(ChatColor.GREEN) + "Selected" : String.valueOf(ChatColor.GRAY) + "Click to set " + minimapSize.name());
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setEnchantmentGlintOverride(Boolean.valueOf(bl));
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private ItemStack menuItem(Material material, String string, String ... stringArray) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }
        itemMeta.setDisplayName(string);
        itemMeta.setLore(List.of(stringArray));
        itemMeta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {
        Object object = inventoryClickEvent.getWhoClicked();
        if (object instanceof Player) {
            Player player = (Player)object;
            InventoryHolder inventoryHolder = inventoryClickEvent.getView().getTopInventory().getHolder();
            if (inventoryHolder instanceof MenuHolder) {
                object = (MenuHolder)inventoryHolder;
                inventoryClickEvent.setCancelled(true);
                if (inventoryClickEvent.getClickedInventory() != null && inventoryClickEvent.getClickedInventory().equals((Object)inventoryClickEvent.getView().getTopInventory())) {
                    switch (((MenuHolder)object).type().ordinal()) {
                        case 0: {
                            this.handleMainClick(player, inventoryClickEvent.getSlot(), inventoryClickEvent.isRightClick());
                            break;
                        }
                        case 1: {
                            this.handleDisplayClick(player, inventoryClickEvent.getSlot());
                            break;
                        }
                        case 2: 
                        case 3: 
                        case 4: 
                        case 5: 
                        case 6: {
                            this.handleSettingsClick(player, ((MenuHolder)object).type(), inventoryClickEvent.getSlot(), inventoryClickEvent.isRightClick());
                            break;
                        }
                        case 7: {
                            this.handleMarkerHubClick(player, inventoryClickEvent.getSlot());
                            break;
                        }
                        case 8: {
                            this.handleRemoveClick(player, (MenuHolder)object, inventoryClickEvent.getSlot());
                            break;
                        }
                        case 9: {
                            this.handleToggleMarkersClick(player, (MenuHolder)object, inventoryClickEvent.getSlot(), inventoryClickEvent.isShiftClick());
                            break;
                        }
                        case 10: {
                            this.handleToggleCategoriesClick(player, inventoryClickEvent.getSlot());
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        this.hiddenMarkers.remove(playerQuitEvent.getPlayer().getUniqueId());
        this.hiddenCategories.remove(playerQuitEvent.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent inventoryDragEvent) {
        if (inventoryDragEvent.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            inventoryDragEvent.setCancelled(true);
        }
    }

    private void handleMainClick(Player player, int n, boolean bl) {
        try {
            switch (n) {
                case 10: {
                    this.toggleEnabled(player);
                    this.openMainMenu(player);
                    break;
                }
                case 12: {
                    this.openDisplayMenu(player);
                    break;
                }
                case 14: {
                    player.performCommand("enohud map fullscreen");
                    break;
                }
                case 16: {
                    this.openMarkerMenu(player);
                }
            }
        }
        catch (Exception exception) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Could not update minimap: " + exception.getMessage());
        }
    }

    private void handleDisplayClick(Player player, int n) {
        switch (n) {
            case 10: {
                this.openSettingsMenu(player, MenuType.LAYOUT);
                break;
            }
            case 12: {
                this.openSettingsMenu(player, MenuType.APPEARANCE);
                break;
            }
            case 14: {
                this.openSettingsMenu(player, MenuType.RADAR);
                break;
            }
            case 16: {
                this.openSettingsMenu(player, MenuType.BEHAVIOR);
                break;
            }
            case 22: {
                this.openSettingsMenu(player, MenuType.PRESETS);
                break;
            }
            case 31: {
                this.openMainMenu(player);
                break;
            }
            case 34: {
                player.performCommand("enohud map fullscreen");
            }
        }
    }

    private void handleSettingsClick(Player player, MenuType menuType, int n, boolean bl) {
        try {
            if (n == 31) {
                this.openDisplayMenu(player);
                return;
            }
            if (n == 35) {
                this.openMainMenu(player);
                return;
            }
            Minimap minimap = this.ensureEnabledMinimap(player);
            if (minimap == null) {
                return;
            }
            boolean bl2 = true;
            block1 : switch (menuType.ordinal()) {
                case 2: {
                    switch (n) {
                        case 10: {
                            minimap.size(MinimapSize.SMALL);
                            break block1;
                        }
                        case 11: {
                            minimap.size(MinimapSize.MEDIUM);
                            break block1;
                        }
                        case 12: {
                            minimap.size(MinimapSize.LARGE);
                            break block1;
                        }
                        case 14: {
                            minimap.shape(MinimapShape.CIRCLE);
                            break block1;
                        }
                        case 15: {
                            minimap.shape(MinimapShape.SQUARE);
                            break block1;
                        }
                        case 16: {
                            minimap.screenPosition(minimap.screenPosition() == MinimapScreenPosition.RIGHT ? MinimapScreenPosition.LEFT : MinimapScreenPosition.RIGHT);
                            break block1;
                        }
                    }
                    bl2 = false;
                    break;
                }
                case 3: {
                    switch (n) {
                        case 10: 
                        case 11: 
                        case 12: {
                            minimap.colorProfile(n - 10);
                            break block1;
                        }
                        case 19: 
                        case 20: 
                        case 21: 
                        case 22: 
                        case 23: 
                        case 24: {
                            minimap.frameStyle(n - 19);
                            break block1;
                        }
                        case 25: {
                            minimap.frameFollowsTheme(true);
                            break block1;
                        }
                    }
                    bl2 = false;
                    break;
                }
                case 4: {
                    switch (n) {
                        case 13: {
                            minimap.radarMasterEnabled(!minimap.radarMasterEnabled());
                            break block1;
                        }
                        case 20: 
                        case 22: 
                        case 24: {
                            RadarCategory radarCategory = RadarCategory.values()[(n - 20) / 2];
                            minimap.radarEnabled(radarCategory, !minimap.radarEnabled(radarCategory));
                            break block1;
                        }
                    }
                    bl2 = false;
                    break;
                }
                case 5: {
                    switch (n) {
                        case 11: {
                            minimap.coordinatesEnabled(!minimap.coordinatesEnabled());
                            break block1;
                        }
                        case 15: {
                            minimap.combatZoomEnabled(!minimap.combatZoomEnabled());
                            break block1;
                        }
                    }
                    bl2 = false;
                    break;
                }
                case 6: {
                    switch (n) {
                        case 11: 
                        case 13: 
                        case 15: {
                            this.usePreset(player, (n - 11) / 2, bl);
                            break block1;
                        }
                    }
                    bl2 = false;
                    break;
                }
                default: {
                    bl2 = false;
                }
            }
            if (bl2) {
                minimap.respawn(this.plugin);
                this.plugin.entityRadar().refresh(minimap);
                this.plugin.menuListener().applyHiddenMarkers(minimap);
                this.plugin.playerDataStorage().save(minimap);
                this.openSettingsMenu(player, menuType);
            }
        }
        catch (Exception exception) {
            this.plugin.getLogger().warning("Minimap display update failed: " + exception.getMessage());
            player.sendMessage(String.valueOf(ChatColor.RED) + "Could not update display settings.");
        }
    }

    private void handleMarkerHubClick(Player player, int n) {
        try {
            switch (n) {
                case 10: {
                    this.markerWizard.open(player);
                    break;
                }
                case 12: {
                    this.openToggleMarkersMenu(player);
                    break;
                }
                case 14: {
                    this.openCategoryMenu(player);
                    break;
                }
                case 16: {
                    this.openRemoveMenu(player);
                    break;
                }
                case 31: {
                    this.openMainMenu(player);
                }
            }
        }
        catch (Exception exception) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Could not open marker menu.");
        }
    }

    private void handleRemoveClick(Player player, MenuHolder menuHolder, int n) {
        if (n == 49) {
            this.openMainMenu(player);
        } else {
            String string = menuHolder.markerName(n);
            if (string != null) {
                try {
                    this.removeMarker(player, string);
                    this.openRemoveMenu(player);
                }
                catch (Exception exception) {
                    this.plugin.getLogger().warning("Failed to remove marker '" + string + "' for " + player.getName() + ": " + exception.getMessage());
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Could not remove marker. See server console.");
                }
            }
        }
    }

    private void handleToggleMarkersClick(Player player, MenuHolder menuHolder, int n, boolean bl) {
        if (n == 49) {
            this.openMainMenu(player);
        } else {
            String string = menuHolder.markerName(n);
            if (string != null) {
                try {
                    if (bl) {
                        this.toggleMarkerPublicity(player, string);
                    } else {
                        this.toggleMarkerVisibility(player, string);
                    }
                    this.openToggleMarkersMenu(player);
                }
                catch (Exception exception) {
                    this.plugin.getLogger().warning("Failed to toggle marker '" + string + "' for " + player.getName() + ": " + exception.getMessage());
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Could not toggle marker. See server console.");
                }
            }
        }
    }

    private void toggleEnabled(Player player) throws Exception {
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        if (minimap == null) {
            this.plugin.playerDataStorage().enableMinimap(player);
            this.plugin.playerDataStorage().restore(this.plugin, player);
            minimap = this.plugin.getPlayerMinimap(player);
            if (minimap != null) {
                this.applyPublicMarkers(minimap);
                this.applyHiddenMarkers(minimap);
            }
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Minimap enabled.");
        } else {
            this.plugin.minimapListener().disableMinimap(player);
            this.plugin.playerDataStorage().disableMinimap(player);
            player.sendMessage(String.valueOf(ChatColor.RED) + "Minimap disabled.");
        }
    }

    private void setSize(Player player, MinimapSize minimapSize) throws Exception {
        Minimap minimap = this.ensureEnabledMinimap(player);
        if (minimap != null) {
            minimap.size(minimapSize);
            minimap.respawn(this.plugin);
            minimap.update(this.plugin);
            this.plugin.playerDataStorage().save(minimap);
            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Minimap size set to " + minimapSize.name() + ".");
        }
    }

    private void toggleDangerRing(Player player) throws Exception {
        Minimap minimap = this.ensureEnabledMinimap(player);
        if (minimap != null) {
            minimap.dangerRingEnabled(!minimap.dangerRingEnabled());
            minimap.respawn(this.plugin);
            minimap.update(this.plugin);
            this.plugin.playerDataStorage().save(minimap);
            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Danger ring " + (minimap.dangerRingEnabled() ? "enabled" : "disabled") + ".");
        }
    }

    private void toggleFogStyling(Player player) throws Exception {
        Minimap minimap = this.ensureEnabledMinimap(player);
        if (minimap != null) {
            minimap.fogStylingEnabled(!minimap.fogStylingEnabled());
            minimap.respawn(this.plugin);
            minimap.update(this.plugin);
            this.plugin.playerDataStorage().save(minimap);
            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Fog styling " + (minimap.fogStylingEnabled() ? "enabled" : "disabled") + ".");
        }
    }

    private void usePreset(Player player, int n, boolean bl) throws Exception {
        Minimap minimap = this.ensureEnabledMinimap(player);
        if (minimap != null) {
            if (bl) {
                MinimapPreset minimapPreset = MinimapPreset.capture(minimap);
                minimap.preset(n, minimapPreset);
                this.plugin.playerDataStorage().save(minimap);
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "Preset " + (n + 1) + " saved.");
            } else {
                MinimapPreset minimapPreset = minimap.preset(n);
                if (minimapPreset == null) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Preset " + (n + 1) + " is empty. Right click to save current settings.");
                } else {
                    minimapPreset.apply(minimap);
                    minimap.respawn(this.plugin);
                    minimap.update(this.plugin);
                    this.plugin.playerDataStorage().save(minimap);
                    player.sendMessage(String.valueOf(ChatColor.GREEN) + "Preset " + (n + 1) + " loaded.");
                }
            }
        }
    }

    private Minimap ensureEnabledMinimap(Player player) throws Exception {
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        if (minimap != null) {
            return minimap;
        }
        this.plugin.playerDataStorage().enableMinimap(player);
        this.plugin.playerDataStorage().restore(this.plugin, player);
        minimap = this.plugin.getPlayerMinimap(player);
        if (minimap == null) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Minimap could not be enabled.");
            return null;
        }
        this.applyPublicMarkers(minimap);
        this.applyHiddenMarkers(minimap);
        return minimap;
    }

    private void openRemoveMenu(Player player) throws Exception {
        Minimap minimap = this.ensureEnabledMinimap(player);
        if (minimap != null) {
            List<Map.Entry<String, SecondaryMinimapLayer>> list = this.ownedMarkers(minimap);
            if (list.isEmpty()) {
                player.sendMessage(String.valueOf(ChatColor.YELLOW) + "You have no custom markers to remove.");
                this.openMainMenu(player);
            } else {
                MenuHolder menuHolder = new MenuHolder(MenuType.REMOVE_MARKER);
                Inventory inventory = Bukkit.createInventory((InventoryHolder)menuHolder, (int)54, (String)REMOVE_TITLE);
                menuHolder.inventory(inventory);
                int n = 0;
                for (Map.Entry<String, SecondaryMinimapLayer> entry : list) {
                    if (n >= 45) break;
                    menuHolder.putMarker(n, entry.getKey());
                    inventory.setItem(n, this.markerItem(entry.getKey(), entry.getValue()));
                    ++n;
                }
                inventory.setItem(49, this.menuItem(Material.ARROW, String.valueOf(ChatColor.GREEN) + "Back", String.valueOf(ChatColor.GRAY) + "Return to main menu"));
                player.openInventory(inventory);
            }
        }
    }

    private void openToggleMarkersMenu(Player player) throws Exception {
        Minimap minimap = this.ensureEnabledMinimap(player);
        if (minimap != null) {
            List<Map.Entry<String, SecondaryMinimapLayer>> list = this.ownedMarkers(minimap);
            if (list.isEmpty()) {
                player.sendMessage(String.valueOf(ChatColor.YELLOW) + "You have no custom markers.");
                this.openMainMenu(player);
            } else {
                this.hiddenMarkers.computeIfAbsent(player.getUniqueId(), uUID -> new HashSet()).retainAll(list.stream().map(Map.Entry::getKey).toList());
                MenuHolder menuHolder = new MenuHolder(MenuType.TOGGLE_MARKERS);
                Inventory inventory = Bukkit.createInventory((InventoryHolder)menuHolder, (int)54, (String)MARKERS_TITLE);
                menuHolder.inventory(inventory);
                int n = 0;
                for (Map.Entry<String, SecondaryMinimapLayer> entry : list) {
                    if (n >= 45) break;
                    String string = entry.getKey();
                    menuHolder.putMarker(n, string);
                    inventory.setItem(n, this.markerToggleItem(player, string, entry.getValue()));
                    ++n;
                }
                inventory.setItem(49, this.menuItem(Material.ARROW, String.valueOf(ChatColor.GREEN) + "Back", String.valueOf(ChatColor.GRAY) + "Return to main menu"));
                player.openInventory(inventory);
            }
        }
    }

    private void openCategoryMenu(Player player) throws Exception {
        Minimap minimap = this.ensureEnabledMinimap(player);
        if (minimap != null) {
            MenuHolder menuHolder = new MenuHolder(MenuType.TOGGLE_CATEGORIES);
            Inventory inventory = Bukkit.createInventory((InventoryHolder)menuHolder, (int)54, (String)CATEGORIES_TITLE);
            menuHolder.inventory(inventory);
            MarkerCategory[] markerCategoryArray = new MarkerCategory[]{MarkerCategory.GENERAL, MarkerCategory.HOME, MarkerCategory.WARPS, MarkerCategory.PARTY, MarkerCategory.QUEST, MarkerCategory.DANGER};
            int[] nArray = new int[]{19, 20, 21, 22, 23, 24};
            for (int i = 0; i < markerCategoryArray.length; ++i) {
                MarkerCategory markerCategory = markerCategoryArray[i];
                boolean bl = this.isCategoryHidden(player, markerCategory);
                inventory.setItem(nArray[i], this.menuItem(bl ? Material.GRAY_DYE : Material.LIME_DYE, String.valueOf(bl ? ChatColor.RED : ChatColor.GREEN) + markerCategory.name(), String.valueOf(ChatColor.GRAY) + "Click to " + (bl ? "show" : "hide") + " this category"));
            }
            inventory.setItem(49, this.menuItem(Material.ARROW, String.valueOf(ChatColor.GREEN) + "Back", String.valueOf(ChatColor.GRAY) + "Return to main menu"));
            player.openInventory(inventory);
        }
    }

    private void handleToggleCategoriesClick(Player player, int n) {
        if (n == 49) {
            this.openMainMenu(player);
        } else {
            MarkerCategory markerCategory;
            switch (n) {
                case 19: {
                    MarkerCategory markerCategory2 = MarkerCategory.GENERAL;
                    break;
                }
                case 20: {
                    MarkerCategory markerCategory2 = MarkerCategory.HOME;
                    break;
                }
                case 21: {
                    MarkerCategory markerCategory2 = MarkerCategory.WARPS;
                    break;
                }
                case 22: {
                    MarkerCategory markerCategory2 = MarkerCategory.PARTY;
                    break;
                }
                case 23: {
                    MarkerCategory markerCategory2 = MarkerCategory.QUEST;
                    break;
                }
                case 24: {
                    MarkerCategory markerCategory2 = MarkerCategory.DANGER;
                    break;
                }
                default: {
                    MarkerCategory markerCategory2 = markerCategory = null;
                }
            }
            if (markerCategory != null) {
                try {
                    this.toggleCategoryVisibility(player, markerCategory);
                    this.openCategoryMenu(player);
                }
                catch (Exception exception) {
                    this.plugin.getLogger().warning("Failed to toggle category '" + String.valueOf((Object)markerCategory) + "' for " + player.getName() + ": " + exception.getMessage());
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Could not toggle marker category. See server console.");
                }
            }
        }
    }

    private List<Map.Entry<String, SecondaryMinimapLayer>> ownedMarkers(Minimap minimap) {
        return minimap.secondaryLayers().entrySet().stream().filter(entry -> this.isOwnedCustomMarker((String)entry.getKey())).sorted(Comparator.comparing(entry -> ((String)entry.getKey()).toLowerCase(Locale.ENGLISH))).toList();
    }

    private boolean isOwnedCustomMarker(String string) {
        return !Objects.equals(string, "player") && !Objects.equals(string, "death_point") && !Objects.equals(string, "__coords__") && !string.startsWith("__radar::") && !string.startsWith("__betonquest::") && !EnomapMenuListener.isPublicLayerKey(string);
    }

    private ItemStack markerItem(String string, SecondaryMinimapLayer secondaryMinimapLayer) {
        MinimapIconRenderer minimapIconRenderer;
        String string2 = "unknown";
        SecondaryMinimapLayerRenderer secondaryMinimapLayerRenderer = secondaryMinimapLayer.getRenderer();
        if (secondaryMinimapLayerRenderer instanceof MinimapIconRenderer && (minimapIconRenderer = (MinimapIconRenderer)secondaryMinimapLayerRenderer).icon() != null) {
            string2 = minimapIconRenderer.icon().key();
        }
        return this.menuItem(Material.RED_WOOL, String.valueOf(ChatColor.RED) + "Remove: " + string, String.valueOf(ChatColor.GRAY) + "Icon: " + string2, String.valueOf(ChatColor.GRAY) + "Click to delete this marker");
    }

    private void removeMarker(Player player, String string) throws Exception {
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        if (minimap == null) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Minimap is disabled.");
        } else {
            SecondaryMinimapLayer secondaryMinimapLayer = (SecondaryMinimapLayer)minimap.secondaryLayers().remove(string);
            if (secondaryMinimapLayer == null) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Marker '" + string + "' was not found.");
            } else {
                this.unpublishMarker(player.getUniqueId(), string);
                this.plugin.packetSender().despawnLayer(minimap.holder(), secondaryMinimapLayer.getBaseLayer());
                this.hiddenMarkers.computeIfAbsent(player.getUniqueId(), uUID -> new HashSet()).remove(string);
                minimap.update(this.plugin);
                this.plugin.playerDataStorage().save(minimap);
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "Marker '" + string + "' removed.");
            }
        }
    }

    private ItemStack markerToggleItem(Player player, String string, SecondaryMinimapLayer secondaryMinimapLayer) {
        MinimapIconRenderer minimapIconRenderer;
        boolean bl = this.isMarkerHidden(player, string);
        MarkerCategory markerCategory = this.markerCategory(secondaryMinimapLayer);
        boolean bl2 = this.isCategoryHidden(player, markerCategory);
        boolean bl3 = this.publicMarkers.containsKey(new PublicMarkerKey(player.getUniqueId(), string));
        String string2 = "unknown";
        SecondaryMinimapLayerRenderer secondaryMinimapLayerRenderer = secondaryMinimapLayer.getRenderer();
        if (secondaryMinimapLayerRenderer instanceof MinimapIconRenderer && (minimapIconRenderer = (MinimapIconRenderer)secondaryMinimapLayerRenderer).icon() != null) {
            string2 = minimapIconRenderer.icon().key();
        }
        return this.menuItem(!bl && !bl2 ? Material.LIME_DYE : Material.GRAY_DYE, String.valueOf(!bl && !bl2 ? ChatColor.GREEN : ChatColor.RED) + string + (!bl && !bl2 ? " [VISIBLE]" : " [HIDDEN]"), String.valueOf(ChatColor.GRAY) + "Icon: " + string2, String.valueOf(ChatColor.GRAY) + "Category: " + markerCategory.name(), String.valueOf(ChatColor.GRAY) + "Public: " + (bl3 ? String.valueOf(ChatColor.GREEN) + "YES" : String.valueOf(ChatColor.RED) + "NO"), String.valueOf(ChatColor.GRAY) + "Click = show/hide", String.valueOf(ChatColor.GRAY) + "Shift+Click = public/private");
    }

    public boolean isMarkerHidden(Player player, String string) {
        Set<String> set = this.hiddenMarkers.get(player.getUniqueId());
        return set != null && set.contains(string);
    }

    public boolean isCategoryHidden(Player player, MarkerCategory markerCategory) {
        Set<MarkerCategory> set = this.hiddenCategories.get(player.getUniqueId());
        return set != null && set.contains((Object)markerCategory);
    }

    private MarkerCategory markerCategory(SecondaryMinimapLayer secondaryMinimapLayer) {
        MarkerCategory markerCategory;
        if (secondaryMinimapLayer instanceof MarkerMinimapLayer) {
            MarkerMinimapLayer markerMinimapLayer = (MarkerMinimapLayer)secondaryMinimapLayer;
            markerCategory = markerMinimapLayer.getCategory();
        } else {
            markerCategory = MarkerCategory.GENERAL;
        }
        return markerCategory;
    }

    private boolean isLayerVisibleForPlayer(Player player, String string, SecondaryMinimapLayer secondaryMinimapLayer) {
        return this.isMarkerHidden(player, string) ? false : !this.isCategoryHidden(player, this.markerCategory(secondaryMinimapLayer));
    }

    public void toggleMarkerVisibility(Player player, String string) throws Exception {
        Minimap minimap = this.ensureEnabledMinimap(player);
        if (minimap != null) {
            SecondaryMinimapLayer secondaryMinimapLayer = minimap.secondaryLayers().get(string);
            if (secondaryMinimapLayer == null) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Marker '" + string + "' was not found.");
            } else {
                Set set = this.hiddenMarkers.computeIfAbsent(player.getUniqueId(), uUID -> new HashSet());
                if (!set.contains(string)) {
                    set.add(string);
                    this.plugin.packetSender().despawnLayer(minimap.holder(), secondaryMinimapLayer.getBaseLayer());
                    player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Marker '" + string + "' is now hidden.");
                } else {
                    set.remove(string);
                    if (this.isLayerVisibleForPlayer(player, string, secondaryMinimapLayer) && (secondaryMinimapLayer.getWorld() == null || secondaryMinimapLayer.getWorld().equals((Object)player.getWorld()))) {
                        this.plugin.packetSender().spawnLayer(minimap.holder(), secondaryMinimapLayer.getBaseLayer());
                    }
                    minimap.updateSecondaryLayer((MinimapProvider)this.plugin, string);
                    player.sendMessage(String.valueOf(ChatColor.GREEN) + "Marker '" + string + "' is now visible.");
                }
            }
        }
    }

    public void toggleCategoryVisibility(Player player, MarkerCategory markerCategory) throws Exception {
        Minimap minimap = this.ensureEnabledMinimap(player);
        if (minimap != null) {
            boolean bl;
            Set set = this.hiddenCategories.computeIfAbsent(player.getUniqueId(), uUID -> new HashSet());
            if (set.contains((Object)markerCategory)) {
                set.remove((Object)markerCategory);
                bl = false;
            } else {
                set.add(markerCategory);
                bl = true;
            }
            for (Map.Entry<String, SecondaryMinimapLayer> entry : minimap.secondaryLayers().entrySet()) {
                SecondaryMinimapLayer secondaryMinimapLayer;
                String string = entry.getKey();
                if (!this.isOwnedCustomMarker(string) && !EnomapMenuListener.isPublicLayerKey(string) || this.markerCategory(secondaryMinimapLayer = entry.getValue()) != markerCategory) continue;
                if (bl) {
                    this.plugin.packetSender().despawnLayer(minimap.holder(), secondaryMinimapLayer.getBaseLayer());
                    continue;
                }
                if (!this.isLayerVisibleForPlayer(player, string, secondaryMinimapLayer) || secondaryMinimapLayer.getWorld() != null && !secondaryMinimapLayer.getWorld().equals((Object)player.getWorld())) continue;
                this.plugin.packetSender().spawnLayer(minimap.holder(), secondaryMinimapLayer.getBaseLayer());
                minimap.updateSecondaryLayer((MinimapProvider)this.plugin, string);
            }
            player.sendMessage(String.valueOf(bl ? ChatColor.YELLOW : ChatColor.GREEN) + "Category " + markerCategory.name() + (bl ? " hidden." : " visible."));
        }
    }

    public void toggleMarkerPublicity(Player player, String string) throws Exception {
        HudAccess.require((CommandSender)player, "enorials.minimap.markers.share");
        Minimap minimap = this.ensureEnabledMinimap(player);
        if (minimap != null) {
            SecondaryMinimapLayer secondaryMinimapLayer = minimap.secondaryLayers().get(string);
            if (secondaryMinimapLayer == null) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Marker '" + string + "' was not found.");
            } else {
                PublicMarkerKey publicMarkerKey = new PublicMarkerKey(player.getUniqueId(), string);
                if (this.publicMarkers.containsKey(publicMarkerKey)) {
                    if (secondaryMinimapLayer instanceof MarkerMinimapLayer) {
                        MarkerMinimapLayer markerMinimapLayer = (MarkerMinimapLayer)secondaryMinimapLayer;
                        markerMinimapLayer.setPublic(false);
                    }
                    this.unpublishMarker(player.getUniqueId(), string);
                    this.plugin.playerDataStorage().save(minimap);
                    player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Marker '" + string + "' is now private.");
                } else {
                    MinimapIconRenderer minimapIconRenderer;
                    SecondaryMinimapLayerRenderer secondaryMinimapLayerRenderer = secondaryMinimapLayer.getRenderer();
                    if (secondaryMinimapLayerRenderer instanceof MinimapIconRenderer && (minimapIconRenderer = (MinimapIconRenderer)secondaryMinimapLayerRenderer).icon() != null) {
                        Object object;
                        if (secondaryMinimapLayer instanceof MarkerMinimapLayer) {
                            object = (MarkerMinimapLayer)secondaryMinimapLayer;
                            v0 = object;
                        } else {
                            v0 = secondaryMinimapLayerRenderer = null;
                        }
                        if (secondaryMinimapLayerRenderer != null) {
                            ((MarkerMinimapLayer)((Object)secondaryMinimapLayerRenderer)).setPublic(true);
                        }
                        this.plugin.playerDataStorage().save(minimap);
                        object = new PublicMarkerData(minimapIconRenderer.icon(), secondaryMinimapLayer.getWorld(), secondaryMinimapLayer.getPositionX(), secondaryMinimapLayer.getPositionZ(), secondaryMinimapLayer.isKeepOnEdge(), secondaryMinimapLayer.getDepth(), secondaryMinimapLayerRenderer != null ? ((MarkerMinimapLayer)((Object)secondaryMinimapLayerRenderer)).getMaxVisibleDistance() : null, this.markerCategory(secondaryMinimapLayer), secondaryMinimapLayerRenderer != null ? ((MarkerMinimapLayer)((Object)secondaryMinimapLayerRenderer)).getCreatedAt() : null, secondaryMinimapLayerRenderer != null ? ((MarkerMinimapLayer)((Object)secondaryMinimapLayerRenderer)).getExpiresAt() : null);
                        this.publicMarkers.put(publicMarkerKey, (PublicMarkerData)object);
                        for (Minimap minimap2 : this.plugin.minimapListener().getPlayerMinimaps().values()) {
                            if (minimap2.holder().getUniqueId().equals(player.getUniqueId())) continue;
                            this.addPublicLayer(minimap2, publicMarkerKey, (PublicMarkerData)object);
                        }
                        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Marker '" + string + "' is now public.");
                    } else {
                        player.sendMessage(String.valueOf(ChatColor.RED) + "Marker '" + string + "' cannot be published (missing icon renderer).");
                    }
                }
            }
        }
    }

    private void unpublishMarker(UUID uUID, String string) {
        PublicMarkerKey publicMarkerKey = new PublicMarkerKey(uUID, string);
        if (this.publicMarkers.remove(publicMarkerKey) != null) {
            for (Minimap minimap : this.plugin.minimapListener().getPlayerMinimaps().values()) {
                this.removePublicLayer(minimap, publicMarkerKey);
            }
        }
    }

    private void addPublicLayer(Minimap minimap, PublicMarkerKey publicMarkerKey, PublicMarkerData publicMarkerData) {
        String string = EnomapMenuListener.publicLayerKey(publicMarkerKey.owner(), publicMarkerKey.markerName());
        SecondaryMinimapLayer secondaryMinimapLayer = (SecondaryMinimapLayer)minimap.secondaryLayers().remove(string);
        if (secondaryMinimapLayer != null) {
            this.plugin.packetSender().despawnLayer(minimap.holder(), secondaryMinimapLayer.getBaseLayer());
        }
        MinimapLayer minimapLayer = this.plugin.clientsideMinimapFactory().createMinimapLayer(minimap.holder().getWorld(), null);
        MarkerMinimapLayer markerMinimapLayer = new MarkerMinimapLayer(minimapLayer, new MinimapIconRenderer(publicMarkerData.icon()), true, publicMarkerData.keepOnEdge(), publicMarkerData.world(), publicMarkerData.x(), publicMarkerData.z(), publicMarkerData.depth(), publicMarkerData.maxVisibleDistance(), publicMarkerData.category(), publicMarkerData.createdAt(), publicMarkerData.expiresAt());
        minimap.secondaryLayers().put(string, markerMinimapLayer);
        if (this.isLayerVisibleForPlayer(minimap.holder(), string, markerMinimapLayer) && (((SecondaryMinimapLayer)markerMinimapLayer).getWorld() == null || ((SecondaryMinimapLayer)markerMinimapLayer).getWorld().equals((Object)minimap.holder().getWorld()))) {
            this.plugin.packetSender().spawnLayer(minimap.holder(), minimapLayer);
            minimap.updateSecondaryLayer((MinimapProvider)this.plugin, string);
        }
    }

    private void removePublicLayer(Minimap minimap, PublicMarkerKey publicMarkerKey) {
        String string = EnomapMenuListener.publicLayerKey(publicMarkerKey.owner(), publicMarkerKey.markerName());
        SecondaryMinimapLayer secondaryMinimapLayer = (SecondaryMinimapLayer)minimap.secondaryLayers().remove(string);
        if (secondaryMinimapLayer != null) {
            this.plugin.packetSender().despawnLayer(minimap.holder(), secondaryMinimapLayer.getBaseLayer());
        }
    }

    public void applyPublicMarkers(Minimap minimap) {
        this.restoreOwnedPublicMarkers(minimap);
        UUID uUID = minimap.holder().getUniqueId();
        for (Map.Entry<PublicMarkerKey, PublicMarkerData> entry : this.publicMarkers.entrySet()) {
            if (entry.getKey().owner().equals(uUID)) continue;
            this.addPublicLayer(minimap, entry.getKey(), entry.getValue());
        }
    }

    public void restoreOwnedPublicMarkers(Minimap minimap) {
        for (Map.Entry<String, SecondaryMinimapLayer> entry : this.ownedMarkers(minimap)) {
            MarkerMinimapLayer markerMinimapLayer;
            Object object = entry.getValue();
            if (!(object instanceof MarkerMinimapLayer) || !(markerMinimapLayer = (MarkerMinimapLayer)object).isPublic() || !((object = markerMinimapLayer.getRenderer()) instanceof MinimapIconRenderer)) continue;
            MinimapIconRenderer minimapIconRenderer = (MinimapIconRenderer)object;
            this.registerPublic(minimap.holder().getUniqueId(), entry.getKey(), new PublicMarkerData(minimapIconRenderer.icon(), markerMinimapLayer.getWorld(), markerMinimapLayer.getPositionX(), markerMinimapLayer.getPositionZ(), markerMinimapLayer.isKeepOnEdge(), markerMinimapLayer.getDepth(), markerMinimapLayer.getMaxVisibleDistance(), markerMinimapLayer.getCategory(), markerMinimapLayer.getCreatedAt(), markerMinimapLayer.getExpiresAt()));
        }
    }

    private void registerPublic(UUID uUID, String string, PublicMarkerData publicMarkerData) {
        PublicMarkerKey publicMarkerKey = new PublicMarkerKey(uUID, string);
        PublicMarkerData publicMarkerData2 = this.publicMarkers.put(publicMarkerKey, publicMarkerData);
        if (publicMarkerData.equals(publicMarkerData2)) {
            return;
        }
        for (Minimap minimap : this.plugin.minimapListener().getPlayerMinimaps().values()) {
            if (minimap.holder().getUniqueId().equals(uUID)) continue;
            this.addPublicLayer(minimap, publicMarkerKey, publicMarkerData);
        }
    }

    public void loadStoredPublicMarkers() {
        try {
            for (DatabaseMarkerModel databaseMarkerModel : this.plugin.playerDataStorage().publicMarkers()) {
                MinimapIcon minimapIcon = this.plugin.iconProvider().getIcon(databaseMarkerModel.getIcon());
                Location location = databaseMarkerModel.getLocation();
                if (minimapIcon == null || location == null || location.getWorld() == null || databaseMarkerModel.getPlayer() == null) continue;
                this.registerPublic(databaseMarkerModel.getPlayer().getHolder(), databaseMarkerModel.getName(), new PublicMarkerData(minimapIcon, location.getWorld(), location.getBlockX(), location.getBlockZ(), Config.instance().markers.customMarkers.stickToBorder, 0.05f, databaseMarkerModel.getMaxVisibleDistance(), MarkerCategory.fromString(databaseMarkerModel.getCategory()), databaseMarkerModel.getCreatedAt(), databaseMarkerModel.getExpiresAt()));
            }
        }
        catch (Exception exception) {
            this.plugin.getLogger().warning("Could not load shared minimap markers: " + exception.getMessage());
        }
    }

    public void applyHiddenMarkers(Minimap minimap) {
        for (Map.Entry<String, SecondaryMinimapLayer> entry : minimap.secondaryLayers().entrySet()) {
            if (!this.isOwnedCustomMarker(entry.getKey()) && !EnomapMenuListener.isPublicLayerKey(entry.getKey()) || this.isLayerVisibleForPlayer(minimap.holder(), entry.getKey(), entry.getValue())) continue;
            this.plugin.packetSender().despawnLayer(minimap.holder(), entry.getValue().getBaseLayer());
        }
    }

    private record PublicMarkerKey(UUID owner, String markerName) {
    }

    private record PublicMarkerData(MinimapIcon icon, World world, int x, int z, boolean keepOnEdge, float depth, Integer maxVisibleDistance, MarkerCategory category, Long createdAt, Long expiresAt) {
    }

    private static final class MenuHolder
    implements InventoryHolder {
        private final MenuType type;
        private final Map<Integer, String> markerSlots = new HashMap<Integer, String>();
        private Inventory inventory;

        private MenuHolder(MenuType menuType) {
            this.type = menuType;
        }

        @NotNull
        public Inventory getInventory() {
            return this.inventory;
        }

        private void inventory(Inventory inventory) {
            this.inventory = inventory;
        }

        private MenuType type() {
            return this.type;
        }

        private void putMarker(int n, String string) {
            this.markerSlots.put(n, string);
        }

        private String markerName(int n) {
            return this.markerSlots.get(n);
        }
    }

    private static enum MenuType {
        MAIN,
        DISPLAY,
        LAYOUT,
        APPEARANCE,
        RADAR,
        BEHAVIOR,
        PRESETS,
        MARKER_HUB,
        REMOVE_MARKER,
        TOGGLE_MARKERS,
        TOGGLE_CATEGORIES;

    }
}

