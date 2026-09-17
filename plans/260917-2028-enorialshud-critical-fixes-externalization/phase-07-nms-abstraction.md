---
phase: 7
title: "NMS Abstraction Layer (Optional)"
status: pending
priority: P2
effort: "56h"
dependencies: [3]
---

# Phase 7: NMS Abstraction Layer (Optional)

## Overview

**Decision Point:** This phase creates a version abstraction layer for NMS/CraftBukkit access, allowing the plugin to support multiple Paper versions without recompilation. However, it adds significant complexity (56h effort) for potentially limited value if the plugin only targets one Paper version.

**Recommendation pending advisory input:** If kongming/researchers recommend locking to one exact Paper build (e.g., Paper 1.21-26.2) and documenting that requirement, skip this phase entirely and move directly to Phase 8. The plugin would validate Paper version on startup and fail fast if incompatible. This is simpler, safer for decompiled code, and standard practice for many complex plugins.

**If proceeding:** Create adapter interfaces and version-specific implementations for NMS operations, using runtime detection to load the correct adapter.

## Requirements

**If Implementing:**
- Support Paper 1.21.x across minor updates (26.0 through 26.x)
- Fail fast on unsupported versions with clear error message
- Zero performance overhead from abstraction layer
- NMS code isolated to adapter implementations

**If Skipping (Recommended):**
- Lock exact Paper build tested (e.g., Paper 1.21-26.2-build-123)
- Add startup version check: fail with actionable error if mismatch
- Document supported build in plugin.yml, README, and startup banner
- Update NMS code freely without abstraction overhead

## Architecture (If Implementing)

### NMS Usage Audit

From code review, NMS is used in these areas:

| Feature | NMS Usage | Files | Abstraction Complexity |
|---------|-----------|-------|----------------------|
| Netty packet intercept | `CraftPlayer.getHandle().connection` | `EnorialsMinimap.java:374-391` | Medium - stable API |
| Command dispatcher | `MinecraftServer.getCommandDispatcher()` | `NMSCommandDispatcherAccessor.java` | Low - rarely changes |
| Entity metadata | Direct NMS entity fields | Multiple entity files | High - fields rename often |
| NBT operations | `CraftItemStack.asNMSCopy()` | Item utilities | Low - stable conversion |

### Adapter Pattern

```java
// Version-agnostic interface
public interface NMSAdapter {
    ChannelPipeline getPlayerPipeline(Player player);
    CommandDispatcher<CommandSourceStack> getCommandDispatcher();
    void setEntityMetadata(Entity entity, String key, Object value);
    ItemStack applyNBT(ItemStack item, Map<String, Object> nbt);
    
    String getVersion();  // "1.21-R0.1"
    boolean isSupported(String paperVersion);
}

// Version-specific implementation
public class NMSAdapter_1_21_R0_1 implements NMSAdapter {
    @Override
    public ChannelPipeline getPlayerPipeline(Player player) {
        return ((CraftPlayer) player).getHandle().connection.connection.channel.pipeline();
    }
    // ... implement each method with 1.21-R0.1 specifics
}

// Loader selects adapter at runtime
public class NMSAdapterLoader {
    private static NMSAdapter adapter;
    
    public static NMSAdapter get() {
        if (adapter == null) {
            String version = Bukkit.getServer().getClass().getPackage().getName()
                .split("\\.")[3]; // "v1_21_R1"
            
            try {
                Class<?> clazz = Class.forName("sk.enorials.nms.NMSAdapter_" + version);
                adapter = (NMSAdapter) clazz.getDeclaredConstructor().newInstance();
                plugin.getLogger().info("Loaded NMS adapter for " + version);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unsupported server version: " + version
                    + ". This plugin supports: 1_21_R1. Download the correct version from "
                    + "https://github.com/enorialshud/releases");
            }
        }
        return adapter;
    }
}
```

**Usage:**
```java
// Before (direct NMS)
ChannelPipeline pipeline = ((CraftPlayer) player).getHandle()
    .connection.connection.channel.pipeline();

// After (via adapter)
ChannelPipeline pipeline = NMSAdapterLoader.get().getPlayerPipeline(player);
```

## Alternative: Version Lock (Recommended)

**Simpler approach** for decompiled code with limited NMS usage:

1. **Document exact Paper build tested:** `paper-1.21-26.2-build-123`

2. **Add startup validation:**
```java
// EnorialsHUD.onEnable()
String minecraftVersion = Bukkit.getVersion(); // "git-Paper-123 (MC: 1.21)"
if (!minecraftVersion.contains("1.21") || !minecraftVersion.contains("Paper")) {
    getLogger().severe("╔════════════════════════════════════════════════╗");
    getLogger().severe("║  UNSUPPORTED SERVER VERSION                    ║");
    getLogger().severe("║  This plugin requires: Paper 1.21 (build 26.2) ║");
    getLogger().severe("║  You are running: " + minecraftVersion);
    getLogger().severe("║  Download Paper: https://papermc.io/downloads  ║");
    getLogger().severe("╚════════════════════════════════════════════════╝");
    Bukkit.getPluginManager().disablePlugin(this);
    return;
}
```

3. **Use NMS freely** — no adapter overhead, clearer code, easier debugging.

4. **Update when Paper updates** — When Paper 1.22 ships, audit NMS call sites, update, test, and release a new plugin version explicitly supporting 1.22.

**Why this is better for decompiled code:**
- Decompiled code already has maintenance burden; adding abstraction increases it
- Most users run the latest Paper build anyway
- Explicit version lock prevents silent breakage and cryptic errors
- 56 hours saved (skip this entire phase)

## Related Code Files (If Implementing)

**NMS Call Sites:**
- Read: `src/sk/enorials/minimap/EnorialsMinimap.java:374-391` (Netty pipeline)
- Read: `src/sk/enorials/hud/nms/NMSCommandDispatcherAccessor.java` (command system)
- Grep: `import net.minecraft` and `import org.bukkit.craftbukkit` (find all NMS usage)

**Adapter Layer (Create):**
- Create: `src/sk/enorials/nms/NMSAdapter.java` (interface)
- Create: `src/sk/enorials/nms/NMSAdapterLoader.java` (runtime selector)
- Create: `src/sk/enorials/nms/v1_21_R1/NMSAdapter_1_21_R1.java` (1.21 impl)
- Create: `src/sk/enorials/nms/v1_21_R2/NMSAdapter_1_21_R2.java` (1.21.1+ impl, if needed)

**Version Check (If Skipping):**
- Modify: `src/sk/enorials/hud/EnorialsHUD.java:onEnable()` (add version validation)
- Modify: `src/main/resources/plugin.yml` (document `api-version: 1.21`)
- Create: `docs/VERSION_REQUIREMENTS.md` (exact builds tested)

## Implementation Steps (If Implementing)

1. **Audit all NMS usage** — Grep for `net.minecraft`, `CraftBukkit`, `getHandle()`, `asNMSCopy()`. Categorize by volatility (stable API vs frequently-changing internals).

2. **Design adapter interface** — One method per NMS operation. Keep interface minimal — only abstract what the plugin actually uses, not every possible NMS operation.

3. **Implement 1.21 adapter** — Version-specific implementation using current NMS imports. This is the baseline that works today.

4. **Refactor call sites** — Replace direct NMS with `NMSAdapterLoader.get().method()`. Compile and test to ensure behavioral equivalence.

5. **Add Paper 1.21.1 adapter** (if released) — Diff NMS changes between 1.21 and 1.21.1, implement second adapter if interfaces changed.

6. **Test version detection** — Mock different Paper versions, confirm correct adapter loads or fails gracefully.

7. **Document supported versions** — README table showing which plugin version supports which Paper versions.

## Implementation Steps (If Skipping - Recommended)

1. **Add version check in onEnable()** — Validate Paper 1.21, fail fast with clear error.

2. **Document build requirement** — Update plugin.yml, README, GitHub releases with exact Paper build tested.

3. **Test validation** — Try loading on Paper 1.20, Spigot 1.21, Purpur 1.21 — confirm error message is actionable.

4. **Skip to Phase 8** — Proceed directly to testing and validation, saving 56 hours.

## Success Criteria

**If Implemented:**
- [ ] Plugin works on Paper 1.21.0, 1.21.1, 1.21.2 without recompilation
- [ ] Adapter loaded at runtime based on server version
- [ ] Unsupported versions fail fast with clear error message
- [ ] Zero performance overhead (adapter calls inline, no reflection)
- [ ] All NMS call sites migrated to adapter interface

**If Skipped:**
- [ ] Version validation added to onEnable() with clear error
- [ ] Exact Paper build documented in plugin.yml, README
- [ ] Plugin tested on exact build specified (e.g., Paper 1.21-26.2-build-123)
- [ ] Error message includes download link for correct Paper version
- [ ] 56 hours saved, moved directly to Phase 8

## Risk Assessment

**HIGH RISK (If Implementing):** Adapter abstraction adds complexity to decompiled code that already has gaps (CFR failed reconstructions). Each new Paper version needs a new adapter.
- **Response:** Consider this a long-term maintenance burden. Most complex plugins (WorldEdit, EssentialsX) use this pattern, but they have original source and active development.

**MEDIUM RISK (If Skipping):** Locking to one Paper version means the plugin breaks on Paper updates until a new release.
- **Response:** Standard practice for many plugins. Communicate version requirement clearly. Most users understand "Plugin X requires Paper 1.21".

**LOW RISK:** Version detection might fail on Purpur/Pufferfish forks.
- **Response:** These forks maintain Paper API compatibility. The version check can accept "Paper", "Purpur", "Pufferfish" in the version string.

## Recommendation

**Skip this phase** unless there is a specific business need to support multiple Paper minor versions without recompilation. The 56-hour investment produces limited value for a decompiled plugin that already requires significant maintenance. A clear version lock with fail-fast validation is simpler, safer, and standard practice.

**Proceed to Phase 8** with version validation implemented as described in "If Skipping" section above.
