---
phase: 1
title: "Setup Build System & SQL Externalization"
status: pending
priority: P1
effort: "6h"
dependencies: []
---

# Phase 1: Setup Build System & SQL Externalization

## Overview

Tạo build infrastructure hoàn chỉnh (build.gradle.kts) và externalize SQLite JDBC driver từ JAR sang external libs folder. Phase này giảm ~2-3MB và thiết lập foundation cho các optimizations tiếp theo.

## Requirements

**Functional:**
- [ ] Tạo `build.gradle.kts` với proper dependency management
- [ ] Implement `ExternalLibraryLoader` để load SQLite từ external folder
- [ ] Refactor tất cả database access points để dùng external SQLite
- [ ] Auto-download SQLite nếu thiếu
- [ ] Maintain backward compatibility với existing databases

**Non-functional:**
- [ ] Build thành công với Gradle 8+
- [ ] SQLite driver load trong <1s
- [ ] Fallback sang bundled driver nếu download fails
- [ ] Không break existing plugin functionality

## Architecture

**New Structure:**
```
plugins/EnorialsHUD/
└── libs/
    └── sqlite-jdbc-3.46.0.0.jar  (tải động)
```

**Component Flow:**
```
Plugin onEnable()
  → ExternalLibraryLoader.loadSQLite()
      → Check libs/sqlite-jdbc-3.46.0.0.jar exists
      → If not: downloadFromMaven()
      → addToClasspath()
  → FriendSqlite, MinimapPlayerDatabase load driver
  → Normal database operations
```

**Key Classes:**
- `ExternalLibraryLoader` - Dynamic classloader for external JARs
- `FriendSqlite`, `MinimapPlayerDatabase` - Updated để call loader trước khi dùng DriverManager

## Related Code Files

**Create:**
- `src/sk/enorials/hud/loader/ExternalLibraryLoader.java`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle/wrapper/gradle-wrapper.properties`

**Modify:**
- `src/sk/enorials/partyhud/friends/FriendSqlite.java:28-50`
- `src/sk/enorials/minimap/storage/MinimapPlayerDatabase.java`
- `src/sk/enorials/gps/data/QuestRouteService.java`
- `src/sk/enorials/gps/data/RoutePersistenceService.java`

## Implementation Steps

1. **Tạo Build System (2h)**
   - Viết `build.gradle.kts` với dependencies
   - Cấu hình Shadow plugin (relocate Gson, không shade SQLite)
   - Setup Paper API, Adventure, ProtocolLib
   - Test build thành công

2. **Implement ExternalLibraryLoader (2h)**
   - Tạo `ExternalLibraryLoader.java`
   - Method `loadSQLite()`: check → download → load
   - Method `downloadFromMaven()`: Maven Central fallback
   - Method `addToClasspath()`: URLClassLoader reflection
   - Error handling và logging

3. **Refactor Database Access (1.5h)**
   - Sửa `FriendSqlite.java`: thêm `ExternalLibraryLoader.loadSQLite()` trước `DriverManager.getConnection()`
   - Sửa `MinimapPlayerDatabase.java`: same pattern
   - Sửa `QuestRouteService.java`, `RoutePersistenceService.java`
   - Verify không break existing code

4. **Testing & Verification (0.5h)**
   - Build plugin với Gradle
   - Test SQLite loading trên clean server
   - Test với existing database files
   - Verify fallback logic

## Success Criteria

- [ ] `build.gradle.kts` builds successfully
- [ ] JAR size giảm ~2-3MB (SQLite không được shade)
- [ ] `ExternalLibraryLoader` downloads SQLite on first run
- [ ] Database operations work normally
- [ ] Existing `.db` files load correctly
- [ ] No SQLite-related errors in logs

## Risk Assessment

**Risks:**
- Classloader reflection có thể fail trên một số Java versions → use `Instrumentation` API as fallback
- Maven Central unreachable → bundle minimal SQLite trong JAR as backup
- Existing servers có SQLite shaded conflict → detect và skip external loading

**Mitigations:**
- Test trên Java 17, 21
- Implement retry logic với exponential backoff
- Clear error messages khi download fails
