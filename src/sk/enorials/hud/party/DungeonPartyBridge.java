/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.playavalon.mythicdungeons.MythicDungeons
 *  net.playavalon.mythicdungeons.api.party.IDungeonParty
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.server.PluginDisableEvent
 *  org.bukkit.event.server.PluginEnableEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.hud.party;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.playavalon.mythicdungeons.MythicDungeons;
import net.playavalon.mythicdungeons.api.party.IDungeonParty;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.module.PartyConfig;
import sk.enorials.partyhud.event.PartyCreateEvent;
import sk.enorials.partyhud.event.PartyDisbandEvent;
import sk.enorials.partyhud.event.PartyJoinEvent;
import sk.enorials.partyhud.event.PartyLeaveEvent;
import sk.enorials.partyhud.party.Party;

public final class DungeonPartyBridge
implements Listener,
AutoCloseable {
    private final EnorialsHUD hud;
    private Adapter adapter;
    private final BukkitTask task;
    private boolean warned;

    public DungeonPartyBridge(EnorialsHUD enorialsHUD) {
        this.hud = enorialsHUD;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
        this.task = Bukkit.getScheduler().runTaskTimer((Plugin)enorialsHUD, this::sync, 1L, 20L);
    }

    public void sync() {
        block4: {
            if (!Bukkit.getPluginManager().isPluginEnabled("MythicDungeons") || !this.hud.cfg().getBoolean("integrations.mythicdungeons.parties", true)) {
                return;
            }
            try {
                if (this.adapter == null) {
                    this.adapter = new Adapter(this.hud);
                }
                this.adapter.sync();
            }
            catch (LinkageError | RuntimeException throwable) {
                if (this.warned) break block4;
                this.warned = true;
                this.hud.getLogger().warning("MythicDungeons party integration: " + throwable.getMessage());
            }
        }
    }

    public void start(Player player, String string) {
        if (!string.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid dungeon name");
        }
        this.sync();
        if (this.adapter == null || !this.adapter.selected()) {
            throw new IllegalStateException("Set MythicDungeons General.PartyPlugin to EnorialsHUD and restart the server");
        }
        Party party = this.hud.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
        if (party != null && !party.getLeader().equals(player.getUniqueId())) {
            throw new IllegalArgumentException("Only the Enorials party leader can start a dungeon");
        }
        player.performCommand("mythicdungeons:md play " + string);
    }

    @EventHandler
    public void enable(PluginEnableEvent pluginEnableEvent) {
        if (pluginEnableEvent.getPlugin().getName().equals("MythicDungeons")) {
            this.sync();
        }
    }

    @EventHandler
    public void disable(PluginDisableEvent pluginDisableEvent) {
        if (pluginDisableEvent.getPlugin().getName().equals("MythicDungeons")) {
            this.adapter = null;
            this.warned = false;
        }
    }

    @EventHandler
    public void join(PlayerJoinEvent playerJoinEvent) {
        Bukkit.getScheduler().runTask((Plugin)this.hud, this::sync);
    }

    @EventHandler
    public void party(PartyCreateEvent partyCreateEvent) {
        this.sync();
    }

    @EventHandler
    public void party(PartyJoinEvent partyJoinEvent) {
        this.sync();
    }

    @EventHandler
    public void party(PartyLeaveEvent partyLeaveEvent) {
        this.sync();
    }

    @EventHandler
    public void party(PartyDisbandEvent partyDisbandEvent) {
        this.sync();
    }

    @Override
    public void close() {
        this.task.cancel();
        HandlerList.unregisterAll((Listener)this);
        if (this.adapter != null && Bukkit.getPluginManager().isPluginEnabled("MythicDungeons")) {
            this.adapter.close();
        }
        this.adapter = null;
    }

    private static final class Adapter {
        final EnorialsHUD hud;
        final Map<UUID, View> parties = new HashMap<UUID, View>();
        final Map<UUID, View> assigned = new HashMap<UUID, View>();

        Adapter(EnorialsHUD enorialsHUD) {
            this.hud = enorialsHUD;
        }

        boolean selected() {
            return MythicDungeons.inst().getPartyPluginName().equalsIgnoreCase(this.hud.getName()) || MythicDungeons.inst().getPartyPluginName().equalsIgnoreCase("EnorialsAnchorHUD");
        }

        void sync() {
            if (!this.selected()) {
                return;
            }
            MythicDungeons mythicDungeons = MythicDungeons.inst();
            HashSet<UUID> hashSet = new HashSet<UUID>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                View view;
                View view2;
                Party party = this.hud.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
                View view3 = view2 = party == null ? null : this.parties.computeIfAbsent(party.getId(), uUID -> new View(this.hud, party));
                if (view2 != null) {
                    hashSet.add(party.getId());
                }
                if ((view = this.assigned.get(player.getUniqueId())) == view2) continue;
                mythicDungeons.getProviderManager().remove(player);
                mythicDungeons.getMythicPlayer(player).setDungeonParty((IDungeonParty)view2);
                if (view2 == null) {
                    this.assigned.remove(player.getUniqueId());
                    continue;
                }
                this.assigned.put(player.getUniqueId(), view2);
            }
            this.assigned.keySet().removeIf(uUID -> Bukkit.getPlayer((UUID)uUID) == null);
            this.parties.keySet().retainAll(hashSet);
        }

        void close() {
            MythicDungeons mythicDungeons = MythicDungeons.inst();
            for (UUID uUID : this.assigned.keySet()) {
                Player player = Bukkit.getPlayer((UUID)uUID);
                if (player == null) continue;
                mythicDungeons.getProviderManager().remove(player);
                mythicDungeons.getMythicPlayer(player).setDungeonParty(null);
            }
            this.assigned.clear();
            this.parties.clear();
        }
    }

    private static final class View
    implements IDungeonParty {
        final EnorialsHUD hud;
        final Party party;

        View(EnorialsHUD enorialsHUD, Party party) {
            this.hud = enorialsHUD;
            this.party = party;
        }

        public boolean hasPlayer(Player player) {
            return this.hud.getPartyService().getPartyOf(player.getUniqueId()).map(party -> party.getId().equals(this.party.getId())).orElse(false);
        }

        public List<Player> getPlayers() {
            return this.party.getMembers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).filter(this::hasPlayer).toList();
        }

        public OfflinePlayer getLeader() {
            return Bukkit.getOfflinePlayer((UUID)this.party.getLeader());
        }

        public void addPlayer(Player player) {
            if (this.party.isOpen()) {
                this.hud.getPartyService().joinParty(this.party, player, PartyConfig.get((Plugin)this.hud).getInt("party.max-size", 4));
            }
        }

        public void removePlayer(Player player) {
            if (this.hasPlayer(player)) {
                this.hud.getPartyService().leaveParty(player.getUniqueId(), false);
            }
        }
    }
}

