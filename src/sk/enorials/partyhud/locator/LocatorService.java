/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.Vector
 */
package sk.enorials.partyhud.locator;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import sk.enorials.hud.module.PartyConfig;
import sk.enorials.partyhud.locator.LocatorVertical;
import sk.enorials.partyhud.party.Party;
import sk.enorials.partyhud.party.PartyService;

public class LocatorService
implements Listener {
    private final Plugin plugin;
    private final PartyService partyService;
    private BukkitTask task;
    private BukkitTask compassTask;
    private final Map<UUID, UUID> trackedTarget = new ConcurrentHashMap<UUID, UUID>();

    public LocatorService(Plugin plugin, PartyService partyService) {
        this.plugin = plugin;
        this.partyService = partyService;
    }

    public void start() {
        if (PartyConfig.get(this.plugin).getBoolean("locator.enabled", true)) {
            this.stop();
            long l = Math.max(2L, PartyConfig.get(this.plugin).getLong("locator.update-ticks", 10L));
            boolean bl = PartyConfig.get(this.plugin).getBoolean("locator.actionbar", true);
            if (bl) {
                this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tickActionbar, 20L, l);
            }
            if (PartyConfig.get(this.plugin).getBoolean("locator.compass.enabled", true)) {
                long l2 = Math.max(10L, PartyConfig.get(this.plugin).getLong("locator.compass.cycle-ticks", 40L));
                this.compassTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tickCompass, 40L, l2);
            }
        }
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        if (this.compassTask != null) {
            this.compassTask.cancel();
            this.compassTask = null;
        }
        this.trackedTarget.clear();
    }

    public Optional<UUID> getTrackedTarget(UUID uUID) {
        return Optional.ofNullable(this.trackedTarget.get(uUID));
    }

    public void toggleTracking(Player player, UUID uUID) {
        UUID uUID2 = this.trackedTarget.get(player.getUniqueId());
        if (uUID2 != null && uUID2.equals(uUID)) {
            this.trackedTarget.remove(player.getUniqueId());
            player.sendActionBar((Component)Component.empty());
        } else {
            this.trackedTarget.put(player.getUniqueId(), uUID);
        }
    }

    private void tickActionbar() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uUID;
            Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
            if (optional.isEmpty() || (uUID = this.trackedTarget.get(player.getUniqueId())) == null) continue;
            Party party = optional.get();
            Player player2 = Bukkit.getPlayer((UUID)uUID);
            if (player2 == null || !player2.isOnline() || !player2.getWorld().equals((Object)player.getWorld()) || !party.getMembers().contains(uUID)) continue;
            String string = this.arrowTo(player, player2.getLocation());
            String string2 = LocatorVertical.decorate(player, player2, string);
            int n = (int)Math.round(player.getLocation().distance(player2.getLocation()));
            Component component = ((TextComponent)((TextComponent)Component.text((String)"Tracking: ", (TextColor)NamedTextColor.GRAY).append((Component)Component.text((String)(string2 + " "), (TextColor)NamedTextColor.YELLOW))).append((Component)Component.text((String)player2.getName(), (TextColor)NamedTextColor.AQUA))).append((Component)Component.text((String)(" " + n + "m"), (TextColor)NamedTextColor.GRAY));
            player.sendActionBar(component);
        }
    }

    private void tickCompass() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uUID;
            Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
            if (optional.isEmpty() || (uUID = this.trackedTarget.get(player.getUniqueId())) == null) continue;
            Party party = optional.get();
            Player player2 = Bukkit.getPlayer((UUID)uUID);
            if (player2 == null || !player2.isOnline() || !player2.getWorld().equals((Object)player.getWorld()) || !party.getMembers().contains(uUID)) continue;
            try {
                player.setCompassTarget(player2.getLocation());
            }
            catch (Throwable throwable) {}
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        UUID uUID = playerQuitEvent.getPlayer().getUniqueId();
        this.trackedTarget.remove(uUID);
    }

    private String arrowTo(Player player, Location location) {
        double d;
        double d2;
        double d3;
        double d4;
        Location location2 = player.getLocation();
        double d5 = location.getX() - location2.getX();
        if (d5 * d5 + d4 * (d3 = location.getZ() - location2.getZ()) < 0.25) {
            return "\ue200";
        }
        Vector vector = location2.getDirection();
        double d6 = vector.getX();
        double d7 = Math.sqrt(d6 * d6 + (d2 = vector.getZ()) * d2);
        if (d7 < 1.0E-6) {
            return "\ue201";
        }
        double d8 = Math.sqrt(d5 * d5 + d3 * d3);
        double d9 = Math.toDegrees(Math.atan2((d6 /= d7) * (d3 /= d8) - (d2 /= d7) * (d5 /= d8), d6 * d5 + d2 * d3));
        if (d >= -22.5 && d9 < 22.5) {
            return "\ue201";
        }
        if (d9 >= 22.5 && d9 < 67.5) {
            return "\ue202";
        }
        if (d9 >= 67.5 && d9 < 112.5) {
            return "\ue203";
        }
        if (d9 >= 112.5 && d9 < 157.5) {
            return "\ue204";
        }
        if (d9 >= 157.5 || d9 < -157.5) {
            return "\ue205";
        }
        if (d9 >= -157.5 && d9 < -112.5) {
            return "\ue206";
        }
        return d9 >= -112.5 && d9 < -67.5 ? "\ue207" : "\ue208";
    }
}

