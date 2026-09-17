/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.menu;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.api.HudTypes;
import sk.enorials.hud.compat.HudAttributes;
import sk.enorials.hud.menu.GraphicalHudMenu;
import sk.enorials.hud.menu.HudAdminMenu;
import sk.enorials.hud.menu.MenuGeometry;
import sk.enorials.hud.menu.NpcSocialMenus;
import sk.enorials.hud.theme.HudTheme;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapScreenPosition;
import sk.enorials.minimap.map.MinimapShape;
import sk.enorials.minimap.map.MinimapSize;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.icon.MarkerIcons;
import sk.enorials.minimap.map.marker.MarkerCategory;
import sk.enorials.minimap.map.marker.MarkerMinimapLayer;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.minimap.menu.EnomapMenuListener;
import sk.enorials.minimap.quest.BetonQuestMarkers;
import sk.enorials.minimap.radar.RadarCategory;
import sk.enorials.partyhud.invite.PartyInvite;
import sk.enorials.partyhud.party.Party;
import sk.enorials.partyhud.patch.FriendRequestBridge;

public final class HudMenu
implements Listener {
    private final EnorialsHUD plugin;
    private final GraphicalHudMenu graphical;
    private final HudAdminMenu admin;
    private final Map<UUID, Input> inputs = new ConcurrentHashMap<UUID, Input>();
    private final Map<UUID, Draft> drafts = new HashMap<UUID, Draft>();
    private static final String[] SIZES = new String[]{"Small", "Medium", "Large"};
    private static final String[] FRAMES = new String[]{"Enorials", "Frostbound", "Corsair", "Inferno", "Wildwood", "Astral"};
    private static final String[] FRAME_CHOICES = new String[]{"Follow HUD theme", "Enorials", "Frostbound", "Corsair", "Inferno", "Wildwood", "Astral"};
    private static final String[] LIGHT = new String[]{"Natural", "Bright", "Soft"};
    private static final String[] RANGES = new String[]{"64", "128", "256", "512", "1024", "2048", "Unlimited"};

    public HudMenu(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
        this.graphical = new GraphicalHudMenu(enorialsHUD);
        this.admin = new HudAdminMenu(enorialsHUD, this);
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
    }

    public void close() {
        this.admin.close();
        this.inputs.clear();
        this.drafts.clear();
        this.graphical.close();
        HandlerList.unregisterAll((Listener)this);
    }

    public boolean graphicalOpen(Player player) {
        return this.graphical.isOpen(player);
    }

    public boolean inputOpen(Player player) {
        return this.inputs.containsKey(player.getUniqueId());
    }

    public void trading(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.trade.use") || this.plugin.trades() == null) {
            return;
        }
        if (!this.plugin.cfg().getBoolean("trading.enabled", true)) {
            player.sendMessage((Component)Component.text((String)"Trading is disabled.", (TextColor)NamedTextColor.RED));
            return;
        }
        this.page(player, "Trading", List.of(this.b("ui:members", "Party players", () -> this.tradePlayers(player, true)), this.b("ui:player", "Online players", () -> this.tradePlayers(player, false))), () -> this.open(player));
    }

    public void tradePlayers(Player player, boolean bl) {
        if (!HudAccess.check((CommandSender)player, "enorials.trade.use") || this.plugin.trades() == null) {
            return;
        }
        Party party = this.plugin.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        for (Player object : this.plugin.trades().nearby(player)) {
            if (bl && (party == null || !party.isMember(object.getUniqueId()))) continue;
            UUID uUID = object.getUniqueId();
            arrayList.add(this.b("head:" + String.valueOf(uUID), object.getName(), () -> {
                Party party = this.plugin.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
                if (bl && (party == null || !party.isMember(uUID))) {
                    this.tradePlayers(player, true);
                    return;
                }
                if (this.plugin.trades().sendRequest(player, Bukkit.getPlayer((UUID)uUID))) {
                    player.closeInventory();
                } else {
                    this.tradePlayers(player, bl);
                }
            }));
        }
        for (NpcSocialMenus.Target target : NpcSocialMenus.targets(this.plugin, player)) {
            if (bl && (party == null || !party.isMember(target.npc().getUniqueId()))) continue;
            arrayList.add(this.b("head:" + String.valueOf(target.npc().getUniqueId()), target.npc().getName() + "\nNPC", () -> NpcSocialMenus.request(this.plugin, player, target, true)));
        }
        Object object = bl && party == null ? "You are not in a party" : "No players within " + (int)this.plugin.trades().maxDistance() + " blocks";
        this.graphical.open(player, bl ? "Trade - Party" : "Trade - Online", arrayList, () -> this.trading(player), (String)object, () -> this.tradePlayers(player, bl));
    }

    static int iconIndex(String string) {
        return switch (string) {
            case "hud" -> 0;
            case "map" -> 1;
            case "party" -> 2;
            case "effects" -> 3;
            case "markers" -> 4;
            case "settings" -> 5;
            case "friends" -> 6;
            case "back" -> 7;
            case "save" -> 8;
            case "close" -> 9;
            case "add" -> 10;
            case "minus" -> 11;
            case "edit" -> 12;
            case "unlock" -> 13;
            case "lock" -> 14;
            default -> 5;
        };
    }

    private GraphicalHudMenu.Button b(String string, String string2, Runnable runnable) {
        return new GraphicalHudMenu.Button(string, string2, runnable);
    }

    private GraphicalHudMenu.Button action(String string, String string2, Runnable runnable) {
        return this.b(string, string2, runnable).as(MenuGeometry.Kind.ACTION);
    }

    private void page(Player player, String string, List<GraphicalHudMenu.Button> list, Runnable runnable) {
        this.graphical.open(player, string, list, runnable);
    }

    private void livePage(Player player, String string, List<GraphicalHudMenu.Button> list, Runnable runnable, Runnable runnable2) {
        this.graphical.open(player, string, list, runnable, "Nothing here yet", runnable2);
    }

    private boolean pref(Player player, String string) {
        return this.plugin.storage().preference(player.getUniqueId(), string, true);
    }

    private String state(boolean bl) {
        return bl ? "On" : "Off";
    }

    private GraphicalHudMenu.Button toggle(String string, String string2, boolean bl, Runnable runnable) {
        return this.b(string, string2 + "\n" + this.state(bl), runnable).as(MenuGeometry.Kind.TOGGLE);
    }

    private void setPref(Player player, String string, Runnable runnable) {
        this.plugin.storage().setPreference(player.getUniqueId(), string, !this.pref(player, string));
        runnable.run();
    }

    public void open(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.menu.use")) {
            return;
        }
        this.inputs.remove(player.getUniqueId());
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        arrayList.add(this.b("party", "Party", () -> this.party(player)));
        arrayList.add(this.b("map", "Minimap", () -> this.map(player)));
        arrayList.add(this.b("hud", "HUD", () -> this.hud(player)));
        arrayList.add(this.b("gps", "GPS", () -> this.gps(player)));
        arrayList.add(this.action("ui:trading", "Trading", () -> this.trading(player)));
        if (this.hasQuests()) {
            arrayList.add(this.action("atlas:quests", "Quests", () -> this.quests(player)));
        }
        arrayList.add(this.action("atlas:customization", "Customization", () -> this.customization(player)));
        if (HudAccess.has((CommandSender)player, "enorials.hud.admin")) {
            arrayList.add(this.action("atlas:admin", "Admin", () -> this.admin.open(player)));
        }
        this.page(player, "ENORIALS", arrayList, null);
    }

    private boolean hasQuests() {
        return Bukkit.getPluginManager().isPluginEnabled("BetonQuest") && this.plugin.betonQuest() != null;
    }

    public void customization(Player player) {
        this.page(player, "Customization", List.of(this.b("atlas:player", "Player", () -> this.playerCustom(player)), this.b("atlas:party", "Party", () -> this.partyPanel(player)), this.b("atlas:minimap", "Minimap", () -> this.mapCustom(player)), this.b("atlas:navigation", "Navigation", () -> this.navigationCustom(player)), this.b("theme:themes", "Themes", () -> this.themes(player)), this.b("settings", "Hotbar", () -> this.interfaceCustom(player))), () -> this.open(player));
    }

    public void interfaceCustom(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.hud.use")) {
            return;
        }
        String string = "themed-hotbar";
        this.page(player, "Hotbar", List.of(this.b("settings", "Hotbar\n" + (this.pref(player, string) ? "Theme" : "Vanilla"), () -> {
            this.plugin.storage().setPreference(player.getUniqueId(), string, !this.pref(player, string));
            this.plugin.renderer().refreshTheme(player);
            this.interfaceCustom(player);
        }).as(MenuGeometry.Kind.TOGGLE)), () -> this.customization(player));
    }

    public void themes(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.hud.use")) {
            return;
        }
        HudTheme hudTheme = this.plugin.storage().theme(player.getUniqueId());
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        for (HudTheme hudTheme2 : HudTheme.values()) {
            arrayList.add(this.b("theme:" + hudTheme2.id(), hudTheme2.title() + (hudTheme2 == hudTheme ? "\nSelected" : ""), () -> {
                this.plugin.api().setSetting(player, "hud.theme", hudTheme2.id());
                this.themes(player);
            }));
        }
        this.page(player, "Themes", arrayList, () -> this.customization(player));
    }

    public void playerCustom(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.hud.use")) {
            return;
        }
        this.livePage(player, "Player display", List.of(this.toggle("hud", "Show HUD", this.pref(player, "hud"), () -> this.setPref(player, "hud", () -> this.playerCustom(player))), this.b("effects", "Effects", () -> this.effects(player)), this.action("edit", "Refresh avatar", () -> this.plugin.hudCommand().onCommand((CommandSender)player, (Command)this.plugin.getCommand("enohud"), "enohud", new String[]{"avatarrefresh"}))), () -> this.customization(player), () -> this.playerCustom(player));
    }

    public void navigationCustom(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.gps.use") || this.plugin.gps() == null) {
            return;
        }
        this.page(player, "Navigation display", List.of(this.b("atlas:navigation", "HUD options", () -> this.plugin.gps().gui().openOptions(player)), this.b("gps", "Navigation mode", () -> this.plugin.gps().gui().openMode(player))), () -> this.customization(player));
    }

    public void hud(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.hud.use")) {
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        arrayList.add(this.b("head:" + String.valueOf(player.getUniqueId()), player.getName() + "\nHP " + (int)Math.ceil(player.getHealth()) + "/" + (int)player.getAttribute(HudAttributes.MAX_HEALTH).getValue(), null));
        if (Bukkit.getPluginCommand((String)"spells") != null || Bukkit.getPluginCommand((String)"class") != null) {
            arrayList.add(this.b("atlas:player", "Character", () -> this.character(player)));
        }
        this.page(player, "Player", arrayList, () -> this.open(player));
    }

    public void quests(Player player) {
        if (!this.hasQuests()) {
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        arrayList.add(this.b("atlas:accepted", "Accepted quests", () -> this.plugin.gps().gui().openQuestRoutes(player)));
        if (this.plugin.cfg().getBoolean("quest-log.enabled", true)) {
            arrayList.add(this.toggle("atlas:quests", "Quest log", this.plugin.storage().preference(player.getUniqueId(), "quest-log", this.plugin.cfg().getBoolean("quest-log.default-visible", true)), () -> {
                HudAccess.require((CommandSender)player, "enorials.hud.questlog");
                this.plugin.storage().setPreference(player.getUniqueId(), "quest-log", !this.plugin.storage().preference(player.getUniqueId(), "quest-log", this.plugin.cfg().getBoolean("quest-log.default-visible", true)));
                this.quests(player);
            }));
            arrayList.add(this.b("ui:size", "Quest log size\n" + this.plugin.storage().questLogSize(player.getUniqueId()) + "%", () -> this.questLogSize(player)));
        }
        if (this.plugin.cfg().getBoolean("conversations.enabled", true)) {
            arrayList.add(this.toggle("atlas:quest_settings", "NPC dialogues", this.plugin.storage().preference(player.getUniqueId(), "conversations", this.plugin.cfg().getBoolean("conversations.default-visible", true)), () -> {
                HudAccess.require((CommandSender)player, "enorials.hud.conversations");
                this.plugin.storage().setPreference(player.getUniqueId(), "conversations", !this.plugin.storage().preference(player.getUniqueId(), "conversations", this.plugin.cfg().getBoolean("conversations.default-visible", true)));
                this.quests(player);
            }));
        }
        arrayList.add(this.toggle("marker:quest_questionmark", "Quest markers", this.pref(player, "betonquest-markers"), () -> {
            HudAccess.require((CommandSender)player, "enorials.minimap.quests");
            this.plugin.storage().setPreference(player.getUniqueId(), "betonquest-markers", !this.pref(player, "betonquest-markers"));
            BetonQuestMarkers betonQuestMarkers = BetonQuestMarkers.get();
            if (betonQuestMarkers != null) {
                betonQuestMarkers.refresh(player);
            }
            this.quests(player);
        }));
        if (this.plugin.betonQuest().questLog() != null && this.plugin.betonQuest().questLog().tracking(player) || this.plugin.gps().navigation().hasActiveNavigation(player)) {
            arrayList.add(this.action("close", "Stop tracking", () -> {
                this.plugin.gps().navigation().stopNavigation(player, "manual");
                this.quests(player);
            }));
        }
        this.page(player, "Quests", arrayList, () -> this.open(player));
    }

    private void questLogSize(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.hud.questlog")) {
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        for (int n : new int[]{50, 75, 100, 125}) {
            arrayList.add(this.action("ui:size", n + "%" + (this.plugin.storage().questLogSize(player.getUniqueId()) == n ? "\nSelected" : ""), () -> {
                this.plugin.storage().setQuestLogSize(player.getUniqueId(), n);
                this.questLogSize(player);
            }));
        }
        this.page(player, "Quest log size", arrayList, () -> this.quests(player));
    }

    public void effects(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.hud.effects")) {
            return;
        }
        this.page(player, "Effects", List.of(this.toggle("effects", "Icons and timers", this.pref(player, "effects"), () -> this.setPref(player, "effects", () -> this.effects(player)))), () -> this.playerCustom(player));
    }

    public void character(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.hud.use")) {
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        for (String[] stringArray : List.of(new String[]{"Class", "class"}, new String[]{"Spells", "spells"}, new String[]{"Talents", "talents"}, new String[]{"Stats", "stats"}, new String[]{"Race", "race"})) {
            if (Bukkit.getPluginCommand((String)stringArray[1]) == null) continue;
            arrayList.add(this.b("effects", stringArray[0], () -> player.performCommand(stringArray[1])));
        }
        this.page(player, "Character", arrayList, () -> this.open(player));
    }

    public void map(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.minimap.use")) {
            return;
        }
        this.page(player, "Minimap", List.of(this.action("map", "Fullscreen", () -> this.plugin.mapCommand(player, "fullscreen")), this.b("markers", "Markers", () -> this.markers(player))), () -> this.open(player));
    }

    public void mapCustom(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.minimap.use")) {
            return;
        }
        this.page(player, "Minimap display", List.of(this.b("map", "Display", () -> this.mapDisplay(player)), this.b("settings", "Map layout", () -> this.mapLayout(player)), this.b("edit", "Appearance", () -> this.appearance(player)), this.b("effects", "Mob tracking", () -> this.radar(player)), this.b("markers", "Marker display", () -> this.markerDisplay(player)), this.b("save", "Presets", () -> this.presets(player))), () -> this.customization(player));
    }

    private void mapDisplay(Player player) {
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        arrayList.add(this.toggle("map", "Show map", minimap != null, () -> {
            this.plugin.mapCommand(player, minimap == null ? "enable" : "disable");
            this.mapDisplay(player);
        }));
        if (minimap != null) {
            arrayList.add(this.toggle("markers", "Coordinates", minimap.coordinatesEnabled(), () -> {
                minimap.coordinatesEnabled(!minimap.coordinatesEnabled());
                this.changed(player, minimap);
                this.mapDisplay(player);
            }));
            arrayList.add(this.toggle("effects", "Combat zoom", minimap.combatZoomEnabled(), () -> {
                minimap.combatZoomEnabled(!minimap.combatZoomEnabled());
                this.changed(player, minimap);
                this.mapDisplay(player);
            }));
        }
        this.livePage(player, "Map display", arrayList, () -> this.mapCustom(player), () -> this.mapDisplay(player));
    }

    private Minimap requireMap(Player player) {
        Minimap minimap = this.plugin.getPlayerMinimap(player);
        if (minimap == null) {
            this.plugin.mapCommand(player, "enable");
            minimap = this.plugin.getPlayerMinimap(player);
        }
        return minimap;
    }

    private void changed(Player player, Minimap minimap) {
        minimap.respawn(this.plugin.minimap());
        this.plugin.minimap().queueMinimapSave(minimap);
    }

    public void mapLayout(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.minimap.use")) {
            return;
        }
        Minimap minimap = this.requireMap(player);
        if (minimap == null) {
            return;
        }
        this.page(player, "Map layout", List.of(this.b("map", "Zoom\n" + (new String[]{"Farthest", "Far", "Near"})[minimap.zoomLevel()], () -> this.select(player, "Map zoom", new String[]{"Near", "Far", "Farthest"}, 2 - minimap.zoomLevel(), n -> {
            minimap.zoomLevel(2 - n);
            this.changed(player, minimap);
        }, () -> this.mapLayout(player))), this.b("map", "Map size\n" + SIZES[minimap.size().ordinal()], () -> this.select(player, "Map size", SIZES, minimap.size().ordinal(), n -> {
            minimap.size(MinimapSize.values()[n]);
            this.changed(player, minimap);
        }, () -> this.mapLayout(player))), this.b("map", "Shape\n" + (minimap.shape() == MinimapShape.CIRCLE ? "Circle" : "Square"), () -> this.select(player, "Shape", new String[]{"Circle", "Square"}, minimap.shape() == MinimapShape.CIRCLE ? 0 : 1, n -> {
            minimap.shape(n == 0 ? MinimapShape.CIRCLE : MinimapShape.SQUARE);
            this.changed(player, minimap);
        }, () -> this.mapLayout(player))), this.b("settings", "Position\n" + (minimap.screenPosition() == MinimapScreenPosition.LEFT ? "Left" : "Right"), () -> this.select(player, "Position", new String[]{"Left", "Right"}, minimap.screenPosition() == MinimapScreenPosition.LEFT ? 0 : 1, n -> {
            minimap.screenPosition(n == 0 ? MinimapScreenPosition.LEFT : MinimapScreenPosition.RIGHT);
            this.changed(player, minimap);
        }, () -> this.mapLayout(player)))), () -> this.mapCustom(player));
    }

    private void select(Player player, String string, String[] stringArray, int n, IntConsumer intConsumer, Runnable runnable) {
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        for (int i = 0; i < stringArray.length; ++i) {
            int n2 = i;
            arrayList.add(this.b(i == n ? "save" : "settings", stringArray[i] + (i == n ? "\nSelected" : ""), () -> {
                intConsumer.accept(n2);
                this.select(player, string, stringArray, n2, intConsumer, runnable);
            }).as(MenuGeometry.Kind.ACTION));
        }
        this.page(player, string, arrayList, runnable);
    }

    public void appearance(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.minimap.use")) {
            return;
        }
        Minimap minimap = this.requireMap(player);
        if (minimap == null) {
            return;
        }
        this.page(player, "Appearance", List.of(this.b("edit", "Frame style\n" + (minimap.frameFollowsTheme() ? "HUD theme" : FRAMES[minimap.frameStyle()]), () -> this.select(player, "Frame style", FRAME_CHOICES, minimap.frameFollowsTheme() ? 0 : minimap.frameStyle() + 1, n -> {
            if (n == 0) {
                minimap.frameFollowsTheme(true);
            } else {
                minimap.frameStyle(n - 1);
            }
            this.changed(player, minimap);
        }, () -> this.appearance(player))), this.b("map", "Map colors\n" + LIGHT[minimap.colorProfile()], () -> this.select(player, "Map colors", LIGHT, minimap.colorProfile(), n -> {
            minimap.colorProfile(n);
            this.changed(player, minimap);
        }, () -> this.appearance(player)))), () -> this.mapCustom(player));
    }

    public void presets(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.minimap.use")) {
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        for (int i = 1; i <= 3; ++i) {
            int n = i;
            arrayList.add(this.action("save", "Save preset " + i, () -> {
                this.plugin.mapCommand(player, "preset " + n + " save");
                this.presets(player);
            }));
            arrayList.add(this.action("map", "Load preset " + i, () -> {
                this.plugin.mapCommand(player, "preset " + n + " load");
                this.presets(player);
            }));
        }
        this.page(player, "Map presets", arrayList, () -> this.mapCustom(player));
    }

    public void radar(Player player) {
        Minimap minimap = this.requireMap(player);
        if (minimap == null) {
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        arrayList.add(this.toggle("effects", "All mobs", minimap.radarMasterEnabled(), () -> {
            minimap.radarMasterEnabled(!minimap.radarMasterEnabled());
            this.changed(player, minimap);
            this.radar(player);
        }));
        for (RadarCategory radarCategory : RadarCategory.values()) {
            String string = radarCategory == RadarCategory.AGGRESSIVE ? "close" : (radarCategory == RadarCategory.NEUTRAL ? "minus" : "save");
            arrayList.add(this.toggle(string, switch (radarCategory) {
                default -> throw new IncompatibleClassChangeError();
                case RadarCategory.AGGRESSIVE -> "Hostile";
                case RadarCategory.NEUTRAL -> "Neutral";
                case RadarCategory.FRIENDLY -> "Friendly / NPC";
            }, minimap.radarEnabled(radarCategory), () -> {
                minimap.radarEnabled(radarCategory, !minimap.radarEnabled(radarCategory));
                this.changed(player, minimap);
                this.radar(player);
            }));
        }
        this.page(player, "Mob tracking", arrayList, () -> this.mapCustom(player));
    }

    public void party(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.party.use")) {
            return;
        }
        this.page(player, "Party", List.of(this.b("party", "Manage party", () -> this.partyManage(player)), this.b("friends", "Friends", () -> this.friends(player)), this.b("gps", "Find party", () -> {
            if (this.plugin.gps() != null) {
                this.plugin.gps().gui().openParty(player);
            }
        })), () -> this.open(player));
    }

    private void partyPanel(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.party.hud")) {
            return;
        }
        this.livePage(player, "Party HUD", List.of(this.b("settings", "Panel size\n" + this.displaySize(this.plugin.storage().partySize(player.getUniqueId())), () -> this.partySize(player)), this.toggle("party_hud", "Show party", this.pref(player, "party"), () -> this.setPref(player, "party", () -> this.partyPanel(player)))), () -> this.customization(player), () -> this.partyPanel(player));
    }

    private void partyManage(Player player) {
        Party party = this.plugin.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        if (party == null) {
            arrayList.add(this.action("lock", "Create private", () -> {
                this.plugin.partyCommand(player, "create");
                this.party(player);
            }));
            arrayList.add(this.action("unlock", "Create public", () -> {
                this.plugin.partyCommand(player, "create open");
                this.party(player);
            }));
            arrayList.add(this.b("party", "Join party", () -> this.openParties(player)));
        } else {
            arrayList.add(this.b("party", "Members\n" + party.getMembers().size() + " players", () -> this.members(player)));
            arrayList.add(this.b("add", "Invite player", () -> this.invite(player)));
            arrayList.add(this.action("minus", "Leave party", () -> {
                this.plugin.partyCommand(player, "leave");
                this.party(player);
            }));
        }
        this.plugin.party().invites().getInvite(player.getUniqueId()).filter(partyInvite -> !partyInvite.isExpired()).ifPresent(partyInvite -> {
            String string = Bukkit.getOfflinePlayer((UUID)partyInvite.getLeader()).getName();
            if (string != null) {
                arrayList.add(this.b("friends", "Invitation\n" + string, () -> this.plugin.party().invites().showPopup(player, (PartyInvite)partyInvite, true)));
            }
        });
        this.page(player, "Manage party", arrayList, () -> this.party(player));
    }

    private String displaySize(String string) {
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    public void partySize(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.party.hud")) {
            return;
        }
        this.select(player, "Party size", SIZES, List.of("small", "medium", "large").indexOf(this.plugin.storage().partySize(player.getUniqueId())), n -> this.plugin.storage().setPartySize(player.getUniqueId(), SIZES[n].toLowerCase(Locale.ROOT)), () -> this.partyPanel(player));
    }

    public void openParties(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.party.use")) {
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        for (Party party : this.plugin.getPartyService().getOpenParties()) {
            Player player2 = Bukkit.getPlayer((UUID)party.getLeader());
            if (player2 == null) continue;
            arrayList.add(this.b("head:" + String.valueOf(player2.getUniqueId()), player2.getName(), () -> {
                this.plugin.partyCommand(player, "join " + player2.getName());
                this.party(player);
            }));
        }
        this.page(player, "Open parties", arrayList, () -> this.partyManage(player));
    }

    private void members(Player player) {
        Party party = this.plugin.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
        if (party == null) {
            this.party(player);
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        for (UUID uUID : party.getMembers()) {
            arrayList.add(this.b("head:" + String.valueOf(uUID), this.plugin.api().member(uUID).name() + (uUID.equals(party.getLeader()) ? "\nLeader" : ""), () -> this.member(player, uUID)));
        }
        this.page(player, "Party members", arrayList, () -> this.partyManage(player));
    }

    private void member(Player player, UUID uUID) {
        Party party = this.plugin.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
        Player player2 = this.plugin.api().resolveMember(uUID);
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        arrayList.add(this.b("head:" + String.valueOf(uUID), this.plugin.api().member(uUID).name() + "\n" + (party != null && party.getLeader().equals(uUID) ? "Leader" : "Member"), null));
        if (party != null && party.getLeader().equals(player.getUniqueId()) && !uUID.equals(player.getUniqueId()) && player2 != null) {
            if (!this.plugin.api().member(uUID).npc()) {
                arrayList.add(this.action("party", "Make leader", () -> {
                    this.plugin.api().promote(player, uUID);
                    this.members(player);
                }));
            }
            arrayList.add(this.action("minus", "Remove member", () -> {
                this.plugin.api().kick(player, uUID);
                this.members(player);
            }));
        }
        this.page(player, this.plugin.api().member(uUID).name(), arrayList, () -> this.members(player));
    }

    private void invite(Player player) {
        this.askName(player, "Enter the player name to invite.", string -> {
            this.plugin.partyCommand(player, "invite " + string);
            this.party(player);
        }, () -> this.party(player));
    }

    public void gps(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.gps.use")) {
            return;
        }
        if (this.plugin.gps() != null) {
            this.plugin.gps().gui().openMain(player);
        }
    }

    public void friends(Player player) {
        String string;
        if (!HudAccess.check((CommandSender)player, "enorials.party.friends")) {
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        arrayList.add(this.b("add", "Add friend", () -> this.addFriend(player)));
        for (UUID uUID : FriendRequestBridge.incoming(player.getUniqueId())) {
            string = this.plugin.party().friends().name(uUID);
            arrayList.add(this.b("friends", "Request\n" + string, () -> this.page(player, string, List.of(this.action("save", "Accept", () -> {
                this.plugin.partyCommand(player, "friend accept " + string);
                this.friends(player);
            }), this.action("close", "Decline", () -> {
                this.plugin.partyCommand(player, "friend deny " + string);
                this.friends(player);
            })), () -> this.friends(player))));
        }
        for (UUID uUID : this.plugin.party().friends().getFriends(player.getUniqueId())) {
            string = this.plugin.party().friends().name(uUID);
            HudTypes.Member member = this.plugin.api().member(uUID);
            arrayList.add(this.b("head:" + String.valueOf(uUID), string + "\n" + (member.npc() ? "NPC" : (member.online() ? "Online" : "Offline")), () -> this.friendDetails(player, uUID, string)));
        }
        this.page(player, "Friends", arrayList, () -> this.party(player));
    }

    private void addFriend(Player player) {
        List<NpcSocialMenus.Target> list = NpcSocialMenus.targets(this.plugin, player);
        Runnable runnable = () -> this.askName(player, "Enter the friend name.", string -> {
            this.plugin.partyCommand(player, "friend add " + string);
            this.friends(player);
        }, () -> this.friends(player));
        if (list.isEmpty()) {
            runnable.run();
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        arrayList.add(this.b("add", "Player name", runnable));
        for (NpcSocialMenus.Target target : list) {
            arrayList.add(this.b("head:" + String.valueOf(target.npc().getUniqueId()), target.npc().getName() + "\nNPC", () -> NpcSocialMenus.request(this.plugin, player, target, false)));
        }
        this.page(player, "Add friend", arrayList, () -> this.friends(player));
    }

    private void friendDetails(Player player, UUID uUID, String string) {
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        if (!this.plugin.api().member(uUID).npc()) {
            arrayList.add(this.action("party", "Invite to party", () -> {
                this.plugin.partyCommand(player, "invite " + string);
                this.friends(player);
            }));
        }
        arrayList.add(this.action("minus", "Remove friend", () -> {
            this.plugin.party().friends().removeFriend(player.getUniqueId(), uUID);
            this.friends(player);
        }));
        this.page(player, string, arrayList, () -> this.friends(player));
    }

    private void askName(Player player, String string2, Consumer<String> consumer, Runnable runnable) {
        this.ask(player, string2, string -> {
            if (!string.matches("[A-Za-z0-9_]{1,16}")) {
                throw new IllegalArgumentException("Use 1 to 16 letters, digits or underscores.");
            }
            consumer.accept((String)string);
        }, runnable);
    }

    void ask(Player player, String string, Consumer<String> consumer, Runnable runnable) {
        player.closeInventory();
        Input input = new Input(consumer, runnable);
        this.inputs.put(player.getUniqueId(), input);
        player.sendMessage(Component.text((String)(string + " Open chat (T). Type cancel to go back.")).color((TextColor)NamedTextColor.AQUA));
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (this.inputs.remove(player.getUniqueId(), input) && player.isOnline()) {
                player.sendMessage(Component.text((String)"Input timed out.").color((TextColor)NamedTextColor.GRAY));
                runnable.run();
            }
        }, 2400L);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void chat(AsyncChatEvent asyncChatEvent) {
        UUID uUID = asyncChatEvent.getPlayer().getUniqueId();
        Input input = this.inputs.get(uUID);
        if (input == null) {
            return;
        }
        asyncChatEvent.setCancelled(true);
        String string = PlainTextComponentSerializer.plainText().serialize(asyncChatEvent.message()).trim();
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            Player player = asyncChatEvent.getPlayer();
            if (!player.isOnline() || !this.inputs.remove(uUID, input)) {
                return;
            }
            if (Set.of("cancel", "zru\u00c4\u201a\u00e2\u20ac\u017e\u0102\u201e\u00e2\u20ac\u00a6\u00c4\u201a\u00e2\u20ac\u0105\u0102\u02d8\u00e2\u201a\u00ac\u00cb\u2021i\u00c4\u201a\u00e2\u20ac\u017e\u0102\u201e\u00e2\u20ac\u00a6\u00c4\u201a\u00e2\u20ac\u017e\u0102\u02d8\u00e2\u201a\u00ac\u0139\u013e", "zrusit").contains(string.toLowerCase(Locale.ROOT))) {
                input.back.run();
                return;
            }
            try {
                input.accept.accept(string);
            }
            catch (Exception exception) {
                this.error(player, exception);
                this.ask(player, "Try entering the value again.", input.accept, input.back);
            }
        });
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        this.inputs.remove(playerQuitEvent.getPlayer().getUniqueId());
        this.drafts.remove(playerQuitEvent.getPlayer().getUniqueId());
    }

    public void markers(Player player) {
        if (!HudAccess.check((CommandSender)player, "enorials.minimap.use")) {
            return;
        }
        Minimap minimap = this.requireMap(player);
        if (minimap == null) {
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        arrayList.add(this.b("add", "Create marker", () -> {
            this.drafts.put(player.getUniqueId(), new Draft(player));
            this.draft(player);
        }));
        arrayList.add(this.b("markers", "My markers", () -> this.markerList(player)));
        this.page(player, "Markers", arrayList, () -> this.map(player));
    }

    private void markerDisplay(Player player) {
        Minimap minimap = this.requireMap(player);
        if (minimap == null) {
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        arrayList.add(this.b("settings", "Categories", () -> this.categories(player)));
        arrayList.add(this.b("marker:waypoint", "Marker size\n" + this.plugin.storage().markerSize(player.getUniqueId()) + "%", () -> this.markerSize(player)));
        arrayList.add(this.toggle("marker:home", "Scale markers", this.pref(player, "marker-scaling"), () -> {
            this.plugin.storage().setPreference(player.getUniqueId(), "marker-scaling", !this.pref(player, "marker-scaling"));
            this.changed(player, minimap);
            this.markerDisplay(player);
        }));
        this.page(player, "Marker display", arrayList, () -> this.mapCustom(player));
    }

    private void markerSize(Player player) {
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        for (int n : new int[]{75, 100, 125, 150}) {
            arrayList.add(this.action("marker:waypoint", n + "%" + (this.plugin.storage().markerSize(player.getUniqueId()) == n ? "\nSelected" : ""), () -> {
                this.plugin.storage().setMarkerSize(player.getUniqueId(), n);
                Minimap minimap = this.plugin.getPlayerMinimap(player);
                if (minimap != null) {
                    this.changed(player, minimap);
                }
                this.markerSize(player);
            }));
        }
        this.page(player, "Marker size", arrayList, () -> this.markerDisplay(player));
    }

    private void markerList(Player player) {
        Minimap minimap = this.requireMap(player);
        if (minimap == null) {
            return;
        }
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        if (minimap.getDeathPoint() != null) {
            arrayList.add(this.b("marker:death", "Death location", () -> this.deathMarker(player)));
        }
        for (Map.Entry<String, SecondaryMinimapLayer> entry : minimap.secondaryLayers().entrySet()) {
            if (!HudMenu.owned(entry.getKey(), entry.getValue())) continue;
            String string = entry.getKey();
            arrayList.add(this.b("marker:" + ((MinimapIconRenderer)entry.getValue().getRenderer()).icon().key(), string, () -> this.marker(player, string)));
        }
        this.page(player, "My markers", arrayList, () -> this.markers(player));
    }

    private void deathMarker(Player player) {
        this.page(player, "Death location", List.of(this.action("minus", "Delete marker", () -> this.page(player, "Delete death marker?", List.of(this.action("close", "Yes, delete", () -> {
            try {
                Minimap minimap = this.requireMap(player);
                this.plugin.minimap().playerDataStorage().clearDeathLocation(player.getUniqueId());
                minimap.resetDeathPoint(this.plugin.minimap());
                this.markerList(player);
            }
            catch (Exception exception) {
                this.error(player, exception);
            }
        }), this.action("save", "Keep marker", () -> this.deathMarker(player))), () -> this.deathMarker(player)))), () -> this.markerList(player));
    }

    private static boolean owned(String string, SecondaryMinimapLayer secondaryMinimapLayer) {
        return secondaryMinimapLayer instanceof MarkerMinimapLayer && !Set.of("player", "death_point").contains(string) && !string.startsWith("__") && !EnomapMenuListener.isPublicLayerKey(string);
    }

    private String category(MarkerCategory markerCategory) {
        return switch (markerCategory) {
            default -> throw new IncompatibleClassChangeError();
            case MarkerCategory.GENERAL -> "General";
            case MarkerCategory.HOME -> "Home";
            case MarkerCategory.WARPS -> "Warps";
            case MarkerCategory.PARTY -> "Party";
            case MarkerCategory.QUEST -> "Quests";
            case MarkerCategory.DANGER -> "Danger";
        };
    }

    private void categories(Player player) {
        EnomapMenuListener enomapMenuListener = this.plugin.minimap().menuListener();
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        for (MarkerCategory markerCategory : MarkerCategory.values()) {
            arrayList.add(this.toggle("markers", this.category(markerCategory), !enomapMenuListener.isCategoryHidden(player, markerCategory), () -> {
                try {
                    enomapMenuListener.toggleCategoryVisibility(player, markerCategory);
                }
                catch (Exception exception) {
                    this.error(player, exception);
                }
                this.categories(player);
            }));
        }
        this.page(player, "Categories", arrayList, () -> this.markerDisplay(player));
    }

    private String[] icons() {
        return (String[])MarkerIcons.selectable(this.plugin.minimap().iconProvider()).toArray(String[]::new);
    }

    private void draft(Player player) {
        String[] stringArray;
        Draft draft = this.drafts.get(player.getUniqueId());
        if (draft == null) {
            this.markers(player);
            return;
        }
        if (draft.icon == null && (stringArray = this.icons()).length > 0) {
            draft.icon = stringArray[0];
        }
        this.page(player, "New marker", List.of(this.b("edit", "Name\n" + (draft.name.isBlank() ? "Enter name" : draft.name), () -> this.ask(player, "Enter a marker name (up to 32 characters).", string -> {
            HudMenu.validateName(string);
            draft.name = string;
            this.draft(player);
        }, () -> this.draft(player))), this.b("marker:" + draft.icon, "Marker icon", () -> this.selectIcon(player, draft.icon, string -> {
            draft.icon = string;
        }, () -> this.draft(player))), this.b("settings", "Details", () -> this.draftDetails(player)), this.b("save", "Create marker", () -> {
            try {
                HudMenu.validateName(draft.name);
                if (draft.icon == null) {
                    throw new IllegalArgumentException("Icons are unavailable.");
                }
                this.plugin.minimap().menuListener().markerWizard().create(player, draft.name, draft.icon, draft.category, draft.publicMarker, draft.range, draft.lifetime == null ? null : Long.valueOf(System.currentTimeMillis() + draft.lifetime * 60000L), draft.at);
                this.drafts.remove(player.getUniqueId());
                this.markerList(player);
            }
            catch (Exception exception) {
                this.error(player, exception);
                this.draft(player);
            }
        })), () -> this.markers(player));
    }

    private void draftDetails(Player player) {
        Draft draft = this.drafts.get(player.getUniqueId());
        if (draft == null) {
            this.markers(player);
            return;
        }
        this.page(player, "Marker details", List.of(this.b("settings", "Category\n" + this.category(draft.category), () -> this.selectCategory(player, draft.category, markerCategory -> {
            draft.category = markerCategory;
        }, () -> this.draftDetails(player))), this.toggle("friends", "Public marker", draft.publicMarker, () -> {
            draft.publicMarker = !draft.publicMarker;
            this.draftDetails(player);
        }), this.b("map", "Distance\n" + (String)(draft.range == null ? "Unlimited" : draft.range + " blocks"), () -> this.range(player, draft.range, n -> {
            draft.range = n;
        }, () -> this.draftDetails(player))), this.b("effects", "Lifetime\n" + (String)(draft.lifetime == null ? "Permanent" : draft.lifetime + " min"), () -> this.select(player, "Marker lifetime", new String[]{"Permanent", "30 min", "60 min", "120 min", "1 day"}, -1, n -> {
            draft.lifetime = n == 0 ? null : (new Long[]{30L, 60L, 120L, 1440L})[n - 1];
        }, () -> this.draftDetails(player))), this.b("edit", "Location X Z", () -> this.ask(player, "Enter X Z coordinates, or here for your location.", string -> {
            if (string.equalsIgnoreCase("here") || string.equalsIgnoreCase("tu")) {
                draft.at = player.getLocation().clone();
            } else {
                String[] stringArray = string.split("\\s+");
                if (stringArray.length != 2) {
                    throw new IllegalArgumentException("Separate X and Z with a space.");
                }
                double d = Double.parseDouble(stringArray[0]);
                double d2 = Double.parseDouble(stringArray[1]);
                if (!Double.isFinite(d) || !Double.isFinite(d2) || Math.abs(d) > 2.9999984E7 || Math.abs(d2) > 2.9999984E7) {
                    throw new IllegalArgumentException("Coordinates are outside the world.");
                }
                draft.at.setX(d);
                draft.at.setZ(d2);
            }
            this.draftDetails(player);
        }, () -> this.draftDetails(player)))), () -> this.draft(player));
    }

    private void selectCategory(Player player, MarkerCategory markerCategory, Consumer<MarkerCategory> consumer, Runnable runnable) {
        this.select(player, "Marker category", (String[])Arrays.stream(MarkerCategory.values()).map(this::category).toArray(String[]::new), markerCategory.ordinal(), n -> consumer.accept(MarkerCategory.values()[n]), runnable);
    }

    private void range(Player player, Integer n2, Consumer<Integer> consumer, Runnable runnable) {
        this.select(player, "Marker range", RANGES, Arrays.asList(RANGES).indexOf(n2 == null ? "Unlimited" : n2.toString()), n -> consumer.accept(n == RANGES.length - 1 ? null : Integer.valueOf(RANGES[n])), runnable);
    }

    private void marker(Player player, String string) {
        Minimap minimap = this.requireMap(player);
        if (minimap == null) {
            return;
        }
        SecondaryMinimapLayer secondaryMinimapLayer = minimap.secondaryLayers().get(string);
        if (!HudMenu.owned(string, secondaryMinimapLayer)) {
            this.markerList(player);
            return;
        }
        MarkerMinimapLayer markerMinimapLayer = (MarkerMinimapLayer)secondaryMinimapLayer;
        EnomapMenuListener enomapMenuListener = this.plugin.minimap().menuListener();
        this.page(player, string, List.of(this.toggle("markers", "Show marker", !enomapMenuListener.isMarkerHidden(player, string), () -> {
            try {
                enomapMenuListener.toggleMarkerVisibility(player, string);
            }
            catch (Exception exception) {
                this.error(player, exception);
            }
            this.marker(player, string);
        }), this.b("edit", "Rename", () -> this.ask(player, "Enter the new marker name.", string2 -> {
            HudMenu.validateName(string2);
            this.plugin.mapCommand(player, "marker rename " + HudMenu.quote(string) + " " + HudMenu.quote(string2));
            this.markerList(player);
        }, () -> this.marker(player, string))), this.b("markers", "Change icon", () -> this.selectIcon(player, null, string2 -> this.plugin.mapCommand(player, "marker set " + HudMenu.quote(string) + " icon " + HudMenu.quote(string2)), () -> this.marker(player, string))), this.b("settings", "Details", () -> this.markerDetails(player, string, markerMinimapLayer)), this.b("minus", "Delete marker", () -> this.page(player, "Delete marker?", List.of(this.action("close", "Yes, delete", () -> {
            this.plugin.mapCommand(player, "marker remove " + HudMenu.quote(string));
            this.markerList(player);
        }), this.b("back", "Keep marker", () -> this.marker(player, string))), () -> this.marker(player, string)))), () -> this.markerList(player));
    }

    private void markerDetails(Player player, String string, MarkerMinimapLayer markerMinimapLayer) {
        EnomapMenuListener enomapMenuListener = this.plugin.minimap().menuListener();
        this.page(player, "Marker details", List.of(this.b("settings", "Category\n" + this.category(markerMinimapLayer.getCategory()), () -> this.selectCategory(player, markerMinimapLayer.getCategory(), markerCategory -> this.plugin.mapCommand(player, "marker set " + HudMenu.quote(string) + " category " + markerCategory.name()), () -> this.marker(player, string))), this.toggle("friends", "Public marker", markerMinimapLayer.isPublic(), () -> {
            try {
                enomapMenuListener.toggleMarkerPublicity(player, string);
            }
            catch (Exception exception) {
                this.error(player, exception);
            }
            this.markerDetails(player, string, markerMinimapLayer);
        }), this.b("map", "Distance", () -> this.range(player, markerMinimapLayer.getMaxVisibleDistance(), n -> this.plugin.mapCommand(player, "marker set " + HudMenu.quote(string) + " range " + String.valueOf(n == null ? "off" : n)), () -> this.marker(player, string)))), () -> this.marker(player, string));
    }

    private void selectIcon(Player player, String string, Consumer<String> consumer, Runnable runnable) {
        String[] stringArray = this.icons();
        ArrayList<GraphicalHudMenu.Button> arrayList = new ArrayList<GraphicalHudMenu.Button>();
        for (String string2 : stringArray) {
            arrayList.add(this.b("marker:" + string2, HudMenu.markerLabel(string2) + (string2.equals(string) ? "\nSelected" : ""), () -> {
                consumer.accept(string2);
                this.selectIcon(player, string2, consumer, runnable);
            }).as(MenuGeometry.Kind.ACTION));
        }
        this.page(player, "Marker icon", arrayList, runnable);
    }

    private static String markerLabel(String string) {
        return MarkerIcons.label(string);
    }

    public GraphicalHudMenu graphical() {
        return this.graphical;
    }

    private static void validateName(String string) {
        if (!string.matches("[\\p{L}\\p{N}_ .-]{1,32}") || string.isBlank() || string.startsWith("__")) {
            throw new IllegalArgumentException("Use 1 to 32 letters, digits, spaces, dots or dashes.");
        }
    }

    private void error(Player player, Exception exception) {
        player.sendMessage(Component.text((String)Objects.toString(exception.getMessage(), "Could not apply the change.")).color((TextColor)NamedTextColor.RED));
    }

    private static String quote(String string) {
        return "\"" + string.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private record Input(Consumer<String> accept, Runnable back) {
    }

    private static class Draft {
        String name = "";
        String icon;
        MarkerCategory category = MarkerCategory.GENERAL;
        boolean publicMarker;
        Integer range = 512;
        Long lifetime;
        Location at;

        Draft(Player player) {
            this.at = player.getLocation().clone();
        }
    }
}

