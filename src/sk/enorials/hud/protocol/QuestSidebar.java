/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.channel.Channel
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.ChannelOutboundHandlerAdapter
 *  io.netty.channel.ChannelPromise
 *  net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
 *  net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
 *  net.minecraft.world.scores.DisplaySlot
 *  net.minecraft.world.scores.Scoreboard
 *  org.bukkit.Bukkit
 *  org.bukkit.craftbukkit.entity.CraftPlayer
 *  org.bukkit.craftbukkit.scoreboard.CraftScoreboard
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.hud.protocol;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Scoreboard;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.scoreboard.CraftScoreboard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class QuestSidebar
implements Listener,
AutoCloseable {
    private static final String NAME = "enorials_quest_sidebar";
    private final Map<UUID, Gate> gates = new ConcurrentHashMap<UUID, Gate>();
    private volatile boolean closed;

    private static boolean sidebar(DisplaySlot displaySlot) {
        return displaySlot != DisplaySlot.LIST && displaySlot != DisplaySlot.BELOW_NAME;
    }

    public QuestSidebar(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents((Listener)this, plugin);
        Bukkit.getOnlinePlayers().forEach(this::attach);
    }

    private void attach(Player player) {
        Channel channel = ((CraftPlayer)player).getHandle().connection.connection.channel;
        Gate gate = new Gate(channel);
        this.gates.put(player.getUniqueId(), gate);
        Scoreboard scoreboard = ((CraftScoreboard)player.getScoreboard()).getHandle();
        for (DisplaySlot displaySlot : DisplaySlot.values()) {
            if (!QuestSidebar.sidebar(displaySlot)) continue;
            gate.latest.put(displaySlot, new ClientboundSetDisplayObjectivePacket(displaySlot, scoreboard.getDisplayObjective(displaySlot)));
        }
        channel.eventLoop().execute(() -> {
            if (!this.closed && channel.pipeline().get(NAME) == null) {
                channel.pipeline().addBefore("packet_handler", NAME, (ChannelHandler)gate);
            }
        });
    }

    public void hide(Player player, boolean bl) {
        Gate gate = this.gates.get(player.getUniqueId());
        if (gate != null) {
            gate.channel.eventLoop().execute(() -> gate.hide(bl));
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void join(PlayerJoinEvent playerJoinEvent) {
        this.attach(playerJoinEvent.getPlayer());
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        this.detach(playerQuitEvent.getPlayer().getUniqueId(), false);
    }

    private void detach(UUID uUID, boolean bl) {
        Gate gate = this.gates.remove(uUID);
        if (gate != null) {
            gate.channel.eventLoop().execute(() -> {
                if (bl) {
                    gate.hide(false);
                }
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
            this.detach(uUID, true);
        }
    }

    private final class Gate
    extends ChannelOutboundHandlerAdapter {
        final Channel channel;
        final Map<DisplaySlot, ClientboundSetDisplayObjectivePacket> latest = new EnumMap<DisplaySlot, ClientboundSetDisplayObjectivePacket>(DisplaySlot.class);
        boolean hidden;

        Gate(Channel channel) {
            this.channel = channel;
        }

        public void write(ChannelHandlerContext channelHandlerContext, Object object, ChannelPromise channelPromise) throws Exception {
            ClientboundSetObjectivePacket clientboundSetObjectivePacket;
            ClientboundSetDisplayObjectivePacket clientboundSetDisplayObjectivePacket;
            if (object instanceof ClientboundSetDisplayObjectivePacket && QuestSidebar.sidebar((clientboundSetDisplayObjectivePacket = (ClientboundSetDisplayObjectivePacket)object).getSlot())) {
                this.latest.put(clientboundSetDisplayObjectivePacket.getSlot(), clientboundSetDisplayObjectivePacket);
                if (this.hidden) {
                    object = new ClientboundSetDisplayObjectivePacket(clientboundSetDisplayObjectivePacket.getSlot(), null);
                }
            } else if (object instanceof ClientboundSetObjectivePacket && (clientboundSetObjectivePacket = (ClientboundSetObjectivePacket)object).getMethod() == 1) {
                this.latest.entrySet().removeIf(entry -> Objects.equals(((ClientboundSetDisplayObjectivePacket)entry.getValue()).getObjectiveName(), clientboundSetObjectivePacket.getObjectiveName()));
            }
            super.write(channelHandlerContext, object, channelPromise);
        }

        void hide(boolean bl) {
            if (this.hidden == bl) {
                return;
            }
            this.hidden = bl;
            ChannelHandlerContext channelHandlerContext = this.channel.pipeline().context((ChannelHandler)this);
            if (channelHandlerContext == null) {
                return;
            }
            if (bl) {
                for (DisplaySlot displaySlot : DisplaySlot.values()) {
                    if (!QuestSidebar.sidebar(displaySlot)) continue;
                    channelHandlerContext.write((Object)new ClientboundSetDisplayObjectivePacket(displaySlot, null));
                }
            } else {
                for (ClientboundSetDisplayObjectivePacket clientboundSetDisplayObjectivePacket : this.latest.values()) {
                    channelHandlerContext.write((Object)clientboundSetDisplayObjectivePacket);
                }
            }
            channelHandlerContext.flush();
        }
    }
}

