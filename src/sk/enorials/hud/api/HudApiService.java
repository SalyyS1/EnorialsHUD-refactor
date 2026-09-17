/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.server.PluginDisableEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.RegisteredServiceProvider
 *  org.bukkit.plugin.ServicePriority
 */
package sk.enorials.hud.api;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import sk.enorials.gps.api.GpsTargetType;
import sk.enorials.gps.api.event.GpsNavigationStartEvent;
import sk.enorials.gps.api.event.GpsNavigationStopEvent;
import sk.enorials.gps.integration.IntegrationManager;
import sk.enorials.gps.nav.NavigationSession;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.gps.nav.target.StaticLocationTarget;
import sk.enorials.gps.nav.target.TargetLocator;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.api.EnorialsHudApi;
import sk.enorials.hud.api.HudApiExtras;
import sk.enorials.hud.api.HudBqApi;
import sk.enorials.hud.api.HudChangeEvent;
import sk.enorials.hud.api.HudControls;
import sk.enorials.hud.api.HudStatProvider;
import sk.enorials.hud.api.HudTypes;
import sk.enorials.hud.api.HudValue;
import sk.enorials.hud.compat.HudAttributes;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapProvider;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.minimap.radar.RadarCategory;
import sk.enorials.partyhud.event.PartyCreateEvent;
import sk.enorials.partyhud.event.PartyDisbandEvent;
import sk.enorials.partyhud.event.PartyJoinEvent;
import sk.enorials.partyhud.event.PartyLeaveEvent;
import sk.enorials.partyhud.party.Party;

public final class HudApiService
implements EnorialsHudApi,
Listener {
    private static final String PREFIX = "__external::";
    private final EnorialsHUD hud;
    private final HudApiExtras extra;
    private final Map<String, Marker> markers = new HashMap<String, Marker>();
    private final Map<String, Long> warnings = new HashMap<String, Long>();

    public HudApiService(EnorialsHUD enorialsHUD) {
        this.hud = enorialsHUD;
        this.extra = new HudApiExtras(enorialsHUD);
        Bukkit.getServicesManager().register(EnorialsHudApi.class, (Object)this, (Plugin)enorialsHUD, ServicePriority.Normal);
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
        Bukkit.getScheduler().runTaskTimer((Plugin)enorialsHUD, this::tick, 1L, 5L);
    }

    private static void mainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Use the Bukkit server thread");
        }
    }

    public HudValue read(Player player, String string) {
        for (RegisteredServiceProvider registeredServiceProvider2 : Bukkit.getServicesManager().getRegistrations(HudStatProvider.class).stream().sorted(Comparator.comparing(registeredServiceProvider -> registeredServiceProvider.getPriority()).reversed()).toList()) {
            if (!registeredServiceProvider2.getPlugin().isEnabled()) continue;
            try {
                HudValue hudValue = ((HudStatProvider)registeredServiceProvider2.getProvider()).value(player, string);
                if (hudValue == null) continue;
                return hudValue;
            }
            catch (RuntimeException runtimeException) {
                String string2 = registeredServiceProvider2.getPlugin().getName();
                long l = System.currentTimeMillis();
                if (l - this.warnings.getOrDefault(string2, 0L) <= 60000L) continue;
                this.warnings.put(string2, l);
                this.hud.getLogger().log(Level.WARNING, "HUD stat provider failed: " + string2, runtimeException);
            }
        }
        return null;
    }

    @Override
    public Set<String> markerIcons() {
        HudApiService.mainThread();
        return Set.copyOf(this.hud.minimap().iconProvider().allKeys());
    }

    @Override
    public boolean hasMarker(Plugin plugin, Player player, String string) {
        HudApiService.mainThread();
        return this.markers.containsKey(this.key(plugin, player, string));
    }

    @Override
    public String getSetting(Player player, String string) {
        HudApiService.mainThread();
        return new HudControls(this.hud).get(player, string);
    }

    @Override
    public void setSetting(Player player, String string, String string2) {
        HudApiService.mainThread();
        HudApiExtras.player(player);
        new HudControls(this.hud).set(player, string, string2);
        HudApiExtras.event(HudChangeEvent.Kind.SETTING, player.getUniqueId(), string);
    }

    @Override
    public void openMenu(Player player, String string) {
        HudApiService.mainThread();
        new HudControls(this.hud).open(player, string);
    }

    private String key(Plugin plugin, Player player, String string) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(player);
        if (string == null || !string.matches("[a-zA-Z0-9_.-]{1,64}")) {
            throw new IllegalArgumentException("Invalid marker id");
        }
        return PREFIX + plugin.getName() + "::" + String.valueOf(player.getUniqueId()) + "::" + string;
    }

    @Override
    public void putMarker(Plugin plugin, Player player, String string, Location location, String string2, String string3) {
        HudApiService.mainThread();
        String string4 = this.key(plugin, player, string);
        if (!plugin.isEnabled() || !player.isOnline()) {
            throw new IllegalArgumentException("Owner and viewer must be active");
        }
        Objects.requireNonNull(location);
        if (!(location.getWorld() != null && Double.isFinite(location.getX()) && Double.isFinite(location.getY()) && Double.isFinite(location.getZ()))) {
            throw new IllegalArgumentException("Invalid marker location");
        }
        if (string2 == null || this.hud.minimap().iconProvider().getIcon(string2) == null) {
            throw new IllegalArgumentException("Unknown marker icon: " + string2);
        }
        this.putMarker(plugin, player, new HudTypes.Marker(string, location, string2, string3, true));
    }

    @Override
    public boolean removeMarker(Plugin plugin, Player player, String string) {
        boolean bl;
        HudApiService.mainThread();
        String string2 = this.key(plugin, player, string);
        this.removeLayer(player, string2);
        boolean bl2 = bl = this.markers.remove(string2) != null;
        if (bl) {
            HudApiExtras.event(HudChangeEvent.Kind.MARKER_REMOVE, player.getUniqueId(), string);
        }
        return bl;
    }

    @Override
    public int clearMarkers(Plugin plugin, Player player) {
        HudApiService.mainThread();
        int n = 0;
        for (Map.Entry<String, Marker> entry : List.copyOf(this.markers.entrySet())) {
            if (entry.getValue().owner() != plugin || !entry.getValue().viewer().equals(player.getUniqueId())) continue;
            this.removeLayer(player, entry.getKey());
            this.markers.remove(entry.getKey());
            ++n;
        }
        return n;
    }

    private void removeLayer(Player player, String string) {
        Minimap minimap = this.hud.getPlayerMinimap(player);
        if (minimap == null) {
            return;
        }
        SecondaryMinimapLayer secondaryMinimapLayer = (SecondaryMinimapLayer)minimap.secondaryLayers().remove(string);
        if (secondaryMinimapLayer != null) {
            this.hud.minimap().packetSender().despawnLayer(player, secondaryMinimapLayer.getBaseLayer());
        }
    }

    private void tick() {
        for (Map.Entry<String, Marker> entry : List.copyOf(this.markers.entrySet())) {
            Marker marker = entry.getValue();
            Player player = Bukkit.getPlayer((UUID)marker.viewer());
            if (player == null || !marker.owner().isEnabled()) {
                if (player != null) {
                    this.removeLayer(player, entry.getKey());
                }
                this.markers.remove(entry.getKey());
                continue;
            }
            Minimap minimap = this.hud.getPlayerMinimap(player);
            if (minimap == null) continue;
            if (!player.getWorld().equals((Object)marker.position().getWorld())) {
                this.removeLayer(player, entry.getKey());
                continue;
            }
            if (!minimap.secondaryLayers().containsKey(entry.getKey())) {
                MinimapLayer minimapLayer = this.hud.minimap().clientsideMinimapFactory().createMinimapLayer(player.getWorld(), null);
                MarkerLayer markerLayer = new MarkerLayer(minimapLayer, marker.position(), this.hud.minimap().iconProvider().getIcon(marker.icon()));
                markerLayer.setKeepOnEdge(marker.edge());
                minimap.secondaryLayers().put(entry.getKey(), markerLayer);
                this.hud.minimap().packetSender().spawnLayer(player, minimapLayer);
            }
            minimap.updateSecondaryLayer((MinimapProvider)this.hud.minimap(), entry.getKey());
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void quit(PlayerQuitEvent playerQuitEvent) {
        NavigationSession navigationSession = this.hud.gps().navigation().session(playerQuitEvent.getPlayer());
        if (navigationSession != null && (navigationSession.source().startsWith("hud-api:") || this.extra.routes.values().stream().anyMatch(route -> route.player().equals(playerQuitEvent.getPlayer().getUniqueId()) && navigationSession.source().equals("betonquest-quest:" + route.id())))) {
            this.hud.gps().api().stopNavigation(playerQuitEvent.getPlayer(), "temporary_api_quit");
        }
        this.markers.entrySet().removeIf(entry -> ((Marker)entry.getValue()).viewer().equals(playerQuitEvent.getPlayer().getUniqueId()));
        this.extra.quit(playerQuitEvent.getPlayer().getUniqueId());
    }

    @EventHandler
    public void disabled(PluginDisableEvent pluginDisableEvent) {
        this.release(pluginDisableEvent.getPlugin());
        this.warnings.remove(pluginDisableEvent.getPlugin().getName());
    }

    public void close() {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            this.release(plugin);
        }
        this.markers.clear();
        Bukkit.getServicesManager().unregister((Object)this);
        HandlerList.unregisterAll((Listener)this);
    }

    @Override
    public String apiVersion() {
        return "2.0";
    }

    @Override
    public Set<String> capabilities() {
        HudApiService.mainThread();
        HashSet<String> hashSet = new HashSet<String>(Set.of("markers", "minimap", "gps", "party", "npc-members", "stats", "popups", "settings", "events"));
        if (this.bq()) {
            hashSet.add("betonquest");
        }
        return Set.copyOf(hashSet);
    }

    @Override
    public Map<String, HudTypes.Setting> settings() {
        HudApiService.mainThread();
        return HudApiExtras.settings();
    }

    @Override
    public Set<String> menus() {
        return Set.of("main", "minimap", "party", "party-join", "quests", "hud", "gps", "markers", "effects");
    }

    @Override
    public boolean hasPermission(Player player, String string) {
        HudApiService.mainThread();
        return HudAccess.has((CommandSender)player, Objects.requireNonNull(string));
    }

    @Override
    public HudTypes.Status status(Player player) {
        HudApiExtras.player(player);
        return new HudTypes.Status(this.hud.getDescription().getVersion(), this.hud.packLoadGuard() != null && this.hud.packLoadGuard().isWaiting(player), this.hud.getPlayerMinimap(player) != null, player.hasMetadata("enorials.conversation"));
    }

    @Override
    public void putMarker(Plugin plugin, Player player, HudTypes.Marker marker2) {
        HudApiExtras.active(plugin);
        HudApiExtras.player(player);
        Objects.requireNonNull(marker2);
        String string = this.key(plugin, player, marker2.id());
        if (this.hud.iconProvider().getIcon(marker2.icon()) == null) {
            throw new IllegalArgumentException("Unknown marker icon: " + marker2.icon());
        }
        Marker marker3 = this.markers.get(string);
        Location location = marker2.location();
        if (marker3 != null && marker3.position().equals((Object)location) && marker3.icon().equals(marker2.icon()) && marker3.label().equals(marker2.label()) && marker3.edge() == marker2.keepOnEdge()) {
            return;
        }
        if (marker3 == null && this.markers.values().stream().filter(marker -> marker.owner() == plugin && marker.viewer().equals(player.getUniqueId())).count() >= 128L) {
            throw new IllegalStateException("Maximum 128 markers per owner and viewer");
        }
        this.removeLayer(player, string);
        this.markers.put(string, new Marker(plugin, player.getUniqueId(), location, marker2.icon(), marker2.label(), marker2.keepOnEdge()));
        this.tick();
        Minimap minimap = this.hud.getPlayerMinimap(player);
        if (minimap != null && minimap.secondaryLayers().get(string) != null) {
            minimap.secondaryLayers().get(string).setKeepOnEdge(marker2.keepOnEdge());
        }
        HudApiExtras.event(HudChangeEvent.Kind.MARKER_PUT, player.getUniqueId(), marker2.id());
    }

    @Override
    public List<HudTypes.Marker> markers(Plugin plugin, Player player) {
        HudApiService.mainThread();
        return this.markers.entrySet().stream()
            .filter(entry -> ((Marker)entry.getValue()).owner() == plugin
                && ((Marker)entry.getValue()).viewer().equals(player.getUniqueId()))
            .map(entry -> {
                Marker marker = (Marker)entry.getValue();
                return new HudTypes.Marker(((String)entry.getKey()).substring(((String)entry.getKey()).lastIndexOf("::") + 2), marker.position(), marker.icon(), marker.label(), marker.edge());
            }).sorted(Comparator.comparing(HudTypes.Marker::id)).toList();
    }

    private Minimap map(Player player) {
        HudApiExtras.player(player);
        Minimap minimap = this.hud.getPlayerMinimap(player);
        if (minimap == null) {
            throw new IllegalStateException("Enable the minimap first");
        }
        return minimap;
    }

    @Override
    public HudTypes.Zoom zoom(Player player) {
        return HudTypes.Zoom.values()[2 - this.map(player).zoomLevel()];
    }

    @Override
    public void setZoom(Player player, HudTypes.Zoom zoom) {
        HudAccess.require((CommandSender)player, "enorials.minimap.use");
        Minimap minimap = this.map(player);
        minimap.zoomLevel(2 - Objects.requireNonNull(zoom).ordinal());
        minimap.respawn(this.hud.minimap());
        this.hud.queueMinimapSave(minimap);
        HudApiExtras.event(HudChangeEvent.Kind.SETTING, player.getUniqueId(), "map.zoom");
    }

    private RadarCategory category(HudTypes.Radar radar) {
        return switch (Objects.requireNonNull(radar)) {
            default -> throw new IncompatibleClassChangeError();
            case HudTypes.Radar.HOSTILE -> RadarCategory.AGGRESSIVE;
            case HudTypes.Radar.NEUTRAL -> RadarCategory.NEUTRAL;
            case HudTypes.Radar.FRIENDLY -> RadarCategory.FRIENDLY;
        };
    }

    @Override
    public boolean radar(Player player, HudTypes.Radar radar) {
        return this.map(player).radarEnabled(this.category(radar));
    }

    @Override
    public void setRadar(Player player, HudTypes.Radar radar, boolean bl) {
        HudAccess.require((CommandSender)player, HudAccess.radar(this.category(radar)));
        Minimap minimap = this.map(player);
        minimap.radarEnabled(this.category(radar), bl);
        minimap.respawn(this.hud.minimap());
        this.hud.queueMinimapSave(minimap);
        HudApiExtras.event(HudChangeEvent.Kind.SETTING, player.getUniqueId(), "map.radar." + radar.name());
    }

    @Override
    public Optional<HudTypes.Navigation> navigation(Player player) {
        HudApiExtras.player(player);
        NavigationSession navigationSession = this.hud.gps().navigation().session(player);
        return navigationSession == null ? Optional.empty() : Optional.of(new HudTypes.Navigation(navigationSession.locator().displayName(), navigationSession.source(), HudApiExtras.mode(navigationSession.mode()), navigationSession.lastResolvedTarget()));
    }

    @Override
    public boolean go(Plugin plugin, Player player, String string, Location location, HudTypes.NavigationMode navigationMode) {
        HudApiExtras.active(plugin);
        HudApiExtras.player(player);
        Location location2 = HudTypes.validLocation(location);
        return this.hud.gps().navigation().startNavigation(player, new StaticLocationTarget(Objects.requireNonNull(string), location2), HudApiExtras.mode(navigationMode), HudApiExtras.source(plugin), RouteDescriptor.location(location2));
    }

    @Override
    public boolean go(Plugin plugin, Player player, HudTypes.TargetType targetType, String string, HudTypes.NavigationMode navigationMode) {
        HudApiExtras.active(plugin);
        HudApiExtras.player(player);
        return this.hud.gps().api().startNavigation(player, GpsTargetType.valueOf(targetType.name()), string, HudApiExtras.mode(navigationMode), HudApiExtras.source(plugin));
    }

    @Override
    public boolean stop(Plugin plugin, Player player) {
        HudApiService.mainThread();
        return this.navigation(player).filter(navigation -> this.extra.owns(plugin, player.getUniqueId(), navigation.source())).isPresent() && this.hud.gps().api().stopNavigation(player, "owner_api");
    }

    @Override
    public boolean stop(Player player) {
        HudApiExtras.player(player);
        return this.hud.gps().api().stopNavigation(player, "player_api");
    }

    @Override
    public List<HudTypes.QuestRoute> questRoutes(Player player) {
        HudApiExtras.player(player);
        if (!HudAccess.has((CommandSender)player, "enorials.gps.quests")) {
            return List.of();
        }
        return this.hud.gps().api().getQuestRouteButtons(player).values().stream()
            .map(gpsQuestRoute -> new HudTypes.QuestRoute(gpsQuestRoute.id(), gpsQuestRoute.displayName(), HudTypes.TargetType.valueOf(gpsQuestRoute.targetType().name()), gpsQuestRoute.targetValue(), HudApiExtras.mode(gpsQuestRoute.mode()), gpsQuestRoute.source())).toList();
    }

    @Override
    public String routeId(Plugin plugin, String string) {
        HudApiService.mainThread();
        return HudApiExtras.routeId(Objects.requireNonNull(plugin), string);
    }

    @Override
    public String putQuestRoute(Plugin plugin, Player player, String string, String string2, Location location, HudTypes.NavigationMode navigationMode) {
        return this.putQuestRoute(plugin, player, string, string2, location, navigationMode, false);
    }

    @Override
    public String putQuestRoute(Plugin plugin, Player player, String string, String string2, Location location, HudTypes.NavigationMode navigationMode, boolean bl) {
        HudApiExtras.active(plugin);
        HudApiExtras.player(player);
        if (bl && !this.bq()) {
            throw new IllegalStateException("BetonQuest is unavailable");
        }
        String string3 = HudApiExtras.routeId(plugin, string);
        Location location2 = HudTypes.validLocation(location);
        String string4 = String.valueOf(player.getUniqueId()) + ":" + string3;
        if (!this.extra.routes.containsKey(string4) && this.extra.routes.values().stream().filter(route -> route.owner() == plugin && route.player().equals(player.getUniqueId())).count() >= 128L) {
            throw new IllegalStateException("Maximum 128 routes per owner and viewer");
        }
        this.hud.gps().navigation().integrations().putExternalRoute(player.getUniqueId(), string3, Objects.requireNonNull(string2), RouteDescriptor.location(location2), HudApiExtras.mode(navigationMode), (String)(bl ? "betonquest-quest:" + string3 : HudApiExtras.source(plugin)));
        this.extra.routes.put(string4, new HudApiExtras.Route(plugin, player.getUniqueId(), string3));
        return string3;
    }

    @Override
    public boolean removeQuestRoute(Plugin plugin, Player player, String string) {
        HudApiService.mainThread();
        String string2 = HudApiExtras.routeId(plugin, string);
        this.extra.routes.remove(String.valueOf(player.getUniqueId()) + ":" + string2);
        return this.hud.gps().navigation().integrations().removeExternalRoute(player.getUniqueId(), string2);
    }

    @Override
    public boolean track(Player player, String string) {
        HudApiExtras.player(player);
        HudAccess.require((CommandSender)player, "enorials.gps.quests");
        if (this.bq() && HudBqApi.track(this.hud, player, string)) {
            return true;
        }
        IntegrationManager integrationManager = this.hud.gps().navigation().integrations();
        IntegrationManager.AutoRoute autoRoute = integrationManager.getAutoRoutes(player).get(string);
        if (autoRoute == null) {
            return false;
        }
        Optional<TargetLocator> optional = integrationManager.resolveAutoRoute(player, autoRoute);
        return optional.isPresent() && this.hud.gps().navigation().startNavigation(player, optional.get(), autoRoute.mode(), autoRoute.source(), autoRoute.descriptor());
    }

    public Player resolveMember(UUID uUID) {
        HudApiService.mainThread();
        return this.extra.resolve(uUID);
    }

    @Override
    public Optional<HudTypes.Party> party(UUID uUID) {
        HudApiService.mainThread();
        return this.hud.getPartyService().getPartyOf(uUID).map(this.extra::snapshot);
    }

    @Override
    public List<HudTypes.Party> openParties() {
        HudApiService.mainThread();
        return this.hud.getPartyService().getOpenParties().stream().map(this.extra::snapshot).toList();
    }

    @Override
    public HudTypes.Member member(UUID uUID) {
        return this.extra.member(uUID);
    }

    @Override
    public HudTypes.Party createParty(Player player, boolean bl) {
        HudApiExtras.player(player);
        if (this.party(player.getUniqueId()).isPresent()) {
            throw new IllegalStateException("Leave the current party first");
        }
        return this.extra.snapshot(this.hud.getPartyService().createParty(player, bl));
    }

    @Override
    public boolean joinOpenParty(Player player, UUID uUID) {
        HudApiExtras.player(player);
        return this.hud.getPartyService().getOpenParties().stream()
            .filter(party -> party.getId().equals(uUID)).findFirst()
            .map(party -> this.hud.getPartyService().joinParty((Party)party, player, this.extra.capacity()))
            .orElse(false);
    }

    @Override
    public boolean leaveParty(Player player) {
        HudApiExtras.player(player);
        return this.hud.getPartyService().leaveParty(player.getUniqueId(), false);
    }

    @Override
    public boolean kick(Player player, UUID uUID) {
        this.extra.leader(player);
        boolean bl = this.hud.getPartyService().kick(player, uUID);
        if (bl) {
            HudApiExtras.event(HudChangeEvent.Kind.PARTY_UPDATED, player.getUniqueId(), uUID.toString());
        }
        return bl;
    }

    @Override
    public boolean promote(Player player, UUID uUID) {
        this.extra.leader(player);
        if (this.extra.npcs.containsKey(uUID)) {
            return false;
        }
        boolean bl = this.hud.getPartyService().promote(player, uUID);
        if (bl) {
            HudApiExtras.event(HudChangeEvent.Kind.PARTY_UPDATED, player.getUniqueId(), uUID.toString());
        }
        return bl;
    }

    @Override
    public void openParty(Player player, boolean bl) {
        Party party = this.extra.leader(player);
        party.setOpen(bl);
        HudApiExtras.event(HudChangeEvent.Kind.PARTY_UPDATED, player.getUniqueId(), party.getId().toString());
    }

    @Override
    public void disbandParty(Player player) {
        this.hud.getPartyService().disbandParty(this.extra.leader(player));
    }

    @Override
    public HudTypes.Registration partyLimit(Plugin plugin, Player player, int n) {
        return this.extra.limit(plugin, player, n);
    }

    public int partyLimit(UUID uUID, int n) {
        HudApiService.mainThread();
        HudApiExtras.Limit limit = this.extra.limits.get(uUID);
        return limit == null ? n : limit.maximum();
    }

    @Override
    public HudTypes.Registration registerNpc(Plugin plugin, Player player) {
        return this.extra.npc(plugin, player);
    }

    @Override
    public boolean joinNpc(Plugin plugin, Player player, UUID uUID) {
        HudApiExtras.active(plugin);
        Party party = this.extra.leader(player);
        HudApiExtras.Npc npc = this.extra.npcs.get(uUID);
        if (npc == null || npc.owner() != plugin || !npc.player().isValid()) {
            return false;
        }
        return this.hud.getPartyService().joinParty(party, npc.player(), this.extra.capacity());
    }

    @Override
    public boolean leaveNpc(Plugin plugin, UUID uUID) {
        return this.extra.leave(plugin, uUID);
    }

    @Override
    public Optional<HudTypes.Target> currentTarget(Player player) {
        HudApiExtras.player(player);
        if (!HudAccess.has((CommandSender)player, "enorials.hud.target")) {
            return Optional.empty();
        }
        LivingEntity livingEntity = this.hud.renderer().getCurrentTarget(player);
        return livingEntity == null ? Optional.empty() : Optional.of(new HudTypes.Target(livingEntity.getUniqueId(), this.hud.listeners().getTargetName(player, livingEntity), livingEntity.getType().name(), livingEntity.getLocation(), new HudValue(livingEntity.getHealth(), livingEntity.getAttribute(HudAttributes.MAX_HEALTH).getValue())));
    }

    @Override
    public HudValue stat(Player player, String string) {
        HudApiExtras.player(player);
        if (!Set.of("health", "mana", "rage", "resource", "food", "armor", "air").contains(string)) {
            throw new IllegalArgumentException("Unknown stat: " + string);
        }
        double d = this.hud.listeners().getMax(player, string);
        return d > 0.0 ? new HudValue(this.hud.listeners().getCurrent(player, string), d) : null;
    }

    @Override
    public HudTypes.Registration registerStats(Plugin plugin, HudStatProvider hudStatProvider, ServicePriority servicePriority) {
        return this.extra.stat(plugin, hudStatProvider, servicePriority);
    }

    @Override
    public boolean showPopup(Player player, String string, List<String> list, long l) {
        HudApiExtras.player(player);
        HudAccess.require((CommandSender)player, "enorials.hud.use");
        if (l < 1L || l > 72000L) {
            throw new IllegalArgumentException("Duration must be 1..72000 ticks");
        }
        if (!this.hud.popups().has(string)) {
            return false;
        }
        this.hud.renderer().showPopup(player, string, (String[])List.copyOf(list).toArray(String[]::new), l);
        return true;
    }

    private boolean bq() {
        return Bukkit.getPluginManager().isPluginEnabled("BetonQuest");
    }

    @Override
    public List<HudTypes.Quest> quests(Player player) {
        HudApiExtras.player(player);
        return this.bq() && HudAccess.has((CommandSender)player, "enorials.hud.questlog") ? HudBqApi.quests(this.hud, player) : List.of();
    }

    @Override
    public void refreshQuests(Player player) {
        HudApiExtras.player(player);
        if (this.bq()) {
            HudBqApi.refresh(this.hud, player);
        }
    }

    @Override
    public boolean startConversation(Player player, String string, Location location) {
        HudApiExtras.player(player);
        HudAccess.require((CommandSender)player, "enorials.hud.conversations");
        return this.bq() && HudBqApi.start(player, Objects.requireNonNull(string), HudTypes.validLocation(location));
    }

    @Override
    public boolean cancelConversation(Player player) {
        HudApiExtras.player(player);
        return this.bq() && HudBqApi.cancel(this.hud, player);
    }

    @Override
    public void release(Plugin plugin) {
        HudApiService.mainThread();
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.clearMarkers(plugin, player);
        }
        this.markers.values().removeIf(marker -> marker.owner() == plugin);
        this.extra.release(plugin);
    }

    @EventHandler
    public void created(PartyCreateEvent partyCreateEvent) {
        HudApiExtras.event(HudChangeEvent.Kind.PARTY_CREATED, partyCreateEvent.getLeader().getUniqueId(), partyCreateEvent.getPartyId().toString());
    }

    @EventHandler
    public void joined(PartyJoinEvent partyJoinEvent) {
        HudApiExtras.event(HudChangeEvent.Kind.PARTY_JOINED, partyJoinEvent.getPlayer().getUniqueId(), partyJoinEvent.getPartyId().toString());
    }

    @EventHandler
    public void left(PartyLeaveEvent partyLeaveEvent) {
        HudApiExtras.event(HudChangeEvent.Kind.PARTY_LEFT, partyLeaveEvent.getPlayer().getUniqueId(), partyLeaveEvent.getPartyId().toString());
    }

    @EventHandler
    public void disbanded(PartyDisbandEvent partyDisbandEvent) {
        this.extra.limits.remove(partyDisbandEvent.getPartyId());
        HudApiExtras.event(HudChangeEvent.Kind.PARTY_DISBANDED, null, partyDisbandEvent.getPartyId().toString());
    }

    @EventHandler
    public void started(GpsNavigationStartEvent gpsNavigationStartEvent) {
        HudApiExtras.event(HudChangeEvent.Kind.NAVIGATION_STARTED, gpsNavigationStartEvent.getPlayer().getUniqueId(), gpsNavigationStartEvent.getSource());
    }

    @EventHandler
    public void stopped(GpsNavigationStopEvent gpsNavigationStopEvent) {
        HudApiExtras.event(HudChangeEvent.Kind.NAVIGATION_STOPPED, gpsNavigationStopEvent.getPlayer().getUniqueId(), gpsNavigationStopEvent.getReason());
    }

    private record Marker(Plugin owner, UUID viewer, Location position, String icon, String label, boolean edge) {
    }

    public static final class MarkerLayer
    extends SecondaryMinimapLayer {
        MarkerLayer(MinimapLayer minimapLayer, Location location, MinimapIcon minimapIcon) {
            super(minimapLayer, new MinimapIconRenderer(minimapIcon), true, true, location.getWorld(), location.getBlockX(), location.getBlockZ(), 0.065f);
        }
    }
}

