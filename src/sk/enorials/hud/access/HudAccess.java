/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.permissions.Permission
 */
package sk.enorials.hud.access;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.gps.nav.target.CitizensNpcTarget;
import sk.enorials.gps.nav.target.FancyNpcTarget;
import sk.enorials.gps.nav.target.PlayerTarget;
import sk.enorials.gps.nav.target.ShopkeeperTarget;
import sk.enorials.gps.nav.target.StaticLocationTarget;
import sk.enorials.gps.nav.target.TargetLocator;
import sk.enorials.gps.nav.target.WorldSpawnTarget;
import sk.enorials.gps.nav.target.ZnpcsPlusNpcTarget;
import sk.enorials.hud.access.AccessManager;
import sk.enorials.hud.api.HudApiService;
import sk.enorials.hud.party.PartyMapTracker;
import sk.enorials.hud.util.PackHostingSetup;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.marker.MarkerMinimapLayer;
import sk.enorials.minimap.map.renderer.CoordinatesOverlayRenderer;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.minimap.map.renderer.SecondaryMinimapLayerRenderer;
import sk.enorials.minimap.quest.QuestMarkerLayer;
import sk.enorials.minimap.radar.RadarCategory;
import sk.enorials.minimap.radar.RadarLayer;

public final class HudAccess {
    public static final String ADMIN = "enorials.hud.admin";
    private static AccessManager manager;

    private HudAccess() {
    }

    static void install(AccessManager accessManager) {
        manager = accessManager;
    }

    public static boolean has(CommandSender commandSender, String string) {
        return HudAccess.raw(commandSender, ADMIN) || HudAccess.raw(commandSender, string);
    }

    /*
     * Unable to fully structure code
     */
    private static boolean raw(CommandSender var0, String var1_1) {
        if (var0.isPermissionSet(var1_1)) {
            return var0.hasPermission(var1_1);
        }
        if (!(var0 instanceof Player)) ** GOTO lbl-1000
        var3_2 = (Player)var0;
        if (HudAccess.manager != null) {
            v0 = HudAccess.manager.local(var3_2.getUniqueId(), var1_1);
        } else lbl-1000:
        // 2 sources

        {
            v0 = null;
        }
        var2_3 = v0;
        return var2_3 == null ? var0.hasPermission(var1_1) : var2_3.booleanValue();
    }

    public static boolean check(CommandSender commandSender, String string) {
        if (HudAccess.has(commandSender, string)) {
            return true;
        }
        commandSender.sendMessage(Component.text((String)("EnorialsHUD: Missing permission: " + string)).color((TextColor)NamedTextColor.RED));
        return false;
    }

    public static void require(CommandSender commandSender, String string) {
        if (!HudAccess.has(commandSender, string)) {
            throw new IllegalArgumentException("Missing permission: " + string);
        }
    }

    static boolean includes(String string, String string2, Set<String> set) {
        if (string.equals(string2)) {
            return true;
        }
        if (!set.add(string)) {
            return false;
        }
        Permission permission = Bukkit.getPluginManager().getPermission(string);
        if (permission == null) {
            return false;
        }
        for (Map.Entry entry : permission.getChildren().entrySet()) {
            if (!((Boolean)entry.getValue()).booleanValue() || !HudAccess.includes((String)entry.getKey(), string2, set)) continue;
            return true;
        }
        return false;
    }

    public static boolean hud(Player player) {
        return PackHostingSetup.ready(player) && HudAccess.has((CommandSender)player, "enorials.hud.use");
    }

    public static boolean map(Player player) {
        return PackHostingSetup.ready(player) && HudAccess.has((CommandSender)player, "enorials.minimap.use");
    }

    public static String radar(RadarCategory radarCategory) {
        return "enorials.minimap.radar." + (switch (radarCategory) {
            default -> throw new IncompatibleClassChangeError();
            case RadarCategory.AGGRESSIVE -> "hostile";
            case RadarCategory.NEUTRAL -> "neutral";
            case RadarCategory.FRIENDLY -> "friendly";
        });
    }

    public static boolean anyRadar(Player player) {
        for (RadarCategory radarCategory : RadarCategory.values()) {
            if (!HudAccess.has((CommandSender)player, HudAccess.radar(radarCategory))) continue;
            return true;
        }
        return false;
    }

    public static boolean layer(Player player, SecondaryMinimapLayer secondaryMinimapLayer) {
        if (!HudAccess.map(player)) {
            return false;
        }
        if (secondaryMinimapLayer instanceof HudApiService.MarkerLayer) {
            return HudAccess.has((CommandSender)player, "enorials.minimap.markers.use");
        }
        if (secondaryMinimapLayer instanceof QuestMarkerLayer) {
            return HudAccess.has((CommandSender)player, "enorials.minimap.quests");
        }
        if (secondaryMinimapLayer instanceof PartyMapTracker.Layer) {
            return HudAccess.has((CommandSender)player, "enorials.party.map");
        }
        if (secondaryMinimapLayer.getRenderer() instanceof CoordinatesOverlayRenderer) {
            return HudAccess.has((CommandSender)player, "enorials.minimap.coordinates");
        }
        if (secondaryMinimapLayer instanceof RadarLayer) {
            RadarLayer radarLayer = (RadarLayer)secondaryMinimapLayer;
            return HudAccess.has((CommandSender)player, HudAccess.radar(radarLayer.category()));
        }
        if (secondaryMinimapLayer instanceof MarkerMinimapLayer) {
            MarkerMinimapLayer markerMinimapLayer = (MarkerMinimapLayer)secondaryMinimapLayer;
            SecondaryMinimapLayerRenderer secondaryMinimapLayerRenderer = secondaryMinimapLayer.getRenderer();
            if (secondaryMinimapLayerRenderer instanceof MinimapIconRenderer) {
                MinimapIconRenderer minimapIconRenderer = (MinimapIconRenderer)secondaryMinimapLayerRenderer;
                if (minimapIconRenderer.rotateWithHolderYaw()) {
                    return true;
                }
                if (minimapIconRenderer.icon().key().equals("death")) {
                    return HudAccess.has((CommandSender)player, "enorials.minimap.markers.death");
                }
            }
            return HudAccess.has((CommandSender)player, markerMinimapLayer.isPublic() ? "enorials.minimap.markers.public" : "enorials.minimap.markers.use");
        }
        return true;
    }

    public static String setting(String string) {
        return switch (string) {
            case "hud.quest-log", "hud.quest-log-size" -> "enorials.hud.questlog";
            case "hud.conversations" -> "enorials.hud.conversations";
            case "effects.visible" -> "enorials.hud.effects";
            case "party.visible", "party.size" -> "enorials.party.hud";
            case "map.betonquest-markers" -> "enorials.minimap.quests";
            case "map.marker-size", "map.marker-scaling" -> "enorials.minimap.markers.use";
            case "map.coords" -> "enorials.minimap.coordinates";
            case "map.radar" -> "enorials.minimap.radar";
            default -> string.startsWith("map.") ? "enorials.minimap.use" : (string.startsWith("gps.") ? "enorials.gps.use" : "enorials.hud.use");
        };
    }

    public static String navigationNode(TargetLocator targetLocator, RouteDescriptor routeDescriptor, String string) {
        if ((string = Objects.requireNonNullElse(string, "")).startsWith("betonquest-quest:")) {
            return "enorials.gps.quests";
        }
        if (string.equals("hud-party")) {
            return "enorials.gps.party";
        }
        if (string.endsWith("-block")) {
            return "enorials.gps.track.block";
        }
        if (routeDescriptor != null) {
            return switch (routeDescriptor.type()) {
                default -> throw new IncompatibleClassChangeError();
                case RouteDescriptor.Type.PLAYER -> "enorials.gps.track.player";
                case RouteDescriptor.Type.MOB -> "enorials.gps.track.mob";
                case RouteDescriptor.Type.NPC -> "enorials.gps.track.npc";
                case RouteDescriptor.Type.POI -> "enorials.gps.places";
                case RouteDescriptor.Type.LOCATION -> "enorials.gps.coordinates";
            };
        }
        if (targetLocator instanceof PlayerTarget) {
            return "enorials.gps.track.player";
        }
        if (targetLocator instanceof StaticLocationTarget || targetLocator instanceof WorldSpawnTarget) {
            return "enorials.gps.coordinates";
        }
        if (targetLocator instanceof CitizensNpcTarget || targetLocator instanceof FancyNpcTarget || targetLocator instanceof ZnpcsPlusNpcTarget || targetLocator instanceof ShopkeeperTarget) {
            return "enorials.gps.track.npc";
        }
        return "enorials.gps.track.mob";
    }

    public static boolean navigation(Player player, TargetLocator targetLocator, RouteDescriptor routeDescriptor, String string) {
        return HudAccess.has((CommandSender)player, "enorials.gps.use") && HudAccess.has((CommandSender)player, HudAccess.navigationNode(targetLocator, routeDescriptor, string));
    }

    public static boolean menu(Player player, String string, String string2) {
        String string3 = string2.split("\\n", 2)[0];
        String string4 = null;
        if (Set.of("Back", "Close", "Exit", "Previous", "Next").contains(string3)) {
            return true;
        }
        if (!HudAccess.check((CommandSender)player, "enorials.menu.use")) {
            return false;
        }
        if (string.equals("HUD display") || string.equals("Player display")) {
            string4 = string3.startsWith("Quest log") ? "enorials.hud.questlog" : (string3.equals("NPC dialogues") ? "enorials.hud.conversations" : "enorials.hud.use");
        } else if (string.equals("Quest log size")) {
            string4 = "enorials.hud.questlog";
        } else if (string.equals("Themes")) {
            string4 = "enorials.hud.use";
        } else if (string.equals("Trading") || string.startsWith("Trade - ") || string.equals("ENORIALS") && string3.equals("Trading")) {
            string4 = "enorials.trade.use";
        } else if (string.equals("Map display")) {
            string4 = string3.equals("Fullscreen") ? "enorials.minimap.fullscreen" : (string3.equals("Coordinates") ? "enorials.minimap.coordinates" : "enorials.minimap.use");
        } else if (string.equals("Mob tracking")) {
            switch (string3) {
                case "Hostile": {
                    String string5 = "enorials.minimap.radar.hostile";
                    break;
                }
                case "Neutral": {
                    String string5 = "enorials.minimap.radar.neutral";
                    break;
                }
                case "Friendly / NPC": {
                    String string5 = "enorials.minimap.radar.friendly";
                    break;
                }
                default: {
                    String string5 = string4 = null;
                }
            }
            if (string4 == null && !HudAccess.anyRadar(player)) {
                return HudAccess.check((CommandSender)player, "enorials.minimap.radar");
            }
        } else if (string.equals("Markers")) {
            string4 = string3.equals("BetonQuest") ? "enorials.minimap.quests" : (string3.equals("Categories") ? "enorials.minimap.use" : "enorials.minimap.markers.use");
        } else if (string.equals("Marker display") || string.equals("Marker size")) {
            string4 = string3.equals("Categories") ? "enorials.minimap.use" : "enorials.minimap.markers.use";
        } else if (string.equals("Minimap") && string3.equals("Fullscreen")) {
            string4 = "enorials.minimap.fullscreen";
        } else if (string.equals("Marker details") && string3.equals("Public marker")) {
            string4 = "enorials.minimap.markers.share";
        } else if (string.equals("Effects")) {
            string4 = "enorials.hud.effects";
        } else if (string.equals("Party HUD") || string.equals("Party size")) {
            string4 = "enorials.party.hud";
        } else if (string.equals("Friends")) {
            string4 = "enorials.party.friends";
        } else if (string.equals("Quests")) {
            string4 = string3.startsWith("Quest log") ? "enorials.hud.questlog" : (string3.equals("NPC dialogues") ? "enorials.hud.conversations" : (string3.equals("Quest markers") ? "enorials.minimap.quests" : "enorials.gps.quests"));
        } else if (string.equals("Accepted quests")) {
            string4 = "enorials.gps.quests";
        }
        return string4 == null || HudAccess.check((CommandSender)player, string4);
    }
}

