# BetonQuest 3.0-dev Snippets for EnorialsGPS

Owner contact: `enorrial@gmail.com`
Owner Discord: `klon_92`

These snippets are ready to adapt into your BetonQuest package files.

## 1) Events (`events.yml`)

```yaml
events:
  gps_spawn: "command gpsapi start {player} poi spawn"
  gps_blacksmith: "command gpsapi start {player} npc citizens:1"
  gps_town_spawn: "command gpsapi start {player} town SpawnTown"
  gps_holo_board: "command gpsapi start {player} hologram spawn_info"
  gps_worldboss: "command gpsapi start {player} mob tag:event_boss"
  gps_mythic_boss: "command gpsapi start {player} mob mythic:BossSkeleton"
  gps_elite_boss: "command gpsapi start {player} mob elite:miniboss"
  gps_stop: "command gpsapi stop {player}"
  gps_mode_bossbar: "command gpsapi mode {player} bossbar"
  gps_mode_compass: "command gpsapi mode {player} compass"
```

## 2) Objectives (`objectives.yml`)

```yaml
objectives:
  talk_blacksmith: "npcinteract 1"
  kill_worldboss: "mobkill 1 ZOMBIE"
```

## 3) Conditions (`conditions.yml`)

```yaml
conditions:
  gps_enabled: "tag gps_enabled"
  at_spawn_area: "location 0;64;0;world;8"
```

## 4) Conversations (`conversations.yml`)

```yaml
conversations:
  blacksmith_intro:
    first: start
    NPC_options:
      start:
        text: "Need help finding the forge?"
        pointer: p1,p2,p3
    player_options:
      p1:
        text: "Yes, guide me."
        event: gps_blacksmith
      p2:
        text: "Guide me to spawn first."
        event: gps_spawn
      p3:
        text: "Stop current guidance."
        event: gps_stop
```

## 5) Journal / progression pattern

Use GPS-related hooks fired by this plugin:

- `gps_start:<source>:<target>`
- `gps_stop:<reason>:<target>`
- `gps_reach:<target>`
- `gps_mode_change:<mode>`

In practice, you can make BetonQuest events listening for those identifiers and:
- add tags (`bq tag add`)
- add objectives (`bq objective add`)
- update quest branches based on route completion.

## 5.1) Built-in objective/tag/condition hook dispatch (new)

In `plugins/EnorialsGPS/configs/betonquest.yml` you can map every GPS hook directly to:
- extra event payloads
- objective values
- tag values
- condition values

Example:

```yaml
betonquest:
  command_templates:
    condition: "bq condition {player} {value}"
  objective_hooks:
    start:
      - "gps_nav_started"
    reach:
      - "gps_reached_{target}"
  tag_hooks:
    start:
      - "gps_active"
    stop:
      - "gps_inactive"
    mode_change:
      - "gps_mode_{mode}"
  extra_event_hooks:
    reach:
      - "gps_chain_next:{target}"
  condition_hooks:
    reach:
      - "is_at_{target}"
```

Supported placeholders in hook values:
- `{hook}` (`start|stop|reach|mode_change`)
- `{source}` (start only)
- `{reason}` (stop only)
- `{target}`
- `{mode}` (mode change only)
- `{payload}` (full default payload string)

## 6) Conditional POI visibility

In `config.yml` of EnorialsGPS, each POI supports conditions:

```yaml
conditions:
  - "permission:enorials.gps.poi.spawn"
  - "world:world"
  - "near:world,0,64,0,120"
  - "placeholder_true:%some_placeholder%"
  - "placeholder_equals:%player_world%==world"
```

This pairs nicely with BetonQuest tags/placeholder expansions to show POIs only when relevant in the quest phase.

## 7) Strong pattern for staged navigation

Stage chain:
1. Set mode (bossbar/compass).
2. Start GPS via `/gpsapi start ...`.
3. Wait for `gps_reach:*`.
4. Add next objective/event.
5. Optionally stop current GPS.

This gives you proximity-like guided quest flow without hard teleporting or manual coords in chat.

## 8) Dynamic Quest Routes buttons (new)

You can add/remove per-player buttons in the Quest Routes GUI from BetonQuest actions.

```yaml
events:
  # Add button into Quest Routes GUI.
  gps_route_blacksmith_add: "command gpsapi route add {player} blacksmith_step Find_Blacksmith NPC bossbar citizens:1"
  # Short form (auto routeId/display, player's current/default mode).
  gps_route_spawn_add: "command gpsapi route add {player} poi spawn"

  # Optional: remove this button later.
  gps_route_blacksmith_remove: "command gpsapi route remove {player} blacksmith_step"

  # Optional: clear all dynamic route buttons for player.
  gps_route_clear_all: "command gpsapi route clear {player}"
```

Notes:
- `display_with_underscores` is shown in GUI (`_` becomes space).
- `routeId` is your unique key for later remove/update.
- This is ideal when player has multiple active quest objectives and should choose what to track.
- If you use short form (`route add {player} <type> <value...>`), routeId/display/mode are auto-generated.
