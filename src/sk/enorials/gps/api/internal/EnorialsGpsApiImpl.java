/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package sk.enorials.gps.api.internal;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.entity.Player;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.api.EnorialsGpsApi;
import sk.enorials.gps.api.GpsQuestRoute;
import sk.enorials.gps.api.GpsTargetType;
import sk.enorials.gps.integration.IntegrationManager;
import sk.enorials.gps.nav.NavigationManager;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.gps.nav.target.TargetLocator;

public final class EnorialsGpsApiImpl
implements EnorialsGpsApi {
    private static final String DEFAULT_SOURCE = "external-api";
    private final EnorialsGpsPlugin plugin;
    private final NavigationManager navigationManager;
    private final IntegrationManager integrationManager;

    public EnorialsGpsApiImpl(EnorialsGpsPlugin enorialsGpsPlugin, NavigationManager navigationManager, IntegrationManager integrationManager) {
        this.plugin = enorialsGpsPlugin;
        this.navigationManager = navigationManager;
        this.integrationManager = integrationManager;
    }

    @Override
    public String apiVersion() {
        return "1";
    }

    @Override
    public boolean isModeEnabled(NavigationMode navigationMode) {
        return this.plugin.isModeEnabled(navigationMode);
    }

    @Override
    public NavigationMode getPlayerMode(Player player) {
        return this.navigationManager.getCurrentOrDefaultMode(player);
    }

    @Override
    public void setPlayerMode(Player player, NavigationMode navigationMode) {
        if (player == null || navigationMode == null) {
            return;
        }
        this.navigationManager.setMode(player, navigationMode);
    }

    @Override
    public boolean hasActiveNavigation(Player player) {
        return player != null && this.navigationManager.hasActiveNavigation(player);
    }

    @Override
    public boolean stopNavigation(Player player, String string) {
        if (player == null) {
            return false;
        }
        String string2 = this.blankToDefault(string, "external_api");
        return this.navigationManager.stopNavigation(player, string2);
    }

    @Override
    public boolean startNavigation(Player player, GpsTargetType gpsTargetType, String string) {
        if (player == null || gpsTargetType == null || this.isBlank(string)) {
            return false;
        }
        return this.startNavigation(player, gpsTargetType, string, this.getPlayerMode(player), DEFAULT_SOURCE);
    }

    @Override
    public boolean startNavigation(Player player, GpsTargetType gpsTargetType, String string, NavigationMode navigationMode, String string2) {
        if (player == null || gpsTargetType == null || this.isBlank(string)) {
            return false;
        }
        RouteDescriptor routeDescriptor = this.toDescriptor(gpsTargetType, string);
        return this.startNavigation(player, routeDescriptor, navigationMode, string2);
    }

    @Override
    public boolean startNavigation(Player player, RouteDescriptor routeDescriptor, NavigationMode navigationMode, String string) {
        if (player == null || routeDescriptor == null || routeDescriptor.value() == null || routeDescriptor.value().isBlank()) {
            return false;
        }
        NavigationMode navigationMode2 = navigationMode == null ? this.getPlayerMode(player) : navigationMode;
        String string2 = this.blankToDefault(string, DEFAULT_SOURCE);
        Optional<TargetLocator> optional = this.integrationManager.resolveDescriptor(player, routeDescriptor);
        if (optional.isEmpty()) {
            return false;
        }
        return this.navigationManager.startNavigation(player, optional.get(), navigationMode2, string2, routeDescriptor);
    }

    @Override
    public boolean addQuestRouteButton(Player player, String string, String string2, GpsTargetType gpsTargetType, String string3) {
        if (player == null || gpsTargetType == null || this.isBlank(string3)) {
            return false;
        }
        return this.addQuestRouteButton(player, string, string2, gpsTargetType, string3, this.getPlayerMode(player), DEFAULT_SOURCE);
    }

    @Override
    public boolean addQuestRouteButton(Player player, String string, String string2, GpsTargetType gpsTargetType, String string3, NavigationMode navigationMode, String string4) {
        if (player == null || gpsTargetType == null || this.isBlank(string3)) {
            return false;
        }
        String string5 = this.normalizeRouteId(string, gpsTargetType, string3, player);
        if (string5.isBlank()) {
            return false;
        }
        String string6 = string2 == null || string2.isBlank() ? this.autoDisplay(gpsTargetType, string3) : string2;
        NavigationMode navigationMode2 = navigationMode == null ? this.getPlayerMode(player) : navigationMode;
        String string7 = this.blankToDefault(string4, DEFAULT_SOURCE);
        RouteDescriptor routeDescriptor = this.toDescriptor(gpsTargetType, string3);
        if (this.integrationManager.resolveDescriptor(player, routeDescriptor).isEmpty()) {
            return false;
        }
        this.integrationManager.putDynamicQuestRoute(player.getUniqueId(), string5, string6, routeDescriptor, navigationMode2, string7);
        return true;
    }

    @Override
    public boolean removeQuestRouteButton(Player player, String string) {
        if (player == null || this.isBlank(string)) {
            return false;
        }
        return this.integrationManager.removeDynamicQuestRoute(player.getUniqueId(), string);
    }

    @Override
    public int clearQuestRouteButtons(Player player) {
        if (player == null) {
            return 0;
        }
        return this.integrationManager.clearDynamicQuestRoutes(player.getUniqueId());
    }

    @Override
    public Map<String, GpsQuestRoute> getQuestRouteButtons(Player player) {
        if (player == null) {
            return Map.of();
        }
        Map<String, IntegrationManager.AutoRoute> map = this.integrationManager.getAutoRoutes(player);
        LinkedHashMap<String, GpsQuestRoute> linkedHashMap = new LinkedHashMap<String, GpsQuestRoute>();
        for (Map.Entry<String, IntegrationManager.AutoRoute> entry : map.entrySet()) {
            IntegrationManager.AutoRoute autoRoute = entry.getValue();
            linkedHashMap.put(entry.getKey(), new GpsQuestRoute(autoRoute.key(), autoRoute.displayName(), this.fromDescriptorType(autoRoute.descriptor().type()), autoRoute.descriptor().value(), autoRoute.mode(), autoRoute.source()));
        }
        return linkedHashMap;
    }

    private RouteDescriptor toDescriptor(GpsTargetType gpsTargetType, String string) {
        return new RouteDescriptor(this.toDescriptorType(gpsTargetType), string);
    }

    private RouteDescriptor.Type toDescriptorType(GpsTargetType gpsTargetType) {
        return switch (gpsTargetType) {
            default -> throw new IncompatibleClassChangeError();
            case GpsTargetType.PLAYER -> RouteDescriptor.Type.PLAYER;
            case GpsTargetType.MOB -> RouteDescriptor.Type.MOB;
            case GpsTargetType.NPC -> RouteDescriptor.Type.NPC;
            case GpsTargetType.POI -> RouteDescriptor.Type.POI;
            case GpsTargetType.LOCATION -> RouteDescriptor.Type.LOCATION;
        };
    }

    private GpsTargetType fromDescriptorType(RouteDescriptor.Type type) {
        return switch (type) {
            default -> throw new IncompatibleClassChangeError();
            case RouteDescriptor.Type.PLAYER -> GpsTargetType.PLAYER;
            case RouteDescriptor.Type.MOB -> GpsTargetType.MOB;
            case RouteDescriptor.Type.NPC -> GpsTargetType.NPC;
            case RouteDescriptor.Type.POI -> GpsTargetType.POI;
            case RouteDescriptor.Type.LOCATION -> GpsTargetType.LOCATION;
        };
    }

    private String normalizeRouteId(String string, GpsTargetType gpsTargetType, String string2, Player player) {
        Object object = string;
        if (object == null || ((String)object).isBlank()) {
            object = "q_" + gpsTargetType.name().toLowerCase(Locale.ROOT) + "_" + this.sanitizeId(string2);
        }
        if (((String)(object = this.sanitizeId((String)object))).isBlank()) {
            return "route";
        }
        Set<String> set = this.integrationManager.getDynamicQuestRouteKeys(player.getUniqueId());
        Object object2 = object;
        int n = 2;
        while (this.containsIgnoreCase(set, (String)object2)) {
            object2 = (String)object + "_" + n++;
        }
        return object2;
    }

    private boolean containsIgnoreCase(Set<String> set, String string) {
        for (String string2 : set) {
            if (string2 == null || !string2.equalsIgnoreCase(string)) continue;
            return true;
        }
        return false;
    }

    private String autoDisplay(GpsTargetType gpsTargetType, String string) {
        String string2 = string == null ? "" : string.trim();
        String string3 = string2;
        if (gpsTargetType == GpsTargetType.POI) {
            return this.integrationManager.getPoiDisplayName(string2);
        }
        int n = string2.indexOf(58);
        if (n > 0 && n + 1 < string2.length()) {
            string2 = string2.substring(n + 1);
        }
        if ((string2 = string2.replace('_', ' ').trim()).isEmpty()) {
            string2 = gpsTargetType.name();
        }
        return string2;
    }

    private String sanitizeId(String string) {
        if (string == null) {
            return "";
        }
        return string.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private String blankToDefault(String string, String string2) {
        return this.isBlank(string) ? string2 : string.trim();
    }

    private boolean isBlank(String string) {
        return string == null || string.trim().isEmpty();
    }
}

