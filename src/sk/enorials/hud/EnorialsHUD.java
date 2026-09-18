package sk.enorials.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import sk.enorials.gps.EnorialsGpsPlugin;
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

public final class EnorialsHUD extends JavaPlugin {
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

   public void partyCommand(Player var1, String var2) {
      this.party.command().onCommand(var1, this.getCommand("enohud"), "enohud party", var2.split(" +"));
   }

   public void mapCommand(Player var1, String var2) {
      if (var2.equalsIgnoreCase("disable") || HudAccess.check(var1, "enorials.minimap.use")) {
         try {
            this.minimap.commands().execute("map " + var2, ((CraftPlayer)var1).getHandle().createCommandSourceStack());
         } catch (CommandSyntaxException var4) {
            var1.sendMessage(Component.text(var4.getRawMessage().getString()));
         }
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

   public List<UUID> getPartyMemberIds(UUID var1) {
      return this.party.getPartyMemberIds(var1);
   }

   public boolean areInSameParty(UUID var1, UUID var2) {
      return this.party.areInSameParty(var1, var2);
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

   public Minimap getPlayerMinimap(Player var1) {
      return this.minimap.getPlayerMinimap(var1);
   }

   public FullscreenMinimap getFullscreenMinimap(Player var1) {
      return this.minimap.getFullscreenMinimap(var1);
   }

   public PassengerRewriter getPassengerRewriter(Player var1) {
      return this.minimap.getPassengerRewriter(var1);
   }

   public void queueMinimapSave(Minimap var1) {
      this.minimap.queueMinimapSave(var1);
   }

   public void onLoad() {
      if (Bukkit.getPluginManager().getPlugin("BetonQuest") != null) {
         try {
            this.betonQuest = new HudBetonQuest(this);
            this.betonQuest.register();
         } catch (RuntimeException | LinkageError var2) {
            this.getLogger().log(Level.SEVERE, "Cannot prepare optional BetonQuest integration", var2);
            this.betonQuest = null;
         }
      }
   }

   public DungeonPartyBridge dungeons() {
      return this.dungeons;
   }

   public void onEnable() {
      INSTANCE = this;
      this.access = new AccessManager(this);
      this.saveDefaultConfig();
      this.ensureConfigDefaults();
      this.reloadConfig();
      if (this.getConfig().getBoolean("integrations.betonquest.suppress-expected-version-warnings", true)) {
         this.betonQuestWarnings = BetonQuestWarningBridge.install(this);
      }

      boolean var1 = this.areDefaultFilesMissing();
      boolean var2 = this.getConfig().getBoolean("force-sync-default-files", false);
      boolean var3 = this.getConfig().getBoolean("force-reset-player-hud-state", false);
      boolean var4 = false;
      if (var2) {
         try {
            this.syncManagedDefaults();
            var4 = true;
         } catch (IOException var13) {
            this.getLogger().log(Level.SEVERE, "Failed to sync default files.", var13);
         }
      } else if (var1 || this.getConfig().getBoolean("generate-default-files", true)) {
         try {
            ResourceCopier.copyDirectoryFromJar(this, "default", this.getDataFolder().toPath());
            var4 = true;
         } catch (IOException var12) {
            this.getLogger().log(Level.SEVERE, "Failed to generate default files.", var12);
         }
      }

      boolean var5 = false;
      if (this.getConfig().getBoolean("generate-unicode-files", true)) {
         try {
            ResourceCopier.copyDirectoryFromJar(this, "unicode", this.getDataFolder().toPath());
            var5 = true;
         } catch (IOException var11) {
            this.getLogger().log(Level.SEVERE, "Failed to generate unicode files.", var11);
         }
      }

      if (var4) {
         this.getConfig().set("generate-default-files", false);
      }

      if (var5) {
         this.getConfig().set("generate-unicode-files", false);
      }

      if (var2) {
         List var6 = this.getConfig().getList("layout.default");
         if (var6 == null || var6.isEmpty()) {
            this.getConfig().set("layout.default", List.of("enorialscorehud-corners-layout"));
         }
      }

      if (var4 || var5 || var2) {
         this.saveConfig();
      }

      this.packBuildQueue = new PackBuildQueue(this);
      this.packComposition.start();
      this.ensureCoreHudExtras();
      this.reloadAll();
      this.clientProfiles = new ClientProfiles(this);
      this.singlePack = new SinglePackDelivery(this);
      PackHostingSetup.initialize(this);
      if (var3) {
         this.storageManager.resetHudStateForAllPlayers();
      }

      this.hudCommand = new EnorialsHUDCommand(this);
      this.menu = new HudMenu(this);
      this.prompts = new SocialPrompts(this);

      try {
         this.trades = new TradeService(this);
      } catch (IOException var10) {
         this.getLogger().log(Level.SEVERE, "Trading unavailable: cannot read transaction journals", var10);
      }

      EnohudCommand var14 = new EnohudCommand(this);
      this.getCommand("enohud").setExecutor(var14);
      this.getCommand("enohud").setTabCompleter(var14);
      this.renderManager = new RenderManager(this);
      this.renderManager.start();
      this.packLoadGuard = new PackLoadGuard(this);
      this.vanillaBars = new VanillaBars(this);
      this.vanillaEffectIcons = VanillaEffectIcons.start(this);
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

      this.getCommand("gpsapi").setExecutor(this.gps.apiCommand());
      this.getCommand("gpsapi").setTabCompleter(this.gps.apiCommand());
      PartyMapTracker var7 = new PartyMapTracker(this);
      Bukkit.getScheduler().runTaskTimer(this, var7, 1L, 1L);

      for (Player var9 : Bukkit.getOnlinePlayers()) {
         this.renderManager.attach(var9);
         this.avatarManager.handleJoin(var9);
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

   public void reloadAll(boolean var1) {
      synchronized (this.stateLock) {
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

         if (var1) {
            this.packManager.requestBuild();
         }
      }
   }

   private void ensureCoreHudExtras() {
      try {
         PlayerPortraitAlignment.migrate(new File(this.getDataFolder(), "assets/hud/player-panel.yml"));
         boolean var1 = this.getConfig().getBoolean("preserve-user-layout-edits", true);
         File var2 = this.getDataFolder();
         File var3 = new File(var2, "layout/default-layouts.yml");
         this.mergeMissingTopLevel("default/layout/default-layouts.yml", var3);
         this.mergeMissingLayoutElements("default/layout/default-layouts.yml", var3, "enorialshud-livefix-layout");
         this.removeLegacyCornerDuplicates(var3);
         this.mergeMissingTopLevel("default/assets/hud/corehud-extras.yml", new File(var2, "assets/hud/corehud-extras.yml"));
         this.copyMissingResource("default/assets/hud/enorialshud-hud.yml", new File(var2, "assets/hud/enorialshud-hud.yml"));
         this.migrateTargetHealthPosition(new File(var2, "assets/hud/enorialshud-hud.yml"));
         this.migratePartyPortraitDepth(new File(var2, "assets/hud/party-panel.yml"));
         if (!var1) {
            File var4 = new File(var2, "assets/hud/enorialshud-hud.yml");
            this.overwriteTopLevelSection("default/assets/hud/enorialshud-hud.yml", var4, "event-info");
            this.overwriteTopLevelSection("default/assets/hud/enorialshud-hud.yml", var4, "target-frame");
         }

         this.mergeMissingTopLevel("default/assets/hud/corehud-corners.yml", new File(var2, "assets/hud/corehud-corners.yml"));
         if (!var1) {
            this.overwriteLayoutElement("default/layout/default-layouts.yml", var3, "enorialshud-livefix-layout", "target-frame");
            this.moveLayoutElementToEnd(var3, "enorialshud-livefix-layout", "target-frame");
         }

         this.copyMissingResource("default/textures/assets/enorialshud/icon_armor.png", new File(var2, "textures/assets/enorialshud/icon_armor.png"));
         this.copyMissingResource("default/textures/assets/enorialshud/icon_buff.png", new File(var2, "textures/assets/enorialshud/icon_buff.png"));
         this.ensureSpellGuiPackAssets(var2);
         Path var19 = var2.toPath().resolve("reference-menu-schema.txt");
         if (!Files.exists(var19) || !Files.readString(var19).equals("target-portraits-14")) {
            for (String var6 : List.of("font", "textures")) {
               ResourceCopier.syncDirectoryFromJar(
                  this, "default/pack/assets/enorials/" + var6 + "/menu/ref", var2.toPath().resolve("pack/assets/enorials/" + var6 + "/menu/ref"), false
               );
            }

            ResourceCopier.copyDirectoryFromJar(this, "default/pack/assets/minecraft/lang", var2.toPath().resolve("pack/assets/minecraft/lang"));

            try (Stream var20 = Files.list(var2.toPath().resolve("pack/assets/minecraft/lang"))) {
               for (Path var7 : var20.filter(var0 -> var0.toString().endsWith(".json")).toList()) {
                  JsonObject var8 = JsonParser.parseString(Files.readString(var7)).getAsJsonObject();
                  var8.addProperty("container.inventory", "");
                  Files.writeString(var7, var8.toString());
               }
            }

            ResourceCopier.syncDirectoryFromJar(
               this, "default/pack/assets/enorials/items/menu", var2.toPath().resolve("pack/assets/enorials/items/menu"), false
            );
            ResourceCopier.syncDirectoryFromJar(
               this, "default/pack/assets/enorials/models/item/menu", var2.toPath().resolve("pack/assets/enorials/models/item/menu"), false
            );
            Files.writeString(var19, "target-portraits-14");
         }

         this.ensureGps3dAssets(var2);

         for (String var24 : List.of(
            "layout/target-panel.yml",
            "assets/hud/target-panel.yml",
            "assets/font/target-heads.yml",
            "textures/assets/enorialshud/combat_target_frame.png",
            "textures/assets/enorialshud/combat_target_health.png",
            "textures/assets/enorialshud/target_heads.png"
         )) {
            this.copyMissingResource("default/" + var24, new File(var2, var24));
         }

         Path var22 = var2.toPath().resolve("hud-artwork-schema.txt");
         if (!Files.exists(var22) || !Files.readString(var22).equals("portraits-bust-17")) {
            for (String var27 : List.of(
               "layout/target-panel.yml",
               "assets/font/target-heads.yml",
               "textures/assets/enorialshud/combat_target_frame.png",
               "textures/assets/enorialshud/target_heads.png",
               "textures/assets/enorialshud/player_frame.png",
               "textures/assets/enorialshud/player_frame_compact.png"
            )) {
               try (InputStream var29 = this.getResource("default/" + var27)) {
                  if (var29 == null) {
                     throw new IOException("Missing HUD artwork: " + var27);
                  }

                  Path var9 = var2.toPath().resolve(var27);
                  Files.createDirectories(var9.getParent());
                  Files.copy(var29, var9, StandardCopyOption.REPLACE_EXISTING);
               }
            }

            ObsoleteArtwork.clean(this);
            Files.writeString(var22, "portraits-bust-17");
         }

         for (String var28 : List.of("air", "air_bursting", "air_empty", "armor_empty", "armor_half", "armor_full")) {
            String var30 = "pack/assets/minecraft/textures/gui/sprites/hud/" + var28 + ".png";

            try (InputStream var31 = this.getResource("default/" + var30)) {
               if (var31 != null) {
                  Path var10 = var2.toPath().resolve(var30);
                  Files.createDirectories(var10.getParent());
                  Files.copy(var31, var10, StandardCopyOption.REPLACE_EXISTING);
               }
            }
         }
      } catch (IOException var18) {
         this.getLogger().log(Level.SEVERE, "Failed to ensure CoreHUD extra files.", var18);
      }
   }

   private void ensureGps3dAssets(File var1) throws IOException {
      Path var2 = var1.toPath().resolve("gps-3d-schema.txt");
      if (!Files.exists(var2) || !Files.readString(var2).equals("gps-3d-2")) {
         for (String var4 : List.of(
            "assets/font/gps-gyro.yml",
            "textures/assets/enorialshud/gps_pose.png",
            "pack/assets/minecraft/shaders/core/text.vsh",
            "pack/assets/minecraft/shaders/core/text.fsh",
            "pack/assets/minecraft/shaders/include/gps3d/vertex_main.glsl",
            "pack/assets/minecraft/shaders/include/gps3d/model.glsl",
            "pack/assets/minecraft/shaders/include/guiminimap/vertex_main.glsl"
         )) {
            try (InputStream var5 = this.getResource("default/" + var4)) {
               if (var5 == null) {
                  throw new IOException("Missing GPS 3D resource: " + var4);
               }

               Path var6 = var1.toPath().resolve(var4);
               Files.createDirectories(var6.getParent());
               Files.copy(var5, var6, StandardCopyOption.REPLACE_EXISTING);
            }
         }

         File var11 = new File(var1, "assets/hud/gps-panel.yml");
         this.copyMissingResource("default/assets/hud/gps-panel.yml", var11);
         YamlConfiguration var12 = YamlConfiguration.loadConfiguration(var11);

         for (String var15 : List.of("gps", "gps-compact")) {
            String var7 = var15 + ".layers.arrow";
            boolean var8 = !var12.getString(var7 + ".font", "").equals("gps-gyro");
            var12.set(var7 + ".font", "gps-gyro");
            if (var8) {
               var12.set(var7 + ".offset.x", var12.getInt(var7 + ".offset.x", 7) - 2);
               var12.set(var7 + ".offset.y", var12.getInt(var7 + ".offset.y", 6) - 2);
            }
         }

         var12.save(var11);
         File var14 = new File(var1, "assets/font/hud-modern.yml");
         if (var14.exists()) {
            String var16 = Files.readString(var14.toPath());
            var16 = var16.replaceFirst("(?ms)^gps-arrows:\\r?\\n.*?(?=^[A-Za-z0-9_-]+:|\\z)", "");
            Files.writeString(var14.toPath(), var16);
         }

         Files.writeString(var2, "gps-3d-2");
      }
   }

   private void ensureSpellGuiPackAssets(File var1) throws IOException {
      Path var2 = var1.toPath().resolve("pack");
      Path var3 = var2.resolve("assets").resolve("enorials");
      ResourceCopier.copyDirectoryFromJar(this, "default/pack/assets/enorials", var3);
   }

   private void migratePartyPortraitDepth(File var1) throws IOException {
      if (var1.isFile()) {
         YamlConfiguration var2 = YamlConfiguration.loadConfiguration(var1);
         boolean var3 = false;

         for (String var5 : List.of("small", "medium", "large")) {
            for (int var6 = 0; var6 < 8; var6++) {
               String var7 = "party-row-" + var5 + "-" + var6 + ".layers.";
               if ((var2.getDouble(var7 + "head.offset.z", -1.0) == 4.0 || var2.getDouble(var7 + "head.offset.z", -1.0) == 2.0)
                  && var2.getDouble(var7 + "frame.offset.z", -1.0) == 1.0
                  && var2.getDouble(var7 + "health.offset.z", -1.0) == 2.0) {
                  var2.set(var7 + "head.offset.z", 0);
                  var3 = true;
               }
            }
         }

         if (var3) {
            var2.save(var1);
         }
      }
   }

   private void migrateTargetHealthPosition(File var1) throws IOException {
      YamlConfiguration var2 = YamlConfiguration.loadConfiguration(var1);
      String var3 = "target-frame.layers.";
      if (var2.getInt(var3 + "target-health.offset.y", -1) == 24
         && var2.getInt(var3 + "target-health-background.offset.y", -1) == 10
         && var2.getInt(var3 + "target-health-fill.offset.y", -1) == 12) {
         var2.set(var3 + "target-health.offset.y", 13);
         var2.set(var3 + "target-health.offset.z", 8);
         var2.save(var1);
      }
   }

   private void ensureConfigDefaults() {
      try {
         ServerResourcePacks.initialize(this.getDataFolder().toPath());
         File var1 = new File(this.getDataFolder(), "config.yml");
         if (var1.exists()) {
            YamlConfiguration var2 = YamlConfiguration.loadConfiguration(var1);

            YamlConfiguration var3;
            try (InputStream var4 = this.getResource("default/config.yml")) {
               if (var4 == null) {
                  return;
               }

               var3 = YamlConfiguration.loadConfiguration(new InputStreamReader(var4));
            }

            boolean var10 = false;
            if (!var2.isSet("pack-build.max-concurrent-builds")) {
               var2.set("pack-build.max-concurrent-builds", 1);
               var10 = true;
            }

            for (String var6 : List.of("allow-player-move", "pack-build.rebuild-on-move", "pack-build.async")) {
               if (var2.isSet(var6)) {
                  var2.set(var6, null);
                  var10 = true;
               }
            }

            var10 |= this.mergeMissingSectionKeys(var2, var3, "upload.github");
            var10 |= this.mergeMissingSectionKeys(var2, var3, "avatars.cleanup");
            var10 |= this.mergeMissingSectionKeys(var2, var3, "avatars.name-lookup");
            var10 |= this.mergeMissingSectionKeys(var2, var3, "server-resource-packs");
            var10 |= this.mergeMissingSectionKeys(var2, var3, "social");
            var10 |= this.mergeMissingSectionKeys(var2, var3, "trading");
            if (var10) {
               var2.save(var1);
            }

            return;
         }
      } catch (Exception var9) {
         this.getLogger().log(Level.WARNING, "Failed to ensure config defaults.", var9);
      }
   }

   private boolean mergeMissingSectionKeys(YamlConfiguration var1, YamlConfiguration var2, String var3) {
      if (var3 != null && !var3.isBlank() && var2.isSet(var3)) {
         if (!var1.isSet(var3)) {
            var1.set(var3, var2.get(var3));
            return true;
         }

         if (!var2.isConfigurationSection(var3)) {
            return false;
         }

         boolean var4 = false;

         for (String var6 : var2.getConfigurationSection(var3).getKeys(true)) {
            String var7 = var3 + "." + var6;
            if (!var1.isSet(var7)) {
               var1.set(var7, var2.get(var7));
               var4 = true;
            }
         }

         return var4;
      } else {
         return false;
      }
   }

   private void syncManagedDefaults() throws IOException {
      Path var1 = this.getDataFolder().toPath();
      ResourceCopier.syncDirectoryFromJar(this, "default/pack", var1.resolve("pack"), true);
      ResourceCopier.syncDirectoryFromJar(this, "default/assets", var1.resolve("assets"), true);
      ResourceCopier.syncDirectoryFromJar(this, "default/layout", var1.resolve("layout"), true);
      ResourceCopier.syncDirectoryFromJar(this, "default/textures", var1.resolve("textures"), true);
      ResourceCopier.syncDirectoryFromJar(this, "default/popup", var1.resolve("popup"), true);
      ResourceCopier.syncDirectoryFromJar(this, "default/listeners", var1.resolve("listeners"), true);
   }

   private void copyMissingResource(String var1, File var2) throws IOException {
      if (!var2.exists()) {
         InputStream var3 = this.getResource(var1);
         if (var3 != null) {
            Files.createDirectories(var2.toPath().getParent());

            try (var3) {
               Files.copy(var3, var2.toPath());
            }
         }
      }
   }

   private void mergeMissingTopLevel(String var1, File var2) throws IOException {
      if (!var2.exists()) {
         this.copyMissingResource(var1, var2);
      } else {
         YamlConfiguration var3 = YamlConfiguration.loadConfiguration(var2);

         YamlConfiguration var4;
         try (InputStream var5 = this.getResource(var1)) {
            if (var5 == null) {
               return;
            }

            var4 = YamlConfiguration.loadConfiguration(new InputStreamReader(var5));
         }

         boolean var10 = false;

         for (String var7 : var4.getKeys(false)) {
            if (!var3.isSet(var7)) {
               var3.set(var7, var4.get(var7));
               var10 = true;
            }
         }

         if (var10) {
            var3.save(var2);
         }
      }
   }

   private void overwriteTopLevelSection(String var1, File var2, String var3) throws IOException {
      if (var2.exists()) {
         YamlConfiguration var4 = YamlConfiguration.loadConfiguration(var2);

         YamlConfiguration var5;
         try (InputStream var6 = this.getResource(var1)) {
            if (var6 == null) {
               return;
            }

            var5 = YamlConfiguration.loadConfiguration(new InputStreamReader(var6));
         }

         if (var5.isSet(var3)) {
            Object var11 = var5.get(var3);
            Object var7 = var4.get(var3);
            if (!Objects.equals(var7, var11)) {
               var4.set(var3, var11);
               var4.save(var2);
               return;
            }
         }
      }
   }

   private void mergeMissingLayoutElements(String var1, File var2, String var3) throws IOException {
      if (var2.exists()) {
         YamlConfiguration var4 = YamlConfiguration.loadConfiguration(var2);

         YamlConfiguration var5;
         try (InputStream var6 = this.getResource(var1)) {
            if (var6 == null) {
               return;
            }

            var5 = YamlConfiguration.loadConfiguration(new InputStreamReader(var6));
         }

         String var13 = var3 + ".elements";
         if (var5.isConfigurationSection(var13) && var4.isConfigurationSection(var3 + ".elements")) {
            boolean var7 = false;

            for (String var9 : var5.getConfigurationSection(var13).getKeys(false)) {
               String var10 = var3 + ".elements." + var9;
               if (!var4.isSet(var10)) {
                  var4.set(var10, var5.get(var13 + "." + var9));
                  var7 = true;
               }
            }

            if (var7) {
               var4.save(var2);
               return;
            }
         }
      }
   }

   private void overwriteLayoutElement(String var1, File var2, String var3, String var4) throws IOException {
      if (var2.exists()) {
         YamlConfiguration var5 = YamlConfiguration.loadConfiguration(var2);

         YamlConfiguration var6;
         try (InputStream var7 = this.getResource(var1)) {
            if (var7 == null) {
               return;
            }

            var6 = YamlConfiguration.loadConfiguration(new InputStreamReader(var7));
         }

         String var13 = var3 + ".elements." + var4;
         String var8 = var3 + ".elements." + var4;
         if (var6.isSet(var13)) {
            Object var9 = var6.get(var13);
            Object var10 = var5.get(var8);
            if (!Objects.equals(var10, var9)) {
               var5.set(var8, var9);
               var5.save(var2);
               return;
            }
         }
      }
   }

   private void moveLayoutElementToEnd(File var1, String var2, String var3) throws IOException {
      if (var1.exists()) {
         YamlConfiguration var4 = YamlConfiguration.loadConfiguration(var1);
         String var5 = var2 + ".elements";
         if (var4.isConfigurationSection(var5)) {
            String var6 = var5 + "." + var3;
            if (var4.isSet(var6)) {
               ConfigurationSection var7 = var4.getConfigurationSection(var5);
               if (var7 != null) {
                  LinkedHashMap var8 = new LinkedHashMap();
                  Object var9 = var7.get(var3);

                  for (String var11 : var7.getKeys(false)) {
                     if (!var3.equals(var11)) {
                        var8.put(var11, var7.get(var11));
                     }
                  }

                  var8.put(var3, var9);
                  var4.set(var5, null);

                  for (Entry var13 : var8.entrySet()) {
                     var4.set(var5 + "." + (String)var13.getKey(), var13.getValue());
                  }

                  var4.save(var1);
               }
            }
         }
      }
   }

   private void removeLegacyCornerDuplicates(File var1) throws IOException {
      if (var1.exists()) {
         YamlConfiguration var2 = YamlConfiguration.loadConfiguration(var1);
         String var3 = "enorialscorehud-corners-layout.elements.";
         String[] var4 = new String[]{
            "corner-tl-text",
            "corner-tr-panel",
            "corner-tr-icon",
            "corner-tr-health",
            "corner-tr-mana",
            "corner-tr-hunger",
            "corner-tr-text",
            "corner-bl-panel",
            "corner-bl-icon",
            "corner-bl-health",
            "corner-bl-mana",
            "corner-bl-hunger",
            "corner-bl-text",
            "corner-br-panel",
            "corner-br-icon",
            "corner-br-health",
            "corner-br-mana",
            "corner-br-hunger",
            "corner-br-text"
         };
         boolean var5 = false;

         for (String var9 : var4) {
            String var10 = var3 + var9;
            if (var2.isSet(var10)) {
               var2.set(var10, null);
               var5 = true;
            }
         }

         if (var5) {
            var2.save(var1);
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
      File var1 = this.getDataFolder();
      return !new File(var1, "pack/pack.mcmeta").exists()
         || !new File(var1, "assets/hud").isDirectory()
         || !new File(var1, "layout/default-layouts.yml").exists()
         || !new File(var1, "textures").isDirectory();
   }

   public File ensureDataFile(String var1) {
      File var2 = new File(this.getDataFolder(), var1);
      if (!var2.exists()) {
         try {
            Files.createDirectories(var2.getParentFile().toPath());
            Files.createFile(var2.toPath());
         } catch (IOException var4) {
            this.getLogger().log(Level.SEVERE, "Failed to create data file: " + var1, var4);
         }
      }

      return var2;
   }
}
