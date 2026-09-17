/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.minimap.listener;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.Plugin;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.config.Config;

public class VersionedResourcePackDispatcher
implements Listener {
    private final EnorialsMinimap plugin;
    private final Map<UUID, String> versionKey = new ConcurrentHashMap<UUID, String>();
    private final Method packWithPrompt;
    private final Method packWithForce;
    private final Method packWithHash;
    private final Method packSimple;

    public VersionedResourcePackDispatcher(EnorialsMinimap enorialsMinimap) {
        this.plugin = enorialsMinimap;
        this.packWithPrompt = VersionedResourcePackDispatcher.resolveMethod("setResourcePack", String.class, byte[].class, Component.class, Boolean.TYPE);
        this.packWithForce = VersionedResourcePackDispatcher.resolveMethod("setResourcePack", String.class, byte[].class, Boolean.TYPE);
        this.packWithHash = VersionedResourcePackDispatcher.resolveMethod("setResourcePack", String.class, byte[].class);
        this.packSimple = VersionedResourcePackDispatcher.resolveMethod("setResourcePack", String.class);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        if (this.enabled()) {
            Player player = playerJoinEvent.getPlayer();
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin.owner(), () -> this.dispatch(player), 10L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        this.versionKey.remove(playerQuitEvent.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent playerResourcePackStatusEvent) {
        if (this.enabled()) {
            switch (playerResourcePackStatusEvent.getStatus()) {
                case DECLINED: 
                case FAILED_DOWNLOAD: {
                    if (this.required()) {
                        playerResourcePackStatusEvent.getPlayer().kickPlayer(VersionedResourcePackDispatcher.color(Config.instance().resourcePack.kickMessage));
                    } else {
                        this.restoreMinimap(playerResourcePackStatusEvent.getPlayer());
                    }
                    this.versionKey.remove(playerResourcePackStatusEvent.getPlayer().getUniqueId());
                    break;
                }
                case SUCCESSFULLY_LOADED: {
                    this.versionKey.remove(playerResourcePackStatusEvent.getPlayer().getUniqueId());
                    this.restoreMinimap(playerResourcePackStatusEvent.getPlayer());
                }
            }
        }
    }

    private void dispatch(Player player) {
        if (player.isOnline()) {
            String string = this.resolveVersionKey(player);
            String string2 = this.resolveUrl(string);
            if (string2 != null && !string2.isBlank()) {
                this.versionKey.put(player.getUniqueId(), string);
                this.sendResourcePack(player, string2, this.required(), Config.instance().resourcePack.prompt);
            } else {
                this.plugin.getLogger().warning("Resource-pack URL is empty for version key '" + string + "'. Minimap shaders will not be applied.");
                this.restoreMinimap(player);
            }
        }
    }

    private static Method resolveMethod(String string, Class<?> ... classArray) {
        try {
            return Player.class.getMethod(string, classArray);
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    private boolean enabled() {
        return Config.instance().resourcePack.enabled;
    }

    private boolean required() {
        return Config.instance().resourcePack.required;
    }

    private String resolveUrl(String string) {
        Config.ResourcePack.Urls urls = Config.instance().resourcePack.url;
        return switch (string) {
            case "1_21" -> urls.v1_21;
            case "1_20" -> urls.v1_20;
            default -> urls.v1_19;
        };
    }

    private String resolveVersionKey(Player player) {
        String string = this.getClientVersionName(player);
        if (string == null) {
            return this.serverVersionFallback();
        }
        String string2 = string.toLowerCase(Locale.ROOT);
        if (string2.startsWith("1.21")) {
            return "1_21";
        }
        return string2.startsWith("1.20") ? "1_20" : "1_19";
    }

    private String serverVersionFallback() {
        try {
            String string = this.plugin.getServer().getMinecraftVersion();
            if (string != null) {
                String string2 = string.toLowerCase(Locale.ROOT);
                if (string2.startsWith("1.21")) {
                    return "1_21";
                }
                if (string2.startsWith("1.20")) {
                    return "1_20";
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return "1_19";
    }

    private String getClientVersionName(Player player) {
        try {
            Class<?> clazz = Class.forName("com.viaversion.viaversion.api.Via");
            Object object = clazz.getMethod("getAPI", new Class[0]).invoke(null, new Object[0]);
            Class<?> clazz2 = object.getClass().getMethod("getPlayerVersion", UUID.class).invoke(object, player.getUniqueId());
            if (clazz2 instanceof Integer) {
                String string;
                Integer n = (Integer)((Object)clazz2);
                clazz2 = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
                Object object2 = clazz2.getMethod("getProtocol", Integer.TYPE).invoke(null, n);
                if (object2 == null) {
                    return null;
                }
                Object object3 = object2.getClass().getMethod("getName", new Class[0]).invoke(object2, new Object[0]);
                return object3 instanceof String ? (string = (String)object3) : null;
            }
            return null;
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    private void sendResourcePack(Player player, String string, boolean bl, String string2) {
        if (this.packWithPrompt != null) {
            try {
                TextComponent textComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(string2 == null ? "" : string2);
                this.packWithPrompt.invoke((Object)player, string, null, textComponent, bl);
                return;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        if (this.packWithForce != null) {
            try {
                this.packWithForce.invoke((Object)player, string, null, bl);
                return;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        if (this.packWithHash != null) {
            try {
                this.packWithHash.invoke((Object)player, string, null);
                return;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        if (this.packSimple != null) {
            try {
                this.packSimple.invoke((Object)player, string);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    private void restoreMinimap(Player player) {
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin.owner(), () -> {
            if (player.isOnline() && this.plugin.getPlayerMinimap(player) == null) {
                try {
                    this.plugin.playerDataStorage().restore(this.plugin, player);
                }
                catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
        });
    }

    private static String color(String string) {
        return string == null ? "" : string.replace('&', '\u00a7');
    }
}

