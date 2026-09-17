# EnorialsGPS

Navigation plugin for Paper `1.19.x`, `1.20.x`, `1.21.x`.
Owner contact: `enorrial@gmail.com`
Owner Discord: `klon_92`

## Config structure
- Main settings are split into logical files in `plugins/EnorialsGPS/configs/`:
  - `core.yml` (language, modes, GUI, favorites)
  - `navigation.yml` (navigation, effects, restrictions)
  - `input.yml` (prompts + chat suggestions)
  - `integrations.yml` (integration toggles + hint profiles)
  - `poi.yml` (points of interest)
  - `betonquest.yml` (BQ bridge + auto routes)
- Each config option includes comments.
- Legacy `config.yml` is kept as fallback compatibility.
- If you still have old split files (`language.yml`, `modes.yml`, etc.), the plugin imports them once into the new compact layout.

## Features
- `/gps` and `/enogps` open custom GUI navigation menu.
- GUI can be globally disabled via config (`gui.enabled: false`).
- Three navigation modes: `ActionBar`, `BossBar`, `Compass`.
- Mode output is exclusive: only one navigation mode is active at a time.
- If a mode is disabled in config, plugin can automatically fallback to ActionBar.
- Chat-driven target input for player name, mob type/custom name, exact coordinates, NPC id/name, and block ids.
- In target menu, shift-clicking `Player` opens an online player head GUI; clicking a head starts tracking that player.
- Clickable quick chat suggestions/autocomplete shown after selecting target type in GUI.
- POI GUI now supports pagination + search.
- Favorites GUI tab for saved destinations.
- Route persistence + resume after relog/restart.
- Active routes are persisted on plugin/server shutdown for safe restart recovery.
- Restriction layer: allowed/denied worlds + blocked cuboids.
- Quest route GUI tab with auto-routes from config conditions.
- Dynamic per-player Quest Routes buttons via `/gpsapi route add/remove/clear` (great for BetonQuest multi-objective flow).
- POI system with conditional visibility (`permission`, `world`, placeholder checks, proximity checks).
- VFX + sounds for start, pulse, and arrival.
- API command for quests/scripts: `/gpsapi`.
- Java plugin API (Bukkit `ServicesManager`) for direct integration without commands.
 - Bukkit events for passive integrations (`GpsNavigationPreStartEvent`, `GpsNavigationPreModeChangeEvent`, `GpsNavigationStartEvent`, `GpsNavigationStopEvent`, `GpsNavigationReachEvent`, `GpsNavigationModeChangeEvent`).
  - Live profiler summary: `/gpsapi perf`
  - Reset profiler counters: `/gpsapi perf reset`
- Legal docs are bundled in jar and exported on first setup:
  - `EULA.md`
  - `DEVELOPER_API_COMPLETE.md`
- Detailed plugin error log file:
  - `plugins/EnorialsGPS/logs/errors.log` (warnings/errors with stack traces when related to EnoGPS)

## Permissions
- Base:
  - `enorials.gps.use` - open `/gps` and `/enogps`
  - `enorials.gps.api` - use `/gpsapi`
- Target tracking permissions (required for players):
  - `enorials.gps.track.player`
  - `enorials.gps.track.mob`
  - `enorials.gps.track.npc`
  - `enorials.gps.track.block`
  - bundle: `enorials.gps.track.all`
- Role bundles:
  - `enorials.gps.role.player` (default true): base `/gps` access
  - `enorials.gps.role.admin`: all tracking + admin/API bundle
  - `enorials.gps.role.op` (default op): OP admin bundle

## Integrations
- BetonQuest 3.0-dev (soft hook via configurable command templates)
  - start/stop/reach/mode-change hooks
  - helper methods to trigger BQ event/objective/tag from GPS actions
  - optional per-hook `extra_event_hooks`, `objective_hooks`, `tag_hooks`, `condition_hooks`
- Citizens
  - target NPC by ID or name
  - POI target to Citizens NPC
- MythicMobs
  - target nearest active mob by mythic internal type
- PlaceholderAPI
  - POI conditions can use placeholder checks
- Block integrations
  - vanilla blocks (`STONE`, `minecraft:diamond_ore`)
  - ItemsAdder custom blocks (`itemsadder:<namespace:id>` / `ia:<namespace:id>`)
  - Nexo custom blocks (`nexo:<block_id>`)
  - Oraxen custom blocks (`oraxen:<block_id>`, best-effort API reflection)
- WorldGuard
  - region center targeting (`wg:<region>`, `wg:<world>:<region>`)
- EssentialsX
  - warp targeting (`warp:<name>`, `essentials:<name>`)
- CMI
  - warp targeting fallback (`cmiwarp:<name>`, `cmi:<name>`)
- Towny
  - town spawn targeting (`town:<name>`, `towny:<name>`)
- DecentHolograms
  - hologram anchor targeting (`dh:<name>`, `holo:<name>`)
- FancyNpcs
  - target FancyNPC by id/name (`fancynpc:<id|name>`, `fancy:<id|name>`)
- Shopkeepers
  - target shopkeepers by id/name/wildcard (`shopkeeper:<query>`)
- Generic entity support (custom mobs from other plugins)
  - target by Bukkit entity type
  - target by custom entity name
  - target by scoreboard tag / metadata / PDC key
- Extra optional presets (configurable hint profiles)
  - EliteMobs (`elite:<query>`)
  - EcoBosses (`ecoboss:<query>` / `ecobosses:<query>`)
  - ModelEngine (`modelengine:<query>` / `model:<query>`)
  - Denizen (`denizen:<query>`)
  - ZNPCsPlus (`znpcs:<id|name>` / `znpcsplus:<id|name>`)
  - MMOCore (`mmocore:<query>`)
  - MMOItems (`mmoitems:<query>`)

## Target syntax
- Players: `Steve`
- NPCs: `1`, `citizens:1`, `citizens:Blacksmith`, `znpcs:15`, `fancynpc:guard_1`, `shopkeeper:*`, `denizen:QuestGiver`
- Mobs: `ZOMBIE`, `mythic:BossSkeleton`, `elite:miniboss`, `ecoboss:worldboss`, `modelengine:guardian`
- Locations: `100 64 -50 world`, `warp:spawn`, `cmi:spawn`, `wg:spawn_region`, `town:SpawnTown`, `dh:spawn_info`
- Blocks: `STONE`, `minecraft:diamond_ore`, `itemsadder:myplugin:jade_ore`, `nexo:ancient_rock`, `oraxen:my_custom_block`
- Generic hooks: `tag:event_boss`, `meta:eventMarker`, `pdc:enorials:event_id=worldboss`

Quick suggestions are configurable in `configs/input.yml` under `chat_suggestions`.

## New GUI controls
- Main menu has `Favorites` and `Quest Routes` buttons.
- Main menu has an `Options` button:
  - `Show Distance` toggle
  - `Show High/Low` (vertical indicator) toggle
  - `Show Target Name` toggle
  - `Compact HUD` toggle
  - `Target Beacon` toggle (personal marker at tracked target)
- Target menu `Player` button:
  - click = type player name in chat
  - shift-click = open online player head picker
- POI list:
  - `Search` button to filter by text
  - `Previous` / `Next` page controls
  - Shift-click POI to toggle favorite
- Favorites list:
  - click to navigate
  - shift-click to remove

## Persistence
- `navigation.persist_routes: true` stores active route in `routes.yml`.
- `navigation.resume_on_join: true` resumes route on player join.
- `navigation.target_resolve_interval_ticks` controls moving-target resolve frequency (performance tuning).

## Restrictions
Use `restrictions` in `configs/navigation.yml`:
- `allowed_worlds` and `denied_worlds`
- `blocked_cuboids` (`world`, `min`, `max`)

If a route target is inside a blocked area, route start/update is denied.

## Performance Tips
- Keep `navigation.update_interval_ticks` at `20` (1s) for low overhead.
- Increase `navigation.target_resolve_interval_ticks` if you expect many concurrent trackers.
- Keep `integrations.block_search.radius` moderate (large values make block-target lookup expensive).
- Use `navigation.profiling.*` and `/gpsapi perf` for live timing diagnostics.

## BetonQuest auto-routes
Use `betonquest.auto_routes` entries:
- `display`
- `type` (`PLAYER|MOB|NPC|POI|LOCATION`)
- `value`
- `mode`
- `source`
- `conditions` (same condition syntax as POIs)

## BetonQuest examples
Use BetonQuest events to run API commands:

```yaml
events:
  gps_to_spawn: "command gpsapi start {player} poi spawn"
  gps_to_blacksmith: "command gpsapi start {player} npc 1"
  gps_stop: "command gpsapi stop {player}"
```

Or let GPS auto-fire events back via `configs/betonquest.yml`:
- `gps_start:<source>:<targetId>`
- `gps_stop:<reason>:<targetId>`
- `gps_reach:<targetId>`
- `gps_mode_change:<mode>`

Full BetonQuest 3.0-dev snippet pack:
- `docs/BETONQUEST_3_DEV_SNIPPETS.md`
Full EnorialsGPS + BetonQuest usage manual:
- `docs/BETONQUEST_ENOGPS_MANUAL.md`

Staff/admin `/gpsapi` command guide:
- `docs/STAFF_ADMIN_GPSAPI.md`
Plugin developer API guide:
- `docs/PLUGIN_DEVELOPER_API.md`
Complete developer API reference:
- `docs/DEVELOPER_API_COMPLETE.md`

Legal/release checklist:
- `docs/LEGAL_RELEASE_CHECKLIST.md`

Legal notices:
- `LICENSE`
- `EULA.md`
- `THIRD_PARTY_NOTICES.md`

## Build
```bash
./gradlew build
```
On Windows PowerShell (without wrapper):
```powershell
..\\gradle-8.7\\gradle-8.7\\bin\\gradle.bat build
```
