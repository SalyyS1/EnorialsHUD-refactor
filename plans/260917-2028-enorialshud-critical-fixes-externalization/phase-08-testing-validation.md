---
phase: 8
title: "Testing & Validation"
status: pending
priority: P1
effort: "64h"
dependencies: [3, 4, 5, 6, 7]
---

# Phase 8: Testing & Validation

## Overview

Comprehensive validation of all fixes and resource externalization work with no original test suite available. Establishes regression detection through manual test protocols, performance baselines, load testing, and production staging. This phase confirms the plugin is production-ready: functionally equivalent to the original, safer (critical issues fixed), smaller (70MB → <10MB), and faster (async I/O, spatial optimizations).

**Challenge:** Decompiled code has no unit tests, integration tests, or documented expected behavior. Validation relies on behavioral testing, profiler evidence, and comparative analysis against the original JAR.

## Requirements

**Functional Validation:**
- All 7 critical issues verified fixed with evidence
- HUD, minimap, GPS, friends, trades work identically to original
- Resource externalization transparent to players (textures/SQLite work)
- No visual regression in HUD icons, minimap rendering, custom items
- Fallback mechanisms work (bundled SQLite native, minimal texture pack)

**Performance Validation:**
- JAR size <10MB (from 70MB) — 86% reduction verified
- Startup time reduced 40-60% (measured on identical hardware)
- Main thread blocking reduced by 15-30% under 50-player load (profiler)
- No memory leaks after 8+ hours at 100-player load (heap dump comparison)

**Safety Validation:**
- GPS cannot track vanished players (all code paths tested)
- Error handling produces actionable logs (no silent failures)
- Database integrity maintained across shutdown/restart/crash
- Netty handlers clean up (no leaks after 10 hot reloads)

## Architecture

### Test Protocol Structure

```
Testing Pyramid (inverted — manual-heavy due to no unit tests):

┌──────────────────────────────────────────┐
│  Manual Feature Testing (40h)           │  ← Most time here
│  - GPS, HUD, minimap, friends, trades   │
│  - Fallback scenarios, error paths      │
└──────────────────────────────────────────┘
         ▲
┌──────────────────────────────────────────┐
│  Load Testing (12h)                      │
│  - 50-200 concurrent players             │
│  - Sustained 8h+ runs, memory profiling │
└──────────────────────────────────────────┘
         ▲
┌──────────────────────────────────────────┐
│  Regression Testing (8h)                 │
│  - Compare behavior vs original JAR      │
│  - Screenshot diff, database schema diff │
└──────────────────────────────────────────┘
         ▲
┌──────────────────────────────────────────┐
│  Build Verification (4h)                 │
│  - Reproducible builds, dependency audit │
│  - JAR size, startup time baseline       │
└──────────────────────────────────────────┘
```

### Test Environments

| Environment | Purpose | Players | Duration |
|-------------|---------|---------|----------|
| Local dev | Feature verification, quick iteration | 1 (self) | 2 hours |
| Test server | Integration, multi-player scenarios | 5-10 (staff) | 4 hours |
| Staging | Load testing, memory profiling | 50-100 (bots + real) | 8+ hours |
| Production pilot | Real-world validation | 20-50 (opt-in group) | 1 week |

### Critical Fix Verification Matrix

| Issue | Test Scenario | Pass Criteria | Evidence |
|-------|---------------|---------------|----------|
| **C1 GPS Privacy** | Staff in vanish, player tries chat/GUI/API track | All denied, route stops if vanish mid-nav | Logs show "Player not found" or "Target lost" |
| **C2 Version Compat** | Load on Paper 1.20, Spigot 1.21, correct Paper 1.21 | Fails fast on wrong version, loads on correct | Startup logs show version check result |
| **C3 Error Handling** | Break optional plugin, cause reflection failure, disk full | Plugin degrades gracefully, all errors logged | Zero catch(Throwable), no printStackTrace() |
| **C4 Build System** | `./gradlew clean shadowJar` twice | Identical JARs, dependency checksums match | SHA-256 hash comparison |
| **C5 Reflection** | Remove hunger plugin, update it to incompatible version | Feature disabled, logged once, no spam | Logs show "Plugin X v... has no field..." |
| **C6 Friends Race** | Save friend data, shutdown during save, crash during save | Friends data intact after all scenarios | Database query shows all friendships preserved |
| **C7 Async I/O** | 50 players spam HUD menu changes for 5 min | Main thread never blocks >5ms | Spark profiler shows zero sync file I/O |

### Resource Externalization Verification

**SQLite (Phase 4):**
- [ ] Fresh install downloads natives (or extracts fallback)
- [ ] Database operations identical to original (friends CRUD, minimap data)
- [ ] Works on Windows x64, Linux x64, Linux ARM (Raspberry Pi test)
- [ ] Offline install with bundled Windows fallback works
- [ ] JAR size reduced by 24MB (verified with `du -h`)

**Textures (Phase 5):**
- [ ] Fresh install downloads default pack (or extracts minimal fallback)
- [ ] HUD icons render identically (screenshot comparison)
- [ ] Minimap textures load correctly (waypoints, markers, terrain)
- [ ] Custom items show correct textures in inventory
- [ ] Admin can drop custom pack in `packs/custom/`, plugin detects and serves it
- [ ] JAR size reduced by 26MB (verified with `du -h`)

### Performance Baselines

Measure on identical hardware (same CPU, RAM, disk) for fair comparison:

| Metric | Before (Original) | Target (Fixed) | Measured |
|--------|-------------------|----------------|----------|
| JAR Size | 70MB | <10MB | _TBD_ |
| Startup Time | 8-12s | 3-5s | _TBD_ |
| Memory (50 players) | ~200MB heap | ~120MB heap | _TBD_ |
| Main Thread Idle % | 65% | 80%+ | _TBD_ |
| Config Save Latency | 50ms (sync) | <1ms (async) | _TBD_ |

**Measurement Tools:**
- Spark profiler for thread usage, method timings
- VisualVM or JProfiler for heap dumps, memory leak detection
- Paper's `/timings` for tick performance
- Custom instrumentation for async I/O latency

## Related Code Files

**Test Harness (Create):**
- Create: `test-scripts/manual-test-protocol.md` (step-by-step checklist)
- Create: `test-scripts/gps-privacy-test.sh` (automated vanish + track attempts)
- Create: `test-scripts/load-test-bot.py` (spawn bots, simulate player actions)
- Create: `test-scripts/compare-screenshots.py` (diff HUD visuals vs original)

**Performance Measurement:**
- Create: `test-scripts/measure-startup.sh` (time plugin enable)
- Create: `test-scripts/profile-main-thread.sh` (run Spark, export report)
- Create: `test-scripts/heap-dump-diff.sh` (compare heap before/after load test)

**Regression Evidence:**
- Create: `test-results/260917-baseline-original.md` (original JAR measurements)
- Create: `test-results/260917-validation-fixed.md` (fixed JAR measurements)
- Create: `test-results/screenshots/` (before/after visual comparisons)

## Implementation Steps

1. **Build Verification (4h)**
   - Run `./gradlew clean shadowJar` twice, verify identical SHA-256
   - Audit dependencies with `./gradlew dependencies`, check for known CVEs
   - Measure JAR size, startup time on fresh Paper 1.21 server
   - Verify relocation: `jar tf` shows no `org/sqlite/native`, relocated JDBC present
   - Record baseline metrics in `test-results/260917-baseline-fixed.md`

2. **Critical Fix Verification (8h)**
   - Test C1 GPS privacy: staff in vanish, attempt tracking via all paths (chat, GUI, API)
   - Test C2 version compat: try loading on Paper 1.20, Spigot 1.21, confirm fail-fast
   - Test C3 error handling: grep logs for catch(Throwable), printStackTrace(), confirm zero
   - Test C5 reflection: remove hunger plugin, confirm graceful degradation with logged warning
   - Test C6 friends race: add friends, shutdown during save, verify data intact
   - Test C7 async I/O: Spark profile during 50-player HUD spam, confirm zero main-thread file I/O

3. **Resource Externalization Verification (6h)**
   - Fresh server install: confirm SQLite natives download (or fallback extracts)
   - Fresh server install: confirm texture pack downloads (or minimal fallback extracts)
   - Test SQLite on Windows, Linux, ARM: all platforms load natives correctly
   - Test texture fallback: block download, confirm minimal pack extracts, HUD icons render
   - Admin test: drop custom pack in `packs/custom/`, confirm detection and serving
   - Measure JAR size reduction: original 70MB → fixed <10MB

4. **Feature Regression Testing (12h)**
   - **HUD:** Test all HUD modes (armor, health, hunger, air, etc.), screenshot comparison
   - **Minimap:** Test waypoints, markers, zoom, chunk borders, entity radar
   - **GPS:** Test player tracking, coordinate navigation, BetonQuest integration
   - **Friends:** Test add/remove friends, friend teleport, online status
   - **Trades:** Test money transfer, trade journal, transaction rollback on disconnect
   - **Pack:** Test resource pack delivery (accept/decline), hash validation, custom pack variants

5. **Integration Testing (8h)**
   - Test with ViaVersion (1.20 clients on 1.21 server)
   - Test with PlaceholderAPI (HUD placeholders in other plugins)
   - Test with Vault (economy integration for trades)
   - Test with BetonQuest (GPS waypoint objectives)
   - Test with 5 other soft-depend plugins from plugin.yml
   - Confirm graceful degradation when optional plugins absent

6. **Load Testing (12h)**
   - Spawn 50 bots, simulate HUD updates, minimap rendering, GPS navigation for 30 min
   - Monitor main thread idle %, heap usage, GC frequency
   - Increase to 100 bots, run for 2 hours, confirm no memory leak (heap stable)
   - Increase to 200 bots, sustained 8 hour run, heap dump before/after comparison
   - Spark profiler: confirm radar spatial index O(k log n), async I/O working, no hot sync I/O
   - Record performance metrics in `test-results/260917-validation-load-test.md`

7. **Hot Reload Testing (2h)**
   - Reload plugin 10 times with 10 players online
   - Confirm no `ClassCastException`, no duplicate Netty handlers
   - Confirm no memory leak (heap dump before reload 1, after reload 10)
   - Confirm pipeline handlers removed (check via debugger or packet count)

8. **Production Pilot (1 week, low hands-on time)**
   - Deploy to staging server with 20-50 real players (opt-in group)
   - Monitor for 7 days: logs, crash reports, player feedback
   - Compare metrics vs original: startup time, memory, tick lag
   - If stable, promote to full production; if issues, rollback and iterate

9. **Evidence Collection (4h)**
   - Compile all test results into `test-results/260917-final-validation-report.md`
   - Include: JAR size comparison, startup time graphs, Spark profiler reports
   - Include: Screenshots (HUD comparison), heap dumps (memory leak check)
   - Include: Load test metrics (50/100/200 players), hot reload results
   - Include: Production pilot feedback summary

10. **Documentation Update (4h)**
    - Update README with validated Paper build, installation instructions
    - Document external resource setup (SQLite natives, texture packs)
    - Create troubleshooting guide for common issues (native load failure, pack download)
    - Update CHANGELOG with all fixes, externalization changes, performance improvements

## Success Criteria

**Build Verification:**
- [ ] Reproducible builds: two clean builds produce identical JAR (SHA-256 match)
- [ ] No dependency CVEs (checked via `./gradlew dependencyCheckAnalyze` or manual audit)
- [ ] JAR size <10MB (target: 8-9MB) — verified with `du -h`
- [ ] Startup time <5s on reference hardware (from plugin enable to "Enabled EnorialsHUD")

**Critical Fixes Verified:**
- [ ] C1: GPS privacy leak fixed — all tracking paths check visibility
- [ ] C2: Version validation working — fails fast on wrong Paper version
- [ ] C3: Error handling complete — zero catch(Throwable), proper logging everywhere
- [ ] C4: Build system working — compiles, shades, relocates correctly
- [ ] C5: Reflection guarded — version checks, graceful degradation, logged warnings
- [ ] C6: Friends race fixed — data survives shutdown/crash scenarios
- [ ] C7: Async I/O working — zero main-thread file operations under load

**Resource Externalization Verified:**
- [ ] SQLite natives external — 24MB removed, downloads/fallback works, database ops identical
- [ ] Textures external — 26MB removed, downloads/fallback works, HUD/minimap render identically
- [ ] External loaders resilient — offline install works with bundled fallbacks
- [ ] Admin customization works — custom packs detected, custom SQLite natives loadable

**Performance Validated:**
- [ ] Startup 40-60% faster (8-12s → 3-5s measured)
- [ ] Memory 30-40% lower (200MB → 120MB heap at 50 players)
- [ ] Main thread 15-30% more idle (65% → 80%+ under load)
- [ ] Config saves async — <1ms main thread impact (from 50ms sync)
- [ ] Radar optimized — O(k log n) confirmed via profiler

**No Regressions:**
- [ ] All HUD features work identically (screenshot comparison pass)
- [ ] Minimap rendering unchanged (waypoints, markers, zoom, radar)
- [ ] GPS navigation unchanged (player track, coordinate nav, BetonQuest)
- [ ] Friends system unchanged (CRUD, teleport, online status)
- [ ] Trade system unchanged (money transfer, journal, rollback)

**Load Testing Passed:**
- [ ] 50-player load: 30 min sustained, stable memory, <5% tick lag
- [ ] 100-player load: 2 hour sustained, no memory leak, profiler clean
- [ ] 200-player load: 8 hour sustained, heap stable, no crashes
- [ ] Hot reload: 10 reloads with 10 players, no leaks or exceptions

**Production Ready:**
- [ ] Staging pilot: 1 week with 20-50 real players, zero critical issues
- [ ] Player feedback: HUD/minimap work, no visual glitches, performance improved
- [ ] Rollback plan tested: can revert to original JAR if issues arise
- [ ] Documentation complete: install guide, troubleshooting, admin customization

## Risk Assessment

**HIGH RISK:** No unit tests means every regression must be caught manually.
- **Mitigation:** Comprehensive manual test protocol (12h), screenshot comparison, load testing with profiler evidence. Accept that some edge cases may slip through — production pilot (1 week) catches those.

**HIGH RISK:** Decompiled code may have subtle semantic differences from original that manual testing doesn't catch.
- **Mitigation:** Compare behavior exhaustively against original JAR. For critical paths (GPS, friends, trades), run both versions side-by-side and diff database state, logs, and player experience.

**MEDIUM RISK:** Performance improvements hard to measure without production-like load.
- **Mitigation:** Load testing with bots simulates 50-200 players. Production pilot with real players validates under actual usage patterns.

**MEDIUM RISK:** Resource externalization adds first-run download dependency — network failures could break initial setup.
- **Mitigation:** Bundled fallbacks (Windows SQLite native, 2MB minimal texture pack) ensure plugin works offline. Documented manual download process for airgapped servers.

**LOW RISK:** Version lock (if skipping Phase 7) means plugin breaks on Paper updates.
- **Mitigation:** Explicitly document supported Paper build. Users understand "Plugin X requires Paper 1.21-26.2" — standard practice for complex plugins.
