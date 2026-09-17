/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.BetonQuest
 *  org.betonquest.betonquest.api.BetonQuestApi
 *  org.betonquest.betonquest.api.config.quest.QuestPackage
 *  org.betonquest.betonquest.api.identifier.ConditionIdentifier
 *  org.betonquest.betonquest.api.profile.OnlineProfile
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.reload.ReloadPhase
 *  org.betonquest.betonquest.api.service.condition.ConditionManager
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.minimap.quest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.identifier.ConditionIdentifier;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.reload.ReloadPhase;
import org.betonquest.betonquest.api.service.condition.ConditionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.api.event.GpsNavigationStartEvent;
import sk.enorials.gps.api.event.GpsNavigationStopEvent;
import sk.enorials.gps.nav.NavigationSession;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.betonquest.HudBetonQuest;
import sk.enorials.hud.betonquest.HudQuestLog;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapProvider;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.marker.MarkerCategory;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.minimap.quest.BetonQuestNpcResolver;
import sk.enorials.minimap.quest.QuestMarkerLayer;
import sk.enorials.minimap.quest.QuestMarkerMappings;
import sk.enorials.minimap.quest.QuestMarkerState;

public final class BetonQuestMarkers
implements Listener {
    public static final String PREFIX = "__betonquest::";
    private static BetonQuestMarkers instance;
    private final EnorialsMinimap plugin;
    private final BetonQuestApi api;
    private final BetonQuestNpcResolver npcResolver;
    private final List<NpcRule> npcRules = new ArrayList<NpcRule>();
    private final Set<String> warned = new HashSet<String>();
    private int ticks;

    private BetonQuestMarkers(EnorialsMinimap enorialsMinimap) {
        this.plugin = enorialsMinimap;
        this.api = BetonQuest.getInstance().getBetonQuestApi();
        this.npcResolver = new BetonQuestNpcResolver(this.api, this::warn);
        this.api.reloader().register(ReloadPhase.POST, () -> {
            if (!enorialsMinimap.owner().isEnabled()) {
                return;
            }
            if (Bukkit.isPrimaryThread()) {
                this.reloadMappings();
            } else {
                Bukkit.getScheduler().runTask((Plugin)enorialsMinimap.owner(), this::reloadMappings);
            }
        });
    }

    public static void install(EnorialsMinimap enorialsMinimap) {
        if (!Bukkit.getPluginManager().isPluginEnabled("BetonQuest")) {
            return;
        }
        instance = new BetonQuestMarkers(enorialsMinimap);
        Bukkit.getPluginManager().registerEvents((Listener)instance, (Plugin)enorialsMinimap.owner());
        Bukkit.getScheduler().runTaskTimer((Plugin)enorialsMinimap.owner(), instance::tick, 20L, 10L);
    }

    public static BetonQuestMarkers get() {
        return instance;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onNavigationStart(GpsNavigationStartEvent gpsNavigationStartEvent) {
        this.refreshAfterNavigation(gpsNavigationStartEvent.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onNavigationStop(GpsNavigationStopEvent gpsNavigationStopEvent) {
        this.refreshAfterNavigation(gpsNavigationStopEvent.getPlayer());
    }

    private void refreshAfterNavigation(Player player) {
        try {
            this.refresh(player);
        }
        catch (Exception | LinkageError throwable) {
            this.warn("navigation", throwable);
        }
    }

    private boolean isQuestTracking(Player player) {
        EnorialsGpsPlugin enorialsGpsPlugin = this.plugin.owner().gps();
        if (enorialsGpsPlugin == null) {
            return false;
        }
        NavigationSession navigationSession = enorialsGpsPlugin.navigation().session(player);
        return navigationSession != null && navigationSession.isTrackedQuest();
    }

    private void tick() {
        try {
            if (this.enabled() && this.ticks++ % 120 == 0) {
                this.reloadMappings();
            }
            for (Minimap minimap : List.copyOf(this.plugin.minimapListener().getPlayerMinimaps().values())) {
                this.refresh(minimap.holder());
            }
        }
        catch (Exception | LinkageError throwable) {
            this.warn("runtime", throwable);
        }
    }

    public boolean enabled() {
        return Bukkit.getPluginManager().isPluginEnabled("BetonQuest") && (this.plugin.owner().gps() == null || this.plugin.owner().gps().config().getBoolean("betonquest.enabled", true));
    }

    public void reloadMappings() {
        if (!this.enabled()) {
            this.npcRules.clear();
            return;
        }
        ArrayList<NpcRule> arrayList = new ArrayList<NpcRule>();
        EnorialsGpsPlugin enorialsGpsPlugin = this.plugin.owner().gps();
        QuestMarkerMappings questMarkerMappings = enorialsGpsPlugin == null ? QuestMarkerMappings.defaults() : enorialsGpsPlugin.questMarkerMappings();
        this.npcResolver.configure(questMarkerMappings.npcSources());
        for (QuestPackage questPackage : this.api.packages().getPackages().values()) {
            for (String string : List.of("npc_holograms", "enorials_markers")) {
                ConfigurationSection configurationSection = questPackage.getConfig().getConfigurationSection(string);
                if (configurationSection == null) continue;
                for (String string2 : configurationSection.getKeys(false)) {
                    ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection(string2);
                    if (configurationSection2 == null) continue;
                    QuestMarkerState questMarkerState = questMarkerMappings.fromLines(configurationSection2.getStringList("lines"), questPackage.getQuestPath());
                    String string3 = configurationSection2.getString("conditions", "").trim();
                    if (questMarkerState == null) continue;
                    String string4 = questPackage.getQuestPath() + ">" + string + "/" + string2;
                    try {
                        ArrayList<ConditionIdentifier> arrayList2 = new ArrayList<ConditionIdentifier>();
                        for (String string5 : string3.split(",")) {
                            if (string5.isBlank()) continue;
                            arrayList2.add((ConditionIdentifier)this.api.identifiers().getFactory(ConditionIdentifier.class).parseIdentifier(questPackage, string5.trim()));
                        }
                        List<BetonQuestNpcResolver.Reference> list = this.npcResolver.references(questPackage, configurationSection2.getString("npcs", ""));
                        if (list.isEmpty()) continue;
                        arrayList.add(new NpcRule(string4, List.copyOf(arrayList2), list, questMarkerState));
                    }
                    catch (Exception exception) {
                        this.warn(string4, exception);
                    }
                }
            }
        }
        this.npcRules.clear();
        this.npcRules.addAll(arrayList);
    }

    /*
     * WARNING - void declaration
     */
    public void refresh(Player player) {
        Object object;
        OnlineProfile onlineProfile;
        List list;
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        if (minimap == null || !player.isOnline()) {
            return;
        }
        LinkedHashMap<String, Target> linkedHashMap = new LinkedHashMap<String, Target>();
        boolean bl = this.isQuestTracking(player);
        EnorialsGpsPlugin enorialsGpsPlugin = this.plugin.owner().gps();
        NavigationSession navigationSession = enorialsGpsPlugin == null ? null : enorialsGpsPlugin.navigation().session(player);
        Location location2 = bl ? navigationSession.locator().resolve(player) : null;
        List<Object> list2 = list = bl ? enorialsGpsPlugin.navigation().trackedQuestNpcLocations(player) : List.of();
        if (this.enabled() && HudAccess.has((CommandSender)player, "enorials.minimap.quests") && this.plugin.owner().storage().preference(player.getUniqueId(), "betonquest-markers", true) && !this.plugin.menuListener().isCategoryHidden(player, MarkerCategory.QUEST)) {
            onlineProfile = this.api.profiles().getProfile(player);
            LinkedHashMap linkedHashMap2 = new LinkedHashMap();
            for (NpcRule npcRule : this.npcRules) {
                if (npcRule.state() == QuestMarkerState.ACTIVE && !bl) continue;
                try {
                    if (!((ConditionManager)this.api.conditions().manager()).testAll((Profile)onlineProfile, npcRule.conditions())) continue;
                    for (BetonQuestNpcResolver.Reference reference2 : npcRule.npcs()) {
                        for (BetonQuestNpcResolver.Located located : linkedHashMap2.computeIfAbsent(reference2, reference -> this.npcResolver.locations((BetonQuestNpcResolver.Reference)reference, onlineProfile))) {
                            boolean bl2;
                            boolean bl3 = bl2 = BetonQuestMarkers.sameLocation(located.location(), location2) || list.stream().anyMatch(location -> BetonQuestMarkers.sameLocation(located.location(), location));
                            if (npcRule.state() == QuestMarkerState.ACTIVE && !bl2) continue;
                            boolean bl4 = npcRule.state().keepOnMapEdge() && bl2;
                            BetonQuestMarkers.put(linkedHashMap, located.key(), new Target(located.location(), npcRule.state(), npcRule.source(), bl4));
                        }
                    }
                }
                catch (Exception | LinkageError throwable) {
                    this.warn(npcRule.source(), throwable);
                }
            }
            HudBetonQuest hudBetonQuest = this.plugin.owner().betonQuest();
            Set set = Set.copyOf(linkedHashMap.keySet());
            if (hudBetonQuest != null && hudBetonQuest.questLog() != null) {
                for (HudQuestLog.NpcHint npcHint : hudBetonQuest.questLog().npcHints(player)) {
                    if (set.contains(npcHint.key())) continue;
                    BetonQuestMarkers.put(linkedHashMap, npcHint.key(), new Target(npcHint.location(), npcHint.complete() ? QuestMarkerState.COMPLETE : (npcHint.incomplete() ? QuestMarkerState.INCOMPLETE : QuestMarkerState.AVAILABLE), npcHint.title(), false));
                }
            }
            if (location2 != null && location2.getWorld() != null) {
                object = "tracked/" + navigationSession.source().substring("betonquest-quest:".length());
                for (Map.Entry entry : linkedHashMap.entrySet()) {
                    if (!BetonQuestMarkers.sameLocation(((Target)entry.getValue()).location(), location2)) continue;
                    object = (String)entry.getKey();
                    break;
                }
                BetonQuestMarkers.put(linkedHashMap, (String)object, new Target(location2, QuestMarkerState.ACTIVE, navigationSession.locator().displayName(), true));
            }
        }
        onlineProfile = new HashSet();
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            Target target = (Target)entry.getValue();
            if (!target.state().visibleOnMap() || target.location().getWorld() == null || !target.location().getWorld().equals((Object)player.getWorld())) continue;
            object = PREFIX + (String)entry.getKey();
            MinimapIcon minimapIcon = this.plugin.iconProvider().getIcon(target.state().icon());
            if (minimapIcon == null) {
                minimapIcon = this.plugin.iconProvider().getIcon(target.state() == QuestMarkerState.ACTIVE ? "quest_!" : "quest_questionmark");
            }
            if (minimapIcon == null) continue;
            boolean sameLayer = secondaryMinimapLayer instanceof QuestMarkerLayer
                && target.location().getWorld().equals(secondaryMinimapLayer.getWorld())
                && secondaryMinimapLayer.getRenderer() instanceof MinimapIconRenderer renderer
                && renderer.icon().key().equals(minimapIcon.key());
            if (!sameLayer) {
                if (secondaryMinimapLayer != null) {
                    this.plugin.packetSender().despawnLayer(player, secondaryMinimapLayer.getBaseLayer());
                }
                MinimapLayer baseLayer = this.plugin.clientsideMinimapFactory().createMinimapLayer(player.getWorld(), null);
                QuestMarkerLayer questLayer = new QuestMarkerLayer(baseLayer, target.location(), minimapIcon, target.label());
                questLayer.setKeepOnEdge(target.keepOnEdge());
                minimap.secondaryLayers().put((String)object, questLayer);
                this.plugin.packetSender().spawnLayer(player, baseLayer);
            } else {
                secondaryMinimapLayer.setKeepOnEdge(target.keepOnEdge());
                secondaryMinimapLayer.setPositionX(target.location().getBlockX());
                secondaryMinimapLayer.setPositionZ(target.location().getBlockZ());
            }
            onlineProfile.add(object);
            minimap.updateSecondaryLayer((MinimapProvider)this.plugin, (String)object);
        }
        for (String string : List.copyOf(minimap.secondaryLayers().keySet())) {
            if (!string.startsWith(PREFIX) || onlineProfile.contains(string)) continue;
            SecondaryMinimapLayer secondaryMinimapLayer = (SecondaryMinimapLayer)minimap.secondaryLayers().remove(string);
            this.plugin.packetSender().despawnLayer(player, secondaryMinimapLayer.getBaseLayer());
        }
    }

    private static boolean sameLocation(Location location, Location location2) {
        return location != null && location2 != null && location.getWorld() != null && location.getWorld().equals((Object)location2.getWorld()) && location.distanceSquared(location2) < 1.0E-4;
    }

    private static void put(Map<String, Target> map, String string, Target target3) {
        map.merge(string, target3, (target, target2) -> {
            Target target3 = target2.state().priority() > target.state().priority() ? target2 : target;
            boolean bl = target3.keepOnEdge() || BetonQuestMarkers.sameLocation(target.location(), target2.location()) && (target.keepOnEdge() || target2.keepOnEdge());
            return bl == target3.keepOnEdge() ? target3 : new Target(target3.location(), target3.state(), target3.label(), bl);
        });
    }

    private void warn(String string, Throwable throwable) {
        if (this.warned.add(string)) {
            this.plugin.getLogger().warning("Quest minimap target skipped (" + string + "): " + throwable.getMessage());
        }
    }

    private record NpcRule(String source, List<ConditionIdentifier> conditions, List<BetonQuestNpcResolver.Reference> npcs, QuestMarkerState state) {
    }

    private record Target(Location location, QuestMarkerState state, String label, boolean keepOnEdge) {
    }
}

