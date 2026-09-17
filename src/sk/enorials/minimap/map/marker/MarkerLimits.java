/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 */
package sk.enorials.minimap.map.marker;

import org.bukkit.command.CommandSender;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.marker.MarkerMinimapLayer;

public final class MarkerLimits {
    private MarkerLimits() {
    }

    public static boolean owned(String string) {
        return !string.equals("player") && !string.equals("death_point") && !string.startsWith("__") && !string.startsWith("public::");
    }

    public static long unlimitedCount(Minimap minimap, String string) {
        long l = System.currentTimeMillis();
        return minimap.secondaryLayers().entrySet().stream().filter(entry -> {
            MarkerMinimapLayer markerMinimapLayer;
            Object v;
            return MarkerLimits.owned((String)entry.getKey()) && !((String)entry.getKey()).equals(string) && (v = entry.getValue()) instanceof MarkerMinimapLayer && !(markerMinimapLayer = (MarkerMinimapLayer)v).isExpired(l) && (markerMinimapLayer.getMaxVisibleDistance() == null || markerMinimapLayer.getMaxVisibleDistance() <= 0);
        }).count();
    }

    public static boolean canSetUnlimited(Minimap minimap, String string) {
        MarkerMinimapLayer markerMinimapLayer;
        HudAccess.require((CommandSender)minimap.holder(), "enorials.minimap.markers.unlimited");
        SecondaryMinimapLayer secondaryMinimapLayer = minimap.secondaryLayers().get(string);
        if (secondaryMinimapLayer instanceof MarkerMinimapLayer && ((markerMinimapLayer = (MarkerMinimapLayer)secondaryMinimapLayer).getMaxVisibleDistance() == null || markerMinimapLayer.getMaxVisibleDistance() <= 0)) {
            return true;
        }
        return MarkerLimits.unlimitedCount(minimap, string) < (long)Math.max(0, Config.instance().markers.customMarkers.unlimitedLimit);
    }

    public static String message() {
        return "You can have at most " + Math.max(0, Config.instance().markers.customMarkers.unlimitedLimit) + " unlimited-distance markers. Choose a distance or remove another unlimited marker.";
    }
}

