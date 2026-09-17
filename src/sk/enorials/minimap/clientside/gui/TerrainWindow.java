/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongArrayList
 *  it.unimi.dsi.fastutil.longs.LongList
 *  org.bukkit.World
 */
package sk.enorials.minimap.clientside.gui;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.bukkit.World;
import sk.enorials.minimap.map.renderer.world.cache.WorldMapCache;

public final class TerrainWindow {
    private TerrainWindow() {
    }

    public static LongList keys(World world, int n, int n2) {
        LongArrayList longArrayList = new LongArrayList(9);
        int n3 = n + 143 + 64 >> 7 << 7;
        int n4 = n2 + 143 + 64 >> 7 << 7;
        for (int i = n2 + 64 >> 7 << 7; i <= n4; i += 128) {
            for (int j = n + 64 >> 7 << 7; j <= n3; j += 128) {
                longArrayList.add(WorldMapCache.getKey(world, j, i));
            }
        }
        return longArrayList;
    }

    public static void copy(int n, int n2, byte[] byArray, Tiles tiles) {
        if (byArray.length != 20736) {
            throw new IllegalArgumentException("terrain window size");
        }
        int n3 = n + 143 + 64 >> 7 << 7;
        int n4 = n2 + 143 + 64 >> 7 << 7;
        for (int i = n2 + 64 >> 7 << 7; i <= n4; i += 128) {
            for (int j = n + 64 >> 7 << 7; j <= n3; j += 128) {
                byte[] byArray2 = tiles.get(j, i);
                int n5 = Math.max(n, j - 64);
                int n6 = Math.min(n + 144, j + 64);
                int n7 = Math.max(n2, i - 64);
                int n8 = Math.min(n2 + 144, i + 64);
                for (int k = n7; k < n8; ++k) {
                    int n9 = (127 - (k - i + 64)) * 128 + 127 - (n5 - j + 64);
                    int n10 = (k - n2) * 144 + n5 - n;
                    for (int i2 = n5; i2 < n6; ++i2) {
                        byArray[n10++] = byArray2[n9--];
                    }
                }
            }
        }
    }

    public static boolean contains(double d, double d2, int n, int n2) {
        return Math.abs(d - (double)n - 72.0) < 3.0 && Math.abs(d2 - (double)n2 - 72.0) < 3.0;
    }

    @FunctionalInterface
    public static interface Tiles {
        public byte[] get(int var1, int var2);
    }
}

