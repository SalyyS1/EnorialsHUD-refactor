/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.hud.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.compat.ClientProfiles;
import sk.enorials.hud.compat.ClientRenderProfile;
import sk.enorials.hud.compat.PackTransport;
import sk.enorials.hud.pack.VanillaBarPack;
import sk.enorials.hud.util.LocalPackPublisher;

public final class VanillaBars
implements Listener,
AutoCloseable {
    public static final UUID MAIN_PACK_ID = UUID.nameUUIDFromBytes("enorials-hud-main".getBytes(StandardCharsets.UTF_8));
    public static final UUID OVERLAY_ID = UUID.nameUUIDFromBytes("enorials-hud-vanilla-bars".getBytes(StandardCharsets.UTF_8));
    private final EnorialsHUD plugin;
    private final Map<String, Pack> packs;
    private final Set<UUID> loaded;
    private final Map<UUID, Boolean> baseHidden;
    private final Map<UUID, String> requested;
    private final BukkitTask task;

    public VanillaBars(EnorialsHUD enorialsHUD) {
        block9: {
            this.packs = new ConcurrentHashMap<String, Pack>();
            this.loaded = new HashSet<UUID>();
            this.baseHidden = new HashMap<UUID, Boolean>();
            this.requested = new HashMap<UUID, String>();
            this.plugin = enorialsHUD;
            try {
                if (enorialsHUD.singlePack() != null && enorialsHUD.singlePack().single()) break block9;
                for (String string : List.of("all", "visible")) {
                    InputStream inputStream = enorialsHUD.getResource("vanilla-bars/" + string + ".zip");
                    try {
                        if (inputStream == null) {
                            throw new IllegalStateException("Missing vanilla bars overlay: " + string);
                        }
                        byte[] byArray = VanillaBarPack.overlay(enorialsHUD, string.equals("all"), VanillaBarPack.defaults(enorialsHUD, false));
                        String string2 = LocalPackPublisher.publish(enorialsHUD, byArray);
                        if (string2 == null) continue;
                        this.packs.put(ClientRenderProfile.nativeProfile().key() + ":" + string, new Pack(string2, MessageDigest.getInstance("SHA-1").digest(byArray)));
                    }
                    finally {
                        if (inputStream == null) continue;
                        inputStream.close();
                    }
                }
            }
            catch (Exception exception) {
                enorialsHUD.getLogger().warning("Vanilla HUD overlay unavailable: " + exception.getMessage());
            }
        }
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
        this.task = Bukkit.getScheduler().runTaskTimer((Plugin)enorialsHUD, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.refresh(player);
            }
        }, 1L, 5L);
    }

    public void basePackSent(Player player) {
        this.basePackSent(player, false);
    }

    public void basePackSent(Player player, boolean bl) {
        UUID uUID = player.getUniqueId();
        this.loaded.remove(uUID);
        this.requested.remove(uUID);
        this.baseHidden.put(uUID, bl);
        this.plugin.packLoadGuard().complete(player, OVERLAY_ID);
    }

    public void prepareVisible(Map<String, byte[]> map) throws IOException {
        this.prepareVisible(map, ClientRenderProfile.nativeProfile());
    }

    public void prepareVisible(Map<String, byte[]> map, ClientRenderProfile clientRenderProfile) throws IOException {
        if (this.plugin.singlePack() != null && this.plugin.singlePack().single()) {
            return;
        }
        try {
            for (boolean bl : new boolean[]{false, true}) {
                byte[] byArray = VanillaBarPack.overlay(this.plugin, bl, map, clientRenderProfile);
                String string = LocalPackPublisher.publish(this.plugin, byArray);
                if (string == null) continue;
                this.packs.put(clientRenderProfile.key() + ":" + (bl ? "all" : "visible"), new Pack(string, MessageDigest.getInstance("SHA-1").digest(byArray)));
            }
        }
        catch (Exception exception) {
            throw new IOException("Cannot prepare the owner's visible vanilla bars", exception);
        }
    }

    public void refresh(Player player) {
        String string;
        boolean bl;
        if (this.plugin.singlePack() != null && this.plugin.singlePack().single()) {
            return;
        }
        UUID uUID = player.getUniqueId();
        boolean bl2 = bl = HudAccess.hud(player) && this.plugin.storage().preference(uUID, "hud", true);
        String string2 = !this.loaded.contains(uUID) || bl == this.baseHidden.getOrDefault(uUID, false) ? "" : (string = bl ? "all" : "visible");
        if (string.equals(this.requested.getOrDefault(uUID, ""))) {
            return;
        }
        if (!PackTransport.stacked(player)) {
            if (string.isEmpty()) {
                this.requested.remove(uUID);
                return;
            }
            if (this.plugin.avatars().requestPack(player, false)) {
                this.requested.put(uUID, string);
            }
            return;
        }
        if (string.isEmpty()) {
            player.removeResourcePack(OVERLAY_ID);
            this.requested.remove(uUID);
            this.plugin.packLoadGuard().complete(player, OVERLAY_ID);
            return;
        }
        Pack pack = this.packs.get(ClientProfiles.of(player).key() + ":" + string);
        if (pack == null) {
            return;
        }
        this.plugin.packLoadGuard().expect(player, OVERLAY_ID);
        player.addResourcePack(OVERLAY_ID, pack.url(), pack.hash(), (String)null, false);
        this.requested.put(uUID, string);
    }

    @EventHandler
    public void status(PlayerResourcePackStatusEvent playerResourcePackStatusEvent) {
        if (!PackTransport.id(playerResourcePackStatusEvent, MAIN_PACK_ID).equals(MAIN_PACK_ID)) {
            return;
        }
        switch (playerResourcePackStatusEvent.getStatus()) {
            case SUCCESSFULLY_LOADED: {
                this.loaded.add(playerResourcePackStatusEvent.getPlayer().getUniqueId());
                this.refresh(playerResourcePackStatusEvent.getPlayer());
                break;
            }
            case DECLINED: 
            case FAILED_DOWNLOAD: 
            case FAILED_RELOAD: 
            case INVALID_URL: 
            case DISCARDED: {
                this.loaded.remove(playerResourcePackStatusEvent.getPlayer().getUniqueId());
                this.refresh(playerResourcePackStatusEvent.getPlayer());
                break;
            }
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        UUID uUID = playerQuitEvent.getPlayer().getUniqueId();
        this.loaded.remove(uUID);
        this.requested.remove(uUID);
        this.baseHidden.remove(uUID);
    }

    @Override
    public void close() {
        this.task.cancel();
        HandlerList.unregisterAll((Listener)this);
        for (UUID uUID : this.baseHidden.keySet()) {
            Player player = Bukkit.getPlayer((UUID)uUID);
            if (player == null || !PackTransport.stacked(player)) continue;
            player.removeResourcePack(OVERLAY_ID);
            player.removeResourcePack(MAIN_PACK_ID);
        }
        this.requested.clear();
        this.loaded.clear();
        this.baseHidden.clear();
    }

    private record Pack(String url, byte[] hash) {
    }
}

