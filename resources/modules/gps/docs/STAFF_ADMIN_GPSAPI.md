# Staff/Admin GPSAPI Guide

Owner contact: `enorrial@gmail.com`
Owner Discord: `klon_92`

Use this when staff, quest designers, or scripts need to control GPS directly.

## Permissions

- API/staff:
  - `enorials.gps.api` (run `/gpsapi`)
  - `enorials.gps.admin` (admin bundle)
- Player base:
  - `enorials.gps.use` (use `/gps` and `/enogps`)
- Player target tracking permissions:
  - `enorials.gps.track.player`
  - `enorials.gps.track.mob`
  - `enorials.gps.track.npc`
  - `enorials.gps.track.block`
  - bundle: `enorials.gps.track.all`
- Role bundles:
  - `enorials.gps.role.player`
  - `enorials.gps.role.admin`
  - `enorials.gps.role.op`

## Base Syntax

```text
/gpsapi start <player> <type> <value...>
/gpsapi stop <player>
/gpsapi mode <player> <actionbar|bossbar|compass>
/gpsapi perf
/gpsapi perf reset
/gpsapi route add <player> <routeId> <display_with_underscores> <PLAYER|MOB|NPC|POI|LOCATION> <mode> <value...>
/gpsapi route add <player> <PLAYER|MOB|NPC|POI|LOCATION> [mode] <value...>
/gpsapi route add <player> <value...>  # type/mode/id/display auto
/gpsapi route remove <player> <routeId>
/gpsapi route clear <player>
/gpsapi route list <player>
```

## Start Types + Examples

```text
# Player
/gpsapi start Steve player Alex

# Mob
/gpsapi start Steve mob ZOMBIE
/gpsapi start Steve mob mythic:BossSkeleton
/gpsapi start Steve mob elite:miniboss

# NPC
/gpsapi start Steve npc citizens:1
/gpsapi start Steve npc fancynpc:guard_1
/gpsapi start Steve npc shopkeeper:*

# POI
/gpsapi start Steve poi spawn

# Location
/gpsapi start Steve loc 120 64 -50 world
/gpsapi start Steve loc warp:spawn
/gpsapi start Steve loc cmi:spawn
/gpsapi start Steve loc wg:spawn_region
/gpsapi start Steve loc town:SpawnTown
/gpsapi start Steve loc dh:spawn_info

# Dedicated aliases
/gpsapi start Steve warp spawn
/gpsapi start Steve region spawn_region world
/gpsapi start Steve town SpawnTown
/gpsapi start Steve hologram spawn_info

# Block targets
/gpsapi start Steve block minecraft:diamond_ore
/gpsapi start Steve block itemsadder:myplugin:jade_ore
/gpsapi start Steve block nexo:ancient_rock
/gpsapi start Steve block oraxen:my_custom_block

# Live profiler
/gpsapi perf
/gpsapi perf reset

# Dynamic Quest GPS Routes (buttons in Quest Routes GUI)
/gpsapi route add Steve main_quest Find_Blacksmith NPC bossbar citizens:1
/gpsapi route add Steve POI spawn
/gpsapi route add Steve NPC bossbar citizens:1
/gpsapi route add Steve warp:spawn
/gpsapi route add Steve ore_step Find_Diamond_Ore LOCATION actionbar 100 12 -30 world
/gpsapi route add Steve town_step Go_To_SpawnTown POI bossbar spawn
/gpsapi route list Steve
/gpsapi route remove Steve ore_step
/gpsapi route clear Steve
```

## BetonQuest Event Examples

```yaml
events:
  gps_mode_compass: "command gpsapi mode {player} compass"
  gps_to_spawn: "command gpsapi start {player} poi spawn"
  gps_to_town: "command gpsapi start {player} town SpawnTown"
  gps_to_holo: "command gpsapi start {player} hologram spawn_info"
  gps_stop_now: "command gpsapi stop {player}"
```

## GPS -> BetonQuest Extra Methods

Besides default event payload hooks, EnorialsGPS can also dispatch:
- objective values (`command_templates.objective`)
- tag values (`command_templates.tag`)
- condition checks (`command_templates.condition`)
- extra event payloads (`command_templates.event`)

Configure in `plugins/EnorialsGPS/configs/betonquest.yml`:

```yaml
betonquest:
  command_templates:
    condition: "bq condition {player} {value}"
  objective_hooks:
    start: [ "gps_nav_started" ]
    reach: [ "gps_reached_{target}" ]
  tag_hooks:
    start: [ "gps_active" ]
    stop: [ "gps_inactive" ]
    mode_change: [ "gps_mode_{mode}" ]
  extra_event_hooks:
    reach: [ "gps_chain_next:{target}" ]
  condition_hooks:
    reach: [ "is_at_{target}" ]
```

Hook placeholders:
`{hook}`, `{source}`, `{reason}`, `{target}`, `{mode}`, `{payload}`

## Script/Automation Flow Pattern

1. Set mode (`/gpsapi mode ...`)
2. Start route (`/gpsapi start ...`)
3. Wait for GPS reach hook (`gps_reach:*`) in BetonQuest bridge
4. Trigger next quest/script step
5. Stop route when needed (`/gpsapi stop ...`)

## BetonQuest Event Hook Pattern (recommended)

Use BetonQuest events to control Quest Routes buttons explicitly:
- quest accept/start -> `gpsapi route add ...`
- quest complete/cancel -> `gpsapi route remove ...`

This keeps behavior predictable and quest-script driven.

## Ready-to-Post Staff Message (Discord)

```text
🛠️ Staff GPS controls are enabled!

Use `/gpsapi` for quests/events/scripts:
- `/gpsapi start <player> <type> <value...>`
- `/gpsapi mode <player> <actionbar|bossbar|compass>`
- `/gpsapi stop <player>`

Supported start types:
`player, mob, npc, poi, loc, warp, region, town, hologram, block`

Examples:
- `/gpsapi start Steve poi spawn`
- `/gpsapi start Steve npc citizens:1`
- `/gpsapi start Steve town SpawnTown`
- `/gpsapi start Steve block itemsadder:myplugin:jade_ore`
```
