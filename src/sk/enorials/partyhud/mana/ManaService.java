/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.partyhud.mana;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.hud.module.PartyConfig;

public class ManaService
implements Listener {
    private final Plugin plugin;
    private final Map<UUID, Integer> mana = new ConcurrentHashMap<UUID, Integer>();
    private BukkitTask regenTask;

    public ManaService(Plugin plugin) {
        this.plugin = plugin;
    }

    public int getMaxMana(Player player) {
        return PartyConfig.get(this.plugin).getInt("mana.max", 100);
    }

    public int getMana(Player player) {
        return this.mana.getOrDefault(player.getUniqueId(), this.getMaxMana(player));
    }

    public void setMana(Player player, int n) {
        int n2 = this.getMaxMana(player);
        this.mana.put(player.getUniqueId(), Math.max(0, Math.min(n2, n)));
    }

    public boolean consume(Player player, int n) {
        int n2 = this.getMana(player);
        if (n2 < n) {
            return false;
        }
        this.setMana(player, n2 - n);
        return true;
    }

    public void start() {
        if (PartyConfig.get(this.plugin).getBoolean("mana.enabled", true)) {
            this.stop();
            int n = PartyConfig.get(this.plugin).getInt("mana.regen-per-second", 2);
            this.regenTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    int n2 = this.getMaxMana(player);
                    int n3 = this.getMana(player);
                    if (n3 >= n2) continue;
                    this.setMana(player, Math.min(n2, n3 + n));
                }
            }, 20L, 20L);
        }
    }

    public void stop() {
        if (this.regenTask != null) {
            this.regenTask.cancel();
            this.regenTask = null;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        Player player = playerJoinEvent.getPlayer();
        this.mana.putIfAbsent(player.getUniqueId(), this.getMaxMana(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
    }
}

