package sk.enorials.gps.integration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.data.QuestRouteService;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.gps.nav.target.CitizensNpcTarget;
import sk.enorials.gps.nav.target.FancyNpcTarget;
import sk.enorials.gps.nav.target.HintedEntityTarget;
import sk.enorials.gps.nav.target.MythicMobTarget;
import sk.enorials.gps.nav.target.NamedEntityTarget;
import sk.enorials.gps.nav.target.NearestEntityTypeTarget;
import sk.enorials.gps.nav.target.PlayerTarget;
import sk.enorials.gps.nav.target.ShopkeeperTarget;
import sk.enorials.gps.nav.target.StaticLocationTarget;
import sk.enorials.gps.nav.target.TargetLocator;
import sk.enorials.gps.nav.target.WorldSpawnTarget;
import sk.enorials.gps.nav.target.ZnpcsPlusNpcTarget;
import sk.enorials.gps.util.Color;
import sk.enorials.gps.util.ConditionEvaluator;

public final class IntegrationManager {
   private final Map<UUID, Map<String, IntegrationManager.AutoRoute>> externalRoutes = new HashMap<>();
   private final EnorialsGpsPlugin plugin;
   private final ConditionEvaluator conditionEvaluator;
   private final QuestRouteService questRouteService;
   private Method itemsAdderByPlacedMethod;
   private Method itemsAdderIdMethod;
   private boolean itemsAdderApiChecked;
   private Method nexoCustomBlockMethod;
   private Method nexoMechanicIdMethod;
   private boolean nexoApiChecked;
   private Class<?> oraxenBlocksClass;
   private Method oraxenIsBlockMethod;
   private Method oraxenDirectIdMethod;
   private Method oraxenMechanicMethod;
   private Method oraxenMechanicIdMethod;
   private boolean oraxenApiChecked;
   private final QuestRouteNames questRouteNames = new QuestRouteNames();

   public void putExternalRoute(UUID var1, String var2, String var3, RouteDescriptor var4, NavigationMode var5, String var6) {
      this.externalRoutes.computeIfAbsent(var1, var0 -> new LinkedHashMap<>()).put(var2, new IntegrationManager.AutoRoute(var2, var3, var4, var5, var6));
   }

   public boolean removeExternalRoute(UUID var1, String var2) {
      Map var3 = this.externalRoutes.get(var1);
      if (var3 == null) {
         return false;
      }

      boolean var4 = var3.remove(var2) != null;
      if (var3.isEmpty()) {
         this.externalRoutes.remove(var1);
      }

      return var4;
   }

   public IntegrationManager(EnorialsGpsPlugin var1, QuestRouteService var2) {
      this.plugin = var1;
      this.conditionEvaluator = new ConditionEvaluator(var1);
      this.questRouteService = var2;
   }

   public boolean isPlayerTargetsEnabled() {
      return this.plugin.config().getBoolean("integrations.allow_player_targets", true);
   }

   public boolean isLocationTargetsEnabled() {
      return this.plugin.config().getBoolean("integrations.allow_location_targets", true);
   }

   public Optional<TargetLocator> resolveLocationInput(Player var1, String var2) {
      if (!this.isLocationTargetsEnabled()) {
         return Optional.empty();
      }

      if (var1 != null && var2 != null && !var2.isBlank()) {
         String var3 = var2.trim();
         String var4 = var3.toLowerCase(Locale.ROOT);
         if (var4.startsWith("warp:")) {
            return this.resolveWarpAlias(var3.substring("warp:".length()).trim());
         } else if (var4.startsWith("esswarp:")) {
            return this.resolveWarpAlias(var3.substring("esswarp:".length()).trim());
         } else if (var4.startsWith("ess:")) {
            return this.resolveWarpAlias(var3.substring("ess:".length()).trim());
         } else if (var4.startsWith("essentials:")) {
            return this.resolveWarpAlias(var3.substring("essentials:".length()).trim());
         } else if (var4.startsWith("cmiwarp:")) {
            return this.resolveCmiWarpTarget(var3.substring("cmiwarp:".length()).trim());
         } else if (var4.startsWith("cmi:")) {
            return this.resolveCmiWarpTarget(var3.substring("cmi:".length()).trim());
         } else if (var4.startsWith("wg:")) {
            return this.resolveWorldGuardRegionTarget(var1, var3.substring("wg:".length()).trim());
         } else if (var4.startsWith("worldguard:")) {
            return this.resolveWorldGuardRegionTarget(var1, var3.substring("worldguard:".length()).trim());
         } else if (var4.startsWith("region:")) {
            return this.resolveWorldGuardRegionTarget(var1, var3.substring("region:".length()).trim());
         } else if (var4.startsWith("towny:")) {
            return this.resolveTownyTownTarget(var3.substring("towny:".length()).trim());
         } else if (var4.startsWith("town:")) {
            return this.resolveTownyTownTarget(var3.substring("town:".length()).trim());
         } else if (var4.startsWith("dh:")) {
            return this.resolveDecentHologramTarget(var3.substring("dh:".length()).trim());
         } else if (var4.startsWith("decentholograms:")) {
            return this.resolveDecentHologramTarget(var3.substring("decentholograms:".length()).trim());
         } else {
            return var4.startsWith("holo:") ? this.resolveDecentHologramTarget(var3.substring("holo:".length()).trim()) : Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   public List<String> getWarpNames() {
      ArrayList var1 = new ArrayList();

      for (String var3 : this.getEssentialsWarpNames()) {
         if (!this.containsIgnoreCase(var1, var3)) {
            var1.add(var3);
         }
      }

      for (String var5 : this.getCmiWarpNames()) {
         if (!this.containsIgnoreCase(var1, var5)) {
            var1.add(var5);
         }
      }

      return var1;
   }

   public List<String> getEssentialsWarpNames() {
      if (!this.plugin.config().getBoolean("integrations.allow_warp_targets", true)) {
         return Collections.emptyList();
      }

      if (!this.isPluginEnabled("Essentials")) {
         return Collections.emptyList();
      }

      try {
         Plugin var1 = Bukkit.getPluginManager().getPlugin("Essentials");
         if (var1 == null) {
            return Collections.emptyList();
         }

         Method var2 = var1.getClass().getMethod("getWarps");
         Object var3 = var2.invoke(var1);
         if (var3 == null) {
            return Collections.emptyList();
         }

         Method var4 = this.findFirstMethod(var3.getClass(), "getList", "getWarpNames", "list");
         if (var4 == null) {
            return Collections.emptyList();
         }

         Object var5 = var4.invoke(var3);
         ArrayList var6 = new ArrayList();
         if (var5 instanceof Collection) {
            for (Object var9 : (Collection)var5) {
               if (var9 != null) {
                  var6.add(String.valueOf(var9));
               }
            }
         }

         return var6;
      } catch (Exception var10) {
         return Collections.emptyList();
      }
   }

   public List<String> getCmiWarpNames() {
      if (!this.plugin.config().getBoolean("integrations.allow_warp_targets", true)) {
         return Collections.emptyList();
      }

      if (!this.plugin.config().getBoolean("integrations.allow_cmi_warp_targets", true)) {
         return Collections.emptyList();
      }

      if (!this.isPluginEnabled("CMI")) {
         return Collections.emptyList();
      }

      Object var1 = this.getCmiWarpManager();
      if (var1 == null) {
         return Collections.emptyList();
      }

      ArrayList var2 = new ArrayList();
      this.collectNamesFromValue(this.invokeNoArg(var1, "getWarps", "getWarpMap", "getWarpsMap", "getAllWarps"), var2);
      this.collectNamesFromValue(this.invokeNoArg(var1, "getWarpNames", "getNames", "list"), var2);
      return var2;
   }

   public List<String> getTownyTownNames() {
      if (!this.plugin.config().getBoolean("integrations.allow_town_targets", true)) {
         return Collections.emptyList();
      }

      if (!this.isPluginEnabled("Towny")) {
         return Collections.emptyList();
      }

      try {
         Class var1 = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
         Object var2 = var1.getMethod("getInstance").invoke(null);
         if (var2 == null) {
            return Collections.emptyList();
         }

         ArrayList var3 = new ArrayList();
         this.collectNamesFromValue(this.invokeNoArg(var2, "getTowns"), var3);
         this.collectNamesFromValue(this.invokeNoArg(var2, "getTownsMap"), var3);
         return var3;
      } catch (Exception var4) {
         return Collections.emptyList();
      }
   }

   public List<String> getDecentHologramNames() {
      if (!this.plugin.config().getBoolean("integrations.allow_hologram_targets", true)) {
         return Collections.emptyList();
      }

      if (!this.isPluginEnabled("DecentHolograms")) {
         return Collections.emptyList();
      }

      ArrayList var1 = new ArrayList();

      try {
         Class var2 = Class.forName("eu.decentsoftware.holograms.api.holograms.Hologram");
         Method var3 = this.findFirstMethod(var2, "getCachedHologramNames");
         if (var3 != null) {
            this.collectNamesFromValue(var3.invoke(null), var1);
         }
      } catch (Exception var5) {
      }

      try {
         Class var6 = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
         Method var7 = this.findFirstMethod(var6, "getHologramNames");
         if (var7 != null) {
            this.collectNamesFromValue(var7.invoke(null), var1);
         }
      } catch (Exception var4) {
      }

      return var1;
   }

   public List<String> getWorldGuardRegionNames(Player var1) {
      if (!this.plugin.config().getBoolean("integrations.allow_region_targets", true)) {
         return Collections.emptyList();
      } else {
         return var1 != null && this.isPluginEnabled("WorldGuard") ? this.fetchWorldGuardRegionNames(var1.getWorld()) : Collections.emptyList();
      }
   }

   public Optional<TargetLocator> resolveMobInput(String var1) {
      if (!this.plugin.config().getBoolean("integrations.allow_mob_targets", true)) {
         return Optional.empty();
      }

      if (var1 != null && !var1.isBlank()) {
         String var2 = var1.toLowerCase(Locale.ROOT);
         if (var2.startsWith("elite:")) {
            return this.resolvePresetHintMob("elitemobs", var1.substring("elite:".length()));
         }

         if (var2.startsWith("elitemobs:")) {
            return this.resolvePresetHintMob("elitemobs", var1.substring("elitemobs:".length()));
         }

         if (var2.startsWith("modelengine:")) {
            return this.resolvePresetHintMob("modelengine", var1.substring("modelengine:".length()));
         }

         if (var2.startsWith("model:")) {
            return this.resolvePresetHintMob("modelengine", var1.substring("model:".length()));
         }

         if (var2.startsWith("denizen:")) {
            return this.resolvePresetHintMob("denizen", var1.substring("denizen:".length()));
         }

         if (var2.startsWith("mmocore:")) {
            return this.resolvePresetHintMob("mmocore", var1.substring("mmocore:".length()));
         }

         if (var2.startsWith("mmoitems:")) {
            return this.resolvePresetHintMob("mmoitems", var1.substring("mmoitems:".length()));
         }

         if (var2.startsWith("ecoboss:")) {
            return this.resolvePresetHintMob("ecobosses", var1.substring("ecoboss:".length()));
         }

         if (var2.startsWith("ecobosses:")) {
            return this.resolvePresetHintMob("ecobosses", var1.substring("ecobosses:".length()));
         }

         if (var2.startsWith("tag:")) {
            String var8 = var1.substring("tag:".length()).trim();
            return Optional.of(
               new HintedEntityTarget(
                  "tag:" + var8, "Tag " + var8, "*", 1000.0, Collections.emptyList(), List.of(var8), Collections.emptyList(), this.plugin.owner()
               )
            );
         }

         if (var2.startsWith("meta:")) {
            String var7 = var1.substring("meta:".length()).trim();
            return Optional.of(
               new HintedEntityTarget(
                  "meta:" + var7, "Metadata " + var7, "*", 1000.0, List.of(var7), Collections.emptyList(), Collections.emptyList(), this.plugin.owner()
               )
            );
         }

         if (var2.startsWith("pdc:")) {
            String var6 = var1.substring("pdc:".length()).trim();
            return Optional.of(
               new HintedEntityTarget(
                  "pdc:" + var6, "PDC " + var6, "*", 1000.0, Collections.emptyList(), Collections.emptyList(), List.of(var6), this.plugin.owner()
               )
            );
         }

         if (var2.startsWith("mythic:")) {
            String var5 = var1.substring("mythic:".length());
            return !this.isPluginEnabled("MythicMobs") ? Optional.empty() : Optional.of(new MythicMobTarget(var5, 512.0));
         }

         try {
            EntityType var3 = EntityType.valueOf(var1.toUpperCase());
            return Optional.of(new NearestEntityTypeTarget(var3, 512.0));
         } catch (IllegalArgumentException var4) {
            return Optional.of(new NamedEntityTarget(var1, 512.0));
         }
      } else {
         return Optional.empty();
      }
   }

   public Optional<TargetLocator> resolveNpcInput(String var1) {
      if (!this.plugin.config().getBoolean("integrations.allow_npc_targets", true)) {
         return Optional.empty();
      }

      if (var1 != null && !var1.isBlank()) {
         String var2 = var1.toLowerCase(Locale.ROOT);
         if (var2.startsWith("citizens:")) {
            String var10 = var1.substring("citizens:".length()).trim();
            return !this.isPluginEnabled("Citizens") ? Optional.empty() : Optional.of(new CitizensNpcTarget(var10, "NPC " + var10));
         } else if (var2.startsWith("denizen:")) {
            String var9 = var1.substring("denizen:".length()).trim();
            return this.isPluginEnabled("Denizen") && this.isPluginEnabled("Citizens")
               ? Optional.of(new CitizensNpcTarget(var9, "Denizen NPC " + var9))
               : Optional.empty();
         } else if (var2.startsWith("znpcs:")) {
            String var8 = var1.substring("znpcs:".length()).trim();
            return !this.isPluginEnabled("ZNPCsPlus") ? Optional.empty() : Optional.of(new ZnpcsPlusNpcTarget(var8, "ZNPC " + var8));
         } else if (var2.startsWith("znpcsplus:")) {
            String var7 = var1.substring("znpcsplus:".length()).trim();
            return !this.isPluginEnabled("ZNPCsPlus") ? Optional.empty() : Optional.of(new ZnpcsPlusNpcTarget(var7, "ZNPC " + var7));
         } else if (var2.startsWith("fancynpc:")) {
            String var6 = var1.substring("fancynpc:".length()).trim();
            return !this.isPluginEnabled("FancyNpcs") ? Optional.empty() : Optional.of(new FancyNpcTarget(var6, "FancyNPC " + var6));
         } else if (var2.startsWith("fancy:")) {
            String var5 = var1.substring("fancy:".length()).trim();
            return !this.isPluginEnabled("FancyNpcs") ? Optional.empty() : Optional.of(new FancyNpcTarget(var5, "FancyNPC " + var5));
         } else if (var2.startsWith("shopkeeper:")) {
            String var4 = var1.substring("shopkeeper:".length()).trim();
            return !this.isPluginEnabled("Shopkeepers") ? Optional.empty() : Optional.of(new ShopkeeperTarget(var4, "Shopkeeper " + var4));
         } else if (var2.startsWith("shopkeepers:")) {
            String var3 = var1.substring("shopkeepers:".length()).trim();
            return !this.isPluginEnabled("Shopkeepers") ? Optional.empty() : Optional.of(new ShopkeeperTarget(var3, "Shopkeeper " + var3));
         } else {
            return !this.isPluginEnabled("Citizens") ? Optional.empty() : Optional.of(new CitizensNpcTarget(var1, "NPC " + var1));
         }
      } else {
         return Optional.empty();
      }
   }

   public Optional<TargetLocator> resolveBlockInput(Player var1, String var2) {
      if (!this.plugin.config().getBoolean("integrations.allow_block_targets", true)) {
         return Optional.empty();
      }

      if (var1 != null && var2 != null && !var2.isBlank()) {
         IntegrationManager.BlockQuery var4 = this.parseBlockQuery(var2);
         if (var4 == null) {
            return Optional.empty();
         }

         double var5 = Math.max(8.0, this.plugin.config().getDouble("integrations.block_search.radius", 64.0));
         Location var7 = this.findNearestBlock(var1, var4, var5, this.plugin.config().getInt("integrations.block_search.vertical_range", 48));
         if (var7 == null) {
            return Optional.empty();
         }

         String var8 = this.buildBlockDisplayName(var4, var7);
         return Optional.of(new StaticLocationTarget(var8, var7));
      } else {
         return Optional.empty();
      }
   }

   public Optional<TargetLocator> resolvePoi(Player var1, String var2) {
      if (!this.plugin.config().getBoolean("integrations.allow_poi_targets", true)) {
         return Optional.empty();
      }

      ConfigurationSection var3 = this.plugin.config().getConfigurationSection("poi.points");
      if (var3 != null && var3.isConfigurationSection(var2)) {
         ConfigurationSection var4 = var3.getConfigurationSection(var2);
         if (var4 == null) {
            return Optional.empty();
         }

         if (!this.conditionEvaluator.checkAll(var1, var4.getStringList("conditions"))) {
            return Optional.empty();
         }

         TargetLocator var5 = this.buildPoiTarget(var1, var2, var4);
         return Optional.ofNullable(var5);
      } else {
         return Optional.empty();
      }
   }

   public Map<String, TargetLocator> getVisiblePois(Player var1) {
      ConfigurationSection var2 = this.plugin.config().getConfigurationSection("poi.points");
      if (var2 == null) {
         return Collections.emptyMap();
      }

      LinkedHashMap var3 = new LinkedHashMap();

      for (String var5 : var2.getKeys(false)) {
         this.resolvePoi(var1, var5).ifPresent(var2x -> var3.put(var5, var2x));
      }

      return var3;
   }

   public Map<String, String> getVisiblePoiDisplayNames(Player var1) {
      if (!this.plugin.config().getBoolean("integrations.allow_poi_targets", true)) {
         return Collections.emptyMap();
      }

      ConfigurationSection var2 = this.plugin.config().getConfigurationSection("poi.points");
      if (var2 == null) {
         return Collections.emptyMap();
      }

      LinkedHashMap var3 = new LinkedHashMap();

      for (String var5 : var2.getKeys(false)) {
         ConfigurationSection var6 = var2.getConfigurationSection(var5);
         if (var6 != null && this.conditionEvaluator.checkAll(var1, var6.getStringList("conditions"))) {
            var3.put(var5, var6.getString("display_name", var5));
         }
      }

      return var3;
   }

   public Set<String> getAvailablePoiKeys() {
      ConfigurationSection var1 = this.plugin.config().getConfigurationSection("poi.points");
      return var1 == null ? Collections.emptySet() : var1.getKeys(false);
   }

   public String getPoiDisplayName(String var1) {
      if (var1 != null && !var1.isBlank()) {
         ConfigurationSection var2 = this.plugin.config().getConfigurationSection("poi.points");
         if (var2 != null && var2.isConfigurationSection(var1)) {
            ConfigurationSection var3 = var2.getConfigurationSection(var1);
            return var3 == null ? var1 : var3.getString("display_name", var1);
         } else {
            return var1;
         }
      } else {
         return "";
      }
   }

   public Optional<TargetLocator> resolveDescriptor(Player var1, RouteDescriptor var2) {
      switch (var2.type()) {
         case PLAYER:
            if (!this.isPlayerTargetsEnabled()) {
               return Optional.empty();
            } else {
               String var4 = var2.value();

               try {
                  UUID var5 = UUID.fromString(var4);
                  Player var9 = Bukkit.getPlayer(var5);
                  return var9 == null ? Optional.empty() : Optional.of(new PlayerTarget(var9.getUniqueId(), var9.getName()));
               } catch (IllegalArgumentException var8) {
                  Player var6 = Bukkit.getPlayerExact(var4);
                  return var6 == null ? Optional.empty() : Optional.of(new PlayerTarget(var6.getUniqueId(), var6.getName()));
               }
            }
         case MOB:
            return this.resolveMobInput(var2.value());
         case NPC:
            return this.resolveNpcInput(var2.value());
         case POI:
            return this.resolvePoi(var1, var2.value());
         case LOCATION:
            if (!this.isLocationTargetsEnabled()) {
               return Optional.empty();
            }

            return this.resolveLocationDescriptor(var1, var2.value());
         default:
            throw new IncompatibleClassChangeError();
      }
   }

   public boolean betonQuestEnabled() {
      return this.isPluginEnabled("BetonQuest") && this.plugin.config().getBoolean("betonquest.enabled", true);
   }

   public Map<String, IntegrationManager.AutoRoute> getAutoRoutes(Player var1) {
      LinkedHashMap<String, IntegrationManager.AutoRoute> var2 = new LinkedHashMap<>();

      for (IntegrationManager.AutoRoute var4 : this.externalRoutes.getOrDefault(var1.getUniqueId(), Map.of()).values()) {
         if ((!var4.source().startsWith("betonquest") || this.betonQuestEnabled()) && this.resolveDescriptor(var1, var4.descriptor()).isPresent()) {
            var2.put(var4.key(), var4);
         }
      }

      Map<String, QuestRouteService.StoredRoute> var17 = this.questRouteService.getRoutes(var1.getUniqueId());

      for (Entry<String, QuestRouteService.StoredRoute> var5 : var17.entrySet()) {
         QuestRouteService.StoredRoute var6 = var5.getValue();
         if ((this.betonQuestEnabled() || !var6.source().startsWith("betonquest")) && !this.resolveDescriptor(var1, var6.descriptor()).isEmpty()) {
            var2.put(
               var5.getKey(),
               new IntegrationManager.AutoRoute(var5.getKey(), var6.displayName(), var6.descriptor(), var6.mode(), var6.source())
            );
         }
      }

      if (!this.betonQuestEnabled()) {
         return var2;
      }

      ConfigurationSection var19 = this.plugin.config().getConfigurationSection("betonquest.auto_routes");
      if (var19 == null) {
         return var2;
      }

      for (String var21 : var19.getKeys(false)) {
         ConfigurationSection var7 = var19.getConfigurationSection(var21);
         if (var7 != null && this.conditionEvaluator.checkAll(var1, var7.getStringList("conditions"))) {
            String var8 = var7.getString("type", "").toUpperCase();
            String var9 = var7.getString("value", "");
            String var10 = this.questRouteNames.name(var1, var7, var7.getString("display", var21));
            NavigationMode var11 = NavigationMode.fromConfig(var7.getString("mode", this.plugin.getDefaultMode().name().toLowerCase()));
            String var12 = var7.getString("source", "betonquest-auto");

            try {
               RouteDescriptor var13 = new RouteDescriptor(RouteDescriptor.Type.valueOf(var8), var9);
               if (!this.resolveDescriptor(var1, var13).isEmpty()) {
                  String var14 = var7.getString("quest", "").trim();
                  String var15 = var14.isEmpty() ? var21 : var14;
                  if (!var14.isEmpty()) {
                     var12 = "betonquest-quest:" + var14;
                  }

                  var2.putIfAbsent(var15, new IntegrationManager.AutoRoute(var15, var10, var13, var11, var12));
               }
            } catch (IllegalArgumentException var16) {
            }
         }
      }

      return var2;
   }

   public void putDynamicQuestRoute(UUID var1, String var2, String var3, RouteDescriptor var4, NavigationMode var5, String var6) {
      this.questRouteService.putRoute(var1, var2, var3, var4, var5, var6);
   }

   public Optional<TargetLocator> resolveAutoRoute(Player var1, IntegrationManager.AutoRoute var2) {
      return this.resolveDescriptor(var1, var2.descriptor()).map(var2x -> new TargetLocator() {
         @Override
         public String id() {
            return var2x.id();
         }

         @Override
         public String displayName() {
            return ChatColor.stripColor(Color.colorize(var2.displayName()));
         }

         @Override
         public Location resolve(Player var1) {
            return var2x.resolve(var1);
         }
      });
   }

   public boolean removeDynamicQuestRoute(UUID var1, String var2) {
      return this.questRouteService.removeRoute(var1, var2);
   }

   public int clearDynamicQuestRoutes(UUID var1) {
      return this.questRouteService.clearRoutes(var1);
   }

   public Set<String> getDynamicQuestRouteKeys(UUID var1) {
      return this.questRouteService.getRoutes(var1).keySet();
   }

   private TargetLocator buildPoiTarget(Player var1, String var2, ConfigurationSection var3) {
      String var4 = var3.getString("display_name", var2);
      if (var3.getBoolean("spawn", false)) {
         return new WorldSpawnTarget(var4, var3.getString("world"));
      }

      if (var3.contains("world") && var3.contains("x") && var3.contains("y") && var3.contains("z")) {
         String var18 = var3.getString("world");
         if (Bukkit.getWorld(var18) == null) {
            return null;
         }

         Location var19 = new Location(Bukkit.getWorld(var18), var3.getDouble("x"), var3.getDouble("y"), var3.getDouble("z"));
         return new StaticLocationTarget(var4, var19);
      } else {
         if (var3.contains("citizens_id")) {
            return !this.isPluginEnabled("Citizens") ? null : new CitizensNpcTarget(String.valueOf(var3.getInt("citizens_id")), var4);
         }

         if (var3.contains("citizens_name")) {
            return !this.isPluginEnabled("Citizens") ? null : new CitizensNpcTarget(var3.getString("citizens_name"), var4);
         }

         if (var3.contains("fancynpc")) {
            if (!this.isPluginEnabled("FancyNpcs")) {
               return null;
            }

            String var17 = var3.getString("fancynpc", "").trim();
            return var17.isEmpty() ? null : new FancyNpcTarget(var17, var4);
         } else if (var3.contains("shopkeeper")) {
            if (!this.isPluginEnabled("Shopkeepers")) {
               return null;
            }

            String var16 = var3.getString("shopkeeper", "").trim();
            return var16.isEmpty() ? null : new ShopkeeperTarget(var16, var4);
         } else {
            if (var3.contains("mythic_mob")) {
               return !this.isPluginEnabled("MythicMobs") ? null : new MythicMobTarget(var3.getString("mythic_mob"), 1000.0);
            }

            if (var3.contains("elite_mob")) {
               return this.buildPresetHintTarget("elitemobs", var3.getString("elite_mob"), var4, 1000.0);
            }

            if (var3.contains("modelengine_model")) {
               return this.buildPresetHintTarget("modelengine", var3.getString("modelengine_model"), var4, 1000.0);
            }

            if (var3.contains("denizen_npc")) {
               if (this.isPluginEnabled("Denizen") && this.isPluginEnabled("Citizens")) {
                  String var15 = var3.getString("denizen_npc", "").trim();
                  return var15.isEmpty() ? null : new CitizensNpcTarget(var15, var4);
               } else {
                  return null;
               }
            } else if (var3.contains("znpcs_id")) {
               if (!this.isPluginEnabled("ZNPCsPlus")) {
                  return null;
               }

               String var14 = String.valueOf(var3.get("znpcs_id", "")).trim();
               return var14.isEmpty() ? null : new ZnpcsPlusNpcTarget(var14, var4);
            } else if (var3.contains("znpcs_name")) {
               if (!this.isPluginEnabled("ZNPCsPlus")) {
                  return null;
               }

               String var13 = var3.getString("znpcs_name", "").trim();
               return var13.isEmpty() ? null : new ZnpcsPlusNpcTarget(var13, var4);
            } else {
               if (var3.contains("ecoboss_mob")) {
                  return this.buildPresetHintTarget("ecobosses", var3.getString("ecoboss_mob"), var4, 1000.0);
               }

               if (var3.contains("essentials_warp")) {
                  return this.resolveLocationPoi(var1, "warp:" + var3.getString("essentials_warp"), var4);
               }

               if (var3.contains("cmi_warp")) {
                  return this.resolveLocationPoi(var1, "cmiwarp:" + var3.getString("cmi_warp"), var4);
               }

               if (var3.contains("worldguard_region")) {
                  String var12 = var3.getString("worldguard_region", "").trim();
                  String var6 = var3.getString("worldguard_world", "").trim();
                  String var7 = var6.isBlank() ? var12 : var6 + ":" + var12;
                  return this.resolveLocationPoi(var1, "wg:" + var7, var4);
               }

               if (var3.contains("towny_town")) {
                  return this.resolveLocationPoi(var1, "town:" + var3.getString("towny_town"), var4);
               }

               if (var3.contains("decent_hologram")) {
                  return this.resolveLocationPoi(var1, "dh:" + var3.getString("decent_hologram"), var4);
               }

               if (var3.contains("vanilla_block")) {
                  return this.resolveBlockPoi(var1, "vanilla:" + var3.getString("vanilla_block"), var4);
               }

               if (var3.contains("itemsadder_block")) {
                  return this.resolveBlockPoi(var1, "itemsadder:" + var3.getString("itemsadder_block"), var4);
               }

               if (var3.contains("ia_block")) {
                  return this.resolveBlockPoi(var1, "ia:" + var3.getString("ia_block"), var4);
               }

               if (var3.contains("nexo_block")) {
                  return this.resolveBlockPoi(var1, "nexo:" + var3.getString("nexo_block"), var4);
               }

               if (var3.contains("oraxen_block")) {
                  return this.resolveBlockPoi(var1, "oraxen:" + var3.getString("oraxen_block"), var4);
               }

               if (var3.contains("entity_tag")) {
                  String var11 = var3.getString("entity_tag");
                  return var11 != null && !var11.isBlank()
                     ? new HintedEntityTarget(
                        "poi-tag:" + var11, var4, "*", 1000.0, Collections.emptyList(), List.of(var11), Collections.emptyList(), this.plugin.owner()
                     )
                     : null;
               }

               if (var3.contains("entity_metadata")) {
                  String var10 = var3.getString("entity_metadata");
                  return var10 != null && !var10.isBlank()
                     ? new HintedEntityTarget(
                        "poi-meta:" + var10, var4, "*", 1000.0, List.of(var10), Collections.emptyList(), Collections.emptyList(), this.plugin.owner()
                     )
                     : null;
               }

               if (var3.contains("entity_pdc")) {
                  String var9 = var3.getString("entity_pdc");
                  return var9 != null && !var9.isBlank()
                     ? new HintedEntityTarget(
                        "poi-pdc:" + var9, var4, "*", 1000.0, Collections.emptyList(), Collections.emptyList(), List.of(var9), this.plugin.owner()
                     )
                     : null;
               }

               if (var3.contains("entity_type")) {
                  try {
                     EntityType var5 = EntityType.valueOf(var3.getString("entity_type", "").toUpperCase());
                     return new NearestEntityTypeTarget(var5, 1000.0);
                  } catch (Exception var8) {
                     return null;
                  }
               } else {
                  return var3.contains("entity_name") ? new NamedEntityTarget(var3.getString("entity_name"), 1000.0) : null;
               }
            }
         }
      }
   }

   private TargetLocator resolveBlockPoi(Player var1, String var2, String var3) {
      Optional var4 = this.resolveBlockInput(var1, var2);
      if (var4.isEmpty()) {
         return null;
      }

      Location var5 = ((TargetLocator)var4.get()).resolve(var1);
      return var5 == null ? null : new StaticLocationTarget(var3, var5);
   }

   private TargetLocator resolveLocationPoi(Player var1, String var2, String var3) {
      Optional var4 = this.resolveLocationInput(var1, var2);
      if (var4.isEmpty()) {
         return null;
      }

      Location var5 = ((TargetLocator)var4.get()).resolve(var1);
      return var5 == null ? null : new StaticLocationTarget(var3, var5);
   }

   private boolean isPluginEnabled(String var1) {
      Plugin var2 = Bukkit.getPluginManager().getPlugin(var1);
      return var2 != null && var2.isEnabled();
   }

   private Optional<TargetLocator> resolvePresetHintMob(String var1, String var2) {
      TargetLocator var3 = this.buildPresetHintTarget(var1, var2, var1 + " " + var2, 1000.0);
      return Optional.ofNullable(var3);
   }

   private TargetLocator buildPresetHintTarget(String var1, String var2, String var3, double var4) {
      ConfigurationSection var6 = this.plugin.config().getConfigurationSection("integrations.hints." + var1);
      if (var6 == null) {
         return null;
      }

      String var7 = var6.getString("plugin");
      if (var7 != null && !var7.isBlank() && !this.isPluginEnabled(var7)) {
         return null;
      }

      String var8 = var2 != null && !var2.isBlank() ? var2.trim() : "*";
      return new HintedEntityTarget(
         var1 + ":" + var8,
         var3,
         var8,
         var4,
         var6.getStringList("metadata_keys"),
         var6.getStringList("scoreboard_tags"),
         var6.getStringList("pdc_keys"),
         this.plugin.owner()
      );
   }

   private Optional<TargetLocator> resolveWarpAlias(String var1) {
      Optional var2 = this.resolveEssentialsWarpTarget(var1);
      return var2.isPresent() ? var2 : this.resolveCmiWarpTarget(var1);
   }

   private Optional<TargetLocator> resolveEssentialsWarpTarget(String var1) {
      if (!this.plugin.config().getBoolean("integrations.allow_warp_targets", true)) {
         return Optional.empty();
      }

      if (var1 == null || var1.isBlank()) {
         return Optional.empty();
      }

      if (!this.isPluginEnabled("Essentials")) {
         return Optional.empty();
      }

      String var2 = var1.trim();

      try {
         Plugin var4 = Bukkit.getPluginManager().getPlugin("Essentials");
         if (var4 == null) {
            return Optional.empty();
         }

         Method var5 = this.findFirstMethod(var4.getClass(), "getWarps");
         if (var5 == null) {
            return Optional.empty();
         }

         Object var6 = var5.invoke(var4);
         if (var6 == null) {
            return Optional.empty();
         }

         Method var7 = this.findFirstMethod(var6.getClass(), new Class[]{String.class}, "getWarp", "getWarpLocation", "get");
         if (var7 == null) {
            return Optional.empty();
         }

         Object var8 = var7.invoke(var6, var2);
         Location var3;
         return var8 instanceof Location && (var3 = (Location)var8).getWorld() != null
            ? Optional.of(new StaticLocationTarget("Warp " + var2, var3))
            : Optional.empty();
      } catch (Exception var9) {
         return Optional.empty();
      }
   }

   private Optional<TargetLocator> resolveCmiWarpTarget(String var1) {
      if (!this.plugin.config().getBoolean("integrations.allow_warp_targets", true)) {
         return Optional.empty();
      }

      if (!this.plugin.config().getBoolean("integrations.allow_cmi_warp_targets", true)) {
         return Optional.empty();
      }

      if (var1 == null || var1.isBlank()) {
         return Optional.empty();
      }

      if (!this.isPluginEnabled("CMI")) {
         return Optional.empty();
      }

      Object var2 = this.getCmiWarpManager();
      if (var2 == null) {
         return Optional.empty();
      }

      String var3 = var1.trim();
      Object var4 = this.findCmiWarp(var2, var3);
      if (var4 == null) {
         return Optional.empty();
      }

      Location var5 = this.extractFlexibleLocation(var4);
      return var5 != null && var5.getWorld() != null ? Optional.of(new StaticLocationTarget("Warp " + var3, var5)) : Optional.empty();
   }

   private Object getCmiWarpManager() {
      try {
         Class var1 = Class.forName("com.Zrips.CMI.CMI");
         Method var2 = this.findFirstMethod(var1, "getInstance");
         if (var2 == null) {
            return null;
         }

         Object var3 = var2.invoke(null);
         return var3 == null ? null : this.invokeNoArg(var3, "getWarpManager", "getWarpsManager", "getCMIWarpManager");
      } catch (Exception var4) {
         return null;
      }
   }

   private Object findCmiWarp(Object var1, String var2) {
      if (var1 != null && var2 != null && !var2.isBlank()) {
         Method var4 = this.findFirstMethod(var1.getClass(), new Class[]{String.class}, "getWarp", "getByName", "get");
         if (var4 != null) {
            try {
               Object var5 = var4.invoke(var1, var2);
               if (var5 != null) {
                  return var5;
               }
            } catch (Exception var9) {
            }
         }

         Object var3;
         if ((var3 = this.invokeNoArg(var1, "getWarps", "getWarpMap", "getWarpsMap", "getAllWarps")) instanceof Map) {
            Map var11 = (Map)var3;
            Object var12 = var11.get(var2);
            if (var12 != null) {
               return var12;
            }

            for (Object var8Object : var11.entrySet()) {
               Entry<?, ?> var8 = (Entry<?, ?>)var8Object;
               if (var8.getKey() != null && String.valueOf(var8.getKey()).equalsIgnoreCase(var2)) {
                  return var8.getValue();
               }

               if (this.matchesNamedValue(var8.getValue(), var2)) {
                  return var8.getValue();
               }
            }

            return null;
         } else {
            if (var3 instanceof Iterable) {
               for (Object var7 : (Iterable)var3) {
                  if (this.matchesNamedValue(var7, var2)) {
                     return var7;
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private boolean matchesNamedValue(Object var1, String var2) {
      if (var1 != null && var2 != null && !var2.isBlank()) {
         Object var3 = this.invokeNoArg(var1, "getName", "name", "getWarpName");
         return var3 != null && String.valueOf(var3).equalsIgnoreCase(var2);
      } else {
         return false;
      }
   }

   private Optional<TargetLocator> resolveTownyTownTarget(String var1) {
      if (!this.plugin.config().getBoolean("integrations.allow_town_targets", true)) {
         return Optional.empty();
      }

      if (var1 == null || var1.isBlank()) {
         return Optional.empty();
      }

      if (!this.isPluginEnabled("Towny")) {
         return Optional.empty();
      }

      String var2 = var1.trim();

      try {
         Class var4 = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
         Object var5 = var4.getMethod("getInstance").invoke(null);
         if (var5 == null) {
            return Optional.empty();
         }

         Method var6 = this.findFirstMethod(var5.getClass(), new Class[]{String.class}, "getTown", "getTownOrNull");
         if (var6 == null) {
            return Optional.empty();
         }

         Object var7 = var6.invoke(var5, var2);
         if (var7 == null) {
            return Optional.empty();
         }

         Location var8 = null;

         try {
            var8 = this.extractFlexibleLocation(this.invokeNoArg(var7, "getSpawnOrNull", "getSpawnLocation"));
         } catch (Exception var11) {
         }

         Method var3;
         if (var8 == null && (var3 = this.findFirstMethod(var7.getClass(), "getSpawn")) != null) {
            try {
               var8 = this.extractFlexibleLocation(var3.invoke(var7));
            } catch (Exception var10) {
            }
         }

         if (var8 == null) {
            var8 = this.extractFlexibleLocation(this.invokeNoArg(var7, "getHomeBlock", "getHomeBlockOrNull"));
         }

         return var8 != null && var8.getWorld() != null ? Optional.of(new StaticLocationTarget("Town " + var2, var8)) : Optional.empty();
      } catch (Exception var12) {
         return Optional.empty();
      }
   }

   private Optional<TargetLocator> resolveDecentHologramTarget(String var1) {
      if (!this.plugin.config().getBoolean("integrations.allow_hologram_targets", true)) {
         return Optional.empty();
      }

      if (var1 == null || var1.isBlank()) {
         return Optional.empty();
      }

      if (!this.isPluginEnabled("DecentHolograms")) {
         return Optional.empty();
      }

      String var2 = var1.trim();

      try {
         Class var3 = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
         Method var4 = this.findFirstMethod(var3, new Class[]{String.class}, "getHologram");
         if (var4 == null) {
            return Optional.empty();
         }

         Object var5 = var4.invoke(null, var2);
         if (var5 == null) {
            return Optional.empty();
         }

         Location var6 = this.extractFlexibleLocation(var5);
         return var6 != null && var6.getWorld() != null ? Optional.of(new StaticLocationTarget("Hologram " + var2, var6)) : Optional.empty();
      } catch (Exception var7) {
         return Optional.empty();
      }
   }

   private Optional<TargetLocator> resolveWorldGuardRegionTarget(Player var1, String var2) {
      if (!this.plugin.config().getBoolean("integrations.allow_region_targets", true)) {
         return Optional.empty();
      }

      if (var1 == null || var2 == null || var2.isBlank()) {
         return Optional.empty();
      }

      if (!this.isPluginEnabled("WorldGuard")) {
         return Optional.empty();
      }

      String var3 = var1.getWorld().getName();
      String var4 = var2.trim();
      int var5 = var4.indexOf(58);
      if (var5 > 0) {
         var3 = var4.substring(0, var5).trim();
         var4 = var4.substring(var5 + 1).trim();
      }

      if (!var3.isBlank() && !var4.isBlank()) {
         World var6 = Bukkit.getWorld(var3);
         if (var6 == null) {
            return Optional.empty();
         }

         Location var7 = this.fetchWorldGuardRegionCenter(var6, var4);
         return var7 == null ? Optional.empty() : Optional.of(new StaticLocationTarget("Region " + var4, var7));
      } else {
         return Optional.empty();
      }
   }

   private Location fetchWorldGuardRegionCenter(World var1, String var2) {
      Object var3 = this.getWorldGuardRegionManager(var1);
      if (var3 == null) {
         return null;
      }

      Object var4 = this.findWorldGuardRegion(var3, var2);
      if (var4 == null) {
         return null;
      }

      Object var5 = this.invokeNoArg(var4, "getMinimumPoint", "getMin");
      Object var6 = this.invokeNoArg(var4, "getMaximumPoint", "getMax");
      if (var5 != null && var6 != null) {
         Double var7 = this.readVectorComponent(var5, 'x');
         Double var8 = this.readVectorComponent(var5, 'y');
         Double var9 = this.readVectorComponent(var5, 'z');
         Double var10 = this.readVectorComponent(var6, 'x');
         Double var11 = this.readVectorComponent(var6, 'y');
         Double var12 = this.readVectorComponent(var6, 'z');
         if (var7 != null && var8 != null && var9 != null && var10 != null && var11 != null && var12 != null) {
            double var13 = (var7 + var10) / 2.0 + 0.5;
            double var15 = (var8 + var11) / 2.0;
            double var17 = (var9 + var12) / 2.0 + 0.5;
            return new Location(var1, var13, var15, var17);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private List<String> fetchWorldGuardRegionNames(World var1) {
      Object var2 = this.getWorldGuardRegionManager(var1);
      if (var2 == null) {
         return Collections.emptyList();
      } else if (!(this.invokeNoArg(var2, "getRegions") instanceof Map var4)) {
         return Collections.emptyList();
      } else {
         ArrayList var5 = new ArrayList();

         for (Object var7 : var4.keySet()) {
            if (var7 != null) {
               var5.add(String.valueOf(var7));
            }
         }

         return var5;
      }
   }

   private Object getWorldGuardRegionManager(World var1) {
      if (var1 == null) {
         return null;
      }

      try {
         Class var2 = Class.forName("com.sk89q.worldguard.WorldGuard");
         Object var3 = var2.getMethod("getInstance").invoke(null);
         Object var4 = var3.getClass().getMethod("getPlatform").invoke(var3);
         Object var5 = var4.getClass().getMethod("getRegionContainer").invoke(var4);
         Class var6 = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
         Object var7 = var6.getMethod("adapt", World.class).invoke(null, var1);
         if (var7 == null) {
            return null;
         }

         Class var8 = Class.forName("com.sk89q.worldedit.world.World");
         Method var9 = var5.getClass().getMethod("get", var8);
         return var9.invoke(var5, var7);
      } catch (Exception var10) {
         return null;
      }
   }

   private Object findWorldGuardRegion(Object var1, String var2) {
      if (var1 != null && var2 != null && !var2.isBlank()) {
         String var3 = var2.trim();

         try {
            Method var5 = this.findFirstMethod(var1.getClass(), new Class[]{String.class}, "getRegion", "get");
            Object var4;
            if (var5 != null && (var4 = var5.invoke(var1, var3)) != null) {
               return var4;
            }
         } catch (Exception var9) {
         }

         if (!(this.invokeNoArg(var1, "getRegions") instanceof Map var11)) {
            return null;
         } else {
            Object var6 = var11.get(var3);
            if (var6 != null) {
               return var6;
            }

            for (Object var8Object : var11.entrySet()) {
               Entry<?, ?> var8 = (Entry<?, ?>)var8Object;
               if (var8.getKey() != null && String.valueOf(var8.getKey()).equalsIgnoreCase(var3)) {
                  return var8.getValue();
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private Object invokeNoArg(Object var1, String... var2) {
      if (var1 == null) {
         return null;
      }

      Method var3 = this.findFirstMethod(var1.getClass(), var2);
      if (var3 == null) {
         return null;
      }

      try {
         return var3.invoke(var1);
      } catch (Exception var5) {
         return null;
      }
   }

   private Double readVectorComponent(Object var1, char var2) {
      String[] var4;
      switch (var2) {
         case 'x':
            String[] var14 = new String[]{"getX", "x", null};
            var4 = var14;
            var14[2] = "getBlockX";
            break;
         case 'y':
            String[] var13 = new String[]{"getY", "y", null};
            var4 = var13;
            var13[2] = "getBlockY";
            break;
         case 'z':
            String[] var5 = new String[]{"getZ", "z", null};
            var4 = var5;
            var5[2] = "getBlockZ";
            break;
         default:
            var4 = new String[0];
      }

      for (String var8 : var4) {
         try {
            Method var9 = var1.getClass().getMethod(var8);
            if (var9.invoke(var1) instanceof Number var11) {
               return var11.doubleValue();
            }
         } catch (Exception var12) {
         }
      }

      return null;
   }

   private boolean containsIgnoreCase(List<String> var1, String var2) {
      if (var2 == null) {
         return false;
      }

      for (String var4 : var1) {
         if (var2.equalsIgnoreCase(var4)) {
            return true;
         }
      }

      return false;
   }

   private void collectNamesFromValue(Object var1, List<String> var2) {
      if (var1 != null && var2 != null) {
         if (var1 instanceof Map<?, ?> var7) {
            for (Entry<?, ?> var9 : var7.entrySet()) {
               String var6;
               if (var9.getKey() != null && !(var6 = String.valueOf(var9.getKey()).trim()).isEmpty() && !this.containsIgnoreCase(var2, var6)) {
                  var2.add(var6);
               }

               this.addNameFromObject(var9.getValue(), var2);
            }
         } else if (!(var1 instanceof Iterable)) {
            this.addNameFromObject(var1, var2);
         } else {
            for (Object var5 : (Iterable)var1) {
               this.addNameFromObject(var5, var2);
            }
         }
      }
   }

   private void addNameFromObject(Object var1, List<String> var2) {
      if (var1 != null && var2 != null) {
         if (var1 instanceof String var6) {
            String var7 = var6.trim();
            if (!var7.isEmpty() && !this.containsIgnoreCase(var2, var7)) {
               var2.add(var7);
            }
         } else {
            Object var4 = this.invokeNoArg(var1, "getName", "name", "getWarpName");
            String var3;
            if (var4 != null && !(var3 = String.valueOf(var4).trim()).isEmpty() && !this.containsIgnoreCase(var2, var3)) {
               var2.add(var3);
            } else {
               String var5 = String.valueOf(var1).trim();
               if (!var5.isEmpty() && !var5.contains("@") && !this.containsIgnoreCase(var2, var5)) {
                  var2.add(var5);
               }
            }
         }
      }
   }

   private Location extractFlexibleLocation(Object var1) {
      if (var1 == null) {
         return null;
      } else if (var1 instanceof Location var12) {
         return var12;
      } else {
         Object var6 = this.invokeNoArg(var1, "getLocation", "getLoc", "toLocation", "getBukkitLoc", "getBukkitLocation");
         if (var6 instanceof Location var13) {
            return var13;
         } else {
            Location var5;
            if (var6 != null && var6 != var1 && (var5 = this.extractFlexibleLocation(var6)) != null) {
               return var5;
            }

            Object var7 = this.invokeNoArg(var1, "getWorld");
            World var3;
            World var4 = var7 instanceof World ? (var3 = (World)var7) : null;
            if (var4 == null && var7 != null) {
               var4 = Bukkit.getWorld(String.valueOf(var7));
            }

            String var2;
            if (var4 == null && (var2 = this.stringValue(this.invokeNoArg(var1, "getWorldName", "getName"))) != null) {
               var4 = Bukkit.getWorld(var2);
            }

            if (var4 == null) {
               return null;
            }

            Double var9 = this.numericValue(this.invokeNoArg(var1, "getX", "getBlockX", "x"));
            Double var10 = this.numericValue(this.invokeNoArg(var1, "getY", "getBlockY", "y"));
            Double var11 = this.numericValue(this.invokeNoArg(var1, "getZ", "getBlockZ", "z"));
            return var9 != null && var10 != null && var11 != null ? new Location(var4, var9, var10, var11) : null;
         }
      }
   }

   private Double numericValue(Object var1) {
      if (var1 instanceof Number var5) {
         return var5.doubleValue();
      } else {
         if (var1 instanceof String var2) {
            try {
               return Double.parseDouble(var2);
            } catch (NumberFormatException var4) {
            }
         }

         return null;
      }
   }

   private String stringValue(Object var1) {
      if (var1 == null) {
         return null;
      }

      String var2 = String.valueOf(var1).trim();
      return var2.isEmpty() ? null : var2;
   }

   private IntegrationManager.BlockQuery parseBlockQuery(String var1) {
      String var2 = var1 == null ? "" : var1.trim();
      if (var2.isEmpty()) {
         return null;
      } else {
         String var4 = var2.toLowerCase(Locale.ROOT);
         if (var4.startsWith("itemsadder:")) {
            return new IntegrationManager.BlockQuery(IntegrationManager.BlockProvider.ITEMSADDER, var2.substring("itemsadder:".length()).trim(), null);
         } else if (var4.startsWith("ia:")) {
            return new IntegrationManager.BlockQuery(IntegrationManager.BlockProvider.ITEMSADDER, var2.substring("ia:".length()).trim(), null);
         } else if (var4.startsWith("nexo:")) {
            return new IntegrationManager.BlockQuery(IntegrationManager.BlockProvider.NEXO, var2.substring("nexo:".length()).trim(), null);
         } else if (var4.startsWith("oraxen:")) {
            return new IntegrationManager.BlockQuery(IntegrationManager.BlockProvider.ORAXEN, var2.substring("oraxen:".length()).trim(), null);
         } else if (var4.startsWith("minecraft:")) {
            Material var8 = this.parseMaterial(var2.substring("minecraft:".length()));
            return var8 == null
               ? null
               : new IntegrationManager.BlockQuery(IntegrationManager.BlockProvider.VANILLA, var8.name().toLowerCase(Locale.ROOT), var8);
         } else if (var4.startsWith("vanilla:")) {
            Material var7 = this.parseMaterial(var2.substring("vanilla:".length()));
            return var7 == null
               ? null
               : new IntegrationManager.BlockQuery(IntegrationManager.BlockProvider.VANILLA, var7.name().toLowerCase(Locale.ROOT), var7);
         } else if (var4.startsWith("block:")) {
            Material var6 = this.parseMaterial(var2.substring("block:".length()));
            return var6 == null
               ? null
               : new IntegrationManager.BlockQuery(IntegrationManager.BlockProvider.VANILLA, var6.name().toLowerCase(Locale.ROOT), var6);
         } else {
            Material var5 = this.parseMaterial(var2);
            if (var5 != null) {
               return new IntegrationManager.BlockQuery(IntegrationManager.BlockProvider.VANILLA, var5.name().toLowerCase(Locale.ROOT), var5);
            } else {
               return var2.contains(":") ? new IntegrationManager.BlockQuery(IntegrationManager.BlockProvider.ANY_CUSTOM, var2, null) : null;
            }
         }
      }
   }

   private String buildBlockDisplayName(IntegrationManager.BlockQuery var1, Location var2) {
      String var3 = switch (var1.provider()) {
         case VANILLA -> "Block";
         case ITEMSADDER -> "ItemsAdder";
         case NEXO -> "Nexo";
         case ORAXEN -> "Oraxen";
         case ANY_CUSTOM -> "Custom block";
      };
      String var4 = var1.provider() == IntegrationManager.BlockProvider.VANILLA && var1.material() != null
         ? var1.material().name().toLowerCase(Locale.ROOT)
         : var1.query();
      return var3 + " " + var4 + " @ " + var2.getBlockX() + " " + var2.getBlockY() + " " + var2.getBlockZ();
   }

   private Location findNearestBlock(Player var1, IntegrationManager.BlockQuery var2, double var3, int var5) {
      World var6 = var1.getWorld();
      Location var7 = var1.getLocation();
      double var8 = var3 * var3;
      int var10 = (int)Math.floor(var7.getX() - var3);
      int var11 = (int)Math.ceil(var7.getX() + var3);
      int var12 = (int)Math.floor(var7.getZ() - var3);
      int var13 = (int)Math.ceil(var7.getZ() + var3);
      int var14 = var6.getMinHeight();
      int var15 = var6.getMaxHeight() - 1;
      int var16 = var14;
      int var17 = var15;
      if (var5 >= 0) {
         int var18 = var7.getBlockY();
         var16 = Math.max(var14, var18 - var5);
         var17 = Math.min(var15, var18 + var5);
      }

      int var49 = Math.floorDiv(var10, 16);
      int var19 = Math.floorDiv(var11, 16);
      int var20 = Math.floorDiv(var12, 16);
      int var21 = Math.floorDiv(var13, 16);
      Location var22 = null;
      double var23 = Double.MAX_VALUE;

      for (int var25 = var49; var25 <= var19; var25++) {
         for (int var26 = var20; var26 <= var21; var26++) {
            if (var6.isChunkLoaded(var25, var26)) {
               int var27 = var25 << 4;
               int var28 = var27 + 15;
               int var29 = var26 << 4;
               int var30 = var29 + 15;
               int var31 = Math.max(var10, var27);
               int var32 = Math.min(var11, var28);
               int var33 = Math.max(var12, var29);
               int var34 = Math.min(var13, var30);

               for (int var35 = var31; var35 <= var32; var35++) {
                  for (int var36 = var33; var36 <= var34; var36++) {
                     double var39 = var35 + 0.5 - var7.getX();
                     double var37;
                     double var41 = var39 * var39 + (var37 = var36 + 0.5 - var7.getZ()) * var37;
                     if (!(var41 > var8)) {
                        for (int var43 = var16; var43 <= var17; var43++) {
                           Block var48 = var6.getBlockAt(var35, var43, var36);
                           double var44;
                           double var46;
                           if (!var48.getType().isAir()
                              && this.matchesBlock(var48, var2)
                              && !((var46 = var41 + (var44 = var43 + 0.5 - var7.getY()) * var44) > var8)
                              && !(var46 >= var23)) {
                              var23 = var46;
                              var22 = var48.getLocation().add(0.5, 0.5, 0.5);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return var22;
   }

   private boolean matchesBlock(Block var1, IntegrationManager.BlockQuery var2) {
      return switch (var2.provider()) {
         case VANILLA -> var2.material() != null && var1.getType() == var2.material();
         case ITEMSADDER -> this.matchesIdentifier(this.resolveItemsAdderBlockId(var1), var2.query());
         case NEXO -> this.matchesIdentifier(this.resolveNexoBlockId(var1), var2.query());
         case ORAXEN -> this.matchesIdentifier(this.resolveOraxenBlockId(var1), var2.query());
         case ANY_CUSTOM -> this.matchesIdentifier(this.resolveItemsAdderBlockId(var1), var2.query())
            || this.matchesIdentifier(this.resolveNexoBlockId(var1), var2.query())
            || this.matchesIdentifier(this.resolveOraxenBlockId(var1), var2.query());
      };
   }

   private boolean matchesIdentifier(String var1, String var2) {
      if (var1 != null && !var1.isBlank()) {
         String var4 = var1.trim().toLowerCase(Locale.ROOT);
         String var3 = var2 == null ? "" : var2.trim().toLowerCase(Locale.ROOT);
         if (var3.isEmpty() || var3.equals("*")) {
            return true;
         } else if (var3.contains("*")) {
            String var6 = "\\Q" + var3.replace("*", "\\E.*\\Q") + "\\E";
            return var4.matches(var6);
         } else {
            return var4.equals(var3) || var4.endsWith(":" + var3);
         }
      } else {
         return false;
      }
   }

   private Material parseMaterial(String var1) {
      if (var1 != null && !var1.isBlank()) {
         String var2 = var1.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');

         try {
            return Material.valueOf(var2);
         } catch (IllegalArgumentException var4) {
            return null;
         }
      } else {
         return null;
      }
   }

   private String resolveItemsAdderBlockId(Block var1) {
      if (!this.isPluginEnabled("ItemsAdder")) {
         return null;
      }

      if (!this.initItemsAdderApi()) {
         return null;
      }

      try {
         Object var2 = this.itemsAdderByPlacedMethod.invoke(null, var1);
         return var2 == null ? null : this.normalizeId(this.itemsAdderIdMethod.invoke(var2));
      } catch (Exception var3) {
         return null;
      }
   }

   private boolean initItemsAdderApi() {
      if (this.itemsAdderApiChecked) {
         return this.itemsAdderByPlacedMethod != null && this.itemsAdderIdMethod != null;
      }

      this.itemsAdderApiChecked = true;

      try {
         Class var1 = Class.forName("dev.lone.itemsadder.api.CustomBlock");
         this.itemsAdderByPlacedMethod = this.findFirstMethod(var1, new Class[]{Block.class}, "byAlreadyPlaced");
         this.itemsAdderIdMethod = this.findFirstMethod(var1, "getNamespacedID", "getNamespacedId", "getId");
      } catch (Exception var2) {
         this.itemsAdderByPlacedMethod = null;
         this.itemsAdderIdMethod = null;
      }

      return this.itemsAdderByPlacedMethod != null && this.itemsAdderIdMethod != null;
   }

   private String resolveNexoBlockId(Block var1) {
      if (!this.isPluginEnabled("Nexo")) {
         return null;
      }

      if (!this.initNexoApi()) {
         return null;
      }

      try {
         Object var2 = this.nexoCustomBlockMethod.invoke(null, var1);
         return var2 != null && this.nexoMechanicIdMethod != null ? this.normalizeId(this.nexoMechanicIdMethod.invoke(var2)) : null;
      } catch (Exception var3) {
         return null;
      }
   }

   private boolean initNexoApi() {
      if (this.nexoApiChecked) {
         return this.nexoCustomBlockMethod != null && this.nexoMechanicIdMethod != null;
      }

      this.nexoApiChecked = true;

      try {
         Class var1 = Class.forName("com.nexomc.nexo.api.NexoBlocks");
         this.nexoCustomBlockMethod = this.findFirstMethod(var1, new Class[]{Block.class}, "customBlockMechanic");
         if (this.nexoCustomBlockMethod != null) {
            Class var2 = this.nexoCustomBlockMethod.getReturnType();
            this.nexoMechanicIdMethod = this.findFirstMethod(var2, "getItemID", "getItemId", "getID", "getId");
         }
      } catch (Exception var3) {
         this.nexoCustomBlockMethod = null;
         this.nexoMechanicIdMethod = null;
      }

      return this.nexoCustomBlockMethod != null && this.nexoMechanicIdMethod != null;
   }

   private String resolveOraxenBlockId(Block var1) {
      if (!this.isPluginEnabled("Oraxen")) {
         return null;
      }

      if (!this.initOraxenApi()) {
         return null;
      }

      try {
         Boolean var3;
         Object var4;
         if (this.oraxenIsBlockMethod != null
            && (var4 = this.invokeByBlockOrLocation(this.oraxenIsBlockMethod, var1)) instanceof Boolean
            && !((Boolean)var4)) {
            return null;
         }

         String var2;
         if (this.oraxenDirectIdMethod != null && (var2 = this.normalizeId(this.invokeByBlockOrLocation(this.oraxenDirectIdMethod, var1))) != null) {
            return var2;
         }

         if (this.oraxenMechanicMethod != null) {
            Object var5 = this.invokeByBlockOrLocation(this.oraxenMechanicMethod, var1);
            if (var5 == null) {
               return null;
            }

            if (this.oraxenMechanicIdMethod == null || !this.oraxenMechanicIdMethod.getDeclaringClass().isInstance(var5)) {
               this.oraxenMechanicIdMethod = this.findFirstMethod(var5.getClass(), "getItemID", "getItemId", "getID", "getId", "getModelName", "getModel");
            }

            if (this.oraxenMechanicIdMethod != null) {
               return this.normalizeId(this.oraxenMechanicIdMethod.invoke(var5));
            }
         }
      } catch (Exception var6) {
      }

      return null;
   }

   private boolean initOraxenApi() {
      if (this.oraxenApiChecked) {
         return this.oraxenBlocksClass != null;
      }

      this.oraxenApiChecked = true;

      try {
         this.oraxenBlocksClass = Class.forName("io.th0rgal.oraxen.api.OraxenBlocks");
         this.oraxenIsBlockMethod = this.findFirstMethod(
            this.oraxenBlocksClass, new Class[]{Block.class}, "isOraxenBlock", "isCustomBlock", "isOraxenNoteBlock", "isOraxenStringBlock"
         );
         this.oraxenDirectIdMethod = this.findFirstMethod(
            this.oraxenBlocksClass, new Class[]{Block.class}, "getBlockID", "getBlockId", "getIdByBlock", "getIDByBlock", "getId"
         );
         if (this.oraxenDirectIdMethod == null) {
            this.oraxenDirectIdMethod = this.findFirstMethod(
               this.oraxenBlocksClass, new Class[]{Location.class}, "getBlockID", "getBlockId", "getIdByLocation", "getIDByLocation", "getId"
            );
         }

         this.oraxenMechanicMethod = this.findFirstMethod(
            this.oraxenBlocksClass,
            new Class[]{Block.class},
            "getNoteBlockMechanic",
            "getStringBlockMechanic",
            "getBlockMechanic",
            "getMechanic",
            "getOraxenBlock"
         );
         if (this.oraxenMechanicMethod == null) {
            this.oraxenMechanicMethod = this.findFirstMethod(
               this.oraxenBlocksClass,
               new Class[]{Location.class},
               "getNoteBlockMechanic",
               "getStringBlockMechanic",
               "getBlockMechanic",
               "getMechanic",
               "getOraxenBlock"
            );
         }
      } catch (Exception var2) {
         this.oraxenBlocksClass = null;
         this.oraxenIsBlockMethod = null;
         this.oraxenDirectIdMethod = null;
         this.oraxenMechanicMethod = null;
      }

      return this.oraxenBlocksClass != null;
   }

   private Object invokeByBlockOrLocation(Method var1, Block var2) throws Exception {
      Class[] var3 = var1.getParameterTypes();
      if (var3.length != 1) {
         return null;
      } else if (Block.class.isAssignableFrom(var3[0])) {
         return var1.invoke(null, var2);
      } else {
         return Location.class.isAssignableFrom(var3[0]) ? var1.invoke(null, var2.getLocation()) : null;
      }
   }

   private Method findFirstMethod(Class<?> var1, String... var2) {
      if (var1 == null) {
         return null;
      }

      for (String var6 : var2) {
         try {
            return var1.getMethod(var6);
         } catch (NoSuchMethodException var8) {
         }
      }

      return null;
   }

   private Method findFirstMethod(Class<?> var1, Class<?>[] var2, String... var3) {
      if (var1 == null) {
         return null;
      }

      for (String var7 : var3) {
         try {
            return var1.getMethod(var7, var2);
         } catch (NoSuchMethodException var9) {
         }
      }

      return null;
   }

   private String normalizeId(Object var1) {
      if (var1 == null) {
         return null;
      }

      String var2 = String.valueOf(var1).trim();
      return !var2.isEmpty() && !"null".equalsIgnoreCase(var2) ? var2 : null;
   }

   private Optional<TargetLocator> resolveLocationDescriptor(Player var1, String var2) {
      String var3 = var2 == null ? "" : var2.trim();
      if (var3.isEmpty()) {
         return Optional.empty();
      }

      Optional var5 = this.resolveLocationInput(var1, var3);
      if (var5.isPresent()) {
         return var5;
      }

      String[] var6 = var3.split("[,;\\s]+");
      if (var6.length < 3) {
         return Optional.empty();
      }

      String var7 = null;
      Double var8 = null;
      Double var9 = null;
      Double var10 = null;
      if (var6.length >= 4 && Bukkit.getWorld(var6[0]) != null) {
         var7 = var6[0];
         var8 = this.parseDouble(var6[1]);
         var9 = this.parseDouble(var6[2]);
         var10 = this.parseDouble(var6[3]);
      } else if (var6.length >= 4 && Bukkit.getWorld(var6[3]) != null) {
         var7 = var6[3];
         var8 = this.parseDouble(var6[0]);
         var9 = this.parseDouble(var6[1]);
         var10 = this.parseDouble(var6[2]);
      } else if (var6.length >= 3) {
         var7 = var1.getWorld().getName();
         var8 = this.parseDouble(var6[0]);
         var9 = this.parseDouble(var6[1]);
         var10 = this.parseDouble(var6[2]);
      }

      return var7 != null && var8 != null && var9 != null && var10 != null && Bukkit.getWorld(var7) != null
         ? Optional.of(
            new StaticLocationTarget(
               "Location " + var8.intValue() + " " + var9.intValue() + " " + var10.intValue(), new Location(Bukkit.getWorld(var7), var8, var9, var10)
            )
         )
         : Optional.empty();
   }

   private Double parseDouble(String var1) {
      try {
         return Double.parseDouble(var1);
      } catch (NumberFormatException var3) {
         return null;
      }
   }

   public record AutoRoute(String key, String displayName, RouteDescriptor descriptor, NavigationMode mode, String source) {
   }

   private enum BlockProvider {
      VANILLA,
      ITEMSADDER,
      NEXO,
      ORAXEN,
      ANY_CUSTOM;
   }

   private record BlockQuery(IntegrationManager.BlockProvider provider, String query, Material material) {
   }
}
