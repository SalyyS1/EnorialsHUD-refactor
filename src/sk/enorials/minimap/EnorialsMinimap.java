/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  io.netty.channel.ChannelHandler
 *  net.minecraft.commands.CommandSourceStack
 *  org.bukkit.Bukkit
 *  org.bukkit.craftbukkit.entity.CraftPlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.minimap;

import com.mojang.brigadier.CommandDispatcher;
import io.netty.channel.ChannelHandler;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.compat.ClientProfiles;
import sk.enorials.hud.module.HudModule;
import sk.enorials.minimap.clientside.ClientsideMinimapFactory;
import sk.enorials.minimap.clientside.MinimapPacketSender;
import sk.enorials.minimap.clientside.SteerableViewFactory;
import sk.enorials.minimap.clientside.gui.GuiClientsideMinimapFactory;
import sk.enorials.minimap.clientside.gui.GuiMinimapPacketSender;
import sk.enorials.minimap.clientside.impl.NMSClientsideMinimapFactory;
import sk.enorials.minimap.clientside.impl.NMSMinimapPacketSender;
import sk.enorials.minimap.clientside.impl.NMSSteerableViewFactory;
import sk.enorials.minimap.command.MinimapCommand;
import sk.enorials.minimap.config.BlockConfig;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.injection.PassengerRewriter;
import sk.enorials.minimap.listener.MinimapBlockListener;
import sk.enorials.minimap.listener.MinimapListener;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapProvider;
import sk.enorials.minimap.map.VisualSettingsManager;
import sk.enorials.minimap.map.fullscreen.FullscreenMinimap;
import sk.enorials.minimap.map.icon.provider.BuiltinMinimapIconProvider;
import sk.enorials.minimap.map.icon.provider.MinimapIconProvider;
import sk.enorials.minimap.map.renderer.world.WorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.cache.CacheableWorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.provider.BuiltinMinimapWorldRendererProvider;
import sk.enorials.minimap.map.renderer.world.provider.MinimapWorldRendererProvider;
import sk.enorials.minimap.menu.EnomapMenuListener;
import sk.enorials.minimap.placeholder.EnorialsMinimapPlaceholders;
import sk.enorials.minimap.quest.BetonQuestMarkers;
import sk.enorials.minimap.radar.EntityRadar;
import sk.enorials.minimap.resourcepack.MinimapResourcePackBuilder;
import sk.enorials.minimap.storage.MinimapPlayerDatabase;

public final class EnorialsMinimap
extends HudModule
implements MinimapProvider,
Listener {
    private static final AtomicReference<EnorialsMinimap> PLUGIN = new AtomicReference();
    private final Map<Player, PassengerRewriter> passengerRewriters = new HashMap<Player, PassengerRewriter>();
    private ClientsideMinimapFactory defaultClientsideMinimapFactory;
    private MinimapPacketSender defaultMinimapPacketSender;
    private WorldMinimapRenderer defaultWorldRenderer;
    private MinimapIconProvider minimapIconProvider;
    private MinimapWorldRendererProvider worldRendererProvider;
    private MinimapBlockListener minimapBlockListener;
    private MinimapListener minimapListener;
    private EnomapMenuListener enomapMenuListener;
    private SteerableViewFactory steerableViewFactory;
    private MinimapPlayerDatabase playerDataStorage;
    private MinimapResourcePackBuilder minimapResourcePackBuilder;
    private EnorialsMinimapPlaceholders minimapPlaceholders;
    private EntityRadar entityRadar;
    private final CommandDispatcher<CommandSourceStack> commands = new CommandDispatcher();

    public EnorialsMinimap(EnorialsHUD enorialsHUD) {
        super(enorialsHUD, "minimap", "EnorialsMinimap");
    }

    public CommandDispatcher<CommandSourceStack> commands() {
        return this.commands;
    }

    public EntityRadar entityRadar() {
        return this.entityRadar;
    }

    public static EnorialsMinimap get() {
        return PLUGIN.get();
    }

    public void onEnable() {
        try {
            Object object;
            System.setProperty("com.j256.simplelogger.backend", "LOCAL");
            System.setProperty("com.j256.simplelogging.level", "ERROR");
            PLUGIN.set(this);
            Path path = this.getDataFolder().toPath();
            this.migrateLegacyDataFolder(path);
            Files.createDirectories(path, new FileAttribute[0]);
            this.writeBundledDefaults(path);
            this.normalizeYamlFile(path.resolve("config.yml"));
            this.normalizeYamlFile(path.resolve("blocks.yml"));
            Config.instance().reload(path.resolve("config.yml"));
            BlockConfig.instance().reload(path.resolve("blocks.yml"));
            Path path2 = path.resolve("icons");
            Files.createDirectories(path2, new FileAttribute[0]);
            this.installDefaultIcons(path2);
            this.playerDataStorage = new MinimapPlayerDatabase(path.resolve("players.db"));
            if (Config.instance().resourcePack.guiMode()) {
                this.defaultClientsideMinimapFactory = new GuiClientsideMinimapFactory();
                this.defaultMinimapPacketSender = new GuiMinimapPacketSender(this);
            } else {
                this.defaultClientsideMinimapFactory = new NMSClientsideMinimapFactory();
                this.defaultMinimapPacketSender = new NMSMinimapPacketSender(this);
            }
            this.minimapIconProvider = new BuiltinMinimapIconProvider(path2);
            this.worldRendererProvider = new BuiltinMinimapWorldRendererProvider();
            this.minimapBlockListener = new MinimapBlockListener(this);
            this.minimapListener = new MinimapListener(this);
            this.entityRadar = new EntityRadar(this);
            this.enomapMenuListener = new EnomapMenuListener(this);
            this.steerableViewFactory = new NMSSteerableViewFactory();
            this.minimapResourcePackBuilder = new MinimapResourcePackBuilder(this);
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                this.minimapPlaceholders = new EnorialsMinimapPlaceholders(this);
                this.minimapPlaceholders.register();
                this.getLogger().info("Registered PlaceholderAPI placeholders: %enomap_x%, %enomap_y%, %enomap_z%, %enomap_xyz%");
            }
            this.defaultWorldRenderer = this.worldRendererProvider.create(Config.instance().defaultMinimapRenderer);
            if (this.defaultWorldRenderer == null) {
                throw new IllegalArgumentException("default-world-renderer");
            }
            WorldMinimapRenderer worldMinimapRenderer = this.defaultWorldRenderer;
            if (worldMinimapRenderer instanceof CacheableWorldMinimapRenderer) {
                object = (CacheableWorldMinimapRenderer)worldMinimapRenderer;
                this.minimapBlockListener.registerCache(object.getWorldMapCache());
            }
            Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this.owner());
            Bukkit.getPluginManager().registerEvents((Listener)this.minimapListener, (Plugin)this.owner());
            Bukkit.getPluginManager().registerEvents((Listener)this.enomapMenuListener, (Plugin)this.owner());
            Bukkit.getPluginManager().registerEvents((Listener)this.enomapMenuListener.markerWizard(), (Plugin)this.owner());
            Bukkit.getScheduler().runTask((Plugin)this.owner(), this.enomapMenuListener::loadStoredPublicMarkers);
            if (Bukkit.getPluginManager().isPluginEnabled("BetonQuest")) {
                BetonQuestMarkers.install(this);
            }
            object = new VisualSettingsManager(this);
            Bukkit.getPluginManager().registerEvents((Listener)object, (Plugin)this.owner());
            Bukkit.getScheduler().runTaskTimer((Plugin)this.owner(), ((VisualSettingsManager)object)::tick, 2L, 2L);
            this.minimapBlockListener.registerListener((Plugin)this.owner());
            new MinimapCommand(this).register(this.commands);
            long l = Math.max(1, Math.min(10, Config.instance().radar.updateIntervalTicks));
            Bukkit.getScheduler().runTaskTimer((Plugin)this.owner(), this.entityRadar::tick, l, l);
            long l2 = 20L;
            Bukkit.getScheduler().runTaskTimer((Plugin)this.owner(), () -> {
                try {
                    this.minimapListener.cleanupExpiredMarkers();
                }
                catch (Exception exception) {
                    this.getLogger().warning("Failed to clean up expired POI markers: " + exception.getMessage());
                }
            }, l2, l2);
            long l3 = Math.max(1, Config.instance().dataRetention.cleanupIntervalMinutes);
            long l4 = l3 * 60L * 20L;
            Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this.owner(), () -> {
                try {
                    this.playerDataStorage.cleanupExpiredData(System.currentTimeMillis());
                }
                catch (Exception exception) {
                    this.getLogger().warning("Failed to run minimap data retention cleanup: " + exception.getMessage());
                }
            }, l4, l4);
        }
        catch (Throwable throwable) {
            throw new IllegalStateException("Could not initialize minimap", throwable);
        }
    }

    public void onDisable() {
        try {
            MinimapPacketSender minimapPacketSender = this.defaultMinimapPacketSender;
            if (minimapPacketSender instanceof GuiMinimapPacketSender) {
                GuiMinimapPacketSender guiMinimapPacketSender = (GuiMinimapPacketSender)minimapPacketSender;
                guiMinimapPacketSender.close();
            }
            if (this.minimapPlaceholders != null) {
                this.minimapPlaceholders.unregister();
            }
            if (this.playerDataStorage != null) {
                this.playerDataStorage.close();
            }
        }
        catch (Throwable throwable) {
            throw new IllegalStateException("Could not close minimap", throwable);
        }
    }

    private void migrateLegacyDataFolder(Path path) throws Exception {
        Path path2 = path.resolveSibling("VanillaMinimaps");
        if (!Files.exists(path, new LinkOption[0]) && Files.exists(path2, new LinkOption[0])) {
            try (Stream<Path> stream = Files.walk(path2, new FileVisitOption[0]);){
                for (Path path3 : stream.toList()) {
                    Path path4 = path2.relativize(path3);
                    Path path5 = path.resolve(path4);
                    if (Files.isDirectory(path3, new LinkOption[0])) {
                        Files.createDirectories(path5, new FileAttribute[0]);
                        continue;
                    }
                    Files.createDirectories(path5.getParent(), new FileAttribute[0]);
                    Files.copy(path3, path5, new CopyOption[0]);
                }
            }
        }
    }

    private void writeBundledDefaults(Path path) throws Exception {
        Path path2;
        Path path3 = path.resolve("config.yml");
        if (Files.notExists(path3, new LinkOption[0]) && this.getResource("config.yml") != null) {
            this.saveResource("config.yml", false);
        }
        if (Files.notExists(path2 = path.resolve("blocks.yml"), new LinkOption[0]) && this.getResource("blocks.yml") != null) {
            this.saveResource("blocks.yml", false);
        }
    }

    private void normalizeYamlFile(Path path) {
        try {
            String string;
            if (Files.notExists(path, new LinkOption[0]) || Files.isDirectory(path, new LinkOption[0])) {
                return;
            }
            byte[] byArray = Files.readAllBytes(path);
            String string2 = new String(byArray, StandardCharsets.UTF_8);
            if (!string2.isEmpty() && string2.charAt(0) == '\ufeff') {
                string2 = string2.substring(1);
            }
            if (!(string = string2.replace("\r\n", "\n").replace('\r', '\n')).equals(string2)) {
                Files.writeString(path, (CharSequence)string, StandardCharsets.UTF_8, new OpenOption[0]);
            }
        }
        catch (Exception exception) {
            this.getLogger().warning("Failed to normalize YAML file '" + String.valueOf(path.getFileName()) + "': " + exception.getMessage());
        }
    }

    private void installDefaultIcons(Path path) {
        for (String string : List.of("chest.png", "death.png", "home.png", "pig.png", "quest_!.png", "quest_questionmark.png", "village.png")) {
            Path path2 = path.resolve(string);
            if (Files.exists(path2, new LinkOption[0])) continue;
            try {
                InputStream inputStream = this.getResource(string);
                try {
                    if (inputStream == null) continue;
                    Files.copy(inputStream, path2, StandardCopyOption.REPLACE_EXISTING);
                }
                finally {
                    if (inputStream == null) continue;
                    inputStream.close();
                }
            }
            catch (Exception exception) {
                this.getLogger().warning("Failed to install default minimap icon '" + string + "': " + exception.getMessage());
            }
        }
    }

    @Override
    public ClientsideMinimapFactory clientsideMinimapFactory() {
        return this.defaultClientsideMinimapFactory;
    }

    @Override
    public MinimapPacketSender packetSender() {
        return this.defaultMinimapPacketSender;
    }

    @Override
    public WorldMinimapRenderer worldRenderer() {
        return this.defaultWorldRenderer;
    }

    @Override
    public MinimapIconProvider iconProvider() {
        return this.minimapIconProvider;
    }

    @Override
    public MinimapWorldRendererProvider worldRendererProvider() {
        return this.worldRendererProvider;
    }

    @Override
    public MinimapListener minimapListener() {
        return this.minimapListener;
    }

    @Override
    public MinimapBlockListener minimapBlockListener() {
        return this.minimapBlockListener;
    }

    public EnomapMenuListener menuListener() {
        return this.enomapMenuListener;
    }

    @Override
    public SteerableViewFactory steerableViewFactory() {
        return this.steerableViewFactory;
    }

    @Override
    public MinimapPlayerDatabase playerDataStorage() {
        return this.playerDataStorage;
    }

    public MinimapResourcePackBuilder resourcePackBuilder() {
        return this.minimapResourcePackBuilder;
    }

    @Override
    public Minimap getPlayerMinimap(Player player) {
        return this.minimapListener.getPlayerMinimaps().get(player.getUniqueId());
    }

    @Override
    public FullscreenMinimap getFullscreenMinimap(Player player) {
        return this.minimapListener.getFullscreenMinimaps().get(player.getUniqueId());
    }

    public PassengerRewriter getPassengerRewriter(Player player) {
        return this.passengerRewriters.get(player);
    }

    public void queueMinimapSave(Minimap minimap) {
        if (minimap == null || this.playerDataStorage == null) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            if (this.owner().isEnabled()) {
                Bukkit.getScheduler().runTask((Plugin)this.owner(), () -> this.queueMinimapSave(minimap));
            }
            return;
        }
        this.playerDataStorage.queueSave(minimap, this.getLogger());
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent playerJoinEvent) {
        if (this.defaultMinimapPacketSender instanceof GuiMinimapPacketSender && !ClientProfiles.legacy(playerJoinEvent.getPlayer())) {
            return;
        }
        PassengerRewriter passengerRewriter = new PassengerRewriter();
        ((CraftPlayer)playerJoinEvent.getPlayer()).getHandle().connection.connection.channel.pipeline().addBefore("packet_handler", "passenger_rewriter", (ChannelHandler)passengerRewriter);
        this.passengerRewriters.put(playerJoinEvent.getPlayer(), passengerRewriter);
    }

    @EventHandler(priority=EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        this.passengerRewriters.remove(playerQuitEvent.getPlayer());
        MinimapPacketSender minimapPacketSender = this.defaultMinimapPacketSender;
        if (minimapPacketSender instanceof GuiMinimapPacketSender) {
            GuiMinimapPacketSender guiMinimapPacketSender = (GuiMinimapPacketSender)minimapPacketSender;
            guiMinimapPacketSender.forget(playerQuitEvent.getPlayer());
        }
    }

    public Map<Player, PassengerRewriter> getPassengerRewriters() {
        return this.passengerRewriters;
    }
}

