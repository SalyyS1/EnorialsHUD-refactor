---
phase: 5
title: "Externalize Resource Pack Textures"
status: pending
priority: P1
effort: "40h"
dependencies: [2]
---

# Phase 5: Externalize Resource Pack Textures

## Overview

Remove 26MB of embedded texture pack assets from the JAR, delivering them via HTTP from the plugin's built-in pack host or downloading them to `plugins/EnorialsHUD/packs/` on first run. This reduces JAR size by 37% (46MB → 20MB after Phase 4) without breaking HUD/minimap visuals. The plugin already has an HTTP pack server (`ResourcePackHost`) for client delivery — we repurpose it to serve from external files instead of JAR resources.

**Key insight:** The codebase already hosts resource packs over HTTP for client downloads (see `ResourcePackHost.java`, `PackManager.java`). We're just changing the pack *source* from `resources/default/pack/` inside the JAR to `plugins/EnorialsHUD/packs/default/` on disk.

## Requirements

**Functional:**
- HUD icons, minimap textures, and UI elements render identically
- Pack auto-downloads to disk on first run if missing
- Built-in HTTP server serves packs from external directory
- Admins can drop custom packs in `packs/` directory (override default)
- Client pack delivery (hash, prompt) unchanged

**Non-functional:**
- JAR size reduced by ~26MB (46MB → ~20MB)
- Pack load adds <200ms to startup (directory scan + hash cache)
- Supports multiple pack variants (default, HD, minimal) via subfolders
- No dependency on external CDN during runtime (self-hosted HTTP)

## Architecture

### Current State (Embedded)

```
EnorialsHUD.jar
└── resources/default/pack/
    ├── pack.mcmeta                    (metadata)
    ├── assets/minecraft/
    │   ├── textures/
    │   │   ├── item/custom/          (818 PNG files, ~22MB)
    │   │   ├── gui/                  (HUD icons, ~3MB)
    │   │   └── font/                 (custom fonts, ~1MB)
    │   ├── models/item/              (JSON models)
    │   └── sounds.json
    └── assets/enorialshud/           (custom namespace)
```

`PackManager` builds pack ZIPs from these resources, caches them, and serves via `ResourcePackHost` on a configurable port. Clients download `http://<server-ip>:<port>/pack/<hash>.zip`.

### Target State (Externalized)

```
EnorialsHUD.jar (~20MB after both Phase 4 + 5)
└── resources/pack-template/
    └── pack.mcmeta.template          (minimal template for first-run generation)

plugins/EnorialsHUD/packs/
├── default/                          (26MB, downloaded or admin-provided)
│   ├── pack.mcmeta
│   ├── assets/minecraft/textures/...
│   └── assets/enorialshud/...
├── hd/                               (optional HD variant)
└── minimal/                          (optional lightweight pack)

plugins/EnorialsHUD/pack-cache/
└── <hash>.zip                        (built ZIPs served to clients)
```

### Loading Sequence

```
Plugin startup:
1. Check `plugins/EnorialsHUD/packs/default/` exists
2. If missing → download from GitHub release OR generate minimal fallback
3. Scan `packs/` for all pack variants (default, hd, minimal)
4. For each pack: compute SHA-1, build ZIP if changed, cache in `pack-cache/`
5. Start `ResourcePackHost` serving from `pack-cache/` directory
6. Client joins → send pack prompt with URL `http://<ip>:<port>/pack/<hash>.zip`
```

**Why external directory:** Lets admins drop custom texture packs without rebuilding the plugin, supports per-server branding (each server can have different pack variants), and allows pack updates without plugin reload.

### Pack Download Strategy

**Option A (Chosen):** Bundle a minimal 2MB fallback pack in JAR resources (`resources/pack-fallback-minimal.zip`), extract on first run if download fails. The fallback has only essential HUD icons, no minimap textures (minimap degrades to vanilla map visuals).

**Option B (Rejected):** Require admin to manually download and place packs. Too manual, breaks out-of-box experience.

**Option C (Rejected):** Keep textures in JAR but lazy-extract to disk. Saves nothing — JAR still 70MB.

## Related Code Files

**Pack Management:**
- Read: `src/sk/enorials/pack/PackManager.java` (builds and caches packs)
- Read: `src/sk/enorials/pack/ResourcePackHost.java` (HTTP server for client downloads)
- Read: `src/sk/enorials/pack/PackBuilder.java` (ZIP construction from assets)
- Read: `src/sk/enorials/hud/resource/ResourceLoader.java` (loads textures from pack)

**Download/Fallback:**
- Create: `src/sk/enorials/pack/PackDownloader.java` (GitHub release fetch, fallback extraction)
- Modify: `src/sk/enorials/hud/EnorialsHUD.java:onEnable()` (call downloader before PackManager init)

**Build System:**
- Modify: `build.gradle.kts` (exclude `resources/default/pack/`, include 2MB fallback)
- Create: `resources/pack-fallback-minimal.zip` (essential HUD icons only, 2MB)

**Client Delivery (Unchanged):**
- `ResourcePackHost` already serves from a directory — just point it at `pack-cache/` instead of building from JAR resources.

## Implementation Steps

1. **Create minimal fallback pack** — Extract just the essential HUD icons from `resources/default/pack/assets/minecraft/textures/gui/`, package as `pack-fallback-minimal.zip` (target <2MB). Include `pack.mcmeta` with `pack_format: 48` (1.21.4+).

2. **Create `PackDownloader`** — Fetches from GitHub release or Cloudflare R2 mirror. Download format: `enorialshud-pack-default-<version>.zip` (26MB). Extract to `plugins/EnorialsHUD/packs/default/`, verify SHA-256. If download fails, extract `pack-fallback-minimal.zip` from JAR resources and log WARNING.

3. **Modify `PackManager.buildPack()`** — Change asset source from `getResource("default/pack/...")` to `Files.readAllBytes(Paths.get(dataFolder, "packs/default/..."))`. Scan `packs/` directory for all subdirectories (each is a pack variant).

4. **Modify Shadow config** — Exclude `resources/default/pack/**` (saves 26MB). Include `resources/pack-fallback-minimal.zip` (adds 2MB).

5. **Call downloader in `onEnable()`** — Before `PackManager` initializes:
   ```java
   try {
       PackDownloader.ensureDefaultPack(this);
   } catch (IOException e) {
       getLogger().warning("Pack download failed, using minimal fallback");
       PackDownloader.extractFallback(this);
   }
   PackManager.init(this);  // Now loads from external packs/
   ```

6. **Update `ResourcePackHost`** — Verify it already serves from `pack-cache/` directory (it should). If it builds from JAR resources, refactor to always serve from cache dir.

7. **Test pack variants** — Create `packs/hd/` with 2x resolution textures. Confirm `PackManager` detects both `default` and `hd`, builds separate ZIPs, and clients can request either via config.

8. **Document pack customization** — README section on adding custom packs, expected directory structure, how to switch pack variants per-client (config option).

9. **Rebuild and measure** — `./gradlew clean shadowJar`, verify JAR size drop from ~46MB → ~20MB (26MB removed).

10. **Verify no visual regression** — Load plugin, join server, accept pack, confirm all HUD icons, minimap textures, custom items render correctly. Test with and without external pack present (fallback scenario).

## Success Criteria

- [ ] JAR size reduced by 26MB (46MB → ~20MB combined with Phase 4)
- [ ] Default pack auto-downloads on first run (logged to console)
- [ ] Minimal fallback (2MB) extracts if download fails
- [ ] All HUD icons, minimap textures, custom items render identically
- [ ] Admins can add custom pack variants in `packs/` (documented)
- [ ] Multiple pack variants (default, hd, minimal) detected and selectable
- [ ] Client pack delivery (hash, HTTP serve) unchanged
- [ ] Zero visual regression in HUD, minimap, custom items

## Risk Assessment

**HIGH RISK:** If fallback pack is incomplete, HUD may render with missing icons (purple/black checkerboard).
- **Signal it broke:** Players report missing HUD icons after pack download fails.
- **Pre-decided response:** Review fallback pack contents against HUD code's required textures. Add all required icons to fallback even if it grows to 3-4MB (still massive savings vs 26MB).

**MEDIUM RISK:** Pack download on first run adds network dependency. Some servers block outbound HTTP or are airgapped.
- **Signal:** Download fails, fallback extracts, players complain HUD looks "basic" (missing minimap textures).
- **Response:** Expected. Admin must manually download default pack from GitHub releases and place in `packs/default/`. Document clearly in README with direct download link.

**MEDIUM RISK:** `PackManager.buildPack()` might hardcode JAR resource paths (`getResourceAsStream`) that don't translate to filesystem paths.
- **Grep:** Search `PackManager.java` for `getResource`, `getResourceAsStream`, `getClass().getClassLoader()`. Rewrite to use `Files.readAllBytes(externalPath)` and `Files.walk(packDir)` for directory traversal.

**LOW RISK:** SHA-1 hash changes with external packs. Clients may need to re-download.
- **Response:** Expected and acceptable. Pack hash should change when pack changes. Document that custom packs trigger client re-download.

**LOW RISK:** HTTP pack host may have hardcoded `resources/` paths in URL routes.
- **Grep:** Search `ResourcePackHost.java` for `"/resources/"`, `"default/pack"`. Rewrite routes to serve from `pack-cache/<hash>.zip` which is already filesystem-based.
