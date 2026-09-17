/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.material.MapColor
 *  net.minecraft.world.level.material.MapColor$Brightness
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.craftbukkit.CraftWorld
 *  org.bukkit.craftbukkit.block.data.CraftBlockData
 */
package sk.enorials.minimap.map.renderer.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import sk.enorials.minimap.config.BlockConfig;
import sk.enorials.minimap.map.renderer.world.cache.CacheableWorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.cache.WorldMapCache;

public class FlatWorldMinimapRenderer
implements CacheableWorldMinimapRenderer {
    private final WorldMapCache<FlatWorldMinimapRenderer> cache = new WorldMapCache<FlatWorldMinimapRenderer>(this);

    @Override
    public WorldMapCache<?> getWorldMapCache() {
        return this.cache;
    }

    @Override
    public void renderFully(World world, int n, int n2, byte[] byArray) {
        for (int i = 0; i < 16384; ++i) {
            int n3 = n + 127 - i % 128;
            int n4 = n2 + 127 - i / 128;
            byArray[i] = FlatWorldMinimapRenderer.colour(world.getHighestBlockAt(n3, n4));
        }
    }

    private static byte colour(Block block) {
        BlockState blockState = ((CraftBlockData)block.getBlockData()).getState();
        MapColor mapColor = BlockConfig.instance().getResolvedOverrides().get(blockState);
        if (mapColor == null) {
            mapColor = blockState.getMapColor((BlockGetter)((CraftWorld)block.getWorld()).getHandle(), new BlockPos(block.getX(), block.getY(), block.getZ()));
        }
        int n = Math.floorMod((block.getLightLevel() >> 2) - 1, 4);
        return mapColor.getPackedId(MapColor.Brightness.byId((int)n));
    }

    @Override
    public void updateBlock(Block block, int n, byte[] byArray) {
        byArray[n] = FlatWorldMinimapRenderer.colour(block);
    }
}

