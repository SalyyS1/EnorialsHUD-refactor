/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.friends;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.module.PartyConfig;
import sk.enorials.partyhud.friends.FriendConfig;
import sk.enorials.partyhud.friends.FriendSaveScheduler;
import sk.enorials.partyhud.friends.FriendSqlite;

public class FriendService {
    private final Plugin plugin;
    private final File file;
    private FileConfiguration cfg;
    private final Map<UUID, Set<UUID>> friends = new ConcurrentHashMap<UUID, Set<UUID>>();

    public FriendService(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(PartyConfig.folder(plugin), "friends.yml");
        this.load();
    }

    public Set<UUID> getFriends(UUID uUID) {
        return !FriendConfig.enabled(this.plugin) ? Collections.emptySet() : this.friends.getOrDefault(uUID, Collections.emptySet());
    }

    public boolean areFriends(UUID uUID, UUID uUID2) {
        return !FriendConfig.enabled(this.plugin) ? false : this.friends.getOrDefault(uUID, Collections.emptySet()).contains(uUID2);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean addFriend(UUID uUID2, UUID uUID3) {
        if (!FriendConfig.enabled(this.plugin)) {
            return false;
        }
        if (uUID2.equals(uUID3)) {
            return false;
        }
        Map<UUID, Set<UUID>> map = this.friends;
        synchronized (map) {
            this.friends.computeIfAbsent(uUID2, uUID -> ConcurrentHashMap.newKeySet()).add(uUID3);
            this.friends.computeIfAbsent(uUID3, uUID -> ConcurrentHashMap.newKeySet()).add(uUID2);
        }
        FriendSaveScheduler.queue(this);
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean removeFriend(UUID uUID, UUID uUID2) {
        if (!FriendConfig.enabled(this.plugin)) {
            return false;
        }
        boolean bl = false;
        Map<UUID, Set<UUID>> map = this.friends;
        synchronized (map) {
            Set<UUID> set;
            Set<UUID> set2 = this.friends.get(uUID);
            if (set2 != null) {
                bl |= set2.remove(uUID2);
            }
            if ((set = this.friends.get(uUID2)) != null) {
                bl |= set.remove(uUID);
            }
        }
        if (bl) {
            FriendSaveScheduler.queue(this);
        }
        return bl;
    }

    public String name(UUID uUID) {
        Object object;
        Player player;
        if (uUID == null) {
            return "";
        }
        EnorialsHUD enorialsHUD = EnorialsHUD.getInstance();
        Player player2 = player = enorialsHUD != null && enorialsHUD.api() != null ? enorialsHUD.api().resolveMember(uUID) : Bukkit.getPlayer((UUID)uUID);
        if (player != null && player.getName() != null) {
            return player.getName();
        }
        try {
            String string;
            object = Bukkit.class.getMethod("getOfflinePlayerIfCached", UUID.class);
            Object object2 = ((Method)object).invoke(null, uUID);
            if (object2 instanceof OfflinePlayer && (string = ((OfflinePlayer)object2).getName()) != null) {
                return string;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        object = Bukkit.getOfflinePlayer((UUID)uUID);
        return object.getName() != null ? object.getName() : uUID.toString();
    }

    private void load() {
        if (!FriendConfig.enabled(this.plugin)) {
            this.friends.clear();
        } else if (FriendConfig.useSqlite(this.plugin)) {
            FriendSqlite.load(this.plugin, this.friends);
        } else {
            FriendConfig.ensureYaml(this.plugin);
            try {
                if (!this.file.exists()) {
                    this.file.getParentFile().mkdirs();
                    this.file.createNewFile();
                }
            }
            catch (IOException iOException) {
                this.plugin.getLogger().warning("Failed to create friends.yml: " + iOException.getMessage());
            }
            this.cfg = YamlConfiguration.loadConfiguration((File)this.file);
            this.friends.clear();
            for (String string : this.cfg.getKeys(false)) {
                try {
                    UUID uUID = UUID.fromString(string);
                    List list = this.cfg.getStringList(string);
                    ConcurrentHashMap.KeySetView<UUID, Boolean> keySetView = ConcurrentHashMap.newKeySet();
                    for (String string2 : list) {
                        try {
                            keySetView.add(UUID.fromString(string2));
                        }
                        catch (IllegalArgumentException illegalArgumentException) {}
                    }
                    if (keySetView.isEmpty()) continue;
                    this.friends.put(uUID, keySetView);
                }
                catch (IllegalArgumentException illegalArgumentException) {}
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public synchronized void save() {
        if (FriendConfig.enabled(this.plugin)) {
            if (FriendConfig.useSqlite(this.plugin)) {
                ConcurrentHashMap<UUID, Set<UUID>> concurrentHashMap = new ConcurrentHashMap<UUID, Set<UUID>>();
                Map<UUID, Set<UUID>> map = this.friends;
                synchronized (map) {
                    for (Map.Entry<UUID, Set<UUID>> entry : this.friends.entrySet()) {
                        concurrentHashMap.put(entry.getKey(), new HashSet(entry.getValue()));
                    }
                }
                FriendSqlite.saveAll(this.plugin, concurrentHashMap);
            } else {
                YamlConfiguration yamlConfiguration = new YamlConfiguration();
                Map<UUID, Set<UUID>> map = this.friends;
                synchronized (map) {
                    for (Map.Entry<UUID, Set<UUID>> entry : this.friends.entrySet()) {
                        ArrayList<String> arrayList = new ArrayList<String>();
                        for (UUID uUID : entry.getValue()) {
                            arrayList.add(uUID.toString());
                        }
                        yamlConfiguration.set(entry.getKey().toString(), arrayList);
                    }
                }
                try {
                    yamlConfiguration.save(this.file);
                }
                catch (IOException iOException) {
                    this.plugin.getLogger().warning("Failed to save friends.yml: " + iOException.getMessage());
                }
                this.cfg = yamlConfiguration;
            }
        }
    }

    public void close() {
        this.save();
    }

    public void shutdown() {
        this.save();
        this.friends.clear();
    }
}

