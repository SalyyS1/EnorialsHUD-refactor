# EnorialsGPS Developer API - Complete Reference

Owner contact: `enorrial@gmail.com`  
Owner Discord: `klon_92`

This is the single developer-focused file for integrating with EnorialsGPS.

## 1) Compatibility

- Minecraft/Paper: `1.19.x`, `1.20.x`, `1.21.x`
- Java: `17`
- Plugin API is exposed via Bukkit `ServicesManager`.

## 2) Add dependency in your plugin

`plugin.yml`:

```yaml
softdepend:
  - EnorialsGPS
```

## 3) Get Java API service

```java
import sk.enorials.gps.api.EnorialsGpsApi;
import sk.enorials.gps.api.EnorialsGpsApiProvider;

EnorialsGpsApi api = EnorialsGpsApiProvider.get().orElse(null);
if (api == null) {
    // EnorialsGPS not loaded/enabled
    return;
}
```

## 4) Java API surface

Interface: `sk.enorials.gps.api.EnorialsGpsApi`

```java
String apiVersion();
boolean isModeEnabled(NavigationMode mode);
NavigationMode getPlayerMode(Player player);
void setPlayerMode(Player player, NavigationMode mode);
boolean hasActiveNavigation(Player player);
boolean stopNavigation(Player player, String reason);
boolean startNavigation(Player player, GpsTargetType targetType, String targetValue);
boolean startNavigation(Player player, GpsTargetType targetType, String targetValue, NavigationMode mode, String source);
boolean startNavigation(Player player, RouteDescriptor descriptor, NavigationMode mode, String source);
boolean addQuestRouteButton(Player player, String routeId, String displayName, GpsTargetType targetType, String targetValue);
boolean addQuestRouteButton(Player player, String routeId, String displayName, GpsTargetType targetType, String targetValue, NavigationMode mode, String source);
boolean removeQuestRouteButton(Player player, String routeId);
int clearQuestRouteButtons(Player player);
Map<String, GpsQuestRoute> getQuestRouteButtons(Player player);
```

Notes:
- Methods return `false` when route/target cannot be resolved or action fails.
- `routeId` can be blank in add calls (auto-generated).
- API works together with `/gpsapi` command model.

## 5) Target types and values

Enum: `sk.enorials.gps.api.GpsTargetType`

- `PLAYER` -> player UUID/name input
- `MOB` -> e.g. `ZOMBIE`, `mythic:BossSkeleton`, `tag:event_boss`
- `NPC` -> e.g. `citizens:1`, `fancynpc:guard_1`, `znpcs:15`
- `POI` -> POI key from `poi.points`
- `LOCATION` -> supports:
  - `100 64 -30 world`
  - `warp:spawn`
  - `wg:spawn_region`
  - `town:SpawnTown`
  - `dh:quest_board`

## 6) Navigation modes

Enum: `sk.enorials.gps.NavigationMode`

- `ACTIONBAR`
- `BOSSBAR`
- `COMPASS`

## 7) Bukkit events (listen for GPS lifecycle)

Base: `sk.enorials.gps.api.event.GpsPlayerEvent` -> `getPlayer()`

Pre-events (cancellable):
- `GpsNavigationPreStartEvent`
  - `getMode()/setMode(...)`
  - `getSource()/setSource(...)`
  - `getTargetId()`
  - `getTargetDisplayName()`
  - `getDescriptor()`
- `GpsNavigationPreModeChangeEvent`
  - `getOldMode()`
  - `getNewMode()/setNewMode(...)`

Post-events:
- `GpsNavigationStartEvent`
  - mode/source/target/descriptor
- `GpsNavigationStopEvent`
  - mode/source/reason/target/descriptor
- `GpsNavigationReachEvent`
  - mode/source/target/descriptor
- `GpsNavigationModeChangeEvent`
  - old/new mode

Example listener:

```java
@EventHandler
public void onGpsPreStart(sk.enorials.gps.api.event.GpsNavigationPreStartEvent event) {
    if (event.getTargetId().contains("forbidden")) {
        event.setCancelled(true);
    }
}

@EventHandler
public void onGpsReach(sk.enorials.gps.api.event.GpsNavigationReachEvent event) {
    event.getPlayer().sendMessage("Reached: " + event.getTargetDisplayName());
}
```

## 8) Command API (for scripts/quests)

Permission: `enorials.gps.api`

Main command:

```text
/gpsapi start <player> <type> <value...>
/gpsapi stop <player>
/gpsapi mode <player> <actionbar|bossbar|compass>
/gpsapi route add <player> ...
/gpsapi route remove <player> <routeId>
/gpsapi route clear <player>
/gpsapi route list <player>
```

Common start types:
- `player`, `mob`, `npc`, `poi`, `loc`, `warp`, `region`, `town`, `hologram`, `block`

## 9) Dynamic Quest Routes (GUI buttons)

Use Java API or `/gpsapi route ...` to manage per-player route buttons shown in **Quest GPS Routes** GUI.

Suggested lifecycle:
- On quest accept: add button
- On quest complete/cancel: remove button
- On player cleanup: clear all

## 10) RouteDescriptor helper

Type: `sk.enorials.gps.nav.RouteDescriptor`

Helpers:
- `RouteDescriptor.player(...)`
- `RouteDescriptor.mob(...)`
- `RouteDescriptor.npc(...)`
- `RouteDescriptor.poi(...)`
- `RouteDescriptor.location(Location)`

## 11) Permissions relevant to integrations

- `enorials.gps.use`
- `enorials.gps.api`
- `enorials.gps.track.player`
- `enorials.gps.track.mob`
- `enorials.gps.track.npc`
- `enorials.gps.track.block`
- `enorials.gps.track.all`
- `enorials.gps.admin`

## 12) Threading / safety notes

- Call API on the Bukkit main thread unless you schedule sync execution.
- Do not block event handlers with long I/O.
- If API is unavailable (`Optional.empty()`), fail gracefully.

## 13) Troubleshooting

- Plugin-specific warnings/errors are written to:
  - `plugins/EnorialsGPS/logs/errors.log`
- For command checks, test from console first:
  - `/gpsapi start <player> poi <key>`

## 14) Minimal end-to-end sample

```java
EnorialsGpsApi api = EnorialsGpsApiProvider.get().orElse(null);
if (api == null) return;

api.setPlayerMode(player, NavigationMode.BOSSBAR);
api.addQuestRouteButton(player, "q_blacksmith", "Find Blacksmith", GpsTargetType.NPC, "citizens:1");
api.startNavigation(player, GpsTargetType.NPC, "citizens:1", NavigationMode.BOSSBAR, "myplugin");
```
