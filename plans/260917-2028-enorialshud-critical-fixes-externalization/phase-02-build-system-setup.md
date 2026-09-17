---
phase: 2
title: "Build System & Dependency Setup"
status: pending
priority: P1
effort: "24h"
dependencies: [1]
---

# Phase 2: Build System & Dependency Setup

## Overview

Create a complete Gradle build system that compiles the decompiled sources, manages dependencies, and produces a reproducible JAR. This phase must come before code fixes because it establishes the toolchain (compiler, dependency resolution, shadow/relocation) needed to verify that later phases actually compile and work.

**Why build system first:** Making concurrent-safety or async I/O changes without a compiler to catch errors is reckless. Every fix in phases 3-7 depends on being able to build and run.

## Requirements

**Functional:**
- Gradle wrapper (8.x) committed to repo for reproducible builds
- `build.gradle.kts` with Paper API, SQLite JDBC, ORMLite, Gson dependencies
- Shadow plugin configured for fat JAR with relocation
- Resources included from `resources/` directory
- Build produces `EnorialsHUD-<version>.jar` in `build/libs/`

**Non-functional:**
- Compilation succeeds with Java 21 (Paper 1.21+ baseline)
- No dependency conflicts (verify with `gradle dependencies`)
- Relocation prevents classloader conflicts with other plugins
- Build is reproducible: same source → same artifact hash

## Architecture

**Dependency Strategy:**

| Dependency | Scope | Bundled? | Rationale |
|------------|-------|----------|-----------|
| `io.papermc.paper:paper-api` | compileOnly | No | Provided by server |
| `org.xerial:sqlite-jdbc` | implementation | Shaded | JDBC driver needed at runtime |
| `com.j256.ormlite:ormlite-jdbc` | implementation | Shaded | ORM layer for minimap DB |
| `com.google.code.gson:gson` | compileOnly→shaded | Shaded | Paper bundles Gson but version varies; relocate |
| `net.kyori:adventure-*` | compileOnly | No | Paper bundles Adventure |
| `com.mojang:brigadier` | compileOnly | No | Paper bundles Brigadier |

**Relocation Plan (avoid conflicts):**
```
org.sqlite.*          → sk.enorials.libs.sqlite.*
com.j256.ormlite.*    → sk.enorials.libs.ormlite.*
com.google.gson.*     → sk.enorials.libs.gson.*
```

**Critical constraint:** The 24MB of native SQLite binaries must NOT be shaded. Only the `org.xerial:sqlite-jdbc` Java classes are shaded; native loading is handled in Phase 4.

**Build Output Structure:**
```
build/libs/EnorialsHUD-1.1.16.jar (<10MB)
├── sk/enorials/**/*.class              (compiled plugin)
├── sk/enorials/libs/sqlite/**          (~1MB shaded JDBC, no natives)
├── sk/enorials/libs/ormlite/**         (~600KB shaded ORM)
├── sk/enorials/libs/gson/**            (~300KB shaded Gson)
├── resources/config.yml, plugin.yml
└── resources/default/** (fonts, JSON — NOT textures)
```

## Related Code Files

- Create: `build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`
- Create: `.gitignore` (build/, .gradle/, *.jar except wrapper)
- Modify: `src/**` — move to `src/main/java/` if needed (Gradle convention)
- Modify: `resources/**` — move to `src/main/resources/` if needed
- Verify: `resources/plugin.yml` (main class, version, api-version)

## Implementation Steps

1. **Establish source layout** — Verify/move sources to Gradle-standard `src/main/java` and `src/main/resources`. Current layout is `src/sk/enorials/**` and `resources/**`; decide whether to restructure or configure custom source sets. Prefer configuring custom source sets to minimize file moves and diff noise.

2. **Generate Gradle wrapper** — Pin Gradle 8.x. Commit wrapper JAR and scripts for reproducibility.

3. **Declare repositories and dependencies** — Paper API repo (`https://repo.papermc.io/repository/maven-public/`), Maven Central for SQLite/ORMLite/Gson. Add all 24 soft-depend plugins as `compileOnly` where reflection requires compile-time types (most use `Class.forName` so no compile dependency needed — verify per integration).

4. **Configure Shadow plugin** — Set `relocate()` calls for sqlite, ormlite, gson. Configure `exclude()` to strip native binaries from the shaded SQLite artifact. Add `mergeServiceFiles()` for JDBC driver registration.

5. **Set Java toolchain** — Java 21 target, matching Paper 1.21+. Configure `sourceCompatibility`/`targetCompatibility` and compiler `--release 21`.

6. **Configure resource processing** — Include `resources/` as resource root. Add `processResources` filter for version token substitution if `plugin.yml` uses a placeholder version.

7. **Verify compilation** — Run `./gradlew compileJava` and fix any decompiler-induced compile errors (CFR output sometimes won't compile: missing generics, unreachable code, illegal forward references). Document every fix.

8. **Produce first build** — Run `./gradlew shadowJar`. Measure artifact size. This is the baseline for Phase 4/5 size reduction.

9. **Verify relocation worked** — Inspect JAR contents (`jar tf`), confirm `org/sqlite/` paths are relocated and no `org/sqlite/native/**` present.

10. **Test load on a Paper server** — Drop the built JAR on a test Paper 1.21 server, confirm plugin enables without `NoClassDefFoundError`.

## Success Criteria

- [ ] `./gradlew shadowJar` completes with exit 0
- [ ] Built JAR loads on test Paper 1.21 server without classpath errors
- [ ] JAR contents verified: relocated libs present, native binaries absent
- [ ] Baseline JAR size recorded (expected ~46MB before phases 4-5)
- [ ] All decompiler-induced compile errors documented and fixed
- [ ] Build reproducible: two consecutive clean builds produce identical class output

## Risk Assessment

**HIGH RISK:** Decompiled source may not compile cleanly. CFR output frequently has illegal forward references, missing generic bounds, and unreachable code that `javac` rejects.
- **Signal it broke:** `./gradlew compileJava` fails with errors in files marked "Decompiled with CFR 0.152".
- **Pre-decided response:** Fix each compile error minimally; if a file has CFR artifacts in critical logic (the 4 known: `HudAccess.raw`, `PackManager.lambda$startBuild$0`, `PartyMapTracker.run`, `GuiMinimapPacketSender.iconComponent`), flag it for manual reconstruction and defer to Phase 3.

**MEDIUM RISK:** Missing dependencies not visible from decompiled imports (soft-depend classes referenced only via reflection).
- **Signal:** Runtime `NoClassDefFoundError` on a soft integration path.
- **Response:** Add as `compileOnly`; these are runtime-optional so absence must degrade gracefully.

**MEDIUM RISK:** Relocation breaks reflection that references original package names as strings.
- **Signal:** Runtime `ClassNotFoundException` for a relocated class.
- **Response:** Grep for string literals containing `org.sqlite`, `com.j256`, `com.google.gson` and rewrite to relocated names, or exclude that library from relocation.

**LOW RISK:** Gradle version incompatibility with Java 21 — pin a known-good Gradle 8.x.
