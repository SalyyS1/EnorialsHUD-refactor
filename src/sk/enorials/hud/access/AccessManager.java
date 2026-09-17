/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.permissions.Permission
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.hud.access;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.minimap.clientside.MinimapPacketSender;
import sk.enorials.minimap.clientside.gui.GuiMinimapPacketSender;
import sk.enorials.minimap.map.Minimap;

public final class AccessManager
implements Listener,
AutoCloseable {
    private final EnorialsHUD hud;
    private final File file;
    private YamlConfiguration config;
    private final Map<UUID, Map<String, Optional<Boolean>>> resolved = new ConcurrentHashMap<UUID, Map<String, Optional<Boolean>>>();
    private final Map<UUID, Integer> signatures = new HashMap<UUID, Integer>();
    private BukkitTask task;

    public AccessManager(EnorialsHUD enorialsHUD) {
        this.hud = enorialsHUD;
        this.file = new File(enorialsHUD.getDataFolder(), "access.yml");
        if (!this.file.exists()) {
            enorialsHUD.saveResource("access.yml", false);
        }
        this.reload();
        HudAccess.install(this);
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
        this.task = Bukkit.getScheduler().runTaskTimer((Plugin)enorialsHUD, this::refresh, 20L, 20L);
    }

    public synchronized void reload() {
        this.config = YamlConfiguration.loadConfiguration((File)this.file);
        this.resolved.clear();
    }

    private void save() {
        try {
            this.config.save(this.file);
            this.resolved.clear();
        }
        catch (IOException iOException) {
            throw new IllegalStateException("Cannot save EnorialsHUD access.yml", iOException);
        }
    }

    Boolean local(UUID uUID2, String string) {
        Map map = this.resolved.computeIfAbsent(uUID2, uUID -> new ConcurrentHashMap());
        Optional<Boolean> optional = (Optional<Boolean>)map.get(string);
        if (optional == null) {
            optional = Optional.ofNullable(this.resolve(uUID2, string));
            map.put(string, optional);
        }
        return optional.orElse(null);
    }

    private synchronized Boolean resolve(UUID uUID, String string) {
        Boolean bl = this.rule("players." + String.valueOf(uUID), string);
        return bl != null ? bl : this.rule("defaults", string);
    }

    private Boolean rule(String string, String string2) {
        List list = this.config.getStringList(string + ".deny");
        List list2 = this.config.getStringList(string + ".grant");
        if (list.contains(string2)) {
            return false;
        }
        if (list2.contains(string2)) {
            return true;
        }
        for (String string3 : list) {
            if (!HudAccess.includes(string3, string2, new HashSet<String>())) continue;
            return false;
        }
        for (String string3 : list2) {
            if (!HudAccess.includes(string3, string2, new HashSet<String>())) continue;
            return true;
        }
        return null;
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public synchronized void join(PlayerJoinEvent playerJoinEvent) {
        Player player = playerJoinEvent.getPlayer();
        String string = "players." + String.valueOf(player.getUniqueId());
        if (this.config.getBoolean(string + ".first-join-processed", false)) {
            return;
        }
        this.config.set(string + ".name", (Object)player.getName());
        this.config.set(string + ".first-join-processed", (Object)true);
        if (!player.hasPlayedBefore()) {
            LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>(this.config.getStringList(string + ".grant"));
            for (String string2 : this.config.getStringList("first-join.grant")) {
                if (this.known(string2)) {
                    linkedHashSet.add(string2);
                    continue;
                }
                this.hud.getLogger().warning("Unknown first-join permission: " + string2);
            }
            this.config.set(string + ".grant", new ArrayList(linkedHashSet));
        }
        this.save();
    }

    private boolean known(String string) {
        return (string.startsWith("enorials.") || string.startsWith("enorialshud.")) && Bukkit.getPluginManager().getPermission(string) != null;
    }

    public synchronized boolean command(CommandSender commandSender, String[] stringArray) {
        if (!HudAccess.check(commandSender, "enorials.hud.admin")) {
            return true;
        }
        try {
            Object object;
            Object object2;
            int n;
            Object object3;
            if (stringArray.length == 1 && stringArray[0].equalsIgnoreCase("reload")) {
                this.reload();
                commandSender.sendMessage("EnorialsHUD: access.yml reloaded.");
                return true;
            }
            if (stringArray.length == 1 && stringArray[0].equalsIgnoreCase("list")) {
                this.hud.getDescription().getPermissions().stream().map(Permission::getName).sorted().forEach(arg_0 -> ((CommandSender)commandSender).sendMessage(arg_0));
                return true;
            }
            if (stringArray.length == 3 && stringArray[0].equalsIgnoreCase("default")) {
                object3 = "defaults";
                n = 1;
            } else if (stringArray.length == 4 && stringArray[0].equalsIgnoreCase("player")) {
                try {
                    object2 = UUID.fromString(stringArray[1]);
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    object = Bukkit.getOfflinePlayerIfCached((String)stringArray[1]);
                    if (object == null) {
                        throw new IllegalArgumentException("Unknown player; use an online/cached player name or UUID.");
                    }
                    object2 = object.getUniqueId();
                }
                object3 = "players." + String.valueOf(object2);
                n = 2;
            } else {
                throw new IllegalArgumentException("Usage: /enohud access list | reload | default <allow|deny|unset> <node> | player <name|UUID> <allow|deny|unset> <node>");
            }
            object2 = stringArray[n].toLowerCase(Locale.ROOT);
            String string = stringArray[n + 1].toLowerCase(Locale.ROOT);
            if (!Set.of("allow", "deny", "unset").contains(object2) || !this.known(string)) {
                throw new IllegalArgumentException("Use allow, deny or unset and a known EnorialsHUD permission (access list).");
            }
            object = new LinkedHashSet(this.config.getStringList((String)object3 + ".grant"));
            LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>(this.config.getStringList((String)object3 + ".deny"));
            ((HashSet)object).remove(string);
            linkedHashSet.remove(string);
            if (((String)object2).equals("allow")) {
                ((HashSet)object).add(string);
            }
            if (((String)object2).equals("deny")) {
                linkedHashSet.add(string);
            }
            this.config.set((String)object3 + ".grant", new ArrayList(object));
            this.config.set((String)object3 + ".deny", new ArrayList(linkedHashSet));
            this.save();
            commandSender.sendMessage("EnorialsHUD: " + (String)object3 + " " + (String)object2 + " " + string + ". Explicit Bukkit/LuckPerms rules take precedence.");
        }
        catch (IllegalArgumentException illegalArgumentException) {
            commandSender.sendMessage("EnorialsHUD: " + illegalArgumentException.getMessage());
        }
        return true;
    }

    private void refresh() {
        if (this.hud.minimap() == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            Object object2;
            int n = 1;
            for (Object object2 : this.hud.getDescription().getPermissions()) {
                n = 31 * n + (HudAccess.has((CommandSender)player, object2.getName()) ? 1 : 0);
            }
            Integer n2 = this.signatures.put(player.getUniqueId(), n);
            object2 = this.hud.getPlayerMinimap(player);
            if (object2 != null && !HudAccess.map(player)) {
                this.hud.queueMinimapSave((Minimap)object2);
                this.hud.minimap().minimapListener().disableMinimap(player);
            } else if (object2 != null && !Objects.equals(n2, n)) {
                ((Minimap)object2).update(this.hud.minimap());
                MinimapPacketSender minimapPacketSender = this.hud.minimap().packetSender();
                if (minimapPacketSender instanceof GuiMinimapPacketSender) {
                    GuiMinimapPacketSender guiMinimapPacketSender = (GuiMinimapPacketSender)minimapPacketSender;
                    guiMinimapPacketSender.refreshPermissions(player);
                }
            } else if (object2 == null && n2 != null && !Objects.equals(n2, n) && HudAccess.map(player)) {
                try {
                    this.hud.minimap().playerDataStorage().restore(this.hud.minimap(), player);
                }
                catch (Exception exception) {
                    this.hud.getLogger().log(Level.WARNING, "Could not restore minimap after access change", exception);
                }
            }
            if (this.hud.getFullscreenMinimap(player) == null || HudAccess.map(player) && HudAccess.has((CommandSender)player, "enorials.minimap.fullscreen") && (n2 == null || Objects.equals(n2, n))) continue;
            this.hud.minimap().minimapListener().closeFullscreen(player);
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        this.resolved.remove(playerQuitEvent.getPlayer().getUniqueId());
        this.signatures.remove(playerQuitEvent.getPlayer().getUniqueId());
    }

    @Override
    public void close() {
        if (this.task != null) {
            this.task.cancel();
        }
        HandlerList.unregisterAll((Listener)this);
        this.resolved.clear();
        HudAccess.install(null);
    }
}

