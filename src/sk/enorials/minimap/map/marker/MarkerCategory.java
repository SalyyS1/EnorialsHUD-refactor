/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map.marker;

import java.util.Locale;

public enum MarkerCategory {
    GENERAL,
    HOME,
    WARPS,
    PARTY,
    QUEST,
    DANGER;


    public static MarkerCategory fromString(String string) {
        if (string != null && !string.isBlank()) {
            try {
                return MarkerCategory.valueOf(string.trim().toUpperCase(Locale.ENGLISH));
            }
            catch (IllegalArgumentException illegalArgumentException) {
                return GENERAL;
            }
        }
        return GENERAL;
    }
}

