/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.bossbar.BossBar
 *  org.bukkit.Location
 */
package sk.enorials.gps.nav;

import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.gps.nav.target.TargetLocator;

public final class NavigationSession {
    private final UUID playerId;
    private TargetLocator locator;
    private final String source;
    private RouteDescriptor descriptor;
    private NavigationMode mode;
    private BossBar bossBar;
    private int tickCount;
    private Location waypoint;
    private int lastWaypointRecalcTick;
    private Location lastResolvedTarget;
    private int lastResolveTick = -1;
    private boolean questArrivalReported;

    public NavigationSession(UUID uUID, TargetLocator targetLocator, NavigationMode navigationMode, String string, RouteDescriptor routeDescriptor) {
        this.playerId = uUID;
        this.locator = targetLocator;
        this.mode = navigationMode;
        this.source = string;
        this.descriptor = routeDescriptor;
    }

    public UUID playerId() {
        return this.playerId;
    }

    public TargetLocator locator() {
        return this.locator;
    }

    public void updateQuestTarget(TargetLocator targetLocator, RouteDescriptor routeDescriptor) {
        this.locator = targetLocator;
        this.descriptor = routeDescriptor;
        this.lastResolvedTarget = null;
        this.lastResolveTick = -1;
        this.waypoint = null;
        this.lastWaypointRecalcTick = 0;
        this.questArrivalReported = false;
    }

    public NavigationMode mode() {
        return this.mode;
    }

    public boolean isTrackedQuest() {
        return this.source.startsWith("betonquest-quest:");
    }

    public boolean markQuestArrival() {
        if (this.questArrivalReported) {
            return false;
        }
        this.questArrivalReported = true;
        return true;
    }

    public void setMode(NavigationMode navigationMode) {
        this.mode = navigationMode;
    }

    public BossBar bossBar() {
        return this.bossBar;
    }

    public void setBossBar(BossBar bossBar) {
        this.bossBar = bossBar;
    }

    public String source() {
        return this.source;
    }

    public RouteDescriptor descriptor() {
        return this.descriptor;
    }

    public int tickCount() {
        return this.tickCount;
    }

    public void incrementTick() {
        ++this.tickCount;
    }

    public Location waypoint() {
        return this.waypoint == null ? null : this.waypoint.clone();
    }

    public void setWaypoint(Location location) {
        this.waypoint = location == null ? null : location.clone();
    }

    public int lastWaypointRecalcTick() {
        return this.lastWaypointRecalcTick;
    }

    public void setLastWaypointRecalcTick(int n) {
        this.lastWaypointRecalcTick = n;
    }

    public Location lastResolvedTarget() {
        return this.lastResolvedTarget == null ? null : this.lastResolvedTarget.clone();
    }

    public void setLastResolvedTarget(Location location) {
        this.lastResolvedTarget = location == null ? null : location.clone();
    }

    public int lastResolveTick() {
        return this.lastResolveTick;
    }

    public void setLastResolveTick(int n) {
        this.lastResolveTick = n;
    }
}

