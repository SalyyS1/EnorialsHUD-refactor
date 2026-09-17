/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonParser
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.craftbukkit.entity.CraftPlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package sk.enorials.hud;

import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.hud.EnorialsHUDCommand;
import sk.enorials.hud.access.AccessManager;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.access.HudDataMigration;
import sk.enorials.hud.api.HudApiService;
import sk.enorials.hud.betonquest.BetonQuestWarningBridge;
import sk.enorials.hud.betonquest.HudBetonQuest;
import sk.enorials.hud.compat.ClientProfiles;
import sk.enorials.hud.compat.PlayerPortraitAlignment;
import sk.enorials.hud.condition.ConditionManager;
import sk.enorials.hud.manager.AssetManager;
import sk.enorials.hud.manager.AvatarManager;
import sk.enorials.hud.manager.DisplayManager;
import sk.enorials.hud.manager.FontDefinitionManager;
import sk.enorials.hud.manager.FontManager;
import sk.enorials.hud.manager.LanguageManager;
import sk.enorials.hud.manager.LayoutManager;
import sk.enorials.hud.manager.ListenerManager;
import sk.enorials.hud.manager.PackBuildQueue;
import sk.enorials.hud.manager.PackManager;
import sk.enorials.hud.manager.PopupManager;
import sk.enorials.hud.manager.RenderManager;
import sk.enorials.hud.manager.SinglePackDelivery;
import sk.enorials.hud.manager.StorageManager;
import sk.enorials.hud.menu.EnohudCommand;
import sk.enorials.hud.menu.HudMenu;
import sk.enorials.hud.pack.HudPackComposition;
import sk.enorials.hud.pack.ServerResourcePacks;
import sk.enorials.hud.party.DungeonPartyBridge;
import sk.enorials.hud.party.PartyMapTracker;
import sk.enorials.hud.protocol.PackLoadGuard;
import sk.enorials.hud.protocol.VanillaBars;
import sk.enorials.hud.protocol.VanillaEffectIcons;
import sk.enorials.hud.social.SocialPrompts;
import sk.enorials.hud.trade.TradeService;
import sk.enorials.hud.travel.WaystoneTravel;
import sk.enorials.hud.util.ObsoleteArtwork;
import sk.enorials.hud.util.PackHostingSetup;
import sk.enorials.hud.util.ResourceCopier;
import sk.enorials.hud.util.ResourcePackHost;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.clientside.ClientsideMinimapFactory;
import sk.enorials.minimap.clientside.MinimapPacketSender;
import sk.enorials.minimap.clientside.SteerableViewFactory;
import sk.enorials.minimap.injection.PassengerRewriter;
import sk.enorials.minimap.listener.MinimapBlockListener;
import sk.enorials.minimap.listener.MinimapListener;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.fullscreen.FullscreenMinimap;
import sk.enorials.minimap.map.icon.provider.MinimapIconProvider;
import sk.enorials.minimap.map.renderer.world.WorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.provider.MinimapWorldRendererProvider;
import sk.enorials.minimap.menu.EnomapMenuListener;
import sk.enorials.minimap.resourcepack.MinimapResourcePackBuilder;
import sk.enorials.minimap.storage.MinimapPlayerDatabase;
import sk.enorials.partyhud.EnorialsPartyPlugin;
import sk.enorials.partyhud.party.PartyService;

public final class EnorialsHUD
extends JavaPlugin {
    private static EnorialsHUD INSTANCE;
    private final boolean dataMigrated = HudDataMigration.prepare(this);
    private AccessManager access;
    private final AssetManager assetManager = new AssetManager();
    private final ConditionManager conditionManager = new ConditionManager();
    private final LayoutManager layoutManager = new LayoutManager();
    private final FontManager fontManager = new FontManager();
    private final PopupManager popupManager = new PopupManager();
    private final DisplayManager displayManager = new DisplayManager();
    private final FontDefinitionManager fontDefinitionManager = new FontDefinitionManager();
    private final LanguageManager languageManager = new LanguageManager();
    private final ListenerManager listenerManager = new ListenerManager();
    private final StorageManager storageManager = new StorageManager(this);
    private final AvatarManager avatarManager = new AvatarManager(this);
    private final Object stateLock = new Object();
    private PackManager packManager = new PackManager(this);
    private PackBuildQueue packBuildQueue;
    private final HudPackComposition packComposition = new HudPackComposition(this);
    private RenderManager renderManager;
    private VanillaEffectIcons vanillaEffectIcons;
    private SinglePackDelivery singlePack;
    private ClientProfiles clientProfiles;
    private VanillaBars vanillaBars;
    private PackLoadGuard packLoadGuard;
    private EnorialsPartyPlugin party;
    private EnorialsMinimap minimap;
    private HudMenu menu;
    private SocialPrompts prompts;
    private TradeService trades;
    private EnorialsHUDCommand hudCommand;
    private EnorialsGpsPlugin gps;
    private WaystoneTravel waystoneTravel;
    private HudApiService api;
    private HudBetonQuest betonQuest;
    private BetonQuestWarningBridge betonQuestWarnings;
    private DungeonPartyBridge dungeons;

    public AccessManager access() {
        return this.access;
    }

    public PackBuildQueue packBuildQueue() {
        return this.packBuildQueue;
    }

    public HudPackComposition packComposition() {
        return this.packComposition;
    }

    public ClientProfiles clientProfiles() {
        return this.clientProfiles;
    }

    public SinglePackDelivery singlePack() {
        return this.singlePack;
    }

    public PackLoadGuard packLoadGuard() {
        return this.packLoadGuard;
    }

    public VanillaBars vanillaBars() {
        return this.vanillaBars;
    }

    public SocialPrompts prompts() {
        return this.prompts;
    }

    public TradeService trades() {
        return this.trades;
    }

    public HudMenu menu() {
        return this.menu;
    }

    public EnorialsHUDCommand hudCommand() {
        return this.hudCommand;
    }

    public void partyCommand(Player player, String string) {
        this.party.command().onCommand((CommandSender)player, (Command)this.getCommand("enohud"), "enohud party", string.split(" +"));
    }

    public void mapCommand(Player player, String string) {
        if (!string.equalsIgnoreCase("disable") && !HudAccess.check((CommandSender)player, "enorials.minimap.use")) {
            return;
        }
        try {
            this.minimap.commands().execute("map " + string, (Object)((CraftPlayer)player).getHandle().createCommandSourceStack());
        }
        catch (CommandSyntaxException commandSyntaxException) {
            player.sendMessage((Component)Component.text((String)commandSyntaxException.getRawMessage().getString()));
        }
    }

    public WaystoneTravel waystoneTravel() {
        return this.waystoneTravel;
    }

    public HudApiService api() {
        return this.api;
    }

    public HudBetonQuest betonQuest() {
        return this.betonQuest;
    }

    public EnorialsGpsPlugin gps() {
        return this.gps;
    }

    public EnorialsPartyPlugin party() {
        return this.party;
    }

    public EnorialsMinimap minimap() {
        return this.minimap;
    }

    public PartyService getPartyService() {
        return this.party.getPartyService();
    }

    public List<UUID> getPartyMemberIds(UUID uUID) {
        return this.party.getPartyMemberIds(uUID);
    }

    public boolean areInSameParty(UUID uUID, UUID uUID2) {
        return this.party.areInSameParty(uUID, uUID2);
    }

    public ClientsideMinimapFactory clientsideMinimapFactory() {
        return this.minimap.clientsideMinimapFactory();
    }

    public MinimapPacketSender packetSender() {
        return this.minimap.packetSender();
    }

    public WorldMinimapRenderer worldRenderer() {
        return this.minimap.worldRenderer();
    }

    public MinimapIconProvider iconProvider() {
        return this.minimap.iconProvider();
    }

    public MinimapWorldRendererProvider worldRendererProvider() {
        return this.minimap.worldRendererProvider();
    }

    public MinimapListener minimapListener() {
        return this.minimap.minimapListener();
    }

    public MinimapBlockListener minimapBlockListener() {
        return this.minimap.minimapBlockListener();
    }

    public EnomapMenuListener menuListener() {
        return this.minimap.menuListener();
    }

    public SteerableViewFactory steerableViewFactory() {
        return this.minimap.steerableViewFactory();
    }

    public MinimapPlayerDatabase playerDataStorage() {
        return this.minimap.playerDataStorage();
    }

    public MinimapResourcePackBuilder resourcePackBuilder() {
        return this.minimap.resourcePackBuilder();
    }

    public Minimap getPlayerMinimap(Player player) {
        return this.minimap.getPlayerMinimap(player);
    }

    public FullscreenMinimap getFullscreenMinimap(Player player) {
        return this.minimap.getFullscreenMinimap(player);
    }

    public PassengerRewriter getPassengerRewriter(Player player) {
        return this.minimap.getPassengerRewriter(player);
    }

    public void queueMinimapSave(Minimap minimap) {
        this.minimap.queueMinimapSave(minimap);
    }

    public void onLoad() {
        if (Bukkit.getPluginManager().getPlugin("BetonQuest") != null) {
            try {
                this.betonQuest = new HudBetonQuest(this);
                this.betonQuest.register();
            }
            catch (LinkageError | RuntimeException throwable) {
                this.getLogger().log(Level.SEVERE, "Cannot prepare optional BetonQuest integration", throwable);
                this.betonQuest = null;
            }
        }
    }

    public DungeonPartyBridge dungeons() {
        return this.dungeons;
    }

    public void onEnable() {
        Object object;
        INSTANCE = this;
        this.access = new AccessManager(this);
        this.saveDefaultConfig();
        this.ensureConfigDefaults();
        this.reloadConfig();
        if (this.getConfig().getBoolean("integrations.betonquest.suppress-expected-version-warnings", true)) {
            this.betonQuestWarnings = BetonQuestWarningBridge.install((Plugin)this);
        }
        boolean bl = this.areDefaultFilesMissing();
        boolean bl2 = this.getConfig().getBoolean("force-sync-default-files", false);
        boolean bl3 = this.getConfig().getBoolean("force-reset-player-hud-state", false);
        boolean bl4 = false;
        if (bl2) {
            try {
                this.syncManagedDefaults();
                bl4 = true;
            }
            catch (IOException iOException) {
                this.getLogger().log(Level.SEVERE, "Failed to sync default files.", iOException);
            }
        } else if (bl || this.getConfig().getBoolean("generate-default-files", true)) {
            try {
                ResourceCopier.copyDirectoryFromJar(this, "default", this.getDataFolder().toPath());
                bl4 = true;
            }
            catch (IOException iOException) {
                this.getLogger().log(Level.SEVERE, "Failed to generate default files.", iOException);
            }
        }
        boolean bl5 = false;
        if (this.getConfig().getBoolean("generate-unicode-files", true)) {
            try {
                ResourceCopier.copyDirectoryFromJar(this, "unicode", this.getDataFolder().toPath());
                bl5 = true;
            }
            catch (IOException iOException) {
                this.getLogger().log(Level.SEVERE, "Failed to generate unicode files.", iOException);
            }
        }
        if (bl4) {
            this.getConfig().set("generate-default-files", (Object)false);
        }
        if (bl5) {
            this.getConfig().set("generate-unicode-files", (Object)false);
        }
        if (bl2 && ((object = this.getConfig().getList("layout.default")) == null || object.isEmpty())) {
            this.getConfig().set("layout.default", List.of("enorialscorehud-corners-layout"));
        }
        if (bl4 || bl5 || bl2) {
            this.saveConfig();
        }
        this.packBuildQueue = new PackBuildQueue(this);
        this.packComposition.start();
        this.ensureCoreHudExtras();
        this.reloadAll();
        this.clientProfiles = new ClientProfiles(this);
        this.singlePack = new SinglePackDelivery(this);
        PackHostingSetup.initialize(this);
        if (bl3) {
            this.storageManager.resetHudStateForAllPlayers();
        }
        this.hudCommand = new EnorialsHUDCommand(this);
        this.menu = new HudMenu(this);
        this.prompts = new SocialPrompts(this);
        try {
            this.trades = new TradeService(this);
        }
        catch (IOException iOException) {
            this.getLogger().log(Level.SEVERE, "Trading unavailable: cannot read transaction journals", iOException);
        }
        object = new EnohudCommand(this);
        this.getCommand("enohud").setExecutor((CommandExecutor)object);
        this.getCommand("enohud").setTabCompleter((TabCompleter)object);
        this.renderManager = new RenderManager(this);
        this.renderManager.start();
        this.packLoadGuard = new PackLoadGuard(this);
        this.vanillaBars = new VanillaBars(this);
        this.vanillaEffectIcons = VanillaEffectIcons.start((Plugin)this);
        this.party = new EnorialsPartyPlugin(this);
        this.party.onEnable();
        this.dungeons = new DungeonPartyBridge(this);
        this.minimap = new EnorialsMinimap(this);
        this.minimap.onEnable();
        this.gps = new EnorialsGpsPlugin(this);
        this.gps.onEnable();
        this.waystoneTravel = new WaystoneTravel(this);
        this.api = new HudApiService(this);
        if (this.betonQuest != null) {
            this.betonQuest.start();
        }
        this.getCommand("gpsapi").setExecutor((CommandExecutor)this.gps.apiCommand());
        this.getCommand("gpsapi").setTabCompleter((TabCompleter)this.gps.apiCommand());
        PartyMapTracker partyMapTracker = new PartyMapTracker(this);
        Bukkit.getScheduler().runTaskTimer((Plugin)this, (Runnable)partyMapTracker, 1L, 1L);
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.renderManager.attach(player);
            this.avatarManager.handleJoin(player);
        }
        this.getLogger().info("Enorials HUD " + this.getDescription().getVersion());
    }

    public void onDisable() {
        if (this.singlePack != null) {
            this.singlePack.close();
        }
        if (this.avatarManager != null) {
            this.avatarManager.close();
        }
        if (this.trades != null) {
            this.trades.close();
        }
        if (this.prompts != null) {
            this.prompts.close();
        }
        if (this.betonQuestWarnings != null) {
            this.betonQuestWarnings.close();
        }
        if (this.access != null) {
            this.access.close();
        }
        if (this.packBuildQueue != null) {
            this.packBuildQueue.close();
        }
        if (this.packManager != null) {
            this.packManager.close();
        }
        if (this.vanillaBars != null) {
            this.vanillaBars.close();
        }
        if (this.packLoadGuard != null) {
            this.packLoadGuard.close();
        }
        if (this.betonQuest != null) {
            this.betonQuest.close();
        }
        if (this.dungeons != null) {
            this.dungeons.close();
        }
        if (this.api != null) {
            this.api.close();
        }
        ResourcePackHost.close();
        if (this.menu != null) {
            this.menu.close();
        }
        if (this.gps != null) {
            this.gps.onDisable();
        }
        if (this.minimap != null) {
            this.minimap.onDisable();
        }
        if (this.party != null) {
            this.party.onDisable();
        }
        if (this.vanillaEffectIcons != null) {
            this.vanillaEffectIcons.close();
            this.vanillaEffectIcons = null;
        }
        if (this.renderManager != null) {
            this.renderManager.stop();
        }
        if (this.clientProfiles != null) {
            this.clientProfiles.close();
        }
    }

    public void reloadAll() {
        this.reloadAll(true);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void reloadAll(boolean bl) {
        Object object = this.stateLock;
        synchronized (object) {
            this.ensureConfigDefaults();
            this.ensureCoreHudExtras();
            this.reloadConfig();
            if (this.packBuildQueue != null) {
                this.packBuildQueue.reload();
            }
            this.assetManager.reload(this);
            this.fontManager.reload(this);
            this.layoutManager.reload(this);
            this.popupManager.reload(this);
            this.displayManager.reload(this);
            this.fontDefinitionManager.reload(this);
            this.languageManager.reload(this);
            this.listenerManager.reload(this);
            this.storageManager.reload();
            this.avatarManager.reload();
            if (this.packManager == null) {
                this.packManager = new PackManager(this);
            }
            if (bl) {
                this.packManager.requestBuild();
            }
        }
    }

    private void ensureCoreHudExtras() {
        try {
            Object object;
            Object object2;
            Object object3;
            Comparable<File> comparable;
            PlayerPortraitAlignment.migrate(new File(this.getDataFolder(), "assets/hud/player-panel.yml"));
            boolean bl = this.getConfig().getBoolean("preserve-user-layout-edits", true);
            File file = this.getDataFolder();
            File file2 = new File(file, "layout/default-layouts.yml");
            this.mergeMissingTopLevel("default/layout/default-layouts.yml", file2);
            this.mergeMissingLayoutElements("default/layout/default-layouts.yml", file2, "enorialshud-livefix-layout");
            this.removeLegacyCornerDuplicates(file2);
            this.mergeMissingTopLevel("default/assets/hud/corehud-extras.yml", new File(file, "assets/hud/corehud-extras.yml"));
            this.copyMissingResource("default/assets/hud/enorialshud-hud.yml", new File(file, "assets/hud/enorialshud-hud.yml"));
            this.migrateTargetHealthPosition(new File(file, "assets/hud/enorialshud-hud.yml"));
            this.migratePartyPortraitDepth(new File(file, "assets/hud/party-panel.yml"));
            if (!bl) {
                comparable = new File(file, "assets/hud/enorialshud-hud.yml");
                this.overwriteTopLevelSection("default/assets/hud/enorialshud-hud.yml", (File)comparable, "event-info");
                this.overwriteTopLevelSection("default/assets/hud/enorialshud-hud.yml", (File)comparable, "target-frame");
            }
            this.mergeMissingTopLevel("default/assets/hud/corehud-corners.yml", new File(file, "assets/hud/corehud-corners.yml"));
            if (!bl) {
                this.overwriteLayoutElement("default/layout/default-layouts.yml", file2, "enorialshud-livefix-layout", "target-frame");
                this.moveLayoutElementToEnd(file2, "enorialshud-livefix-layout", "target-frame");
            }
            this.copyMissingResource("default/textures/assets/enorialshud/icon_armor.png", new File(file, "textures/assets/enorialshud/icon_armor.png"));
            this.copyMissingResource("default/textures/assets/enorialshud/icon_buff.png", new File(file, "textures/assets/enorialshud/icon_buff.png"));
            this.ensureSpellGuiPackAssets(file);
            comparable = file.toPath().resolve("reference-menu-schema.txt");
            if (!Files.exists((Path)comparable, new LinkOption[0]) || !Files.readString((Path)comparable).equals("target-portraits-14")) {
                for (String iterator : List.of("font", "textures")) {
                    ResourceCopier.syncDirectoryFromJar(this, "default/pack/assets/enorials/" + iterator + "/menu/ref", file.toPath().resolve("pack/assets/enorials/" + iterator + "/menu/ref"), false);
                }
                ResourceCopier.copyDirectoryFromJar(this, "default/pack/assets/minecraft/lang", file.toPath().resolve("pack/assets/minecraft/lang"));
                object3 = Files.list(file.toPath().resolve("pack/assets/minecraft/lang"));
                try {
                    for (Path path2 : object3.filter(path -> path.toString().endsWith(".json")).toList()) {
                        object2 = JsonParser.parseString((String)Files.readString(path2)).getAsJsonObject();
                        object2.addProperty("container.inventory", "");
                        Files.writeString(path2, (CharSequence)object2.toString(), new OpenOption[0]);
                    }
                }
                finally {
                    if (object3 != null) {
                        object3.close();
                    }
                }
                ResourceCopier.syncDirectoryFromJar(this, "default/pack/assets/enorials/items/menu", file.toPath().resolve("pack/assets/enorials/items/menu"), false);
                ResourceCopier.syncDirectoryFromJar(this, "default/pack/assets/enorials/models/item/menu", file.toPath().resolve("pack/assets/enorials/models/item/menu"), false);
                Files.writeString((Path)comparable, (CharSequence)"target-portraits-14", new OpenOption[0]);
            }
            this.ensureGps3dAssets(file);
            for (String string : List.of("layout/target-panel.yml", "assets/hud/target-panel.yml", "assets/font/target-heads.yml", "textures/assets/enorialshud/combat_target_frame.png", "textures/assets/enorialshud/combat_target_health.png", "textures/assets/enorialshud/target_heads.png")) {
                this.copyMissingResource("default/" + string, new File(file, string));
            }
            object3 = file.toPath().resolve("hud-artwork-schema.txt");
            if (!Files.exists((Path)object3, new LinkOption[0]) || !Files.readString((Path)object3).equals("portraits-bust-17")) {
                for (String string : List.of("layout/target-panel.yml", "assets/font/target-heads.yml", "textures/assets/enorialshud/combat_target_frame.png", "textures/assets/enorialshud/target_heads.png", "textures/assets/enorialshud/player_frame.png", "textures/assets/enorialshud/player_frame_compact.png")) {
                    object2 = this.getResource("default/" + string);
                    try {
                        if (object2 == null) {
                            throw new IOException("Missing HUD artwork: " + string);
                        }
                        object = file.toPath().resolve(string);
                        Files.createDirectories(object.getParent(), new FileAttribute[0]);
                        Files.copy((InputStream)object2, (Path)object, StandardCopyOption.REPLACE_EXISTING);
                    }
                    finally {
                        if (object2 == null) continue;
                        ((InputStream)object2).close();
                    }
                }
                ObsoleteArtwork.clean(this);
                Files.writeString((Path)object3, (CharSequence)"portraits-bust-17", new OpenOption[0]);
            }
            for (String string : List.of("air", "air_bursting", "air_empty", "armor_empty", "armor_half", "armor_full")) {
                object2 = "pack/assets/minecraft/textures/gui/sprites/hud/" + string + ".png";
                object = this.getResource("default/" + (String)object2);
                try {
                    if (object == null) continue;
                    Path path3 = file.toPath().resolve((String)object2);
                    Files.createDirectories(path3.getParent(), new FileAttribute[0]);
                    Files.copy((InputStream)object, path3, StandardCopyOption.REPLACE_EXISTING);
                }
                finally {
                    if (object == null) continue;
                    ((InputStream)object).close();
                }
            }
        }
        catch (IOException iOException) {
            this.getLogger().log(Level.SEVERE, "Failed to ensure CoreHUD extra files.", iOException);
        }
    }

    private void ensureGps3dAssets(File file) throws IOException {
        Object object2;
        String string22;
        Path path = file.toPath().resolve("gps-3d-schema.txt");
        if (Files.exists(path, new LinkOption[0]) && Files.readString(path).equals("gps-3d-2")) {
            return;
        }
        for (String string22 : List.of("assets/font/gps-gyro.yml", "textures/assets/enorialshud/gps_pose.png", "pack/assets/minecraft/shaders/core/text.vsh", "pack/assets/minecraft/shaders/core/text.fsh", "pack/assets/minecraft/shaders/include/gps3d/vertex_main.glsl", "pack/assets/minecraft/shaders/include/gps3d/model.glsl", "pack/assets/minecraft/shaders/include/guiminimap/vertex_main.glsl")) {
            object2 = this.getResource("default/" + string22);
            try {
                if (object2 == null) {
                    throw new IOException("Missing GPS 3D resource: " + string22);
                }
                Path object3 = file.toPath().resolve(string22);
                Files.createDirectories(object3.getParent(), new FileAttribute[0]);
                Files.copy((InputStream)object2, object3, StandardCopyOption.REPLACE_EXISTING);
            }
            finally {
                if (object2 == null) continue;
                ((InputStream)object2).close();
            }
        }
        File file2 = new File(file, "assets/hud/gps-panel.yml");
        this.copyMissingResource("default/assets/hud/gps-panel.yml", file2);
        string22 = YamlConfiguration.loadConfiguration((File)file2);
        for (String string : List.of("gps", "gps-compact")) {
            String string3 = string + ".layers.arrow";
            boolean bl = !string22.getString(string3 + ".font", "").equals("gps-gyro");
            string22.set(string3 + ".font", (Object)"gps-gyro");
            if (!bl) continue;
            string22.set(string3 + ".offset.x", (Object)(string22.getInt(string3 + ".offset.x", 7) - 2));
            string22.set(string3 + ".offset.y", (Object)(string22.getInt(string3 + ".offset.y", 6) - 2));
        }
        string22.save(file2);
        object2 = new File(file, "assets/font/hud-modern.yml");
        if (((File)object2).exists()) {
            String string4 = Files.readString(((File)object2).toPath());
            string4 = string4.replaceFirst("(?ms)^gps-arrows:\\r?\\n.*?(?=^[A-Za-z0-9_-]+:|\\z)", "");
            Files.writeString(((File)object2).toPath(), (CharSequence)string4, new OpenOption[0]);
        }
        Files.writeString(path, (CharSequence)"gps-3d-2", new OpenOption[0]);
    }

    private void ensureSpellGuiPackAssets(File file) throws IOException {
        Path path = file.toPath().resolve("pack");
        Path path2 = path.resolve("assets").resolve("enorials");
        ResourceCopier.copyDirectoryFromJar(this, "default/pack/assets/enorials", path2);
    }

    private void migratePartyPortraitDepth(File file) throws IOException {
        if (!file.isFile()) {
            return;
        }
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file);
        boolean bl = false;
        for (String string : List.of("small", "medium", "large")) {
            for (int i = 0; i < 8; ++i) {
                String string2 = "party-row-" + string + "-" + i + ".layers.";
                if (yamlConfiguration.getDouble(string2 + "head.offset.z", -1.0) != 4.0 && yamlConfiguration.getDouble(string2 + "head.offset.z", -1.0) != 2.0 || yamlConfiguration.getDouble(string2 + "frame.offset.z", -1.0) != 1.0 || yamlConfiguration.getDouble(string2 + "health.offset.z", -1.0) != 2.0) continue;
                yamlConfiguration.set(string2 + "head.offset.z", (Object)0);
                bl = true;
            }
        }
        if (bl) {
            yamlConfiguration.save(file);
        }
    }

    private void migrateTargetHealthPosition(File file) throws IOException {
        String string;
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file);
        if (yamlConfiguration.getInt((string = "target-frame.layers.") + "target-health.offset.y", -1) == 24 && yamlConfiguration.getInt(string + "target-health-background.offset.y", -1) == 10 && yamlConfiguration.getInt(string + "target-health-fill.offset.y", -1) == 12) {
            yamlConfiguration.set(string + "target-health.offset.y", (Object)13);
            yamlConfiguration.set(string + "target-health.offset.z", (Object)8);
            yamlConfiguration.save(file);
        }
    }

    private void ensureConfigDefaults() {
        block13: {
            try {
                YamlConfiguration yamlConfiguration;
                ServerResourcePacks.initialize(this.getDataFolder().toPath());
                File file = new File(this.getDataFolder(), "config.yml");
                if (!file.exists()) break block13;
                YamlConfiguration yamlConfiguration2 = YamlConfiguration.loadConfiguration((File)file);
                try (InputStream inputStream = this.getResource("default/config.yml");){
                    if (inputStream == null) {
                        return;
                    }
                    yamlConfiguration = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(inputStream));
                }
                boolean bl = false;
                if (!yamlConfiguration2.isSet("pack-build.max-concurrent-builds")) {
                    yamlConfiguration2.set("pack-build.max-concurrent-builds", (Object)1);
                    bl = true;
                }
                for (String string : List.of("allow-player-move", "pack-build.rebuild-on-move", "pack-build.async")) {
                    if (!yamlConfiguration2.isSet(string)) continue;
                    yamlConfiguration2.set(string, null);
                    bl = true;
                }
                bl |= this.mergeMissingSectionKeys(yamlConfiguration2, yamlConfiguration, "upload.github");
                bl |= this.mergeMissingSectionKeys(yamlConfiguration2, yamlConfiguration, "avatars.cleanup");
                bl |= this.mergeMissingSectionKeys(yamlConfiguration2, yamlConfiguration, "avatars.name-lookup");
                bl |= this.mergeMissingSectionKeys(yamlConfiguration2, yamlConfiguration, "server-resource-packs");
                bl |= this.mergeMissingSectionKeys(yamlConfiguration2, yamlConfiguration, "social");
                if (bl |= this.mergeMissingSectionKeys(yamlConfiguration2, yamlConfiguration, "trading")) {
                    yamlConfiguration2.save(file);
                }
            }
            catch (Exception exception) {
                this.getLogger().log(Level.WARNING, "Failed to ensure config defaults.", exception);
            }
        }
    }

    private boolean mergeMissingSectionKeys(YamlConfiguration yamlConfiguration, YamlConfiguration yamlConfiguration2, String string) {
        if (string != null && !string.isBlank() && yamlConfiguration2.isSet(string)) {
            if (!yamlConfiguration.isSet(string)) {
                yamlConfiguration.set(string, yamlConfiguration2.get(string));
                return true;
            }
            if (!yamlConfiguration2.isConfigurationSection(string)) {
                return false;
            }
            boolean bl = false;
            for (String string2 : yamlConfiguration2.getConfigurationSection(string).getKeys(true)) {
                String string3 = string + "." + string2;
                if (yamlConfiguration.isSet(string3)) continue;
                yamlConfiguration.set(string3, yamlConfiguration2.get(string3));
                bl = true;
            }
            return bl;
        }
        return false;
    }

    private void syncManagedDefaults() throws IOException {
        Path path = this.getDataFolder().toPath();
        ResourceCopier.syncDirectoryFromJar(this, "default/pack", path.resolve("pack"), true);
        ResourceCopier.syncDirectoryFromJar(this, "default/assets", path.resolve("assets"), true);
        ResourceCopier.syncDirectoryFromJar(this, "default/layout", path.resolve("layout"), true);
        ResourceCopier.syncDirectoryFromJar(this, "default/textures", path.resolve("textures"), true);
        ResourceCopier.syncDirectoryFromJar(this, "default/popup", path.resolve("popup"), true);
        ResourceCopier.syncDirectoryFromJar(this, "default/listeners", path.resolve("listeners"), true);
    }

    private void copyMissingResource(String string, File file) throws IOException {
        InputStream inputStream;
        if (!file.exists() && (inputStream = this.getResource(string)) != null) {
            Files.createDirectories(file.toPath().getParent(), new FileAttribute[0]);
            try (InputStream inputStream2 = inputStream;){
                Files.copy(inputStream, file.toPath(), new CopyOption[0]);
            }
        }
    }

    private void mergeMissingTopLevel(String string, File file) throws IOException {
        if (!file.exists()) {
            this.copyMissingResource(string, file);
        } else {
            YamlConfiguration yamlConfiguration;
            YamlConfiguration yamlConfiguration2 = YamlConfiguration.loadConfiguration((File)file);
            try (InputStream inputStream = this.getResource(string);){
                if (inputStream == null) {
                    return;
                }
                yamlConfiguration = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(inputStream));
            }
            boolean bl = false;
            for (String string2 : yamlConfiguration.getKeys(false)) {
                if (yamlConfiguration2.isSet(string2)) continue;
                yamlConfiguration2.set(string2, yamlConfiguration.get(string2));
                bl = true;
            }
            if (bl) {
                yamlConfiguration2.save(file);
            }
        }
    }

    private void overwriteTopLevelSection(String string, File file, String string2) throws IOException {
        if (file.exists()) {
            YamlConfiguration yamlConfiguration;
            YamlConfiguration yamlConfiguration2 = YamlConfiguration.loadConfiguration((File)file);
            try (Object object = this.getResource(string);){
                if (object == null) {
                    return;
                }
                yamlConfiguration = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader((InputStream)object));
            }
            if (yamlConfiguration.isSet(string2)) {
                object = yamlConfiguration.get(string2);
                Object object = yamlConfiguration2.get(string2);
                if (!Objects.equals(object, object)) {
                    yamlConfiguration2.set(string2, object);
                    yamlConfiguration2.save(file);
                }
            }
        }
    }

    private void mergeMissingLayoutElements(String string, File file, String string2) throws IOException {
        if (file.exists()) {
            YamlConfiguration yamlConfiguration;
            YamlConfiguration yamlConfiguration2 = YamlConfiguration.loadConfiguration((File)file);
            try (Object object = this.getResource(string);){
                if (object == null) {
                    return;
                }
                yamlConfiguration = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader((InputStream)object));
            }
            object = string2 + ".elements";
            if (yamlConfiguration.isConfigurationSection((String)object) && yamlConfiguration2.isConfigurationSection(string2 + ".elements")) {
                boolean bl = false;
                for (String string3 : yamlConfiguration.getConfigurationSection((String)object).getKeys(false)) {
                    String string4 = string2 + ".elements." + string3;
                    if (yamlConfiguration2.isSet(string4)) continue;
                    yamlConfiguration2.set(string4, yamlConfiguration.get((String)object + "." + string3));
                    bl = true;
                }
                if (bl) {
                    yamlConfiguration2.save(file);
                }
            }
        }
    }

    private void overwriteLayoutElement(String string, File file, String string2, String string3) throws IOException {
        if (file.exists()) {
            YamlConfiguration yamlConfiguration;
            YamlConfiguration yamlConfiguration2 = YamlConfiguration.loadConfiguration((File)file);
            try (Object object = this.getResource(string);){
                if (object == null) {
                    return;
                }
                yamlConfiguration = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader((InputStream)object));
            }
            object = string2 + ".elements." + string3;
            String string4 = string2 + ".elements." + string3;
            if (yamlConfiguration.isSet((String)object)) {
                Object object = yamlConfiguration.get((String)object);
                Object object2 = yamlConfiguration2.get(string4);
                if (!Objects.equals(object2, object)) {
                    yamlConfiguration2.set(string4, object);
                    yamlConfiguration2.save(file);
                }
            }
        }
    }

    private void moveLayoutElementToEnd(File file, String string, String string2) throws IOException {
        ConfigurationSection configurationSection;
        String string3;
        String string4;
        YamlConfiguration yamlConfiguration;
        if (file.exists() && (yamlConfiguration = YamlConfiguration.loadConfiguration((File)file)).isConfigurationSection(string4 = string + ".elements") && yamlConfiguration.isSet(string3 = string4 + "." + string2) && (configurationSection = yamlConfiguration.getConfigurationSection(string4)) != null) {
            LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
            Object object = configurationSection.get(string2);
            for (String object2 : configurationSection.getKeys(false)) {
                if (string2.equals(object2)) continue;
                linkedHashMap.put(object2, configurationSection.get(object2));
            }
            linkedHashMap.put(string2, object);
            yamlConfiguration.set(string4, null);
            for (Map.Entry entry : linkedHashMap.entrySet()) {
                yamlConfiguration.set(string4 + "." + (String)entry.getKey(), entry.getValue());
            }
            yamlConfiguration.save(file);
        }
    }

    private void removeLegacyCornerDuplicates(File file) throws IOException {
        if (file.exists()) {
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file);
            String string = "enorialscorehud-corners-layout.elements.";
            String[] stringArray = new String[]{"corner-tl-text", "corner-tr-panel", "corner-tr-icon", "corner-tr-health", "corner-tr-mana", "corner-tr-hunger", "corner-tr-text", "corner-bl-panel", "corner-bl-icon", "corner-bl-health", "corner-bl-mana", "corner-bl-hunger", "corner-bl-text", "corner-br-panel", "corner-br-icon", "corner-br-health", "corner-br-mana", "corner-br-hunger", "corner-br-text"};
            boolean bl = false;
            for (String string2 : stringArray) {
                String string3 = string + string2;
                if (!yamlConfiguration.isSet(string3)) continue;
                yamlConfiguration.set(string3, null);
                bl = true;
            }
            if (bl) {
                yamlConfiguration.save(file);
            }
        }
    }

    public AssetManager assets() {
        return this.assetManager;
    }

    public ConditionManager conditions() {
        return this.conditionManager;
    }

    public LayoutManager layouts() {
        return this.layoutManager;
    }

    public FontManager fonts() {
        return this.fontManager;
    }

    public PopupManager popups() {
        return this.popupManager;
    }

    public DisplayManager displays() {
        return this.displayManager;
    }

    public FontDefinitionManager fontDefinitions() {
        return this.fontDefinitionManager;
    }

    public LanguageManager languages() {
        return this.languageManager;
    }

    public ListenerManager listeners() {
        return this.listenerManager;
    }

    public PackManager packs() {
        return this.packManager;
    }

    public StorageManager storage() {
        return this.storageManager;
    }

    public AvatarManager avatars() {
        return this.avatarManager;
    }

    public RenderManager renderer() {
        return this.renderManager;
    }

    public Object stateLock() {
        return this.stateLock;
    }

    public static EnorialsHUD getInstance() {
        return INSTANCE;
    }

    public FileConfiguration cfg() {
        return this.getConfig();
    }

    private boolean areDefaultFilesMissing() {
        File file = this.getDataFolder();
        return !new File(file, "pack/pack.mcmeta").exists() || !new File(file, "assets/hud").isDirectory() || !new File(file, "layout/default-layouts.yml").exists() || !new File(file, "textures").isDirectory();
    }

    public File ensureDataFile(String string) {
        File file = new File(this.getDataFolder(), string);
        if (!file.exists()) {
            try {
                Files.createDirectories(file.getParentFile().toPath(), new FileAttribute[0]);
                Files.createFile(file.toPath(), new FileAttribute[0]);
            }
            catch (IOException iOException) {
                this.getLogger().log(Level.SEVERE, "Failed to create data file: " + string, iOException);
            }
        }
        return file;
    }
}

