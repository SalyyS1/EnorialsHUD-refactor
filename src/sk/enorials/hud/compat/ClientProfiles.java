/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.compat;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.compat.ClientRenderProfile;
import sk.enorials.hud.util.StaticPackHosting;

public final class ClientProfiles
implements Listener,
AutoCloseable {
    private static volatile ClientProfiles active;
    private final EnorialsHUD hud;
    private final Map<UUID, ClientRenderProfile> players = new ConcurrentHashMap<UUID, ClientRenderProfile>();
    private final ClientRenderProfile nativeProfile = ClientRenderProfile.nativeProfile();
    private Object via;
    private Method viaVersion;
    private final boolean enabled;
    private final Map<String, ClientRenderProfile> exports = new LinkedHashMap<String, ClientRenderProfile>();

    public ClientProfiles(EnorialsHUD enorialsHUD) {
        Iterator iterator;
        Object object;
        this.hud = enorialsHUD;
        this.enabled = enorialsHUD.cfg().getBoolean("resource-pack.clients.enabled", true);
        this.exports.put(this.nativeProfile.key(), this.nativeProfile);
        if (this.enabled) {
            object = enorialsHUD.cfg().getStringList("resource-pack.clients.prebuild").iterator();
            while (object.hasNext()) {
                iterator = (String)object.next();
                ClientRenderProfile clientRenderProfile = ClientRenderProfile.minecraft((String)((Object)iterator));
                if (this.nativeProfile.legacyTerrain() && !clientRenderProfile.legacyTerrain()) {
                    throw new IllegalArgumentException("Client " + iterator + " needs a server from 1.21.4 for HUD text transport.");
                }
                this.exports.put(clientRenderProfile.key(), clientRenderProfile);
            }
        }
        if ((object = Bukkit.getPluginManager().getPlugin("ViaVersion")) != null && object.isEnabled()) {
            try {
                iterator = object.getClass().getClassLoader();
                this.via = Class.forName("com.viaversion.viaversion.api.Via", true, iterator).getMethod("getAPI", new Class[0]).invoke(null, new Object[0]);
                this.viaVersion = Class.forName("com.viaversion.viaversion.api.ViaAPI", true, iterator).getMethod("getPlayerVersion", UUID.class);
            }
            catch (LinkageError | ReflectiveOperationException throwable) {
                throw new IllegalStateException("Cannot read ViaVersion client protocols", throwable);
            }
        }
        active = this;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
        for (ClientRenderProfile clientRenderProfile : Bukkit.getOnlinePlayers()) {
            this.capture((Player)clientRenderProfile);
        }
        enorialsHUD.getLogger().info("HUD client profiles: " + (String)(this.via == null ? "native " + this.nativeProfile.minecraft() : (this.enabled ? "per-player ViaVersion protocols" : "translated clients disabled by configuration")));
    }

    public static ClientRenderProfile of(Player player) {
        ClientProfiles clientProfiles = active;
        return clientProfiles == null ? ClientRenderProfile.nativeProfile() : clientProfiles.players.getOrDefault(player.getUniqueId(), clientProfiles.nativeProfile);
    }

    public static boolean legacy(Player player) {
        return ClientProfiles.of(player).legacyTerrain();
    }

    public boolean supports(Player player) {
        ClientRenderProfile clientRenderProfile = ClientProfiles.of(player);
        return !(!clientRenderProfile.supported() || !this.enabled && !clientRenderProfile.sameResources(this.nativeProfile) || StaticPackHosting.enabled(this.hud) && !this.exports.containsKey(clientRenderProfile.key()) || this.nativeProfile.legacyTerrain() && !clientRenderProfile.legacyTerrain());
    }

    public List<ClientRenderProfile> exportProfiles() {
        return List.copyOf(this.exports.values());
    }

    public void describe(CommandSender commandSender) {
        commandSender.sendMessage("HUD native profile: " + this.nativeProfile.minecraft() + " / " + this.nativeProfile.key() + ". ViaVersion: " + (this.via != null) + ". Per-client profiles: " + this.enabled + ".");
        TreeMap<String, Integer> treeMap = new TreeMap<String, Integer>();
        this.players.values().forEach(clientRenderProfile -> treeMap.merge(clientRenderProfile.minecraft() + " / " + clientRenderProfile.key(), 1, Integer::sum));
        treeMap.forEach((string, n) -> commandSender.sendMessage("  " + string + ": " + n + " connected"));
        if (this.via == null) {
            commandSender.sendMessage("Install released ViaVersion (and ViaBackwards for older clients) on this server to detect translated clients. Proxy-only ViaVersion does not expose their profiles here.");
        }
    }

    public boolean usable(Player player) {
        ClientRenderProfile clientRenderProfile = ClientProfiles.of(player);
        if (this.supports(player)) {
            return true;
        }
        player.sendMessage("EnorialsHUD is not configured for this Minecraft client (protocol " + clientRenderProfile.protocol() + "). Ask the administrator to check /enohud hud packcheck.");
        this.hud.getLogger().warning("Unsupported HUD connection: " + clientRenderProfile.minecraft() + " / " + clientRenderProfile.key() + ". Check clients.enabled; static hosting also needs clients.prebuild and an uploaded export. Clients from 1.21.4 need a server from 1.21.4 for HUD text transport.");
        return false;
    }

    private void capture(Player player) {
        ClientRenderProfile clientRenderProfile = this.nativeProfile;
        if (this.via != null) {
            try {
                clientRenderProfile = ClientRenderProfile.protocol(((Number)this.viaVersion.invoke(this.via, player.getUniqueId())).intValue());
            }
            catch (ReflectiveOperationException | RuntimeException exception) {
                clientRenderProfile = ClientRenderProfile.protocol(-1);
                this.hud.getLogger().warning("Could not determine a joining player's ViaVersion protocol; no native pack will be guessed.");
            }
        }
        this.players.put(player.getUniqueId(), clientRenderProfile);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void join(PlayerJoinEvent playerJoinEvent) {
        this.capture(playerJoinEvent.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void quit(PlayerQuitEvent playerQuitEvent) {
        this.players.remove(playerQuitEvent.getPlayer().getUniqueId());
    }

    @Override
    public void close() {
        HandlerList.unregisterAll((Listener)this);
        this.players.clear();
        if (active == this) {
            active = null;
        }
    }
}

