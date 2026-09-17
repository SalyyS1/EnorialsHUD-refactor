/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.player.PlayerExpChangeEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.sharedexp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.module.PartyConfig;
import sk.enorials.partyhud.party.Party;
import sk.enorials.partyhud.party.PartyService;

public final class SharedExpHandler {
    private static final long CONFIG_CACHE_MS = 1000L;
    private static volatile ConfigSnapshot cachedConfig;
    private static volatile long cachedAtMs;

    private SharedExpHandler() {
    }

    public static void handle(Plugin plugin, PartyService partyService, PlayerExpChangeEvent playerExpChangeEvent, Set<UUID> set) {
        if (plugin != null && partyService != null && playerExpChangeEvent != null && set != null) {
            ConfigSnapshot configSnapshot = SharedExpHandler.getConfig(plugin);
            if (configSnapshot.enabled) {
                Party party;
                UUID uUID;
                Player player = playerExpChangeEvent.getPlayer();
                int n = playerExpChangeEvent.getAmount();
                if (n > 0 && !set.contains(uUID = player.getUniqueId()) && (party = (Party)partyService.getPartyOf(uUID).orElse(null)) != null) {
                    boolean bl = configSnapshot.rangeEnabled;
                    double d = configSnapshot.rangeSq;
                    ArrayList<Player> arrayList = new ArrayList<Player>();
                    for (UUID uUID2 : party.getMembers()) {
                        Player player2;
                        if (uUID2.equals(uUID) || (player2 = Bukkit.getPlayer((UUID)uUID2)) == null || !player2.isOnline() || bl && (!player2.getWorld().equals((Object)player.getWorld()) || player2.getLocation().distanceSquared(player.getLocation()) > d)) continue;
                        arrayList.add(player2);
                    }
                    boolean bl2 = configSnapshot.splitMode;
                    if (bl2) {
                        int n2 = arrayList.size() + 1;
                        if (n2 > 1) {
                            int n3 = n / n2;
                            int n4 = n % n2;
                            playerExpChangeEvent.setAmount(n3 + n4);
                            SharedExpHandler.giveExpToMembers(arrayList, n3, set);
                        }
                    } else {
                        SharedExpHandler.giveExpToMembers(arrayList, n, set);
                    }
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void giveExpToMembers(List<Player> list, int n, Set<UUID> set) {
        if (n > 0) {
            for (Player player : list) {
                UUID uUID = player.getUniqueId();
                if (!set.add(uUID)) continue;
                try {
                    player.giveExp(n);
                }
                finally {
                    set.remove(uUID);
                }
            }
        }
    }

    private static ConfigSnapshot getConfig(Plugin plugin) {
        ConfigSnapshot configSnapshot;
        long l = System.currentTimeMillis();
        ConfigSnapshot configSnapshot2 = cachedConfig;
        if (configSnapshot2 != null && l - cachedAtMs < 1000L && configSnapshot2.source == PartyConfig.get(plugin)) {
            return configSnapshot2;
        }
        cachedConfig = configSnapshot = new ConfigSnapshot(plugin);
        cachedAtMs = l;
        return configSnapshot;
    }

    private static final class ConfigSnapshot {
        private final Object source;
        private final boolean enabled;
        private final boolean rangeEnabled;
        private final double rangeSq;
        private final boolean splitMode;

        private ConfigSnapshot(Plugin plugin) {
            this.source = PartyConfig.get(plugin);
            this.enabled = PartyConfig.get(plugin).getBoolean("shared-exp.enabled", true);
            this.rangeEnabled = PartyConfig.get(plugin).getBoolean("shared-exp.range-check.enabled", true);
            double d = PartyConfig.get(plugin).getDouble("shared-exp.range-check.blocks", 48.0);
            this.rangeSq = d * d;
            String string = PartyConfig.get(plugin).getString("shared-exp.mode", "DUPLICATE");
            this.splitMode = "SPLIT".equalsIgnoreCase(string);
        }
    }
}

