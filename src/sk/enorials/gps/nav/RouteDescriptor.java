/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 */
package sk.enorials.gps.nav;

import org.bukkit.Location;

public record RouteDescriptor(Type type, String value) {
    public static RouteDescriptor player(String string) {
        return new RouteDescriptor(Type.PLAYER, string);
    }

    public static RouteDescriptor mob(String string) {
        return new RouteDescriptor(Type.MOB, string);
    }

    public static RouteDescriptor npc(String string) {
        return new RouteDescriptor(Type.NPC, string);
    }

    public static RouteDescriptor poi(String string) {
        return new RouteDescriptor(Type.POI, string);
    }

    public static RouteDescriptor location(Location location) {
        String string = location.getWorld().getName() + ";" + location.getX() + ";" + location.getY() + ";" + location.getZ();
        return new RouteDescriptor(Type.LOCATION, string);
    }

    public static enum Type {
        PLAYER,
        MOB,
        NPC,
        POI,
        LOCATION;

    }
}

