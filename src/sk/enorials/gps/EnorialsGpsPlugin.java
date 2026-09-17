/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.PluginManager
 *  org.bukkit.plugin.ServicePriority
 */
package sk.enorials.gps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import sk.enorials.gps.NavigationMode;
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

public final class EnorialsGpsPlugin
extends HudModule {
    private GpsGuiService gui;
    private GpsApiCommand apiCommand;
    private Message message;
    private NavigationManager navigationManager;
    private EnorialsGpsApi apiService;
    private SplitConfigManager splitConfigManager;
    private DetailedErrorLogService detailedErrorLogService;
    private QuestMarkerMappings.Store questMarkerMappings;

    public EnorialsGpsPlugin(EnorialsHUD enorialsHUD) {
        super(enorialsHUD, "gps", "EnorialsGPS");
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
        String string = this.getDescription().getVersion();
        if (string.contains("${")) {
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
        QuestRouteService questRouteService = new QuestRouteService(this);
        IntegrationManager integrationManager = new IntegrationManager(this, questRouteService);
        BetonQuestBridge betonQuestBridge = new BetonQuestBridge(this);
        RoutePersistenceService routePersistenceService = new RoutePersistenceService(this);
        FavoritesService favoritesService = new FavoritesService(this);
        this.navigationManager = new NavigationManager(this, integrationManager, betonQuestBridge, routePersistenceService);
        ChatInputService chatInputService = new ChatInputService(this, this.navigationManager, integrationManager);
        GpsGuiService gpsGuiService = new GpsGuiService(this, this.navigationManager, chatInputService, integrationManager, favoritesService);
        this.apiService = new EnorialsGpsApiImpl(this, this.navigationManager, integrationManager);
        this.gui = gpsGuiService;
        this.apiCommand = new GpsApiCommand(this.navigationManager, integrationManager);
        this.registerListeners(gpsGuiService, chatInputService);
        this.getServer().getServicesManager().register(EnorialsGpsApi.class, (Object)this.apiService, (Plugin)this.owner(), ServicePriority.Normal);
        this.getLogger().info("GPS module enabled.");
    }

    public void onDisable() {
        if (this.navigationManager != null) {
            this.navigationManager.shutdown();
        }
        if (this.apiService != null) {
            this.getServer().getServicesManager().unregister((Object)this.apiService);
        }
        this.apiService = null;
        if (this.detailedErrorLogService != null) {
            this.detailedErrorLogService.uninstall();
            this.detailedErrorLogService = null;
        }
        this.getLogger().info("GPS module disabled.");
    }

    private void registerListeners(GpsGuiService gpsGuiService, ChatInputService chatInputService) {
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents((Listener)new GpsGuiListener(gpsGuiService), (Plugin)this.owner());
        pluginManager.registerEvents((Listener)new ChatInputListener(this, chatInputService), (Plugin)this.owner());
        pluginManager.registerEvents((Listener)new PlayerSessionListener(this.navigationManager), (Plugin)this.owner());
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

    public boolean isModeEnabled(NavigationMode navigationMode) {
        if (navigationMode == NavigationMode.HUD) {
            return true;
        }
        return this.config().getBoolean("modes." + navigationMode.name().toLowerCase(), navigationMode == NavigationMode.ACTIONBAR);
    }

    public NavigationMode getDefaultMode() {
        String string = this.config().getString("modes.default_mode", "hud");
        NavigationMode navigationMode = NavigationMode.hudMode(NavigationMode.fromConfig(string));
        if (this.isModeEnabled(navigationMode)) {
            return navigationMode;
        }
        return NavigationMode.ACTIONBAR;
    }

    private void exportDeveloperApiFileOnFirstSetup() {
        this.exportBundledDocIfMissing("docs/DEVELOPER_API_COMPLETE.md", "DEVELOPER_API_COMPLETE.md", "developer API file");
    }

    private void exportEulaFileOnFirstSetup() {
        this.exportBundledDocIfMissing("docs/EULA.md", "EULA.md", "EULA file");
    }

    private void exportBundledDocIfMissing(String string, String string2, String string3) {
        File file = new File(this.getDataFolder(), string2);
        if (file.exists()) {
            return;
        }
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
        }
        try (InputStream inputStream = this.getResource(string);){
            if (inputStream == null) {
                return;
            }
            Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException iOException) {
            this.getLogger().warning("Failed to export " + string3 + ": " + iOException.getMessage());
        }
    }
}

