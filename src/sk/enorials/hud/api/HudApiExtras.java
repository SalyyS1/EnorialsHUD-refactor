/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.permissions.PermissionAttachment
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.ServicePriority
 */
package sk.enorials.hud.api;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import sk.enorials.gps.NavigationMode;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.api.HudChangeEvent;
import sk.enorials.hud.api.HudStatProvider;
import sk.enorials.hud.api.HudTypes;
import sk.enorials.hud.api.HudValue;
import sk.enorials.hud.compat.HudAttributes;
import sk.enorials.hud.theme.HudTheme;
import sk.enorials.partyhud.party.Party;

final class HudApiExtras {
    final EnorialsHUD hud;
    final Map<UUID, Npc> npcs = new HashMap<UUID, Npc>();
    final List<Stat> stats = new ArrayList<Stat>();
    final Map<String, Route> routes = new HashMap<String, Route>();
    final Map<UUID, Limit> limits = new HashMap<UUID, Limit>();

    HudApiExtras(EnorialsHUD enorialsHUD) {
        this.hud = enorialsHUD;
    }

    static void thread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Enorials HUD API must run on the server thread");
        }
    }

    static void active(Plugin plugin) {
        HudApiExtras.thread();
        if (plugin == null || !plugin.isEnabled()) {
            throw new IllegalArgumentException("Owner must be an enabled plugin");
        }
    }

    static void player(Player player) {
        HudApiExtras.thread();
        if (player == null || !player.isOnline()) {
            throw new IllegalArgumentException("Player must be online");
        }
    }

    static String source(Plugin plugin) {
        return "hud-api:" + plugin.getName().toLowerCase(Locale.ROOT);
    }

    boolean owns(Plugin plugin, UUID uUID, String string) {
        return string.equals(HudApiExtras.source(plugin)) || this.routes.values().stream().anyMatch(route -> route.owner() == plugin && route.player().equals(uUID) && string.equals("betonquest-quest:" + route.id()));
    }

    static String routeId(Plugin plugin, String string) {
        HudTypes.requireId(string);
        return "eno_api_" + HexFormat.of().formatHex(plugin.getName().getBytes(StandardCharsets.UTF_8)) + "_" + HexFormat.of().formatHex(string.getBytes(StandardCharsets.UTF_8));
    }

    Player resolve(UUID uUID) {
        Player player = Bukkit.getPlayer((UUID)uUID);
        if (player != null) {
            return player;
        }
        Npc npc = this.npcs.get(uUID);
        return npc != null && npc.owner().isEnabled() && npc.player().isValid() ? npc.player() : null;
    }

    int capacity() {
        return Math.max(1, this.hud.party().getConfig().getInt("party.max-size", 5));
    }

    HudTypes.Party snapshot(Party party) {
        return new HudTypes.Party(party.getId(), party.getLeader(), List.copyOf(party.getMembers()), party.isOpen(), this.limits.containsKey(party.getId()) ? this.limits.get(party.getId()).maximum() : this.capacity());
    }

    HudTypes.Registration limit(Plugin plugin, Player player, int n) {
        HudApiExtras.active(plugin);
        Party party = this.leader(player);
        HudAccess.require((CommandSender)player, "enorials.party.admin");
        if (n < 1 || n > 8 || n < party.size()) {
            throw new IllegalArgumentException("Capacity must be 1..8 and at least the current member count");
        }
        if (this.limits.containsKey(party.getId())) {
            throw new IllegalStateException("Another capacity registration is active");
        }
        Limit limit = new Limit(plugin, n);
        this.limits.put(party.getId(), limit);
        return () -> {
            HudApiExtras.thread();
            this.limits.remove(party.getId(), limit);
        };
    }

    Party leader(Player player) {
        HudApiExtras.player(player);
        HudAccess.require((CommandSender)player, "enorials.party.use");
        Party party = this.hud.getPartyService().getPartyOf(player.getUniqueId()).orElseThrow(() -> new IllegalStateException("Player has no party"));
        if (!party.getLeader().equals(player.getUniqueId())) {
            throw new IllegalArgumentException("Only the party leader can do this");
        }
        return party;
    }

    HudTypes.Member member(UUID uUID) {
        HudApiExtras.thread();
        Player player = this.resolve(Objects.requireNonNull(uUID));
        return new HudTypes.Member(uUID, player == null ? Objects.toString(Bukkit.getOfflinePlayer((UUID)uUID).getName(), "Player") : player.getName(), player != null, this.npcs.containsKey(uUID), player == null ? null : player.getLocation(), player == null ? null : new HudValue(player.getHealth(), player.getAttribute(HudAttributes.MAX_HEALTH).getValue()));
    }

    HudTypes.Registration npc(Plugin plugin, Player player) {
        HudApiExtras.active(plugin);
        if (player == null || !player.isValid() || !player.hasMetadata("NPC") || Bukkit.getOnlinePlayers().contains(player)) {
            throw new IllegalArgumentException("Expected a spawned player-type NPC, not a connected player");
        }
        if (this.npcs.containsKey(player.getUniqueId())) {
            throw new IllegalStateException("NPC is already registered");
        }
        PermissionAttachment permissionAttachment = player.addAttachment((Plugin)this.hud, "enorials.party.use", true);
        Npc npc = new Npc(plugin, player, permissionAttachment);
        this.npcs.put(player.getUniqueId(), npc);
        return () -> {
            HudApiExtras.thread();
            if (this.npcs.get(player.getUniqueId()) == npc) {
                this.leave(plugin, player.getUniqueId());
                this.npcs.remove(player.getUniqueId());
                this.hud.avatars().releaseNpc(player.getUniqueId());
                player.removeAttachment(permissionAttachment);
            }
        };
    }

    boolean leave(Plugin plugin, UUID uUID) {
        HudApiExtras.thread();
        Npc npc = this.npcs.get(uUID);
        if (npc == null || npc.owner() != plugin) {
            return false;
        }
        return this.hud.getPartyService().leaveParty(uUID, true);
    }

    HudTypes.Registration stat(Plugin plugin, HudStatProvider hudStatProvider, ServicePriority servicePriority) {
        HudApiExtras.active(plugin);
        Objects.requireNonNull(hudStatProvider);
        Objects.requireNonNull(servicePriority);
        HudStatProvider hudStatProvider2 = (player, string) -> hudStatProvider.value(player, string);
        Stat stat = new Stat(plugin, hudStatProvider2);
        this.stats.add(stat);
        Bukkit.getServicesManager().register(HudStatProvider.class, (Object)hudStatProvider2, plugin, servicePriority);
        return () -> {
            HudApiExtras.thread();
            if (this.stats.remove(stat)) {
                Bukkit.getServicesManager().unregister(HudStatProvider.class, (Object)hudStatProvider2);
            }
        };
    }

    void release(Plugin plugin) {
        this.limits.values().removeIf(limit -> limit.owner() == plugin);
        HudApiExtras.thread();
        for (Player object : Bukkit.getOnlinePlayers()) {
            if (this.hud.gps().navigation().session(object) == null || !this.hud.gps().navigation().session(object).source().equals(HudApiExtras.source(plugin)) && !this.routes.values().stream().anyMatch(route -> route.owner() == plugin && route.player().equals(object.getUniqueId()) && this.hud.gps().navigation().session(object).source().equals("betonquest-quest:" + route.id()))) continue;
            this.hud.gps().api().stopNavigation(object, "owner_disabled");
        }
        for (Route route2 : List.copyOf(this.routes.values())) {
            if (route2.owner() != plugin) continue;
            this.hud.gps().navigation().integrations().removeExternalRoute(route2.player(), route2.id());
            this.routes.remove(String.valueOf(route2.player()) + ":" + route2.id());
        }
        for (Npc npc : List.copyOf(this.npcs.values())) {
            if (npc.owner() != plugin) continue;
            this.leave(plugin, npc.player().getUniqueId());
            this.npcs.remove(npc.player().getUniqueId());
            this.hud.avatars().releaseNpc(npc.player().getUniqueId());
            npc.player().removeAttachment(npc.permission());
        }
        for (Stat stat : List.copyOf(this.stats)) {
            if (stat.owner() != plugin) continue;
            Bukkit.getServicesManager().unregister(HudStatProvider.class, (Object)stat.provider());
            this.stats.remove(stat);
        }
    }

    void quit(UUID uUID) {
        for (Route route : List.copyOf(this.routes.values())) {
            if (!route.player().equals(uUID)) continue;
            this.hud.gps().navigation().integrations().removeExternalRoute(uUID, route.id());
            this.routes.remove(String.valueOf(uUID) + ":" + route.id());
        }
    }

    static NavigationMode mode(HudTypes.NavigationMode navigationMode) {
        return NavigationMode.valueOf(Objects.requireNonNull(navigationMode).name());
    }

    static HudTypes.NavigationMode mode(NavigationMode navigationMode) {
        return navigationMode == NavigationMode.COMPASS ? HudTypes.NavigationMode.COMPASS : HudTypes.NavigationMode.HUD;
    }

    static Map<String, HudTypes.Setting> settings() {
        LinkedHashMap<String, HudTypes.Setting> linkedHashMap = new LinkedHashMap<String, HudTypes.Setting>();
        for (String string : List.of("hud.visible", "hud.quest-log", "hud.conversations", "party.visible", "effects.visible", "map.visible", "map.coords", "map.combat-zoom", "map.radar", "map.betonquest-markers", "map.marker-scaling")) {
            HudApiExtras.add(linkedHashMap, string, List.of("on", "off"));
        }
        HudApiExtras.add(linkedHashMap, "hud.theme", HudTheme.ids());
        HudApiExtras.add(linkedHashMap, "hud.quest-log-size", List.of("50", "75", "100", "125"));
        HudApiExtras.add(linkedHashMap, "map.marker-size", List.of("75", "100", "125", "150"));
        HudApiExtras.add(linkedHashMap, "party.size", List.of("small", "medium", "large"));
        HudApiExtras.add(linkedHashMap, "map.size", List.of("small", "medium", "large"));
        HudApiExtras.add(linkedHashMap, "map.shape", List.of("circle", "square"));
        HudApiExtras.add(linkedHashMap, "map.position", List.of("left", "right"));
        HudApiExtras.add(linkedHashMap, "map.frame", List.of("theme", "steel", "crystal", "gold", "ruby", "forest", "ivory"));
        HudApiExtras.add(linkedHashMap, "gps.mode", List.of("hud", "compass"));
        return Collections.unmodifiableMap(linkedHashMap);
    }

    static void add(Map<String, HudTypes.Setting> map, String string, List<String> list) {
        map.put(string, new HudTypes.Setting(string, list, HudAccess.setting(string)));
    }

    static void event(HudChangeEvent.Kind kind, UUID uUID, String string) {
        Bukkit.getPluginManager().callEvent((Event)new HudChangeEvent(kind, uUID, string));
    }

    record Npc(Plugin owner, Player player, PermissionAttachment permission) {
    }

    record Limit(Plugin owner, int maximum) {
    }

    record Stat(Plugin owner, HudStatProvider provider) {
    }

    record Route(Plugin owner, UUID player, String id) {
    }
}

