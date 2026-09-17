/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.theme;

import java.util.Arrays;
import java.util.List;

public enum HudTheme {
    ORIGINAL("original", "Enorials"),
    FROSTBOUND("frostbound", "Frostbound"),
    WILDWOOD("wildwood", "Wildwood"),
    INFERNO("inferno", "Inferno"),
    ASTRAL("astral", "Astral"),
    CORSAIR("corsair", "Corsair");

    private final String id;
    private final String title;

    private HudTheme(String string2, String string3) {
        this.id = string2;
        this.title = string3;
    }

    public String id() {
        return this.id;
    }

    public String title() {
        return this.title;
    }

    public int minimapFrame() {
        return switch (this.ordinal()) {
            default -> throw new IncompatibleClassChangeError();
            case 0 -> 0;
            case 1 -> 1;
            case 5 -> 2;
            case 3 -> 3;
            case 2 -> 4;
            case 4 -> 5;
        };
    }

    public static List<String> ids() {
        return Arrays.stream(HudTheme.values()).map(HudTheme::id).toList();
    }

    public static HudTheme parse(String string) {
        return Arrays.stream(HudTheme.values()).filter(hudTheme -> hudTheme.id.equals(string)).findFirst().orElseThrow(() -> new IllegalArgumentException("Theme: " + String.join((CharSequence)", ", HudTheme.ids())));
    }

    public static HudTheme stored(String string) {
        try {
            return HudTheme.parse(string);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return ORIGINAL;
        }
    }
}

