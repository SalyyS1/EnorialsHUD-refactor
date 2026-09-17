# EnorialsGPS + BetonQuest Manual (Complete)

Owner contact: `enorrial@gmail.com`
Owner Discord: `klon_92`

This is the complete reference for EnorialsGPS <-> BetonQuest integration:
- all hooks
- all target syntaxes
- all supported integration types
- copy-paste templates

Use this as the single source for quest builders and admins.

## 1) How integration works (important)

There are 2 directions:

1. BetonQuest -> EnorialsGPS  
   BetonQuest runs commands like:
   - `/gpsapi start {player} ...`
   - `/gpsapi mode {player} ...`
   - `/gpsapi stop {player}`

2. EnorialsGPS -> BetonQuest  
   EnorialsGPS fires hook payloads through console command templates from:
   - `plugins/EnorialsGPS/configs/betonquest.yml`

So you can start routes from quests and also react to GPS progress automatically.

## 2) Requirements

- Paper 1.19.x / 1.20.x / 1.21.x
- EnorialsGPS
- BetonQuest 3.0-dev
- Optional integrations: Citizens, MythicMobs, ItemsAdder, Nexo, Oraxen, Essentials, CMI, WorldGuard, Towny, DecentHolograms, FancyNpcs, Shopkeepers, ZNPCsPlus, EliteMobs, ModelEngine, Denizen, MMOCore, MMOItems, EcoBosses, PlaceholderAPI

## 3) Permissions (player/admin/op)

Player/base:
- `enorials.gps.use` (use `/gps` and `/enogps`)

Player tracking permissions (for GUI/chat target picking):
- `enorials.gps.track.player`
- `enorials.gps.track.mob`
- `enorials.gps.track.npc`
- `enorials.gps.track.block`
- bundle: `enorials.gps.track.all`

Automation/admin:
- `enorials.gps.api` (use `/gpsapi`)
- `enorials.gps.admin` (admin bundle)

Role bundles:
- `enorials.gps.role.player`
- `enorials.gps.role.admin`
- `enorials.gps.role.op`

Note:
- If BetonQuest executes command as console, permissions are usually not required.
- If a player executes `/gpsapi`, that sender needs `enorials.gps.api`.

## 4) Command reference

```text
/gps
/enogps
/gpsapi start <player> <type> <value...>
/gpsapi mode <player> <actionbar|bossbar|compass>
/gpsapi stop <player>
/gpsapi route add <player> <routeId> <display_with_underscores> <PLAYER|MOB|NPC|POI|LOCATION> <mode> <value...>
/gpsapi route add <player> <PLAYER|MOB|NPC|POI|LOCATION> [mode] <value...>
/gpsapi route add <player> <value...>   # auto type/mode/id/display
/gpsapi route remove <player> <routeId>
/gpsapi route clear <player>
/gpsapi route list <player>
```

Supported `/gpsapi start` types:
- `player`
- `mob`
- `npc`
- `poi`
- `loc`
- `warp`
- `region`
- `town`
- `hologram`
- `block`

## 5) Full target syntax (all integrations)

### 5.1 Player targets

```text
player Alex
```

### 5.2 Mob targets

Vanilla / direct:
```text
mob ZOMBIE
```

MythicMobs:
```text
mob mythic:BossSkeleton
```

Hint-based custom mob families:
```text
mob elite:miniboss
mob elitemobs:miniboss
mob modelengine:guardian
mob model:guardian
mob denizen:QuestMob
mob mmocore:my_mob
mob mmoitems:my_mob
mob ecoboss:worldboss
mob ecobosses:worldboss
```

Generic entity lookup helpers:
```text
mob tag:event_boss
mob meta:eventMarker
mob pdc:enorials:event_id=worldboss
```

### 5.3 NPC targets

Citizens:
```text
npc citizens:1
npc citizens:Blacksmith
npc 1
```

Denizen on Citizens:
```text
npc denizen:QuestGiver
```

ZNPCsPlus:
```text
npc znpcs:15
npc znpcsplus:guard_1
```

FancyNpcs:
```text
npc fancynpc:guard_1
npc fancy:guard_1
```

Shopkeepers:
```text
npc shopkeeper:*
npc shopkeeper:WeaponSeller
npc shopkeepers:42
```

### 5.4 Location aliases

Coordinates:
```text
loc 120 64 -50 world
```

Warps:
```text
loc warp:spawn
loc esswarp:spawn
loc ess:spawn
loc essentials:spawn
```

CMI warps:
```text
loc cmiwarp:spawn
loc cmi:spawn
```

WorldGuard:
```text
loc wg:spawn_region
loc wg:world:spawn_region
loc worldguard:spawn_region
loc region:spawn_region
```

Towny:
```text
loc town:SpawnTown
loc towny:SpawnTown
```

DecentHolograms:
```text
loc dh:spawn_info
loc decentholograms:spawn_info
loc holo:spawn_info
```

Dedicated aliases:
```text
warp spawn
region spawn_region world
town SpawnTown
hologram spawn_info
```

### 5.5 Block targets

Vanilla:
```text
block STONE
block minecraft:diamond_ore
block vanilla:diamond_ore
block block:diamond_ore
```

Custom block integrations:
```text
block itemsadder:myplugin:jade_ore
block ia:myplugin:jade_ore
block nexo:ancient_rock
block oraxen:my_custom_block
```

### 5.6 POI targets

```text
poi <poi_key>
```

POI keys come from `plugins/EnorialsGPS/configs/poi.yml` (`poi.points`).

## 6) Hook system reference (EnorialsGPS -> BetonQuest)

Configured in:
- `plugins/EnorialsGPS/configs/betonquest.yml`

### 6.1 Default hook payloads

- Start: `gps_start:<source>:<targetId>`
- Stop: `gps_stop:<reason>:<targetId>`
- Reach: `gps_reach:<targetId>`
- Mode change: `gps_mode_change:<mode>`

Hook names are configurable in:
- `betonquest.hooks.start_event`
- `betonquest.hooks.stop_event`
- `betonquest.hooks.reach_event`
- `betonquest.hooks.mode_change_event`

### 6.2 What `source` can be

Common `source` values:
- `chat-player`, `chat-mob`, `chat-npc`, `chat-loc`, `chat-block`
- `api-player`, `api-mob`, `api-npc`, `api-poi`, `api-loc`, `api-warp`, `api-region`, `api-town`, `api-hologram`, `api-block`
- `poi`, `favorite`, `gui-player-list`
- `resume`
- auto-route value from config (`betonquest.auto_routes.<key>.source`, default often `bq-auto`)

### 6.3 What `reason` can be

`reason` comes from explicit stop call. Common values:
- `manual` (GUI stop button or `/gps stop`)
- `api` (`/gpsapi stop`)

### 6.4 `targetId` format examples

These come from target locator IDs:
- Player: `player:<uuid>`
- Static location: `loc:<world>:<x>:<y>:<z>`
- Citizens: `citizens:<id_or_name>`
- FancyNpcs: `fancynpc:<id_or_name>`
- ZNPCsPlus: `znpcs:<id_or_name>`
- Shopkeeper: `shopkeeper:<query>`
- Mythic: `mythic:<internal_name>`
- Vanilla type: `mob-type:<entity_type>`
- Named entity: `mob-name:<custom_name>`
- Hint-based entities: `elitemobs:<query>`, `modelengine:<query>`, `ecobosses:<query>`, `tag:<tag>`, `meta:<key>`, `pdc:<key_or_key=value>`
- POI generic entity keys: `poi-tag:<tag>`, `poi-meta:<key>`, `poi-pdc:<key_or_key=value>`

### 6.5 Important behavior notes

- `gps_stop` is fired when stop is explicit (`stopNavigation` paths), not on every implicit session removal.
- Starting a new route replaces the old route directly.
- Hook command dispatch runs as console.

## 7) Command templates and placeholders

In `configs/betonquest.yml`:

```yaml
betonquest:
  command_templates:
    event: "bq action {player} {value}"
    objective: "bq objective add {player} {value}"
    tag: "bq tag add {player} {value}"
    condition: "bq condition {player} {value}"
    condition: "bq condition {player} {value}"
```

Template placeholders:
- `{player}`
- `{uuid}`
- `{value}` (payload/value sent by EnorialsGPS)
- `{hook}`, `{source}`, `{reason}`, `{target}`, `{mode}`, `{payload}`

Hook value placeholders (inside `extra_event_hooks`, `objective_hooks`, `tag_hooks`, `condition_hooks` values):
- `{hook}`, `{source}`, `{reason}`, `{target}`, `{mode}`, `{payload}`

Example:
```yaml
betonquest:
  extra_event_hooks:
    reach:
      - "gps_chain_next:{target}"
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
  condition_hooks:
    reach:
      - "is_at_{target}"
```

Note:
- Values inserted into commands are sanitized (`space -> _`), so avoid relying on spaces in payload identifiers.
- `condition` hooks run a condition check command; use action/tag/objective hooks for actual quest state changes.

## 8) Complete `betonquest.yml` template (copy-paste)

File: `plugins/EnorialsGPS/configs/betonquest.yml`

```yaml
betonquest:
  enabled: true

  command_templates:
    event: "bq action {player} {value}"
    objective: "bq objective add {player} {value}"
    tag: "bq tag add {player} {value}"

  hooks:
    start_event: "gps_start"
    stop_event: "gps_stop"
    reach_event: "gps_reach"
    mode_change_event: "gps_mode_change"

  extra_event_hooks:
    start:
      - "gps_started:{source}:{target}"
    stop:
      - "gps_stopped:{reason}:{target}"
    reach:
      - "gps_reached:{target}"
      - "gps_chain_next:{target}"
    mode_change:
      - "gps_mode_switched:{mode}"

  objective_hooks:
    start:
      - "gps_nav_started"
    stop: []
    reach:
      - "gps_reach_{target}"
    mode_change: []

  tag_hooks:
    start:
      - "gps_active"
    stop:
      - "gps_inactive"
    reach: []
    mode_change:
      - "gps_mode_{mode}"

  condition_hooks:
    start: []
    stop: []
    reach:
      - "is_at_{target}"
    mode_change: []

  auto_routes:
    quest_spawn:
      display: "&aQuest: Go To Spawn"
      type: "POI"           # PLAYER | MOB | NPC | POI | LOCATION
      value: "spawn"
      mode: "bossbar"
      source: "bq-auto"
      conditions:
        - "world:world"

    quest_blacksmith:
      display: "&6Quest: Find Blacksmith"
      type: "NPC"
      value: "citizens:1"
      mode: "actionbar"
      source: "bq-auto"
      conditions:
        - "permission:enorials.gps.use"
```

## 9) BetonQuest package templates

Depending on your BetonQuest package style, use `actions.yml` or `events.yml`.
The command strings are the same.

### 9.1 Quest actions/events template

```yaml
events:
  gps_mode_bossbar: "command gpsapi mode {player} bossbar"
  gps_mode_compass: "command gpsapi mode {player} compass"

  gps_to_spawn_poi: "command gpsapi start {player} poi spawn"
  gps_to_blacksmith_npc: "command gpsapi start {player} npc citizens:1"
  gps_to_fancy_guard: "command gpsapi start {player} npc fancynpc:guard_1"
  gps_to_worldboss_mob: "command gpsapi start {player} mob mythic:BossSkeleton"
  gps_to_tagged_mob: "command gpsapi start {player} mob tag:event_boss"
  gps_to_region: "command gpsapi start {player} region spawn_region world"
  gps_to_town: "command gpsapi start {player} town SpawnTown"
  gps_to_hologram: "command gpsapi start {player} hologram spawn_info"
  gps_to_block: "command gpsapi start {player} block itemsadder:myplugin:jade_ore"
  gps_to_coords: "command gpsapi start {player} loc 100 64 -40 world"

  gps_stop: "command gpsapi stop {player}"

  # Dynamic Quest Routes button integration:
  # Adds a clickable quest button into player's Quest Routes GUI.
  gps_route_blacksmith_add: "command gpsapi route add {player} blacksmith_step Find_Blacksmith NPC bossbar citizens:1"
  # Removes this specific dynamic button.
  gps_route_blacksmith_remove: "command gpsapi route remove {player} blacksmith_step"
  # Clears all dynamic quest buttons for this player.
  gps_route_clear_all: "command gpsapi route clear {player}"
```

### 9.2 Conversation template

```yaml
conversations:
  navigator:
    first: start
    NPC_options:
      start:
        text: "Need directions?"
        pointer: p1,p2,p3,p4
    player_options:
      p1:
        text: "Guide me to blacksmith."
        event: gps_mode_bossbar,gps_to_blacksmith_npc
      p2:
        text: "Guide me to spawn."
        event: gps_to_spawn_poi
      p3:
        text: "Guide me to world boss."
        event: gps_mode_compass,gps_to_worldboss_mob
      p4:
        text: "Stop navigation."
        event: gps_stop
```

### 9.3 Conditions/objectives template

```yaml
conditions:
  can_use_gps: "tag gps_enabled"
  at_spawn_area: "location 0;64;0;world;8"

objectives:
  talk_blacksmith: "npcinteract 1"
  kill_boss: "mobkill 1 ZOMBIE"
```

## 10) POI integration schema (all supported keys)

File: `plugins/EnorialsGPS/configs/poi.yml` under `poi.points.<key>`.

Each POI can have:
- `display_name`
- `conditions` (optional)
- one target definition key from the list below

### 10.1 Full POI template block

```yaml
poi:
  points:
    # 1) Static location
    spawn:
      display_name: "&aSpawn"
      world: "world"
      x: 0
      y: 64
      z: 0
      conditions:
        - "permission:enorials.gps.poi.spawn"

    # 2) Citizens
    blacksmith_by_id:
      display_name: "&6Blacksmith"
      citizens_id: 1

    blacksmith_by_name:
      display_name: "&6Blacksmith"
      citizens_name: "Blacksmith"

    # 3) FancyNpcs
    fancy_guard:
      display_name: "&bFancy Guard"
      fancynpc: "guard_1"

    # 4) Shopkeepers
    market_shopkeeper:
      display_name: "&eShopkeeper"
      shopkeeper: "*"

    # 5) MythicMobs
    mythic_boss:
      display_name: "&cMythic Boss"
      mythic_mob: "BossSkeleton"

    # 6) EliteMobs (hint profile)
    elite_miniboss:
      display_name: "&4Elite Miniboss"
      elite_mob: "miniboss"

    # 7) ModelEngine (hint profile)
    model_guardian:
      display_name: "&dModel Guardian"
      modelengine_model: "guardian"

    # 8) Denizen NPC (via Citizens)
    denizen_npc_target:
      display_name: "&9Quest NPC"
      denizen_npc: "QuestGiver"

    # 9) ZNPCsPlus
    znpc_by_id:
      display_name: "&aZNPC by id"
      znpcs_id: "15"

    znpc_by_name:
      display_name: "&aZNPC by name"
      znpcs_name: "Guard"

    # 10) EcoBosses (hint profile)
    ecoboss_target:
      display_name: "&cEcoBoss"
      ecoboss_mob: "worldboss"

    # 11) Essentials warp
    ess_spawn:
      display_name: "&aEssentials Spawn"
      essentials_warp: "spawn"

    # 12) CMI warp
    cmi_market:
      display_name: "&aCMI Market"
      cmi_warp: "market"

    # 13) WorldGuard region
    wg_spawn_region:
      display_name: "&dSpawn Region"
      worldguard_region: "spawn_region"
      worldguard_world: "world"

    # 14) Towny
    towny_spawn:
      display_name: "&6Town Spawn"
      towny_town: "SpawnTown"

    # 15) DecentHolograms
    holo_spawn:
      display_name: "&eSpawn Hologram"
      decent_hologram: "spawn_info"

    # 16) Block targets
    vanilla_diamond_ore:
      display_name: "&bNearest Diamond Ore"
      vanilla_block: "DIAMOND_ORE"

    ia_jade_ore:
      display_name: "&2ItemsAdder Jade Ore"
      itemsadder_block: "myplugin:jade_ore"

    ia_alias_example:
      display_name: "&2ItemsAdder alias"
      ia_block: "myplugin:jade_ore"

    nexo_ancient_rock:
      display_name: "&3Nexo Ancient Rock"
      nexo_block: "ancient_rock"

    oraxen_custom_block:
      display_name: "&5Oraxen Custom Block"
      oraxen_block: "my_custom_block"

    # 17) Generic entity selectors
    tagged_entity:
      display_name: "&eTagged Entity"
      entity_tag: "event_boss"

    metadata_entity:
      display_name: "&bMetadata Entity"
      entity_metadata: "eventMarker"

    pdc_entity:
      display_name: "&aPDC Entity"
      entity_pdc: "enorials:event_id=worldboss"

    typed_entity:
      display_name: "&fNearest Zombie"
      entity_type: "ZOMBIE"

    named_entity:
      display_name: "&fNamed Entity"
      entity_name: "Raid Captain"
```

## 11) POI/auto-route condition language

Supported condition formats:

- `permission:<node>`
- `world:<worldName>`
- `near:<world>,<x>,<y>,<z>,<radius>`
- `placeholder_true:<placeholder>`
- `placeholder_equals:<placeholder>==<value>`

Examples:

```yaml
conditions:
  - "permission:enorials.gps.use"
  - "world:world"
  - "near:world,0,64,0,120"
  - "placeholder_true:%quests_can_track%"
  - "placeholder_equals:%player_world%==world"
```

## 12) Integration matrix

| Integration | `/gpsapi` syntax example | POI key example |
|---|---|---|
| Citizens | `npc citizens:1` | `citizens_id` / `citizens_name` |
| MythicMobs | `mob mythic:BossSkeleton` | `mythic_mob` |
| EliteMobs | `mob elite:miniboss` | `elite_mob` |
| ModelEngine | `mob modelengine:guardian` | `modelengine_model` |
| Denizen | `npc denizen:QuestGiver` / `mob denizen:QuestMob` | `denizen_npc` |
| ZNPCsPlus | `npc znpcs:15` | `znpcs_id` / `znpcs_name` |
| FancyNpcs | `npc fancynpc:guard_1` | `fancynpc` |
| Shopkeepers | `npc shopkeeper:*` | `shopkeeper` |
| Essentials | `loc warp:spawn` / `warp spawn` | `essentials_warp` |
| CMI | `loc cmi:spawn` | `cmi_warp` |
| WorldGuard | `loc wg:spawn_region` / `region spawn_region world` | `worldguard_region` |
| Towny | `loc town:SpawnTown` / `town SpawnTown` | `towny_town` |
| DecentHolograms | `loc dh:spawn_info` / `hologram spawn_info` | `decent_hologram` |
| ItemsAdder | `block itemsadder:ns:id` | `itemsadder_block` / `ia_block` |
| Nexo | `block nexo:block_id` | `nexo_block` |
| Oraxen | `block oraxen:block_id` | `oraxen_block` |
| PlaceholderAPI | condition placeholders | `conditions` |
| Generic tags/meta/pdc | `mob tag:x`, `mob meta:x`, `mob pdc:key=value` | `entity_tag` / `entity_metadata` / `entity_pdc` |

## 13) Recommended staged quest pattern

1. Set mode:
   - `gpsapi mode {player} bossbar`
2. Start route:
   - `gpsapi start {player} ...`
3. Wait for reach hook payload (`gps_reach:*`) through bridge.
4. Trigger next quest action/objective/tag.
5. Optional explicit stop:
   - `gpsapi stop {player}`

## 14) Debug checklist

1. Verify plugin state:
   - `/bq version`
   - `/plugins`
2. Verify GPS API manually:
   - `gpsapi start <player> poi spawn`
3. Debug outgoing payloads by changing template:
   - `betonquest.command_templates.event: "say [GPS-HOOK] {player} {value}"`
4. Trigger start/stop/reach/mode and watch console/chat output.
5. Restore template to production command (for example `bq action {player} {value}`).
6. Check integration toggles in `configs/integrations.yml`.
7. Check POI visibility conditions and target availability in `configs/poi.yml`.

## 15) Quick links

- `docs/BETONQUEST_3_DEV_SNIPPETS.md`
- `docs/STAFF_ADMIN_GPSAPI.md`
- `README.md`

## 16) Beginner-friendly commented templates (actions, objectives, conditions, events, parties)

This section is intentionally verbose so even new admins can copy-paste it safely.

Important:
- BetonQuest 3.0-dev renamed `events` -> `actions` in scripting.
- Prefer `actions:` in new packages.
- Keep `events:` only for legacy packs that were not migrated yet.

### 16.1 Example package folder layout

```text
plugins/BetonQuest/QuestPackages/my_gps_quest/
- package.yml
- actions.yml
- conditions.yml
- objectives.yml
- conversations.yml
- schedules.yml   # optional
```

### 16.2 `package.yml` (commented)

```yaml
package:
  # Optional templates you want to inherit from.
  # Remove this block if you don't use templates.
  templates:
    - common_rewards

  # Optional package options can be added here depending on your setup.
  # Keep minimal for first setup.
```

### 16.3 `actions.yml` (commented, GPS + party ready)

```yaml
actions:
  # ---------------------------
  # GPS MODE CONTROL
  # ---------------------------

  # Force bossbar mode for better quest visibility.
  gps_mode_bossbar: "command gpsapi mode {player} bossbar"

  # Force compass mode (useful for long travel quests).
  gps_mode_compass: "command gpsapi mode {player} compass"

  # ---------------------------
  # GPS START TARGETS
  # ---------------------------

  # Start route to a POI key from EnorialsGPS poi.yml (poi.points.spawn).
  gps_to_spawn_poi: "command gpsapi start {player} poi spawn"

  # Start route to Citizens NPC id 1.
  gps_to_blacksmith_npc: "command gpsapi start {player} npc citizens:1"

  # Start route to a MythicMobs boss by internal name.
  gps_to_mythic_boss: "command gpsapi start {player} mob mythic:BossSkeleton"

  # Start route to a WorldGuard region center.
  gps_to_region: "command gpsapi start {player} region spawn_region world"

  # Start route to nearest custom block (ItemsAdder).
  gps_to_custom_block: "command gpsapi start {player} block itemsadder:myplugin:jade_ore"

  # ---------------------------
  # GPS STOP
  # ---------------------------

  # Explicitly stop player's active navigation.
  gps_stop: "command gpsapi stop {player}"

  # ---------------------------
  # PARTY ACTION EXAMPLES
  # ---------------------------
  # Syntax (BQ 3.0-dev): party <range> <conditions> <actions> [amount]
  # - range: radius around triggering player
  # - conditions: party member filters (comma-separated condition names)
  # - actions: actions to execute for selected party members
  # - amount: optional max selected players (closest first)

  # Give GPS route to all nearby party members who match condition has_gps_quest.
  gps_party_sync_spawn: "party 40 has_gps_quest gps_to_spawn_poi"

  # Same as above, but limit to 3 closest party members.
  gps_party_sync_spawn_limited: "party 40 has_gps_quest gps_to_spawn_poi amount:3"
```

### 16.4 `conditions.yml` (commented, incl. party condition)

```yaml
conditions:
  # Player has the quest state tag.
  has_gps_quest: "tag gps_quest_active"

  # Player must be in world 'world'.
  is_in_main_world: "world world"

  # Player is close to spawn area (x;y;z;world radius).
  near_spawn: "location 0;64;0;world 20"

  # Party condition example:
  # Syntax: party <radius> <baseConditions> [location] [every] [any] [count]
  # This returns true when party around player has at least 2 members
  # and every party member has has_gps_quest.
  party_ready_for_sync: "party 40 has_gps_quest every:has_gps_quest count:2"
```

### 16.5 `objectives.yml` (commented)

```yaml
objectives:
  # Talk to Citizens NPC id 1.
  # On completion, run actions to set mode and start GPS route.
  talk_blacksmith: "npcinteract 1 actions:gps_mode_bossbar,gps_to_blacksmith_npc"

  # Kill 1 zombie, then route player to spawn POI.
  kill_test_mob: "mobkill 1 ZOMBIE actions:gps_to_spawn_poi"

  # Reach location objective; when completed, stop GPS.
  reach_spawn_area: "location 0;64;0;world 8 actions:gps_stop"
```

### 16.6 `conversations.yml` (commented)

```yaml
conversations:
  navigator_npc:
    # First NPC option node shown.
    first: start

    NPC_options:
      start:
        # NPC text shown to player.
        text: "Need directions?"
        # Show these player options.
        pointer: p_spawn,p_blacksmith,p_stop

    player_options:
      p_spawn:
        text: "Guide me to spawn."
        # Run both actions in order.
        actions: gps_mode_compass,gps_to_spawn_poi

      p_blacksmith:
        text: "Guide me to blacksmith."
        actions: gps_mode_bossbar,gps_to_blacksmith_npc

      p_stop:
        text: "Stop current navigation."
        actions: gps_stop
```

### 16.7 `schedules.yml` (optional, commented)

```yaml
schedules:
  gps_reminder_tick:
    # Run every 5 minutes (ticks).
    interval: 6000
    # Only run for players matching where-conditions.
    conditions: has_gps_quest
    # Action to run.
    actions: gps_to_spawn_poi
```

### 16.8 Legacy `events.yml` compatibility (only if your pack still uses events)

If your existing package is older and still uses `events:`, use this pattern:

```yaml
events:
  # Legacy equivalent of actions examples above.
  gps_mode_bossbar: "command gpsapi mode {player} bossbar"
  gps_to_spawn_poi: "command gpsapi start {player} poi spawn"
  gps_stop: "command gpsapi stop {player}"
```

Do not mix old `events` and new `actions` blindly across the same package logic.
For new setups on BQ 3.0-dev, prefer `actions`.

### 16.9 Minimal end-to-end test flow (commented checklist)

1. Put the files above in one quest package.
2. Reload BetonQuest (`/bq reload`).
3. Run manual test action:
   - `bq action <player> gps_to_spawn_poi`
4. Confirm GPS starts in-game.
5. Switch debug template in EnorialsGPS `configs/betonquest.yml`:
   - `event: "say [GPS-HOOK] {player} {value}"`
6. Start/stop/reach route and verify hook payloads appear.
7. Restore production template:
   - `event: "bq action {player} {value}"`
