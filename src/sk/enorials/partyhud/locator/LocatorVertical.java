/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package sk.enorials.partyhud.locator;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class LocatorVertical {
    private static final double THRESHOLD = 2.0;

    private LocatorVertical() {
    }

    public static String decorate(Player player, Player player2, String string) {
        if (player != null && player2 != null) {
            Location location = player.getLocation();
            Location location2 = player2.getLocation();
            if (location != null && location2 != null) {
                double d = location2.getY() - location.getY();
                if (d > 2.0) {
                    return LocatorVertical.withIndicator(string, "^");
                }
                return d < -2.0 ? LocatorVertical.withIndicator(string, "v") : string;
            }
            return string;
        }
        return string;
    }

    private static String withIndicator(String string, String string2) {
        return string != null && !string.isEmpty() ? string + " " + string2 : string2;
    }
}

