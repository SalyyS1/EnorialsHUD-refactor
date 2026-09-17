/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonNull
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonPrimitive
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.event.ClickEvent
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.inventory.InventoryOpenEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerGameModeChangeEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerSwapHandItemsEvent
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.hud.trade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.common.pack.ResourcePackGuard;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.compat.SocialMenuTitle;
import sk.enorials.hud.social.SocialPrompts;
import sk.enorials.hud.social.SocialUi;
import sk.enorials.hud.trade.TradeAgreement;
import sk.enorials.hud.trade.TradeJournal;
import sk.enorials.hud.trade.TradeMoney;
import sk.enorials.hud.trade.TradePlan;
import sk.enorials.hud.trade.VaultEconomy;
import sk.enorials.hud.util.PackHostingSetup;

public final class TradeService
implements Listener,
AutoCloseable {
    private static final int[] OWN = new int[]{9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30};
    private static final int[] OTHER = new int[]{14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35};
    private final EnorialsHUD hud;
    private final SocialUi ui;
    private final TradeJournal journal;
    private final BukkitTask task;
    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();
    private final Map<UUID, Request> requests = new HashMap<UUID, Request>();
    private final Map<UUID, Long> cooldowns = new HashMap<UUID, Long>();
    private final Map<UUID, Input> inputs = new ConcurrentHashMap<UUID, Input>();
    private final TradePlan<ItemStack> planner = new TradePlan<ItemStack>(new TradePlan.Stacks<ItemStack>(){

        @Override
        public boolean empty(ItemStack itemStack) {
            return TradeService.isEmpty(itemStack);
        }

        @Override
        public int amount(ItemStack itemStack) {
            return itemStack.getAmount();
        }

        @Override
        public int limit(ItemStack itemStack) {
            return Math.min(64, itemStack.getMaxStackSize());
        }

        @Override
        public boolean similar(ItemStack itemStack, ItemStack itemStack2) {
            return itemStack.isSimilar(itemStack2);
        }

        @Override
        public ItemStack amount(ItemStack itemStack, int n) {
            ItemStack itemStack2 = itemStack.clone();
            itemStack2.setAmount(n);
            return itemStack2;
        }
    });

    public TradeService(EnorialsHUD enorialsHUD) throws IOException {
        this.hud = enorialsHUD;
        this.ui = new SocialUi(enorialsHUD);
        this.journal = new TradeJournal(enorialsHUD.getDataFolder().toPath().resolve("trades/journal"));
        if (!this.journal.healthy() || !this.journal.pending().isEmpty()) {
            enorialsHUD.getLogger().warning("Trade journals need review. Use /enohud trade review.");
        }
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
        this.task = Bukkit.getScheduler().runTaskTimer((Plugin)enorialsHUD, this::tick, 1L, 5L);
    }

    private static boolean isEmpty(ItemStack itemStack) {
        return itemStack == null || itemStack.getType().isAir() || itemStack.getAmount() <= 0;
    }

    private static ItemStack[] snapshot(Player player) {
        return (ItemStack[])Arrays.stream(player.getInventory().getStorageContents()).map(itemStack -> TradeService.isEmpty(itemStack) ? null : itemStack.clone()).toArray(ItemStack[]::new);
    }

    private static void message(CommandSender commandSender, String string) {
        commandSender.sendMessage((Component)Component.text((String)("[Trade] " + string), (TextColor)NamedTextColor.AQUA));
    }

    public boolean busy(Player player) {
        return this.sessions.containsKey(player.getUniqueId());
    }

    public double maxDistance() {
        double d = this.hud.cfg().getDouble("trading.max-distance", 10.0);
        return Double.isFinite(d) && d >= 1.0 ? Math.min(10.0, d) : 10.0;
    }

    public boolean inRange(Player player, Player player2) {
        return player != null && player2 != null && player.getWorld() == player2.getWorld() && player.getLocation().distanceSquared(player2.getLocation()) <= this.maxDistance() * this.maxDistance();
    }

    public List<Player> nearby(Player player) {
        return Bukkit.getOnlinePlayers().stream().map(Player.class::cast).filter(player2 -> !player.equals(player2) && player2.isOnline() && player.canSee(player2) && player2.canSee(player) && this.inRange(player, (Player)player2) && HudAccess.has((CommandSender)player2, "enorials.trade.use")).sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(OfflinePlayer::getUniqueId)).toList();
    }

    public boolean command(CommandSender commandSender, String[] stringArray) {
        if (stringArray.length > 0 && Set.of("review", "resolve").contains(stringArray[0].toLowerCase(Locale.ROOT))) {
            if (!HudAccess.check(commandSender, "enorials.trade.admin")) {
                return true;
            }
            if (stringArray[0].equalsIgnoreCase("review")) {
                TradeService.message(commandSender, "Unresolved exchanges: " + String.valueOf(this.journal.pending()) + ". Journals: plugins/EnorialsHUD/trades/journal. Healthy: " + this.journal.healthy());
                TradeService.message(commandSender, "After reconciling both inventories and economy accounts: /enohud trade resolve <id>. This only unlocks trading; it does not change items or money.");
            } else {
                try {
                    if (stringArray.length != 2) {
                        throw new IllegalArgumentException("Usage: /enohud trade resolve <id>");
                    }
                    UUID uUID = UUID.fromString(stringArray[1]);
                    this.journal.resolve(uUID);
                    this.hud.getLogger().warning(commandSender.getName() + " acknowledged manual reconciliation of trade " + String.valueOf(uUID));
                    TradeService.message(commandSender, "Trade marked resolved.");
                }
                catch (Exception exception) {
                    TradeService.message(commandSender, Objects.toString(exception.getMessage(), "Cannot resolve trade."));
                }
            }
            return true;
        }
        if (!(commandSender instanceof Player)) {
            TradeService.message(commandSender, "Player commands: /enohud trade <player>; admin: review / resolve <id>.");
            return true;
        }
        Player player = (Player)commandSender;
        if (stringArray.length > 0 && stringArray[0].equalsIgnoreCase("cancel")) {
            this.cancel(player, "Trade cancelled.");
            return true;
        }
        if (!HudAccess.check((CommandSender)player, "enorials.trade.use")) {
            return true;
        }
        if (!this.hud.cfg().getBoolean("trading.enabled", true)) {
            TradeService.message((CommandSender)player, "Trading is disabled.");
            return true;
        }
        if (stringArray.length == 0) {
            this.hud.menu().trading(player);
            return true;
        }
        switch (stringArray[0].toLowerCase(Locale.ROOT)) {
            case "accept": 
            case "decline": {
                Request request = this.requests.get(player.getUniqueId());
                if (request == null || !request.to.equals(player.getUniqueId()) || stringArray.length != 2 || !request.id.toString().equals(stringArray[1])) {
                    TradeService.message((CommandSender)player, "This trade request is no longer available.");
                    break;
                }
                this.answer(request, stringArray[0].equalsIgnoreCase("accept"));
                break;
            }
            case "money": {
                Session session = this.sessions.get(player.getUniqueId());
                if (session == null || stringArray.length != 2) {
                    TradeService.message((CommandSender)player, "Usage during a trade: /enohud trade money <amount>");
                    break;
                }
                this.setMoney(session, session.side(player.getUniqueId()), stringArray[1]);
                break;
            }
            default: {
                Player player2 = Bukkit.getPlayerExact((String)stringArray[0]);
                if (stringArray.length != 1 || player2 == null || !player.canSee(player2)) {
                    TradeService.message((CommandSender)player, "Player not found.");
                    break;
                }
                this.request(player, player2);
            }
        }
        return true;
    }

    public List<String> complete(CommandSender commandSender, String[] stringArray) {
        Player player;
        if (stringArray.length != 1 || !(commandSender instanceof Player) || !HudAccess.has((CommandSender)(player = (Player)commandSender), "enorials.trade.use")) {
            return List.of();
        }
        ArrayList<String> arrayList = new ArrayList<String>(List.of("cancel", "money"));
        for (Player player2 : this.nearby(player)) {
            arrayList.add(player2.getName());
        }
        if (HudAccess.has((CommandSender)player, "enorials.trade.admin")) {
            arrayList.addAll(List.of("review", "resolve"));
        }
        return arrayList.stream().filter(string -> string.toLowerCase(Locale.ROOT).startsWith(stringArray[0].toLowerCase(Locale.ROOT))).toList();
    }

    private String eligible(Player player, Player player2) {
        if (!this.hud.cfg().getBoolean("trading.enabled", true)) {
            return "Trading is disabled.";
        }
        if (player == null || player2 == null || !player.isOnline() || !player2.isOnline() || player.isDead() || player2.isDead()) {
            return "A player is no longer available.";
        }
        if (player.equals((Object)player2)) {
            return "You cannot trade with yourself.";
        }
        if (!HudAccess.has((CommandSender)player, "enorials.trade.use") || !HudAccess.has((CommandSender)player2, "enorials.trade.use")) {
            return "Both players need permission to trade.";
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR || player2.getGameMode() == GameMode.CREATIVE || player2.getGameMode() == GameMode.SPECTATOR) {
            return "Trading requires Survival or Adventure mode.";
        }
        if (this.journal.blocked(player.getUniqueId()) || this.journal.blocked(player2.getUniqueId())) {
            return "An earlier exchange needs an administrator's review.";
        }
        if (!this.inRange(player, player2)) {
            return "Move closer to the other player (" + this.maxDistance() + " blocks).";
        }
        if (!player.canSee(player2) || !player2.canSee(player)) {
            return "A player is no longer available.";
        }
        if (player.hasMetadata("enorials.dialogue-active") || player2.hasMetadata("enorials.dialogue-active") || ResourcePackGuard.isLoading(player) || ResourcePackGuard.isLoading(player2)) {
            return "Wait until both players are ready.";
        }
        return null;
    }

    public void request(Player player, Player player2) {
        this.sendRequest(player, player2);
    }

    public boolean sendRequest(Player player, Player player2) {
        String string = this.eligible(player, player2);
        if (string != null) {
            TradeService.message((CommandSender)player, string);
            return false;
        }
        if (!PackHostingSetup.ready()) {
            PackHostingSetup.explain((CommandSender)player);
            return false;
        }
        if (this.busy(player) || this.busy(player2) || this.requests.containsKey(player.getUniqueId()) || this.requests.containsKey(player2.getUniqueId())) {
            TradeService.message((CommandSender)player, "One of you already has an active trade or request.");
            return false;
        }
        long l = System.currentTimeMillis();
        if (this.cooldowns.getOrDefault(player.getUniqueId(), 0L) > l) {
            TradeService.message((CommandSender)player, "Please wait before sending another trade request.");
            return false;
        }
        this.cooldowns.put(player.getUniqueId(), l + (long)Math.max(1, Math.min(300, this.hud.cfg().getInt("trading.request-cooldown-seconds", 10))) * 1000L);
        Request request = new Request(UUID.randomUUID(), player.getUniqueId(), player2.getUniqueId(), l + (long)Math.max(5, Math.min(300, this.hud.cfg().getInt("trading.request-timeout-seconds", 30))) * 1000L);
        this.requests.put(request.from, request);
        this.requests.put(request.to, request);
        TradeService.message((CommandSender)player, "Trade request sent to " + player2.getName() + ".");
        player2.sendMessage(((TextComponent)Component.text((String)("[Trade] " + player.getName() + " wants to trade. "), (TextColor)NamedTextColor.AQUA).append(Component.text((String)"[ACCEPT] ", (TextColor)NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand((String)("/enohud trade accept " + String.valueOf(request.id)))))).append(Component.text((String)"[DECLINE]", (TextColor)NamedTextColor.RED).clickEvent(ClickEvent.runCommand((String)("/enohud trade decline " + String.valueOf(request.id))))));
        this.hud.prompts().offer(player2, "TRADE REQUEST", request, player.getName(), "wants to trade with you", request.expires, () -> this.valid(request), bl -> this.answer(request, (boolean)bl));
        return true;
    }

    private boolean valid(Request request) {
        return this.requests.get(request.from) == request && this.requests.get(request.to) == request && System.currentTimeMillis() < request.expires;
    }

    private void remove(Request request) {
        this.requests.remove(request.from, request);
        this.requests.remove(request.to, request);
    }

    private boolean available(Player player) {
        return !this.busy(player) && !this.hud.menu().inputOpen(player) && (SocialPrompts.free(player) || this.hud.menu().graphicalOpen(player) || this.hud.prompts().open(player)) && TradeService.isEmpty(player.getItemOnCursor());
    }

    private void answer(Request request, boolean bl) {
        if (!this.valid(request)) {
            return;
        }
        Player player = Bukkit.getPlayer((UUID)request.from);
        Player player2 = Bukkit.getPlayer((UUID)request.to);
        if (!bl) {
            this.remove(request);
            if (player != null) {
                TradeService.message((CommandSender)player, "Trade request declined.");
            }
            if (player2 != null) {
                TradeService.message((CommandSender)player2, "Trade request declined.");
            }
            return;
        }
        String string = this.eligible(player, player2);
        if (!(string != null || this.available(player) && this.available(player2))) {
            string = "Both players must close other windows before trading. Try the chat invitation again.";
        }
        if (string != null) {
            if (player2 != null) {
                TradeService.message((CommandSender)player2, string);
            }
            return;
        }
        this.remove(request);
        player.closeInventory();
        player2.closeInventory();
        Session session = new Session(player, player2, System.currentTimeMillis() + (long)Math.max(30, Math.min(900, this.hud.cfg().getInt("trading.session-timeout-seconds", 180))) * 1000L, this.hud.cfg().getBoolean("trading.money.enabled", true) ? VaultEconomy.find() : null);
        this.sessions.put(request.from, session);
        this.sessions.put(request.to, session);
        try {
            this.open(session, 0);
            this.open(session, 1);
        }
        catch (RuntimeException runtimeException) {
            this.finish(session, "Unable to open the trade window.");
            this.hud.getLogger().log(Level.WARNING, "Cannot open trade", runtimeException);
            return;
        }
        if (!this.isWindow(session, 0) || !this.isWindow(session, 1)) {
            this.finish(session, "A trade window could not be opened.");
            return;
        }
        for (Side side : session.sides) {
            TradeService.message((CommandSender)side.player, "Click inventory: left = full stack, right = one. Click your offer to remove it; click the gold amount to set money. Items stay yours until both players confirm.");
        }
    }

    private Component title(Session session, int n) {
        int n2;
        String string;
        String string2;
        Side side = session.sides[n];
        Side side2 = session.sides[1 - n];
        boolean bl = session.agreement.reviewing();
        long l = session.agreement.waitSeconds(System.currentTimeMillis());
        Object object = session.agreement.confirmed(n) ? "CONFIRMED" : (bl ? (l > 0L ? "REVIEW " + l + "s" : "CONFIRM") : (string2 = session.agreement.locked(n) ? "UNLOCK" : "LOCK OFFER"));
        String string3 = session.agreement.confirmed(n) ? "confirmed" : (bl && l == 0L ? "confirm" : (string = session.agreement.locked(n) ? "locked" : "open"));
        int n3 = session.agreement.confirmed(n) ? 10875274 : (n2 = session.agreement.locked(n) ? 8574195 : 15910262);
        int n4 = session.agreement.confirmed(1 - n) ? 10875274 : (session.agreement.locked(1 - n) ? 8574195 : 15910262);
        String string4 = session.economy == null ? "ITEMS ONLY" : side.money.toPlainString();
        return this.ui.trade(side.player, side2.player.getName()).append(this.ui.stateButton(side.player, string)).append(this.ui.status(side.player, session.agreement.confirmed(n) ? "YOU CONFIRMED" : (session.agreement.locked(n) ? "YOU / LOCKED" : "YOU / OPEN"), 43, n2, session.agreement.locked(n))).append(this.ui.status(side.player, session.agreement.confirmed(1 - n) ? "THEY CONFIRMED" : (session.agreement.locked(1 - n) ? "THEY / LOCKED" : "THEY / OPEN"), 133, n4, session.agreement.locked(1 - n))).append(this.ui.money(string4, 43)).append(this.ui.money(session.economy == null ? "ITEMS ONLY" : side2.money.toPlainString(), 133)).append(this.ui.action(side.player, string2, n2, session.agreement.locked(n))).append(this.ui.text("CANCEL", 133, 113, 66, 0xFFB4B4)).append(this.ui.text(session.agreement.locked(n) ? "Right-click your button to unlock" : "Click inventory to offer items", 88, 130, 158, 0xAAB7BB));
    }

    private void open(Session session, int n) {
        if (session.closed) {
            return;
        }
        Side side = session.sides[n];
        side.editing = false;
        this.inputs.remove(side.player.getUniqueId());
        View view = new View(session, n);
        view.inventory = Bukkit.createInventory((InventoryHolder)view, (int)54, (Component)this.title(session, n));
        side.view = view;
        this.fill(session, n);
        side.player.openInventory(view.inventory);
    }

    private void fill(Session session, int n) {
        Side side = session.sides[n];
        if (side.view == null) {
            return;
        }
        side.view.inventory.clear();
        for (int i = 0; i < 2; ++i) {
            int n2 = 0;
            int[] nArray = i == n ? OWN : OTHER;
            for (TradePlan.Reservation<ItemStack> reservation : session.sides[i].offers.values()) {
                ItemStack itemStack = reservation.snapshot().clone();
                itemStack.setAmount(reservation.amount());
                side.view.inventory.setItem(nArray[n2++], itemStack);
            }
        }
    }

    private boolean isWindow(Session session, int n) {
        Side side = session.sides[n];
        return side.view != null && side.player.getOpenInventory().getTopInventory() == side.view.inventory;
    }

    private void refresh(Session session) {
        if (session.closed) {
            return;
        }
        for (int i = 0; i < 2; ++i) {
            if (!this.isWindow(session, i)) continue;
            this.fill(session, i);
            SocialMenuTitle.update(session.sides[i].player, this.title(session, i));
        }
    }

    private void changed(Session session) {
        session.agreement.change();
        this.refresh(session);
    }

    private boolean observe(Session session) {
        boolean bl = false;
        for (Side side : session.sides) {
            Object[] objectArray = TradeService.snapshot(side.player);
            if (Arrays.equals(objectArray, side.observed)) continue;
            bl = true;
            side.observed = objectArray;
            side.offers.entrySet().removeIf(arg_0 -> this.lambda$observe$0((ItemStack[])objectArray, arg_0));
        }
        if (bl) {
            session.agreement.change();
            this.refresh(session);
            for (Side side : session.sides) {
                TradeService.message((CommandSender)side.player, "An inventory changed. Please review and lock both offers again.");
            }
        }
        return !bl;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void click(InventoryClickEvent inventoryClickEvent) {
        View view;
        InventoryHolder inventoryHolder;
        block6: {
            block5: {
                inventoryHolder = inventoryClickEvent.getView().getTopInventory().getHolder();
                if (!(inventoryHolder instanceof View)) {
                    return;
                }
                view = (View)inventoryHolder;
                inventoryClickEvent.setCancelled(true);
                HumanEntity humanEntity = inventoryClickEvent.getWhoClicked();
                if (!(humanEntity instanceof Player)) break block5;
                inventoryHolder = (Player)humanEntity;
                if (!view.session.closed && !view.session.committing && inventoryHolder.equals((Object)view.session.sides[view.side].player)) break block6;
            }
            return;
        }
        if (inventoryClickEvent.getClick() != ClickType.LEFT && inventoryClickEvent.getClick() != ClickType.RIGHT && inventoryClickEvent.getClick() != ClickType.SHIFT_LEFT && inventoryClickEvent.getClick() != ClickType.SHIFT_RIGHT) {
            return;
        }
        int n = inventoryClickEvent.getRawSlot();
        int n2 = inventoryClickEvent.getSlot();
        boolean bl = inventoryClickEvent.isRightClick();
        boolean bl2 = n >= 54 && n < 90 && n2 >= 0 && n2 < 36;
        ItemStack itemStack = bl2 ? inventoryHolder.getInventory().getItem(n2) : null;
        ItemStack itemStack2 = TradeService.isEmpty(itemStack) ? null : itemStack.clone();
        long l = view.session.agreement.revision();
        boolean bl3 = view.session.agreement.reviewing();
        Bukkit.getScheduler().runTask((Plugin)this.hud, () -> this.lambda$click$0(view, n, bl2, l, bl, bl3, (Player)inventoryHolder, n2, itemStack2));
    }

    private void promptMoney(Session session, int n) {
        Side side = session.sides[n];
        if (session.economy == null) {
            TradeService.message((CommandSender)side.player, "Money needs Vault and an enabled economy provider.");
            return;
        }
        session.agreement.change();
        side.editing = true;
        side.inputExpires = System.currentTimeMillis() + 60000L;
        this.inputs.put(side.player.getUniqueId(), new Input(session, n));
        side.player.closeInventory();
        this.refresh(session);
        TradeService.message((CommandSender)side.player, "Open chat (T) and enter your money offer. Use 0 to remove money, or cancel to return. /enohud trade cancel ends the trade.");
    }

    private void setMoney(Session session, int n, String string) {
        if (session.closed || session.committing) {
            return;
        }
        Side side = session.sides[n];
        try {
            if (session.economy == null || !session.economy.current()) {
                throw new IllegalArgumentException("Economy is unavailable.");
            }
            BigDecimal bigDecimal = new BigDecimal(this.hud.cfg().getString("trading.money.max-offer", "100000000"));
            if (bigDecimal.signum() < 0 || bigDecimal.compareTo(new BigDecimal("1000000000")) > 0) {
                bigDecimal = new BigDecimal("100000000");
            }
            int n2 = Math.max(0, Math.min(6, this.hud.cfg().getInt("trading.money.decimal-places", 2)));
            BigDecimal bigDecimal2 = TradeMoney.parse(string, Math.min(n2, session.economy.digits()), bigDecimal);
            if (session.economy.balance((OfflinePlayer)side.player).compareTo(bigDecimal2) < 0) {
                throw new IllegalArgumentException("You do not have enough money.");
            }
            side.money = bigDecimal2;
            session.agreement.change();
            if (side.editing) {
                this.open(session, n);
            }
            this.refresh(session);
        }
        catch (Exception exception) {
            TradeService.message((CommandSender)side.player, Objects.toString(exception.getMessage(), "Cannot set money offer."));
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void chat(AsyncChatEvent asyncChatEvent) {
        Input input = this.inputs.get(asyncChatEvent.getPlayer().getUniqueId());
        if (input == null) {
            return;
        }
        asyncChatEvent.setCancelled(true);
        String string = PlainTextComponentSerializer.plainText().serialize(asyncChatEvent.message());
        Bukkit.getScheduler().runTask((Plugin)this.hud, () -> {
            if (this.inputs.get(asyncChatEvent.getPlayer().getUniqueId()) != input || input.session.closed) {
                return;
            }
            if (string.equalsIgnoreCase("cancel")) {
                this.open(input.session, input.side);
                this.refresh(input.session);
            } else {
                this.setMoney(input.session, input.side, string);
            }
        });
    }

    private void commit(final Session session) {
        if (session.closed || session.committing || !this.observe(session)) {
            return;
        }
        String string = this.eligible(session.sides[0].player, session.sides[1].player);
        if (string != null) {
            this.finish(session, string);
            return;
        }
        if (!(this.isWindow(session, 0) && this.isWindow(session, 1) && TradeService.isEmpty(session.sides[0].player.getItemOnCursor()) && TradeService.isEmpty(session.sides[1].player.getItemOnCursor()))) {
            this.finish(session, "A player is not ready.");
            return;
        }
        TradeJournal.Entry entry = null;
        try {
            Side side = session.sides[0];
            Side side2 = session.sides[1];
            Object[] objectArray = TradeService.snapshot(side.player);
            Object[] objectArray2 = TradeService.snapshot(side2.player);
            TradePlan.Result<ItemStack> result = this.planner.build(Arrays.asList(objectArray), List.copyOf(side.offers.values()), Arrays.asList(objectArray2), List.copyOf(side2.offers.values()));
            boolean bl = side.money.signum() > 0 || side2.money.signum() > 0;
            BigDecimal bigDecimal = null;
            BigDecimal bigDecimal2 = null;
            if (bl) {
                if (session.economy == null || !session.economy.current()) {
                    throw new IllegalArgumentException("Economy is unavailable.");
                }
                bigDecimal = session.economy.balance((OfflinePlayer)side.player);
                bigDecimal2 = session.economy.balance((OfflinePlayer)side2.player);
                if (bigDecimal.compareTo(side.money) < 0 || bigDecimal2.compareTo(side2.money) < 0) {
                    throw new IllegalArgumentException("A player no longer has enough money.");
                }
            }
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("leftName", side.player.getName());
            jsonObject.addProperty("rightName", side2.player.getName());
            jsonObject.addProperty("leftMoney", side.money.toPlainString());
            jsonObject.addProperty("rightMoney", side2.money.toPlainString());
            jsonObject.addProperty("economy", bl ? session.economy.name() : "none");
            if (bl) {
                jsonObject.addProperty("leftBalance", bigDecimal.toPlainString());
                jsonObject.addProperty("rightBalance", bigDecimal2.toPlainString());
            }
            jsonObject.add("leftBefore", (JsonElement)TradeService.encode(Arrays.asList(objectArray)));
            jsonObject.add("rightBefore", (JsonElement)TradeService.encode(Arrays.asList(objectArray2)));
            jsonObject.add("leftAfter", (JsonElement)TradeService.encode(result.left()));
            jsonObject.add("rightAfter", (JsonElement)TradeService.encode(result.right()));
            if (!Arrays.equals(objectArray, TradeService.snapshot(side.player)) || !Arrays.equals(objectArray2, TradeService.snapshot(side2.player))) {
                throw new IllegalArgumentException("An inventory changed during validation.");
            }
            session.committing = true;
            entry = this.journal.prepare(session.id, side.player.getUniqueId(), side2.player.getUniqueId(), jsonObject);
            TradeMoney.Outcome outcome = TradeMoney.transfer(side.money, side2.money, new TradeMoney.Wallet(){
                final /* synthetic */ TradeService this$0;
                {
                    this.this$0 = tradeService;
                }

                @Override
                public boolean withdraw(int n, BigDecimal bigDecimal) throws Exception {
                    return session.economy.change(false, (OfflinePlayer)session.sides[n].player, bigDecimal);
                }

                @Override
                public boolean deposit(int n, BigDecimal bigDecimal) throws Exception {
                    return session.economy.change(true, (OfflinePlayer)session.sides[n].player, bigDecimal);
                }
            }, entry);
            if (outcome == TradeMoney.Outcome.CANCELLED) {
                entry.stage("CANCELLED");
                this.finish(session, "Payment failed. No items were exchanged.");
                return;
            }
            if (outcome == TradeMoney.Outcome.REVIEW) {
                entry.stage("REVIEW");
                this.review(session);
                return;
            }
            if (!(Arrays.equals(objectArray, TradeService.snapshot(side.player)) && Arrays.equals(objectArray2, TradeService.snapshot(side2.player)) && side.player.isOnline() && side2.player.isOnline())) {
                entry.stage("REVIEW_INVENTORY_CHANGED_AFTER_PAYMENT");
                this.review(session);
                return;
            }
            entry.stage("INVENTORIES_PENDING");
            side.player.getInventory().setStorageContents((ItemStack[])result.left().toArray(ItemStack[]::new));
            side2.player.getInventory().setStorageContents((ItemStack[])result.right().toArray(ItemStack[]::new));
            side.player.saveData();
            entry.stage("LEFT_SAVED");
            side2.player.saveData();
            entry.stage("RIGHT_SAVED");
            entry.stage("COMPLETE");
            this.finish(session, "Trade completed with " + (bl ? "items and money." : "items."));
        }
        catch (IllegalArgumentException illegalArgumentException) {
            if (entry != null || session.committing) {
                this.review(session);
                this.hud.getLogger().log(Level.SEVERE, "Trade " + String.valueOf(session.id) + " needs review", illegalArgumentException);
            } else {
                session.agreement.change();
                this.refresh(session);
                for (Side side : session.sides) {
                    TradeService.message((CommandSender)side.player, illegalArgumentException.getMessage());
                }
            }
        }
        catch (Exception exception) {
            if (entry != null || session.committing) {
                this.review(session);
            } else {
                this.finish(session, "Trade validation failed. Nothing was exchanged.");
            }
            this.hud.getLogger().log(Level.SEVERE, "Trade " + String.valueOf(session.id) + " failed", exception);
        }
    }

    private static JsonArray encode(List<ItemStack> list) {
        JsonArray jsonArray = new JsonArray();
        for (ItemStack itemStack : list) {
            jsonArray.add((JsonElement)(TradeService.isEmpty(itemStack) ? JsonNull.INSTANCE : new JsonPrimitive(Base64.getEncoder().encodeToString(itemStack.serializeAsBytes()))));
        }
        return jsonArray;
    }

    private void review(Session session) {
        this.hud.getLogger().severe("Trade " + String.valueOf(session.id) + " needs manual review in trades/journal. Do not blindly retry economy operations.");
        this.finish(session, "Exchange paused for administrator review. Reference: " + String.valueOf(session.id));
    }

    private void tick() {
        Object object;
        long l = System.currentTimeMillis();
        this.cooldowns.entrySet().removeIf(entry -> (Long)entry.getValue() <= l);
        for (Request object2 : new HashSet<Request>(this.requests.values())) {
            if (this.valid(object2) && Bukkit.getPlayer((UUID)object2.from) != null && Bukkit.getPlayer((UUID)object2.to) != null) continue;
            this.remove(object2);
            object = Bukkit.getPlayer((UUID)object2.from);
            if (object == null) continue;
            TradeService.message((CommandSender)object, "Trade request expired.");
        }
        for (Session session : new HashSet<Session>(this.sessions.values())) {
            if (session.closed || session.committing) continue;
            object = this.eligible(session.sides[0].player, session.sides[1].player);
            if (object != null || l > session.expires) {
                this.finish(session, object == null ? "Trade timed out." : object);
                continue;
            }
            boolean bl = false;
            for (int i = 0; i < 2; ++i) {
                Side side = session.sides[i];
                if (side.editing) {
                    if (l <= side.inputExpires) continue;
                    this.open(session, i);
                    this.refresh(session);
                    continue;
                }
                if (this.isWindow(session, i)) continue;
                this.finish(session, "Trade window closed.");
                bl = true;
                break;
            }
            if (bl) continue;
            this.observe(session);
            if (!session.agreement.reviewing() || session.renderedSecond == l / 1000L) continue;
            session.renderedSecond = l / 1000L;
            this.refresh(session);
        }
    }

    public void cancel(Player player, String string) {
        Request request;
        Session session = this.sessions.get(player.getUniqueId());
        if (session != null && !session.committing) {
            this.finish(session, string);
        }
        if ((request = this.requests.get(player.getUniqueId())) != null) {
            this.remove(request);
            Player player2 = Bukkit.getPlayer((UUID)request.from);
            Player player3 = Bukkit.getPlayer((UUID)request.to);
            if (player2 != null) {
                TradeService.message((CommandSender)player2, string);
            }
            if (player3 != null) {
                TradeService.message((CommandSender)player3, string);
            }
        }
    }

    private void finish(Session session, String string) {
        if (session.closed) {
            return;
        }
        session.closed = true;
        for (Side side : session.sides) {
            this.sessions.remove(side.player.getUniqueId(), session);
            this.inputs.remove(side.player.getUniqueId());
        }
        for (int i = 0; i < 2; ++i) {
            Side side = session.sides[i];
            if (this.isWindow(session, i)) {
                side.player.closeInventory();
            }
            if (!side.player.isOnline()) continue;
            side.player.updateInventory();
            TradeService.message((CommandSender)side.player, string);
        }
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
            if (!(view.session.closed || view.session.committing || view.session.sides[view.side].editing)) {
                this.finish(view.session, "Trade window closed.");
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void inventoryOpen(InventoryOpenEvent inventoryOpenEvent) {
        Player player;
        HumanEntity humanEntity = inventoryOpenEvent.getPlayer();
        if (humanEntity instanceof Player && this.busy(player = (Player)humanEntity) && !(inventoryOpenEvent.getInventory().getHolder() instanceof View)) {
            this.cancel(player, "Another window was opened.");
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void drop(PlayerDropItemEvent playerDropItemEvent) {
        if (this.busy(playerDropItemEvent.getPlayer())) {
            playerDropItemEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void swap(PlayerSwapHandItemsEvent playerSwapHandItemsEvent) {
        if (this.busy(playerSwapHandItemsEvent.getPlayer())) {
            playerSwapHandItemsEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        this.cancel(playerQuitEvent.getPlayer(), "A player disconnected.");
        this.inputs.remove(playerQuitEvent.getPlayer().getUniqueId());
    }

    @EventHandler
    public void death(PlayerDeathEvent playerDeathEvent) {
        this.cancel(playerDeathEvent.getEntity(), "A player died.");
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void teleport(PlayerTeleportEvent playerTeleportEvent) {
        this.cancel(playerTeleportEvent.getPlayer(), "A player teleported.");
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void gameMode(PlayerGameModeChangeEvent playerGameModeChangeEvent) {
        this.cancel(playerGameModeChangeEvent.getPlayer(), "A player's game mode changed.");
    }

    @Override
    public void close() {
        this.task.cancel();
        for (Session session : new HashSet<Session>(this.sessions.values())) {
            this.finish(session, "Trading is shutting down.");
        }
        this.requests.clear();
        this.inputs.clear();
        this.cooldowns.clear();
        HandlerList.unregisterAll((Listener)this);
    }

    private /* synthetic */ void lambda$click$0(View view, int n, boolean bl, long l, boolean bl2, boolean bl3, Player player, int n2, ItemStack itemStack) {
        Session session = view.session;
        Side side = session.sides[view.side];
        if (session.closed || session.committing || side.view != view || !this.isWindow(session, view.side)) {
            return;
        }
        if (n >= 50 && n <= 53) {
            this.finish(session, "Trade cancelled.");
            return;
        }
        if (!bl && l != session.agreement.revision()) {
            return;
        }
        if (!this.observe(session)) {
            return;
        }
        String string = this.eligible(session.sides[0].player, session.sides[1].player);
        if (string != null) {
            this.finish(session, string);
            return;
        }
        if (n >= 45 && n <= 48) {
            if (side.editing || session.sides[1 - view.side].editing) {
                return;
            }
            if (session.agreement.locked(view.side) && (bl2 || !bl3)) {
                if (session.agreement.unlock(view.side, l)) {
                    this.refresh(session);
                    for (Side side2 : session.sides) {
                        TradeService.message((CommandSender)side2.player, player.getName() + " unlocked the offer. Both players must lock again.");
                    }
                }
                return;
            }
            if (bl2) {
                return;
            }
            if (session.sides[0].offers.isEmpty() && session.sides[1].offers.isEmpty() && session.sides[0].money.signum() == 0 && session.sides[1].money.signum() == 0) {
                TradeService.message((CommandSender)player, "Add an item or money first.");
                return;
            }
            if (bl3) {
                if (session.agreement.confirm(view.side, l, System.currentTimeMillis())) {
                    this.commit(session);
                } else {
                    this.refresh(session);
                }
            } else {
                session.agreement.lock(view.side, l, System.currentTimeMillis());
                this.refresh(session);
            }
            return;
        }
        if (n >= 36 && n <= 39) {
            this.promptMoney(session, view.side);
            return;
        }
        if (n >= 54 && n < 90 && n2 >= 0 && n2 < 36) {
            int n3;
            ItemStack itemStack2 = side.player.getInventory().getItem(n2);
            if (TradeService.isEmpty(itemStack2)) {
                return;
            }
            if (!itemStack2.equals((Object)itemStack)) {
                return;
            }
            TradePlan.Reservation<ItemStack> reservation = side.offers.get(n2);
            int n4 = bl2 ? (reservation == null ? 1 : Math.min(itemStack2.getAmount(), reservation.amount() + 1)) : (n3 = itemStack2.getAmount());
            if (reservation == null && side.offers.size() >= OWN.length) {
                TradeService.message((CommandSender)player, "You can offer up to 12 stacks.");
                return;
            }
            side.offers.put(n2, new TradePlan.Reservation<ItemStack>(n2, itemStack2.clone(), n3));
            this.changed(session);
            return;
        }
        for (int i = 0; i < OWN.length; ++i) {
            if (n != OWN[i] || i >= side.offers.size()) continue;
            int n5 = new ArrayList<Integer>(side.offers.keySet()).get(i);
            TradePlan.Reservation<ItemStack> reservation = side.offers.get(n5);
            if (bl2 && reservation.amount() > 1) {
                side.offers.put(n5, new TradePlan.Reservation<ItemStack>(n5, reservation.snapshot(), reservation.amount() - 1));
            } else {
                side.offers.remove(n5);
            }
            this.changed(session);
            return;
        }
    }

    private /* synthetic */ boolean lambda$observe$0(ItemStack[] itemStackArray, Map.Entry entry) {
        return !this.planner.matches(itemStackArray[(Integer)entry.getKey()], (TradePlan.Reservation)entry.getValue());
    }

    private record Request(UUID id, UUID from, UUID to, long expires) {
    }

    private static final class Session {
        final UUID id = UUID.randomUUID();
        final Side[] sides;
        final TradeAgreement agreement = new TradeAgreement();
        final long expires;
        final VaultEconomy economy;
        boolean closed;
        boolean committing;
        long renderedSecond = -1L;

        Session(Player player, Player player2, long l, VaultEconomy vaultEconomy) {
            this.sides = new Side[]{new Side(player), new Side(player2)};
            this.expires = l;
            this.economy = vaultEconomy;
        }

        int side(UUID uUID) {
            return this.sides[0].player.getUniqueId().equals(uUID) ? 0 : 1;
        }
    }

    private static final class Side {
        final Player player;
        final LinkedHashMap<Integer, TradePlan.Reservation<ItemStack>> offers = new LinkedHashMap();
        ItemStack[] observed;
        BigDecimal money = BigDecimal.ZERO;
        View view;
        boolean editing;
        long inputExpires;

        Side(Player player) {
            this.player = player;
            this.observed = TradeService.snapshot(player);
        }
    }

    private static final class View
    implements InventoryHolder {
        final Session session;
        final int side;
        Inventory inventory;

        View(Session session, int n) {
            this.session = session;
            this.side = n;
        }

        public Inventory getInventory() {
            return this.inventory;
        }
    }

    private record Input(Session session, int side) {
    }
}

