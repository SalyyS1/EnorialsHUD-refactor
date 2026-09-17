---
phase: 1
title: "Assessment & Discovery"
status: pending
priority: P1
effort: "16h"
dependencies: []
---

# Phase 1: Assessment & Discovery

## Overview

Comprehensive audit of all 7 CRITICAL issues with file-level evidence, dependency analysis, and strategic sequencing decision. This phase produces the detailed execution plan for phases 2-8, incorporating advisory guidance on whether to fix critical code issues first (safety-first) or externalize resources first (quick win).

**Deliverables:**
- Evidence report for each of 7 critical issues (files, line numbers, proof)
- Dependency graph showing fix sequencing constraints
- Resource externalization strategy with fallback mechanisms
- Build system architecture with shadow/relocation plan
- Risk-mitigated phase sequencing recommendation

## Requirements

**Critical Issue Evidence:**
- [ ] C1 GPS Privacy - map all code paths accepting player targets, verify canSee() gaps
- [ ] C2 Version Compat - list all NMS/CraftBukkit imports, identify Paper API alternatives
- [ ] C3 Empty Catches - locate all 56 catch(Throwable) blocks, classify by severity
- [ ] C4 No Build System - document all dependencies (embedded + external), version matrix
- [ ] C5 Reflection - map all setAccessible calls, identify version-sensitive fields
- [ ] C6 Shutdown Race - trace friends save flow, identify snapshot vs clear ordering
- [ ] C7 Blocking I/O - find all synchronous file writes on main thread (config, data, logs)

**Resource Analysis:**
- [ ] SQLite audit - separate JDBC driver (2MB) from native binaries (24MB by platform)
- [ ] Texture audit - count PNG files, measure by category (icons, UI, minimap, misc)
- [ ] Dependency tree - identify what uses SQLite, what needs textures, coupling analysis
- [ ] Delivery mechanisms - evaluate HTTP serve, first-run download, separate JAR options

**Strategic Decisions:**
- [ ] Phase sequencing - incorporate kongming + researcher advice on critical-first vs. resource-first
- [ ] Build system timing - validate at start (enables dependency management) vs. after fixes
- [ ] NMS abstraction scope - full adapter layer vs. documented Paper build lock
- [ ] Testing strategy - define regression checks with no original test suite

## Related Code Files

**GPS Privacy (C1):**
- Read: `src/main/java/com/enorials/gps/ChatInputService.java:131-145`
- Read: `src/main/java/com/enorials/gps/PlayerTarget.java:41-46`
- Read: `src/main/java/com/enorials/gps/GuiNavigationPicker.java` (has visibility filter for comparison)

**Version Compatibility (C2):**
- Read: `src/main/resources/enorials-release.properties`
- Read: `README.md` or `docs/GPS.md` (version claims)
- Read: `plugin.yml` (api-version)
- Grep: NMS imports across codebase (`import net.minecraft`, `import org.bukkit.craftbukkit`)

**Empty Catch Blocks (C3):**
- Grep: `catch (Throwable` pattern across all Java files
- Read: `src/main/java/com/enorials/manager/ListenerManager.java:496-498` (example)
- Read: `src/main/java/com/enorials/command/MinimapCommand.java:155-158` (printStackTrace)

**Build System (C4):**
- Check: No `build.gradle.kts`, `build.gradle`, `pom.xml` exists
- Read: `plugin.yml` dependencies section
- Audit: `resources/org/sqlite/` (native binaries)
- Audit: `resources/default/pack/` (texture assets)

**Reflection (C5):**
- Grep: `setAccessible(true)` across codebase
- Read: `src/main/java/com/enorials/manager/ListenerManager.java:466-499` (hunger plugin reflection)
- Read: `src/main/java/com/enorials/gui/GuiPatchListener.java:100-114`

**Shutdown Race (C6):**
- Read: `src/main/java/com/enorials/friend/FriendSaveScheduler.java:28-39`
- Read: `src/main/java/com/enorials/friend/FriendSqlite.java:51-74`
- Read: `src/main/java/com/enorials/friend/FriendService.java` (clear() method)
- Read: `src/main/java/com/enorials/EnorialsHUD.java:430-492` (onDisable sequence)

**Blocking I/O (C7):**
- Read: `src/main/java/com/enorials/storage/StorageManager.java:68-206`
- Grep: `config.save(` pattern for synchronous saves
- Grep: `Files.write(` or `FileOutputStream` on main thread

**Resource Structure:**
- List: `resources/org/sqlite/` (native binaries by platform)
- Count: `resources/default/pack/assets/**/*.png` (texture files)
- Read: `src/main/java/com/enorials/pack/PackManager.java` (current pack loading logic)
- Read: `src/main/java/com/enorials/pack/ResourcePackHost.java` (HTTP serving capability)

## Implementation Steps

1. **Await Advisory Responses (30min)**
   - Check if kongming strategic advisor has responded with sequencing recommendation
   - Check if researcher-1 (refactoring patterns) has reported
   - Check if researcher-2 (error handling patterns) has reported
   - If not ready, proceed with evidence gathering and incorporate advice later

2. **Evidence Collection for C1-C7 (6h)**
   - For each critical issue, read identified files and collect exact evidence
   - Document file paths, line numbers, problem patterns, impact assessment
   - Classify severity: blocker (must fix before production) vs. should-fix (before scale)
   - Create evidence report: `plans/reports/assessment-260917-critical-evidence.md`

3. **Resource Audit (3h)**
   - Count and measure embedded resources by category
   - Analyze SQLite usage - which modules need database, JDBC vs native separation
   - Analyze texture usage - which features need textures, lazy loading feasibility
   - Evaluate delivery mechanisms - pros/cons of HTTP vs download vs separate JAR

4. **Dependency Analysis (2h)**
   - Map all embedded libraries (SQLite, ORMLite, Gson, others)
   - Identify version conflicts, security advisories, available updates
   - Design shadow/relocation strategy to avoid plugin conflicts
   - Document build system architecture (Gradle, Shadow plugin, Paper API)

5. **Strategic Sequencing Decision (2h)**
   - Synthesize advisory recommendations from kongming + researchers
   - Evaluate two approaches:
     - **Critical-first:** Fix all 7 issues → then externalize resources → build system last
     - **Build-first:** Create build system → externalize resources → fix issues with proper tooling
   - Consider: build system enables dependency management needed for fixes
   - Consider: resource externalization gives quick JAR size win but doesn't reduce risk
   - **Recommendation:** Build system first (phase 2) → enables proper fixes in phases 3-7

6. **Testing Strategy Design (2h)**
   - Define regression checks with no original test suite
   - Manual test protocol: GPS privacy, version validation, error logging, async I/O
   - Performance baseline: startup time, memory footprint, main thread usage
   - Integration test scenarios: 50+ players, HUD updates, minimap rendering
   - Load test plan: sustained 100-player load, memory leak detection

7. **Risk Assessment & Mitigation (30min)**
   - Document high-risk areas: decompiled code semantics, no test coverage
   - Mitigation strategies: conservative refactoring, extensive manual testing
   - Rollback plan: version control checkpoints, feature flags for new code
   - Production deployment: staged rollout (test server → small prod → full prod)

## Success Criteria

**Evidence Reports Complete:**
- [ ] All 7 critical issues documented with file:line evidence
- [ ] Each issue has severity classification, impact assessment, fix approach
- [ ] Evidence report published: `plans/reports/assessment-260917-critical-evidence.md`

**Resource Analysis Complete:**
- [ ] SQLite separated: JDBC driver (2MB shade) vs natives (24MB external)
- [ ] Texture audit: 818 PNG files categorized by feature, size breakdown
- [ ] Delivery strategy chosen with fallback: HTTP serve OR first-run download
- [ ] External loader design: graceful degradation if resources unavailable

**Build System Design Complete:**
- [ ] Gradle architecture documented: dependencies, shadow, Paper API
- [ ] Relocation strategy: avoid conflicts with other plugins using SQLite/Gson
- [ ] Version lock decision: exact Paper build vs. version range
- [ ] Build verification: reproducible builds, dependency checksums

**Phase Sequencing Finalized:**
- [ ] Advisory recommendations incorporated from kongming + 2 researchers
- [ ] Execution order determined: build-first vs. critical-first (with rationale)
- [ ] Dependencies mapped: which phases block others, parallelizable work
- [ ] Risk-mitigated approach: sequence minimizes regression introduction

**Testing Strategy Defined:**
- [ ] Manual test protocol created for each critical fix
- [ ] Performance baselines established: startup, memory, thread usage
- [ ] Load test plan: 50-100 concurrent players, sustained load
- [ ] Regression detection: automated where possible, manual checklist otherwise

## Risk Assessment

**HIGH RISK:**
- Decompiled code may behave differently than implied by source (CFR artifacts)
- No test coverage means every change has manual regression risk
- **Mitigation:** Conservative refactoring, extensive manual testing, staged rollout

**MEDIUM RISK:**
- Advisory agents may not respond in time for this phase
- Build system creation may reveal hidden conflicts
- **Mitigation:** Proceed with evidence gathering if agents delayed, incorporate advice into phase 2-8 sequencing

**LOW RISK:**
- Assessment is read-only, no code changes
- Evidence collection is straightforward audit work
