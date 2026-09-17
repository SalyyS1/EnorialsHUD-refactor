/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 */
package sk.enorials.hud.api;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Location;
import sk.enorials.hud.api.HudValue;

public final class HudTypes {
    private HudTypes() {
    }

    public static void requireId(String string) {
        if (string == null || !string.matches("[a-zA-Z0-9_.-]{1,64}")) {
            throw new IllegalArgumentException("ID must match [a-zA-Z0-9_.-]{1,64}");
        }
    }

    public static Location validLocation(Location location) {
        Objects.requireNonNull(location, "location");
        if (!(location.getWorld() != null && Double.isFinite(location.getX()) && Double.isFinite(location.getY()) && Double.isFinite(location.getZ()))) {
            throw new IllegalArgumentException("A loaded world and finite coordinates are required");
        }
        return location.clone();
    }

    public static interface Registration
    extends AutoCloseable {
        @Override
        public void close();
    }

    public record Status(String pluginVersion, boolean packLoading, boolean minimapVisible, boolean conversationActive) {
    }

    public record Target(UUID id, String name, String entityType, Location location, HudValue health) {
        private final Location location;

        public Target {
            location = location.clone();
        }

        public Location location() {
            return this.location.clone();
        }
    }

    public record Quest(String id, String title, String objective, long acceptedAtMillis, boolean tracked) {
    }

    public record Member(UUID id, String name, boolean online, boolean npc, Location location, HudValue health) {
        private final Location location;

        public Member {
            location = location == null ? null : location.clone();
        }

        public Location location() {
            return this.location == null ? null : this.location.clone();
        }
    }

    public record Party(UUID id, UUID leader, List<UUID> members, boolean open, int capacity) {
        public Party {
            list = List.copyOf(list);
        }
    }

    public record QuestRoute(String id, String title, TargetType type, String target, NavigationMode mode, String source) {
    }

    public record Navigation(String title, String source, NavigationMode mode, Location destination) {
        private final Location destination;

        public Navigation {
            location = location == null ? null : location.clone();
        }

        public Location destination() {
            return this.destination == null ? null : this.destination.clone();
        }
    }

    public record Marker(String id, Location location, String icon, String label, boolean keepOnEdge) {
        private final Location location;

        public Marker {
            HudTypes.requireId(string);
            location = HudTypes.validLocation(location);
            Objects.requireNonNull(string2);
            string3 = Objects.requireNonNullElse(string3, "");
            if (string3.length() > 256) {
                throw new IllegalArgumentException("Marker label exceeds 256 characters");
            }
        }

        public Location location() {
            return this.location.clone();
        }
    }

    public record Setting(String key, List<String> values, String permission) {
        public Setting {
            list = List.copyOf(list);
        }
    }

    public static enum TargetType {
        LOCATION,
        PLAYER,
        MOB,
        NPC,
        POI;

    }

    public static enum NavigationMode {
        HUD,
        COMPASS;

    }

    public static enum Radar {
        HOSTILE,
        NEUTRAL,
        FRIENDLY;

    }

    public static enum Zoom {
        NEAR,
        FAR,
        FARTHEST;

    }
}

