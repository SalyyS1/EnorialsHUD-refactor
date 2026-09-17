---
phase: 6
title: "Async I/O & Concurrency Fixes"
status: pending
priority: P1
effort: "48h"
dependencies: [3]
---

# Phase 6: Async I/O & Concurrency Fixes

## Overview

Move all blocking I/O (config saves, database writes, log file operations) off the main server thread using Paper's async scheduler. Fix the remaining concurrency issues: Netty pipeline handler leaks (M4), radar performance cliff (M3), and GPS input validation holes (M2). This phase eliminates main-thread lag spikes and prevents resource leaks that accumulate across hot reloads.

**Critical issue addressed:** Currently every preference change writes the full `data.yml` synchronously on the main thread (`StorageManager.save():202-205`). With 100 players, a burst of menu interactions can cause 10-20 sequential disk writes, each blocking the server tick for 5-50ms. This phase wraps all I/O in async tasks.

## Requirements

**Functional:**
- All config/data saves execute asynchronously without blocking server tick
- Database queries (friends, minimap, profiles) execute on dedicated thread pool
- Radar entity scanning uses spatial index and bounded heap (O(k log n) vs O(n log n))
- Netty packet handlers clean up on player quit (no pipeline leaks)
- GPS coordinate input rejects NaN, Infinity, and out-of-border values

**Non-functional:**
- Config save latency transparent to users (happens in background)
- Database connection pool sized to server player count (5-20 connections)
- Main thread usage reduced by 15-30% under load (measured via Spark profiler)
- No `ConcurrentModificationException` or `UnsatisfiedLinkError` on hot reload

## Architecture

### C7: Async I/O Wrapper

```java
// AsyncFileWriter.java — single writer thread for all plugin file I/O
public class AsyncFileWriter {
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private final Plugin plugin;

    public CompletableFuture<Void> save(Path file, String content) {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.writeString(file, content, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save " + file, e);
            }
        }, writer);
    }

    public void shutdown() {
        writer.shutdown();
        try { writer.awaitTermination(5, TimeUnit.SECONDS); }
        catch (InterruptedException e) { writer.shutdownNow(); }
    }
}
```

**Why single thread:** Serializes writes, prevents concurrent writes to same file (YAML libraries aren't thread-safe), and makes shutdown ordering simple. For this plugin's I/O volume (< 10 writes/sec), one thread is sufficient.

**Database I/O:** ORMLite already uses a connection pool; just verify queries don't call `.get()` or `.join()` on the main thread. Wrap any synchronous query patterns in `CompletableFuture.supplyAsync(..., dbExecutor)`.

### M3: Radar Spatial Index

Current: `EntityRadar.java:74-96` materializes ALL entities in a bounding box, sorts the full list, then takes top 20. With 500 entities in range, that's 500 object allocations and a 500-element sort every 100ms per player.

**Fix:** Spatial grid + bounded max-heap (priority queue).

```java
// RadarCache.java — per-chunk entity grid, updated on entity move
class RadarSpatialIndex {
    private final Map<ChunkPos, Set<LivingEntity>> grid = new ConcurrentHashMap<>();

    void add(LivingEntity entity) {
        ChunkPos chunk = toChunkPos(entity.getLocation());
        grid.computeIfAbsent(chunk, k -> ConcurrentHashMap.newKeySet()).add(entity);
    }

    List<LivingEntity> queryNearby(Location center, double radius, int limit) {
        PriorityQueue<Pair<LivingEntity, Double>> heap =
            new PriorityQueue<>(limit, Comparator.comparingDouble(Pair::getSecond).reversed());

        for (ChunkPos chunk : getChunksInRadius(center, radius)) {
            Set<LivingEntity> entities = grid.get(chunk);
            if (entities == null) continue;
            for (LivingEntity entity : entities) {
                double dist = entity.getLocation().distanceSquared(center);
                if (dist > radius * radius) continue;

                if (heap.size() < limit) {
                    heap.offer(Pair.of(entity, dist));
                } else if (dist < heap.peek().getSecond()) {
                    heap.poll();
                    heap.offer(Pair.of(entity, dist));
                }
            }
        }

        return heap.stream().map(Pair::getFirst)
            .sorted(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(center)))
            .collect(Collectors.toList());
    }
}
```

**Complexity:** O(c × k) scan + O(k log k) heap operations, where c = chunks in radius (~9-25), k = result limit (20). Far cheaper than O(n log n) full sort where n can be 500+.

### M4: Netty Handler Cleanup

```java
// EnorialsMinimap.java:374-391
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    ChannelPipeline pipeline = ((CraftPlayer) player).getHandle()
        .connection.connection.channel.pipeline();
    
    PassengerRewriter rewriter = new PassengerRewriter(this, player);
    pipeline.addBefore("packet_handler", "passenger_rewriter", rewriter);
    this.passengerRewriters.put(player, rewriter);
}

// MISSING: onPlayerQuit to remove handler!
@EventHandler
public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    PassengerRewriter rewriter = this.passengerRewriters.remove(player);
    if (rewriter != null) {
        ChannelPipeline pipeline = ((CraftPlayer) player).getHandle()
            .connection.connection.channel.pipeline();
        
        try {
            if (pipeline.get("passenger_rewriter") != null) {
                pipeline.remove("passenger_rewriter");
            }
        } catch (NoSuchElementException e) {
            // Race with disconnect — acceptable
        }
    }
}
```

**Why this matters:** Each handler holds references to the plugin instance. Without removal, hot reload leaves zombie handlers in the pipeline that reference the old plugin classloader, causing `ClassCastException` on the next packet. After 10 reloads, there are 10 handlers per player, all throwing exceptions.

### M2: GPS Input Validation

```java
// ChatInputService.java:189-206
private void handleCoordinateInput(Player player, String input) {
    String[] parts = input.split("\\s+");
    if (parts.length != 3) {
        player.sendMessage("Usage: <x> <y> <z>");
        return;
    }

    try {
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);

        // NEW: Validate coordinates
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            player.sendMessage("Invalid coordinates (NaN or Infinity)");
            return;
        }

        WorldBorder border = player.getWorld().getWorldBorder();
        if (!isWithinBorder(x, z, border)) {
            player.sendMessage("Coordinates outside world border");
            return;
        }

        if (y < player.getWorld().getMinHeight() || y > player.getWorld().getMaxHeight()) {
            player.sendMessage("Y coordinate out of range");
            return;
        }

        // Proceed with navigation
        startNavigation(player, new Location(player.getWorld(), x, y, z));
    } catch (NumberFormatException e) {
        player.sendMessage("Invalid number format");
    }
}
```

## Related Code Files

**C7 Async I/O:**
- Modify: `src/sk/enorials/hud/storage/StorageManager.java:202-205` (wrap save() in async)
- Create: `src/sk/enorials/hud/io/AsyncFileWriter.java` (single-thread writer)
- Modify: All `config.save(file)` call sites — grep and wrap async

**M3 Radar:**
- Modify: `src/sk/enorials/minimap/radar/EntityRadar.java:74-96`
- Create: `src/sk/enorials/minimap/radar/RadarSpatialIndex.java` (grid + heap)
- Modify: `src/sk/enorials/minimap/radar/RadarUpdateTask.java` (use spatial index)

**M4 Netty Cleanup:**
- Modify: `src/sk/enorials/minimap/EnorialsMinimap.java:374-391` (add quit listener)
- Verify: All `ChannelPipeline.addBefore/addAfter` have corresponding removes

**M2 GPS Validation:**
- Modify: `src/sk/enorials/gps/input/ChatInputService.java:189-206`
- Create: `src/sk/enorials/gps/util/CoordinateValidator.java` (reusable validator)

**Database Async:**
- Audit: `src/sk/enorials/partyhud/friends/FriendSqlite.java` (confirm async wrappers)
- Audit: `src/sk/enorials/minimap/data/MinimapDatabase.java` (confirm async wrappers)

## Implementation Steps

1. **Create `AsyncFileWriter`** — Single-thread executor for all file I/O, `CompletableFuture` API, graceful shutdown with timeout.

2. **Wrap `StorageManager.save()`** — Replace `config.save(file)` with `asyncWriter.save(...)`. Callers don't `.join()` or `.get()` — fire-and-forget.

3. **Audit all config saves** — Grep for `.save(`, `.saveToString()`, `Files.write`, `FileWriter`, `PrintWriter`. Wrap synchronous I/O in `asyncWriter.save()`.

4. **Create `RadarSpatialIndex`** — Chunk-based grid, concurrent maps, entity add/remove on move events, query with bounded heap.

5. **Replace radar scan** — Modify `EntityRadar.java` to use spatial index instead of `world.getLivingEntities()`. Cache per player with 100ms TTL.

6. **Add Netty handler cleanup** — For every `pipeline.addBefore/addAfter`, add corresponding `pipeline.remove` in quit listener. Wrap removal in try/catch for race with disconnect.

7. **Add GPS coordinate validation** — `CoordinateValidator.isValid(x, y, z, world)` checks finite, in-border, in-height-range. Use in all GPS input paths (chat, GUI, API).

8. **Audit database I/O** — Verify ORMLite queries wrapped in async, no `.get()` on main thread. If synchronous patterns exist, wrap in `CompletableFuture.supplyAsync`.

9. **Load test** — 50-100 concurrent players, rapid HUD menu interactions, radar updates, GPS navigation. Use Spark profiler to confirm main thread idle time improved 15-30%.

10. **Hot reload test** — Reload plugin 10 times with players online. Confirm no `ClassCastException`, no duplicate handlers (check via debugger or packet count metrics).

## Success Criteria

- [ ] Zero synchronous file I/O on main thread (verified by Spark profiler)
- [ ] All config/data saves wrapped in `AsyncFileWriter`
- [ ] Radar uses spatial index with bounded heap (O(k log n) confirmed)
- [ ] Main thread usage reduced 15-30% under 50-player load
- [ ] Netty handlers removed on player quit (no pipeline leaks after reload)
- [ ] GPS input rejects NaN, Infinity, out-of-border coordinates
- [ ] No `ConcurrentModificationException` during hot reload
- [ ] Database connection pool tuned to server size (5-20 connections)

## Risk Assessment

**HIGH RISK:** Async file I/O introduces race where shutdown happens before pending writes complete.
- **Signal it broke:** Config changes lost on server shutdown.
- **Pre-decided response:** `AsyncFileWriter.shutdown()` waits up to 5s for pending writes with `awaitTermination()`. Call it in `onDisable()` before closing other components. If timeout exceeded, log WARNING listing which files may not have saved.

**MEDIUM RISK:** Spatial index adds complexity and could have bugs in chunk edge cases (entity on chunk border).
- **Signal:** Radar sometimes misses nearby entities or includes distant ones.
- **Response:** Add tolerance to chunk queries (scan 3x3 instead of 1x1 for center chunk), and fall back to full scan if heap underflows (< 20 results from spatial query).

**MEDIUM RISK:** Netty handler removal could race with player disconnect, causing `NoSuchElementException`.
- **Signal:** Exception logged on player quit: "No such element: passenger_rewriter".
- **Response:** Expected race condition. Wrap removal in try/catch and ignore `NoSuchElementException`. The handler gets garbage collected either way.

**LOW RISK:** Async database queries could deadlock if connection pool exhausted.
- **Response:** Size pool to 2x expected concurrent queries (typical: 5-10 connections for < 200 players). Monitor pool usage via ORMLite metrics.
