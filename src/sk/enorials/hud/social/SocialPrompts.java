/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.hud.social;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.common.pack.ResourcePackGuard;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.compat.SocialInventoryMask;
import sk.enorials.hud.social.SocialUi;
import sk.enorials.hud.util.PackHostingSetup;

public final class SocialPrompts
implements Listener,
AutoCloseable {
    private final EnorialsHUD hud;
    private final SocialUi ui;
    private final BukkitTask task;
    private final SocialInventoryMask inventoryMask = new SocialInventoryMask();
    private final Map<UUID, LinkedHashMap<String, Prompt>> queued = new HashMap<UUID, LinkedHashMap<String, Prompt>>();
    private final Map<UUID, View> views = new HashMap<UUID, View>();

    public SocialPrompts(EnorialsHUD enorialsHUD) {
        this.hud = enorialsHUD;
        this.ui = new SocialUi(enorialsHUD);
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
        this.task = Bukkit.getScheduler().runTaskTimer((Plugin)enorialsHUD, this::tick, 1L, 10L);
    }

    public boolean open(Player player) {
        return this.views.containsKey(player.getUniqueId());
    }

    public static boolean free(Player player) {
        InventoryType inventoryType = player.getOpenInventory().getType();
        return player.isOnline() && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR && (inventoryType == InventoryType.CRAFTING || inventoryType == InventoryType.CREATIVE) && player.getItemOnCursor().getType().isAir() && !ResourcePackGuard.isLoading(player) && !player.hasMetadata("enorials.dialogue-active");
    }

    public void offer(Player player, String string, Object object, String string2, String string3, long l, BooleanSupplier booleanSupplier, Consumer<Boolean> consumer) {
        this.queued.computeIfAbsent(player.getUniqueId(), uUID -> new LinkedHashMap()).put(string, new Prompt(string, object, string2, string3, l, booleanSupplier, consumer));
    }

    public void show(Player player) {
        if (this.hud.menu().graphicalOpen(player)) {
            player.closeInventory();
        }
        this.tick();
    }

    private boolean valid(Prompt prompt) {
        return System.currentTimeMillis() < prompt.expires && prompt.valid.getAsBoolean();
    }

    private void tick() {
        Player player;
        for (View object : List.copyOf(this.views.values())) {
            if (this.valid(object.prompt)) continue;
            this.views.remove(object.owner, object);
            player = Bukkit.getPlayer((UUID)object.owner);
            if (player == null || player.getOpenInventory().getTopInventory() != object.inventory) continue;
            player.closeInventory();
        }
        for (Map.Entry entry : List.copyOf(this.queued.entrySet())) {
            player = Bukkit.getPlayer((UUID)((UUID)entry.getKey()));
            ((LinkedHashMap)entry.getValue()).values().removeIf(prompt -> !this.valid((Prompt)prompt));
            if (player == null || ((LinkedHashMap)entry.getValue()).isEmpty()) {
                this.queued.remove(entry.getKey());
                continue;
            }
            if (this.views.containsKey(entry.getKey()) || !SocialPrompts.free(player) || this.hud.menu().inputOpen(player) || this.hud.trades() != null && this.hud.trades().busy(player) || !PackHostingSetup.ready(player)) continue;
            Prompt prompt2 = (Prompt)((LinkedHashMap)entry.getValue()).values().iterator().next();
            View view = new View(player.getUniqueId(), prompt2);
            view.inventory = Bukkit.createInventory((InventoryHolder)view, (InventoryType)InventoryType.HOPPER, (Component)this.ui.invite(player, prompt2.kind, prompt2.name, prompt2.detail));
            this.views.put(view.owner, view);
            this.inventoryMask.hide(player, 5);
            player.openInventory(view.inventory);
            if (player.getOpenInventory().getTopInventory() == view.inventory) {
                ((LinkedHashMap)entry.getValue()).remove(prompt2.kind, prompt2);
                continue;
            }
            this.views.remove(view.owner, view);
            this.inventoryMask.restore(player);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void click(InventoryClickEvent inventoryClickEvent) {
        boolean bl;
        InventoryHolder inventoryHolder = inventoryClickEvent.getView().getTopInventory().getHolder();
        if (!(inventoryHolder instanceof View)) {
            return;
        }
        View view = (View)inventoryHolder;
        inventoryClickEvent.setCancelled(true);
        HumanEntity humanEntity = inventoryClickEvent.getWhoClicked();
        if (!(humanEntity instanceof Player) || !(inventoryHolder = (Player)humanEntity).getUniqueId().equals(view.owner) || inventoryClickEvent.getClick() != ClickType.LEFT) {
            return;
        }
        int n = inventoryClickEvent.getRawSlot();
        boolean bl2 = bl = n >= 23 && n <= 26;
        if (!(bl || n >= 28 && n <= 31)) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this.hud, () -> this.lambda$click$0(view, (Player)inventoryHolder, bl));
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void drag(InventoryDragEvent inventoryDragEvent) {
        if (inventoryDragEvent.getView().getTopInventory().getHolder() instanceof View) {
            inventoryDragEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent inventoryCloseEvent) {
        InventoryHolder inventoryHolder = inventoryCloseEvent.getInventory().getHolder();
        if (inventoryHolder instanceof View) {
            View view = (View)inventoryHolder;
            this.views.remove(view.owner, view);
            HumanEntity humanEntity = inventoryCloseEvent.getPlayer();
            if (humanEntity instanceof Player) {
                inventoryHolder = (Player)humanEntity;
                this.inventoryMask.restore((Player)inventoryHolder);
            }
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        UUID uUID = playerQuitEvent.getPlayer().getUniqueId();
        this.queued.remove(uUID);
        this.views.remove(uUID);
        this.inventoryMask.disconnect(uUID);
    }

    @Override
    public void close() {
        this.task.cancel();
        this.queued.clear();
        for (View view : List.copyOf(this.views.values())) {
            Player player = Bukkit.getPlayer((UUID)view.owner);
            if (player == null || player.getOpenInventory().getTopInventory() != view.inventory) continue;
            player.closeInventory();
        }
        this.views.clear();
        this.inventoryMask.close();
        HandlerList.unregisterAll((Listener)this);
    }

    private /* synthetic */ void lambda$click$0(View view, Player player, boolean bl) {
        if (this.views.get(view.owner) != view || player.getOpenInventory().getTopInventory() != view.inventory) {
            return;
        }
        boolean bl2 = this.valid(view.prompt);
        this.views.remove(view.owner, view);
        player.closeInventory();
        if (bl2) {
            view.prompt.answer.accept(bl);
        }
    }

    private record Prompt(String kind, Object token, String name, String detail, long expires, BooleanSupplier valid, Consumer<Boolean> answer) {
    }

    private static final class View
    implements InventoryHolder {
        final UUID owner;
        final Prompt prompt;
        Inventory inventory;

        View(UUID uUID, Prompt prompt) {
            this.owner = uUID;
            this.prompt = prompt;
        }

        public Inventory getInventory() {
            return this.inventory;
        }
    }
}

