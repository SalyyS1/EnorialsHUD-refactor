package sk.enorials.gps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import sk.enorials.gps.api.EnorialsGpsApi;
import sk.enorials.gps.api.internal.EnorialsGpsApiImpl;
import sk.enorials.gps.command.GpsApiCommand;
import sk.enorials.gps.config.SplitConfigManager;
import sk.enorials.gps.data.FavoritesService;
import sk.enorials.gps.data.QuestRouteService;
import sk.enorials.gps.data.RoutePersistenceService;
import sk.enorials.gps.gui.GpsGuiListener;
import sk.enorials.gps.gui.GpsGuiService;
import sk.enorials.gps.input.ChatInputListener;
import sk.enorials.gps.input.ChatInputService;
import sk.enorials.gps.integration.BetonQuestBridge;
import sk.enorials.gps.integration.IntegrationManager;
import sk.enorials.gps.listener.PlayerSessionListener;
import sk.enorials.gps.nav.NavigationManager;
import sk.enorials.gps.util.DetailedErrorLogService;
import sk.enorials.gps.util.Message;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.module.HudModule;
import sk.enorials.minimap.quest.QuestMarkerMappings;

public final class EnorialsGpsPlugin extends HudModule {
   private GpsGuiService gui;
   private GpsApiCommand apiCommand;
   private Message message;
   private NavigationManager navigationManager;
   private EnorialsGpsApi apiService;
   private SplitConfigManager splitConfigManager;
   private DetailedErrorLogService detailedErrorLogService;
   private QuestMarkerMappings.Store questMarkerMappings;

   public EnorialsGpsPlugin(EnorialsHUD var1) {
      super(var1, "gps", "EnorialsGPS");
   }

   public GpsGuiService gui() {
      return this.gui;
   }

   public NavigationManager navigation() {
      return this.navigationManager;
   }

   public GpsApiCommand apiCommand() {
      return this.apiCommand;
   }

   public EnorialsGpsApi api() {
      return this.apiService;
   }

   public QuestMarkerMappings questMarkerMappings() {
      return this.questMarkerMappings == null ? QuestMarkerMappings.defaults() : this.questMarkerMappings.reloadIfChanged();
   }

   public void onEnable() {
      String var1 = this.getDescription().getVersion();
      if (var1.contains("${")) {
         this.getLogger().warning("plugin.yml version token was not expanded. Rebuild the jar with Gradle before deploying.");
      }

      this.saveDefaultConfig();
      this.reloadConfig();
      this.splitConfigManager = new SplitConfigManager(this, super.getConfig());
      this.splitConfigManager.load();
      this.questMarkerMappings = new QuestMarkerMappings.Store(this);
      this.exportDeveloperApiFileOnFirstSetup();
      this.exportEulaFileOnFirstSetup();
      this.detailedErrorLogService = new DetailedErrorLogService(this);
      this.detailedErrorLogService.install();
      this.message = new Message(this);
      QuestRouteService var2 = new QuestRouteService(this);
      IntegrationManager var3 = new IntegrationManager(this, var2);
      BetonQuestBridge var4 = new BetonQuestBridge(this);
      RoutePersistenceService var5 = new RoutePersistenceService(this);
      FavoritesService var6 = new FavoritesService(this);
      this.navigationManager = new NavigationManager(this, var3, var4, var5);
      ChatInputService var7 = new ChatInputService(this, this.navigationManager, var3);
      GpsGuiService var8 = new GpsGuiService(this, this.navigationManager, var7, var3, var6);
      this.apiService = new EnorialsGpsApiImpl(this, this.navigationManager, var3);
      this.gui = var8;
      this.apiCommand = new GpsApiCommand(this.navigationManager, var3);
      this.registerListeners(var8, var7);
      this.getServer().getServicesManager().register(EnorialsGpsApi.class, this.apiService, this.owner(), ServicePriority.Normal);
      this.getLogger().info("GPS module enabled.");
   }

   public void onDisable() {
      if (this.navigationManager != null) {
         this.navigationManager.shutdown();
      }

      if (this.apiService != null) {
         this.getServer().getServicesManager().unregister(this.apiService);
      }

      this.apiService = null;
      if (this.detailedErrorLogService != null) {
         this.detailedErrorLogService.uninstall();
         this.detailedErrorLogService = null;
      }

      this.getLogger().info("GPS module disabled.");
   }

   private void registerListeners(GpsGuiService var1, ChatInputService var2) {
      PluginManager var3 = this.getServer().getPluginManager();
      var3.registerEvents(new GpsGuiListener(var1), this.owner());
      var3.registerEvents(new ChatInputListener(this, var2), this.owner());
      var3.registerEvents(new PlayerSessionListener(this.navigationManager), this.owner());
   }

   public Message msg() {
      return this.message;
   }

   public void reloadPlaces() {
      this.splitConfigManager.load();
   }

   public FileConfiguration config() {
      return this.splitConfigManager.config();
   }

   public boolean isModeEnabled(NavigationMode var1) {
      return var1 == NavigationMode.HUD ? true : this.config().getBoolean("modes." + var1.name().toLowerCase(), var1 == NavigationMode.ACTIONBAR);
   }

   public NavigationMode getDefaultMode() {
      String var1 = this.config().getString("modes.default_mode", "hud");
      NavigationMode var2 = NavigationMode.hudMode(NavigationMode.fromConfig(var1));
      return this.isModeEnabled(var2) ? var2 : NavigationMode.ACTIONBAR;
   }

   private void exportDeveloperApiFileOnFirstSetup() {
      this.exportBundledDocIfMissing("docs/DEVELOPER_API_COMPLETE.md", "DEVELOPER_API_COMPLETE.md", "developer API file");
   }

   private void exportEulaFileOnFirstSetup() {
      this.exportBundledDocIfMissing("docs/EULA.md", "EULA.md", "EULA file");
   }

   private void exportBundledDocIfMissing(String var1, String var2, String var3) {
      File var4 = new File(this.getDataFolder(), var2);
      if (!var4.exists()) {
         if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
         }

         try (InputStream var5 = this.getResource(var1)) {
            if (var5 != null) {
               Files.copy(var5, var4.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
         } catch (IOException var10) {
            this.getLogger().warning("Failed to export " + var3 + ": " + var10.getMessage());
         }
      }
   }
}
