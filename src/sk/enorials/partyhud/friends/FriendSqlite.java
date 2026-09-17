/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.friends;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.module.PartyConfig;

public final class FriendSqlite {
    private FriendSqlite() {
    }

    public static void load(Plugin plugin, Map<UUID, Set<UUID>> map) {
        if (plugin != null && map != null) {
            File file = new File(PartyConfig.folder(plugin), "friends.db");
            FriendSqlite.ensureFolder(plugin);
            try (Connection connection = FriendSqlite.open(file);){
                FriendSqlite.ensureSchema(connection);
                map.clear();
                try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT user_uuid, friend_uuid FROM friends");
                     ResultSet resultSet = preparedStatement.executeQuery();){
                    while (resultSet.next()) {
                        UUID uUID2 = FriendSqlite.parseUuid(resultSet.getString(1));
                        UUID uUID3 = FriendSqlite.parseUuid(resultSet.getString(2));
                        if (uUID2 == null || uUID3 == null) continue;
                        map.computeIfAbsent(uUID2, uUID -> new HashSet()).add(uUID3);
                    }
                }
            }
            catch (SQLException sQLException) {
                plugin.getLogger().log(Level.WARNING, "Failed to load friends from SQLite.", sQLException);
            }
        }
    }

    public static void saveAll(Plugin plugin, Map<UUID, Set<UUID>> map) {
        if (plugin != null && map != null) {
            File file = new File(PartyConfig.folder(plugin), "friends.db");
            FriendSqlite.ensureFolder(plugin);
            try (Connection connection = FriendSqlite.open(file);){
                FriendSqlite.ensureSchema(connection);
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement();){
                    statement.executeUpdate("DELETE FROM friends");
                }
                statement = connection.prepareStatement("INSERT OR IGNORE INTO friends(user_uuid, friend_uuid) VALUES(?, ?)");
                try {
                    for (Map.Entry<UUID, Set<UUID>> entry : map.entrySet()) {
                        UUID uUID = entry.getKey();
                        if (uUID == null) continue;
                        for (UUID uUID2 : entry.getValue()) {
                            if (uUID2 == null) continue;
                            statement.setString(1, uUID.toString());
                            statement.setString(2, uUID2.toString());
                            statement.addBatch();
                        }
                    }
                    statement.executeBatch();
                    connection.commit();
                }
                catch (SQLException sQLException) {
                    try {
                        connection.rollback();
                    }
                    catch (SQLException sQLException2) {
                        plugin.getLogger().log(Level.WARNING, "Failed to rollback friends SQLite save.", sQLException2);
                    }
                    throw sQLException;
                }
                finally {
                    statement.close();
                    try {
                        connection.setAutoCommit(true);
                    }
                    catch (SQLException sQLException) {}
                }
            }
            catch (SQLException sQLException) {
                plugin.getLogger().log(Level.WARNING, "Failed to save friends to SQLite.", sQLException);
            }
        }
    }

    private static void ensureFolder(Plugin plugin) {
        File file = PartyConfig.folder(plugin);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    private static Connection open(File file) throws SQLException {
        String string = file.getAbsolutePath().replace('\\', '/');
        return DriverManager.getConnection("jdbc:sqlite:" + string);
    }

    private static void ensureSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();){
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS friends (user_uuid TEXT NOT NULL, friend_uuid TEXT NOT NULL, PRIMARY KEY(user_uuid, friend_uuid))");
        }
    }

    private static UUID parseUuid(String string) {
        if (string == null) {
            return null;
        }
        try {
            return UUID.fromString(string);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }
}

