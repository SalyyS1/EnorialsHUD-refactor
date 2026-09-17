/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.map;

import java.util.LinkedHashMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapProvider;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.marker.MarkerMinimapLayer;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;

final class DeathMarkerController {
    private DeathMarkerController() {
    }

    static void clear(Minimap minimap, MinimapProvider minimapProvider) {
        LinkedHashMap<String, SecondaryMinimapLayer> linkedHashMap = minimap.secondaryLayers();
        if (!linkedHashMap.containsKey("death_point")) {
            return;
        }
        SecondaryMinimapLayer secondaryMinimapLayer = (SecondaryMinimapLayer)linkedHashMap.remove("death_point");
        if (secondaryMinimapLayer == null) {
            return;
        }
        minimapProvider.packetSender().despawnLayer(minimap.holder(), secondaryMinimapLayer.getBaseLayer());
    }

    static void show(Minimap minimap, MinimapProvider minimapProvider, Location location) {
        DeathMarkerController.clear(minimap, minimapProvider);
        MinimapIcon minimapIcon = minimapProvider.iconProvider().getIcon("death");
        if (minimapIcon == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Player player = minimap.holder();
        MinimapLayer minimapLayer = minimapProvider.clientsideMinimapFactory().createMinimapLayer(player.getWorld(), null);
        MarkerMinimapLayer markerMinimapLayer = new MarkerMinimapLayer(minimapLayer, new MinimapIconRenderer(minimapIcon), true, Config.instance().markers.deathMarker.stickToBorder, world, location.getBlockX(), location.getBlockZ(), 0.05f);
        minimap.secondaryLayers().put("death_point", markerMinimapLayer);
        if (!player.getWorld().equals((Object)world)) {
            return;
        }
        minimapProvider.packetSender().spawnLayer(player, minimapLayer);
    }
}

