/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.World
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.minimap.menu;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.compat.ClientRenderProfile;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapProvider;
import sk.enorials.minimap.map.icon.MarkerIcons;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.marker.MarkerCategory;
import sk.enorials.minimap.map.marker.MarkerLimits;
import sk.enorials.minimap.map.marker.MarkerMinimapLayer;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.minimap.menu.EnomapMenuListener;

public final class MarkerWizard
implements Listener {
    private final EnorialsMinimap plugin;
    private final EnomapMenuListener menus;
    private final Map<UUID, Draft> drafts = new ConcurrentHashMap<UUID, Draft>();
    private static final Integer[] RANGES = new Integer[]{null, 64, 128, 256, 512, 1024, 2048, 50000};
    private static final Integer[] LIFETIMES = new Integer[]{null, 30, 60, 120, 1440};
    private static final int[] ICON_SLOTS = new int[]{10, 13, 16, 28, 31, 34};

    public MarkerWizard(EnorialsMinimap enorialsMinimap, EnomapMenuListener enomapMenuListener) {
        this.plugin = enorialsMinimap;
        this.menus = enomapMenuListener;
    }

    public void open(Player player) {
        Draft draft = new Draft();
        this.drafts.put(player.getUniqueId(), draft);
        this.openCategories(player, draft);
    }

    private Inventory inventory(Draft draft, Step step, int n, String string) {
        Inventory inventory;
        draft.step = step;
        Holder holder = new Holder(draft, step);
        holder.inventory = inventory = Bukkit.createInventory((InventoryHolder)holder, (int)n, (String)(String.valueOf(ChatColor.AQUA) + string));
        for (int i = 0; i < n; ++i) {
            inventory.setItem(i, MarkerWizard.item(Material.GRAY_STAINED_GLASS_PANE, " ", new String[0]));
        }
        inventory.setItem(n - 5, MarkerWizard.item(Material.BARRIER, "Cancel", "Discard this marker draft"));
        return inventory;
    }

    private void openCategories(Player player, Draft draft) {
        Inventory inventory = this.inventory(draft, Step.CATEGORY, 45, "Marker - Category");
        MarkerCategory[] markerCategoryArray = MarkerCategory.values();
        for (int i = 0; i < markerCategoryArray.length; ++i) {
            MarkerCategory markerCategory = markerCategoryArray[i];
            Material material = switch (markerCategory) {
                case MarkerCategory.HOME -> Material.RED_BED;
                case MarkerCategory.WARPS -> Material.ENDER_PEARL;
                case MarkerCategory.QUEST -> Material.WRITABLE_BOOK;
                case MarkerCategory.PARTY -> Material.PLAYER_HEAD;
                case MarkerCategory.DANGER -> Material.REDSTONE;
                default -> Material.MAP;
            };
            inventory.setItem(10 + i, MarkerWizard.item(material, markerCategory.name(), "Click to select category"));
        }
        player.openInventory(inventory);
    }

    private void openIcons(Player player, Draft draft) {
        draft.icons = MarkerIcons.selectable(this.plugin.iconProvider());
        int n = Math.max(1, (draft.icons.size() + 5) / 6);
        draft.page = Math.max(0, Math.min(draft.page, n - 1));
        Inventory inventory = this.inventory(draft, Step.ICON, 54, "Marker - Icon " + (draft.page + 1) + "/" + n);
        for (int i = 0; i < 6 && draft.page * 6 + i < draft.icons.size(); ++i) {
            String string = draft.icons.get(draft.page * 6 + i);
            inventory.setItem(ICON_SLOTS[i], MarkerWizard.iconItem(string, MarkerIcons.label(string), "Click to use this icon"));
        }
        if (draft.page > 0) {
            inventory.setItem(45, MarkerWizard.item(Material.ARROW, "Previous page", new String[0]));
        }
        inventory.setItem(47, MarkerWizard.item(Material.ARROW, "Back", "Return to category selection", "Category: " + String.valueOf((Object)draft.category)));
        if (draft.page + 1 < n) {
            inventory.setItem(53, MarkerWizard.item(Material.ARROW, "Next page", new String[0]));
        }
        player.openInventory(inventory);
    }

    private void openOptions(Player player, Draft draft) {
        Inventory inventory = this.inventory(draft, Step.OPTIONS, 45, "Marker - Options");
        inventory.setItem(10, MarkerWizard.iconItem(draft.icon, "Icon: " + MarkerIcons.label(draft.icon), "Click to choose another icon"));
        inventory.setItem(12, MarkerWizard.item(draft.publicMarker ? Material.ENDER_EYE : Material.CHEST, "Visibility: " + (draft.publicMarker ? "PUBLIC" : "PRIVATE"), "Click to switch", "Public markers are shared with other players"));
        inventory.setItem(14, MarkerWizard.item(Material.COMPASS, "Visible range: " + MarkerWizard.rangeText(draft.rangeIndex), "Click to cycle maximum distance"));
        inventory.setItem(16, MarkerWizard.item(Material.CLOCK, "Expires: " + MarkerWizard.lifetimeText(draft.lifetimeIndex), "Click to cycle optional expiry"));
        inventory.setItem(22, MarkerWizard.item(Material.NAME_TAG, (String)(draft.name == null ? "Name this marker" : "Name: " + draft.name), "Click to type the name privately in chat"));
        inventory.setItem(30, MarkerWizard.item(Material.ARROW, "Back", "Choose another icon"));
        inventory.setItem(32, MarkerWizard.item(Material.LIME_DYE, "Next", "Choose marker name and location"));
        player.openInventory(inventory);
    }

    private void openLocation(Player player, Draft draft) {
        Inventory inventory = this.inventory(draft, Step.LOCATION, 45, "Marker - Location");
        Location location = draft.location != null ? draft.location : player.getLocation();
        inventory.setItem(10, MarkerWizard.item(Material.COMPASS, "Current position", "Use where you stand when you confirm", "Click to use your current position"));
        inventory.setItem(12, MarkerWizard.item(Material.PAPER, "Enter coordinates", "Type x y z in chat, or x y z world", "Relative coordinates such as ~10 are supported"));
        inventory.setItem(14, MarkerWizard.item(Material.NAME_TAG, "Name: " + draft.name, "Click to edit the name"));
        inventory.setItem(16, MarkerWizard.iconItem(draft.icon, String.valueOf((Object)draft.category) + " / " + MarkerIcons.label(draft.icon), draft.publicMarker ? "PUBLIC" : "PRIVATE", "Range: " + MarkerWizard.rangeText(draft.rangeIndex), "Expires: " + MarkerWizard.lifetimeText(draft.lifetimeIndex)));
        inventory.setItem(22, MarkerWizard.item(Material.FILLED_MAP, "Location: " + location.getWorld().getName(), location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ(), draft.location == null ? "Uses your current position" : "Fixed coordinates"));
        inventory.setItem(30, MarkerWizard.item(Material.ARROW, "Back", "Return to marker options"));
        inventory.setItem(32, MarkerWizard.item(Material.LIME_DYE, "Create Marker", "Save this marker"));
        player.openInventory(inventory);
    }

    private void ask(Player player, Draft draft, Step step) {
        draft.step = step;
        player.closeInventory();
        player.sendMessage(String.valueOf(ChatColor.AQUA) + (step == Step.NAME ? "Type your marker name in chat (1-32 characters), or cancel." : "Type x y z [world] in chat, or cancel. Example: ~10 ~ ~-5"));
    }

    @EventHandler
    public void onClick(InventoryClickEvent inventoryClickEvent) {
        Player player;
        Object object;
        block32: {
            block31: {
                object = inventoryClickEvent.getWhoClicked();
                if (!(object instanceof Player)) break block31;
                player = (Player)object;
                object = inventoryClickEvent.getView().getTopInventory().getHolder();
                if (object instanceof Holder) break block32;
            }
            return;
        }
        Holder holder = (Holder)object;
        inventoryClickEvent.setCancelled(true);
        if (inventoryClickEvent.getClickedInventory() != inventoryClickEvent.getView().getTopInventory()) {
            return;
        }
        object = this.drafts.get(player.getUniqueId());
        if (object == null || holder.draft != object) {
            return;
        }
        int n = inventoryClickEvent.getSlot();
        if (n == holder.inventory.getSize() - 5) {
            this.cancel(player);
            return;
        }
        block0 : switch (holder.step.ordinal()) {
            case 0: {
                int n3 = n - 10;
                if (n3 < 0 || n3 >= MarkerCategory.values().length) break;
                object.category = MarkerCategory.values()[n3];
                this.openIcons(player, (Draft)object);
                break;
            }
            case 1: {
                if (n == 45) {
                    --object.page;
                    this.openIcons(player, (Draft)object);
                    break;
                }
                if (n == 53) {
                    ++object.page;
                    this.openIcons(player, (Draft)object);
                    break;
                }
                if (n == 47) {
                    this.openCategories(player, (Draft)object);
                    break;
                }
                int n4 = IntStream.range(0, ICON_SLOTS.length).filter(n2 -> ICON_SLOTS[n2] == n).findFirst().orElse(-1);
                int n5 = object.page * 6 + n4;
                if (n4 < 0 || n5 >= object.icons.size()) break;
                object.icon = object.icons.get(n5);
                this.openOptions(player, (Draft)object);
                break;
            }
            case 2: {
                switch (n) {
                    case 10: 
                    case 30: {
                        this.openIcons(player, (Draft)object);
                        break;
                    }
                    case 12: {
                        object.publicMarker = !object.publicMarker;
                        this.openOptions(player, (Draft)object);
                        break;
                    }
                    case 14: {
                        object.rangeIndex = (object.rangeIndex + 1) % RANGES.length;
                        this.openOptions(player, (Draft)object);
                        break;
                    }
                    case 16: {
                        object.lifetimeIndex = (object.lifetimeIndex + 1) % LIFETIMES.length;
                        this.openOptions(player, (Draft)object);
                        break;
                    }
                    case 22: {
                        this.ask(player, (Draft)object, Step.NAME);
                        break;
                    }
                    case 32: {
                        if (object.name == null) {
                            this.ask(player, (Draft)object, Step.NAME);
                            break;
                        }
                        this.openLocation(player, (Draft)object);
                    }
                }
                break;
            }
            case 5: {
                switch (n) {
                    case 10: {
                        object.location = null;
                        this.openLocation(player, (Draft)object);
                        break block0;
                    }
                    case 12: {
                        this.ask(player, (Draft)object, Step.COORDINATES);
                        break block0;
                    }
                    case 14: {
                        this.ask(player, (Draft)object, Step.NAME);
                        break block0;
                    }
                    case 30: {
                        this.openOptions(player, (Draft)object);
                        break block0;
                    }
                    case 32: {
                        this.finish(player, (Draft)object);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent inventoryDragEvent) {
        if (inventoryDragEvent.getView().getTopInventory().getHolder() instanceof Holder) {
            inventoryDragEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onChat(AsyncChatEvent asyncChatEvent) {
        UUID uUID = asyncChatEvent.getPlayer().getUniqueId();
        Draft draft = this.drafts.get(uUID);
        if (draft == null || draft.step != Step.NAME && draft.step != Step.COORDINATES) {
            return;
        }
        Step step = draft.step;
        String string = PlainTextComponentSerializer.plainText().serialize(asyncChatEvent.message()).trim();
        asyncChatEvent.setCancelled(true);
        Bukkit.getScheduler().runTask((Plugin)this.plugin.owner(), () -> {
            if (this.drafts.get(uUID) != draft || draft.step != step || !asyncChatEvent.getPlayer().isOnline()) {
                return;
            }
            this.acceptInput(asyncChatEvent.getPlayer(), string);
        });
    }

    public boolean isAwaitingInput(Player player) {
        Draft draft = this.drafts.get(player.getUniqueId());
        return draft != null && (draft.step == Step.NAME || draft.step == Step.COORDINATES);
    }

    public void acceptInput(Player player, String string) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Marker draft input must be handled on the server thread");
        }
        Draft draft = this.drafts.get(player.getUniqueId());
        if (draft == null || draft.step != Step.NAME && draft.step != Step.COORDINATES) {
            return;
        }
        Step step = draft.step;
        if (string.equalsIgnoreCase("cancel")) {
            this.cancel(player);
            return;
        }
        if (step == Step.NAME) {
            if (!MarkerWizard.validName(string)) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Use 1-32 letters, numbers, spaces, _ . or -. Type cancel to exit.");
                return;
            }
            draft.name = string;
            this.openLocation(player, draft);
        } else {
            try {
                World world;
                String[] stringArray = string.split("\\s+");
                if (stringArray.length != 3 && stringArray.length != 4) {
                    throw new IllegalArgumentException("Use x y z [world].");
                }
                Location location = player.getLocation();
                World world2 = world = stringArray.length == 4 ? Bukkit.getWorld((String)stringArray[3]) : location.getWorld();
                if (world == null) {
                    throw new IllegalArgumentException("That world is not loaded.");
                }
                double d = MarkerWizard.coordinate(stringArray[0], location.getX());
                double d2 = MarkerWizard.coordinate(stringArray[1], location.getY());
                double d3 = MarkerWizard.coordinate(stringArray[2], location.getZ());
                if (!Double.isFinite(d) || !Double.isFinite(d2) || !Double.isFinite(d3) || Math.abs(d) >= 3.0E7 || Math.abs(d3) >= 3.0E7 || d2 < (double)world.getMinHeight() || d2 >= (double)world.getMaxHeight()) {
                    throw new IllegalArgumentException("Coordinates are outside this world's bounds.");
                }
                draft.location = new Location(world, d, d2, d3);
                this.openLocation(player, draft);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                player.sendMessage(String.valueOf(ChatColor.RED) + illegalArgumentException.getMessage() + " Type cancel to exit.");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        this.drafts.remove(playerQuitEvent.getPlayer().getUniqueId());
    }

    private void cancel(Player player) {
        this.drafts.remove(player.getUniqueId());
        player.sendMessage(String.valueOf(ChatColor.GRAY) + "Marker creation cancelled.");
        this.menus.openMarkerMenu(player);
    }

    private void finish(Player player, Draft draft) {
        try {
            Location location = draft.location != null ? draft.location.clone() : player.getLocation();
            Integer n = LIFETIMES[draft.lifetimeIndex];
            this.create(player, draft.name, draft.icon, draft.category, draft.publicMarker, RANGES[draft.rangeIndex], n == null ? null : Long.valueOf(System.currentTimeMillis() + (long)n.intValue() * 60000L), location);
            this.drafts.remove(player.getUniqueId());
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Marker '" + draft.name + "' created.");
            this.menus.openMarkerMenu(player);
        }
        catch (Exception exception) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Could not create marker: " + exception.getMessage());
        }
    }

    public void create(Player player, String string, String string2, MarkerCategory markerCategory, boolean bl, Integer n, Long l, Location location) throws Exception {
        HudAccess.require((CommandSender)player, "enorials.minimap.markers.use");
        if (bl) {
            HudAccess.require((CommandSender)player, "enorials.minimap.markers.share");
        }
        if (n == null || n <= 0) {
            HudAccess.require((CommandSender)player, "enorials.minimap.markers.unlimited");
        }
        if (!MarkerWizard.validName(string)) {
            throw new IllegalArgumentException("Invalid marker name.");
        }
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        if (minimap == null) {
            throw new IllegalStateException("Enable your minimap first.");
        }
        if (minimap.secondaryLayers().containsKey(string)) {
            throw new IllegalArgumentException("A marker with that name already exists.");
        }
        long l2 = minimap.secondaryLayers().entrySet().stream().filter(entry -> entry.getValue() instanceof MarkerMinimapLayer && MarkerLimits.owned((String)entry.getKey())).count();
        if (l2 >= (long)Config.instance().markers.customMarkers.limit) {
            throw new IllegalArgumentException("You have reached your marker limit.");
        }
        if (!(n != null && n > 0 || MarkerLimits.canSetUnlimited(minimap, string))) {
            throw new IllegalArgumentException(MarkerLimits.message());
        }
        MinimapIcon minimapIcon = this.plugin.iconProvider().getIcon(string2);
        if (minimapIcon == null || this.plugin.iconProvider().specialIconKeys().contains(string2)) {
            throw new IllegalArgumentException("Unknown marker icon.");
        }
        MinimapLayer minimapLayer = this.plugin.clientsideMinimapFactory().createMinimapLayer(player.getWorld(), null);
        MarkerMinimapLayer markerMinimapLayer = new MarkerMinimapLayer(minimapLayer, new MinimapIconRenderer(minimapIcon), true, Config.instance().markers.customMarkers.stickToBorder, location.getWorld(), location.getBlockX(), location.getBlockZ(), 0.05f + (float)l2 * 0.01f, n, markerCategory, System.currentTimeMillis(), l);
        markerMinimapLayer.setPublic(bl);
        minimap.secondaryLayers().put(string, markerMinimapLayer);
        try {
            this.plugin.playerDataStorage().save(minimap);
        }
        catch (Exception exception) {
            minimap.secondaryLayers().remove(string);
            throw exception;
        }
        if (location.getWorld().equals((Object)player.getWorld())) {
            this.plugin.packetSender().spawnLayer(player, minimapLayer);
            minimap.updateSecondaryLayer((MinimapProvider)this.plugin, string);
        }
        this.menus.restoreOwnedPublicMarkers(minimap);
        this.menus.applyHiddenMarkers(minimap);
    }

    private static boolean validName(String string) {
        return string != null && string.matches("[\\p{L}\\p{N} _.\\-]{1,32}") && !string.startsWith("__") && !string.equalsIgnoreCase("player") && !string.equalsIgnoreCase("death_point") && !string.isBlank();
    }

    private static double coordinate(String string, double d) {
        return string.startsWith("~") ? d + (string.length() == 1 ? 0.0 : Double.parseDouble(string.substring(1))) : Double.parseDouble(string);
    }

    private static String rangeText(int n) {
        return RANGES[n] == null ? "Unlimited" : RANGES[n] + " blocks";
    }

    private static String lifetimeText(int n) {
        return LIFETIMES[n] == null ? "Never" : LIFETIMES[n] + " minutes";
    }

    private static ItemStack item(Material material, String string2, String ... stringArray) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(String.valueOf(ChatColor.AQUA) + string2);
        itemMeta.setLore(Arrays.stream(stringArray).map(string -> String.valueOf(ChatColor.GRAY) + string).toList());
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private static ItemStack iconItem(String string, String string2, String ... stringArray) {
        ItemStack itemStack = MarkerWizard.item(Material.PAPER, string2, stringArray);
        if (MarkerIcons.BUILTIN.contains(string)) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setCustomModelData(Integer.valueOf(950000 + MarkerIcons.BUILTIN.indexOf(string)));
            if (ClientRenderProfile.nativeProfile().itemModel()) {
                try {
                    ItemMeta.class.getMethod("setItemModel", NamespacedKey.class).invoke((Object)itemMeta, NamespacedKey.fromString((String)("enorials:marker/" + MarkerIcons.textureKey(string))));
                }
                catch (ReflectiveOperationException reflectiveOperationException) {
                    throw new IllegalStateException("Server item-model API is unavailable", reflectiveOperationException);
                }
            }
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private static final class Draft {
        volatile Step step;
        MarkerCategory category = MarkerCategory.GENERAL;
        String icon;
        String name;
        boolean publicMarker;
        int rangeIndex;
        int lifetimeIndex;
        int page;
        Location location;
        List<String> icons = List.of();

        private Draft() {
        }
    }

    private static enum Step {
        CATEGORY,
        ICON,
        OPTIONS,
        NAME,
        COORDINATES,
        LOCATION;

    }

    private static final class Holder
    implements InventoryHolder {
        final Draft draft;
        final Step step;
        Inventory inventory;

        Holder(Draft draft, Step step) {
            this.draft = draft;
            this.step = step;
        }

        public Inventory getInventory() {
            return this.inventory;
        }
    }
}

