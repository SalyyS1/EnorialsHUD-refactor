/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.clientside.gui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.World;
import org.bukkit.entity.Player;
import sk.enorials.minimap.clientside.ClientsideMinimapFactory;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapScreenPosition;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.renderer.MinimapLayerRenderer;

public final class GuiClientsideMinimapFactory
implements ClientsideMinimapFactory {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(-1000000);

    @Override
    public MinimapLayer createMinimapLayer(World world, MinimapLayerRenderer minimapLayerRenderer) {
        return new MinimapLayer(NEXT_ID.getAndDecrement(), null, null, minimapLayerRenderer);
    }

    @Override
    public Minimap createMinimap(Player player, MinimapScreenPosition minimapScreenPosition, MinimapLayer minimapLayer, Map<String, SecondaryMinimapLayer> map) {
        return new Minimap(player, minimapScreenPosition, minimapLayer, map == null ? new LinkedHashMap<String, SecondaryMinimapLayer>() : new LinkedHashMap<String, SecondaryMinimapLayer>(map));
    }
}

