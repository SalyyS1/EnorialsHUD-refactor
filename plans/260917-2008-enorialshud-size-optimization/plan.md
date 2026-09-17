---
title: "EnorialsHUD Plugin Size Optimization"
description: "Giảm kích thước plugin JAR từ ~40-50MB xuống <10MB bằng cách externalize SQLite driver và resource pack textures"
status: pending
priority: P1
effort: "15-20h"
tags: [minecraft, optimization, refactoring]
created: 2026-09-17
---

# EnorialsHUD Plugin Size Optimization

## Overview

EnorialsHUD là Minecraft plugin cung cấp HUD, minimap, GPS, và party system với kích thước JAR hiện tại ~40-50MB. Kích thước lớn chủ yếu do:
- SQLite JDBC driver được shade vào JAR (~2-3MB)
- Resource pack textures nhúng sẵn trong resources (~50-55MB)
- 100+ font JSON files bundle trong JAR (~5-8MB)

Plan này externalize các tài nguyên nặng ra bên ngoài, giảm JAR size xuống <10MB và cải thiện startup time, memory footprint.

## Goals

| # | Goal | Priority | Target |
|---|------|----------|--------|
| 1 | Externalize SQLite driver | P1 | -2-3MB |
| 2 | Externalize resource pack textures | P1 | -50-55MB |
| 3 | Dynamic font generation | P2 | -5-8MB |
| 4 | Final JAR size | P1 | <10MB |
| 5 | Maintain backward compatibility | P1 | 100% |

## Phases

| # | Phase | Status | Effort |
|---|-------|--------|--------|
| 1 | [Setup Build System & SQL Externalization](./phase-01-start.md) | Pending | 6h |
| 2 | [Externalize Resource Pack Textures](./phase-02-texture-externalization.md) | Pending | 8h |
| 3 | [Dynamic Font Asset Generation](./phase-03-font-optimization.md) | Pending | 4h |

## Success Criteria

- [ ] JAR size giảm từ ~40-50MB xuống <10MB (75-80% reduction)
- [ ] SQLite driver load từ `plugins/EnorialsHUD/libs/` thành công
- [ ] Resource pack textures load từ `plugins/EnorialsHUD/packs/` thành công
- [ ] Font assets generate động không ảnh hưởng performance
- [ ] Plugin khởi động nhanh hơn (từ 8-12s xuống 3-5s)
- [ ] Memory footprint giảm ~40% (từ ~200MB xuống ~120MB)
- [ ] Backward compatible với config hiện tại
- [ ] Build system hoàn chỉnh (build.gradle.kts)
- [ ] Test trên server thực với players

## Expected Results

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| JAR Size | 40-50MB | <10MB | -75-80% |
| Memory | ~200MB | ~120MB | -40% |
| Startup | 8-12s | 3-5s | -60% |
| Pack Load | Blocking | Lazy/Async | Non-blocking |

## Risk Assessment

**Medium Risk:**
- Lần đầu player join có thể chậm hơn do download textures (mitigated by pre-warm on startup)
- SQLite driver download fail cần fallback (mitigated by bundled minimal driver)
- Existing servers cần migration path (mitigated by auto-migration script)

**Low Risk:**
- Code refactoring có thể gây regression (mitigated by comprehensive testing)
- Font generation performance (mitigated by caching)

## Dependencies

- Paper API 1.21+ (existing)
- SQLite JDBC 3.46.0.0 (external)
- Gson (existing, shaded)
- Build tool: Gradle 8+ with Shadow plugin

## Next Steps

1. Tạo `build.gradle.kts` với proper dependency management
2. Implement ExternalLibraryLoader cho SQLite
3. Refactor PackManager để support external packs
4. Create migration tool cho existing servers
5. Test trên test server trước khi deploy production

<!-- slug: enorialshud-size-optimization -->