/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.friends;

import org.bukkit.plugin.Plugin;
import sk.enorials.hud.module.PartyConfig;

public final class FriendConfig {
    private static boolean warnedStorage;
    private static boolean warnedDriver;
    private static Boolean sqliteAvailable;

    private FriendConfig() {
    }

    public static boolean enabled(Plugin plugin) {
        return plugin == null ? true : PartyConfig.get(plugin).getBoolean("friends.enabled", true);
    }

    public static boolean useSqlite(Plugin plugin) {
        if (plugin == null) {
            return false;
        }
        String string = PartyConfig.get(plugin).getString("friends.storage", "YAML");
        if (string == null) {
            string = "YAML";
        }
        return !"SQLITE".equalsIgnoreCase(string) ? false : FriendConfig.sqliteDriverAvailable(plugin);
    }

    public static void ensureYaml(Plugin plugin) {
        if (plugin != null) {
            String string = PartyConfig.get(plugin).getString("friends.storage", "YAML");
            if (string == null) {
                string = "YAML";
            }
            if ("SQLITE".equalsIgnoreCase(string)) {
                FriendConfig.sqliteDriverAvailable(plugin);
            } else if (!"YAML".equalsIgnoreCase(string) && !warnedStorage) {
                warnedStorage = true;
                plugin.getLogger().warning("friends.storage=" + string + " is not supported; using YAML.");
            }
        }
    }

    private static boolean sqliteDriverAvailable(Plugin plugin) {
        block3: {
            if (sqliteAvailable != null) {
                return sqliteAvailable;
            }
            try {
                Class.forName("org.sqlite.JDBC");
                sqliteAvailable = Boolean.TRUE;
            }
            catch (Throwable throwable) {
                sqliteAvailable = Boolean.FALSE;
                if (warnedDriver) break block3;
                warnedDriver = true;
                plugin.getLogger().warning("friends.storage=SQLITE requires an SQLite JDBC driver; falling back to YAML.");
            }
        }
        return sqliteAvailable;
    }
}

