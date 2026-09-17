/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.clientside;

import java.util.Map;
import org.bukkit.World;
import org.bukkit.entity.Player;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapScreenPosition;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.renderer.MinimapLayerRenderer;

public interface ClientsideMinimapFactory {
    public MinimapLayer createMinimapLayer(World var1, MinimapLayerRenderer var2);

    public Minimap createMinimap(Player var1, MinimapScreenPosition var2, MinimapLayer var3, Map<String, SecondaryMinimapLayer> var4);

    default public Minimap createMinimap(Player player, MinimapScreenPosition minimapScreenPosition, MinimapLayer minimapLayer) {
        return this.createMinimap(player, minimapScreenPosition, minimapLayer, null);
    }

    default public Minimap createMinimap(Player player, MinimapScreenPosition minimapScreenPosition, MinimapLayerRenderer minimapLayerRenderer) {
        return this.createMinimap(player, minimapScreenPosition, this.createMinimapLayer(player.getWorld(), minimapLayerRenderer));
    }
}

