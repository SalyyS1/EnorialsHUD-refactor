/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.channel.Channel
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.ChannelOutboundHandlerAdapter
 *  io.netty.channel.ChannelPromise
 *  net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
 *  net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
 *  net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
 *  net.minecraft.world.item.ItemStack
 *  org.bukkit.Bukkit
 *  org.bukkit.craftbukkit.entity.CraftPlayer
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.compat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import sk.enorials.hud.compat.MenuPackets;

public final class SocialInventoryMask
implements AutoCloseable {
    private static final String HANDLER = "enorials_social_inventory";
    private final Map<UUID, Mask> masks = new ConcurrentHashMap<UUID, Mask>();
    private final Map<UUID, Channel> channels = new ConcurrentHashMap<UUID, Channel>();

    public void hide(Player player, int n) {
        Channel channel;
        final UUID uUID = player.getUniqueId();
        if (this.channels.putIfAbsent(uUID, channel = ((CraftPlayer)player).getHandle().connection.connection.channel) == null) {
            channel.pipeline().addBefore("packet_handler", HANDLER, (ChannelHandler)new ChannelOutboundHandlerAdapter(this){
                final /* synthetic */ SocialInventoryMask this$0;
                {
                    this.this$0 = socialInventoryMask;
                }

                public void write(ChannelHandlerContext channelHandlerContext, Object object, ChannelPromise channelPromise) throws Exception {
                    Mask mask = this.this$0.masks.get(uUID);
                    if (mask != null) {
                        ClientboundContainerSetSlotPacket clientboundContainerSetSlotPacket;
                        ClientboundContainerSetContentPacket clientboundContainerSetContentPacket;
                        if (object instanceof ClientboundOpenScreenPacket) {
                            ClientboundOpenScreenPacket clientboundOpenScreenPacket = (ClientboundOpenScreenPacket)object;
                            mask.container = clientboundOpenScreenPacket.getContainerId();
                        } else if (object instanceof ClientboundContainerSetContentPacket && (MenuPackets.id(clientboundContainerSetContentPacket = (ClientboundContainerSetContentPacket)object) == mask.container || MenuPackets.id(clientboundContainerSetContentPacket) == 0)) {
                            int n;
                            ArrayList<ItemStack> arrayList = new ArrayList<ItemStack>(MenuPackets.items(clientboundContainerSetContentPacket));
                            int n2 = n = MenuPackets.id(clientboundContainerSetContentPacket) == 0 ? 0 : mask.top;
                            while (n < arrayList.size()) {
                                arrayList.set(n, ItemStack.EMPTY);
                                ++n;
                            }
                            object = MenuPackets.content(MenuPackets.id(clientboundContainerSetContentPacket), MenuPackets.state(clientboundContainerSetContentPacket), arrayList);
                        } else if (object instanceof ClientboundContainerSetSlotPacket && ((clientboundContainerSetSlotPacket = (ClientboundContainerSetSlotPacket)object).getContainerId() == mask.container && clientboundContainerSetSlotPacket.getSlot() >= mask.top || clientboundContainerSetSlotPacket.getContainerId() == 0 || clientboundContainerSetSlotPacket.getContainerId() == -2)) {
                            object = new ClientboundContainerSetSlotPacket(clientboundContainerSetSlotPacket.getContainerId(), clientboundContainerSetSlotPacket.getStateId(), clientboundContainerSetSlotPacket.getSlot(), ItemStack.EMPTY);
                        }
                    }
                    super.write(channelHandlerContext, object, channelPromise);
                }
            });
        }
        this.masks.put(uUID, new Mask(n));
    }

    public void restore(Player player) {
        this.masks.remove(player.getUniqueId());
        if (player.isOnline()) {
            player.updateInventory();
        }
    }

    public void disconnect(UUID uUID) {
        this.masks.remove(uUID);
        Channel channel = this.channels.remove(uUID);
        if (channel != null) {
            channel.eventLoop().execute(() -> {
                if (channel.pipeline().get(HANDLER) != null) {
                    channel.pipeline().remove(HANDLER);
                }
            });
        }
    }

    @Override
    public void close() {
        for (UUID uUID : List.copyOf(this.masks.keySet())) {
            Player player = Bukkit.getPlayer((UUID)uUID);
            if (player == null) continue;
            this.restore(player);
        }
        for (UUID uUID : List.copyOf(this.channels.keySet())) {
            this.disconnect(uUID);
        }
    }

    private static final class Mask {
        final int top;
        volatile int container = -1;

        Mask(int n) {
            this.top = n;
        }
    }
}

