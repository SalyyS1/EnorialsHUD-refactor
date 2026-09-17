/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 */
package sk.enorials.hud.model;

import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;

public record FunctionData(String listener, int increment, Orientation orientation, boolean reversed) {
    public static FunctionData from(ConfigurationSection configurationSection) {
        if (configurationSection == null) {
            return null;
        }
        String string = configurationSection.getString("listener", "").toLowerCase(Locale.ROOT);
        int n = configurationSection.getInt("increment", 1);
        String string2 = configurationSection.getString("orientation", "horizontal").toUpperCase(Locale.ROOT);
        Orientation orientation = string2.equals("VERTICAL") ? Orientation.VERTICAL : Orientation.HORIZONTAL;
        boolean bl = configurationSection.getBoolean("reversed", false);
        return new FunctionData(string, n, orientation, bl);
    }

    public static enum Orientation {
        HORIZONTAL,
        VERTICAL;

    }
}

