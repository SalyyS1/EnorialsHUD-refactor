/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongCollection
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.scheduler.BukkitTask
 *  org.joml.Vector4i
 *  org.joml.Vector4ic
 */
package sk.enorials.minimap.map.renderer.world.cache;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Vector4i;
import org.joml.Vector4ic;
import sk.enorials.hud.compat.Numbers;
import sk.enorials.minimap.map.renderer.world.VanillaWorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.cache.CacheableWorldMinimapRenderer;

public class WorldMapCache<R extends CacheableWorldMinimapRenderer> {
    private static final Map<UUID, Integer> WORLD_IDS = new HashMap<UUID, Integer>();
    private final R renderer;
    private final Long2ObjectMap<byte[]> images = new Long2ObjectOpenHashMap();
    private final Long2ObjectMap<Set<UUID>> subscribers = new Long2ObjectOpenHashMap();
    private final Map<UUID, LongSet> subscriptions = new HashMap<UUID, LongSet>();
    private final Map<UUID, Consumer<Vector4ic>> notifications = new HashMap<UUID, Consumer<Vector4ic>>();
    private final LinkedHashMap<Long, Long> idle = new LinkedHashMap();
    private final LinkedHashMap<Long, Job> jobs = new LinkedHashMap();
    private int retainedTiles = 256;
    private long retainNanos = 5000000000L;
    private long tick;
    private BukkitTask worker;
    private boolean working;

    public WorldMapCache(R r) {
        this.renderer = (CacheableWorldMinimapRenderer)Objects.requireNonNull(r);
    }

    public static long getKey(World world, int n, int n2) {
        int n3 = WORLD_IDS.computeIfAbsent(world.getUID(), uUID -> WORLD_IDS.size());
        long l = (long)Math.abs(n) >> 7 | (n < 0 ? 0x2000000L : 0L);
        long l2 = (long)Math.abs(n2) >> 7 | (n2 < 0 ? 0x2000000L : 0L);
        return (long)n3 << 52 | l2 << 26 | l;
    }

    public void configureIdleCache(int n, double d) {
        this.retainedTiles = Numbers.clamp(n, 0, 256) * 64;
        this.retainNanos = (long)(Numbers.clamp(Double.isFinite(d) ? d : 5.0, 0.0, 60.0) * 1.0E9);
        this.evict();
    }

    private void evict() {
        long l = System.nanoTime();
        Iterator<Map.Entry<Long, Long>> iterator = this.idle.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Long> entry = iterator.next();
            if (this.idle.size() <= this.retainedTiles && l - entry.getValue() < this.retainNanos) break;
            long l2 = entry.getKey();
            this.images.remove(l2);
            this.jobs.remove(l2);
            iterator.remove();
        }
        this.stopIfIdle();
    }

    public byte[] getCached(World world, int n, int n2) {
        this.evict();
        return (byte[])this.images.get(WorldMapCache.getKey(world, n >> 7 << 7, n2 >> 7 << 7));
    }

    public byte[] get(World world, int n, int n2) {
        this.evict();
        int n3 = n >> 7 << 7;
        int n4 = n2 >> 7 << 7;
        long l = WorldMapCache.getKey(world, n3, n4);
        byte[] byArray = (byte[])this.images.get(l);
        if (byArray != null) {
            return byArray;
        }
        byArray = new byte[16384];
        this.images.put(l, (Object)byArray);
        if (!this.subscribers.containsKey(l)) {
            this.idle.put(l, System.nanoTime());
        }
        if (this.renderer instanceof VanillaWorldMinimapRenderer) {
            this.jobs.put(l, new Job(world, n3, n4, byArray));
            if (this.worker == null) {
                this.worker = Bukkit.getScheduler().runTaskTimer((Plugin)JavaPlugin.getProvidingPlugin(WorldMapCache.class), this::work, 1L, 1L);
            }
        } else {
            this.renderer.renderFully(world, n3 - 64, n4 - 64, byArray);
        }
        return byArray;
    }

    public void invalidate(World world, int n, int n2) {
        long l = WorldMapCache.getKey(world, n, n2);
        this.images.remove(l);
        this.idle.remove(l);
        this.jobs.remove(l);
        this.stopIfIdle();
    }

    private void stopIfIdle() {
        if (!this.working && this.jobs.isEmpty() && this.worker != null) {
            this.worker.cancel();
            this.worker = null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void work() {
        this.working = true;
        try {
            ++this.tick;
            FileConfiguration fileConfiguration = JavaPlugin.getProvidingPlugin(WorldMapCache.class).getConfig();
            double d = fileConfiguration.getDouble("minimap-terrain-cache.build-budget-ms", 2.0);
            long l = System.nanoTime() + (long)(Numbers.clamp(Double.isFinite(d) ? d : 2.0, 0.25, 5.0) * 1000000.0);
            int n = Numbers.clamp(fileConfiguration.getInt("minimap-terrain-cache.max-patches-per-tick", 32), 1, 64);
            int n2 = this.jobs.size() * 8;
            VanillaWorldMinimapRenderer vanillaWorldMinimapRenderer = (VanillaWorldMinimapRenderer)this.renderer;
            while (n > 0 && n2-- > 0 && !this.jobs.isEmpty() && System.nanoTime() < l) {
                int n3;
                int n4;
                Iterator<Map.Entry<Long, Job>> iterator = this.jobs.entrySet().iterator();
                Map.Entry<Long, Job> entry = iterator.next();
                long l2 = entry.getKey();
                Job job = entry.getValue();
                iterator.remove();
                if (this.images.get(l2) != job.pixels) continue;
                World world = Bukkit.getWorld((UUID)job.world);
                if (world == null) {
                    this.images.remove(l2);
                    this.idle.remove(l2);
                    continue;
                }
                if (job.retryAt > this.tick) {
                    this.jobs.put(l2, job);
                    continue;
                }
                int n5 = Math.min(4, n);
                int n6 = n4 = Integer.MAX_VALUE;
                int n7 = n3 = Integer.MIN_VALUE;
                while (n5 > 0 && job.next < 64 && System.nanoTime() < l) {
                    int n8;
                    ++job.next;
                    if ((job.completed & 1L << n8) != 0L) continue;
                    --n5;
                    --n;
                    if (!vanillaWorldMinimapRenderer.renderPatch(world, job.left, job.top, n8, job.pixels)) continue;
                    job.completed |= 1L << n8;
                    int n9 = job.left + (n8 & 7) * 16;
                    int n10 = job.top + (n8 >> 3) * 16;
                    n4 = Math.min(n4, n9);
                    n6 = Math.min(n6, n10);
                    n3 = Math.max(n3, n9 + 16);
                    n7 = Math.max(n7, n10 + 16);
                }
                if (n3 != Integer.MIN_VALUE) {
                    this.notifyDirtyArea(world, new Vector4i(n4, n6, n3 - n4, n7 - n6));
                }
                if (job.completed == -1L || this.images.get(l2) != job.pixels) continue;
                if (job.next == 64) {
                    job.next = 0;
                    job.retryAt = this.tick + 10L;
                }
                this.jobs.put(l2, job);
            }
        }
        finally {
            this.working = false;
            this.stopIfIdle();
        }
    }

    protected void addViewer(long l2, UUID uUID2) {
        this.idle.remove(l2);
        this.subscriptions.computeIfAbsent(uUID2, uUID -> new LongOpenHashSet()).add(l2);
        ((Set)this.subscribers.computeIfAbsent(l2, l -> new HashSet())).add(uUID2);
    }

    public void addViewer(World world, int n, int n2, UUID uUID) {
        this.addViewer(WorldMapCache.getKey(world, n, n2), uUID);
    }

    protected void removeViewer(long l, UUID uUID) {
        Set set;
        LongSet longSet = this.subscriptions.get(uUID);
        if (longSet != null) {
            longSet.remove(l);
        }
        if ((set = (Set)this.subscribers.get(l)) != null && set.remove(uUID) && set.isEmpty()) {
            this.subscribers.remove(l);
            if (this.images.containsKey(l)) {
                this.idle.put(l, System.nanoTime());
            }
        }
    }

    public void removeViewer(World world, int n, int n2, UUID uUID) {
        this.removeViewer(WorldMapCache.getKey(world, n, n2), uUID);
        this.evict();
    }

    public void releaseViewer(UUID uUID) {
        this.notifications.remove(uUID);
        LongSet longSet = this.subscriptions.remove(uUID);
        if (longSet != null) {
            LongIterator longIterator = longSet.iterator();
            while (longIterator.hasNext()) {
                long l = (Long)longIterator.next();
                this.removeViewer(l, uUID);
            }
        }
        this.evict();
    }

    public void setViewerChunks(UUID uUID, LongCollection longCollection) {
        LongOpenHashSet longOpenHashSet = new LongOpenHashSet(longCollection);
        LongSet longSet = this.subscriptions.get(uUID);
        if (longOpenHashSet.equals((Object)longSet)) {
            return;
        }
        if (longSet != null) {
            for (long l : longSet.toLongArray()) {
                if (longOpenHashSet.contains(l)) continue;
                this.removeViewer(l, uUID);
            }
        }
        Object object = longOpenHashSet.iterator();
        while (object.hasNext()) {
            long l = (Long)object.next();
            this.addViewer(l, uUID);
        }
        this.subscriptions.put(uUID, (LongSet)longOpenHashSet);
        this.evict();
    }

    public void setCallback(UUID uUID, Consumer<Vector4ic> consumer) {
        this.notifications.put(uUID, consumer);
    }

    public void notifyDirtyArea(UUID uUID, Vector4i vector4i) {
        Consumer<Vector4ic> consumer = this.notifications.get(uUID);
        if (consumer != null) {
            consumer.accept((Vector4ic)vector4i);
        }
    }

    public void notifyDirtyArea(World world, Vector4i vector4i) {
        if (vector4i.z() <= 0 || vector4i.w() <= 0) {
            return;
        }
        HashSet hashSet = new HashSet();
        int n = vector4i.x() + 64 >> 7 << 7;
        int n2 = vector4i.y() + 64 >> 7 << 7;
        int n3 = vector4i.x() + vector4i.z() - 1 + 64 >> 7 << 7;
        int n4 = vector4i.y() + vector4i.w() - 1 + 64 >> 7 << 7;
        for (int i = n2; i <= n4; i += 128) {
            for (int j = n; j <= n3; j += 128) {
                Set set = (Set)this.subscribers.get(WorldMapCache.getKey(world, j, i));
                if (set == null) continue;
                hashSet.addAll(set);
            }
        }
        for (UUID uUID : hashSet) {
            this.notifyDirtyArea(uUID, vector4i);
        }
    }

    public Long2ObjectMap<byte[]> getCache() {
        return this.images;
    }

    public Long2ObjectMap<Set<UUID>> getRefCounts() {
        return this.subscribers;
    }

    public Map<UUID, LongSet> getViewers() {
        return this.subscriptions;
    }

    public Map<UUID, Consumer<Vector4ic>> getCallbacks() {
        return this.notifications;
    }

    public R getRenderer() {
        return this.renderer;
    }

    private static final class Job {
        final UUID world;
        final int left;
        final int top;
        final byte[] pixels;
        long completed;
        int next;
        long retryAt;

        Job(World world, int n, int n2, byte[] byArray) {
            this.world = world.getUID();
            this.left = n - 64;
            this.top = n2 - 64;
            this.pixels = byArray;
        }
    }
}

