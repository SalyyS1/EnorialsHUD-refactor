/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.permissions.Permission
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.hud.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.menu.GraphicalHudMenu;
import sk.enorials.hud.menu.HudMenu;
import sk.enorials.hud.menu.MenuGeometry;

final class HudAdminMenu
implements Listener,
AutoCloseable {
    private final EnorialsHUD hud;
    private final HudMenu menu;
    private final BukkitTask guard;

    HudAdminMenu(EnorialsHUD enorialsHUD, HudMenu hudMenu) {
        this.hud = enorialsHUD;
        this.menu = hudMenu;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
        this.guard = Bukkit.getScheduler().runTaskTimer((Plugin)enorialsHUD, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof Inspection) || this.allowed(player)) continue;
                player.closeInventory();
            }
        }, 20L, 20L);
    }

    private boolean allowed(Player player) {
        return HudAccess.has((CommandSender)player, "enorials.hud.admin");
    }

    private void require(Player player) {
        HudAccess.require((CommandSender)player, "enorials.hud.admin");
    }

    private GraphicalHudMenu.Button button(Player player, String string, String string2, Runnable runnable) {
        return new GraphicalHudMenu.Button(string, string2, () -> {
            if (HudAccess.check((CommandSender)player, "enorials.hud.admin")) {
                runnable.run();
            }
        });
    }

    private void show(Player player, String string, List<GraphicalHudMenu.Button> list, Runnable runnable) {
        if (HudAccess.check((CommandSender)player, "enorials.hud.admin")) {
            this.menu.graphical().open(player, string, list, runnable);
        }
    }

    void open(Player player) {
        this.show(player, "Admin", List.of(this.button(player, "atlas:search", "Players", () -> this.players(player)), this.button(player, "atlas:permissions", "Default access", () -> this.defaults(player)), this.button(player, "atlas:reset", "Reload access", () -> this.hud.access().command((CommandSender)player, new String[]{"reload"}))), () -> this.menu.open(player));
    }

    private void players(Player player) {
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        arrayList.add(this.button(player, "atlas:search", "Find player", () -> this.menu.ask(player, "Enter a player name or UUID. Example: Steve. Previously joined offline players are supported.", string -> {
            OfflinePlayer offlinePlayer;
            this.require(player);
            try {
                offlinePlayer = Bukkit.getOfflinePlayer((UUID)UUID.fromString(string));
            }
            catch (IllegalArgumentException illegalArgumentException) {
                if (!string.matches("[A-Za-z0-9_]{1,16}")) {
                    throw new IllegalArgumentException("Enter a player name (1-16 letters, digits or underscores), or a UUID.");
                }
                offlinePlayer = Bukkit.getOfflinePlayerIfCached((String)string);
            }
            if (offlinePlayer == null || !offlinePlayer.isOnline() && !offlinePlayer.hasPlayedBefore()) {
                throw new IllegalArgumentException("This player has not joined this server. Check the name or UUID.");
            }
            this.player(player, offlinePlayer.getUniqueId());
        }, () -> this.players(player))));
        Bukkit.getOnlinePlayers().stream().filter(arg_0 -> ((Player)player).canSee(arg_0)).sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER)).forEach(player2 -> arrayList.add(this.button(player, "head:" + String.valueOf(player2.getUniqueId()), player2.getName(), () -> this.player(player, player2.getUniqueId()))));
        this.show(player, "Admin players", arrayList, () -> this.open(player));
    }

    private Player online(UUID uUID) {
        Player player = Bukkit.getPlayer((UUID)uUID);
        if (player == null) {
            throw new IllegalArgumentException("This action needs the player to be online. Offline permissions can still be edited.");
        }
        return player;
    }

    private String name(UUID uUID) {
        return Objects.requireNonNullElse(Bukkit.getOfflinePlayer((UUID)uUID).getName(), uUID.toString().substring(0, 8));
    }

    private void player(Player player, UUID uUID) {
        this.show(player, this.name(uUID), List.of(this.button(player, "atlas:permissions", "Permissions", () -> this.permissions(player, uUID)), this.button(player, "atlas:inventory", "View inventory", () -> this.inspect(player, uUID)), this.button(player, "atlas:teleport", "Teleport to", () -> {
            Player player2 = this.online(uUID);
            player.closeInventory();
            player.teleportAsync(player2.getLocation());
        }), this.button(player, "atlas:teleport", "Bring here", () -> this.confirm(player, "Bring " + this.name(uUID) + "?", () -> {
            this.online(uUID).teleportAsync(player.getLocation());
            this.player(player, uUID);
        }, () -> this.player(player, uUID))), this.button(player, "close", "Stop GPS", () -> {
            this.hud.gps().navigation().stopNavigation(this.online(uUID), "admin");
            this.player(player, uUID);
        }), this.button(player, "atlas:player", "HUD controls", () -> this.controls(player, uUID))), () -> this.players(player));
    }

    private void confirm(Player player, String string, Runnable runnable, Runnable runnable2) {
        this.show(player, string, List.of(this.button(player, "atlas:grant", "Confirm", runnable), this.button(player, "back", "Cancel", runnable2)), runnable2);
    }

    private void controls(Player player, UUID uUID) {
        Player player2 = this.online(uUID);
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        for (String[] stringArray : new String[][]{{"hud", "Player HUD"}, {"party", "Party HUD"}, {"effects", "Effect icons"}, {"quest-log", "Quest log"}, {"conversations", "NPC dialogues"}, {"betonquest-markers", "Quest markers"}}) {
            if (!Bukkit.getPluginManager().isPluginEnabled("BetonQuest") && Set.of("quest-log", "conversations", "betonquest-markers").contains(stringArray[0])) continue;
            boolean bl = this.hud.storage().preference(uUID, stringArray[0], !stringArray[0].equals("quest-log"));
            arrayList.add(this.button(player, "atlas:player", stringArray[1] + "\n" + (bl ? "On" : "Off"), () -> {
                this.online(uUID);
                this.hud.storage().setPreference(uUID, stringArray[0], !bl);
                this.controls(player, uUID);
            }).as(MenuGeometry.Kind.TOGGLE));
        }
        this.show(player, "Player HUD controls", arrayList, () -> this.player(player, uUID));
    }

    private void permissions(Player player, UUID uUID) {
        this.show(player, "Player permissions", List.of(this.button(player, "atlas:grant", "Grant permission", () -> this.inputPermission(player, uUID, "allow")), this.button(player, "atlas:deny", "Deny permission", () -> this.inputPermission(player, uUID, "deny")), this.button(player, "atlas:reset", "Use defaults", () -> this.inputPermission(player, uUID, "unset")), this.button(player, "atlas:permissions", "Browse permissions", () -> this.permissionList(player, uUID))), () -> this.player(player, uUID));
    }

    private void inputPermission(Player player, UUID uUID, String string) {
        this.menu.ask(player, "Enter an Enorials HUD permission. Examples: enorials.minimap.use, enorials.gps.quests, enorials.party.use. Type list to browse.", string2 -> {
            this.require(player);
            if (string2.equalsIgnoreCase("list")) {
                this.permissionList(player, uUID);
                return;
            }
            this.permissionChange(player, uUID, string, (String)string2);
            this.permissions(player, uUID);
        }, () -> this.permissions(player, uUID));
    }

    private void permissionChange(Player player, UUID uUID, String string, String string2) {
        this.require(player);
        if (this.hud.getDescription().getPermissions().stream().noneMatch(permission -> permission.getName().equals(string2))) {
            throw new IllegalArgumentException("Unknown Enorials HUD permission. Type list to browse supported permissions.");
        }
        this.hud.access().command((CommandSender)player, new String[]{"player", uUID.toString(), string, string2});
        Player player2 = Bukkit.getPlayer((UUID)uUID);
        if (player2 != null) {
            player.sendMessage("Effective access for " + player2.getName() + ": " + (HudAccess.has((CommandSender)player2, string2) ? "allowed" : "denied") + ". OP/admin and explicit permission-plugin rules take precedence.");
        }
    }

    private void permissionList(Player player, UUID uUID) {
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        this.hud.getDescription().getPermissions().stream().sorted(Comparator.comparing(Permission::getName)).forEach(permission -> {
            String string = permission.getName();
            String string2 = string.replaceFirst("^enorials\\.", "").replace('.', ' ');
            arrayList.add(this.button(player, "atlas:permissions", string2, () -> {
                player.sendMessage("Permission: " + string + " \u2014 " + permission.getDescription());
                this.show(player, "Permission", List.of(this.button(player, "atlas:grant", "Allow", () -> {
                    this.permissionChange(player, uUID, "allow", string);
                    this.permissionList(player, uUID);
                }), this.button(player, "atlas:deny", "Deny", () -> {
                    this.permissionChange(player, uUID, "deny", string);
                    this.permissionList(player, uUID);
                }), this.button(player, "atlas:reset", "Inherit", () -> {
                    this.permissionChange(player, uUID, "unset", string);
                    this.permissionList(player, uUID);
                })), () -> this.permissionList(player, uUID));
            }));
        });
        this.show(player, "HUD permissions", arrayList, () -> this.permissions(player, uUID));
    }

    private void defaults(Player player) {
        this.show(player, "Default permissions", List.of(this.button(player, "atlas:grant", "Allow by default", () -> this.defaultInput(player, "allow")), this.button(player, "atlas:deny", "Deny by default", () -> this.defaultInput(player, "deny")), this.button(player, "atlas:reset", "Remove override", () -> this.defaultInput(player, "unset"))), () -> this.open(player));
    }

    private void defaultInput(Player player, String string) {
        this.menu.ask(player, "Enter the HUD permission to " + string + " by default. Example: enorials.minimap.use. This affects all players without their own rule.", string2 -> {
            this.require(player);
            this.hud.access().command((CommandSender)player, new String[]{"default", string, string2});
            this.defaults(player);
        }, () -> this.defaults(player));
    }

    private void inspect(Player player, UUID uUID) {
        this.require(player);
        Player player2 = this.online(uUID);
        Inspection inspection = new Inspection(player, uUID);
        inspection.inventory = Bukkit.createInventory((InventoryHolder)inspection, (int)45, (Component)Component.text((String)(this.name(uUID) + " \u2014 inventory (read only)")));
        ItemStack[] itemStackArray = player2.getInventory().getContents();
        for (int i = 0; i < Math.min(41, itemStackArray.length); ++i) {
            if (itemStackArray[i] == null) continue;
            inspection.inventory.setItem(i, itemStackArray[i].clone());
        }
        player.openInventory(inspection.inventory);
        player.sendMessage("Read-only inventory snapshot. Close it to return to the player menu.");
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void click(InventoryClickEvent inventoryClickEvent) {
        if (inventoryClickEvent.getView().getTopInventory().getHolder() instanceof Inspection) {
            inventoryClickEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void drag(InventoryDragEvent inventoryDragEvent) {
        if (inventoryDragEvent.getView().getTopInventory().getHolder() instanceof Inspection) {
            inventoryDragEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void closed(InventoryCloseEvent inventoryCloseEvent) {
        InventoryHolder inventoryHolder = inventoryCloseEvent.getInventory().getHolder();
        if (inventoryHolder instanceof Inspection) {
            Inspection inspection = (Inspection)inventoryHolder;
            inventoryHolder = inventoryCloseEvent.getPlayer();
            if (inventoryHolder instanceof Player) {
                Player player = (Player)inventoryHolder;
                Bukkit.getScheduler().runTask((Plugin)this.hud, () -> {
                    if (player.isOnline() && this.allowed(player) && player.getOpenInventory().getType() == InventoryType.CRAFTING) {
                        this.player(player, inspection.target);
                    }
                });
            }
        }
    }

    @Override
    public void close() {
        this.guard.cancel();
        HandlerList.unregisterAll((Listener)this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof Inspection)) continue;
            player.closeInventory();
        }
    }

    private static final class Inspection
    implements InventoryHolder {
        Inventory inventory;
        final UUID viewer;
        final UUID target;

        Inspection(Player player, UUID uUID) {
            this.viewer = player.getUniqueId();
            this.target = uUID;
        }

        public Inventory getInventory() {
            return this.inventory;
        }
    }
}

