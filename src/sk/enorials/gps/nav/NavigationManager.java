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
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
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
import sk.enorials.gps.nav.target.StaticLocationTarget;
import sk.enorials.gps.nav.target.TargetLocator;
import sk.enorials.gps.util.Color;
import sk.enorials.hud.access.HudAccess;

public final class NavigationManager {
   private final EnorialsGpsPlugin plugin;
   private final IntegrationManager integrationManager;
   private final BetonQuestBridge betonQuestBridge;
   private final RoutePersistenceService routePersistence;
   private final Map<UUID, NavigationSession> sessions = new HashMap<>();
   private final Map<UUID, Location> previousCompassTargets = new HashMap<>();
   private final Map<UUID, NavigationMode> playerModes = new HashMap<>();
   private final Map<UUID, Boolean> playerShowDistance = new HashMap<>();
   private final Map<UUID, Boolean> playerShowVertical = new HashMap<>();
   private final Map<UUID, Boolean> playerShowTargetName = new HashMap<>();
   private final Map<UUID, Boolean> playerCompactHud = new HashMap<>();
   private final Map<UUID, Boolean> playerTargetMarker = new HashMap<>();
   private final Map<UUID, RoutePersistenceService.PersistedRoute> pendingResume = new HashMap<>();
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

   public List<Location> trackedQuestNpcLocations(Player var1) {
      NavigationSession var2 = this.sessions.get(var1.getUniqueId());
      if (var2 != null && var2.isTrackedQuest()) {
         String var3 = var2.source().substring("betonquest-quest:".length());
         ConfigurationSection var4 = this.plugin.config().getConfigurationSection("betonquest.auto_routes");
         if (var4 == null) {
            return List.of();
         }

         ArrayList var5 = new ArrayList();

         for (String var7 : var4.getKeys(false)) {
            ConfigurationSection var8 = var4.getConfigurationSection(var7);
            if (var8 != null && var3.equals(var8.getString("quest", "")) && "NPC".equalsIgnoreCase(var8.getString("type", ""))) {
               try {
                  this.integrationManager.resolveDescriptor(var1, RouteDescriptor.npc(var8.getString("value", ""))).ifPresent(var2x -> {
                     Location var3x = var2x.resolve(var1);
                     if (var3x != null && var3x.getWorld() != null) {
                        var5.add(var3x);
                     }
                  });
               } catch (Exception | LinkageError var10) {
               }
            }
         }

         return List.copyOf(var5);
      } else {
         return List.of();
      }
   }

   public NavigationManager(EnorialsGpsPlugin var1, IntegrationManager var2, BetonQuestBridge var3, RoutePersistenceService var4) {
      this.plugin = var1;
      this.integrationManager = var2;
      this.betonQuestBridge = var3;
      this.routePersistence = var4;
      this.pendingResume.putAll(var4.getAll());
      this.startTickTask();
   }

   private void startTickTask() {
      long var1 = this.plugin.config().getLong("navigation.update_interval_ticks", 20L);
      if (var1 < 1L) {
         var1 = 1L;
      }

      this.task = Bukkit.getScheduler().runTaskTimer(this.plugin.owner(), this::tick, var1, var1);
   }

   public NavigationMode getCurrentOrDefaultMode(Player var1) {
      return this.playerModes.getOrDefault(var1.getUniqueId(), this.plugin.getDefaultMode());
   }

   public NavigationSession session(Player var1) {
      return this.sessions.get(var1.getUniqueId());
   }

   public boolean hasActiveNavigation(Player var1) {
      return this.sessions.containsKey(var1.getUniqueId());
   }

   public boolean isDistanceDisplayEnabled(Player var1) {
      return this.playerShowDistance.getOrDefault(var1.getUniqueId(), this.plugin.config().getBoolean("navigation.display.show_distance_meters", true));
   }

   public boolean isVerticalDisplayEnabled(Player var1) {
      return this.playerShowVertical.getOrDefault(var1.getUniqueId(), this.plugin.config().getBoolean("navigation.display.show_vertical_indicator", true));
   }

   public boolean isTargetNameDisplayEnabled(Player var1) {
      return this.playerShowTargetName.getOrDefault(var1.getUniqueId(), this.plugin.config().getBoolean("navigation.display.show_target_name", true));
   }

   public boolean isCompactHudEnabled(Player var1) {
      return this.playerCompactHud.getOrDefault(var1.getUniqueId(), this.plugin.config().getBoolean("navigation.display.compact_hud", false));
   }

   public boolean isTargetMarkerEnabled(Player var1) {
      return !this.plugin.config().getBoolean("navigation.target_marker.enabled", true)
         ? false
         : this.playerTargetMarker.getOrDefault(var1.getUniqueId(), this.plugin.config().getBoolean("navigation.target_marker.default_for_player", true));
   }

   public boolean toggleDistanceDisplay(Player var1) {
      boolean var2 = !this.isDistanceDisplayEnabled(var1);
      this.playerShowDistance.put(var1.getUniqueId(), var2);
      this.plugin.msg().send(var1, "&eDistance display: " + (var2 ? "&aON" : "&cOFF"));
      return var2;
   }

   public boolean toggleVerticalDisplay(Player var1) {
      boolean var2 = !this.isVerticalDisplayEnabled(var1);
      this.playerShowVertical.put(var1.getUniqueId(), var2);
      this.plugin.msg().send(var1, "&eVertical indicator: " + (var2 ? "&aON" : "&cOFF"));
      return var2;
   }

   public boolean toggleTargetNameDisplay(Player var1) {
      boolean var2 = !this.isTargetNameDisplayEnabled(var1);
      this.playerShowTargetName.put(var1.getUniqueId(), var2);
      this.plugin.msg().send(var1, "&eTarget name in HUD: " + (var2 ? "&aON" : "&cOFF"));
      return var2;
   }

   public boolean toggleCompactHud(Player var1) {
      boolean var2 = !this.isCompactHudEnabled(var1);
      this.playerCompactHud.put(var1.getUniqueId(), var2);
      this.plugin.msg().send(var1, "&eCompact HUD: " + (var2 ? "&aON" : "&cOFF"));
      return var2;
   }

   public boolean toggleTargetMarker(Player var1) {
      if (!this.plugin.config().getBoolean("navigation.target_marker.enabled", true)) {
         this.plugin.msg().send(var1, "&cTarget beacon is disabled by server settings.");
         return false;
      } else {
         boolean var2 = !this.isTargetMarkerEnabled(var1);
         this.playerTargetMarker.put(var1.getUniqueId(), var2);
         this.plugin.msg().send(var1, "&eTarget beacon: " + (var2 ? "&aON" : "&cOFF"));
         return var2;
      }
   }

   public void setMode(Player var1, NavigationMode var2) {
      NavigationMode var3 = this.getCurrentOrDefaultMode(var1);
      NavigationMode var4 = NavigationMode.hudMode(var2);
      if (!this.plugin.isModeEnabled(var4)) {
         boolean var5 = this.plugin.config().getBoolean("modes.fallback_to_actionbar", true);
         if (!var5) {
            this.plugin.msg().send(var1, "&cThat navigation mode is disabled.");
            return;
         }

         var4 = NavigationMode.HUD;
      }

      GpsNavigationPreModeChangeEvent var8 = new GpsNavigationPreModeChangeEvent(var1, var3, var4);
      Bukkit.getPluginManager().callEvent(var8);
      if (!var8.isCancelled()) {
         var4 = var8.getNewMode();
         if (!this.plugin.isModeEnabled(var4)) {
            boolean var6 = this.plugin.config().getBoolean("modes.fallback_to_actionbar", true);
            if (!var6) {
               this.plugin.msg().send(var1, "&cThat navigation mode is disabled.");
               return;
            }

            var4 = NavigationMode.HUD;
         }

         this.playerModes.put(var1.getUniqueId(), var4);
         NavigationSession var9 = this.sessions.get(var1.getUniqueId());
         if (var9 != null) {
            var9.setMode(var4);
            this.clearBossBar(var1, var9);
            if (var4 != NavigationMode.COMPASS) {
               this.clearCompassTarget(var1);
            }

            if (var9.descriptor() != null && this.plugin.config().getBoolean("navigation.persist_routes", true)) {
               this.routePersistence.save(var1.getUniqueId(), var9.descriptor(), var4, var9.source());
            }
         }

         if (var4 != NavigationMode.COMPASS) {
            this.clearCompassTarget(var1);
         }

         this.plugin.msg().send(var1, "&aNavigation mode set to &f" + var4.display() + "&a.");
         this.fireModeChangeEvent(var1, var3, var4);
         this.betonQuestBridge.onModeChange(var1, var4.name().toLowerCase());
      }
   }

   public boolean startNavigation(Player var1, TargetLocator var2, NavigationMode var3, String var4) {
      return this.startNavigation(var1, var2, var3, var4, null);
   }

   public boolean startNavigation(Player var1, TargetLocator var2, NavigationMode var3, String var4, RouteDescriptor var5) {
      if (!HudAccess.navigation(var1, var2, var5, var4)) {
         HudAccess.check(var1, !HudAccess.has(var1, "enorials.gps.use") ? "enorials.gps.use" : HudAccess.navigationNode(var2, var5, var4));
         return false;
      }

      if ("hud-party".equals(var4) && !this.plugin.gui().samePartyTarget(var1, var2)) {
         return false;
      }

      NavigationMode var8 = NavigationMode.hudMode(var3);
      if (!this.plugin.isModeEnabled(var8)) {
         var8 = NavigationMode.HUD;
      }

      Location var7;
      if ((var7 = var2.resolve(var1)) == null) {
         this.plugin.msg().send(var1, "&cTarget unavailable.");
         return false;
      }

      if (!this.isTargetAllowed(var1, var7)) {
         this.plugin.msg().send(var1, "&cTarget is blocked by navigation restrictions.");
         return false;
      }

      GpsNavigationPreStartEvent var9 = new GpsNavigationPreStartEvent(var1, var8, var4, var2.id(), var2.displayName(), var5);
      Bukkit.getPluginManager().callEvent(var9);
      if (var9.isCancelled()) {
         return false;
      }

      var8 = var9.getMode();
      var4 = var9.getSource();
      if (!this.plugin.isModeEnabled(var8)) {
         boolean var10 = this.plugin.config().getBoolean("modes.fallback_to_actionbar", true);
         if (!var10) {
            this.plugin.msg().send(var1, "&cThat navigation mode is disabled.");
            return false;
         }

         var8 = NavigationMode.HUD;
      }

      NavigationSession var6;
      if ((var6 = this.sessions.remove(var1.getUniqueId())) != null) {
         this.clearBossBar(var1, var6);
         this.clearCompassTarget(var1);
      }

      NavigationSession var13 = new NavigationSession(var1.getUniqueId(), var2, var8, var4, var5);
      var13.setLastResolvedTarget(var7);
      var13.setLastResolveTick(0);
      this.sessions.put(var1.getUniqueId(), var13);
      this.playerModes.put(var1.getUniqueId(), var8);
      if (var8 != NavigationMode.COMPASS) {
         this.clearCompassTarget(var1);
      }

      if (var5 != null && this.plugin.config().getBoolean("navigation.persist_routes", true)) {
         this.routePersistence.save(var1.getUniqueId(), var5, var8, var4);
         this.pendingResume.put(var1.getUniqueId(), new RoutePersistenceService.PersistedRoute(var5, var8, var4));
      }

      if (this.plugin.owner().betonQuest() != null && this.plugin.owner().betonQuest().questLog() != null) {
         this.plugin.owner().betonQuest().questLog().navigationStarted(var1, var4);
      }

      this.fireStartEvent(var1, var8, var4, var2, var5);
      this.plugin.msg().send(var1, "&aGPS started: &f" + var2.displayName());
      this.playStartFeedback(var1);
      this.betonQuestBridge.onStart(var1, var4, var2.id());
      return true;
   }

   public boolean stopNavigation(Player var1, String var2) {
      if (!Set.of("quest-stage", "quest-selection").contains(var2)
         && this.plugin.owner().betonQuest() != null
         && this.plugin.owner().betonQuest().questLog() != null) {
         this.plugin.owner().betonQuest().questLog().stopTracking(var1);
      }

      NavigationSession var3 = this.sessions.remove(var1.getUniqueId());
      if (var3 == null) {
         this.plugin.msg().send(var1, "&7No active GPS route.");
         return false;
      } else {
         this.clearBossBar(var1, var3);
         this.clearCompassTarget(var1);
         this.fireStopEvent(var1, var2, var3);
         this.plugin.msg().send(var1, "&cGPS stopped.");
         this.clearPersisted(var1.getUniqueId());
         this.betonQuestBridge.onStop(var1, var2, var3.locator().id());
         return true;
      }
   }

   public void tryResume(Player var1) {
      if (this.plugin.config().getBoolean("navigation.resume_on_join", true)) {
         if (!this.sessions.containsKey(var1.getUniqueId())) {
            RoutePersistenceService.PersistedRoute var2 = this.pendingResume.get(var1.getUniqueId());
            if (var2 != null) {
               Optional var3 = this.integrationManager.resolveDescriptor(var1, var2.descriptor());
               if (var3.isEmpty()) {
                  this.plugin.msg().send(var1, "&7Stored GPS route is not available right now.");
               } else {
                  this.startNavigation(var1, (TargetLocator)var3.get(), var2.mode(), var2.source(), var2.descriptor());
               }
            }
         }
      }
   }

   public void shutdown() {
      if (this.task != null) {
         this.task.cancel();
         this.task = null;
      }

      this.persistActiveRoutesOnShutdown();

      for (Entry var2 : this.sessions.entrySet()) {
         Player var3 = Bukkit.getPlayer((UUID)var2.getKey());
         if (var3 != null) {
            this.clearBossBar(var3, (NavigationSession)var2.getValue());
            this.clearCompassTarget(var3);
         }
      }

      this.sessions.clear();
      this.playerShowDistance.clear();
      this.playerShowVertical.clear();
      this.playerShowTargetName.clear();
      this.playerCompactHud.clear();
      this.playerTargetMarker.clear();
   }

   public String getProfilingSummary() {
      boolean var1 = this.plugin.config().getBoolean("navigation.profiling.enabled", false);
      if (this.profilingRuns <= 0L) {
         return "GPS profiling: enabled=" + var1 + ", no samples yet.";
      }

      double var2 = this.profilingTotalMs / this.profilingRuns;
      double var4 = (double)this.profilingSessionSamples / this.profilingRuns;
      return String.format(
         Locale.US,
         "GPS profiling: enabled=%s runs=%d last=%.3fms avg=%.3fms max=%.3fms avgSessions=%.2f active=%d spikes=%d",
         var1,
         this.profilingRuns,
         this.profilingLastMs,
         var2,
         this.profilingMaxMs,
         var4,
         this.sessions.size(),
         this.profilingSpikeCount
      );
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
      if (this.plugin.config().getBoolean("navigation.persist_routes", true)) {
         for (Entry var2 : this.sessions.entrySet()) {
            UUID var4 = (UUID)var2.getKey();
            NavigationSession var3;
            RouteDescriptor var5 = this.resolveDescriptorForPersistence(var4, var3 = (NavigationSession)var2.getValue());
            if (var5 != null) {
               this.routePersistence.save(var4, var5, var3.mode(), var3.source());
               this.pendingResume.put(var4, new RoutePersistenceService.PersistedRoute(var5, var3.mode(), var3.source()));
            }
         }
      }
   }

   private RouteDescriptor resolveDescriptorForPersistence(UUID var1, NavigationSession var2) {
      if (var2.descriptor() != null) {
         return var2.descriptor();
      } else {
         Player var3 = Bukkit.getPlayer(var1);
         if (var3 != null && var3.isOnline()) {
            Location var4 = var2.locator().resolve(var3);
            return var4 != null && var4.getWorld() != null ? RouteDescriptor.location(var4) : null;
         } else {
            return null;
         }
      }
   }

   private void tick() {
      long var1 = Math.max(1L, this.plugin.config().getLong("navigation.update_interval_ticks", 20L));
      int var3 = Math.max(1, this.plugin.config().getInt("navigation.target_resolve_interval_ticks", (int)var1));
      int var4 = Math.max(1, (int)Math.ceil((double)var3 / var1));
      double var5 = this.plugin.config().getDouble("navigation.arrive_distance", 4.0);
      boolean var7 = this.plugin.config().getBoolean("navigation.profiling.enabled", false);
      long var8 = var7 ? System.nanoTime() : 0L;

      try {
         Iterator var10 = this.sessions.entrySet().iterator();

         while (var10.hasNext()) {
            Entry var11 = (Entry)var10.next();
            Player var12 = Bukkit.getPlayer((UUID)var11.getKey());
            if (var12 != null && var12.isOnline()) {
               NavigationSession var13 = (NavigationSession)var11.getValue();
               if (!HudAccess.navigation(var12, var13.locator(), var13.descriptor(), var13.source())) {
                  this.stopSessionAndCleanup(var12, var13, var10, "permission_revoked");
               } else {
                  var13.incrementTick();
                  if (var13.source().startsWith("betonquest-quest:")
                     && !var13.locator().id().startsWith("quest:")
                     && (var13.tickCount() == 1 || var13.tickCount() % Math.max(1, (int)Math.ceil(20.0 / var1)) == 0)) {
                     String var14 = var13.source().substring("betonquest-quest:".length());
                     IntegrationManager.AutoRoute var15 = this.integrationManager.getAutoRoutes(var12).get(var14);
                     if (var15 == null) {
                        this.stopSessionAndCleanup(var12, var13, var10, "quest_inactive");
                        continue;
                     }

                     if (!var15.descriptor().equals(var13.descriptor())
                        || !ChatColor.stripColor(Color.colorize(var15.displayName())).equals(var13.locator().displayName())) {
                        Optional var16 = this.integrationManager.resolveAutoRoute(var12, var15);
                        if (var16.isEmpty()) {
                           this.stopSessionAndCleanup(var12, var13, var10, "target_unavailable");
                           continue;
                        }

                        var13.updateQuestTarget((TargetLocator)var16.get(), var15.descriptor());
                        if (this.plugin.config().getBoolean("navigation.persist_routes", true)) {
                           this.routePersistence.save(var12.getUniqueId(), var15.descriptor(), var13.mode(), var13.source());
                        }
                     }
                  }

                  if (var13.source().equals("hud-party") && !this.plugin.gui().samePartyTarget(var12, var13.locator())) {
                     this.stopSessionAndCleanup(var12, var13, var10, "party_left");
                  } else if (!this.isWorldAllowed(var12.getWorld().getName())) {
                     this.plugin.msg().send(var12, "&cNavigation stopped in restricted world.");
                     this.stopSessionAndCleanup(var12, var13, var10, "restricted_world");
                  } else {
                     Location var23 = this.resolveTargetForTick(var12, var13, var4);
                     if (var23 == null) {
                        this.plugin.msg().send(var12, "&cTarget unavailable.");
                        this.stopSessionAndCleanup(var12, var13, var10, "target_unavailable");
                     } else if (!this.isTargetAllowed(var12, var23)) {
                        this.plugin.msg().send(var12, "&cNavigation stopped: target entered blocked area.");
                        this.stopSessionAndCleanup(var12, var13, var10, "target_blocked");
                     } else if (var12.getWorld().equals(var23.getWorld())) {
                        Location var24 = this.resolveGuidanceTarget(var12, var13, var23);
                        double var25 = var12.getLocation().distance(var23);
                        if (var25 <= var5 && var13.isTrackedQuest()) {
                           if (var13.markQuestArrival()) {
                              this.onReached(var12, var13);
                           }

                           this.render(var12, var13, var24, var25);
                        } else if (var25 <= var5) {
                           this.onReached(var12, var13);
                           this.clearBossBar(var12, var13);
                           this.clearCompassTarget(var12);
                           var10.remove();
                           this.clearPersisted(var12.getUniqueId());
                        } else {
                           this.render(var12, var13, var24, var25);
                           this.renderTargetMarker(var12, var23, var13, var25);
                           this.playPulseFeedback(var12, var24, var13);
                        }
                     }
                  }
               }
            } else {
               var10.remove();
            }
         }
      } finally {
         if (var7) {
            double var19 = (System.nanoTime() - var8) / 1000000.0;
            this.recordProfilingSample(var19, var1);
         }
      }
   }

   private void recordProfilingSample(double var1, long var3) {
      this.profilingRuns++;
      this.profilingLastMs = var1;
      this.profilingTotalMs += var1;
      this.profilingMaxMs = Math.max(this.profilingMaxMs, var1);
      this.profilingSessionSamples = this.profilingSessionSamples + this.sessions.size();
      this.profilingElapsedTicks += var3;
      double var6 = Math.max(0.1, this.plugin.config().getDouble("navigation.profiling.warn_above_ms", 10.0));
      if (var1 >= var6) {
         this.profilingSpikeCount++;
         if (this.plugin.config().getBoolean("navigation.profiling.log_spikes", false)) {
            long var8 = System.nanoTime();
            if (this.profilingLastSpikeLogNanos == 0L || var8 - this.profilingLastSpikeLogNanos >= 5000000000L) {
               this.profilingLastSpikeLogNanos = var8;
               this.plugin
                  .getLogger()
                  .warning(
                     String.format(Locale.US, "[GPS-Perf] Spike detected: %.3fms (warn threshold %.3fms, active sessions=%d)", var1, var6, this.sessions.size())
                  );
            }
         }
      }

      if (this.profilingElapsedTicks >= Math.max(20, this.plugin.config().getInt("navigation.profiling.log_interval_ticks", 1200))) {
         this.plugin.getLogger().info("[GPS-Perf] " + this.getProfilingSummary());
         if (this.plugin.config().getBoolean("navigation.profiling.reset_after_log", true)) {
            this.resetProfilingStats();
         } else {
            this.profilingElapsedTicks = 0L;
         }
      }
   }

   private Location resolveTargetForTick(Player var1, NavigationSession var2, int var3) {
      if (var2.locator() instanceof StaticLocationTarget) {
         Location var10 = var2.lastResolvedTarget();
         if (var10 != null) {
            return var10;
         }

         Location var11 = var2.locator().resolve(var1);
         var2.setLastResolvedTarget(var11);
         var2.setLastResolveTick(var2.tickCount());
         return var11;
      } else if (var3 <= 1) {
         Location var9 = var2.locator().resolve(var1);
         var2.setLastResolvedTarget(var9);
         var2.setLastResolveTick(var2.tickCount());
         return var9;
      } else {
         int var5 = var2.lastResolveTick();
         Location var6 = var2.lastResolvedTarget();
         boolean var4 = var6 == null || var5 < 0 || var2.tickCount() - var5 >= var3;
         if (var4) {
            Location var8 = var2.locator().resolve(var1);
            var2.setLastResolvedTarget(var8);
            var2.setLastResolveTick(var2.tickCount());
            return var8;
         } else {
            return var6;
         }
      }
   }

   private void stopSessionAndCleanup(Player var1, NavigationSession var2, Iterator<Entry<UUID, NavigationSession>> var3, String var4) {
      this.clearBossBar(var1, var2);
      this.clearCompassTarget(var1);
      var3.remove();
      this.fireStopEvent(var1, var4, var2);
      this.clearPersisted(var1.getUniqueId());
      this.betonQuestBridge.onStop(var1, var4, var2.locator().id());
   }

   private void render(Player var1, NavigationSession var2, Location var3, double var4) {
      if (var2.mode() == NavigationMode.COMPASS) {
         this.previousCompassTargets.putIfAbsent(var1.getUniqueId(), var1.getCompassTarget().clone());
         var1.setCompassTarget(var3);
      }
   }

   private void onReached(Player var1, NavigationSession var2) {
      String var3 = var2.locator().displayName();
      this.plugin.msg().send(var1, "&aReached: &f" + var3);
      this.plugin.msg().send(var1, "&aYou reached &f" + var3 + "&a.");
      if (this.plugin.config().getBoolean("effects.sounds", true)) {
         Sound var4 = this.parseSound(this.plugin.config().getString("effects.reach_sound", "UI_TOAST_CHALLENGE_COMPLETE"), Sound.UI_TOAST_CHALLENGE_COMPLETE);
         var1.playSound(var1.getLocation(), var4, 1.0F, 1.0F);
      }

      if (this.plugin.config().getBoolean("effects.particles", true)) {
         var1.getWorld()
            .spawnParticle(
               this.parseParticle(this.plugin.config().getString("effects.reach_particle", "TOTEM"), Particle.TOTEM_OF_UNDYING),
               var1.getLocation().add(0.0, 1.0, 0.0),
               40,
               0.6,
               0.6,
               0.6,
               0.02
            );
      }

      this.fireReachEvent(var1, var2);
      this.betonQuestBridge.onReach(var1, var2.locator().id());
   }

   private void playStartFeedback(Player var1) {
      if (this.plugin.config().getBoolean("effects.sounds", true)) {
         Sound var2 = this.parseSound(this.plugin.config().getString("effects.start_sound", "ENTITY_EXPERIENCE_ORB_PICKUP"), Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
         var1.playSound(var1.getLocation(), var2, 0.8F, 1.2F);
      }

      if (this.plugin.config().getBoolean("effects.particles", true)) {
         var1.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, var1.getLocation().add(0.0, 1.0, 0.0), 12, 0.35, 0.35, 0.35, 0.01);
      }
   }

   private void playPulseFeedback(Player var1, Location var2, NavigationSession var3) {
   }

   private void renderTargetMarker(Player var1, Location var2, NavigationSession var3, double var4) {
      if (this.isTargetMarkerEnabled(var1)) {
         if (var2 != null && var2.getWorld() != null && var1.getWorld().equals(var2.getWorld())) {
            double var6 = Math.max(0.0, this.plugin.config().getDouble("navigation.target_marker.min_distance_meters", 20.0));
            if (!(var4 < var6)) {
               int var8 = Math.max(1, this.plugin.config().getInt("navigation.target_marker.interval_updates", 2));
               if (var3.tickCount() % var8 == 0) {
                  double var9 = Math.max(3.0, this.plugin.config().getDouble("navigation.target_marker.beam_height", 42.0));
                  double var11 = Math.max(0.5, this.plugin.config().getDouble("navigation.target_marker.beam_step", 2.5));
                  int var13 = Math.max(1, this.plugin.config().getInt("navigation.target_marker.beam_count", 2));
                  double var14 = Math.max(0.0, this.plugin.config().getDouble("navigation.target_marker.beam_spread", 0.05));
                  int var16 = Math.max(1, this.plugin.config().getInt("navigation.target_marker.top_count", 16));
                  double var17 = Math.max(0.0, this.plugin.config().getDouble("navigation.target_marker.top_spread", 0.35));
                  Particle var19 = this.parseParticle(this.plugin.config().getString("navigation.target_marker.beam_particle", "END_ROD"), Particle.END_ROD);
                  Particle var20 = this.parseParticle(
                     this.plugin.config().getString("navigation.target_marker.top_particle", "TOTEM"), Particle.TOTEM_OF_UNDYING
                  );
                  double var21 = var2.getX();
                  double var23 = var2.getY();
                  double var25 = var2.getZ();

                  for (double var27 = 0.0; var27 <= var9; var27 += var11) {
                     var1.spawnParticle(var19, var21, var23 + var27, var25, var13, var14, 0.02, var14, 0.0);
                  }

                  var1.spawnParticle(var20, var21, var23 + var9 + 0.5, var25, var16, var17, var17, var17, 0.01);
               }
            }
         }
      }
   }

   private void clearBossBar(Player var1, NavigationSession var2) {
      if (var2.bossBar() != null) {
         var1.hideBossBar(var2.bossBar());
         var2.setBossBar(null);
      }
   }

   private void clearCompassTarget(Player var1) {
      if (var1 != null && var1.getWorld() != null) {
         Location var2 = this.previousCompassTargets.remove(var1.getUniqueId());
         if (var2 != null) {
            var1.setCompassTarget(var2);
         }
      }
   }

   private String getDirectionArrow(Location var1, Location var2) {
      Vector var3 = var2.clone().subtract(var1).toVector();
      double var4 = Math.toDegrees(Math.atan2(-var3.getX(), var3.getZ()));
      double var6 = this.normalizeDegrees(var4 - var1.getYaw());
      String[] var8 = new String[]{"⬆⬆", "⬆↗", "↗↗", "↗➡", "➡➡", "➡↘", "↘↘", "↘⬇", "⬇⬇", "⬇↙", "↙↙", "↙⬅", "⬅⬅", "⬅↖", "↖↖", "↖⬆"};
      int var9 = (int)Math.floor((var6 + 11.25) / 22.5);
      var9 = (var9 % 16 + 16) % 16;
      return var8[var9];
   }

   private String getVerticalIndicator(Location var1, Location var2) {
      double var3 = var2.getY() - var1.getY();
      if (var3 > 1.5) {
         return "&a▲ +" + Math.round(var3) + "Y";
      } else {
         return var3 < -1.5 ? "&c▼ " + Math.round(var3) + "Y" : "&7■ 0Y";
      }
   }

   private double normalizeDegrees(double var1) {
      double var3 = var1 % 360.0;
      if (var3 >= 180.0) {
         var3 -= 360.0;
      }

      if (var3 < -180.0) {
         var3 += 360.0;
      }

      return var3;
   }

   private Sound parseSound(String var1, Sound var2) {
      try {
         return Sound.valueOf(var1.toUpperCase());
      } catch (Exception var4) {
         return var2;
      }
   }

   private Particle parseParticle(String var1, Particle var2) {
      try {
         return Particle.valueOf(var1.toUpperCase());
      } catch (Exception var4) {
         return var2;
      }
   }

   private boolean isTargetAllowed(Player var1, Location var2) {
      return this.isWorldAllowed(var1.getWorld().getName()) && this.isWorldAllowed(var2.getWorld().getName()) && !this.isInsideBlockedCuboid(var2);
   }

   private boolean isWorldAllowed(String var1) {
      if (!this.plugin.config().getBoolean("restrictions.enabled", false)) {
         return true;
      }

      List var2 = this.plugin.config().getStringList("restrictions.allowed_worlds");
      List var3 = this.plugin.config().getStringList("restrictions.denied_worlds");
      return !var2.isEmpty() && !var2.contains(var1) ? false : !var3.contains(var1);
   }

   private boolean isInsideBlockedCuboid(Location var1) {
      if (!this.plugin.config().getBoolean("restrictions.enabled", false)) {
         return false;
      }

      ConfigurationSection var2 = this.plugin.config().getConfigurationSection("restrictions.blocked_cuboids");
      if (var2 == null) {
         return false;
      }

      for (String var4 : var2.getKeys(false)) {
         ConfigurationSection var5 = var2.getConfigurationSection(var4);
         if (var5 != null) {
            String var6 = var5.getString("world", "");
            if (var1.getWorld().getName().equalsIgnoreCase(var6)) {
               int[] var7 = this.parseTriplet(var5.getString("min", "0,0,0"));
               int[] var8 = this.parseTriplet(var5.getString("max", "0,0,0"));
               int var9 = Math.min(var7[0], var8[0]);
               int var10 = Math.max(var7[0], var8[0]);
               int var11 = Math.min(var7[1], var8[1]);
               int var12 = Math.max(var7[1], var8[1]);
               int var13 = Math.min(var7[2], var8[2]);
               int var14 = Math.max(var7[2], var8[2]);
               int var15 = var1.getBlockX();
               int var16 = var1.getBlockY();
               int var17 = var1.getBlockZ();
               if (var15 >= var9 && var15 <= var10 && var16 >= var11 && var16 <= var12 && var17 >= var13 && var17 <= var14) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private int[] parseTriplet(String var1) {
      String[] var2 = var1.split(",");
      ArrayList var3 = new ArrayList();

      for (String var7 : var2) {
         try {
            var3.add(Integer.parseInt(var7.trim()));
         } catch (NumberFormatException var9) {
         }
      }

      while (var3.size() < 3) {
         var3.add(0);
      }

      return new int[]{(Integer)var3.get(0), (Integer)var3.get(1), (Integer)var3.get(2)};
   }

   private void clearPersisted(UUID var1) {
      this.pendingResume.remove(var1);
      if (this.plugin.config().getBoolean("navigation.persist_routes", true)) {
         this.routePersistence.clear(var1);
      }
   }

   private Location resolveGuidanceTarget(Player var1, NavigationSession var2, Location var3) {
      if (!this.plugin.config().getBoolean("navigation.long_route_waypoints.enabled", true)) {
         var2.setWaypoint(null);
         return var3;
      }

      if (!var1.getWorld().equals(var3.getWorld())) {
         var2.setWaypoint(null);
         return var3;
      }

      double var5 = this.plugin.config().getDouble("navigation.long_route_waypoints.long_distance_threshold", 250.0);
      double var7 = this.plugin.config().getDouble("navigation.long_route_waypoints.waypoint_step_distance", 120.0);
      int var9 = this.plugin.config().getInt("navigation.long_route_waypoints.recalc_interval_ticks", 40);
      double var10 = var1.getLocation().distance(var3);
      if (!(var10 < var5) && !(var7 <= 0.0)) {
         Location var12 = var2.waypoint();
         boolean var4 = var12 == null
            || !var12.getWorld().equals(var1.getWorld())
            || var1.getLocation().distance(var12) <= Math.max(2.0, this.plugin.config().getDouble("navigation.arrive_distance", 4.0))
            || (var2.tickCount() - var2.lastWaypointRecalcTick()) * this.plugin.config().getInt("navigation.update_interval_ticks", 20) >= var9;
         if (var4) {
            Location var14 = this.computeWaypoint(var1.getLocation(), var3, var7);
            var2.setWaypoint(var14);
            var2.setLastWaypointRecalcTick(var2.tickCount());
            return var14;
         } else {
            return var12;
         }
      } else {
         var2.setWaypoint(null);
         return var3;
      }
   }

   private Location computeWaypoint(Location var1, Location var2, double var3) {
      Vector var5 = var2.toVector().subtract(var1.toVector());
      double var6 = var5.length();
      if (!(var6 <= var3) && var6 != 0.0) {
         Vector var8 = var5.normalize().multiply(var3);
         return var1.clone().add(var8);
      } else {
         return var2.clone();
      }
   }

   private void fireStartEvent(Player var1, NavigationMode var2, String var3, TargetLocator var4, RouteDescriptor var5) {
      Bukkit.getPluginManager().callEvent(new GpsNavigationStartEvent(var1, var2, var3, var4.id(), var4.displayName(), var5));
   }

   private void fireStopEvent(Player var1, String var2, NavigationSession var3) {
      Bukkit.getPluginManager()
         .callEvent(new GpsNavigationStopEvent(var1, var3.mode(), var3.source(), var2, var3.locator().id(), var3.locator().displayName(), var3.descriptor()));
   }

   private void fireReachEvent(Player var1, NavigationSession var2) {
      Bukkit.getPluginManager()
         .callEvent(new GpsNavigationReachEvent(var1, var2.mode(), var2.source(), var2.locator().id(), var2.locator().displayName(), var2.descriptor()));
   }

   private void fireModeChangeEvent(Player var1, NavigationMode var2, NavigationMode var3) {
      Bukkit.getPluginManager().callEvent(new GpsNavigationModeChangeEvent(var1, var2, var3));
   }
}
