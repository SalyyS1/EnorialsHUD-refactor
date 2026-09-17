/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.Vector
 */
package sk.enorials.gps.nav;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.api.event.GpsNavigationModeChangeEvent;
import sk.enorials.gps.api.event.GpsNavigationPreModeChangeEvent;
import sk.enorials.gps.api.event.GpsNavigationPreStartEvent;
import sk.enorials.gps.api.event.GpsNavigationReachEvent;
import sk.enorials.gps.api.event.GpsNavigationStartEvent;
import sk.enorials.gps.api.event.GpsNavigationStopEvent;
import sk.enorials.gps.data.RoutePersistenceService;
import sk.enorials.gps.integration.BetonQuestBridge;
import sk.enorials.gps.integration.IntegrationManager;
import sk.enorials.gps.nav.NavigationSession;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.gps.nav.target.StaticLocationTarget;
import sk.enorials.gps.nav.target.TargetLocator;
import sk.enorials.gps.util.Color;
import sk.enorials.hud.access.HudAccess;

public final class NavigationManager {
    private final EnorialsGpsPlugin plugin;
    private final IntegrationManager integrationManager;
    private final BetonQuestBridge betonQuestBridge;
    private final RoutePersistenceService routePersistence;
    private final Map<UUID, NavigationSession> sessions = new HashMap<UUID, NavigationSession>();
    private final Map<UUID, Location> previousCompassTargets = new HashMap<UUID, Location>();
    private final Map<UUID, NavigationMode> playerModes = new HashMap<UUID, NavigationMode>();
    private final Map<UUID, Boolean> playerShowDistance = new HashMap<UUID, Boolean>();
    private final Map<UUID, Boolean> playerShowVertical = new HashMap<UUID, Boolean>();
    private final Map<UUID, Boolean> playerShowTargetName = new HashMap<UUID, Boolean>();
    private final Map<UUID, Boolean> playerCompactHud = new HashMap<UUID, Boolean>();
    private final Map<UUID, Boolean> playerTargetMarker = new HashMap<UUID, Boolean>();
    private final Map<UUID, RoutePersistenceService.PersistedRoute> pendingResume = new HashMap<UUID, RoutePersistenceService.PersistedRoute>();
    private BukkitTask task;
    private long profilingRuns;
    private double profilingTotalMs;
    private double profilingMaxMs;
    private double profilingLastMs;
    private long profilingSessionSamples;
    private long profilingSpikeCount;
    private long profilingElapsedTicks;
    private long profilingLastSpikeLogNanos;

    public IntegrationManager integrations() {
        return this.integrationManager;
    }

    public List<Location> trackedQuestNpcLocations(Player player) {
        NavigationSession navigationSession = this.sessions.get(player.getUniqueId());
        if (navigationSession == null || !navigationSession.isTrackedQuest()) {
            return List.of();
        }
        String string = navigationSession.source().substring("betonquest-quest:".length());
        ConfigurationSection configurationSection = this.plugin.config().getConfigurationSection("betonquest.auto_routes");
        if (configurationSection == null) {
            return List.of();
        }
        ArrayList arrayList = new ArrayList();
        for (String string2 : configurationSection.getKeys(false)) {
            ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection(string2);
            if (configurationSection2 == null || !string.equals(configurationSection2.getString("quest", "")) || !"NPC".equalsIgnoreCase(configurationSection2.getString("type", ""))) continue;
            try {
                this.integrationManager.resolveDescriptor(player, RouteDescriptor.npc(configurationSection2.getString("value", ""))).ifPresent(targetLocator -> {
                    Location location = targetLocator.resolve(player);
                    if (location != null && location.getWorld() != null) {
                        arrayList.add(location);
                    }
                });
            }
            catch (Exception | LinkageError throwable) {}
        }
        return List.copyOf(arrayList);
    }

    public NavigationManager(EnorialsGpsPlugin enorialsGpsPlugin, IntegrationManager integrationManager, BetonQuestBridge betonQuestBridge, RoutePersistenceService routePersistenceService) {
        this.plugin = enorialsGpsPlugin;
        this.integrationManager = integrationManager;
        this.betonQuestBridge = betonQuestBridge;
        this.routePersistence = routePersistenceService;
        this.pendingResume.putAll(routePersistenceService.getAll());
        this.startTickTask();
    }

    private void startTickTask() {
        long l = this.plugin.config().getLong("navigation.update_interval_ticks", 20L);
        if (l < 1L) {
            l = 1L;
        }
        this.task = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin.owner(), this::tick, l, l);
    }

    public NavigationMode getCurrentOrDefaultMode(Player player) {
        return this.playerModes.getOrDefault(player.getUniqueId(), this.plugin.getDefaultMode());
    }

    public NavigationSession session(Player player) {
        return this.sessions.get(player.getUniqueId());
    }

    public boolean hasActiveNavigation(Player player) {
        return this.sessions.containsKey(player.getUniqueId());
    }

    public boolean isDistanceDisplayEnabled(Player player) {
        return this.playerShowDistance.getOrDefault(player.getUniqueId(), this.plugin.config().getBoolean("navigation.display.show_distance_meters", true));
    }

    public boolean isVerticalDisplayEnabled(Player player) {
        return this.playerShowVertical.getOrDefault(player.getUniqueId(), this.plugin.config().getBoolean("navigation.display.show_vertical_indicator", true));
    }

    public boolean isTargetNameDisplayEnabled(Player player) {
        return this.playerShowTargetName.getOrDefault(player.getUniqueId(), this.plugin.config().getBoolean("navigation.display.show_target_name", true));
    }

    public boolean isCompactHudEnabled(Player player) {
        return this.playerCompactHud.getOrDefault(player.getUniqueId(), this.plugin.config().getBoolean("navigation.display.compact_hud", false));
    }

    public boolean isTargetMarkerEnabled(Player player) {
        if (!this.plugin.config().getBoolean("navigation.target_marker.enabled", true)) {
            return false;
        }
        return this.playerTargetMarker.getOrDefault(player.getUniqueId(), this.plugin.config().getBoolean("navigation.target_marker.default_for_player", true));
    }

    public boolean toggleDistanceDisplay(Player player) {
        boolean bl = !this.isDistanceDisplayEnabled(player);
        this.playerShowDistance.put(player.getUniqueId(), bl);
        this.plugin.msg().send((CommandSender)player, "&eDistance display: " + (bl ? "&aON" : "&cOFF"));
        return bl;
    }

    public boolean toggleVerticalDisplay(Player player) {
        boolean bl = !this.isVerticalDisplayEnabled(player);
        this.playerShowVertical.put(player.getUniqueId(), bl);
        this.plugin.msg().send((CommandSender)player, "&eVertical indicator: " + (bl ? "&aON" : "&cOFF"));
        return bl;
    }

    public boolean toggleTargetNameDisplay(Player player) {
        boolean bl = !this.isTargetNameDisplayEnabled(player);
        this.playerShowTargetName.put(player.getUniqueId(), bl);
        this.plugin.msg().send((CommandSender)player, "&eTarget name in HUD: " + (bl ? "&aON" : "&cOFF"));
        return bl;
    }

    public boolean toggleCompactHud(Player player) {
        boolean bl = !this.isCompactHudEnabled(player);
        this.playerCompactHud.put(player.getUniqueId(), bl);
        this.plugin.msg().send((CommandSender)player, "&eCompact HUD: " + (bl ? "&aON" : "&cOFF"));
        return bl;
    }

    public boolean toggleTargetMarker(Player player) {
        if (!this.plugin.config().getBoolean("navigation.target_marker.enabled", true)) {
            this.plugin.msg().send((CommandSender)player, "&cTarget beacon is disabled by server settings.");
            return false;
        }
        boolean bl = !this.isTargetMarkerEnabled(player);
        this.playerTargetMarker.put(player.getUniqueId(), bl);
        this.plugin.msg().send((CommandSender)player, "&eTarget beacon: " + (bl ? "&aON" : "&cOFF"));
        return bl;
    }

    public void setMode(Player player, NavigationMode navigationMode) {
        NavigationMode navigationMode2 = this.getCurrentOrDefaultMode(player);
        NavigationMode navigationMode3 = NavigationMode.hudMode(navigationMode);
        if (!this.plugin.isModeEnabled(navigationMode3)) {
            boolean bl = this.plugin.config().getBoolean("modes.fallback_to_actionbar", true);
            if (bl) {
                navigationMode3 = NavigationMode.HUD;
            } else {
                this.plugin.msg().send((CommandSender)player, "&cThat navigation mode is disabled.");
                return;
            }
        }
        GpsNavigationPreModeChangeEvent gpsNavigationPreModeChangeEvent = new GpsNavigationPreModeChangeEvent(player, navigationMode2, navigationMode3);
        Bukkit.getPluginManager().callEvent((Event)gpsNavigationPreModeChangeEvent);
        if (gpsNavigationPreModeChangeEvent.isCancelled()) {
            return;
        }
        navigationMode3 = gpsNavigationPreModeChangeEvent.getNewMode();
        if (!this.plugin.isModeEnabled(navigationMode3)) {
            boolean bl = this.plugin.config().getBoolean("modes.fallback_to_actionbar", true);
            if (bl) {
                navigationMode3 = NavigationMode.HUD;
            } else {
                this.plugin.msg().send((CommandSender)player, "&cThat navigation mode is disabled.");
                return;
            }
        }
        this.playerModes.put(player.getUniqueId(), navigationMode3);
        NavigationSession navigationSession = this.sessions.get(player.getUniqueId());
        if (navigationSession != null) {
            navigationSession.setMode(navigationMode3);
            this.clearBossBar(player, navigationSession);
            if (navigationMode3 != NavigationMode.COMPASS) {
                this.clearCompassTarget(player);
            }
            if (navigationSession.descriptor() != null && this.plugin.config().getBoolean("navigation.persist_routes", true)) {
                this.routePersistence.save(player.getUniqueId(), navigationSession.descriptor(), navigationMode3, navigationSession.source());
            }
        }
        if (navigationMode3 != NavigationMode.COMPASS) {
            this.clearCompassTarget(player);
        }
        this.plugin.msg().send((CommandSender)player, "&aNavigation mode set to &f" + navigationMode3.display() + "&a.");
        this.fireModeChangeEvent(player, navigationMode2, navigationMode3);
        this.betonQuestBridge.onModeChange(player, navigationMode3.name().toLowerCase());
    }

    public boolean startNavigation(Player player, TargetLocator targetLocator, NavigationMode navigationMode, String string) {
        return this.startNavigation(player, targetLocator, navigationMode, string, null);
    }

    public boolean startNavigation(Player player, TargetLocator targetLocator, NavigationMode navigationMode, String string, RouteDescriptor routeDescriptor) {
        NavigationSession navigationSession;
        Location location;
        if (!HudAccess.navigation(player, targetLocator, routeDescriptor, string)) {
            HudAccess.check((CommandSender)player, !HudAccess.has((CommandSender)player, "enorials.gps.use") ? "enorials.gps.use" : HudAccess.navigationNode(targetLocator, routeDescriptor, string));
            return false;
        }
        if ("hud-party".equals(string) && !this.plugin.gui().samePartyTarget(player, targetLocator)) {
            return false;
        }
        NavigationMode navigationMode2 = NavigationMode.hudMode(navigationMode);
        if (!this.plugin.isModeEnabled(navigationMode2)) {
            navigationMode2 = NavigationMode.HUD;
        }
        if ((location = targetLocator.resolve(player)) == null) {
            this.plugin.msg().send((CommandSender)player, "&cTarget unavailable.");
            return false;
        }
        if (!this.isTargetAllowed(player, location)) {
            this.plugin.msg().send((CommandSender)player, "&cTarget is blocked by navigation restrictions.");
            return false;
        }
        GpsNavigationPreStartEvent gpsNavigationPreStartEvent = new GpsNavigationPreStartEvent(player, navigationMode2, string, targetLocator.id(), targetLocator.displayName(), routeDescriptor);
        Bukkit.getPluginManager().callEvent((Event)gpsNavigationPreStartEvent);
        if (gpsNavigationPreStartEvent.isCancelled()) {
            return false;
        }
        navigationMode2 = gpsNavigationPreStartEvent.getMode();
        string = gpsNavigationPreStartEvent.getSource();
        if (!this.plugin.isModeEnabled(navigationMode2)) {
            boolean bl = this.plugin.config().getBoolean("modes.fallback_to_actionbar", true);
            if (bl) {
                navigationMode2 = NavigationMode.HUD;
            } else {
                this.plugin.msg().send((CommandSender)player, "&cThat navigation mode is disabled.");
                return false;
            }
        }
        if ((navigationSession = this.sessions.remove(player.getUniqueId())) != null) {
            this.clearBossBar(player, navigationSession);
            this.clearCompassTarget(player);
        }
        NavigationSession navigationSession2 = new NavigationSession(player.getUniqueId(), targetLocator, navigationMode2, string, routeDescriptor);
        navigationSession2.setLastResolvedTarget(location);
        navigationSession2.setLastResolveTick(0);
        this.sessions.put(player.getUniqueId(), navigationSession2);
        this.playerModes.put(player.getUniqueId(), navigationMode2);
        if (navigationMode2 != NavigationMode.COMPASS) {
            this.clearCompassTarget(player);
        }
        if (routeDescriptor != null && this.plugin.config().getBoolean("navigation.persist_routes", true)) {
            this.routePersistence.save(player.getUniqueId(), routeDescriptor, navigationMode2, string);
            this.pendingResume.put(player.getUniqueId(), new RoutePersistenceService.PersistedRoute(routeDescriptor, navigationMode2, string));
        }
        if (this.plugin.owner().betonQuest() != null && this.plugin.owner().betonQuest().questLog() != null) {
            this.plugin.owner().betonQuest().questLog().navigationStarted(player, string);
        }
        this.fireStartEvent(player, navigationMode2, string, targetLocator, routeDescriptor);
        this.plugin.msg().send((CommandSender)player, "&aGPS started: &f" + targetLocator.displayName());
        this.playStartFeedback(player);
        this.betonQuestBridge.onStart(player, string, targetLocator.id());
        return true;
    }

    public boolean stopNavigation(Player player, String string) {
        NavigationSession navigationSession;
        if (!Set.of("quest-stage", "quest-selection").contains(string) && this.plugin.owner().betonQuest() != null && this.plugin.owner().betonQuest().questLog() != null) {
            this.plugin.owner().betonQuest().questLog().stopTracking(player);
        }
        if ((navigationSession = this.sessions.remove(player.getUniqueId())) == null) {
            this.plugin.msg().send((CommandSender)player, "&7No active GPS route.");
            return false;
        }
        this.clearBossBar(player, navigationSession);
        this.clearCompassTarget(player);
        this.fireStopEvent(player, string, navigationSession);
        this.plugin.msg().send((CommandSender)player, "&cGPS stopped.");
        this.clearPersisted(player.getUniqueId());
        this.betonQuestBridge.onStop(player, string, navigationSession.locator().id());
        return true;
    }

    public void tryResume(Player player) {
        if (!this.plugin.config().getBoolean("navigation.resume_on_join", true)) {
            return;
        }
        if (this.sessions.containsKey(player.getUniqueId())) {
            return;
        }
        RoutePersistenceService.PersistedRoute persistedRoute = this.pendingResume.get(player.getUniqueId());
        if (persistedRoute == null) {
            return;
        }
        Optional<TargetLocator> optional = this.integrationManager.resolveDescriptor(player, persistedRoute.descriptor());
        if (optional.isEmpty()) {
            this.plugin.msg().send((CommandSender)player, "&7Stored GPS route is not available right now.");
            return;
        }
        this.startNavigation(player, optional.get(), persistedRoute.mode(), persistedRoute.source(), persistedRoute.descriptor());
    }

    public void shutdown() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        this.persistActiveRoutesOnShutdown();
        for (Map.Entry<UUID, NavigationSession> entry : this.sessions.entrySet()) {
            Player player = Bukkit.getPlayer((UUID)entry.getKey());
            if (player == null) continue;
            this.clearBossBar(player, entry.getValue());
            this.clearCompassTarget(player);
        }
        this.sessions.clear();
        this.playerShowDistance.clear();
        this.playerShowVertical.clear();
        this.playerShowTargetName.clear();
        this.playerCompactHud.clear();
        this.playerTargetMarker.clear();
    }

    public String getProfilingSummary() {
        boolean bl = this.plugin.config().getBoolean("navigation.profiling.enabled", false);
        if (this.profilingRuns <= 0L) {
            return "GPS profiling: enabled=" + bl + ", no samples yet.";
        }
        double d = this.profilingTotalMs / (double)this.profilingRuns;
        double d2 = (double)this.profilingSessionSamples / (double)this.profilingRuns;
        return String.format(Locale.US, "GPS profiling: enabled=%s runs=%d last=%.3fms avg=%.3fms max=%.3fms avgSessions=%.2f active=%d spikes=%d", bl, this.profilingRuns, this.profilingLastMs, d, this.profilingMaxMs, d2, this.sessions.size(), this.profilingSpikeCount);
    }

    public void resetProfilingStats() {
        this.profilingRuns = 0L;
        this.profilingTotalMs = 0.0;
        this.profilingMaxMs = 0.0;
        this.profilingLastMs = 0.0;
        this.profilingSessionSamples = 0L;
        this.profilingSpikeCount = 0L;
        this.profilingElapsedTicks = 0L;
        this.profilingLastSpikeLogNanos = 0L;
    }

    private void persistActiveRoutesOnShutdown() {
        if (!this.plugin.config().getBoolean("navigation.persist_routes", true)) {
            return;
        }
        for (Map.Entry<UUID, NavigationSession> entry : this.sessions.entrySet()) {
            NavigationSession navigationSession;
            UUID uUID = entry.getKey();
            RouteDescriptor routeDescriptor = this.resolveDescriptorForPersistence(uUID, navigationSession = entry.getValue());
            if (routeDescriptor == null) continue;
            this.routePersistence.save(uUID, routeDescriptor, navigationSession.mode(), navigationSession.source());
            this.pendingResume.put(uUID, new RoutePersistenceService.PersistedRoute(routeDescriptor, navigationSession.mode(), navigationSession.source()));
        }
    }

    private RouteDescriptor resolveDescriptorForPersistence(UUID uUID, NavigationSession navigationSession) {
        if (navigationSession.descriptor() != null) {
            return navigationSession.descriptor();
        }
        Player player = Bukkit.getPlayer((UUID)uUID);
        if (player == null || !player.isOnline()) {
            return null;
        }
        Location location = navigationSession.locator().resolve(player);
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return RouteDescriptor.location(location);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void tick() {
        long l = Math.max(1L, this.plugin.config().getLong("navigation.update_interval_ticks", 20L));
        int n = Math.max(1, this.plugin.config().getInt("navigation.target_resolve_interval_ticks", (int)l));
        int n2 = Math.max(1, (int)Math.ceil((double)n / (double)l));
        double d = this.plugin.config().getDouble("navigation.arrive_distance", 4.0);
        boolean bl = this.plugin.config().getBoolean("navigation.profiling.enabled", false);
        long l2 = bl ? System.nanoTime() : 0L;
        try {
            Iterator<Map.Entry<UUID, NavigationSession>> iterator = this.sessions.entrySet().iterator();
            while (iterator.hasNext()) {
                IntegrationManager.AutoRoute autoRoute;
                String string;
                Map.Entry<UUID, NavigationSession> entry = iterator.next();
                Player player = Bukkit.getPlayer((UUID)entry.getKey());
                if (player == null || !player.isOnline()) {
                    iterator.remove();
                    continue;
                }
                NavigationSession navigationSession = entry.getValue();
                if (!HudAccess.navigation(player, navigationSession.locator(), navigationSession.descriptor(), navigationSession.source())) {
                    this.stopSessionAndCleanup(player, navigationSession, iterator, "permission_revoked");
                    continue;
                }
                navigationSession.incrementTick();
                if (navigationSession.source().startsWith("betonquest-quest:") && !navigationSession.locator().id().startsWith("quest:") && (navigationSession.tickCount() == 1 || navigationSession.tickCount() % Math.max(1, (int)Math.ceil(20.0 / (double)l)) == 0)) {
                    string = navigationSession.source().substring("betonquest-quest:".length());
                    autoRoute = this.integrationManager.getAutoRoutes(player).get(string);
                    if (autoRoute == null) {
                        this.stopSessionAndCleanup(player, navigationSession, iterator, "quest_inactive");
                        continue;
                    }
                    if (!autoRoute.descriptor().equals(navigationSession.descriptor()) || !ChatColor.stripColor((String)Color.colorize(autoRoute.displayName())).equals(navigationSession.locator().displayName())) {
                        Optional<TargetLocator> optional = this.integrationManager.resolveAutoRoute(player, autoRoute);
                        if (optional.isEmpty()) {
                            this.stopSessionAndCleanup(player, navigationSession, iterator, "target_unavailable");
                            continue;
                        }
                        navigationSession.updateQuestTarget(optional.get(), autoRoute.descriptor());
                        if (this.plugin.config().getBoolean("navigation.persist_routes", true)) {
                            this.routePersistence.save(player.getUniqueId(), autoRoute.descriptor(), navigationSession.mode(), navigationSession.source());
                        }
                    }
                }
                if (navigationSession.source().equals("hud-party") && !this.plugin.gui().samePartyTarget(player, navigationSession.locator())) {
                    this.stopSessionAndCleanup(player, navigationSession, iterator, "party_left");
                    continue;
                }
                if (!this.isWorldAllowed(player.getWorld().getName())) {
                    this.plugin.msg().send((CommandSender)player, "&cNavigation stopped in restricted world.");
                    this.stopSessionAndCleanup(player, navigationSession, iterator, "restricted_world");
                    continue;
                }
                string = this.resolveTargetForTick(player, navigationSession, n2);
                if (string == null) {
                    this.plugin.msg().send((CommandSender)player, "&cTarget unavailable.");
                    this.stopSessionAndCleanup(player, navigationSession, iterator, "target_unavailable");
                    continue;
                }
                if (!this.isTargetAllowed(player, (Location)string)) {
                    this.plugin.msg().send((CommandSender)player, "&cNavigation stopped: target entered blocked area.");
                    this.stopSessionAndCleanup(player, navigationSession, iterator, "target_blocked");
                    continue;
                }
                if (!player.getWorld().equals((Object)string.getWorld())) continue;
                autoRoute = this.resolveGuidanceTarget(player, navigationSession, (Location)string);
                double d2 = player.getLocation().distance((Location)string);
                if (d2 <= d && navigationSession.isTrackedQuest()) {
                    if (navigationSession.markQuestArrival()) {
                        this.onReached(player, navigationSession);
                    }
                    this.render(player, navigationSession, (Location)autoRoute, d2);
                    continue;
                }
                if (d2 <= d) {
                    this.onReached(player, navigationSession);
                    this.clearBossBar(player, navigationSession);
                    this.clearCompassTarget(player);
                    iterator.remove();
                    this.clearPersisted(player.getUniqueId());
                    continue;
                }
                this.render(player, navigationSession, (Location)autoRoute, d2);
                this.renderTargetMarker(player, (Location)string, navigationSession, d2);
                this.playPulseFeedback(player, (Location)autoRoute, navigationSession);
            }
        }
        finally {
            if (bl) {
                double d3 = (double)(System.nanoTime() - l2) / 1000000.0;
                this.recordProfilingSample(d3, l);
            }
        }
    }

    private void recordProfilingSample(double d, long l) {
        int n;
        ++this.profilingRuns;
        this.profilingLastMs = d;
        this.profilingTotalMs += d;
        this.profilingMaxMs = Math.max(this.profilingMaxMs, d);
        this.profilingSessionSamples += (long)this.sessions.size();
        this.profilingElapsedTicks += l;
        double d2 = Math.max(0.1, this.plugin.config().getDouble("navigation.profiling.warn_above_ms", 10.0));
        if (d >= d2) {
            ++this.profilingSpikeCount;
            if (this.plugin.config().getBoolean("navigation.profiling.log_spikes", false)) {
                long l2 = System.nanoTime();
                if (this.profilingLastSpikeLogNanos == 0L || l2 - this.profilingLastSpikeLogNanos >= 5000000000L) {
                    this.profilingLastSpikeLogNanos = l2;
                    this.plugin.getLogger().warning(String.format(Locale.US, "[GPS-Perf] Spike detected: %.3fms (warn threshold %.3fms, active sessions=%d)", d, d2, this.sessions.size()));
                }
            }
        }
        if (this.profilingElapsedTicks >= (long)(n = Math.max(20, this.plugin.config().getInt("navigation.profiling.log_interval_ticks", 1200)))) {
            this.plugin.getLogger().info("[GPS-Perf] " + this.getProfilingSummary());
            if (this.plugin.config().getBoolean("navigation.profiling.reset_after_log", true)) {
                this.resetProfilingStats();
            } else {
                this.profilingElapsedTicks = 0L;
            }
        }
    }

    private Location resolveTargetForTick(Player player, NavigationSession navigationSession, int n) {
        if (navigationSession.locator() instanceof StaticLocationTarget) {
            Location location = navigationSession.lastResolvedTarget();
            if (location != null) {
                return location;
            }
            Location location2 = navigationSession.locator().resolve(player);
            navigationSession.setLastResolvedTarget(location2);
            navigationSession.setLastResolveTick(navigationSession.tickCount());
            return location2;
        }
        if (n <= 1) {
            Location location = navigationSession.locator().resolve(player);
            navigationSession.setLastResolvedTarget(location);
            navigationSession.setLastResolveTick(navigationSession.tickCount());
            return location;
        }
        int n2 = navigationSession.lastResolveTick();
        Location location = navigationSession.lastResolvedTarget();
        boolean bl = location == null || n2 < 0 || navigationSession.tickCount() - n2 >= n;
        boolean bl2 = bl;
        if (bl) {
            Location location3 = navigationSession.locator().resolve(player);
            navigationSession.setLastResolvedTarget(location3);
            navigationSession.setLastResolveTick(navigationSession.tickCount());
            return location3;
        }
        return location;
    }

    private void stopSessionAndCleanup(Player player, NavigationSession navigationSession, Iterator<Map.Entry<UUID, NavigationSession>> iterator, String string) {
        this.clearBossBar(player, navigationSession);
        this.clearCompassTarget(player);
        iterator.remove();
        this.fireStopEvent(player, string, navigationSession);
        this.clearPersisted(player.getUniqueId());
        this.betonQuestBridge.onStop(player, string, navigationSession.locator().id());
    }

    private void render(Player player, NavigationSession navigationSession, Location location, double d) {
        if (navigationSession.mode() == NavigationMode.COMPASS) {
            this.previousCompassTargets.putIfAbsent(player.getUniqueId(), player.getCompassTarget().clone());
            player.setCompassTarget(location);
        }
    }

    private void onReached(Player player, NavigationSession navigationSession) {
        String string = navigationSession.locator().displayName();
        this.plugin.msg().send((CommandSender)player, "&aReached: &f" + string);
        this.plugin.msg().send((CommandSender)player, "&aYou reached &f" + string + "&a.");
        if (this.plugin.config().getBoolean("effects.sounds", true)) {
            Sound sound = this.parseSound(this.plugin.config().getString("effects.reach_sound", "UI_TOAST_CHALLENGE_COMPLETE"), Sound.UI_TOAST_CHALLENGE_COMPLETE);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
        if (this.plugin.config().getBoolean("effects.particles", true)) {
            player.getWorld().spawnParticle(this.parseParticle(this.plugin.config().getString("effects.reach_particle", "TOTEM"), Particle.TOTEM_OF_UNDYING), player.getLocation().add(0.0, 1.0, 0.0), 40, 0.6, 0.6, 0.6, 0.02);
        }
        this.fireReachEvent(player, navigationSession);
        this.betonQuestBridge.onReach(player, navigationSession.locator().id());
    }

    private void playStartFeedback(Player player) {
        if (this.plugin.config().getBoolean("effects.sounds", true)) {
            Sound sound = this.parseSound(this.plugin.config().getString("effects.start_sound", "ENTITY_EXPERIENCE_ORB_PICKUP"), Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
            player.playSound(player.getLocation(), sound, 0.8f, 1.2f);
        }
        if (this.plugin.config().getBoolean("effects.particles", true)) {
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0.0, 1.0, 0.0), 12, 0.35, 0.35, 0.35, 0.01);
        }
    }

    private void playPulseFeedback(Player player, Location location, NavigationSession navigationSession) {
    }

    private void renderTargetMarker(Player player, Location location, NavigationSession navigationSession, double d) {
        if (!this.isTargetMarkerEnabled(player)) {
            return;
        }
        if (location == null || location.getWorld() == null || !player.getWorld().equals((Object)location.getWorld())) {
            return;
        }
        double d2 = Math.max(0.0, this.plugin.config().getDouble("navigation.target_marker.min_distance_meters", 20.0));
        if (d < d2) {
            return;
        }
        int n = Math.max(1, this.plugin.config().getInt("navigation.target_marker.interval_updates", 2));
        if (navigationSession.tickCount() % n != 0) {
            return;
        }
        double d3 = Math.max(3.0, this.plugin.config().getDouble("navigation.target_marker.beam_height", 42.0));
        double d4 = Math.max(0.5, this.plugin.config().getDouble("navigation.target_marker.beam_step", 2.5));
        int n2 = Math.max(1, this.plugin.config().getInt("navigation.target_marker.beam_count", 2));
        double d5 = Math.max(0.0, this.plugin.config().getDouble("navigation.target_marker.beam_spread", 0.05));
        int n3 = Math.max(1, this.plugin.config().getInt("navigation.target_marker.top_count", 16));
        double d6 = Math.max(0.0, this.plugin.config().getDouble("navigation.target_marker.top_spread", 0.35));
        Particle particle = this.parseParticle(this.plugin.config().getString("navigation.target_marker.beam_particle", "END_ROD"), Particle.END_ROD);
        Particle particle2 = this.parseParticle(this.plugin.config().getString("navigation.target_marker.top_particle", "TOTEM"), Particle.TOTEM_OF_UNDYING);
        double d7 = location.getX();
        double d8 = location.getY();
        double d9 = location.getZ();
        for (double d10 = 0.0; d10 <= d3; d10 += d4) {
            player.spawnParticle(particle, d7, d8 + d10, d9, n2, d5, 0.02, d5, 0.0);
        }
        player.spawnParticle(particle2, d7, d8 + d3 + 0.5, d9, n3, d6, d6, d6, 0.01);
    }

    private void clearBossBar(Player player, NavigationSession navigationSession) {
        if (navigationSession.bossBar() != null) {
            player.hideBossBar(navigationSession.bossBar());
            navigationSession.setBossBar(null);
        }
    }

    private void clearCompassTarget(Player player) {
        if (player == null || player.getWorld() == null) {
            return;
        }
        Location location = this.previousCompassTargets.remove(player.getUniqueId());
        if (location != null) {
            player.setCompassTarget(location);
        }
    }

    private String getDirectionArrow(Location location, Location location2) {
        Vector vector = location2.clone().subtract(location).toVector();
        double d = Math.toDegrees(Math.atan2(-vector.getX(), vector.getZ()));
        double d2 = this.normalizeDegrees(d - (double)location.getYaw());
        String[] stringArray = new String[]{"\u2b06\u2b06", "\u2b06\u2197", "\u2197\u2197", "\u2197\u27a1", "\u27a1\u27a1", "\u27a1\u2198", "\u2198\u2198", "\u2198\u2b07", "\u2b07\u2b07", "\u2b07\u2199", "\u2199\u2199", "\u2199\u2b05", "\u2b05\u2b05", "\u2b05\u2196", "\u2196\u2196", "\u2196\u2b06"};
        int n = (int)Math.floor((d2 + 11.25) / 22.5);
        n = (n % 16 + 16) % 16;
        return stringArray[n];
    }

    private String getVerticalIndicator(Location location, Location location2) {
        double d = location2.getY() - location.getY();
        if (d > 1.5) {
            return "&a\u25b2 +" + Math.round(d) + "Y";
        }
        if (d < -1.5) {
            return "&c\u25bc " + Math.round(d) + "Y";
        }
        return "&7\u25a0 0Y";
    }

    private double normalizeDegrees(double d) {
        double d2 = d % 360.0;
        if (d2 >= 180.0) {
            d2 -= 360.0;
        }
        if (d2 < -180.0) {
            d2 += 360.0;
        }
        return d2;
    }

    private Sound parseSound(String string, Sound sound) {
        try {
            return Sound.valueOf((String)string.toUpperCase());
        }
        catch (Exception exception) {
            return sound;
        }
    }

    private Particle parseParticle(String string, Particle particle) {
        try {
            return Particle.valueOf((String)string.toUpperCase());
        }
        catch (Exception exception) {
            return particle;
        }
    }

    private boolean isTargetAllowed(Player player, Location location) {
        return this.isWorldAllowed(player.getWorld().getName()) && this.isWorldAllowed(location.getWorld().getName()) && !this.isInsideBlockedCuboid(location);
    }

    private boolean isWorldAllowed(String string) {
        if (!this.plugin.config().getBoolean("restrictions.enabled", false)) {
            return true;
        }
        List list = this.plugin.config().getStringList("restrictions.allowed_worlds");
        List list2 = this.plugin.config().getStringList("restrictions.denied_worlds");
        if (!list.isEmpty() && !list.contains(string)) {
            return false;
        }
        return !list2.contains(string);
    }

    private boolean isInsideBlockedCuboid(Location location) {
        if (!this.plugin.config().getBoolean("restrictions.enabled", false)) {
            return false;
        }
        ConfigurationSection configurationSection = this.plugin.config().getConfigurationSection("restrictions.blocked_cuboids");
        if (configurationSection == null) {
            return false;
        }
        for (String string : configurationSection.getKeys(false)) {
            ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection(string);
            if (configurationSection2 == null) continue;
            String string2 = configurationSection2.getString("world", "");
            if (!location.getWorld().getName().equalsIgnoreCase(string2)) continue;
            int[] nArray = this.parseTriplet(configurationSection2.getString("min", "0,0,0"));
            int[] nArray2 = this.parseTriplet(configurationSection2.getString("max", "0,0,0"));
            int n = Math.min(nArray[0], nArray2[0]);
            int n2 = Math.max(nArray[0], nArray2[0]);
            int n3 = Math.min(nArray[1], nArray2[1]);
            int n4 = Math.max(nArray[1], nArray2[1]);
            int n5 = Math.min(nArray[2], nArray2[2]);
            int n6 = Math.max(nArray[2], nArray2[2]);
            int n7 = location.getBlockX();
            int n8 = location.getBlockY();
            int n9 = location.getBlockZ();
            if (n7 < n || n7 > n2 || n8 < n3 || n8 > n4 || n9 < n5 || n9 > n6) continue;
            return true;
        }
        return false;
    }

    private int[] parseTriplet(String string) {
        String[] stringArray = string.split(",");
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (String string2 : stringArray) {
            try {
                arrayList.add(Integer.parseInt(string2.trim()));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        while (arrayList.size() < 3) {
            arrayList.add(0);
        }
        return new int[]{(Integer)arrayList.get(0), (Integer)arrayList.get(1), (Integer)arrayList.get(2)};
    }

    private void clearPersisted(UUID uUID) {
        this.pendingResume.remove(uUID);
        if (this.plugin.config().getBoolean("navigation.persist_routes", true)) {
            this.routePersistence.clear(uUID);
        }
    }

    private Location resolveGuidanceTarget(Player player, NavigationSession navigationSession, Location location) {
        if (!this.plugin.config().getBoolean("navigation.long_route_waypoints.enabled", true)) {
            navigationSession.setWaypoint(null);
            return location;
        }
        if (!player.getWorld().equals((Object)location.getWorld())) {
            navigationSession.setWaypoint(null);
            return location;
        }
        double d = this.plugin.config().getDouble("navigation.long_route_waypoints.long_distance_threshold", 250.0);
        double d2 = this.plugin.config().getDouble("navigation.long_route_waypoints.waypoint_step_distance", 120.0);
        int n = this.plugin.config().getInt("navigation.long_route_waypoints.recalc_interval_ticks", 40);
        double d3 = player.getLocation().distance(location);
        if (d3 < d || d2 <= 0.0) {
            navigationSession.setWaypoint(null);
            return location;
        }
        Location location2 = navigationSession.waypoint();
        boolean bl = location2 == null || !location2.getWorld().equals((Object)player.getWorld()) || player.getLocation().distance(location2) <= Math.max(2.0, this.plugin.config().getDouble("navigation.arrive_distance", 4.0)) || (navigationSession.tickCount() - navigationSession.lastWaypointRecalcTick()) * this.plugin.config().getInt("navigation.update_interval_ticks", 20) >= n;
        boolean bl2 = bl;
        if (bl) {
            Location location3 = this.computeWaypoint(player.getLocation(), location, d2);
            navigationSession.setWaypoint(location3);
            navigationSession.setLastWaypointRecalcTick(navigationSession.tickCount());
            return location3;
        }
        return location2;
    }

    private Location computeWaypoint(Location location, Location location2, double d) {
        Vector vector = location2.toVector().subtract(location.toVector());
        double d2 = vector.length();
        if (d2 <= d || d2 == 0.0) {
            return location2.clone();
        }
        Vector vector2 = vector.normalize().multiply(d);
        return location.clone().add(vector2);
    }

    private void fireStartEvent(Player player, NavigationMode navigationMode, String string, TargetLocator targetLocator, RouteDescriptor routeDescriptor) {
        Bukkit.getPluginManager().callEvent((Event)new GpsNavigationStartEvent(player, navigationMode, string, targetLocator.id(), targetLocator.displayName(), routeDescriptor));
    }

    private void fireStopEvent(Player player, String string, NavigationSession navigationSession) {
        Bukkit.getPluginManager().callEvent((Event)new GpsNavigationStopEvent(player, navigationSession.mode(), navigationSession.source(), string, navigationSession.locator().id(), navigationSession.locator().displayName(), navigationSession.descriptor()));
    }

    private void fireReachEvent(Player player, NavigationSession navigationSession) {
        Bukkit.getPluginManager().callEvent((Event)new GpsNavigationReachEvent(player, navigationSession.mode(), navigationSession.source(), navigationSession.locator().id(), navigationSession.locator().displayName(), navigationSession.descriptor()));
    }

    private void fireModeChangeEvent(Player player, NavigationMode navigationMode, NavigationMode navigationMode2) {
        Bukkit.getPluginManager().callEvent((Event)new GpsNavigationModeChangeEvent(player, navigationMode, navigationMode2));
    }
}

