/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.generator.WorldInfo
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package sk.enorials.gps.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.integration.IntegrationManager;
import sk.enorials.gps.nav.NavigationManager;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.gps.nav.target.PlayerTarget;
import sk.enorials.gps.nav.target.StaticLocationTarget;
import sk.enorials.gps.nav.target.TargetLocator;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;

public final class GpsApiCommand
implements CommandExecutor,
TabCompleter {
    private final NavigationManager navigationManager;
    private final IntegrationManager integrationManager;
    private static final String DYNAMIC_ROUTE_SOURCE = "bq-dynamic";

    public GpsApiCommand(NavigationManager navigationManager, IntegrationManager integrationManager) {
        this.navigationManager = navigationManager;
        this.integrationManager = integrationManager;
    }

    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String string, @NotNull String[] stringArray) {
        if (!HudAccess.check(commandSender, "enorials.gps.api")) {
            return true;
        }
        if (stringArray.length < 1) {
            commandSender.sendMessage("Usage: /gpsapi <start|stop|mode|perf|route> ...");
            return true;
        }
        String string2 = stringArray[0].toLowerCase();
        if (string2.equals("waystone")) {
            return EnorialsHUD.getInstance().waystoneTravel().command(commandSender, stringArray);
        }
        if (string2.equals("perf")) {
            return this.handlePerf(commandSender, stringArray);
        }
        if (string2.equals("route")) {
            return this.handleRoute(commandSender, stringArray);
        }
        if (stringArray.length < 2) {
            commandSender.sendMessage("Usage: /gpsapi <start|stop|mode> <player> ...");
            return true;
        }
        Player player = Bukkit.getPlayerExact((String)stringArray[1]);
        if (player == null) {
            commandSender.sendMessage("Player not found: " + stringArray[1]);
            return true;
        }
        return switch (string2) {
            case "stop" -> {
                this.navigationManager.stopNavigation(player, "api");
                commandSender.sendMessage("Stopped navigation for " + player.getName());
                yield true;
            }
            case "mode" -> {
                if (stringArray.length < 3) {
                    commandSender.sendMessage("Usage: /gpsapi mode <player> <actionbar|bossbar|compass>");
                    yield true;
                }
                NavigationMode var9_9 = NavigationMode.fromConfig(stringArray[2]);
                this.navigationManager.setMode(player, var9_9);
                commandSender.sendMessage("Mode set to " + var9_9.display() + " for " + player.getName());
                yield true;
            }
            case "start" -> this.handleStart(commandSender, player, stringArray);
            default -> {
                commandSender.sendMessage("Unknown subcommand.");
                yield true;
            }
        };
    }

    private boolean handlePerf(CommandSender commandSender, String[] stringArray) {
        if (stringArray.length >= 2 && stringArray[1].equalsIgnoreCase("reset")) {
            this.navigationManager.resetProfilingStats();
            commandSender.sendMessage("GPS profiling stats reset.");
            return true;
        }
        commandSender.sendMessage(this.navigationManager.getProfilingSummary());
        return true;
    }

    private boolean handleRoute(CommandSender commandSender, String[] stringArray) {
        if (stringArray.length < 3) {
            commandSender.sendMessage("Usage: /gpsapi route <add|remove|clear|list> <player> ...");
            return true;
        }
        String string = stringArray[1].toLowerCase();
        Player player = Bukkit.getPlayerExact((String)stringArray[2]);
        if (player == null) {
            commandSender.sendMessage("Player not found: " + stringArray[2]);
            return true;
        }
        return switch (string) {
            case "add" -> this.handleRouteAdd(commandSender, player, stringArray);
            case "remove" -> this.handleRouteRemove(commandSender, player, stringArray);
            case "clear" -> this.handleRouteClear(commandSender, player);
            case "list" -> this.handleRouteList(commandSender, player);
            default -> {
                commandSender.sendMessage("Unknown route action. Use add/remove/clear/list.");
                yield true;
            }
        };
    }

    private boolean handleRouteAdd(CommandSender commandSender, Player player, String[] stringArray) {
        RouteDescriptor.Type type;
        if (stringArray.length < 4) {
            commandSender.sendMessage("Usage:");
            commandSender.sendMessage("/gpsapi route add <player> <routeId> <display_with_underscores> <PLAYER|MOB|NPC|POI|LOCATION> <actionbar|bossbar|compass> <value...>");
            commandSender.sendMessage("/gpsapi route add <player> <PLAYER|MOB|NPC|POI|LOCATION> [actionbar|bossbar|compass] <value...>");
            commandSender.sendMessage("/gpsapi route add <player> <value...>   # type/mode/id/display auto");
            return true;
        }
        if (stringArray.length >= 8 && (type = this.parseRouteType(stringArray[5])) != null) {
            return this.addRoute(commandSender, player, stringArray[3].trim(), stringArray[4].replace('_', ' '), type, NavigationMode.fromConfig(stringArray[6]), this.join(stringArray, 7));
        }
        RouteDescriptor.Type type2 = this.parseRouteType(stringArray[3]);
        if (type2 != null) {
            int n = 4;
            NavigationMode navigationMode = this.navigationManager.getCurrentOrDefaultMode(player);
            if (stringArray.length >= 5 && this.isModeToken(stringArray[4])) {
                navigationMode = NavigationMode.fromConfig(stringArray[4]);
                n = 5;
            }
            if (stringArray.length <= n - 1) {
                commandSender.sendMessage("Missing route value.");
                return true;
            }
            String string = this.join(stringArray, n);
            return this.addAutoRoute(commandSender, player, type2, string, navigationMode);
        }
        String string = this.join(stringArray, 3);
        InferredInput inferredInput = this.inferRouteTypeAndValue(player, string);
        if (inferredInput == null) {
            commandSender.sendMessage("Could not infer route type. Use explicit type: PLAYER|MOB|NPC|POI|LOCATION.");
            return true;
        }
        return this.addAutoRoute(commandSender, player, inferredInput.type(), inferredInput.value(), this.navigationManager.getCurrentOrDefaultMode(player));
    }

    private boolean addAutoRoute(CommandSender commandSender, Player player, RouteDescriptor.Type type, String string, NavigationMode navigationMode) {
        String string2 = this.nextAutoRouteId(player, type, string);
        String string3 = this.autoDisplay(type, string);
        return this.addRoute(commandSender, player, string2, string3, type, navigationMode, string);
    }

    private boolean addRoute(CommandSender commandSender, Player player, String string, String string2, RouteDescriptor.Type type, NavigationMode navigationMode, String string3) {
        if (string == null || string.isBlank()) {
            commandSender.sendMessage("Route id is empty.");
            return true;
        }
        if (string2 == null || string2.isBlank()) {
            commandSender.sendMessage("Display is empty.");
            return true;
        }
        RouteDescriptor routeDescriptor = new RouteDescriptor(type, string3);
        if (this.integrationManager.resolveDescriptor(player, routeDescriptor).isEmpty()) {
            commandSender.sendMessage("Could not resolve route target right now for value: " + string3);
            return true;
        }
        this.integrationManager.putDynamicQuestRoute(player.getUniqueId(), string, string2, routeDescriptor, navigationMode, DYNAMIC_ROUTE_SOURCE);
        commandSender.sendMessage("Added quest route button '" + string + "' for " + player.getName() + ".");
        return true;
    }

    private RouteDescriptor.Type parseRouteType(String string) {
        if (string == null || string.isBlank()) {
            return null;
        }
        try {
            return RouteDescriptor.Type.valueOf(string.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private boolean isModeToken(String string) {
        if (string == null) {
            return false;
        }
        String string2 = string.toLowerCase(Locale.ROOT);
        return string2.equals("actionbar") || string2.equals("bossbar") || string2.equals("compass");
    }

    private String nextAutoRouteId(Player player, RouteDescriptor.Type type, String string) {
        Object object = "q_" + type.name().toLowerCase(Locale.ROOT) + "_" + this.sanitizeId(string);
        if (((String)object).length() > 42) {
            object = ((String)object).substring(0, 42);
        }
        String string2 = object;
        int n = 2;
        Set<String> set = this.integrationManager.getDynamicQuestRouteKeys(player.getUniqueId());
        while (this.containsIgnoreCase(set, string2)) {
            string2 = (String)object + "_" + n++;
        }
        return string2;
    }

    private String autoDisplay(RouteDescriptor.Type type, String string) {
        String string2 = string == null ? "" : string.trim();
        String string3 = string2;
        if (type == RouteDescriptor.Type.POI) {
            string2 = this.integrationManager.getPoiDisplayName(string2);
        } else {
            int n = string2.indexOf(58);
            if (n > 0 && n + 1 < string2.length()) {
                string2 = string2.substring(n + 1);
            }
            string2 = string2.replace('_', ' ');
        }
        if (string2.isBlank()) {
            string2 = type.name();
        }
        return string2;
    }

    private String sanitizeId(String string) {
        if (string == null || string.isBlank()) {
            return "route";
        }
        String string2 = string.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (string2.isBlank()) {
            return "route";
        }
        return string2;
    }

    private boolean containsIgnoreCase(Iterable<String> iterable, String string) {
        if (string == null) {
            return false;
        }
        for (String string2 : iterable) {
            if (string2 == null || !string2.equalsIgnoreCase(string)) continue;
            return true;
        }
        return false;
    }

    private InferredInput inferRouteTypeAndValue(Player player, String string) {
        String string2 = string == null ? "" : string.trim();
        String string3 = string2;
        if (string2.isBlank()) {
            return null;
        }
        Player player2 = Bukkit.getPlayerExact((String)string2);
        if (player2 != null && this.integrationManager.isPlayerTargetsEnabled()) {
            return new InferredInput(RouteDescriptor.Type.PLAYER, player2.getUniqueId().toString());
        }
        String string4 = this.findPoiKeyIgnoreCase(string2);
        if (string4 != null) {
            return new InferredInput(RouteDescriptor.Type.POI, string4);
        }
        String string5 = string2.toLowerCase(Locale.ROOT);
        if (this.looksLikeLocationInput(string2, string5)) {
            return new InferredInput(RouteDescriptor.Type.LOCATION, string2);
        }
        if (this.looksLikeNpcInput(string5)) {
            return new InferredInput(RouteDescriptor.Type.NPC, string2);
        }
        if (this.looksLikeMobInput(string2, string5)) {
            return new InferredInput(RouteDescriptor.Type.MOB, string2);
        }
        return null;
    }

    private String findPoiKeyIgnoreCase(String string) {
        for (String string2 : this.integrationManager.getAvailablePoiKeys()) {
            if (!string2.equalsIgnoreCase(string)) continue;
            return string2;
        }
        return null;
    }

    private boolean looksLikeLocationInput(String string, String string2) {
        if (string2.startsWith("warp:") || string2.startsWith("esswarp:") || string2.startsWith("ess:") || string2.startsWith("essentials:") || string2.startsWith("cmiwarp:") || string2.startsWith("cmi:") || string2.startsWith("wg:") || string2.startsWith("worldguard:") || string2.startsWith("region:") || string2.startsWith("town:") || string2.startsWith("towny:") || string2.startsWith("dh:") || string2.startsWith("holo:") || string2.startsWith("decentholograms:")) {
            return true;
        }
        String[] stringArray = string.split("[,;\\s]+");
        if (stringArray.length >= 3 && this.isDouble(stringArray[0]) && this.isDouble(stringArray[1]) && this.isDouble(stringArray[2])) {
            return true;
        }
        return stringArray.length >= 4 && this.isDouble(stringArray[1]) && this.isDouble(stringArray[2]) && this.isDouble(stringArray[3]);
    }

    private boolean looksLikeNpcInput(String string) {
        return string.startsWith("citizens:") || string.startsWith("denizen:") || string.startsWith("znpcs:") || string.startsWith("znpcsplus:") || string.startsWith("fancynpc:") || string.startsWith("fancy:") || string.startsWith("shopkeeper:") || string.startsWith("shopkeepers:");
    }

    private boolean looksLikeMobInput(String string, String string2) {
        if (string2.startsWith("mythic:") || string2.startsWith("elite:") || string2.startsWith("elitemobs:") || string2.startsWith("modelengine:") || string2.startsWith("model:") || string2.startsWith("denizen:") || string2.startsWith("mmocore:") || string2.startsWith("mmoitems:") || string2.startsWith("ecoboss:") || string2.startsWith("ecobosses:") || string2.startsWith("tag:") || string2.startsWith("meta:") || string2.startsWith("pdc:")) {
            return true;
        }
        try {
            EntityType.valueOf((String)string.toUpperCase(Locale.ROOT));
            return true;
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }
    }

    private boolean isDouble(String string) {
        try {
            Double.parseDouble(string);
            return true;
        }
        catch (NumberFormatException numberFormatException) {
            return false;
        }
    }

    private boolean handleRouteRemove(CommandSender commandSender, Player player, String[] stringArray) {
        if (stringArray.length < 4) {
            commandSender.sendMessage("Usage: /gpsapi route remove <player> <routeId>");
            return true;
        }
        String string = stringArray[3];
        boolean bl = this.integrationManager.removeDynamicQuestRoute(player.getUniqueId(), string);
        if (!bl) {
            commandSender.sendMessage("Route not found: " + string);
            return true;
        }
        commandSender.sendMessage("Removed quest route button '" + string + "' for " + player.getName() + ".");
        return true;
    }

    private boolean handleRouteClear(CommandSender commandSender, Player player) {
        int n = this.integrationManager.clearDynamicQuestRoutes(player.getUniqueId());
        commandSender.sendMessage("Cleared " + n + " dynamic quest route(s) for " + player.getName() + ".");
        return true;
    }

    private boolean handleRouteList(CommandSender commandSender, Player player) {
        ArrayList<String> arrayList = new ArrayList<String>(this.integrationManager.getDynamicQuestRouteKeys(player.getUniqueId()));
        if (arrayList.isEmpty()) {
            commandSender.sendMessage("No dynamic quest routes for " + player.getName() + ".");
            return true;
        }
        Collections.sort(arrayList);
        commandSender.sendMessage("Dynamic quest routes for " + player.getName() + ": " + String.join((CharSequence)", ", arrayList));
        return true;
    }

    private boolean handleStart(CommandSender commandSender, Player player, String[] stringArray) {
        if (stringArray.length < 4) {
            commandSender.sendMessage("Usage: /gpsapi start <player> <player|mob|npc|poi|loc|warp|region|town|hologram|block> <value...>");
            return true;
        }
        String string = stringArray[2].toLowerCase();
        NavigationMode navigationMode = this.navigationManager.getCurrentOrDefaultMode(player);
        switch (string) {
            case "player": {
                if (!this.integrationManager.isPlayerTargetsEnabled()) {
                    commandSender.sendMessage("Player targets are disabled.");
                    return true;
                }
                Player player2 = Bukkit.getPlayerExact((String)stringArray[3]);
                if (player2 == null) {
                    commandSender.sendMessage("Target player not found.");
                    return true;
                }
                this.navigationManager.startNavigation(player, new PlayerTarget(player2.getUniqueId(), player2.getName()), navigationMode, "api-player", RouteDescriptor.player(player2.getUniqueId().toString()));
                commandSender.sendMessage("Navigation started.");
                return true;
            }
            case "mob": {
                String string2 = this.join(stringArray, 3);
                Optional<TargetLocator> optional = this.integrationManager.resolveMobInput(string2);
                if (optional.isEmpty()) {
                    commandSender.sendMessage("Could not resolve mob target: " + string2);
                    return true;
                }
                this.navigationManager.startNavigation(player, optional.get(), navigationMode, "api-mob", RouteDescriptor.mob(string2));
                commandSender.sendMessage("Navigation started.");
                return true;
            }
            case "npc": {
                String string3 = this.join(stringArray, 3);
                Optional<TargetLocator> optional = this.integrationManager.resolveNpcInput(string3);
                if (optional.isEmpty()) {
                    commandSender.sendMessage("Could not resolve NPC target: " + string3);
                    return true;
                }
                this.navigationManager.startNavigation(player, optional.get(), navigationMode, "api-npc", RouteDescriptor.npc(string3));
                commandSender.sendMessage("Navigation started.");
                return true;
            }
            case "poi": {
                String string4 = stringArray[3];
                Optional<TargetLocator> optional = this.integrationManager.resolvePoi(player, string4);
                if (optional.isEmpty()) {
                    commandSender.sendMessage("POI not available: " + string4);
                    return true;
                }
                this.navigationManager.startNavigation(player, optional.get(), navigationMode, "api-poi", RouteDescriptor.poi(string4));
                commandSender.sendMessage("Navigation started.");
                return true;
            }
            case "loc": {
                if (!this.integrationManager.isLocationTargetsEnabled()) {
                    commandSender.sendMessage("Location targets are disabled.");
                    return true;
                }
                String string5 = this.join(stringArray, 3);
                Optional<TargetLocator> optional = this.integrationManager.resolveLocationInput(player, string5);
                if (optional.isPresent()) {
                    Location location = optional.get().resolve(player);
                    if (location == null) {
                        commandSender.sendMessage("Could not resolve location target: " + string5);
                        return true;
                    }
                    this.navigationManager.startNavigation(player, optional.get(), navigationMode, "api-loc", RouteDescriptor.location(location));
                    commandSender.sendMessage("Navigation started.");
                    return true;
                }
                if (stringArray.length < 6) {
                    commandSender.sendMessage("Usage: /gpsapi start <player> loc <x> <y> <z> [world] OR /gpsapi start <player> loc warp:spawn OR /gpsapi start <player> loc wg:region OR /gpsapi start <player> loc town:MyTown OR /gpsapi start <player> loc dh:spawn_holo");
                    return true;
                }
                try {
                    double d = Double.parseDouble(stringArray[3]);
                    double d2 = Double.parseDouble(stringArray[4]);
                    double d3 = Double.parseDouble(stringArray[5]);
                    String string6 = stringArray.length >= 7 ? stringArray[6] : player.getWorld().getName();
                    String string7 = string6;
                    if (Bukkit.getWorld((String)string6) == null) {
                        commandSender.sendMessage("World not found: " + string6);
                        return true;
                    }
                    Location location = new Location(Bukkit.getWorld((String)string6), d, d2, d3);
                    this.navigationManager.startNavigation(player, new StaticLocationTarget("Location " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ(), location), navigationMode, "api-loc", RouteDescriptor.location(location));
                    commandSender.sendMessage("Navigation started.");
                    return true;
                }
                catch (NumberFormatException numberFormatException) {
                    commandSender.sendMessage("Invalid coordinates.");
                    return true;
                }
            }
            case "warp": {
                if (!this.integrationManager.isLocationTargetsEnabled()) {
                    commandSender.sendMessage("Location targets are disabled.");
                    return true;
                }
                String string8 = this.join(stringArray, 3);
                Optional<TargetLocator> optional = this.integrationManager.resolveLocationInput(player, "warp:" + string8);
                if (optional.isEmpty()) {
                    commandSender.sendMessage("Could not resolve warp target: " + string8);
                    return true;
                }
                Location location = optional.get().resolve(player);
                if (location == null) {
                    commandSender.sendMessage("Warp target is unavailable.");
                    return true;
                }
                this.navigationManager.startNavigation(player, optional.get(), navigationMode, "api-warp", RouteDescriptor.location(location));
                commandSender.sendMessage("Navigation started.");
                return true;
            }
            case "region": {
                if (!this.integrationManager.isLocationTargetsEnabled()) {
                    commandSender.sendMessage("Location targets are disabled.");
                    return true;
                }
                if (stringArray.length < 4) {
                    commandSender.sendMessage("Usage: /gpsapi start <player> region <region> [world]");
                    return true;
                }
                String string9 = stringArray[3];
                String string10 = stringArray.length >= 5 ? stringArray[4] : "";
                String string11 = string10.isBlank() ? "wg:" + string9 : "wg:" + string10 + ":" + string9;
                Optional<TargetLocator> optional = this.integrationManager.resolveLocationInput(player, string11);
                if (optional.isEmpty()) {
                    commandSender.sendMessage("Could not resolve region target: " + string11);
                    return true;
                }
                Location location = optional.get().resolve(player);
                if (location == null) {
                    commandSender.sendMessage("Region target is unavailable.");
                    return true;
                }
                this.navigationManager.startNavigation(player, optional.get(), navigationMode, "api-region", RouteDescriptor.location(location));
                commandSender.sendMessage("Navigation started.");
                return true;
            }
            case "town": {
                if (!this.integrationManager.isLocationTargetsEnabled()) {
                    commandSender.sendMessage("Location targets are disabled.");
                    return true;
                }
                String string12 = this.join(stringArray, 3);
                Optional<TargetLocator> optional = this.integrationManager.resolveLocationInput(player, "town:" + string12);
                if (optional.isEmpty()) {
                    commandSender.sendMessage("Could not resolve town target: " + string12);
                    return true;
                }
                Location location = optional.get().resolve(player);
                if (location == null) {
                    commandSender.sendMessage("Town target is unavailable.");
                    return true;
                }
                this.navigationManager.startNavigation(player, optional.get(), navigationMode, "api-town", RouteDescriptor.location(location));
                commandSender.sendMessage("Navigation started.");
                return true;
            }
            case "hologram": {
                if (!this.integrationManager.isLocationTargetsEnabled()) {
                    commandSender.sendMessage("Location targets are disabled.");
                    return true;
                }
                String string13 = this.join(stringArray, 3);
                Optional<TargetLocator> optional = this.integrationManager.resolveLocationInput(player, "dh:" + string13);
                if (optional.isEmpty()) {
                    commandSender.sendMessage("Could not resolve hologram target: " + string13);
                    return true;
                }
                Location location = optional.get().resolve(player);
                if (location == null) {
                    commandSender.sendMessage("Hologram target is unavailable.");
                    return true;
                }
                this.navigationManager.startNavigation(player, optional.get(), navigationMode, "api-hologram", RouteDescriptor.location(location));
                commandSender.sendMessage("Navigation started.");
                return true;
            }
            case "block": {
                String string14 = this.join(stringArray, 3);
                Optional<TargetLocator> optional = this.integrationManager.resolveBlockInput(player, string14);
                if (optional.isEmpty()) {
                    commandSender.sendMessage("Block target was not found near the player: " + string14);
                    return true;
                }
                Location location = optional.get().resolve(player);
                if (location == null) {
                    commandSender.sendMessage("Block target is unavailable.");
                    return true;
                }
                this.navigationManager.startNavigation(player, optional.get(), navigationMode, "api-block", RouteDescriptor.location(location));
                commandSender.sendMessage("Navigation started.");
                return true;
            }
        }
        commandSender.sendMessage("Unknown start type: " + string);
        return true;
    }

    private String join(String[] stringArray, int n) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = n; i < stringArray.length; ++i) {
            if (i > n) {
                stringBuilder.append(' ');
            }
            stringBuilder.append(stringArray[i]);
        }
        return stringBuilder.toString();
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String string, @NotNull String[] stringArray) {
        if (stringArray.length == 1) {
            return List.of("start", "stop", "mode", "perf", "route");
        }
        if (stringArray.length == 2 && stringArray[0].equalsIgnoreCase("perf")) {
            return List.of("reset");
        }
        if (stringArray.length == 2 && stringArray[0].equalsIgnoreCase("route")) {
            return List.of("add", "remove", "clear", "list");
        }
        if (stringArray.length == 3 && stringArray[0].equalsIgnoreCase("route")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (stringArray.length == 4 && stringArray[0].equalsIgnoreCase("route") && stringArray[1].equalsIgnoreCase("add")) {
            return List.of("POI", "NPC", "MOB", "PLAYER", "LOCATION", "quest_route_1");
        }
        if (stringArray.length == 4 && stringArray[0].equalsIgnoreCase("route") && stringArray[1].equalsIgnoreCase("remove")) {
            Player player = Bukkit.getPlayerExact((String)stringArray[2]);
            if (player == null) {
                return Collections.emptyList();
            }
            return new ArrayList<String>(this.integrationManager.getDynamicQuestRouteKeys(player.getUniqueId()));
        }
        if (stringArray.length == 5 && stringArray[0].equalsIgnoreCase("route") && stringArray[1].equalsIgnoreCase("add")) {
            if (this.parseRouteType(stringArray[3]) != null) {
                return List.of("actionbar", "bossbar", "compass");
            }
            return List.of("Quest_Objective_1");
        }
        if (stringArray.length == 6 && stringArray[0].equalsIgnoreCase("route") && stringArray[1].equalsIgnoreCase("add")) {
            if (this.parseRouteType(stringArray[3]) != null) {
                String string2;
                return switch (string2 = stringArray[3].toUpperCase(Locale.ROOT)) {
                    case "PLAYER" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                    case "MOB" -> List.of("ZOMBIE", "mythic:BossSkeleton", "tag:event_boss");
                    case "NPC" -> List.of("citizens:1", "fancynpc:guard_1", "znpcs:15");
                    case "POI" -> new ArrayList<String>(this.integrationManager.getAvailablePoiKeys());
                    case "LOCATION" -> List.of("warp:spawn", "wg:spawn_region", "0 64 0 world");
                    default -> Collections.emptyList();
                };
            }
            return List.of("PLAYER", "MOB", "NPC", "POI", "LOCATION");
        }
        if (stringArray.length == 7 && stringArray[0].equalsIgnoreCase("route") && stringArray[1].equalsIgnoreCase("add")) {
            if (this.parseRouteType(stringArray[5]) != null) {
                return List.of("actionbar", "bossbar", "compass");
            }
            return Collections.emptyList();
        }
        if (stringArray.length == 8 && stringArray[0].equalsIgnoreCase("route") && stringArray[1].equalsIgnoreCase("add")) {
            String string3;
            return switch (string3 = stringArray[5].toUpperCase(Locale.ROOT)) {
                case "PLAYER" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                case "MOB" -> List.of("ZOMBIE", "mythic:BossSkeleton", "tag:event_boss");
                case "NPC" -> List.of("citizens:1", "fancynpc:guard_1", "znpcs:15");
                case "POI" -> new ArrayList<String>(this.integrationManager.getAvailablePoiKeys());
                case "LOCATION" -> List.of("warp:spawn", "wg:spawn_region", "0 64 0 world");
                default -> Collections.emptyList();
            };
        }
        if (stringArray.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (stringArray.length == 3 && stringArray[0].equalsIgnoreCase("start")) {
            return List.of("player", "mob", "npc", "poi", "loc", "warp", "region", "town", "hologram", "block");
        }
        if (stringArray.length == 4 && stringArray[0].equalsIgnoreCase("start") && stringArray[2].equalsIgnoreCase("mob")) {
            return List.of("ZOMBIE", "mythic:BossSkeleton", "elite:miniboss", "ecoboss:worldboss", "modelengine:guardian", "tag:event_boss");
        }
        if (stringArray.length == 4 && stringArray[0].equalsIgnoreCase("start") && stringArray[2].equalsIgnoreCase("npc")) {
            return List.of("1", "citizens:1", "citizens:Blacksmith", "znpcs:15", "fancynpc:guard_1", "shopkeeper:*", "denizen:QuestGiver");
        }
        if (stringArray.length == 4 && stringArray[0].equalsIgnoreCase("start") && stringArray[2].equalsIgnoreCase("loc")) {
            return List.of("0", "warp:spawn", "cmi:spawn", "wg:spawn_region", "town:SpawnTown", "dh:spawn_info");
        }
        if (stringArray.length == 4 && stringArray[0].equalsIgnoreCase("start") && stringArray[2].equalsIgnoreCase("warp")) {
            List<String> list = this.integrationManager.getWarpNames();
            if (!list.isEmpty()) {
                return list;
            }
            return List.of("spawn", "market");
        }
        if (stringArray.length == 4 && stringArray[0].equalsIgnoreCase("start") && stringArray[2].equalsIgnoreCase("region")) {
            List<String> list;
            Player player = stringArray.length >= 2 ? Bukkit.getPlayerExact((String)stringArray[1]) : null;
            Player player2 = player;
            if (player == null && commandSender instanceof Player) {
                Player player3;
                player = player3 = (Player)commandSender;
            }
            if (!(list = this.integrationManager.getWorldGuardRegionNames(player)).isEmpty()) {
                return list;
            }
            return List.of("spawn_region", "market_region");
        }
        if (stringArray.length == 5 && stringArray[0].equalsIgnoreCase("start") && stringArray[2].equalsIgnoreCase("region")) {
            return Bukkit.getWorlds().stream().map(WorldInfo::getName).toList();
        }
        if (stringArray.length == 4 && stringArray[0].equalsIgnoreCase("start") && stringArray[2].equalsIgnoreCase("town")) {
            List<String> list = this.integrationManager.getTownyTownNames();
            if (!list.isEmpty()) {
                return list;
            }
            return List.of("SpawnTown", "MarketTown");
        }
        if (stringArray.length == 4 && stringArray[0].equalsIgnoreCase("start") && stringArray[2].equalsIgnoreCase("hologram")) {
            List<String> list = this.integrationManager.getDecentHologramNames();
            if (!list.isEmpty()) {
                return list;
            }
            return List.of("spawn_info", "quests_board");
        }
        if (stringArray.length == 4 && stringArray[0].equalsIgnoreCase("start") && stringArray[2].equalsIgnoreCase("block")) {
            return List.of("STONE", "itemsadder:myplugin:jade_ore", "nexo:ancient_rock", "oraxen:my_custom_block");
        }
        if (stringArray.length == 3 && stringArray[0].equalsIgnoreCase("mode")) {
            return List.of("actionbar", "bossbar", "compass");
        }
        if (stringArray.length == 4 && stringArray[0].equalsIgnoreCase("start") && stringArray[2].equalsIgnoreCase("poi")) {
            return new ArrayList<String>(this.integrationManager.getAvailablePoiKeys());
        }
        return Collections.emptyList();
    }

    private record InferredInput(RouteDescriptor.Type type, String value) {
    }
}

