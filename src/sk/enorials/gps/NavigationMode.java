/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.gps;

public enum NavigationMode {
    HUD,
    ACTIONBAR,
    BOSSBAR,
    COMPASS;


    public static NavigationMode hudMode(NavigationMode navigationMode) {
        return navigationMode == COMPASS ? COMPASS : HUD;
    }

    public static NavigationMode fromConfig(String string) {
        if (string == null) {
            return ACTIONBAR;
        }
        return switch (string.toLowerCase()) {
            case "hud" -> HUD;
            case "bossbar" -> BOSSBAR;
            case "compass" -> COMPASS;
            default -> ACTIONBAR;
        };
    }

    public String display() {
        return switch (this.ordinal()) {
            default -> throw new IncompatibleClassChangeError();
            case 0 -> "HUD";
            case 1 -> "ActionBar";
            case 2 -> "BossBar";
            case 3 -> "Compass";
        };
    }
}

