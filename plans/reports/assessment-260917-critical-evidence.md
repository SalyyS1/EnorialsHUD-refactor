# Phase 1 Evidence Report — EnorialsHUD Critical Issues

**Date:** 2026-09-17
**Scope:** Assessment & Discovery (Phase 1)
**Method:** Decompiled source audit (CFR 0.152), resource measurement, dependency mapping
**Scope constraint:** Private server use only — no distribution

---

## Resource Measurements (Verified)

| Category | Path | Size | Files |
|----------|------|------|-------|
| **SQLite natives** | `resources/org/` | **24MB** | 20+ platform binaries |
| **Texture pack** | `resources/default/` | **27MB** | 818 PNG |
| **Social frames** | `resources/social-frames/` | 6.8MB | - |
| **Theme art** | `resources/theme-art/` | 5.4MB | - |
| **Pack profiles** | `resources/pack-profiles/` | 1.4MB | - |
| **META-INF** | `resources/META-INF/` | 1.1MB | - |
| **Hotbar art** | `resources/hotbar-art/` | 652KB | - |
| **Modules** | `resources/modules/` | 517KB | - |
| **Social headings** | `resources/social-headings/` | 216KB | - |
| **Compat shaders** | `resources/compat/` | 55KB | - |
| **Total resources** | `resources/` | **66MB** | 3,448 |
| **Java source** | `src/` | 3.4MB | 399 files |

**Reduction opportunity: 51MB** (SQLite 24MB + textures 27MB) → target <10MB JAR.

---

## C1: GPS Privacy Leak — CONFIRMED

**File:** `src/sk/enorials/gps/input/ChatInputService.java:131-146`

**Evidence:**
```java
private void handlePlayer(Player player, String string) {
    if (!HudAccess.has((CommandSender)player, "enorials.gps.track.player")) {
        this.plugin.msg().send((CommandSender)player, "&cYou do not have permission: ...");
        return;
    }
    if (!this.integrationManager.isPlayerTargetsEnabled()) {
        this.plugin.msg().send((CommandSender)player, "&cPlayer targets are disabled.");
        return;
    }
    Player player2 = Bukkit.getPlayerExact((String)string);
    if (player2 == null) {
        this.plugin.msg().send((CommandSender)player, "&cPlayer not found: &f" + string);
        return;
    }
    this.navigationManager.startNavigation(player,
        new PlayerTarget(player2.getUniqueId(), player2.getName()),
        this.navigationManager.getCurrentOrDefaultMode(player), "chat-player",
        RouteDescriptor.player(player2.getUniqueId().toString()));
}
```

**Defects:**
1. No `player.canSee(player2)` check — vanished players are trackable by exact name
2. Permission check exists but `enorials.gps.track.player` is granted by default in `access.yml`
3. No re-check when target vanishes mid-route

**Comparison:** `GpsGuiService.java` DOES call `canSee(` (confirmed via grep) — the GUI path has the check, the chat path does not. Inconsistent authorization.

**Severity:** CRITICAL — staff position disclosure.

---

## C2: Version Compatibility Contract Chaos — CONFIRMED

**Evidence — three contradictory sources:**

| Source | Claim |
|--------|-------|
| `resources/META-INF/enorials-release.properties:3` | `minecraft=1.21` |
| `resources/plugin.yml:11` | `api-version: '1.19'` |
| Code (19 files) | Direct NMS/CraftBukkit for 26.2/26.3 |

**NMS dependency count:** 19 files with `import net.minecraft` or `import org.bukkit.craftbukkit`, totaling **91 import lines**.

**Files requiring version abstraction:**
```
minimap/command/NMSCommandDispatcherAccessor.java
minimap/clientside/impl/NMSMinimapPacketSender.java
minimap/clientside/impl/NMSClientsideMinimapFactory.java
minimap/injection/PassengerRewriter.java
minimap/clientside/gui/MotionTimePacket.java
minimap/clientside/gui/MinimapMotionClock.java
minimap/map/renderer/world/VanillaWorldMinimapRenderer.java
minimap/map/renderer/world/FlatWorldMinimapRenderer.java
minimap/config/BlockConfig.java
minimap/EnorialsMinimap.java
hud/protocol/VanillaEffectIcons.java
hud/protocol/QuestSidebar.java
hud/menu/GraphicalHudMenu.java
hud/compat/SocialMenuTitle.java
hud/compat/SocialInventoryMask.java
hud/compat/MenuPackets.java
hud/EnorialsHUD.java
+ 2 more
```

**`api-version: '1.19'` does not prove binary compatibility with 1.19+** — it only sets the plugin-loading floor. Direct NMS means the plugin breaks on any Paper internals change.

**Severity:** CRITICAL — silent `LinkageError` on mismatched server builds.

---

## C3: Silent Exception Swallowing — CONFIRMED (with CFR caveat)

**Count:** 25 files contain `catch (Throwable` — the earlier "56" figure was inflated by nested occurrences.

**Confirmed sites:**

| File | Line | Pattern | Impact |
|------|------|---------|--------|
| `ListenerManager.java` | 496-498 | `catch (Throwable) { return null; }` | Reflection failure → HUD hunger silently broken |
| `FriendSaveScheduler.java` | 36-38 | `catch (Throwable) { warning(msg) }` | Save failure partially logged, no stack trace |
| `MinimapCommand.java` | 155-158 | `catch (Throwable) { printStackTrace(); return false; }` | Not using plugin logger |
| `StorageManager.java` | 39-41 | `catch (IOException) { printStackTrace(); }` | File creation failure hidden |
| `FriendSqlite.java` | 80-82 | `catch (SQLException) {}` (empty) | Rollback failure swallowed inside error handler |

**⚠️ CFR ARTIFACT WARNING:** CFR 0.152 can produce empty catch blocks when it fails to reconstruct complex control flow. Each site must be manually verified against the original JAR behavior before "fixing". Do not assume all 25 are genuine defects.

**Policy tiers (from advisory):**
- **Fatal:** lifecycle, DB init, config load → log SEVERE + rethrow
- **Degraded:** optional integration → log WARNING once (dedupe), disable feature
- **Recoverable:** per-player render → log FINE, return documented fallback
- **Never catch:** `OutOfMemoryError`, `StackOverflowError`, `ThreadDeath`, `LinkageError`

**Severity:** CRITICAL — diagnostic blindness in production.

---

## C4: No Build System — CONFIRMED

**Evidence:**
- No `build.gradle`, `build.gradle.kts`, `pom.xml`, or `settings.gradle` anywhere
- `src/` is 399 raw `.java` files in `sk/enorials/**` layout (non-Gradle-standard)
- `resources/` at project root (non-Gradle-standard)
- No dependency declarations of any kind

**Runtime dependencies identified from imports:**
- Paper/Spigot API (server-provided)
- SQLite JDBC — embedded in `resources/org/sqlite/`
- ORMLite (`com.j256.ormlite`) — embedded
- Gson (`com.google.gson`) — server-provided
- Adventure (`net.kyori.adventure`) — server-provided
- Brigadier (`com.mojang.brigadier`) — server-provided

**27 soft-depend plugins** declared in `plugin.yml:13-40`, all accessed via reflection (`Class.forName`) — no compile-time coupling.

**Severity:** CRITICAL — cannot compile, verify, or update anything.

---

## C5: Unguarded Reflection — CONFIRMED

**Count:** 5 files use `setAccessible(true)`.

| File | Purpose | Risk |
|------|---------|------|
| `hud/manager/ListenerManager.java:468-469` | Reads hunger plugin's private `states` field | Silent failure → HUD hunger broken |
| `partyhud/patch/GuiPatchListener.java` | Patches external GUI | Silent failure |
| `partyhud/patch/FriendRequestBridge.java` | Bridges friend requests | Silent failure |
| `partyhud/friends/FriendSaveScheduler.java:44-47` | Reads plugin field from `FriendService` | Silent failure → saves never queue |
| `hud/betonquest/BetonQuestWarningBridge.java` | BQ version detection | Silent failure |

**Hot-path defect:** `ListenerManager.readHungerSlotsFromStateMap()` (line 466) runs in the HUD render loop with a 100ms cache. On cache miss it performs `getDeclaredField` + `setAccessible` + reflective `invoke` per player. Combined with a documented YAML disk-parse fallback, this is main-thread I/O.

**Severity:** CRITICAL — silent feature death + main-thread cost.

---

## C6: Friends Shutdown Data Race — CONFIRMED

**Chain of evidence:**

**`FriendSaveScheduler.java:28-41`** — queue() schedules an async save 40 ticks later:
```java
TASKS.compute(friendService, (fs, task) -> task != null && !task.isCancelled()
    ? task
    : Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
        TASKS.remove(fs);          // ← removes itself BEFORE saving
        try { fs.save(); }
        catch (Throwable t) { plugin.getLogger().warning("... " + t.getMessage()); }
    }, 40L));
```

**`FriendSqlite.java:51-60`** — saveAll deletes the whole table then re-inserts:
```java
statement.executeUpdate("DELETE FROM friends");     // ← wipes table
statement = connection.prepareStatement("INSERT OR IGNORE INTO friends ...");
```

**Race sequence on shutdown:**
1. Player quits → `FriendSaveScheduler.queue()` schedules async save (40 ticks)
2. Server shuts down → `onDisable()` runs → `FriendService` clears in-memory map
3. Async task fires, calls `fs.save()`, serializes the now-EMPTY map
4. `saveAll()` executes `DELETE FROM friends` then inserts nothing
5. **All friend relationships permanently lost**

**Aggravating factor:** Bukkit cancels *scheduled* tasks on disable but cannot stop an async task already executing.

**Severity:** CRITICAL — irreversible data loss.

---

## C7: Main-Thread Blocking I/O — CONFIRMED

**File:** `src/sk/enorials/hud/storage/StorageManager.java:202-205`

```java
public void save() {
    this.config.save(this.file);    // ← synchronous disk write on server thread
}
```

**Callers:** `setTheme`, `setPreference`, `setMarkerSize`, `setQuestLogSize`, `setPartySize`, `setLayout`, and more — every menu interaction triggers a full `data.yml` serialize + write on the main thread.

**Additional hot-path I/O:** `ListenerManager` hunger-slot fallback parses YAML from disk on cache miss (100ms cache window) — per player.

**Severity:** CRITICAL — TPS spike proportional to menu interaction rate.

---

## Dependency Map (for Phase 2 build system)

```
EnorialsHUD (399 classes)
├── paper-api              compileOnly   (server-provided)
├── org.xerial:sqlite-jdbc  implementation  ← 24MB natives, 2MB driver
│   ├── FriendSqlite.java:108  (DriverManager, raw JDBC)
│   └── MinimapPlayerDatabase.java:73  (JdbcPooledConnectionSource)
├── com.j256.ormlite        implementation  ← ~600KB
│   └── MinimapPlayerDatabase.java
├── com.google.gson         compileOnly → relocate on shade
├── net.kyori:adventure-*   compileOnly (server-provided)
├── com.mojang:brigadier    compileOnly (server-provided)
└── 27 soft-depend plugins  reflection-only, no compile coupling
```

**Reference implementation found:** `MinimapPlayerDatabase.java` already does async DB correctly — `JdbcPooledConnectionSource` + single-thread daemon writer (`EnorialsHUD-minimap-save`) + `ConcurrentMap` latest-state + `LinkedHashMap` pending queue. **Use this as the in-repo pattern for fixing `FriendSqlite`.**

---

## Resource Delivery Decision (resolved)

| Asset | Approach | Rationale |
|-------|----------|-----------|
| SQLite natives (24MB) | Extract to `plugins/EnorialsHUD/natives/` on first run | Standard SQLite JDBC behavior; no operational burden |
| Textures (27MB) | **Separate resource JAR** | Avoids network dependency; works on airgapped servers; independent versioning |
| Fonts/JSON/audio | Keep in main JAR | Small, tightly coupled to code |

---

## Severity Summary

| ID | Issue | Severity | Confirmed | Guard Exists Elsewhere |
|----|-------|----------|-----------|------------------------|
| C1 | GPS tracks vanished players | CRITICAL | ✅ | Yes (GUI path) |
| C2 | Version contract chaos | CRITICAL | ✅ | No |
| C3 | Silent exception swallowing | CRITICAL | ✅ | No |
| C4 | No build system | CRITICAL | ✅ | N/A |
| C5 | Unguarded reflection | CRITICAL | ✅ | No |
| C6 | Friends data race on shutdown | CRITICAL | ✅ | No |
| C7 | Main-thread blocking I/O | CRITICAL | ✅ | Yes (Minimap DB) |

---

## Unresolved Questions

1. Does the original (non-decompiled) JAR behave identically for the 25 `catch (Throwable)` sites? CFR artifacts must be separated from genuine defects.
2. Which exact Paper build was the JAR compiled against? The `26.2`/`26.3` markers suggest a post-1.21 internal numbering.
3. Is `access.yml`'s default grant of `enorials.hud.all` (which includes `enorials.gps.track.player`) intentional, or a misconfiguration?
4. Are the 4 CFR-failed methods (`HudAccess.raw`, `PackManager.lambda$startBuild$0`, `PartyMapTracker.run`, `GuiMinimapPacketSender.iconComponent`) functionally load-bearing?

---

**Phase 1 Status:** Evidence collection COMPLETE for all 7 critical issues.
**Next:** Phase 2 — Build System & Dependency Setup (24h).
