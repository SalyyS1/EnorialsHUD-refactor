/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.block.Block
 */
package sk.enorials.minimap.map.renderer.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.renderer.MinimapLayerRenderer;

public interface WorldMinimapRenderer
extends MinimapLayerRenderer {
    public void renderFully(World var1, int var2, int var3, byte[] var4);

    public void updateBlock(Block var1, int var2, byte[] var3);

    @Override
    default public void render(Minimap minimap, MinimapLayer minimapLayer, byte[] byArray) {
        Location location = minimap.holder().getLocation();
        this.renderFully(location.getWorld(), location.getBlockX() - 64, location.getBlockZ() - 64, byArray);
    }
}

