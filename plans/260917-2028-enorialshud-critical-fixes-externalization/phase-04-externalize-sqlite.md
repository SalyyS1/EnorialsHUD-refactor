---
phase: 4
title: "Externalize SQLite Dependencies"
status: pending
priority: P1
effort: "32h"
dependencies: [2]
---

# Phase 4: Externalize SQLite Dependencies

## Overview

Remove 24MB of embedded SQLite native binaries from the JAR while keeping the 2MB JDBC driver shaded. Native libraries are downloaded to `plugins/EnorialsHUD/libs/sqlite-natives/` on first run or loaded from bundled fallback. This reduces JAR size by 34% without breaking SQLite functionality.

**Strategy:** Shade the Java JDBC driver into the plugin JAR (it's 2MB and provides the SQLite API), but externalize the platform-specific native binaries (24MB across Windows/Linux/macOS/ARM). The shaded JDBC driver will attempt to load natives from the external directory first, falling back to a bundled minimal set if download fails.

## Requirements

**Functional:**
- SQLite database operations work identically after externalization
- Plugin falls back gracefully if external natives unavailable (bundled Windows x64 only)
- First-run download is automatic and silent, with progress logged
- Admin can pre-populate `libs/sqlite-natives/` for offline environments

**Non-functional:**
- JAR size reduced by ~24MB (70MB → ~46MB before texture removal)
- Native loading adds <100ms to startup (one-time extraction/verification)
- Download resumable if interrupted (checksum verification)
- No dependency on external servers during runtime (post-download)

## Architecture

### Current State (Embedded)

```
EnorialsHUD.jar
└── org/sqlite/native/
    ├── Windows/x86_64/sqlitejdbc.dll     (8.2MB)
    ├── Linux/x86_64/libsqlitejdbc.so     (7.8MB)
    ├── Linux/aarch64/libsqlitejdbc.so    (3.1MB)
    ├── Mac/x86_64/libsqlitejdbc.jnilib   (2.9MB)
    └── Mac/aarch64/libsqlitejdbc.jnilib  (2.4MB)
```

The `org.xerial:sqlite-jdbc` artifact bundles all 5 platform variants. Paper servers typically run Linux x64, but the JAR carries Windows/Mac/ARM natives that never load.

### Target State (Externalized)

```
EnorialsHUD.jar (~46MB)
└── sk/enorials/libs/sqlite/
    ├── SqliteJdbc.class (shaded JDBC API)
    └── NativeLibraryLoader.class (custom loader)

plugins/EnorialsHUD/libs/sqlite-natives/ (external, 24MB total)
├── Windows-x86_64-sqlitejdbc.dll
├── Linux-x86_64-libsqlitejdbc.so
├── Linux-aarch64-libsqlitejdbc.so
├── Mac-x86_64-libsqlitejdbc.jnilib
└── Mac-aarch64-libsqlitejdbc.jnilib

Fallback (bundled in JAR, 8MB):
└── resources/sqlite-natives-fallback/
    └── Windows-x86_64-sqlitejdbc.dll    (most common dev environment)
```

### Loading Sequence

```
Plugin startup:
1. Check `plugins/EnorialsHUD/libs/sqlite-natives/<platform>/` exists
2. If missing → download from GitHub release OR extract bundled fallback
3. Set `org.sqlite.lib.path` system property to external directory
4. Initialize database connections (JDBC driver loads natives from that path)
5. If load fails → log ERROR with platform/path/java.library.path diagnostic
```

**Why external directory:** Lets admins pre-populate natives for airgapped servers, supports server migrations without re-downloading, and allows manual native updates if a security patch ships.

### Shadow Configuration Changes

```kotlin
// build.gradle.kts
shadowJar {
    // Shade JDBC driver API (2MB)
    relocate("org.sqlite", "sk.enorials.libs.sqlite")
    
    // EXCLUDE native binaries from shading (24MB saved)
    exclude("org/sqlite/native/**")
    exclude("org/sqlite/native/**/*.dll")
    exclude("org/sqlite/native/**/*.so")
    exclude("org/sqlite/native/**/*.jnilib")
    exclude("org/sqlite/native/**/*.dylib")
    
    // Include Windows x64 fallback only
    from("fallback-natives") {
        into("sqlite-natives-fallback")
        include("Windows-x86_64-sqlitejdbc.dll")
    }
}
```

## Related Code Files

**JDBC Usage Audit:**
- Read: `src/sk/enorials/partyhud/friends/FriendSqlite.java` (friends database)
- Read: `src/sk/enorials/minimap/data/MinimapDatabase.java` (minimap waypoints, etc.)
- Read: `src/sk/enorials/hud/storage/ProfileDatabase.java` (player profiles)
- Grep: `import org.sqlite` and `import com.j256.ormlite` (ORMLite uses JDBC under the hood)

**Native Loading:**
- Create: `src/sk/enorials/hud/db/SqliteNativeLoader.java` (custom loader, sets lib.path)
- Modify: `src/sk/enorials/hud/EnorialsHUD.java:onEnable()` (call loader before DB init)
- Create: `src/sk/enorials/hud/db/SqliteNativeDownloader.java` (GitHub release fetch)

**Build System:**
- Modify: `build.gradle.kts` (exclude natives from shadow, include fallback)
- Create: `fallback-natives/Windows-x86_64-sqlitejdbc.dll` (extracted from original JAR)

## Implementation Steps

1. **Extract current natives from JAR** — Use `jar xf EnorialsHUD.jar org/sqlite/native/` to pull all 5 platform binaries for reference and fallback bundling.

2. **Create `SqliteNativeLoader`** — On first call, check external directory exists, verify checksums, set `org.sqlite.lib.path` system property. If missing, call `SqliteNativeDownloader`.

3. **Create `SqliteNativeDownloader`** — Fetch from GitHub release or Cloudflare R2 mirror. Download format: `sqlite-natives-<version>.zip` containing all 5 binaries. Extract to `plugins/EnorialsHUD/libs/sqlite-natives/`, verify SHA-256 per file.

4. **Add fallback extraction** — If download fails (offline server, network issue), extract the bundled Windows x64 DLL from JAR resources to external directory. Log WARNING that only Windows x64 will work.

5. **Modify Shadow config** — Add exclude patterns for `org/sqlite/native/**`, include Windows x64 fallback in `resources/sqlite-natives-fallback/`.

6. **Call loader in `onEnable()`** — Before any database initialization:
   ```java
   try {
       SqliteNativeLoader.ensureLoaded(this);
   } catch (IOException e) {
       getLogger().severe("Failed to load SQLite natives: " + e.getMessage());
       getLogger().severe("Database features disabled. See https://docs.enorialshud.com/sqlite-setup");
       disableDatabaseFeatures();
   }
   ```

7. **Test on multiple platforms** — Windows x64, Linux x64, Linux ARM (Raspberry Pi server for edge case). Confirm natives load, databases work, fallback extraction works offline.

8. **Document admin setup** — README section on pre-populating `libs/sqlite-natives/` for airgapped servers, where to get natives, checksum verification.

9. **Rebuild and measure** — `./gradlew clean shadowJar`, verify JAR size drop from ~70MB → ~46MB (24MB removed).

10. **Verify no runtime regression** — Load plugin on test server, create/read/update/delete data in friends/minimap/profile databases, confirm zero functional change.

## Success Criteria

- [ ] JAR size reduced by 24MB (70MB → ~46MB before texture removal)
- [ ] SQLite databases (friends, minimap, profiles) work identically
- [ ] Natives auto-download on first run if missing (logged to console)
- [ ] Bundled Windows x64 fallback extracts if download fails
- [ ] Admin can pre-populate natives (documented in README)
- [ ] Tested on Windows x64, Linux x64, Linux ARM with success
- [ ] `org.sqlite.lib.path` system property set correctly on all platforms
- [ ] Zero functional regression in database operations

## Risk Assessment

**HIGH RISK:** Native loading is platform and JVM-specific. Wrong path, wrong architecture, or wrong JVM (OpenJ9 vs HotSpot) can cause `UnsatisfiedLinkError`.
- **Signal it broke:** Plugin enables but database operations throw `java.lang.UnsatisfiedLinkError: no sqlitejdbc in java.library.path`.
- **Pre-decided response:** Log full diagnostic (platform, arch, JVM vendor, java.library.path, external directory contents). If external load fails, attempt fallback. If fallback also fails, disable DB features gracefully.

**MEDIUM RISK:** Download on first run adds network dependency. Some servers block outbound HTTP.
- **Signal:** Download fails, fallback extracts, but fallback is Windows x64 and server is Linux.
- **Response:** Expected on non-Windows. Admin must pre-populate or allow outbound to GitHub/mirror. Document clearly in README.

**MEDIUM RISK:** Checksum verification could fail if GitHub release is updated mid-download (rare race).
- **Response:** Retry download once, then fall back to bundled. Log that admins should manually verify if persistent.

**LOW RISK:** Shadow relocation could break reflection that references `org.sqlite` as a string.
- **Grep:** Search codebase for string literals containing "org.sqlite". If found, rewrite to "sk.enorials.libs.sqlite" or use `Class.forName` with relocated name.
