/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.gps.integration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import sk.enorials.gps.integration.QuestRouteNames;
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
    private final Map<UUID, Map<String, AutoRoute>> externalRoutes = new HashMap<UUID, Map<String, AutoRoute>>();
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

    public void putExternalRoute(UUID uUID2, String string, String string2, RouteDescriptor routeDescriptor, NavigationMode navigationMode, String string3) {
        this.externalRoutes.computeIfAbsent(uUID2, uUID -> new LinkedHashMap()).put(string, new AutoRoute(string, string2, routeDescriptor, navigationMode, string3));
    }

    public boolean removeExternalRoute(UUID uUID, String string) {
        boolean bl;
        Map<String, AutoRoute> map = this.externalRoutes.get(uUID);
        if (map == null) {
            return false;
        }
        boolean bl2 = bl = map.remove(string) != null;
        if (map.isEmpty()) {
            this.externalRoutes.remove(uUID);
        }
        return bl;
    }

    public IntegrationManager(EnorialsGpsPlugin enorialsGpsPlugin, QuestRouteService questRouteService) {
        this.plugin = enorialsGpsPlugin;
        this.conditionEvaluator = new ConditionEvaluator(enorialsGpsPlugin);
        this.questRouteService = questRouteService;
    }

    public boolean isPlayerTargetsEnabled() {
        return this.plugin.config().getBoolean("integrations.allow_player_targets", true);
    }

    public boolean isLocationTargetsEnabled() {
        return this.plugin.config().getBoolean("integrations.allow_location_targets", true);
    }

    public Optional<TargetLocator> resolveLocationInput(Player player, String string) {
        if (!this.isLocationTargetsEnabled()) {
            return Optional.empty();
        }
        if (player == null || string == null || string.isBlank()) {
            return Optional.empty();
        }
        String string2 = string.trim();
        String string3 = string2.toLowerCase(Locale.ROOT);
        if (string3.startsWith("warp:")) {
            return this.resolveWarpAlias(string2.substring("warp:".length()).trim());
        }
        if (string3.startsWith("esswarp:")) {
            return this.resolveWarpAlias(string2.substring("esswarp:".length()).trim());
        }
        if (string3.startsWith("ess:")) {
            return this.resolveWarpAlias(string2.substring("ess:".length()).trim());
        }
        if (string3.startsWith("essentials:")) {
            return this.resolveWarpAlias(string2.substring("essentials:".length()).trim());
        }
        if (string3.startsWith("cmiwarp:")) {
            return this.resolveCmiWarpTarget(string2.substring("cmiwarp:".length()).trim());
        }
        if (string3.startsWith("cmi:")) {
            return this.resolveCmiWarpTarget(string2.substring("cmi:".length()).trim());
        }
        if (string3.startsWith("wg:")) {
            return this.resolveWorldGuardRegionTarget(player, string2.substring("wg:".length()).trim());
        }
        if (string3.startsWith("worldguard:")) {
            return this.resolveWorldGuardRegionTarget(player, string2.substring("worldguard:".length()).trim());
        }
        if (string3.startsWith("region:")) {
            return this.resolveWorldGuardRegionTarget(player, string2.substring("region:".length()).trim());
        }
        if (string3.startsWith("towny:")) {
            return this.resolveTownyTownTarget(string2.substring("towny:".length()).trim());
        }
        if (string3.startsWith("town:")) {
            return this.resolveTownyTownTarget(string2.substring("town:".length()).trim());
        }
        if (string3.startsWith("dh:")) {
            return this.resolveDecentHologramTarget(string2.substring("dh:".length()).trim());
        }
        if (string3.startsWith("decentholograms:")) {
            return this.resolveDecentHologramTarget(string2.substring("decentholograms:".length()).trim());
        }
        if (string3.startsWith("holo:")) {
            return this.resolveDecentHologramTarget(string2.substring("holo:".length()).trim());
        }
        return Optional.empty();
    }

    public List<String> getWarpNames() {
        ArrayList<String> arrayList = new ArrayList<String>();
        for (String string : this.getEssentialsWarpNames()) {
            if (this.containsIgnoreCase(arrayList, string)) continue;
            arrayList.add(string);
        }
        for (String string : this.getCmiWarpNames()) {
            if (this.containsIgnoreCase(arrayList, string)) continue;
            arrayList.add(string);
        }
        return arrayList;
    }

    public List<String> getEssentialsWarpNames() {
        if (!this.plugin.config().getBoolean("integrations.allow_warp_targets", true)) {
            return Collections.emptyList();
        }
        if (!this.isPluginEnabled("Essentials")) {
            return Collections.emptyList();
        }
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if (plugin == null) {
                return Collections.emptyList();
            }
            Method method = plugin.getClass().getMethod("getWarps", new Class[0]);
            Object object = method.invoke((Object)plugin, new Object[0]);
            if (object == null) {
                return Collections.emptyList();
            }
            Method method2 = this.findFirstMethod(object.getClass(), "getList", "getWarpNames", "list");
            if (method2 == null) {
                return Collections.emptyList();
            }
            Object object2 = method2.invoke(object, new Object[0]);
            ArrayList<String> arrayList = new ArrayList<String>();
            if (object2 instanceof Collection) {
                Collection collection = (Collection)object2;
                for (Object e : collection) {
                    if (e == null) continue;
                    arrayList.add(String.valueOf(e));
                }
            }
            return arrayList;
        }
        catch (Exception exception) {
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
        Object object = this.getCmiWarpManager();
        if (object == null) {
            return Collections.emptyList();
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        this.collectNamesFromValue(this.invokeNoArg(object, "getWarps", "getWarpMap", "getWarpsMap", "getAllWarps"), arrayList);
        this.collectNamesFromValue(this.invokeNoArg(object, "getWarpNames", "getNames", "list"), arrayList);
        return arrayList;
    }

    public List<String> getTownyTownNames() {
        if (!this.plugin.config().getBoolean("integrations.allow_town_targets", true)) {
            return Collections.emptyList();
        }
        if (!this.isPluginEnabled("Towny")) {
            return Collections.emptyList();
        }
        try {
            Class<?> clazz = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object object = clazz.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            if (object == null) {
                return Collections.emptyList();
            }
            ArrayList<String> arrayList = new ArrayList<String>();
            this.collectNamesFromValue(this.invokeNoArg(object, "getTowns"), arrayList);
            this.collectNamesFromValue(this.invokeNoArg(object, "getTownsMap"), arrayList);
            return arrayList;
        }
        catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    public List<String> getDecentHologramNames() {
        Method method;
        Class<?> clazz;
        if (!this.plugin.config().getBoolean("integrations.allow_hologram_targets", true)) {
            return Collections.emptyList();
        }
        if (!this.isPluginEnabled("DecentHolograms")) {
            return Collections.emptyList();
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        try {
            clazz = Class.forName("eu.decentsoftware.holograms.api.holograms.Hologram");
            method = this.findFirstMethod(clazz, "getCachedHologramNames");
            if (method != null) {
                this.collectNamesFromValue(method.invoke(null, new Object[0]), arrayList);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            clazz = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            method = this.findFirstMethod(clazz, "getHologramNames");
            if (method != null) {
                this.collectNamesFromValue(method.invoke(null, new Object[0]), arrayList);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return arrayList;
    }

    public List<String> getWorldGuardRegionNames(Player player) {
        if (!this.plugin.config().getBoolean("integrations.allow_region_targets", true)) {
            return Collections.emptyList();
        }
        if (player == null || !this.isPluginEnabled("WorldGuard")) {
            return Collections.emptyList();
        }
        return this.fetchWorldGuardRegionNames(player.getWorld());
    }

    public Optional<TargetLocator> resolveMobInput(String string) {
        if (!this.plugin.config().getBoolean("integrations.allow_mob_targets", true)) {
            return Optional.empty();
        }
        if (string == null || string.isBlank()) {
            return Optional.empty();
        }
        String string2 = string.toLowerCase(Locale.ROOT);
        if (string2.startsWith("elite:")) {
            return this.resolvePresetHintMob("elitemobs", string.substring("elite:".length()));
        }
        if (string2.startsWith("elitemobs:")) {
            return this.resolvePresetHintMob("elitemobs", string.substring("elitemobs:".length()));
        }
        if (string2.startsWith("modelengine:")) {
            return this.resolvePresetHintMob("modelengine", string.substring("modelengine:".length()));
        }
        if (string2.startsWith("model:")) {
            return this.resolvePresetHintMob("modelengine", string.substring("model:".length()));
        }
        if (string2.startsWith("denizen:")) {
            return this.resolvePresetHintMob("denizen", string.substring("denizen:".length()));
        }
        if (string2.startsWith("mmocore:")) {
            return this.resolvePresetHintMob("mmocore", string.substring("mmocore:".length()));
        }
        if (string2.startsWith("mmoitems:")) {
            return this.resolvePresetHintMob("mmoitems", string.substring("mmoitems:".length()));
        }
        if (string2.startsWith("ecoboss:")) {
            return this.resolvePresetHintMob("ecobosses", string.substring("ecoboss:".length()));
        }
        if (string2.startsWith("ecobosses:")) {
            return this.resolvePresetHintMob("ecobosses", string.substring("ecobosses:".length()));
        }
        if (string2.startsWith("tag:")) {
            String string3 = string.substring("tag:".length()).trim();
            return Optional.of(new HintedEntityTarget("tag:" + string3, "Tag " + string3, "*", 1000.0, Collections.emptyList(), List.of(string3), Collections.emptyList(), (Plugin)this.plugin.owner()));
        }
        if (string2.startsWith("meta:")) {
            String string4 = string.substring("meta:".length()).trim();
            return Optional.of(new HintedEntityTarget("meta:" + string4, "Metadata " + string4, "*", 1000.0, List.of(string4), Collections.emptyList(), Collections.emptyList(), (Plugin)this.plugin.owner()));
        }
        if (string2.startsWith("pdc:")) {
            String string5 = string.substring("pdc:".length()).trim();
            return Optional.of(new HintedEntityTarget("pdc:" + string5, "PDC " + string5, "*", 1000.0, Collections.emptyList(), Collections.emptyList(), List.of(string5), (Plugin)this.plugin.owner()));
        }
        if (string2.startsWith("mythic:")) {
            String string6 = string.substring("mythic:".length());
            if (!this.isPluginEnabled("MythicMobs")) {
                return Optional.empty();
            }
            return Optional.of(new MythicMobTarget(string6, 512.0));
        }
        try {
            EntityType entityType = EntityType.valueOf((String)string.toUpperCase());
            return Optional.of(new NearestEntityTypeTarget(entityType, 512.0));
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return Optional.of(new NamedEntityTarget(string, 512.0));
        }
    }

    public Optional<TargetLocator> resolveNpcInput(String string) {
        if (!this.plugin.config().getBoolean("integrations.allow_npc_targets", true)) {
            return Optional.empty();
        }
        if (string == null || string.isBlank()) {
            return Optional.empty();
        }
        String string2 = string.toLowerCase(Locale.ROOT);
        if (string2.startsWith("citizens:")) {
            String string3 = string.substring("citizens:".length()).trim();
            if (!this.isPluginEnabled("Citizens")) {
                return Optional.empty();
            }
            return Optional.of(new CitizensNpcTarget(string3, "NPC " + string3));
        }
        if (string2.startsWith("denizen:")) {
            String string4 = string.substring("denizen:".length()).trim();
            if (!this.isPluginEnabled("Denizen") || !this.isPluginEnabled("Citizens")) {
                return Optional.empty();
            }
            return Optional.of(new CitizensNpcTarget(string4, "Denizen NPC " + string4));
        }
        if (string2.startsWith("znpcs:")) {
            String string5 = string.substring("znpcs:".length()).trim();
            if (!this.isPluginEnabled("ZNPCsPlus")) {
                return Optional.empty();
            }
            return Optional.of(new ZnpcsPlusNpcTarget(string5, "ZNPC " + string5));
        }
        if (string2.startsWith("znpcsplus:")) {
            String string6 = string.substring("znpcsplus:".length()).trim();
            if (!this.isPluginEnabled("ZNPCsPlus")) {
                return Optional.empty();
            }
            return Optional.of(new ZnpcsPlusNpcTarget(string6, "ZNPC " + string6));
        }
        if (string2.startsWith("fancynpc:")) {
            String string7 = string.substring("fancynpc:".length()).trim();
            if (!this.isPluginEnabled("FancyNpcs")) {
                return Optional.empty();
            }
            return Optional.of(new FancyNpcTarget(string7, "FancyNPC " + string7));
        }
        if (string2.startsWith("fancy:")) {
            String string8 = string.substring("fancy:".length()).trim();
            if (!this.isPluginEnabled("FancyNpcs")) {
                return Optional.empty();
            }
            return Optional.of(new FancyNpcTarget(string8, "FancyNPC " + string8));
        }
        if (string2.startsWith("shopkeeper:")) {
            String string9 = string.substring("shopkeeper:".length()).trim();
            if (!this.isPluginEnabled("Shopkeepers")) {
                return Optional.empty();
            }
            return Optional.of(new ShopkeeperTarget(string9, "Shopkeeper " + string9));
        }
        if (string2.startsWith("shopkeepers:")) {
            String string10 = string.substring("shopkeepers:".length()).trim();
            if (!this.isPluginEnabled("Shopkeepers")) {
                return Optional.empty();
            }
            return Optional.of(new ShopkeeperTarget(string10, "Shopkeeper " + string10));
        }
        if (!this.isPluginEnabled("Citizens")) {
            return Optional.empty();
        }
        return Optional.of(new CitizensNpcTarget(string, "NPC " + string));
    }

    public Optional<TargetLocator> resolveBlockInput(Player player, String string) {
        int n;
        if (!this.plugin.config().getBoolean("integrations.allow_block_targets", true)) {
            return Optional.empty();
        }
        if (player == null || string == null || string.isBlank()) {
            return Optional.empty();
        }
        BlockQuery blockQuery = this.parseBlockQuery(string);
        if (blockQuery == null) {
            return Optional.empty();
        }
        double d = Math.max(8.0, this.plugin.config().getDouble("integrations.block_search.radius", 64.0));
        Location location = this.findNearestBlock(player, blockQuery, d, n = this.plugin.config().getInt("integrations.block_search.vertical_range", 48));
        if (location == null) {
            return Optional.empty();
        }
        String string2 = this.buildBlockDisplayName(blockQuery, location);
        return Optional.of(new StaticLocationTarget(string2, location));
    }

    public Optional<TargetLocator> resolvePoi(Player player, String string) {
        if (!this.plugin.config().getBoolean("integrations.allow_poi_targets", true)) {
            return Optional.empty();
        }
        ConfigurationSection configurationSection = this.plugin.config().getConfigurationSection("poi.points");
        if (configurationSection == null || !configurationSection.isConfigurationSection(string)) {
            return Optional.empty();
        }
        ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection(string);
        if (configurationSection2 == null) {
            return Optional.empty();
        }
        if (!this.conditionEvaluator.checkAll(player, configurationSection2.getStringList("conditions"))) {
            return Optional.empty();
        }
        TargetLocator targetLocator = this.buildPoiTarget(player, string, configurationSection2);
        return Optional.ofNullable(targetLocator);
    }

    public Map<String, TargetLocator> getVisiblePois(Player player) {
        ConfigurationSection configurationSection = this.plugin.config().getConfigurationSection("poi.points");
        if (configurationSection == null) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, TargetLocator> linkedHashMap = new LinkedHashMap<String, TargetLocator>();
        for (String string : configurationSection.getKeys(false)) {
            this.resolvePoi(player, string).ifPresent(targetLocator -> linkedHashMap.put(string, (TargetLocator)targetLocator));
        }
        return linkedHashMap;
    }

    public Map<String, String> getVisiblePoiDisplayNames(Player player) {
        if (!this.plugin.config().getBoolean("integrations.allow_poi_targets", true)) {
            return Collections.emptyMap();
        }
        ConfigurationSection configurationSection = this.plugin.config().getConfigurationSection("poi.points");
        if (configurationSection == null) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (String string : configurationSection.getKeys(false)) {
            ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection(string);
            if (configurationSection2 == null || !this.conditionEvaluator.checkAll(player, configurationSection2.getStringList("conditions"))) continue;
            linkedHashMap.put(string, configurationSection2.getString("display_name", string));
        }
        return linkedHashMap;
    }

    public Set<String> getAvailablePoiKeys() {
        ConfigurationSection configurationSection = this.plugin.config().getConfigurationSection("poi.points");
        if (configurationSection == null) {
            return Collections.emptySet();
        }
        return configurationSection.getKeys(false);
    }

    public String getPoiDisplayName(String string) {
        if (string == null || string.isBlank()) {
            return "";
        }
        ConfigurationSection configurationSection = this.plugin.config().getConfigurationSection("poi.points");
        if (configurationSection == null || !configurationSection.isConfigurationSection(string)) {
            return string;
        }
        ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection(string);
        if (configurationSection2 == null) {
            return string;
        }
        return configurationSection2.getString("display_name", string);
    }

    public Optional<TargetLocator> resolveDescriptor(Player player, RouteDescriptor routeDescriptor) {
        Optional<TargetLocator> optional;
        switch (routeDescriptor.type()) {
            default: {
                throw new IncompatibleClassChangeError();
            }
            case PLAYER: {
                if (!this.isPlayerTargetsEnabled()) {
                    Optional<TargetLocator> optional2;
                    Optional<TargetLocator> optional3 = optional2 = Optional.empty();
                    return optional3;
                }
                String string = routeDescriptor.value();
                try {
                    Optional<TargetLocator> optional4;
                    UUID uUID = UUID.fromString(string);
                    Player player2 = Bukkit.getPlayer((UUID)uUID);
                    if (player2 == null) {
                        Optional<TargetLocator> optional5;
                        Optional<TargetLocator> optional6 = optional5 = Optional.empty();
                        return optional6;
                    }
                    Optional<TargetLocator> optional7 = optional4 = Optional.of(new PlayerTarget(player2.getUniqueId(), player2.getName()));
                    return optional7;
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    Optional<TargetLocator> optional8;
                    Player player3 = Bukkit.getPlayerExact((String)string);
                    if (player3 == null) {
                        Optional<TargetLocator> optional9;
                        Optional<TargetLocator> optional10 = optional9 = Optional.empty();
                        return optional10;
                    }
                    Optional<TargetLocator> optional11 = optional8 = Optional.of(new PlayerTarget(player3.getUniqueId(), player3.getName()));
                    return optional11;
                }
            }
            case MOB: {
                Optional<TargetLocator> optional12;
                Optional<TargetLocator> optional13 = optional12 = this.resolveMobInput(routeDescriptor.value());
                return optional13;
            }
            case NPC: {
                Optional<TargetLocator> optional14;
                Optional<TargetLocator> optional15 = optional14 = this.resolveNpcInput(routeDescriptor.value());
                return optional15;
            }
            case POI: {
                Optional<TargetLocator> optional16;
                Optional<TargetLocator> optional17 = optional16 = this.resolvePoi(player, routeDescriptor.value());
                return optional17;
            }
            case LOCATION: 
        }
        if (!this.isLocationTargetsEnabled()) {
            Optional<TargetLocator> optional18;
            Optional<TargetLocator> optional19 = optional18 = Optional.empty();
            return optional19;
        }
        Optional<TargetLocator> optional20 = optional = this.resolveLocationDescriptor(player, routeDescriptor.value());
        return optional20;
    }

    public boolean betonQuestEnabled() {
        return this.isPluginEnabled("BetonQuest") && this.plugin.config().getBoolean("betonquest.enabled", true);
    }

    public Map<String, AutoRoute> getAutoRoutes(Player player) {
        Object object22;
        LinkedHashMap<String, AutoRoute> linkedHashMap = new LinkedHashMap<String, AutoRoute>();
        for (Object object22 : this.externalRoutes.getOrDefault(player.getUniqueId(), Map.of()).values()) {
            if (((AutoRoute)object22).source().startsWith("betonquest") && !this.betonQuestEnabled() || !this.resolveDescriptor(player, ((AutoRoute)object22).descriptor()).isPresent()) continue;
            linkedHashMap.put(((AutoRoute)object22).key(), (AutoRoute)object22);
        }
        Map<String, QuestRouteService.StoredRoute> map = this.questRouteService.getRoutes(player.getUniqueId());
        for (Map.Entry object3 : map.entrySet()) {
            Object object = (QuestRouteService.StoredRoute)object3.getValue();
            if (!this.betonQuestEnabled() && ((QuestRouteService.StoredRoute)object).source().startsWith("betonquest") || this.resolveDescriptor(player, ((QuestRouteService.StoredRoute)object).descriptor()).isEmpty()) continue;
            linkedHashMap.put((String)object3.getKey(), new AutoRoute((String)object3.getKey(), ((QuestRouteService.StoredRoute)object).displayName(), ((QuestRouteService.StoredRoute)object).descriptor(), ((QuestRouteService.StoredRoute)object).mode(), ((QuestRouteService.StoredRoute)object).source()));
        }
        if (!this.betonQuestEnabled()) {
            return linkedHashMap;
        }
        object22 = this.plugin.config().getConfigurationSection("betonquest.auto_routes");
        if (object22 == null) {
            return linkedHashMap;
        }
        for (Object object : object22.getKeys(false)) {
            ConfigurationSection configurationSection = object22.getConfigurationSection((String)object);
            if (configurationSection == null || !this.conditionEvaluator.checkAll(player, configurationSection.getStringList("conditions"))) continue;
            String string = configurationSection.getString("type", "").toUpperCase();
            String string2 = configurationSection.getString("value", "");
            String string3 = this.questRouteNames.name(player, configurationSection, configurationSection.getString("display", (String)object));
            NavigationMode navigationMode = NavigationMode.fromConfig(configurationSection.getString("mode", this.plugin.getDefaultMode().name().toLowerCase()));
            Object object3 = configurationSection.getString("source", "betonquest-auto");
            try {
                Object object4;
                RouteDescriptor illegalArgumentException = new RouteDescriptor(RouteDescriptor.Type.valueOf(string), string2);
                if (this.resolveDescriptor(player, illegalArgumentException).isEmpty()) continue;
                String string4 = configurationSection.getString("quest", "").trim();
                Object object5 = object4 = string4.isEmpty() ? object : string4;
                if (!string4.isEmpty()) {
                    object3 = "betonquest-quest:" + string4;
                }
                linkedHashMap.putIfAbsent((String)object4, new AutoRoute((String)object4, string3, illegalArgumentException, navigationMode, (String)object3));
            }
            catch (IllegalArgumentException illegalArgumentException) {}
        }
        return linkedHashMap;
    }

    public void putDynamicQuestRoute(UUID uUID, String string, String string2, RouteDescriptor routeDescriptor, NavigationMode navigationMode, String string3) {
        this.questRouteService.putRoute(uUID, string, string2, routeDescriptor, navigationMode, string3);
    }

    public Optional<TargetLocator> resolveAutoRoute(Player player, AutoRoute autoRoute) {
        return this.resolveDescriptor(player, autoRoute.descriptor()).map(targetLocator -> new TargetLocator(){
            final /* synthetic */ TargetLocator val$target;
            final /* synthetic */ AutoRoute val$route;
            final /* synthetic */ IntegrationManager this$0;
            {
                this.val$target = targetLocator;
                this.val$route = autoRoute;
                this.this$0 = integrationManager;
            }

            @Override
            public String id() {
                return this.val$target.id();
            }

            @Override
            public String displayName() {
                return ChatColor.stripColor((String)Color.colorize(this.val$route.displayName()));
            }

            @Override
            public Location resolve(Player player) {
                return this.val$target.resolve(player);
            }
        });
    }

    public boolean removeDynamicQuestRoute(UUID uUID, String string) {
        return this.questRouteService.removeRoute(uUID, string);
    }

    public int clearDynamicQuestRoutes(UUID uUID) {
        return this.questRouteService.clearRoutes(uUID);
    }

    public Set<String> getDynamicQuestRouteKeys(UUID uUID) {
        return this.questRouteService.getRoutes(uUID).keySet();
    }

    private TargetLocator buildPoiTarget(Player player, String string, ConfigurationSection configurationSection) {
        String string2 = configurationSection.getString("display_name", string);
        if (configurationSection.getBoolean("spawn", false)) {
            return new WorldSpawnTarget(string2, configurationSection.getString("world"));
        }
        if (configurationSection.contains("world") && configurationSection.contains("x") && configurationSection.contains("y") && configurationSection.contains("z")) {
            String string3 = configurationSection.getString("world");
            if (Bukkit.getWorld((String)string3) == null) {
                return null;
            }
            Location location = new Location(Bukkit.getWorld((String)string3), configurationSection.getDouble("x"), configurationSection.getDouble("y"), configurationSection.getDouble("z"));
            return new StaticLocationTarget(string2, location);
        }
        if (configurationSection.contains("citizens_id")) {
            if (!this.isPluginEnabled("Citizens")) {
                return null;
            }
            return new CitizensNpcTarget(String.valueOf(configurationSection.getInt("citizens_id")), string2);
        }
        if (configurationSection.contains("citizens_name")) {
            if (!this.isPluginEnabled("Citizens")) {
                return null;
            }
            return new CitizensNpcTarget(configurationSection.getString("citizens_name"), string2);
        }
        if (configurationSection.contains("fancynpc")) {
            if (!this.isPluginEnabled("FancyNpcs")) {
                return null;
            }
            String string4 = configurationSection.getString("fancynpc", "").trim();
            if (string4.isEmpty()) {
                return null;
            }
            return new FancyNpcTarget(string4, string2);
        }
        if (configurationSection.contains("shopkeeper")) {
            if (!this.isPluginEnabled("Shopkeepers")) {
                return null;
            }
            String string5 = configurationSection.getString("shopkeeper", "").trim();
            if (string5.isEmpty()) {
                return null;
            }
            return new ShopkeeperTarget(string5, string2);
        }
        if (configurationSection.contains("mythic_mob")) {
            if (!this.isPluginEnabled("MythicMobs")) {
                return null;
            }
            return new MythicMobTarget(configurationSection.getString("mythic_mob"), 1000.0);
        }
        if (configurationSection.contains("elite_mob")) {
            return this.buildPresetHintTarget("elitemobs", configurationSection.getString("elite_mob"), string2, 1000.0);
        }
        if (configurationSection.contains("modelengine_model")) {
            return this.buildPresetHintTarget("modelengine", configurationSection.getString("modelengine_model"), string2, 1000.0);
        }
        if (configurationSection.contains("denizen_npc")) {
            if (!this.isPluginEnabled("Denizen") || !this.isPluginEnabled("Citizens")) {
                return null;
            }
            String string6 = configurationSection.getString("denizen_npc", "").trim();
            if (string6.isEmpty()) {
                return null;
            }
            return new CitizensNpcTarget(string6, string2);
        }
        if (configurationSection.contains("znpcs_id")) {
            if (!this.isPluginEnabled("ZNPCsPlus")) {
                return null;
            }
            String string7 = String.valueOf(configurationSection.get("znpcs_id", (Object)"")).trim();
            if (string7.isEmpty()) {
                return null;
            }
            return new ZnpcsPlusNpcTarget(string7, string2);
        }
        if (configurationSection.contains("znpcs_name")) {
            if (!this.isPluginEnabled("ZNPCsPlus")) {
                return null;
            }
            String string8 = configurationSection.getString("znpcs_name", "").trim();
            if (string8.isEmpty()) {
                return null;
            }
            return new ZnpcsPlusNpcTarget(string8, string2);
        }
        if (configurationSection.contains("ecoboss_mob")) {
            return this.buildPresetHintTarget("ecobosses", configurationSection.getString("ecoboss_mob"), string2, 1000.0);
        }
        if (configurationSection.contains("essentials_warp")) {
            return this.resolveLocationPoi(player, "warp:" + configurationSection.getString("essentials_warp"), string2);
        }
        if (configurationSection.contains("cmi_warp")) {
            return this.resolveLocationPoi(player, "cmiwarp:" + configurationSection.getString("cmi_warp"), string2);
        }
        if (configurationSection.contains("worldguard_region")) {
            String string9 = configurationSection.getString("worldguard_region", "").trim();
            String string10 = configurationSection.getString("worldguard_world", "").trim();
            String string11 = string10.isBlank() ? string9 : string10 + ":" + string9;
            return this.resolveLocationPoi(player, "wg:" + string11, string2);
        }
        if (configurationSection.contains("towny_town")) {
            return this.resolveLocationPoi(player, "town:" + configurationSection.getString("towny_town"), string2);
        }
        if (configurationSection.contains("decent_hologram")) {
            return this.resolveLocationPoi(player, "dh:" + configurationSection.getString("decent_hologram"), string2);
        }
        if (configurationSection.contains("vanilla_block")) {
            return this.resolveBlockPoi(player, "vanilla:" + configurationSection.getString("vanilla_block"), string2);
        }
        if (configurationSection.contains("itemsadder_block")) {
            return this.resolveBlockPoi(player, "itemsadder:" + configurationSection.getString("itemsadder_block"), string2);
        }
        if (configurationSection.contains("ia_block")) {
            return this.resolveBlockPoi(player, "ia:" + configurationSection.getString("ia_block"), string2);
        }
        if (configurationSection.contains("nexo_block")) {
            return this.resolveBlockPoi(player, "nexo:" + configurationSection.getString("nexo_block"), string2);
        }
        if (configurationSection.contains("oraxen_block")) {
            return this.resolveBlockPoi(player, "oraxen:" + configurationSection.getString("oraxen_block"), string2);
        }
        if (configurationSection.contains("entity_tag")) {
            String string12 = configurationSection.getString("entity_tag");
            if (string12 == null || string12.isBlank()) {
                return null;
            }
            return new HintedEntityTarget("poi-tag:" + string12, string2, "*", 1000.0, Collections.emptyList(), List.of(string12), Collections.emptyList(), (Plugin)this.plugin.owner());
        }
        if (configurationSection.contains("entity_metadata")) {
            String string13 = configurationSection.getString("entity_metadata");
            if (string13 == null || string13.isBlank()) {
                return null;
            }
            return new HintedEntityTarget("poi-meta:" + string13, string2, "*", 1000.0, List.of(string13), Collections.emptyList(), Collections.emptyList(), (Plugin)this.plugin.owner());
        }
        if (configurationSection.contains("entity_pdc")) {
            String string14 = configurationSection.getString("entity_pdc");
            if (string14 == null || string14.isBlank()) {
                return null;
            }
            return new HintedEntityTarget("poi-pdc:" + string14, string2, "*", 1000.0, Collections.emptyList(), Collections.emptyList(), List.of(string14), (Plugin)this.plugin.owner());
        }
        if (configurationSection.contains("entity_type")) {
            try {
                EntityType entityType = EntityType.valueOf((String)configurationSection.getString("entity_type", "").toUpperCase());
                return new NearestEntityTypeTarget(entityType, 1000.0);
            }
            catch (Exception exception) {
                return null;
            }
        }
        if (configurationSection.contains("entity_name")) {
            return new NamedEntityTarget(configurationSection.getString("entity_name"), 1000.0);
        }
        return null;
    }

    private TargetLocator resolveBlockPoi(Player player, String string, String string2) {
        Optional<TargetLocator> optional = this.resolveBlockInput(player, string);
        if (optional.isEmpty()) {
            return null;
        }
        Location location = optional.get().resolve(player);
        if (location == null) {
            return null;
        }
        return new StaticLocationTarget(string2, location);
    }

    private TargetLocator resolveLocationPoi(Player player, String string, String string2) {
        Optional<TargetLocator> optional = this.resolveLocationInput(player, string);
        if (optional.isEmpty()) {
            return null;
        }
        Location location = optional.get().resolve(player);
        if (location == null) {
            return null;
        }
        return new StaticLocationTarget(string2, location);
    }

    private boolean isPluginEnabled(String string) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(string);
        return plugin != null && plugin.isEnabled();
    }

    private Optional<TargetLocator> resolvePresetHintMob(String string, String string2) {
        TargetLocator targetLocator = this.buildPresetHintTarget(string, string2, string + " " + string2, 1000.0);
        return Optional.ofNullable(targetLocator);
    }

    private TargetLocator buildPresetHintTarget(String string, String string2, String string3, double d) {
        ConfigurationSection configurationSection = this.plugin.config().getConfigurationSection("integrations.hints." + string);
        if (configurationSection == null) {
            return null;
        }
        String string4 = configurationSection.getString("plugin");
        if (string4 != null && !string4.isBlank() && !this.isPluginEnabled(string4)) {
            return null;
        }
        String string5 = string2 == null || string2.isBlank() ? "*" : string2.trim();
        return new HintedEntityTarget(string + ":" + string5, string3, string5, d, configurationSection.getStringList("metadata_keys"), configurationSection.getStringList("scoreboard_tags"), configurationSection.getStringList("pdc_keys"), (Plugin)this.plugin.owner());
    }

    private Optional<TargetLocator> resolveWarpAlias(String string) {
        Optional<TargetLocator> optional = this.resolveEssentialsWarpTarget(string);
        if (optional.isPresent()) {
            return optional;
        }
        return this.resolveCmiWarpTarget(string);
    }

    private Optional<TargetLocator> resolveEssentialsWarpTarget(String string) {
        if (!this.plugin.config().getBoolean("integrations.allow_warp_targets", true)) {
            return Optional.empty();
        }
        if (string == null || string.isBlank()) {
            return Optional.empty();
        }
        if (!this.isPluginEnabled("Essentials")) {
            return Optional.empty();
        }
        String string2 = string.trim();
        try {
            Location location;
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if (plugin == null) {
                return Optional.empty();
            }
            Method method = this.findFirstMethod(plugin.getClass(), "getWarps");
            if (method == null) {
                return Optional.empty();
            }
            Object object = method.invoke((Object)plugin, new Object[0]);
            if (object == null) {
                return Optional.empty();
            }
            Method method2 = this.findFirstMethod(object.getClass(), new Class[]{String.class}, "getWarp", "getWarpLocation", "get");
            if (method2 == null) {
                return Optional.empty();
            }
            Object object2 = method2.invoke(object, string2);
            if (!(object2 instanceof Location) || (location = (Location)object2).getWorld() == null) {
                return Optional.empty();
            }
            return Optional.of(new StaticLocationTarget("Warp " + string2, location));
        }
        catch (Exception exception) {
            return Optional.empty();
        }
    }

    private Optional<TargetLocator> resolveCmiWarpTarget(String string) {
        if (!this.plugin.config().getBoolean("integrations.allow_warp_targets", true)) {
            return Optional.empty();
        }
        if (!this.plugin.config().getBoolean("integrations.allow_cmi_warp_targets", true)) {
            return Optional.empty();
        }
        if (string == null || string.isBlank()) {
            return Optional.empty();
        }
        if (!this.isPluginEnabled("CMI")) {
            return Optional.empty();
        }
        Object object = this.getCmiWarpManager();
        if (object == null) {
            return Optional.empty();
        }
        String string2 = string.trim();
        Object object2 = this.findCmiWarp(object, string2);
        if (object2 == null) {
            return Optional.empty();
        }
        Location location = this.extractFlexibleLocation(object2);
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.of(new StaticLocationTarget("Warp " + string2, location));
    }

    private Object getCmiWarpManager() {
        try {
            Class<?> clazz = Class.forName("com.Zrips.CMI.CMI");
            Method method = this.findFirstMethod(clazz, "getInstance");
            if (method == null) {
                return null;
            }
            Object object = method.invoke(null, new Object[0]);
            if (object == null) {
                return null;
            }
            return this.invokeNoArg(object, "getWarpManager", "getWarpsManager", "getCMIWarpManager");
        }
        catch (Exception exception) {
            return null;
        }
    }

    private Object findCmiWarp(Object object, String string) {
        Object object2;
        Object object3;
        if (object == null || string == null || string.isBlank()) {
            return null;
        }
        Method method = this.findFirstMethod(object.getClass(), new Class[]{String.class}, "getWarp", "getByName", "get");
        if (method != null) {
            try {
                object3 = method.invoke(object, string);
                if (object3 != null) {
                    return object3;
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if ((object2 = this.invokeNoArg(object, "getWarps", "getWarpMap", "getWarpsMap", "getAllWarps")) instanceof Map) {
            object3 = (Map)object2;
            Object v = object3.get(string);
            if (v != null) {
                return v;
            }
            for (Map.Entry entry : object3.entrySet()) {
                if (entry.getKey() != null && String.valueOf(entry.getKey()).equalsIgnoreCase(string)) {
                    return entry.getValue();
                }
                if (!this.matchesNamedValue(entry.getValue(), string)) continue;
                return entry.getValue();
            }
            return null;
        }
        if (object2 instanceof Iterable) {
            object3 = (Iterable)object2;
            Iterator iterator = object3.iterator();
            while (iterator.hasNext()) {
                Object t = iterator.next();
                if (!this.matchesNamedValue(t, string)) continue;
                return t;
            }
        }
        return null;
    }

    private boolean matchesNamedValue(Object object, String string) {
        if (object == null || string == null || string.isBlank()) {
            return false;
        }
        Object object2 = this.invokeNoArg(object, "getName", "name", "getWarpName");
        return object2 != null && String.valueOf(object2).equalsIgnoreCase(string);
    }

    private Optional<TargetLocator> resolveTownyTownTarget(String string) {
        if (!this.plugin.config().getBoolean("integrations.allow_town_targets", true)) {
            return Optional.empty();
        }
        if (string == null || string.isBlank()) {
            return Optional.empty();
        }
        if (!this.isPluginEnabled("Towny")) {
            return Optional.empty();
        }
        String string2 = string.trim();
        try {
            Method method;
            Class<?> clazz = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object object = clazz.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            if (object == null) {
                return Optional.empty();
            }
            Method method2 = this.findFirstMethod(object.getClass(), new Class[]{String.class}, "getTown", "getTownOrNull");
            if (method2 == null) {
                return Optional.empty();
            }
            Object object2 = method2.invoke(object, string2);
            if (object2 == null) {
                return Optional.empty();
            }
            Location location = null;
            try {
                location = this.extractFlexibleLocation(this.invokeNoArg(object2, "getSpawnOrNull", "getSpawnLocation"));
            }
            catch (Exception exception) {
                // empty catch block
            }
            if (location == null && (method = this.findFirstMethod(object2.getClass(), "getSpawn")) != null) {
                try {
                    location = this.extractFlexibleLocation(method.invoke(object2, new Object[0]));
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            if (location == null) {
                location = this.extractFlexibleLocation(this.invokeNoArg(object2, "getHomeBlock", "getHomeBlockOrNull"));
            }
            if (location == null || location.getWorld() == null) {
                return Optional.empty();
            }
            return Optional.of(new StaticLocationTarget("Town " + string2, location));
        }
        catch (Exception exception) {
            return Optional.empty();
        }
    }

    private Optional<TargetLocator> resolveDecentHologramTarget(String string) {
        if (!this.plugin.config().getBoolean("integrations.allow_hologram_targets", true)) {
            return Optional.empty();
        }
        if (string == null || string.isBlank()) {
            return Optional.empty();
        }
        if (!this.isPluginEnabled("DecentHolograms")) {
            return Optional.empty();
        }
        String string2 = string.trim();
        try {
            Class<?> clazz = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            Method method = this.findFirstMethod(clazz, new Class[]{String.class}, "getHologram");
            if (method == null) {
                return Optional.empty();
            }
            Object object = method.invoke(null, string2);
            if (object == null) {
                return Optional.empty();
            }
            Location location = this.extractFlexibleLocation(object);
            if (location == null || location.getWorld() == null) {
                return Optional.empty();
            }
            return Optional.of(new StaticLocationTarget("Hologram " + string2, location));
        }
        catch (Exception exception) {
            return Optional.empty();
        }
    }

    private Optional<TargetLocator> resolveWorldGuardRegionTarget(Player player, String string) {
        if (!this.plugin.config().getBoolean("integrations.allow_region_targets", true)) {
            return Optional.empty();
        }
        if (player == null || string == null || string.isBlank()) {
            return Optional.empty();
        }
        if (!this.isPluginEnabled("WorldGuard")) {
            return Optional.empty();
        }
        String string2 = player.getWorld().getName();
        String string3 = string.trim();
        int n = string3.indexOf(58);
        if (n > 0) {
            string2 = string3.substring(0, n).trim();
            string3 = string3.substring(n + 1).trim();
        }
        if (string2.isBlank() || string3.isBlank()) {
            return Optional.empty();
        }
        World world = Bukkit.getWorld((String)string2);
        if (world == null) {
            return Optional.empty();
        }
        Location location = this.fetchWorldGuardRegionCenter(world, string3);
        if (location == null) {
            return Optional.empty();
        }
        return Optional.of(new StaticLocationTarget("Region " + string3, location));
    }

    private Location fetchWorldGuardRegionCenter(World world, String string) {
        Object object = this.getWorldGuardRegionManager(world);
        if (object == null) {
            return null;
        }
        Object object2 = this.findWorldGuardRegion(object, string);
        if (object2 == null) {
            return null;
        }
        Object object3 = this.invokeNoArg(object2, "getMinimumPoint", "getMin");
        Object object4 = this.invokeNoArg(object2, "getMaximumPoint", "getMax");
        if (object3 == null || object4 == null) {
            return null;
        }
        Double d = this.readVectorComponent(object3, 'x');
        Double d2 = this.readVectorComponent(object3, 'y');
        Double d3 = this.readVectorComponent(object3, 'z');
        Double d4 = this.readVectorComponent(object4, 'x');
        Double d5 = this.readVectorComponent(object4, 'y');
        Double d6 = this.readVectorComponent(object4, 'z');
        if (d == null || d2 == null || d3 == null || d4 == null || d5 == null || d6 == null) {
            return null;
        }
        double d7 = (d + d4) / 2.0 + 0.5;
        double d8 = (d2 + d5) / 2.0;
        double d9 = (d3 + d6) / 2.0 + 0.5;
        return new Location(world, d7, d8, d9);
    }

    private List<String> fetchWorldGuardRegionNames(World world) {
        Object object = this.getWorldGuardRegionManager(world);
        if (object == null) {
            return Collections.emptyList();
        }
        Object object2 = this.invokeNoArg(object, "getRegions");
        if (!(object2 instanceof Map)) {
            return Collections.emptyList();
        }
        Map map = (Map)object2;
        ArrayList<String> arrayList = new ArrayList<String>();
        for (Object k : map.keySet()) {
            if (k == null) continue;
            arrayList.add(String.valueOf(k));
        }
        return arrayList;
    }

    private Object getWorldGuardRegionManager(World world) {
        if (world == null) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object object = clazz.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            Object object2 = object.getClass().getMethod("getPlatform", new Class[0]).invoke(object, new Object[0]);
            Object object3 = object2.getClass().getMethod("getRegionContainer", new Class[0]).invoke(object2, new Object[0]);
            Class<?> clazz2 = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object object4 = clazz2.getMethod("adapt", World.class).invoke(null, world);
            if (object4 == null) {
                return null;
            }
            Class<?> clazz3 = Class.forName("com.sk89q.worldedit.world.World");
            Method method = object3.getClass().getMethod("get", clazz3);
            return method.invoke(object3, object4);
        }
        catch (Exception exception) {
            return null;
        }
    }

    private Object findWorldGuardRegion(Object object, String string) {
        Object object2;
        Object object3;
        if (object == null || string == null || string.isBlank()) {
            return null;
        }
        String string2 = string.trim();
        try {
            object3 = this.findFirstMethod(object.getClass(), new Class[]{String.class}, "getRegion", "get");
            if (object3 != null && (object2 = ((Method)object3).invoke(object, string2)) != null) {
                return object2;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        object2 = this.invokeNoArg(object, "getRegions");
        if (!(object2 instanceof Map)) {
            return null;
        }
        object3 = (Map)object2;
        Object v = object3.get(string2);
        if (v != null) {
            return v;
        }
        for (Map.Entry entry : object3.entrySet()) {
            if (entry.getKey() == null || !String.valueOf(entry.getKey()).equalsIgnoreCase(string2)) continue;
            return entry.getValue();
        }
        return null;
    }

    private Object invokeNoArg(Object object, String ... stringArray) {
        if (object == null) {
            return null;
        }
        Method method = this.findFirstMethod(object.getClass(), stringArray);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(object, new Object[0]);
        }
        catch (Exception exception) {
            return null;
        }
    }

    private Double readVectorComponent(Object object, char c) {
        String[] stringArray;
        switch (c) {
            case 'x': {
                String[] stringArray2 = new String[3];
                stringArray2[0] = "getX";
                stringArray2[1] = "x";
                stringArray = stringArray2;
                stringArray2[2] = "getBlockX";
                break;
            }
            case 'y': {
                String[] stringArray2 = new String[3];
                stringArray2[0] = "getY";
                stringArray2[1] = "y";
                stringArray = stringArray2;
                stringArray2[2] = "getBlockY";
                break;
            }
            case 'z': {
                String[] stringArray2 = new String[3];
                stringArray2[0] = "getZ";
                stringArray2[1] = "z";
                stringArray = stringArray2;
                stringArray2[2] = "getBlockZ";
                break;
            }
            default: {
                stringArray = new String[]{};
            }
        }
        String[] stringArray3 = stringArray;
        for (String string : stringArray) {
            try {
                Method method = object.getClass().getMethod(string, new Class[0]);
                Object object2 = method.invoke(object, new Object[0]);
                if (!(object2 instanceof Number)) continue;
                Number number = (Number)object2;
                return number.doubleValue();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return null;
    }

    private boolean containsIgnoreCase(List<String> list, String string) {
        if (string == null) {
            return false;
        }
        for (String string2 : list) {
            if (!string.equalsIgnoreCase(string2)) continue;
            return true;
        }
        return false;
    }

    private void collectNamesFromValue(Object object, List<String> list) {
        if (object == null || list == null) {
            return;
        }
        if (object instanceof Map) {
            Map map = (Map)object;
            for (Map.Entry entry : map.entrySet()) {
                String string;
                if (entry.getKey() != null && !(string = String.valueOf(entry.getKey()).trim()).isEmpty() && !this.containsIgnoreCase(list, string)) {
                    list.add(string);
                }
                this.addNameFromObject(entry.getValue(), list);
            }
            return;
        }
        if (object instanceof Iterable) {
            Iterable iterable = (Iterable)object;
            for (Object t : iterable) {
                this.addNameFromObject(t, list);
            }
            return;
        }
        this.addNameFromObject(object, list);
    }

    private void addNameFromObject(Object object, List<String> list) {
        String string;
        if (object == null || list == null) {
            return;
        }
        if (object instanceof String) {
            String string2 = (String)object;
            String string3 = string2.trim();
            if (!string3.isEmpty() && !this.containsIgnoreCase(list, string3)) {
                list.add(string3);
            }
            return;
        }
        Object object2 = this.invokeNoArg(object, "getName", "name", "getWarpName");
        if (object2 != null && !(string = String.valueOf(object2).trim()).isEmpty() && !this.containsIgnoreCase(list, string)) {
            list.add(string);
            return;
        }
        String string4 = String.valueOf(object).trim();
        if (!(string4.isEmpty() || string4.contains("@") || this.containsIgnoreCase(list, string4))) {
            list.add(string4);
        }
    }

    private Location extractFlexibleLocation(Object object) {
        String string;
        World world;
        Location location;
        if (object == null) {
            return null;
        }
        if (object instanceof Location) {
            Location location2 = (Location)object;
            return location2;
        }
        Object object2 = this.invokeNoArg(object, "getLocation", "getLoc", "toLocation", "getBukkitLoc", "getBukkitLocation");
        if (object2 instanceof Location) {
            Location location3 = (Location)object2;
            return location3;
        }
        if (object2 != null && object2 != object && (location = this.extractFlexibleLocation(object2)) != null) {
            return location;
        }
        Object object3 = this.invokeNoArg(object, "getWorld");
        World world2 = object3 instanceof World ? (world = (World)object3) : null;
        World world3 = world2;
        if (world2 == null && object3 != null) {
            world2 = Bukkit.getWorld((String)String.valueOf(object3));
        }
        if (world2 == null && (string = this.stringValue(this.invokeNoArg(object, "getWorldName", "getName"))) != null) {
            world2 = Bukkit.getWorld((String)string);
        }
        if (world2 == null) {
            return null;
        }
        Double d = this.numericValue(this.invokeNoArg(object, "getX", "getBlockX", "x"));
        Double d2 = this.numericValue(this.invokeNoArg(object, "getY", "getBlockY", "y"));
        Double d3 = this.numericValue(this.invokeNoArg(object, "getZ", "getBlockZ", "z"));
        if (d == null || d2 == null || d3 == null) {
            return null;
        }
        return new Location(world2, d.doubleValue(), d2.doubleValue(), d3.doubleValue());
    }

    private Double numericValue(Object object) {
        if (object instanceof Number) {
            Number number = (Number)object;
            return number.doubleValue();
        }
        if (object instanceof String) {
            String string = (String)object;
            try {
                return Double.parseDouble(string);
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        return null;
    }

    private String stringValue(Object object) {
        if (object == null) {
            return null;
        }
        String string = String.valueOf(object).trim();
        return string.isEmpty() ? null : string;
    }

    private BlockQuery parseBlockQuery(String string) {
        String string2 = string == null ? "" : string.trim();
        String string3 = string2;
        if (string2.isEmpty()) {
            return null;
        }
        String string4 = string2.toLowerCase(Locale.ROOT);
        if (string4.startsWith("itemsadder:")) {
            return new BlockQuery(BlockProvider.ITEMSADDER, string2.substring("itemsadder:".length()).trim(), null);
        }
        if (string4.startsWith("ia:")) {
            return new BlockQuery(BlockProvider.ITEMSADDER, string2.substring("ia:".length()).trim(), null);
        }
        if (string4.startsWith("nexo:")) {
            return new BlockQuery(BlockProvider.NEXO, string2.substring("nexo:".length()).trim(), null);
        }
        if (string4.startsWith("oraxen:")) {
            return new BlockQuery(BlockProvider.ORAXEN, string2.substring("oraxen:".length()).trim(), null);
        }
        if (string4.startsWith("minecraft:")) {
            Material material = this.parseMaterial(string2.substring("minecraft:".length()));
            return material == null ? null : new BlockQuery(BlockProvider.VANILLA, material.name().toLowerCase(Locale.ROOT), material);
        }
        if (string4.startsWith("vanilla:")) {
            Material material = this.parseMaterial(string2.substring("vanilla:".length()));
            return material == null ? null : new BlockQuery(BlockProvider.VANILLA, material.name().toLowerCase(Locale.ROOT), material);
        }
        if (string4.startsWith("block:")) {
            Material material = this.parseMaterial(string2.substring("block:".length()));
            return material == null ? null : new BlockQuery(BlockProvider.VANILLA, material.name().toLowerCase(Locale.ROOT), material);
        }
        Material material = this.parseMaterial(string2);
        if (material != null) {
            return new BlockQuery(BlockProvider.VANILLA, material.name().toLowerCase(Locale.ROOT), material);
        }
        if (string2.contains(":")) {
            return new BlockQuery(BlockProvider.ANY_CUSTOM, string2, null);
        }
        return null;
    }

    private String buildBlockDisplayName(BlockQuery blockQuery, Location location) {
        String string = switch (blockQuery.provider().ordinal()) {
            default -> throw new IncompatibleClassChangeError();
            case 0 -> "Block";
            case 1 -> "ItemsAdder";
            case 2 -> "Nexo";
            case 3 -> "Oraxen";
            case 4 -> "Custom block";
        };
        String string2 = blockQuery.provider() == BlockProvider.VANILLA && blockQuery.material() != null ? blockQuery.material().name().toLowerCase(Locale.ROOT) : blockQuery.query();
        return string + " " + string2 + " @ " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
    }

    private Location findNearestBlock(Player player, BlockQuery blockQuery, double d, int n) {
        int n2;
        World world = player.getWorld();
        Location location = player.getLocation();
        double d2 = d * d;
        int n3 = (int)Math.floor(location.getX() - d);
        int n4 = (int)Math.ceil(location.getX() + d);
        int n5 = (int)Math.floor(location.getZ() - d);
        int n6 = (int)Math.ceil(location.getZ() + d);
        int n7 = world.getMinHeight();
        int n8 = world.getMaxHeight() - 1;
        int n9 = n7;
        int n10 = n8;
        if (n >= 0) {
            n2 = location.getBlockY();
            n9 = Math.max(n7, n2 - n);
            n10 = Math.min(n8, n2 + n);
        }
        n2 = Math.floorDiv(n3, 16);
        int n11 = Math.floorDiv(n4, 16);
        int n12 = Math.floorDiv(n5, 16);
        int n13 = Math.floorDiv(n6, 16);
        Location location2 = null;
        double d3 = Double.MAX_VALUE;
        for (int i = n2; i <= n11; ++i) {
            for (int j = n12; j <= n13; ++j) {
                if (!world.isChunkLoaded(i, j)) continue;
                int n14 = i << 4;
                int n15 = n14 + 15;
                int n16 = j << 4;
                int n17 = n16 + 15;
                int n18 = Math.max(n3, n14);
                int n19 = Math.min(n4, n15);
                int n20 = Math.max(n5, n16);
                int n21 = Math.min(n6, n17);
                for (int k = n18; k <= n19; ++k) {
                    for (int i2 = n20; i2 <= n21; ++i2) {
                        double d4;
                        double d5 = (double)k + 0.5 - location.getX();
                        double d6 = d5 * d5 + (d4 = (double)i2 + 0.5 - location.getZ()) * d4;
                        if (d6 > d2) continue;
                        for (int i3 = n9; i3 <= n10; ++i3) {
                            double d7;
                            double d8;
                            Block block = world.getBlockAt(k, i3, i2);
                            if (block.getType().isAir() || !this.matchesBlock(block, blockQuery) || (d8 = d6 + (d7 = (double)i3 + 0.5 - location.getY()) * d7) > d2 || d8 >= d3) continue;
                            d3 = d8;
                            location2 = block.getLocation().add(0.5, 0.5, 0.5);
                        }
                    }
                }
            }
        }
        return location2;
    }

    private boolean matchesBlock(Block block, BlockQuery blockQuery) {
        return switch (blockQuery.provider().ordinal()) {
            default -> throw new IncompatibleClassChangeError();
            case 0 -> {
                if (blockQuery.material() != null && block.getType() == blockQuery.material()) {
                    yield true;
                }
                yield false;
            }
            case 1 -> this.matchesIdentifier(this.resolveItemsAdderBlockId(block), blockQuery.query());
            case 2 -> this.matchesIdentifier(this.resolveNexoBlockId(block), blockQuery.query());
            case 3 -> this.matchesIdentifier(this.resolveOraxenBlockId(block), blockQuery.query());
            case 4 -> this.matchesIdentifier(this.resolveItemsAdderBlockId(block), blockQuery.query()) || this.matchesIdentifier(this.resolveNexoBlockId(block), blockQuery.query()) || this.matchesIdentifier(this.resolveOraxenBlockId(block), blockQuery.query());
        };
    }

    private boolean matchesIdentifier(String string, String string2) {
        if (string == null || string.isBlank()) {
            return false;
        }
        String string3 = string.trim().toLowerCase(Locale.ROOT);
        String string4 = string2 == null ? "" : string2.trim().toLowerCase(Locale.ROOT);
        String string5 = string4;
        if (string4.isEmpty() || string4.equals("*")) {
            return true;
        }
        if (string4.contains("*")) {
            String string6 = "\\Q" + string4.replace("*", "\\E.*\\Q") + "\\E";
            return string3.matches(string6);
        }
        return string3.equals(string4) || string3.endsWith(":" + string4);
    }

    private Material parseMaterial(String string) {
        if (string == null || string.isBlank()) {
            return null;
        }
        String string2 = string.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return Material.valueOf((String)string2);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private String resolveItemsAdderBlockId(Block block) {
        if (!this.isPluginEnabled("ItemsAdder")) {
            return null;
        }
        if (!this.initItemsAdderApi()) {
            return null;
        }
        try {
            Object object = this.itemsAdderByPlacedMethod.invoke(null, block);
            if (object == null) {
                return null;
            }
            return this.normalizeId(this.itemsAdderIdMethod.invoke(object, new Object[0]));
        }
        catch (Exception exception) {
            return null;
        }
    }

    private boolean initItemsAdderApi() {
        if (this.itemsAdderApiChecked) {
            return this.itemsAdderByPlacedMethod != null && this.itemsAdderIdMethod != null;
        }
        this.itemsAdderApiChecked = true;
        try {
            Class<?> clazz = Class.forName("dev.lone.itemsadder.api.CustomBlock");
            this.itemsAdderByPlacedMethod = this.findFirstMethod(clazz, new Class[]{Block.class}, "byAlreadyPlaced");
            this.itemsAdderIdMethod = this.findFirstMethod(clazz, "getNamespacedID", "getNamespacedId", "getId");
        }
        catch (Exception exception) {
            this.itemsAdderByPlacedMethod = null;
            this.itemsAdderIdMethod = null;
        }
        return this.itemsAdderByPlacedMethod != null && this.itemsAdderIdMethod != null;
    }

    private String resolveNexoBlockId(Block block) {
        if (!this.isPluginEnabled("Nexo")) {
            return null;
        }
        if (!this.initNexoApi()) {
            return null;
        }
        try {
            Object object = this.nexoCustomBlockMethod.invoke(null, block);
            if (object == null || this.nexoMechanicIdMethod == null) {
                return null;
            }
            return this.normalizeId(this.nexoMechanicIdMethod.invoke(object, new Object[0]));
        }
        catch (Exception exception) {
            return null;
        }
    }

    private boolean initNexoApi() {
        if (this.nexoApiChecked) {
            return this.nexoCustomBlockMethod != null && this.nexoMechanicIdMethod != null;
        }
        this.nexoApiChecked = true;
        try {
            Class<?> clazz = Class.forName("com.nexomc.nexo.api.NexoBlocks");
            this.nexoCustomBlockMethod = this.findFirstMethod(clazz, new Class[]{Block.class}, "customBlockMechanic");
            if (this.nexoCustomBlockMethod != null) {
                Class<?> clazz2 = this.nexoCustomBlockMethod.getReturnType();
                this.nexoMechanicIdMethod = this.findFirstMethod(clazz2, "getItemID", "getItemId", "getID", "getId");
            }
        }
        catch (Exception exception) {
            this.nexoCustomBlockMethod = null;
            this.nexoMechanicIdMethod = null;
        }
        return this.nexoCustomBlockMethod != null && this.nexoMechanicIdMethod != null;
    }

    private String resolveOraxenBlockId(Block block) {
        if (!this.isPluginEnabled("Oraxen")) {
            return null;
        }
        if (!this.initOraxenApi()) {
            return null;
        }
        try {
            String string;
            Boolean bl;
            Object object;
            if (this.oraxenIsBlockMethod != null && (object = this.invokeByBlockOrLocation(this.oraxenIsBlockMethod, block)) instanceof Boolean && !(bl = (Boolean)object).booleanValue()) {
                return null;
            }
            if (this.oraxenDirectIdMethod != null && (string = this.normalizeId(this.invokeByBlockOrLocation(this.oraxenDirectIdMethod, block))) != null) {
                return string;
            }
            if (this.oraxenMechanicMethod != null) {
                Object object2 = this.invokeByBlockOrLocation(this.oraxenMechanicMethod, block);
                if (object2 == null) {
                    return null;
                }
                if (this.oraxenMechanicIdMethod == null || !this.oraxenMechanicIdMethod.getDeclaringClass().isInstance(object2)) {
                    this.oraxenMechanicIdMethod = this.findFirstMethod(object2.getClass(), "getItemID", "getItemId", "getID", "getId", "getModelName", "getModel");
                }
                if (this.oraxenMechanicIdMethod != null) {
                    return this.normalizeId(this.oraxenMechanicIdMethod.invoke(object2, new Object[0]));
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
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
            this.oraxenIsBlockMethod = this.findFirstMethod(this.oraxenBlocksClass, new Class[]{Block.class}, "isOraxenBlock", "isCustomBlock", "isOraxenNoteBlock", "isOraxenStringBlock");
            this.oraxenDirectIdMethod = this.findFirstMethod(this.oraxenBlocksClass, new Class[]{Block.class}, "getBlockID", "getBlockId", "getIdByBlock", "getIDByBlock", "getId");
            if (this.oraxenDirectIdMethod == null) {
                this.oraxenDirectIdMethod = this.findFirstMethod(this.oraxenBlocksClass, new Class[]{Location.class}, "getBlockID", "getBlockId", "getIdByLocation", "getIDByLocation", "getId");
            }
            this.oraxenMechanicMethod = this.findFirstMethod(this.oraxenBlocksClass, new Class[]{Block.class}, "getNoteBlockMechanic", "getStringBlockMechanic", "getBlockMechanic", "getMechanic", "getOraxenBlock");
            if (this.oraxenMechanicMethod == null) {
                this.oraxenMechanicMethod = this.findFirstMethod(this.oraxenBlocksClass, new Class[]{Location.class}, "getNoteBlockMechanic", "getStringBlockMechanic", "getBlockMechanic", "getMechanic", "getOraxenBlock");
            }
        }
        catch (Exception exception) {
            this.oraxenBlocksClass = null;
            this.oraxenIsBlockMethod = null;
            this.oraxenDirectIdMethod = null;
            this.oraxenMechanicMethod = null;
        }
        return this.oraxenBlocksClass != null;
    }

    private Object invokeByBlockOrLocation(Method method, Block block) throws Exception {
        Class<?>[] classArray = method.getParameterTypes();
        if (classArray.length != 1) {
            return null;
        }
        if (Block.class.isAssignableFrom(classArray[0])) {
            return method.invoke(null, block);
        }
        if (Location.class.isAssignableFrom(classArray[0])) {
            return method.invoke(null, block.getLocation());
        }
        return null;
    }

    private Method findFirstMethod(Class<?> clazz, String ... stringArray) {
        if (clazz == null) {
            return null;
        }
        for (String string : stringArray) {
            try {
                return clazz.getMethod(string, new Class[0]);
            }
            catch (NoSuchMethodException noSuchMethodException) {
            }
        }
        return null;
    }

    private Method findFirstMethod(Class<?> clazz, Class<?>[] classArray, String ... stringArray) {
        if (clazz == null) {
            return null;
        }
        for (String string : stringArray) {
            try {
                return clazz.getMethod(string, classArray);
            }
            catch (NoSuchMethodException noSuchMethodException) {
            }
        }
        return null;
    }

    private String normalizeId(Object object) {
        if (object == null) {
            return null;
        }
        String string = String.valueOf(object).trim();
        if (string.isEmpty() || "null".equalsIgnoreCase(string)) {
            return null;
        }
        return string;
    }

    private Optional<TargetLocator> resolveLocationDescriptor(Player player, String string) {
        String string2 = string == null ? "" : string.trim();
        String string3 = string2;
        if (string2.isEmpty()) {
            return Optional.empty();
        }
        Optional<TargetLocator> optional = this.resolveLocationInput(player, string2);
        if (optional.isPresent()) {
            return optional;
        }
        String[] stringArray = string2.split("[,;\\s]+");
        if (stringArray.length < 3) {
            return Optional.empty();
        }
        String string4 = null;
        Double d = null;
        Double d2 = null;
        Double d3 = null;
        if (stringArray.length >= 4 && Bukkit.getWorld((String)stringArray[0]) != null) {
            string4 = stringArray[0];
            d = this.parseDouble(stringArray[1]);
            d2 = this.parseDouble(stringArray[2]);
            d3 = this.parseDouble(stringArray[3]);
        } else if (stringArray.length >= 4 && Bukkit.getWorld((String)stringArray[3]) != null) {
            string4 = stringArray[3];
            d = this.parseDouble(stringArray[0]);
            d2 = this.parseDouble(stringArray[1]);
            d3 = this.parseDouble(stringArray[2]);
        } else if (stringArray.length >= 3) {
            string4 = player.getWorld().getName();
            d = this.parseDouble(stringArray[0]);
            d2 = this.parseDouble(stringArray[1]);
            d3 = this.parseDouble(stringArray[2]);
        }
        if (string4 == null || d == null || d2 == null || d3 == null || Bukkit.getWorld((String)string4) == null) {
            return Optional.empty();
        }
        return Optional.of(new StaticLocationTarget("Location " + d.intValue() + " " + d2.intValue() + " " + d3.intValue(), new Location(Bukkit.getWorld((String)string4), d.doubleValue(), d2.doubleValue(), d3.doubleValue())));
    }

    private Double parseDouble(String string) {
        try {
            return Double.parseDouble(string);
        }
        catch (NumberFormatException numberFormatException) {
            return null;
        }
    }

    public record AutoRoute(String key, String displayName, RouteDescriptor descriptor, NavigationMode mode, String source) {
    }

    private record BlockQuery(BlockProvider provider, String query, Material material) {
    }

    private static enum BlockProvider {
        VANILLA,
        ITEMSADDER,
        NEXO,
        ORAXEN,
        ANY_CUSTOM;

    }
}

