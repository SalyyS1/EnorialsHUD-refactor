/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.level.material.MapColor
 *  net.minecraft.world.level.material.MapColor$Brightness
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.craftbukkit.CraftWorld
 */
package sk.enorials.minimap.map.renderer.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import sk.enorials.minimap.config.BlockConfig;
import sk.enorials.minimap.map.renderer.world.cache.CacheableWorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.cache.WorldMapCache;

public class VanillaWorldMinimapRenderer
implements CacheableWorldMinimapRenderer {
    private final WorldMapCache<VanillaWorldMinimapRenderer> cache = new WorldMapCache<VanillaWorldMinimapRenderer>(this);

    private void sample(Level level, LevelChunk levelChunk, BlockPos.MutableBlockPos mutableBlockPos, BlockPos.MutableBlockPos mutableBlockPos2, int n, int n2, int n3, Sample sample) {
        BlockState blockState;
        sample.fluidDepth = 0;
        if (level.dimensionType().hasCeiling()) {
            int n4 = n + n2 * 231871;
            sample.color = (((n4 = n4 * n4 * 31287121 + n4 * 11) >> 20 & 1) == 0 ? Blocks.DIRT : Blocks.STONE).defaultBlockState().getMapColor((BlockGetter)level, BlockPos.ZERO);
            sample.height = 100;
            return;
        }
        mutableBlockPos.set(n, 0, n2);
        int n5 = levelChunk.getHeight(Heightmap.Types.WORLD_SURFACE, n, n2) + 1;
        if (n5 <= n3 + 1) {
            blockState = Blocks.BEDROCK.defaultBlockState();
        } else {
            do {
                mutableBlockPos.setY(--n5);
            } while ((blockState = levelChunk.getBlockState((BlockPos)mutableBlockPos)).getMapColor((BlockGetter)level, (BlockPos)mutableBlockPos) == MapColor.NONE && n5 > n3);
            if (n5 > n3 && !blockState.getFluidState().isEmpty()) {
                BlockState blockState2;
                int n6 = n5 - 1;
                mutableBlockPos2.set((Vec3i)mutableBlockPos);
                do {
                    mutableBlockPos2.setY(n6--);
                    blockState2 = levelChunk.getBlockState((BlockPos)mutableBlockPos2);
                    ++sample.fluidDepth;
                } while (n6 > n3 && !blockState2.getFluidState().isEmpty());
                if (!blockState.isFaceSturdy((BlockGetter)level, (BlockPos)mutableBlockPos, Direction.UP)) {
                    blockState = blockState.getFluidState().createLegacyBlock();
                }
            }
        }
        sample.height = n5;
        sample.color = BlockConfig.instance().getResolvedOverrides().get(blockState);
        if (sample.color == null) {
            sample.color = blockState.getMapColor((BlockGetter)level, (BlockPos)mutableBlockPos);
        }
    }

    private static byte color(Sample sample, double d, int n, int n2) {
        double d2;
        double d3;
        MapColor.Brightness brightness = sample.color == MapColor.WATER ? ((d3 = (double)sample.fluidDepth * 0.1 + (double)(n + n2 & 1) * 0.2) < 0.5 ? MapColor.Brightness.HIGH : (d3 > 0.9 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL)) : ((d2 = ((double)sample.height - d) * 4.0 / 5.0 + ((double)(n + n2 & 1) - 0.5) * 0.4) > 0.6 ? MapColor.Brightness.HIGH : (d2 < -0.6 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL));
        return sample.color.getPackedId(brightness);
    }

    @Override
    public void renderFully(World world, int n, int n2, byte[] byArray) {
        ServerLevel serverLevel = ((CraftWorld)world).getHandle();
        int n3 = world.getMinHeight();
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();
        Sample sample = new Sample();
        int n4 = n >> 4;
        int n5 = n2 - 1 >> 4;
        int n6 = (n + 127 >> 4) - n4 + 1;
        int n7 = (n2 + 127 >> 4) - n5 + 1;
        LevelChunk[] levelChunkArray = new LevelChunk[n6 * n7];
        for (int i = 0; i < 128; ++i) {
            double d = 0.0;
            int n8 = i + n;
            for (int j = -1; j < 128; ++j) {
                int n9 = j + n2;
                int n10 = ((n8 >> 4) - n4) * n7 + (n9 >> 4) - n5;
                LevelChunk levelChunk = levelChunkArray[n10];
                if (levelChunk == null) {
                    levelChunkArray[n10] = levelChunk = serverLevel.getChunkSource().getChunkNow(n8 >> 4, n9 >> 4);
                }
                if (levelChunk == null || levelChunk.isEmpty()) continue;
                this.sample((Level)serverLevel, levelChunk, mutableBlockPos, mutableBlockPos2, n8, n9, n3, sample);
                if (j >= 0) {
                    byArray[(127 - j) * 128 + 127 - i] = VanillaWorldMinimapRenderer.color(sample, d, i, j);
                }
                d = sample.height;
            }
        }
    }

    public Snapshot snapshot(World world, int n, int n2, byte[] byArray) {
        return new Snapshot(world, n, n2, byArray);
    }

    public boolean renderPatch(World world, int n, int n2, int n3, byte[] byArray) {
        ServerLevel serverLevel = ((CraftWorld)world).getHandle();
        int n4 = n3 % 8 * 16;
        int n5 = n3 / 8 * 16;
        LevelChunk levelChunk = serverLevel.getChunkSource().getChunkNow(n + n4 >> 4, n2 + n5 >> 4);
        LevelChunk levelChunk2 = serverLevel.getChunkSource().getChunkNow(n + n4 >> 4, n2 + n5 - 1 >> 4);
        if (levelChunk == null || levelChunk2 == null) {
            return false;
        }
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();
        Sample sample = new Sample();
        for (int i = n4; i < n4 + 16; ++i) {
            double d = 0.0;
            for (int j = n5 - 1; j < n5 + 16; ++j) {
                this.sample((Level)serverLevel, j < n5 ? levelChunk2 : levelChunk, mutableBlockPos, mutableBlockPos2, n + i, n2 + j, world.getMinHeight(), sample);
                if (j >= n5) {
                    byArray[(127 - j) * 128 + 127 - i] = VanillaWorldMinimapRenderer.color(sample, d, i, j);
                }
                d = sample.height;
            }
        }
        return true;
    }

    @Override
    public void updateBlock(Block block, int n, byte[] byArray) {
        ServerLevel serverLevel = ((CraftWorld)block.getWorld()).getHandle();
        int n2 = block.getX();
        int n3 = block.getZ();
        int n4 = block.getWorld().getMinHeight();
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();
        Sample sample = new Sample();
        double d = 0.0;
        for (int i = -1; i <= 0; ++i) {
            LevelChunk levelChunk = serverLevel.getChunkSource().getChunkNow(n2 >> 4, n3 + i >> 4);
            if (levelChunk == null || levelChunk.isEmpty()) continue;
            this.sample((Level)serverLevel, levelChunk, mutableBlockPos, mutableBlockPos2, n2, n3 + i, n4, sample);
            if (i == 0) {
                byArray[n] = VanillaWorldMinimapRenderer.color(sample, d, n2, n3);
                continue;
            }
            d = sample.height;
        }
    }

    @Override
    public WorldMapCache<?> getWorldMapCache() {
        return this.cache;
    }

    private static final class Sample {
        MapColor color;
        int height;
        int fluidDepth;

        private Sample() {
        }
    }

    public final class Snapshot {
        private final World world;
        private final int baseX;
        private final int baseZ;
        private final byte[] pixels;
        private final int[] heights = new int[128];
        private int patch;

        private Snapshot(World world, int n, int n2, byte[] byArray) {
            this.world = world;
            this.baseX = n;
            this.baseZ = n2;
            this.pixels = byArray;
        }

        public boolean step() {
            if (this.patch == 64) {
                return true;
            }
            ServerLevel serverLevel = ((CraftWorld)this.world).getHandle();
            int n = this.patch % 8 * 16;
            int n2 = this.patch / 8 * 16;
            int n3 = this.world.getMinHeight();
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();
            Sample sample = new Sample();
            LevelChunk levelChunk = serverLevel.getChunkSource().getChunkNow(this.baseX + n >> 4, this.baseZ + n2 >> 4);
            LevelChunk levelChunk2 = n2 == 0 ? serverLevel.getChunkSource().getChunkNow(this.baseX + n >> 4, this.baseZ - 1 >> 4) : null;
            for (int i = n; i < n + 16; ++i) {
                if (levelChunk2 != null && !levelChunk2.isEmpty()) {
                    VanillaWorldMinimapRenderer.this.sample((Level)serverLevel, levelChunk2, mutableBlockPos, mutableBlockPos2, this.baseX + i, this.baseZ - 1, n3, sample);
                    this.heights[i] = sample.height;
                }
                if (levelChunk == null || levelChunk.isEmpty()) continue;
                for (int j = n2; j < n2 + 16; ++j) {
                    VanillaWorldMinimapRenderer.this.sample((Level)serverLevel, levelChunk, mutableBlockPos, mutableBlockPos2, this.baseX + i, this.baseZ + j, n3, sample);
                    this.pixels[(127 - j) * 128 + 127 - i] = VanillaWorldMinimapRenderer.color(sample, this.heights[i], i, j);
                    this.heights[i] = sample.height;
                }
            }
            return ++this.patch == 64;
        }
    }
}

