/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.channel.Channel
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.ChannelOutboundHandlerAdapter
 *  io.netty.channel.ChannelPromise
 *  net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
 *  net.minecraft.world.effect.MobEffectInstance
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.craftbukkit.entity.CraftPlayer
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.hud.protocol;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffectInstance;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;

public final class VanillaEffectIcons
implements Listener,
AutoCloseable {
    private static final String HANDLER = "enorials_effect_icons";
    private final Map<UUID, Channel> channels = new ConcurrentHashMap<UUID, Channel>();
    private volatile boolean closed;
    private final Map<UUID, Boolean> hidden = new ConcurrentHashMap<UUID, Boolean>();
    private BukkitTask task;

    private VanillaEffectIcons(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents((Listener)this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.attach(player);
            VanillaEffectIcons.refresh(player);
        }
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            EnorialsHUD enorialsHUD = (EnorialsHUD)plugin;
            for (Player player : Bukkit.getOnlinePlayers()) {
                boolean bl = HudAccess.hud(player) && HudAccess.has((CommandSender)player, "enorials.hud.effects") && enorialsHUD.storage().preference(player.getUniqueId(), "hud", true) && enorialsHUD.storage().preference(player.getUniqueId(), "effects", true);
                Boolean bl2 = this.hidden.put(player.getUniqueId(), bl);
                if (bl2 != null && bl2 == bl) continue;
                VanillaEffectIcons.refresh(player);
            }
        }, 1L, 5L);
    }

    public static VanillaEffectIcons start(Plugin plugin) {
        return new VanillaEffectIcons(plugin);
    }

    private void attach(final Player player) {
        Channel channel = ((CraftPlayer)player).getHandle().connection.connection.channel;
        final int n = player.getEntityId();
        this.channels.put(player.getUniqueId(), channel);
        channel.eventLoop().execute(() -> {
            if (this.closed || channel.pipeline().get(HANDLER) != null) {
                return;
            }
            channel.pipeline().addBefore("packet_handler", HANDLER, (ChannelHandler)new ChannelOutboundHandlerAdapter(this){
                final /* synthetic */ VanillaEffectIcons this$0;
                {
                    this.this$0 = vanillaEffectIcons;
                }

                public void write(ChannelHandlerContext channelHandlerContext, Object object, ChannelPromise channelPromise) throws Exception {
                    ClientboundUpdateMobEffectPacket clientboundUpdateMobEffectPacket;
                    if (object instanceof ClientboundUpdateMobEffectPacket && (clientboundUpdateMobEffectPacket = (ClientboundUpdateMobEffectPacket)object).getEntityId() == n && this.this$0.hidden.getOrDefault(player.getUniqueId(), false).booleanValue() && clientboundUpdateMobEffectPacket.effectShowsIcon()) {
                        MobEffectInstance mobEffectInstance = new MobEffectInstance(clientboundUpdateMobEffectPacket.getEffect(), clientboundUpdateMobEffectPacket.getEffectDurationTicks(), clientboundUpdateMobEffectPacket.getEffectAmplifier(), clientboundUpdateMobEffectPacket.isEffectAmbient(), clientboundUpdateMobEffectPacket.isEffectVisible(), false);
                        object = new ClientboundUpdateMobEffectPacket(n, mobEffectInstance, clientboundUpdateMobEffectPacket.shouldBlend());
                    }
                    super.write(channelHandlerContext, object, channelPromise);
                }
            });
        });
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void join(PlayerJoinEvent playerJoinEvent) {
        this.attach(playerJoinEvent.getPlayer());
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        this.hidden.remove(playerQuitEvent.getPlayer().getUniqueId());
        this.detach(playerQuitEvent.getPlayer().getUniqueId());
    }

    private void detach(UUID uUID) {
        Channel channel = this.channels.remove(uUID);
        if (channel != null) {
            channel.eventLoop().execute(() -> {
                if (channel.pipeline().get(HANDLER) != null) {
                    channel.pipeline().remove(HANDLER);
                }
            });
        }
    }

    private static void refresh(Player player) {
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.sendPotionEffectChange((LivingEntity)player, potionEffect);
        }
    }

    @Override
    public void close() {
        this.closed = true;
        if (this.task != null) {
            this.task.cancel();
        }
        this.hidden.clear();
        HandlerList.unregisterAll((Listener)this);
        for (UUID uUID : List.copyOf(this.channels.keySet())) {
            this.detach(uUID);
        }
        for (UUID uUID : Bukkit.getOnlinePlayers()) {
            VanillaEffectIcons.refresh((Player)uUID);
        }
    }
}

