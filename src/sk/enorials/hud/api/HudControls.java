/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.api;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.enorials.gps.NavigationMode;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapScreenPosition;
import sk.enorials.minimap.map.MinimapShape;
import sk.enorials.minimap.map.MinimapSize;
import sk.enorials.minimap.quest.BetonQuestMarkers;

final class HudControls {
    private static final List<String> FRAMES = List.of("steel", "crystal", "gold", "ruby", "forest", "ivory");
    private final EnorialsHUD hud;

    HudControls(EnorialsHUD enorialsHUD) {
        this.hud = enorialsHUD;
    }

    String get(Player player, String string) {
        string = string.toLowerCase(Locale.ROOT);
        UUID uUID = player.getUniqueId();
        if (Set.of("hud.visible", "party.visible", "effects.visible").contains(string)) {
            return Boolean.toString(this.hud.storage().preference(uUID, string.split("\\.")[0], true));
        }
        Minimap minimap = this.hud.getPlayerMinimap(player);
        return switch (string) {
            case "hud.theme" -> this.hud.storage().theme(uUID).id();
            case "hud.quest-log" -> Boolean.toString(this.hud.storage().preference(uUID, "quest-log", this.hud.cfg().getBoolean("quest-log.default-visible", true)));
            case "hud.quest-log-size" -> Integer.toString(this.hud.storage().questLogSize(uUID));
            case "hud.conversations" -> Boolean.toString(this.hud.storage().preference(uUID, "conversations", this.hud.cfg().getBoolean("conversations.default-visible", true)));
            case "hud.vanilla-bars" -> Boolean.toString(!this.hud.storage().preference(uUID, "hud", true));
            case "party.size" -> this.hud.storage().partySize(uUID);
            case "map.betonquest-markers" -> Boolean.toString(this.hud.storage().preference(uUID, "betonquest-markers", true));
            case "map.marker-scaling" -> Boolean.toString(this.hud.storage().preference(uUID, "marker-scaling", true));
            case "map.marker-size" -> Integer.toString(this.hud.storage().markerSize(uUID));
            case "map.visible" -> Boolean.toString(minimap != null);
            case "map.size" -> {
                if (minimap == null) {
                    yield "disabled";
                }
                yield minimap.size().name().toLowerCase(Locale.ROOT);
            }
            case "map.shape" -> {
                if (minimap == null) {
                    yield "disabled";
                }
                yield minimap.shape().name().toLowerCase(Locale.ROOT);
            }
            case "map.position" -> {
                if (minimap == null) {
                    yield "disabled";
                }
                yield minimap.screenPosition().name().toLowerCase(Locale.ROOT);
            }
            case "map.frame" -> {
                if (minimap == null) {
                    yield "disabled";
                }
                yield FRAMES.get(minimap.frameStyle());
            }
            case "map.coords" -> Boolean.toString(minimap != null && minimap.coordinatesEnabled());
            case "map.combat-zoom" -> Boolean.toString(minimap != null && minimap.combatZoomEnabled());
            case "map.radar" -> Boolean.toString(minimap != null && minimap.radarMasterEnabled());
            case "gps.mode" -> this.hud.gps().api().getPlayerMode(player).name().toLowerCase(Locale.ROOT);
            default -> throw new IllegalArgumentException("Unknown HUD setting: " + string);
        };
    }

    void set(Player player, String string, String string2) {
        string = string.toLowerCase(Locale.ROOT);
        string2 = string2.toLowerCase(Locale.ROOT);
        UUID uUID = player.getUniqueId();
        HudAccess.require((CommandSender)player, HudAccess.setting(string));
        if (string.startsWith("map.")) {
            HudAccess.require((CommandSender)player, "enorials.minimap.use");
        }
        if (Set.of("hud.visible", "party.visible", "effects.visible").contains(string)) {
            this.hud.storage().setPreference(uUID, string.split("\\.")[0], HudControls.bool(string2));
            return;
        }
        switch (string) {
            case "hud.theme": {
                this.hud.storage().setTheme(uUID, string2);
                this.hud.renderer().refreshTheme(player);
                Minimap minimap = this.hud.getPlayerMinimap(player);
                if (minimap == null) break;
                minimap.respawn(this.hud.minimap());
                break;
            }
            case "hud.quest-log": {
                this.hud.storage().setPreference(uUID, "quest-log", HudControls.bool(string2));
                break;
            }
            case "hud.quest-log-size": {
                this.hud.storage().setQuestLogSize(uUID, Integer.parseInt(string2.replace("%", "")));
                break;
            }
            case "hud.conversations": {
                this.hud.storage().setPreference(uUID, "conversations", HudControls.bool(string2));
                break;
            }
            case "hud.vanilla-bars": {
                throw new IllegalArgumentException("Vanilla bars follow hud.visible automatically; use hud.visible instead.");
            }
            case "party.size": {
                this.hud.storage().setPartySize(uUID, string2);
                break;
            }
            case "map.betonquest-markers": {
                BetonQuestMarkers betonQuestMarkers;
                this.hud.storage().setPreference(uUID, "betonquest-markers", HudControls.bool(string2));
                if (!Bukkit.getPluginManager().isPluginEnabled("BetonQuest") || (betonQuestMarkers = BetonQuestMarkers.get()) == null) break;
                betonQuestMarkers.refresh(player);
                break;
            }
            case "map.marker-size": 
            case "map.marker-scaling": {
                if (string.equals("map.marker-size")) {
                    this.hud.storage().setMarkerSize(uUID, Integer.parseInt(string2.replace("%", "")));
                } else {
                    this.hud.storage().setPreference(uUID, "marker-scaling", HudControls.bool(string2));
                }
                Minimap minimap = this.hud.getPlayerMinimap(player);
                if (minimap == null) break;
                minimap.respawn(this.hud.minimap());
                break;
            }
            case "map.visible": {
                boolean bl = HudControls.bool(string2);
                if (bl == (this.hud.getPlayerMinimap(player) != null)) break;
                this.hud.mapCommand(player, bl ? "enable" : "disable");
                break;
            }
            case "gps.mode": {
                if (!Set.of("hud", "compass").contains(string2)) {
                    throw new IllegalArgumentException("GPS mode: hud or compass");
                }
                this.hud.gps().api().setPlayerMode(player, NavigationMode.valueOf(string2.toUpperCase(Locale.ROOT)));
                break;
            }
            default: {
                this.setMap(player, string, string2);
            }
        }
    }

    private void setMap(Player player, String string, String string2) {
        Object object;
        switch (string) {
            case "map.size": {
                MinimapSize.valueOf(string2.toUpperCase(Locale.ROOT));
                break;
            }
            case "map.shape": {
                MinimapShape.valueOf(string2.toUpperCase(Locale.ROOT));
                break;
            }
            case "map.position": {
                MinimapScreenPosition.valueOf(string2.toUpperCase(Locale.ROOT));
                break;
            }
            case "map.frame": {
                if (string2.equals("theme") || FRAMES.contains(string2)) break;
                throw new IllegalArgumentException("Unknown map frame");
            }
            case "map.coords": 
            case "map.combat-zoom": 
            case "map.radar": {
                HudControls.bool(string2);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown HUD setting: " + string);
            }
        }
        if (this.hud.getPlayerMinimap(player) == null) {
            this.hud.mapCommand(player, "enable");
        }
        if ((object = this.hud.getPlayerMinimap(player)) == null) {
            throw new IllegalStateException("Minimap is unavailable for this player");
        }
        switch (string) {
            case "map.size": {
                ((Minimap)object).size(MinimapSize.valueOf(string2.toUpperCase(Locale.ROOT)));
                break;
            }
            case "map.shape": {
                ((Minimap)object).shape(MinimapShape.valueOf(string2.toUpperCase(Locale.ROOT)));
                break;
            }
            case "map.position": {
                ((Minimap)object).screenPosition(MinimapScreenPosition.valueOf(string2.toUpperCase(Locale.ROOT)));
                break;
            }
            case "map.frame": {
                if (string2.equals("theme")) {
                    ((Minimap)object).frameFollowsTheme(true);
                    break;
                }
                ((Minimap)object).frameStyle(FRAMES.indexOf(string2));
                break;
            }
            case "map.coords": {
                ((Minimap)object).coordinatesEnabled(HudControls.bool(string2));
                break;
            }
            case "map.combat-zoom": {
                ((Minimap)object).combatZoomEnabled(HudControls.bool(string2));
                break;
            }
            case "map.radar": {
                ((Minimap)object).radarMasterEnabled(HudControls.bool(string2));
            }
        }
        ((Minimap)object).respawn(this.hud.minimap());
        this.hud.queueMinimapSave((Minimap)object);
    }

    void open(Player player, String string) {
        switch (string.toLowerCase(Locale.ROOT)) {
            case "main": {
                this.hud.menu().open(player);
                break;
            }
            case "map": 
            case "minimap": {
                this.hud.menu().map(player);
                break;
            }
            case "party": {
                this.hud.menu().party(player);
                break;
            }
            case "party-join": {
                this.hud.menu().openParties(player);
                break;
            }
            case "quests": {
                this.hud.gps().gui().openQuestRoutes(player);
                break;
            }
            case "hud": {
                this.hud.menu().hud(player);
                break;
            }
            case "gps": {
                this.hud.menu().gps(player);
                break;
            }
            case "markers": {
                this.hud.menu().markers(player);
                break;
            }
            case "effects": {
                this.hud.menu().effects(player);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown HUD menu: " + string);
            }
        }
    }

    private static boolean bool(String string) {
        return switch (string) {
            case "true", "on" -> true;
            case "false", "off" -> false;
            default -> throw new IllegalArgumentException("Use true/false or on/off");
        };
    }
}

