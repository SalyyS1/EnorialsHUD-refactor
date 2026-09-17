/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.channel.Channel
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.ChannelOutboundHandlerAdapter
 *  io.netty.channel.ChannelPromise
 *  io.netty.util.ReferenceCountUtil
 *  org.bukkit.Bukkit
 *  org.bukkit.craftbukkit.entity.CraftPlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerChangedWorldEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.minimap.clientside.gui;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import sk.enorials.minimap.clientside.gui.MotionClockState;
import sk.enorials.minimap.clientside.gui.MotionTimePacket;

final class MinimapMotionClock
implements Listener,
AutoCloseable {
    private static final String NAME = "enorials_minimap_motion_clock";
    private final Map<UUID, Gate> gates = new ConcurrentHashMap<UUID, Gate>();
    private volatile boolean closed;

    MinimapMotionClock(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents((Listener)this, plugin);
        Bukkit.getOnlinePlayers().forEach(this::attach);
    }

    private void attach(Player player) {
        Channel channel = ((CraftPlayer)player).getHandle().connection.connection.channel;
        Gate gate = new Gate(channel);
        Gate gate2 = this.gates.putIfAbsent(player.getUniqueId(), gate);
        if (gate2 != null) {
            return;
        }
        channel.eventLoop().execute(() -> {
            if (!this.closed && channel.pipeline().get(NAME) == null) {
                channel.pipeline().addBefore("packet_handler", NAME, (ChannelHandler)gate);
            }
        });
    }

    void sample(Player player, boolean bl, boolean bl2) {
        Gate gate = this.gates.get(player.getUniqueId());
        if (gate == null) {
            return;
        }
        if (!bl) {
            gate.movingUntil = 0L;
        } else if (bl2) {
            gate.movingUntil = System.nanoTime() + 250000000L;
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void join(PlayerJoinEvent playerJoinEvent) {
        this.attach(playerJoinEvent.getPlayer());
    }

    @EventHandler
    public void world(PlayerChangedWorldEvent playerChangedWorldEvent) {
        Gate gate = this.gates.get(playerChangedWorldEvent.getPlayer().getUniqueId());
        if (gate != null) {
            gate.movingUntil = 0L;
            gate.channel.eventLoop().execute(() -> {
                gate.previous = null;
            });
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        this.detach(playerQuitEvent.getPlayer().getUniqueId());
    }

    private void detach(UUID uUID) {
        Gate gate = this.gates.remove(uUID);
        if (gate != null) {
            gate.movingUntil = 0L;
            gate.channel.eventLoop().execute(() -> {
                if (gate.channel.pipeline().get(NAME) == gate) {
                    gate.channel.pipeline().remove((ChannelHandler)gate);
                }
            });
        }
    }

    @Override
    public void close() {
        this.closed = true;
        HandlerList.unregisterAll((Listener)this);
        for (UUID uUID : List.copyOf(this.gates.keySet())) {
            this.detach(uUID);
        }
    }

    private static final class Gate
    extends ChannelOutboundHandlerAdapter {
        final Channel channel;
        volatile long movingUntil;
        MotionClockState previous;
        long previousNanos;

        Gate(Channel channel) {
            this.channel = channel;
        }

        public void write(ChannelHandlerContext channelHandlerContext, Object object, ChannelPromise channelPromise) throws Exception {
            MotionClockState motionClockState = MotionTimePacket.read(object);
            if (motionClockState != null) {
                long l = System.nanoTime();
                boolean bl = motionClockState.continues(this.previous) && Math.abs((double)(l - this.previousNanos) / 5.0E7 - (double)(motionClockState.age() - this.previous.age())) < 2.0;
                this.previous = motionClockState;
                this.previousNanos = l;
                if (bl && l < this.movingUntil) {
                    ReferenceCountUtil.release((Object)object);
                    channelPromise.trySuccess();
                    return;
                }
            }
            super.write(channelHandlerContext, object, channelPromise);
        }
    }
}

