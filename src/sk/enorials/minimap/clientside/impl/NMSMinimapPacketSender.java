/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientboundAddEntityPacket
 *  net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
 *  net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
 *  net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
 *  net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.decoration.ItemFrame
 *  net.minecraft.world.level.saveddata.maps.MapId
 *  net.minecraft.world.level.saveddata.maps.MapItemSavedData$MapPatch
 *  org.bukkit.craftbukkit.entity.CraftPlayer
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.clientside.impl;

import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.clientside.AbstractMinimapPacketSender;
import sk.enorials.minimap.injection.PassengerRewriter;
import sk.enorials.minimap.map.MinimapLayer;

public class NMSMinimapPacketSender
extends AbstractMinimapPacketSender {
    private final EnorialsMinimap plugin;

    public NMSMinimapPacketSender(EnorialsMinimap enorialsMinimap) {
        this.plugin = enorialsMinimap;
    }

    private static ServerPlayer client(Player player) {
        return ((CraftPlayer)player).getHandle();
    }

    private static List<ItemFrame> frames(MinimapLayer minimapLayer) {
        return List.of((ItemFrame)minimapLayer.lowerFrame().entity(), (ItemFrame)minimapLayer.upperFrame().entity());
    }

    private static void present(ServerPlayer serverPlayer, ItemFrame itemFrame, double d) {
        itemFrame.setPos(serverPlayer.getX(), serverPlayer.getY() + d, serverPlayer.getZ());
        serverPlayer.connection.send((Packet)new ClientboundAddEntityPacket(itemFrame.getId(), itemFrame.getUUID(), itemFrame.getX(), itemFrame.getY(), itemFrame.getZ(), itemFrame.getXRot(), itemFrame.getYRot(), itemFrame.getType(), itemFrame.getDirection().get3DDataValue(), itemFrame.getDeltaMovement(), (double)itemFrame.getYRot()));
        List list = itemFrame.getEntityData().getNonDefaultValues();
        if (list != null && !list.isEmpty()) {
            serverPlayer.connection.send((Packet)new ClientboundSetEntityDataPacket(itemFrame.getId(), list));
        }
    }

    @Override
    public void updateLayer(Player player, MinimapLayer minimapLayer, int n, int n2, int n3, int n4, byte[] byArray) {
        NMSMinimapPacketSender.client((Player)player).connection.send((Packet)new ClientboundMapItemDataPacket(new MapId(minimapLayer.mapId()), 0, false, List.of(), new MapItemSavedData.MapPatch(n, n2, n3, n4, byArray)));
    }

    @Override
    public void spawnFixedLayer(Player player, MinimapLayer minimapLayer) {
        boolean bl = player.getLocation().getPitch() > -30.0f;
        NMSMinimapPacketSender.present(NMSMinimapPacketSender.client(player), (ItemFrame)(bl ? minimapLayer.upperFrame() : minimapLayer.lowerFrame()).entity(), bl ? 1.0 : 3.0);
    }

    private void mounted(Player player, MinimapLayer minimapLayer, boolean bl) {
        ServerPlayer serverPlayer = NMSMinimapPacketSender.client(player);
        PassengerRewriter passengerRewriter = this.plugin.getPassengerRewriter(player);
        if (!bl) {
            serverPlayer.connection.send((Packet)new ClientboundRemoveEntitiesPacket(NMSMinimapPacketSender.frames(minimapLayer).stream().mapToInt(Entity::getId).toArray()));
        }
        for (ItemFrame itemFrame : NMSMinimapPacketSender.frames(minimapLayer)) {
            if (bl) {
                NMSMinimapPacketSender.present(serverPlayer, itemFrame, 0.0);
            }
            if (passengerRewriter == null) continue;
            if (bl) {
                passengerRewriter.addPassenger(player.getEntityId(), itemFrame.getId());
                continue;
            }
            passengerRewriter.removePassenger(player.getEntityId(), itemFrame.getId());
        }
        serverPlayer.connection.send((Packet)new ClientboundSetPassengersPacket((Entity)serverPlayer));
    }

    @Override
    public void spawnLayer(Player player, MinimapLayer minimapLayer) {
        this.mounted(player, minimapLayer, true);
    }

    @Override
    public void despawnLayer(Player player, MinimapLayer minimapLayer) {
        this.mounted(player, minimapLayer, false);
    }
}

