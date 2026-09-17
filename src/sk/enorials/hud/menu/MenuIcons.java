/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.menu;

import java.util.Locale;

public final class MenuIcons {
    private MenuIcons() {
    }

    public static String resolve(String string, String string2, String string3) {
        String string4;
        if (string2.startsWith("head:") || string2.startsWith("marker:") || string2.startsWith("atlas:") || string2.startsWith("ui:") || string2.startsWith("theme:")) {
            return string2;
        }
        String string5 = string3.split("\n", 2)[0].toLowerCase(Locale.ROOT);
        if (string.equals("Quest log size")) {
            return "ui:size";
        }
        switch (string5) {
            case "display": {
                Object object;
                if (string.equals("Minimap")) {
                    object = "map_display";
                    break;
                }
                if (string.startsWith("GPS")) {
                    object = "gps_display";
                    break;
                }
                object = "hud_display";
                break;
            }
            case "party hud": 
            case "show party": {
                Object object = "party_hud";
                break;
            }
            case "manage party": {
                Object object = "party_manage";
                break;
            }
            case "friends": {
                Object object = "friends";
                break;
            }
            case "find party": {
                Object object = "locate_party";
                break;
            }
            case "create private": {
                Object object = "private";
                break;
            }
            case "create public": {
                Object object = "public";
                break;
            }
            case "join party": {
                Object object = "join";
                break;
            }
            case "leave party": {
                Object object = "leave";
                break;
            }
            case "members": {
                Object object = "members";
                break;
            }
            case "invite player": 
            case "invite to party": 
            case "invitation": 
            case "request": {
                Object object = "invite";
                break;
            }
            case "make leader": {
                Object object = "leader";
                break;
            }
            case "remove member": {
                Object object = "remove_member";
                break;
            }
            case "add friend": {
                Object object = "add_friend";
                break;
            }
            case "remove friend": {
                Object object = "remove_friend";
                break;
            }
            case "accept": {
                Object object = "accept";
                break;
            }
            case "decline": 
            case "yes, delete": {
                Object object = "decline";
                break;
            }
            case "avatar": 
            case "refresh avatar": {
                Object object = "avatar";
                break;
            }
            case "effects": 
            case "icons and timers": {
                Object object = "effects";
                break;
            }
            case "character": {
                Object object = "character";
                break;
            }
            case "class": {
                Object object = "class";
                break;
            }
            case "spells": {
                Object object = "spells";
                break;
            }
            case "talents": {
                Object object = "talents";
                break;
            }
            case "stats": {
                Object object = "stats";
                break;
            }
            case "race": {
                Object object = "race";
                break;
            }
            case "show hud": {
                Object object = "hud_display";
                break;
            }
            case "position": {
                Object object = "position";
                break;
            }
            case "map layout": {
                Object object = "layout";
                break;
            }
            case "appearance": {
                Object object = "appearance";
                break;
            }
            case "mob tracking": 
            case "all mobs": {
                Object object = "radar";
                break;
            }
            case "markers": {
                Object object = "markers";
                break;
            }
            case "presets": {
                Object object = "presets";
                break;
            }
            case "show map": {
                Object object = "map_display";
                break;
            }
            case "fullscreen": {
                Object object = "fullscreen";
                break;
            }
            case "coordinates": {
                Object object = "coordinates";
                break;
            }
            case "combat zoom": {
                Object object = "combat_zoom";
                break;
            }
            case "map size": 
            case "panel size": 
            case "quest log size": {
                Object object = "size";
                break;
            }
            case "zoom": 
            case "near": 
            case "far": 
            case "farthest": {
                Object object = "combat_zoom";
                break;
            }
            case "shape": {
                Object object = "shape";
                break;
            }
            case "frame style": {
                Object object = "frame";
                break;
            }
            case "map colors": {
                Object object = "colors";
                break;
            }
            case "small": {
                Object object = "small";
                break;
            }
            case "medium": {
                Object object = "medium";
                break;
            }
            case "large": {
                Object object = "large";
                break;
            }
            case "circle": {
                Object object = "circle";
                break;
            }
            case "square": {
                Object object = "square";
                break;
            }
            case "left": {
                Object object = "left";
                break;
            }
            case "right": {
                Object object = "right";
                break;
            }
            case "steel": 
            case "crystal": 
            case "gold": 
            case "ruby": 
            case "forest": 
            case "ivory": {
                Object object = string5;
                break;
            }
            case "natural": {
                Object object = "natural";
                break;
            }
            case "bright": {
                Object object = "bright";
                break;
            }
            case "soft": {
                Object object = "soft";
                break;
            }
            case "hostile": {
                Object object = "hostile";
                break;
            }
            case "neutral": {
                Object object = "neutral";
                break;
            }
            case "friendly / npc": {
                Object object = "friendly";
                break;
            }
            case "create marker": {
                Object object = "marker_add";
                break;
            }
            case "my markers": 
            case "show marker": {
                Object object = "markers";
                break;
            }
            case "change icon": {
                Object object = "appearance";
                break;
            }
            case "keep marker": {
                Object object = "accept";
                break;
            }
            case "categories": 
            case "category": {
                Object object = "categories";
                break;
            }
            case "name": 
            case "rename": {
                Object object = "name";
                break;
            }
            case "details": {
                Object object = "details";
                break;
            }
            case "public marker": {
                Object object = "public";
                break;
            }
            case "distance": {
                Object object = "distance";
                break;
            }
            case "lifetime": 
            case "permanent": {
                Object object = "time";
                break;
            }
            case "location x z": 
            case "location": {
                Object object = "location";
                break;
            }
            case "delete marker": {
                Object object = "marker_remove";
                break;
            }
            case "general": {
                Object object = "general";
                break;
            }
            case "home": {
                Object object = "home";
                break;
            }
            case "warps": {
                Object object = "warp";
                break;
            }
            case "party": {
                Object object = "members";
                break;
            }
            case "quests": 
            case "quest routes": 
            case "quest log": {
                Object object = "quests";
                break;
            }
            case "danger": {
                Object object = "danger";
                break;
            }
            case "destinations": {
                Object object = "destinations";
                break;
            }
            case "favorites": 
            case "toggle favorite": 
            case "remove favorite": {
                Object object = "favorite";
                break;
            }
            case "stop gps": {
                Object object = "stop";
                break;
            }
            case "height": {
                Object object = "height";
                break;
            }
            case "target name": {
                Object object = "name";
                break;
            }
            case "compact": {
                Object object = "compact";
                break;
            }
            case "beacon": {
                Object object = "beacon";
                break;
            }
            case "navigation mode": {
                Object object = "gps_mode";
                break;
            }
            case "hud": {
                Object object = "hud_display";
                break;
            }
            case "hud + compass": {
                Object object = "compass";
                break;
            }
            case "player": 
            case "players": 
            case "online players": {
                Object object = "player";
                break;
            }
            case "player name": {
                Object object = "player_search";
                break;
            }
            case "mob": {
                Object object = "hostile";
                break;
            }
            case "npc": {
                Object object = "friendly";
                break;
            }
            case "places": {
                Object object = "places";
                break;
            }
            case "block": {
                Object object = "block";
                break;
            }
            case "navigate": {
                Object object = "navigate";
                break;
            }
            case "search": {
                Object object = "search";
                break;
            }
            case "clear search": {
                Object object = "reset";
                break;
            }
            default: {
                Object object = string5.startsWith("save preset ") ? "save_" + string5.substring(12) : (string5.startsWith("load preset ") ? "load_" + string5.substring(12) : (string5.matches("[0-9]+ min|1 day") ? "time" : (string4 = string5.matches("[0-9]+|unlimited") ? "distance" : null)));
            }
        }
        if (string4 == null) {
            string4 = switch (string2) {
                case "hud" -> "hud_display";
                case "map" -> "map_display";
                case "party" -> "members";
                case "friends" -> "friends";
                case "effects" -> "effects";
                case "back" -> "left";
                case "save" -> "accept";
                case "close" -> "decline";
                case "add" -> "marker_add";
                case "minus" -> "remove_member";
                case "edit" -> "name";
                case "lock" -> "private";
                case "unlock" -> "public";
                default -> "details";
            };
        }
        return "ui:" + string4;
    }
}

