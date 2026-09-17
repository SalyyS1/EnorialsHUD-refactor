# EnorialsGPS Developer API

Owner contact: `enorrial@gmail.com`
Owner Discord: `klon_92`

This plugin now exposes a Bukkit service API so other plugins can integrate directly (without running `/gpsapi` commands).

## 1) Get API service

```java
import sk.enorials.gps.api.EnorialsGpsApi;
import sk.enorials.gps.api.EnorialsGpsApiProvider;

EnorialsGpsApi api = EnorialsGpsApiProvider.get().orElse(null);
if (api == null) {
    // EnorialsGPS not loaded
    return;
}
```

## 2) Start navigation

```java
import sk.enorials.gps.api.GpsTargetType;

// Uses player's current/default mode automatically
api.startNavigation(player, GpsTargetType.POI, "spawn");

// Explicit mode + source
api.startNavigation(player, GpsTargetType.NPC, "citizens:1", NavigationMode.BOSSBAR, "my-quest-plugin");
```

Supported target types:
- `PLAYER`
- `MOB`
- `NPC`
- `POI`
- `LOCATION`

For `LOCATION`, pass the same string formats supported by EnoGPS, for example:
- `0 64 0 world`
- `warp:spawn`
- `wg:spawn_region`
- `town:SpawnTown`
- `dh:quest_board`

## 3) Stop navigation

```java
api.stopNavigation(player, "quest-complete");
```

## 4) Add/remove quest buttons in GUI

```java
import sk.enorials.gps.api.GpsTargetType;

// Adds a button into "Quest GPS Routes" GUI section
api.addQuestRouteButton(
        player,
        "q_blacksmith",          // routeId (can be null/blank -> auto id)
        "Talk to Blacksmith",    // display name
        GpsTargetType.NPC,
        "citizens:1"
);

// Remove when objective is done
api.removeQuestRouteButton(player, "q_blacksmith");
```

You can also clear all dynamic quest buttons:

```java
api.clearQuestRouteButtons(player);
```

## 5) Optional: required dependency in your plugin

Add in your `plugin.yml`:

```yaml
softdepend:
  - EnorialsGPS
```

## 6) Listen to GPS Bukkit events

EnorialsGPS now fires Bukkit events you can listen to:
- `sk.enorials.gps.api.event.GpsNavigationPreStartEvent` (cancellable, mode/source can be modified)
- `sk.enorials.gps.api.event.GpsNavigationPreModeChangeEvent` (cancellable, new mode can be modified)
- `sk.enorials.gps.api.event.GpsNavigationStartEvent`
- `sk.enorials.gps.api.event.GpsNavigationStopEvent`
- `sk.enorials.gps.api.event.GpsNavigationReachEvent`
- `sk.enorials.gps.api.event.GpsNavigationModeChangeEvent`

```java
@EventHandler
public void onGpsPreStart(sk.enorials.gps.api.event.GpsNavigationPreStartEvent event) {
    if (event.getTargetId().contains("forbidden")) {
        event.setCancelled(true);
        return;
    }
    event.setMode(NavigationMode.ACTIONBAR);
}

@EventHandler
public void onGpsPreModeChange(sk.enorials.gps.api.event.GpsNavigationPreModeChangeEvent event) {
    if (event.getNewMode() == NavigationMode.COMPASS) {
        event.setNewMode(NavigationMode.BOSSBAR);
    }
}

@EventHandler
public void onGpsReach(sk.enorials.gps.api.event.GpsNavigationReachEvent event) {
    Player player = event.getPlayer();
    String targetId = event.getTargetId();
    player.sendMessage("Quest step complete: " + targetId);
}
```

## Notes

- API version string is available via `api.apiVersion()`.
- API methods return `false` when target cannot be resolved or action fails.
- This API complements existing `/gpsapi` command integration (both are valid).
