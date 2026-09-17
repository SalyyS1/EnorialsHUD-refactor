/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockBurnEvent
 *  org.bukkit.event.block.BlockEvent
 *  org.bukkit.event.block.BlockExplodeEvent
 *  org.bukkit.event.block.BlockFadeEvent
 *  org.bukkit.event.block.BlockFertilizeEvent
 *  org.bukkit.event.block.BlockFormEvent
 *  org.bukkit.event.block.BlockFromToEvent
 *  org.bukkit.event.block.BlockGrowEvent
 *  org.bukkit.event.block.BlockIgniteEvent
 *  org.bukkit.event.block.BlockPhysicsEvent
 *  org.bukkit.event.block.BlockPistonExtendEvent
 *  org.bukkit.event.block.BlockPistonRetractEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.block.BlockRedstoneEvent
 *  org.bukkit.event.block.BlockSpreadEvent
 *  org.bukkit.event.block.EntityBlockFormEvent
 *  org.bukkit.event.block.FluidLevelChangeEvent
 *  org.bukkit.event.block.LeavesDecayEvent
 *  org.bukkit.event.block.MoistureChangeEvent
 *  org.bukkit.event.block.SpongeAbsorbEvent
 *  org.bukkit.event.entity.EntityChangeBlockEvent
 *  org.bukkit.event.entity.EntityExplodeEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 *  org.joml.Vector4i
 */
package sk.enorials.minimap.listener;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Vector4i;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.map.renderer.world.cache.WorldMapCache;

public class MinimapBlockListener
implements Listener {
    private final EnorialsMinimap plugin;
    private final Set<WorldMapCache<?>> registeredCache = new LinkedHashSet();
    private final LinkedHashSet<Block> pending = new LinkedHashSet();
    private BukkitTask task;
    private Class<? extends Event> tntEvent;
    private Method primingBlock;

    public MinimapBlockListener(EnorialsMinimap enorialsMinimap) {
        this.plugin = enorialsMinimap;
    }

    public void registerListener(Plugin plugin) {
        for (Class object : List.of(BlockBreakEvent.class, BlockBurnEvent.class, BlockExplodeEvent.class, BlockFadeEvent.class, BlockFertilizeEvent.class, BlockFromToEvent.class, BlockGrowEvent.class, BlockIgniteEvent.class, BlockPistonExtendEvent.class, BlockPistonRetractEvent.class, BlockPlaceEvent.class, BlockPhysicsEvent.class, BlockRedstoneEvent.class, FluidLevelChangeEvent.class, LeavesDecayEvent.class, MoistureChangeEvent.class, SpongeAbsorbEvent.class, BlockFormEvent.class, EntityBlockFormEvent.class, BlockSpreadEvent.class, EntityExplodeEvent.class, EntityChangeBlockEvent.class)) {
            Bukkit.getPluginManager().registerEvent(object, (Listener)this, EventPriority.MONITOR, (listener, event) -> this.changed(event), plugin, true);
        }
        for (String string : List.of("org.bukkit.event.block.TNTPrimeEvent", "com.destroystokyo.paper.event.block.TNTPrimeEvent")) {
            try {
                this.tntEvent = Class.forName(string).asSubclass(Event.class);
                try {
                    this.primingBlock = this.tntEvent.getMethod("getPrimingBlock", new Class[0]);
                }
                catch (NoSuchMethodException noSuchMethodException) {
                    this.primingBlock = null;
                }
                Bukkit.getPluginManager().registerEvent(this.tntEvent, (Listener)this, EventPriority.MONITOR, (listener, event) -> this.changed(event), plugin, true);
                break;
            }
            catch (ClassNotFoundException classNotFoundException) {
            }
        }
    }

    private void enqueue(Block block) {
        if (block == null || this.registeredCache.isEmpty()) {
            return;
        }
        boolean bl = false;
        for (WorldMapCache<?> worldMapCache : this.registeredCache) {
            int n = block.getX() + 64;
            int n2 = block.getZ() + 64;
            if (worldMapCache.getCached(block.getWorld(), n, n2) == null && worldMapCache.getCached(block.getWorld(), n - 1, n2) == null && worldMapCache.getCached(block.getWorld(), n + 1, n2) == null && worldMapCache.getCached(block.getWorld(), n, n2 - 1) == null && worldMapCache.getCached(block.getWorld(), n, n2 + 1) == null) continue;
            bl = true;
            break;
        }
        if (!bl) {
            return;
        }
        this.pending.add(block);
        if (this.task == null) {
            this.task = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin.owner(), this::drain, 1L, 1L);
        }
    }

    private void changed(Event event) {
        BlockEvent blockEvent;
        if (event instanceof BlockEvent) {
            blockEvent = (BlockEvent)event;
            this.enqueue(blockEvent.getBlock());
        }
        if (event instanceof EntityChangeBlockEvent) {
            blockEvent = (EntityChangeBlockEvent)event;
            this.enqueue(blockEvent.getBlock());
        } else if (event instanceof EntityExplodeEvent) {
            EntityExplodeEvent entityExplodeEvent = (EntityExplodeEvent)event;
            entityExplodeEvent.blockList().forEach(this::enqueue);
        } else if (event instanceof BlockExplodeEvent) {
            BlockExplodeEvent blockExplodeEvent = (BlockExplodeEvent)event;
            blockExplodeEvent.blockList().forEach(this::enqueue);
        } else if (event instanceof BlockFertilizeEvent) {
            BlockFertilizeEvent blockFertilizeEvent = (BlockFertilizeEvent)event;
            blockFertilizeEvent.getBlocks().forEach(blockState -> this.enqueue(blockState.getBlock()));
        } else if (event instanceof SpongeAbsorbEvent) {
            SpongeAbsorbEvent spongeAbsorbEvent = (SpongeAbsorbEvent)event;
            spongeAbsorbEvent.getBlocks().forEach(blockState -> this.enqueue(blockState.getBlock()));
        } else if (event instanceof BlockFromToEvent) {
            BlockFromToEvent blockFromToEvent = (BlockFromToEvent)event;
            this.enqueue(blockFromToEvent.getToBlock());
        } else if (event instanceof BlockBurnEvent) {
            BlockBurnEvent blockBurnEvent = (BlockBurnEvent)event;
            this.enqueue(blockBurnEvent.getIgnitingBlock());
        } else if (event instanceof BlockIgniteEvent) {
            BlockIgniteEvent blockIgniteEvent = (BlockIgniteEvent)event;
            this.enqueue(blockIgniteEvent.getIgnitingBlock());
        } else if (this.tntEvent != null && this.tntEvent.isInstance(event) && this.primingBlock != null) {
            try {
                this.enqueue((Block)this.primingBlock.invoke((Object)event, new Object[0]));
            }
            catch (ReflectiveOperationException reflectiveOperationException) {
                throw new IllegalStateException("Cannot read TNT priming block", reflectiveOperationException);
            }
        } else if (event instanceof BlockPistonExtendEvent) {
            BlockPistonExtendEvent blockPistonExtendEvent = (BlockPistonExtendEvent)event;
            blockPistonExtendEvent.getBlocks().forEach(block -> {
                this.enqueue((Block)block);
                this.enqueue(block.getRelative(blockPistonExtendEvent.getDirection()));
            });
        } else if (event instanceof BlockPistonRetractEvent) {
            BlockPistonRetractEvent blockPistonRetractEvent = (BlockPistonRetractEvent)event;
            blockPistonRetractEvent.getBlocks().forEach(block -> {
                this.enqueue((Block)block);
                this.enqueue(block.getRelative(blockPistonRetractEvent.getDirection()));
            });
        }
    }

    private void drain() {
        long l = System.nanoTime() + 1000000L;
        for (int i = 0; i < 64 && !this.pending.isEmpty() && System.nanoTime() < l; ++i) {
            Iterator iterator = this.pending.iterator();
            Block block = (Block)iterator.next();
            iterator.remove();
            this.update(block);
        }
        if (this.pending.isEmpty()) {
            this.task.cancel();
            this.task = null;
        }
    }

    public void registerCache(WorldMapCache<?> worldMapCache) {
        this.registeredCache.add(worldMapCache);
    }

    public void unregisterCache(WorldMapCache<?> worldMapCache) {
        this.registeredCache.remove(worldMapCache);
    }

    public Set<WorldMapCache<?>> getRegisteredCache() {
        return this.registeredCache;
    }

    public void update(Block block) {
        if (block != null) {
            for (WorldMapCache<?> worldMapCache : this.registeredCache) {
                this.update(block, worldMapCache, true);
            }
        }
    }

    protected void updateBlock(Block block, WorldMapCache<?> worldMapCache) {
        if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
            return;
        }
        byte[] byArray = worldMapCache.getCached(block.getWorld(), block.getX() + 64, block.getZ() + 64);
        if (byArray == null) {
            return;
        }
        int n = Math.floorMod(block.getX() - 64, 128);
        int n2 = Math.floorMod(block.getZ() - 64, 128);
        worldMapCache.getRenderer().updateBlock(block, (127 - n2) * 128 + 127 - n, byArray);
    }

    protected void update(Block block, WorldMapCache<?> worldMapCache, boolean bl) {
        int n;
        if (block == null) {
            return;
        }
        this.updateBlock(block, worldMapCache);
        int n2 = n = bl ? 1 : 0;
        if (bl) {
            for (BlockFace blockFace : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
                this.updateBlock(block.getRelative(blockFace), worldMapCache);
            }
        }
        worldMapCache.notifyDirtyArea(block.getWorld(), new Vector4i(block.getX() - n, block.getZ() - n, 1 + 2 * n, 1 + 2 * n));
    }
}

