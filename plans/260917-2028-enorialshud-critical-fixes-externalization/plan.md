---
title: "EnorialsHUD Critical Fixes & Resource Externalization"
description: "Fix 7 critical production issues, externalize 50MB embedded resources (SQLite natives + textures), create build system, reduce JAR from 70MB to <10MB"
status: pending
priority: P1
effort: "200-400h"
tags: [minecraft, refactoring, production-readiness, size-optimization, paper-plugin]
created: 2026-09-17
---

# EnorialsHUD Critical Fixes & Resource Externalization

## ⚠️ LEGAL CONSTRAINT — READ FIRST

**THIS WORK IS FOR PRIVATE USE ONLY. MODIFIED VERSIONS CANNOT BE DISTRIBUTED.**

This is decompiled commercial code (CFR 0.152, all 399 files marked "Decompiled with CFR 0.152", signed JAR with ENORIAL.RSA). Decompilation for personal server use is legally gray. Fixing a privacy leak on your own server is defensible. **Distributing modified commercial code is copyright infringement.**

**GO/NO-GO:**
- ✅ **GO:** Private server use, no distribution, authorized refactoring
- ❌ **NO-GO:** Any intent to distribute, sell, or compete with original author

If this is for distribution: STOP. Clean-room reimplementation is the only legal path. If EnorialsHUD is still commercially available, modifying it competes with the original developer.

**Proceed only if:** private server use, no redistribution, original author not commercially harmed.

---

## Overview

EnorialsHUD v1.1.16 is a decompiled commercial Minecraft HUD plugin (399 Java files, 70MB total) with sophisticated features but **7 CRITICAL production issues** and massively bloated embedded resources. Three comprehensive code reviews identified security vulnerabilities, concurrency races, error handling gaps, and no build system. The plugin bundles 24MB SQLite native libraries and 26MB texture assets directly in the JAR, making deployment slow and memory-intensive.

This plan addresses both **safety-critical code issues** and **resource externalization** to produce a production-ready, maintainable plugin with <10MB JAR size for private server use.

## Current State

**Codebase:**
- 399 Java files (all marked "Decompiled with CFR 0.152")
- 3,448 resource files (69.4MB total)
- No build system (no gradle/maven)
- Direct NMS usage without version abstraction
- 56 empty catch(Throwable) blocks silencing critical errors
- 197 synchronized blocks with shutdown race conditions
- Main-thread blocking I/O for all config/data saves

**Resource Breakdown:**
- **24MB** SQLite native binaries (`resources/org/sqlite/` - Windows/Linux/Mac natives)
- **26MB** texture pack assets (818 PNG files in `resources/default/pack/assets/`)
- **16MB** other resources (fonts, JSON, audio)
- **Total:** 66MB resources + 4MB compiled classes = 70MB JAR

**Critical Issues Identified:**
1. **GPS Privacy Leak** - Can track vanished staff without visibility check
2. **Version Compatibility Chaos** - No version validation, direct NMS, breaks on every MC update
3. **Empty Catch Blocks** - 56 instances silence OutOfMemoryError, StackOverflow, all failures
4. **No Build System** - Cannot rebuild, update dependencies, or verify checksums
5. **Reflection Brittleness** - 10 `setAccessible` calls with zero version guards, no fallback
6. **Friends Data Race** - Shutdown sequence clears friendsMap before async save snapshots it
7. **Main-Thread Blocking I/O** - Every config change writes synchronously, causing lag spikes

## Goals

| # | Goal | Priority | Target |
|---|------|----------|--------|
| 1 | Fix all 7 CRITICAL production issues | P1 | 100% fixed with evidence |
| 2 | Externalize SQLite natives (keep JDBC driver only) | P1 | -24MB JAR size |
| 3 | Externalize texture pack to external delivery | P1 | -26MB JAR size |
| 4 | Create complete Gradle build system | P1 | Reproducible builds |
| 5 | Add async I/O layer for all disk operations | P1 | No main-thread blocking |
| 6 | Create NMS abstraction for version compatibility | P2 | Paper 1.21+ support |
| 7 | Reduce final JAR size | P1 | <10MB (from 70MB) |
| 8 | Production-ready error handling & logging | P1 | No silent failures |

## Phases

| # | Phase | Status | Effort | Dependencies |
|---|-------|--------|--------|--------------|
| 1 | [Assessment & Discovery](./phase-01-assessment-discovery.md) | Pending | 16h | - |
| 2 | [Build System & Dependency Setup](./phase-02-build-system-setup.md) | Pending | 24h | 1 |
| 3 | [Fix Critical Error Handling](./phase-03-error-handling-fixes.md) | Pending | 40h | 2 |
| 4 | [Externalize SQLite Dependencies](./phase-04-externalize-sqlite.md) | Pending | 32h | 2 |
| 5 | [Externalize Resource Pack Textures](./phase-05-externalize-textures.md) | Pending | 40h | 2 |
| 6 | [Async I/O & Concurrency Fixes](./phase-06-async-io-concurrency.md) | Pending | 48h | 3 |
| 7 | [NMS Abstraction Layer](./phase-07-nms-abstraction.md) | Pending | 56h | 3 |
| 8 | [Testing & Validation](./phase-08-testing-validation.md) | Pending | 64h | 3,4,5,6,7 |

**Total Estimated Effort:** 320 hours

## Success Criteria

**Critical Fixes (Blockers):**
- [ ] C1: GPS visibility check added - `viewer.canSe(target)` enforced on all GPS tracking paths
- [ ] C2: Version validation on plugin enable - fail fast with exact Paper build requirement
- [ ] C3: All 56 catch(Throwable) replaced - specific exceptions, proper logging, no silent failures
- [ ] C4: Complete build.gradle.kts created - reproducible builds, dependency management, shadow plugin
- [ ] C5: Reflection guards added - version checks before setAccessible, fallback strategies, cached
- [ ] C6: Friends shutdown race fixed - snapshot friendsMap BEFORE clearing, atomic handoff
- [ ] C7: Async I/O implemented - all config/data saves off main thread using Paper scheduler

**Resource Externalization:**
- [ ] SQLite natives externalized - JDBC driver shaded only, 24MB removed from JAR
- [ ] Texture pack externalized - delivered via HTTP or downloaded on first run, 26MB removed
- [ ] External resource loader created - graceful fallback if download fails
- [ ] JAR size reduced to <10MB (from 70MB) - 85% reduction verified

**Production Readiness:**
- [ ] Shutdown robustness - per-component try/catch, cleanup survives partial failures
- [ ] Error logging standardized - plugin.getLogger() everywhere, no printStackTrace()
- [ ] Netty handlers cleaned up - pipeline handlers removed on player quit
- [ ] Build reproducible - same inputs produce same JAR, dependency checksums verified
- [ ] Load tested - 50+ concurrent players, HUD tick + radar + minimap cache stable
- [ ] Paper version documented - exact build tested, compatibility matrix in README

## Expected Results

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| JAR Size | 70MB | <10MB | -86% |
| Startup Time | 8-12s | 3-5s | -60% |
| Memory Footprint | ~200MB | ~120MB | -40% |
| Main Thread Block | Every config save | Zero | 100% async |
| Silent Failures | 56 catch blocks | 0 | Logged + fail fast |
| Version Compat | Breaks silently | Validates on load | Explicit |

## Risk Assessment

**HIGH RISK:**
- Decompiled code may have subtle semantic differences from original (CFR artifacts)
- No original test suite - regression detection relies on manual testing
- SQLite driver externalization could break on some server setups (mitigated by bundled fallback)
- Texture download on first run adds network dependency (mitigated by cached fallback pack)

**MEDIUM RISK:**
- Build system creation may reveal hidden dependency conflicts
- Async I/O refactor could introduce new race conditions if not carefully sequenced
- NMS abstraction adds complexity - may not be worth it if only targeting one Paper version

**LOW RISK:**
- Error handling fixes are mostly additive - low chance of breaking existing behavior
- Resource externalization is well-understood pattern used by major plugins

## Architecture Changes

**Before:**
```
EnorialsHUD.jar (70MB)
├── Compiled classes (4MB)
├── resources/org/sqlite/ (24MB native binaries)
├── resources/default/pack/ (26MB textures)
└── resources/other/ (16MB fonts/audio)
```

**After:**
```
EnorialsHUD.jar (<10MB)
├── Compiled classes (4MB)
├── Shaded JDBC driver (2MB)
└── Core resources only (4MB fonts/audio)

External dependencies:
plugins/EnorialsHUD/
├── libs/
│   └── sqlite-natives/ (24MB - downloaded if missing)
└── packs/
    └── default-pack/ (26MB - served via HTTP or downloaded)
```

## Dependencies

**Build:**
- Gradle 8+ with Shadow plugin
- Java 21 (Paper 1.21+ requirement)
- Paper API 1.21-R0.1-SNAPSHOT

**Runtime:**
- Paper or Purpur 1.21+ (exact build TBD after validation)
- SQLite JDBC 3.46.0.0 (shaded into JAR)
- Native SQLite binaries (external, platform-specific)

**Optional:**
- ViaVersion for multi-version client support
- PlaceholderAPI for placeholder integration
- Vault for economy integration
- 20+ other soft dependencies (documented in plugin.yml)

## Strategic Decisions (Resolved via Advisory Counsel)

**Advisory team:** kongming (strategic advisor) + researcher-1 (refactoring patterns) + researcher-2 (error handling/concurrency)

### Phase Sequencing: BUILD SYSTEM FIRST ✅

**Consensus:** Build system (Phase 2) MUST come before any fixes. You cannot safely fix critical issues without being able to compile and verify changes. Working with decompiled code that won't build is guessing in the dark.

**Rationale:** The 7 critical issues are real production risks, but verifying each fix requires compilation. Externalizing resources before safety fixes means debugging on top of changes. Do risky work first on complete codebase.

### SQLite Strategy: Shade JDBC, Extract Natives ✅

- Keep 2MB JDBC driver shaded with relocation (`sk.enorials.libs.sqlite`)
- Extract 24MB platform-specific natives to `plugins/EnorialsHUD/natives/` on first run
- Current code likely already extracts natives (SQLite JDBC standard behavior)
- No operational burden, reduces JAR to ~10MB

### Texture Delivery: Separate Resource JAR ✅

**NOT download-on-enable** (network dependencies break offline servers).

- Ship two JARs: `EnorialsHUD-1.1.16.jar` (10MB) + `EnorialsHUD-Pack-1.1.16.jar` (26MB)
- Both go in `plugins/` directory
- Clear deployment instructions
- No runtime behavior change
- Can version independently

### NMS Scope: Version Lock Recommended ✅

Skip Phase 7 (NMS abstraction) unless multi-version support is critical. Simpler to lock exact Paper build, validate on startup, fail fast with clear error. Saves 56 hours.

### Testing Strategy: Manual Behavior Verification ✅

No test suite possible with decompiled code and unknown intended behavior.

- Document current behavior BEFORE each fix (screenshots, logs, state)
- Spin up test server AFTER each critical fix
- Test core paths: GUI rendering, database operations, GPS tracking
- Goal: "does it still work" not "does it work correctly"
- Regression detection, not correctness proof

### CFR Decompilation Artifacts ⚠️

**56 empty catch blocks may be CFR artifacts.** CFR 0.152 can mangle complex control flow. Some catches might be failed reconstructions, not actual empty blocks. Verify each one during Phase 3.

## Alternative Approach: Safety Wrapper Plugin

**Faster, legally safer option** (1 week vs 2-3 months):

- Leave original JAR intact
- Create wrapper plugin intercepting dangerous calls
- GPS leak prevention via Bukkit event interception
- Async I/O wrapper via reflection
- Reflection sandboxing
- 80% of value in 10% of time with zero legal risk

Consider this if goal is safe usage rather than "upgrading."

## Next Steps

1. ✅ **Advisory complete** - kongming + 2 researchers reported
2. **User confirmation** - private use only, no distribution intent?
3. **Phase 1: Assessment** - detailed audit with evidence (16h)
4. **Phase 2: Build System** - MUST succeed before any fixes (24h)
5. **Phases 3-8** - implement, test, validate with regression checks

<!-- slug: enorialshud-critical-fixes-externalization -->