/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.betonquest.betonquest.BetonQuest
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.bukkit.event.PlayerConversationEndEvent
 *  org.betonquest.betonquest.api.bukkit.event.PlayerConversationStartEvent
 *  org.betonquest.betonquest.conversation.Conversation
 *  org.betonquest.betonquest.conversation.ConversationIO
 *  org.betonquest.betonquest.conversation.ConversationIOFactory
 *  org.betonquest.betonquest.kernel.registry.feature.ConversationIORegistry
 *  org.bukkit.Bukkit
 *  org.bukkit.Input
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInputEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerItemHeldEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerSwapHandItemsEvent
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.bukkit.event.player.PlayerToggleSneakEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.hud.betonquest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.bukkit.event.PlayerConversationEndEvent;
import org.betonquest.betonquest.api.bukkit.event.PlayerConversationStartEvent;
import org.betonquest.betonquest.conversation.Conversation;
import org.betonquest.betonquest.conversation.ConversationIO;
import org.betonquest.betonquest.conversation.ConversationIOFactory;
import org.betonquest.betonquest.kernel.registry.feature.ConversationIORegistry;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.common.pack.ResourcePackGuard;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.betonquest.DialoguePages;
import sk.enorials.hud.betonquest.DialoguePortraits;
import sk.enorials.hud.betonquest.DialogueWindow;
import sk.enorials.hud.util.PackHostingSetup;

public final class HudConversations
implements Listener,
AutoCloseable {
    private final EnorialsHUD hud;
    private final Map<UUID, WindowIO> sessions = new HashMap<UUID, WindowIO>();
    private final Map<String, ConversationIOFactory> originals = new HashMap<String, ConversationIOFactory>();
    private final Map<String, ConversationIOFactory> wrappers = new HashMap<String, ConversationIOFactory>();
    private ConversationIORegistry registry;
    private DialogueWindow renderer;
    private DialoguePortraits portraits;
    private BukkitTask tick;
    private boolean running;
    private ModernInput modernInput;
    private final Map<UUID, Conversation> activeConversations = new HashMap<UUID, Conversation>();

    public HudConversations(EnorialsHUD enorialsHUD) {
        this.hud = enorialsHUD;
    }

    public void register() {
        this.registry = (ConversationIORegistry)BetonQuest.getInstance().getComponentLoader().get(ConversationIORegistry.class);
        for (String string : List.of("menu", "chest", "simple", "tellraw", "slowtellraw")) {
            if (!this.registry.keySet().contains(string) || this.wrappers.containsKey(string)) continue;
            try {
                ConversationIOFactory conversationIOFactory = (ConversationIOFactory)this.registry.getFactory(string);
                ConversationIOFactory conversationIOFactory2 = (conversation, onlineProfile) -> this.enabled(onlineProfile.getPlayer()) ? new WindowIO(conversation, onlineProfile.getPlayer()) : conversationIOFactory.parse(conversation, onlineProfile);
                this.originals.put(string, conversationIOFactory);
                this.wrappers.put(string, conversationIOFactory2);
                this.registry.register(string, (Object)conversationIOFactory2);
            }
            catch (QuestException questException) {
                this.hud.getLogger().warning("Conversation adapter: " + questException.getMessage());
            }
        }
        ConversationIOFactory conversationIOFactory = this.originals.get("simple");
        this.registry.register("enorials", (conversation, onlineProfile) -> {
            if (this.enabled(onlineProfile.getPlayer())) {
                return new WindowIO(conversation, onlineProfile.getPlayer());
            }
            if (conversationIOFactory == null) {
                throw new QuestException("Enorials dialogue is disabled and BQ's simple IO is unavailable");
            }
            return conversationIOFactory.parse(conversation, onlineProfile);
        });
    }

    private boolean enabled(Player player) {
        return PackHostingSetup.ready(player) && this.running && HudAccess.has((CommandSender)player, "enorials.hud.conversations") && this.hud.cfg().getBoolean("conversations.enabled", true) && this.hud.storage().preference(player.getUniqueId(), "conversations", this.hud.cfg().getBoolean("conversations.default-visible", true));
    }

    public void start() {
        if (this.running) {
            return;
        }
        try {
            this.renderer = new DialogueWindow(this.hud.getResource("dialogue-widths.properties"));
        }
        catch (IOException iOException) {
            this.hud.getLogger().warning("Conversation window unavailable: " + iOException.getMessage());
            return;
        }
        this.portraits = new DialoguePortraits(this.hud);
        this.running = true;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this.hud);
        try {
            Class.forName("org.bukkit.event.player.PlayerInputEvent", false, Player.class.getClassLoader());
            Player.class.getMethod("getCurrentInput", new Class[0]);
            this.modernInput = new ModernInput(this);
            Bukkit.getPluginManager().registerEvents((Listener)this.modernInput, (Plugin)this.hud);
        }
        catch (ClassNotFoundException | NoSuchMethodException reflectiveOperationException) {
            this.modernInput = null;
        }
        this.tick = Bukkit.getScheduler().runTaskTimer((Plugin)this.hud, () -> {
            for (WindowIO windowIO : List.copyOf(this.sessions.values())) {
                if (!windowIO.player.isOnline()) {
                    windowIO.finish();
                    continue;
                }
                if (windowIO.player.isDead()) {
                    windowIO.finish();
                    continue;
                }
                if (!this.enabled(windowIO.player)) {
                    try {
                        windowIO.conversation.suspend();
                        continue;
                    }
                    finally {
                        windowIO.finish();
                        continue;
                    }
                }
                if (!windowIO.dirty || ResourcePackGuard.isLoading(windowIO.player)) continue;
                windowIO.paint();
            }
        }, 1L, 5L);
    }

    private void main(Runnable runnable) {
        if (!this.hud.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask((Plugin)this.hud, runnable);
        }
    }

    public boolean active(Player player) {
        return this.sessions.containsKey(player.getUniqueId());
    }

    public boolean cancel(Player player) {
        WindowIO windowIO = this.session(player);
        if (windowIO == null) {
            return false;
        }
        windowIO.abort();
        return true;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void conversationStart(PlayerConversationStartEvent playerConversationStartEvent) {
        playerConversationStartEvent.getProfile().getOnlineProfile().ifPresent(onlineProfile -> this.main(() -> {
            Player player = onlineProfile.getPlayer();
            this.activeConversations.put(player.getUniqueId(), playerConversationStartEvent.getConversation());
            player.setMetadata("enorials.dialogue-active", (MetadataValue)new FixedMetadataValue((Plugin)this.hud, (Object)true));
        }));
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void conversationEnd(PlayerConversationEndEvent playerConversationEndEvent) {
        playerConversationEndEvent.getProfile().getOnlineProfile().ifPresent(onlineProfile -> this.main(() -> {
            Player player = onlineProfile.getPlayer();
            if (this.activeConversations.remove(player.getUniqueId(), playerConversationEndEvent.getConversation())) {
                player.removeMetadata("enorials.dialogue-active", (Plugin)this.hud);
            }
        }));
    }

    private WindowIO session(Player player) {
        return this.sessions.get(player.getUniqueId());
    }

    private void input(Player player, int n) {
        WindowIO windowIO = this.session(player);
        if (windowIO == null) {
            return;
        }
        int n2 = n & ~windowIO.inputFlags;
        windowIO.inputFlags = n;
        if ((n2 & 8) != 0 && !windowIO.conversation.isMovementBlock()) {
            windowIO.abort();
            return;
        }
        if ((n2 & 4) != 0) {
            windowIO.confirm();
        } else if ((n2 & 1) != 0) {
            windowIO.select(-1);
        } else if ((n2 & 2) != 0) {
            windowIO.select(1);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void legacyMove(PlayerMoveEvent playerMoveEvent) {
        if (this.modernInput != null || playerMoveEvent instanceof PlayerTeleportEvent || playerMoveEvent.getTo() == null) {
            return;
        }
        WindowIO windowIO = this.session(playerMoveEvent.getPlayer());
        if (windowIO == null) {
            return;
        }
        Location location = playerMoveEvent.getFrom();
        Location location2 = playerMoveEvent.getTo();
        long l = System.nanoTime();
        if (location2.getY() - location.getY() > 0.1) {
            if (l >= windowIO.nextLegacyJump) {
                windowIO.nextLegacyJump = l + 500000000L;
                windowIO.confirm();
            }
            return;
        }
        double d = Math.toRadians(location.getYaw());
        double d2 = -(location2.getX() - location.getX()) * Math.sin(d) + (location2.getZ() - location.getZ()) * Math.cos(d);
        if (Math.abs(d2) > 0.015 && l >= windowIO.nextLegacySelect) {
            windowIO.nextLegacySelect = l + 200000000L;
            windowIO.select(d2 > 0.0 ? -1 : 1);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void legacySneak(PlayerToggleSneakEvent playerToggleSneakEvent) {
        if (this.modernInput != null || !playerToggleSneakEvent.isSneaking()) {
            return;
        }
        WindowIO windowIO = this.session(playerToggleSneakEvent.getPlayer());
        if (windowIO != null && !windowIO.conversation.isMovementBlock()) {
            windowIO.abort();
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void wheel(PlayerItemHeldEvent playerItemHeldEvent) {
        WindowIO windowIO = this.session(playerItemHeldEvent.getPlayer());
        if (windowIO == null) {
            return;
        }
        playerItemHeldEvent.setCancelled(true);
        int n = Math.floorMod(playerItemHeldEvent.getNewSlot() - playerItemHeldEvent.getPreviousSlot() + 4, 9) - 4;
        if (n != 0) {
            windowIO.select(Integer.signum(n));
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void click(PlayerInteractEvent playerInteractEvent) {
        WindowIO windowIO = this.session(playerInteractEvent.getPlayer());
        if (windowIO == null) {
            return;
        }
        playerInteractEvent.setCancelled(true);
        if (playerInteractEvent.getHand() == EquipmentSlot.HAND && playerInteractEvent.getAction().isRightClick()) {
            windowIO.confirm();
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void entityClick(PlayerInteractEntityEvent playerInteractEntityEvent) {
        WindowIO windowIO = this.session(playerInteractEntityEvent.getPlayer());
        if (windowIO == null) {
            return;
        }
        playerInteractEntityEvent.setCancelled(true);
        if (playerInteractEntityEvent.getHand() == EquipmentSlot.HAND) {
            windowIO.confirm();
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void move(PlayerMoveEvent playerMoveEvent) {
        WindowIO windowIO = this.session(playerMoveEvent.getPlayer());
        if (windowIO == null || playerMoveEvent instanceof PlayerTeleportEvent) {
            return;
        }
        Location location = playerMoveEvent.getTo();
        if (playerMoveEvent.getFrom().getX() != location.getX() || playerMoveEvent.getFrom().getY() != location.getY() || playerMoveEvent.getFrom().getZ() != location.getZ()) {
            Location location2 = playerMoveEvent.getFrom().clone();
            location2.setYaw(location.getYaw());
            location2.setPitch(location.getPitch());
            playerMoveEvent.setTo(location2);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void block(BlockBreakEvent blockBreakEvent) {
        if (this.active(blockBreakEvent.getPlayer())) {
            blockBreakEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void drop(PlayerDropItemEvent playerDropItemEvent) {
        if (this.active(playerDropItemEvent.getPlayer())) {
            playerDropItemEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void swap(PlayerSwapHandItemsEvent playerSwapHandItemsEvent) {
        if (this.active(playerSwapHandItemsEvent.getPlayer())) {
            playerSwapHandItemsEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void damage(EntityDamageByEntityEvent entityDamageByEntityEvent) {
        Projectile projectile;
        ProjectileSource projectileSource;
        Entity entity = entityDamageByEntityEvent.getDamager();
        if (entity instanceof Projectile && (projectileSource = (projectile = (Projectile)entity).getShooter()) instanceof Entity) {
            Entity entity2;
            entity = entity2 = (Entity)projectileSource;
        }
        if (entity instanceof Player && this.active((Player)(projectile = (Player)entity))) {
            entityDamageByEntityEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void quit(PlayerQuitEvent playerQuitEvent) {
        WindowIO windowIO = this.session(playerQuitEvent.getPlayer());
        if (windowIO != null) {
            windowIO.finish();
        }
        this.activeConversations.remove(playerQuitEvent.getPlayer().getUniqueId());
        playerQuitEvent.getPlayer().removeMetadata("enorials.dialogue-active", (Plugin)this.hud);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void death(PlayerDeathEvent playerDeathEvent) {
        WindowIO windowIO = this.session(playerDeathEvent.getPlayer());
        if (windowIO != null) {
            windowIO.finish();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void close() {
        this.running = false;
        for (WindowIO object : List.copyOf(this.sessions.values())) {
            try {
                if (!object.conversation.isActive()) continue;
                object.conversation.suspend();
            }
            catch (RuntimeException runtimeException) {
                this.hud.getLogger().warning("Could not suspend BQ dialogue during shutdown: " + runtimeException.getMessage());
            }
            finally {
                object.finish();
            }
        }
        if (this.tick != null) {
            this.tick.cancel();
            this.tick = null;
        }
        HandlerList.unregisterAll((Listener)this);
        if (this.modernInput != null) {
            HandlerList.unregisterAll((Listener)this.modernInput);
            this.modernInput = null;
        }
        for (UUID uUID : this.activeConversations.keySet()) {
            Player player = Bukkit.getPlayer((UUID)uUID);
            if (player == null) continue;
            player.removeMetadata("enorials.dialogue-active", (Plugin)this.hud);
        }
        this.activeConversations.clear();
        if (this.portraits != null) {
            this.portraits.close();
        }
        if (this.registry != null) {
            for (Map.Entry entry : this.originals.entrySet()) {
                try {
                    if (this.registry.getFactory((String)entry.getKey()) != this.wrappers.get(entry.getKey())) continue;
                    this.registry.register((String)entry.getKey(), (Object)((ConversationIOFactory)entry.getValue()));
                }
                catch (QuestException questException) {}
            }
        }
        this.originals.clear();
        this.wrappers.clear();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static final class ModernInput
    implements Listener {
        private final HudConversations owner;

        ModernInput(HudConversations hudConversations) {
            this.owner = hudConversations;
        }

        static int flags(Player player) {
            Input input = player.getCurrentInput();
            return (input.isForward() ? 1 : 0) | (input.isBackward() ? 2 : 0) | (input.isJump() ? 4 : 0) | (input.isSneak() ? 8 : 0);
        }

        @EventHandler
        public void input(PlayerInputEvent playerInputEvent) {
            Input input = playerInputEvent.getInput();
            this.owner.input(playerInputEvent.getPlayer(), (input.isForward() ? 1 : 0) | (input.isBackward() ? 2 : 0) | (input.isJump() ? 4 : 0) | (input.isSneak() ? 8 : 0));
        }
    }

    public final class WindowIO
    implements ConversationIO {
        final Conversation conversation;
        final Player player;
        private String name = "";
        private String text = "";
        private final List<String> choices = new ArrayList<String>();
        private DialoguePages pages;
        private BufferedImage portrait;
        private final Map<Integer, Component> portraitComponents = new HashMap<Integer, Component>();
        private boolean dirty;
        private boolean waiting;
        private boolean closed;
        private boolean aborting;
        private Runnable endCallback;
        private int inputFlags;
        private long nextConfirm;
        private long nextLegacySelect;
        private long nextLegacyJump;

        WindowIO(Conversation conversation, Player player) {
            this.conversation = conversation;
            this.player = player;
        }

        public void begin() {
            HudConversations.this.main(() -> {
                WindowIO windowIO = HudConversations.this.sessions.put(this.player.getUniqueId(), this);
                if (windowIO != null && windowIO != this) {
                    windowIO.abort();
                }
                this.player.setMetadata("enorials.conversation", (MetadataValue)new FixedMetadataValue((Plugin)HudConversations.this.hud, (Object)true));
                this.inputFlags = HudConversations.this.modernInput == null ? 0 : ModernInput.flags(this.player);
                HudConversations.this.portraits.resolve(this.conversation.getCenter(), this.player, bufferedImage -> {
                    if (!this.closed) {
                        this.portrait = bufferedImage;
                        this.portraitComponents.clear();
                        this.dirty = true;
                    }
                });
            });
        }

        public void setNpcResponse(Component component, Component component2) {
            HudConversations.this.main(() -> {
                this.name = HudConversations.plain(component);
                this.text = HudConversations.plain(component2);
            });
        }

        public void addPlayerOption(Component component, ConfigurationSection configurationSection) {
            HudConversations.this.main(() -> this.choices.add(HudConversations.plain(component)));
        }

        public void display() {
            HudConversations.this.main(() -> {
                if (this.closed) {
                    return;
                }
                this.pages = new DialoguePages(this.text, List.copyOf(this.choices), HudConversations.this.renderer::width);
                this.waiting = false;
                this.dirty = true;
                this.nextConfirm = System.nanoTime() + 250000000L;
                if (!ResourcePackGuard.isLoading(this.player)) {
                    this.paint();
                }
            });
        }

        public void clear() {
            HudConversations.this.main(() -> {
                this.choices.clear();
                this.text = "";
                this.waiting = true;
            });
        }

        public void end(Runnable runnable) {
            HudConversations.this.main(() -> {
                if (this.closed) {
                    runnable.run();
                    return;
                }
                this.endCallback = runnable;
                if (this.aborting || !this.player.isOnline() || this.player.isDead() || this.pages == null || this.pages.answerCount() > 0 || this.text.isBlank()) {
                    this.finish();
                } else {
                    this.waiting = false;
                    this.dirty = true;
                }
            });
        }

        void paint() {
            if (this.closed || this.pages == null || !this.player.isOnline()) {
                return;
            }
            int n = HudConversations.this.renderer.portraitTop(this.name, this.pages);
            int n2 = HudConversations.this.renderer.layout(this.name, this.pages).extent();
            Component component = this.portrait == null ? null : this.portraitComponents.computeIfAbsent(n * 1024 + n2, n3 -> DialoguePortraits.component(this.portrait, -201, n, n2));
            HudConversations.this.hud.renderer().setConversationOverlay(this.player, HudConversations.this.renderer.render(this.name, this.pages, component, !this.conversation.isMovementBlock()));
            this.dirty = false;
        }

        void select(int n) {
            if (this.closed || this.waiting || this.pages == null || ResourcePackGuard.isLoading(this.player)) {
                return;
            }
            this.pages.select(n);
            this.dirty = true;
            this.paint();
        }

        void confirm() {
            if (this.closed || this.waiting || this.pages == null || ResourcePackGuard.isLoading(this.player) || System.nanoTime() < this.nextConfirm) {
                return;
            }
            this.nextConfirm = System.nanoTime() + 250000000L;
            int n = this.pages.confirm();
            if (n < 0) {
                this.dirty = true;
                this.paint();
                return;
            }
            if (n == 0) {
                if (this.endCallback != null) {
                    this.finish();
                }
                return;
            }
            this.waiting = true;
            this.conversation.passPlayerAnswer(n);
        }

        void abort() {
            if (this.closed) {
                return;
            }
            this.aborting = true;
            if (this.conversation.isActive()) {
                this.conversation.endConversation();
            }
            this.finish();
        }

        void finish() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (HudConversations.this.sessions.remove(this.player.getUniqueId(), this)) {
                this.player.removeMetadata("enorials.conversation", (Plugin)HudConversations.this.hud);
                if (HudConversations.this.hud.renderer() != null) {
                    HudConversations.this.hud.renderer().setConversationOverlay(this.player, (Component)Component.empty());
                }
            }
            if (this.endCallback != null) {
                Runnable runnable = this.endCallback;
                this.endCallback = null;
                runnable.run();
            }
        }
    }
}

