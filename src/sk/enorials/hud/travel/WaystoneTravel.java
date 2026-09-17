/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.travel;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;

public final class WaystoneTravel
implements Listener {
    private final EnorialsHUD plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Set<UUID> pending = new HashSet<UUID>();

    public WaystoneTravel(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
        this.file = new File(enorialsHUD.getDataFolder(), "waystones.yml");
        this.data = YamlConfiguration.loadConfiguration((File)this.file);
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
    }

    public boolean command(CommandSender commandSender, String[] stringArray) {
        if (!HudAccess.has(commandSender, "enorials.gps.api")) {
            commandSender.sendMessage("No permission.");
            return true;
        }
        if (stringArray.length < 3) {
            commandSender.sendMessage("Usage: /gpsapi waystone <unlock|teleport|open|return> <player> [id]");
            return true;
        }
        Player player = Bukkit.getPlayerExact((String)stringArray[2]);
        if (player == null) {
            commandSender.sendMessage("Player not found.");
            return true;
        }
        String string = stringArray[1].toLowerCase(Locale.ROOT);
        if (string.equals("open")) {
            this.open(player);
            return true;
        }
        if (string.equals("return")) {
            String string2 = this.data.getString("players." + String.valueOf(player.getUniqueId()) + ".last");
            if (string2 == null || this.data.getConfigurationSection("waystones." + string2) == null) {
                player.sendMessage((Component)Component.text((String)"Discover a waystone before using a return scroll.", (TextColor)NamedTextColor.YELLOW));
            } else {
                this.teleport(player, string2, 2041);
            }
            return true;
        }
        if (stringArray.length != 4 || this.data.getConfigurationSection("waystones." + stringArray[3]) == null) {
            commandSender.sendMessage("Unknown waystone.");
            return true;
        }
        String string3 = stringArray[3];
        if (string.equals("unlock")) {
            String string4 = "players." + String.valueOf(player.getUniqueId());
            List list = this.data.getStringList(string4 + ".unlocked");
            if (!list.contains(string3)) {
                list.add(string3);
            }
            this.data.set(string4 + ".unlocked", (Object)list);
            this.data.set(string4 + ".last", (Object)string3);
            try {
                this.data.save(this.file);
            }
            catch (IOException iOException) {
                throw new IllegalStateException("Cannot persist waystone discovery", iOException);
            }
            player.sendMessage((Component)Component.text((String)("Waystone discovered: " + this.name(string3)), (TextColor)NamedTextColor.GOLD));
        } else if (string.equals("teleport")) {
            this.teleport(player, string3, 2041);
        } else {
            commandSender.sendMessage("Unknown waystone operation.");
        }
        return true;
    }

    private String name(String string) {
        return this.data.getString("waystones." + string + ".name", string);
    }

    private boolean unlocked(Player player, String string) {
        return this.data.getStringList("players." + String.valueOf(player.getUniqueId()) + ".unlocked").contains(string);
    }

    private void open(Player player) {
        int n;
        ConfigurationSection configurationSection = this.data.getConfigurationSection("waystones");
        if (configurationSection == null) {
            player.sendMessage("No waystones are configured.");
            return;
        }
        List<String> list = configurationSection.getKeys(false).stream().filter(string -> this.unlocked(player, (String)string)).sorted().toList();
        if (list.isEmpty()) {
            player.sendMessage((Component)Component.text((String)"Discover a waystone before using a teleport scroll.", (TextColor)NamedTextColor.YELLOW));
            return;
        }
        TravelMenu travelMenu = new TravelMenu(player.getUniqueId(), list);
        travelMenu.inventory = Bukkit.createInventory((InventoryHolder)travelMenu, (int)27, (Component)Component.text((String)"Enorials Waystones"));
        ItemStack itemStack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName((Component)Component.empty());
        itemStack.setItemMeta(itemMeta);
        for (n = 0; n < 27; ++n) {
            travelMenu.inventory.setItem(n, itemStack);
        }
        for (n = 0; n < Math.min(7, list.size()); ++n) {
            ItemStack itemStack2 = new ItemStack(Material.ENDER_PEARL);
            ItemMeta itemMeta2 = itemStack2.getItemMeta();
            itemMeta2.displayName((Component)Component.text((String)this.name(list.get(n)), (TextColor)NamedTextColor.GOLD));
            itemMeta2.lore(List.of(Component.text((String)"Teleport using one scroll", (TextColor)NamedTextColor.GRAY)));
            itemStack2.setItemMeta(itemMeta2);
            travelMenu.inventory.setItem(10 + n, itemStack2);
        }
        player.openInventory(travelMenu.inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent inventoryClickEvent) {
        InventoryHolder inventoryHolder = inventoryClickEvent.getView().getTopInventory().getHolder();
        if (!(inventoryHolder instanceof TravelMenu)) {
            return;
        }
        TravelMenu travelMenu = (TravelMenu)inventoryHolder;
        inventoryClickEvent.setCancelled(true);
        HumanEntity humanEntity = inventoryClickEvent.getWhoClicked();
        if (!(humanEntity instanceof Player) || !travelMenu.player.equals((inventoryHolder = (Player)humanEntity).getUniqueId())) {
            return;
        }
        int n = inventoryClickEvent.getRawSlot() - 10;
        if (n < 0 || n >= Math.min(7, travelMenu.ids.size())) {
            return;
        }
        inventoryHolder.closeInventory();
        this.teleport((Player)inventoryHolder, travelMenu.ids.get(n), 2042);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent inventoryDragEvent) {
        if (inventoryDragEvent.getView().getTopInventory().getHolder() instanceof TravelMenu) {
            inventoryDragEvent.setCancelled(true);
        }
    }

    private void teleport(Player player, String string, int n) {
        if (!this.unlocked(player, string)) {
            player.sendMessage("Discover this waystone first.");
            return;
        }
        String string2 = "waystones." + string;
        World world = Bukkit.getWorld((String)this.data.getString(string2 + ".world", "world"));
        if (world == null) {
            player.sendMessage("The destination world is unavailable.");
            return;
        }
        if (this.plugin.packLoadGuard() != null && this.plugin.packLoadGuard().isWaiting(player)) {
            player.sendMessage("Please wait until your resource pack finishes loading.");
            return;
        }
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() != Material.FEATHER || !itemStack.hasItemMeta() || !itemStack.getItemMeta().hasCustomModelData() || itemStack.getItemMeta().getCustomModelData() != n || itemStack.getAmount() < 1) {
            player.sendMessage("Hold the matching teleport scroll in your main hand.");
            return;
        }
        if (!this.pending.add(player.getUniqueId())) {
            return;
        }
        ItemStack itemStack2 = itemStack.clone();
        itemStack2.setAmount(1);
        itemStack.subtract(1);
        Location location = new Location(world, this.data.getDouble(string2 + ".x"), this.data.getDouble(string2 + ".y"), this.data.getDouble(string2 + ".z"), (float)this.data.getDouble(string2 + ".yaw"), (float)this.data.getDouble(string2 + ".pitch"));
        try {
            player.teleportAsync(location).whenComplete((bl, throwable) -> Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                this.pending.remove(player.getUniqueId());
                if (throwable != null || !Boolean.TRUE.equals(bl)) {
                    this.refund(player, itemStack2);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                }
            }));
        }
        catch (RuntimeException runtimeException) {
            this.pending.remove(player.getUniqueId());
            this.refund(player, itemStack2);
            throw runtimeException;
        }
    }

    private void refund(Player player, ItemStack itemStack) {
        for (ItemStack itemStack2 : player.getInventory().addItem(new ItemStack[]{itemStack}).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), itemStack2);
        }
        player.sendMessage("Teleport cancelled; your scroll was returned.");
    }

    private static final class TravelMenu
    implements InventoryHolder {
        final UUID player;
        final List<String> ids;
        Inventory inventory;

        TravelMenu(UUID uUID, List<String> list) {
            this.player = uUID;
            this.ids = list;
        }

        public Inventory getInventory() {
            return this.inventory;
        }
    }
}

