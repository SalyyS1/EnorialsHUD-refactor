---
phase: 3
title: "Fix Critical Error Handling & Safety Issues"
status: pending
priority: P1
effort: "40h"
dependencies: [2]
---

# Phase 3: Fix Critical Error Handling & Safety Issues

## Overview

Fix the three safety-critical issues that can cause data loss, privacy violations, or silent production failures: GPS privacy leak (C1), silent exception swallowing (C3), and reflection brittleness (C5). Plus the friends shutdown race (C6) which causes data loss.

These are the highest-severity findings and must land before any resource work, because a smaller JAR does not matter if the plugin leaks staff positions or wipes friend data.

## Requirements

**Functional:**
- GPS must never reveal vanished/hidden players through any code path
- No exception may be silently discarded — every failure is logged with context
- Reflection into external plugins must fail loudly with version context
- Friends data must survive server shutdown under all timing conditions

**Non-functional:**
- Zero `catch (Throwable)` remaining in hot or critical paths
- Reflection failures produce actionable log messages (which plugin, which field, which version)
- Shutdown produces a summary of any cleanup errors instead of stopping at the first

## Architecture

### C1: GPS Privacy — Centralized Authorization

The bug is that authorization is checked at *entry* in some paths (GUI picker) but not others (chat input), and never re-checked during a live route. The fix is a single authorization gate that every target resolution passes through.

```
Target creation paths:
  ChatInputService ──┐
  GUI picker ────────┤
  API callers ───────┼──→ NavigationManager.authorize(viewer, target) ──→ ALLOW / DENY
  BetonQuest bridge ─┘                                                       │
                                                                             ▼
                                              PlayerTarget.resolve() re-checks each tick
                                              → if DENY: stopNavigation() + notify viewer
```

**Authorization rule for player targets:**
```java
// Single source of truth
static boolean canTrack(Player viewer, Player target) {
    if (viewer.equals(target)) return true;
    if (!viewer.canSee(target)) return false;          // vanish-aware
    if (!target.hasPermission("enorials.gps.track.bypass")) { /* policy world check */ }
    if (targetWorldDenied(viewer, target)) return false;
    return true;
}
```

`PlayerTarget.resolve()` calls this every update; on failure it returns `null`, which `NavigationManager` treats as "target lost" and stops the route.

### C3: Exception Handling — Tiered Policy

Not all catches deserve the same treatment. Three tiers:

| Tier | Applies to | Policy |
|------|-----------|--------|
| **Fatal** | Plugin lifecycle, DB init, config load | Log SEVERE + rethrow / disable plugin |
| **Degraded** | Optional integration, single feature | Log WARNING once per site (dedupe) + disable that feature |
| **Recoverable** | Per-player render, per-entity lookup | Log FINE (debug) + return documented fallback |

**Never catch:** `OutOfMemoryError`, `StackOverflowError`, `ThreadDeath`, `LinkageError` — let these propagate. Replace `catch (Throwable)` with specific types. Where a broad catch is genuinely needed (reflective calls), use `catch (ReflectiveOperationException | RuntimeException)`.

**Dedupe logging** so a broken integration in a per-tick loop logs once, not 20 times/sec:
```java
private final Set<String> warnedSites = ConcurrentHashMap.newKeySet();
void warnOnce(String site, String msg, Throwable t) {
    if (warnedSites.add(site)) getLogger().log(Level.WARNING, msg, t);
}
```

### C5: Reflection — Guarded Access with Cache

```java
final class ReflectiveField<T> {
    private final String pluginName, fieldName, expectVersion;
    private volatile Field cached;           // resolved once on success
    private volatile boolean failed;         // negative cache

    T get(Object instance) {
        if (failed) return null;
        Field f = cached;
        if (f == null) {
            f = resolve();                    // may set failed=true, log once
            if (f == null) return null;
            cached = f;
        }
        try { return (T) f.get(instance); }
        catch (IllegalAccessException e) { /* log once, set failed */ return null; }
    }

    private Field resolve() {
        try {
            Field f = plugin.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            getLogger().warning("Plugin " + pluginName + " v" + actualVersion
                + " has no field '" + fieldName + "' (expected for ~" + expectVersion
                + "). Feature disabled. This usually means the plugin was updated.");
            return null;
        }
    }
}
```

**Critically**, the hunger-slot hot path (`ListenerManager.getCachedHungerSlots()`) must stop parsing YAML from disk. Replace the 100ms disk-read cache with an event-driven invalidation: cache indefinitely, invalidate on `InventoryClickEvent`/`InventoryCloseEvent` for that player.

### C6: Friends Shutdown — Snapshot-Before-Clear

```java
// FriendService.shutdown()
public void shutdown() {
    closing = true;                                  // block new queued saves
    Map<UUID, Set<UUID>> snapshot = deepCopy(friendsMap);  // SNAPSHOT FIRST
    scheduler.cancelAndDrain();                      // stop pending async saves
    FriendSqlite.saveAll(plugin, snapshot);          // write the snapshot
    friendsMap.clear();                              // clear only after durable write
}
```

`FriendSaveScheduler` gains a `closing` flag checked before enqueueing, and `cancelAndDrain()` joins the worker thread with a bounded timeout.

## Related Code Files

**C1 GPS Privacy:**
- Modify: `src/sk/enorials/gps/input/ChatInputService.java:131-145` (add canSee check)
- Modify: `src/sk/enorials/gps/nav/target/PlayerTarget.java:41-46` (re-check on resolve)
- Modify: `src/sk/enorials/gps/nav/NavigationManager.java` (central authorize + stop-on-deny)
- Modify: `src/sk/enorials/gps/api/internal/EnorialsGpsApiImpl.java` (API path authorization)
- Read: GUI picker visibility filter (reference implementation)

**C3 Error Handling:**
- Modify: `src/sk/enorials/hud/manager/ListenerManager.java:496-498`
- Modify: `src/sk/enorials/minimap/command/MinimapCommand.java:155-158`
- Modify: `src/sk/enorials/hud/storage/StorageManager.java:39-41`
- Modify: All files matching `catch (Throwable` (56 sites)

**C5 Reflection:**
- Modify: `src/sk/enorials/hud/manager/ListenerManager.java:466-499, 433-531`
- Modify: `src/sk/enorials/gui/GuiPatchListener.java:100-114`
- Create: `src/sk/enorials/hud/util/ReflectiveField.java` (guarded accessor)

**C6 Friends Race:**
- Modify: `src/sk/enorials/partyhud/friends/FriendSaveScheduler.java:28-39`
- Modify: `src/sk/enorials/partyhud/friends/FriendService.java` (shutdown/saveAll)
- Read: `src/sk/enorials/partyhud/friends/FriendSqlite.java:51-74` (delete-then-insert risk)

**Shutdown robustness (M1):**
- Modify: `src/sk/enorials/hud/EnorialsHUD.java:430-492` (per-component try/catch + error summary)

## Implementation Steps

1. **Create `ReflectiveField<T>` helper** — guarded, cached, logged-once accessor. Unit-test the resolve/lookup/negative-cache paths.

2. **Fix C5 reflection sites** — replace all 10 `setAccessible` call sites with `ReflectiveField`. Add version guards where a plugin's field layout is known to change.

3. **Fix hunger-slot hot path** — remove the 100ms YAML disk cache; add event-driven invalidation on inventory events. Verify with a profiler that the render path no longer touches disk.

4. **Fix C1 GPS privacy** — add `canTrack` authorization gate in `NavigationManager`, wire all entry paths through it, add per-resolve re-check in `PlayerTarget`. Test: vanish a player, attempt chat-input tracking, confirm denied.

5. **Fix C3 error handling** — triage all 56 `catch (Throwable)` sites into the three tiers, rewrite each. Remove all `printStackTrace()` in favor of `getLogger().log(...)`.

6. **Fix C6 friends race** — implement snapshot-before-clear with `closing` flag and `cancelAndDrain()`.

7. **Fix M1 shutdown cascade** — wrap every `onDisable` close call in a `tryClose(component, errors::add)` helper; log a summary at the end.

8. **Also apply M2** (GPS NaN/Infinity validation) — shared coordinate parser rejecting non-finite and out-of-border values. Small change, same area as C1, so it rides along.

9. **Build and load-test** — verify plugin enables, all features degrade gracefully when optional plugins absent.

10. **Regression test GPS privacy** — scripted test: staff in vanish, player attempts track via chat + GUI + API, all denied.

## Success Criteria

- [ ] Zero `catch (Throwable)` in codebase (verify with grep)
- [ ] Zero `printStackTrace()` in codebase
- [ ] GPS tracking denied for vanished players via chat, GUI, and API paths
- [ ] GPS route stops automatically if target vanishes mid-route
- [ ] Hungry-slot render path performs zero disk I/O (verified by profiler or code audit)
- [ ] Friends data survives: quit-during-save, shutdown-during-save (test both)
- [ ] Plugin enables with no optional dependencies present (graceful degradation intact)
- [ ] Shutdown logs a summary listing any component that failed to close

## Risk Assessment

**HIGH RISK:** Changing error handling can convert silent degradation into hard failures that block plugin startup.
- **Signal it broke:** Plugin fails to enable on a server where an optional plugin is absent.
- **Pre-decided response:** Re-tier that catch to "degraded" (log + continue), not "fatal". Only plugin lifecycle/DB/config errors should be fatal.

**HIGH RISK:** GPS authorization gate may be too strict and break legitimate tracking (self-tracking, same-team, bypass permission).
- **Signal:** Players report cannot track own party members.
- **Pre-decided response:** Add explicit same-party / same-team allowance and a `enorials.gps.track.bypass` permission for admins.

**MEDIUM RISK:** Reflection guards surface real version mismatches that were previously silent.
- **Signal:** Startup logs show new WARNING about missing fields.
- **Response:** Expected and desired — this is the fix working. Document which plugin versions are supported.

**LOW RISK:** `ReflectiveField` caching introduces staleness if a plugin reloads classes.
- **Response:** Tie cache invalidation to plugin reload events where detectable; otherwise accept staleness within a server session.
