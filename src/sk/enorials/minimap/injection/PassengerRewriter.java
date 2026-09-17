/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.ChannelOutboundHandlerAdapter
 *  io.netty.channel.ChannelPromise
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMaps
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntArrays
 *  it.unimi.dsi.fastutil.ints.IntList
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
 */
package sk.enorials.minimap.injection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;

public class PassengerRewriter
extends ChannelOutboundHandlerAdapter {
    private final Int2ObjectMap<IntList> passengers = Int2ObjectMaps.synchronize((Int2ObjectMap)new Int2ObjectOpenHashMap());

    public Int2ObjectMap<IntList> passengers() {
        return this.passengers;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addPassenger(int n2, int n3) {
        Int2ObjectMap<IntList> int2ObjectMap = this.passengers;
        synchronized (int2ObjectMap) {
            IntList intList = (IntList)this.passengers.computeIfAbsent(n2, n -> new IntArrayList());
            if (!intList.contains(n3)) {
                intList.add(n3);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removePassenger(int n, int n2) {
        Int2ObjectMap<IntList> int2ObjectMap = this.passengers;
        synchronized (int2ObjectMap) {
            IntList intList = (IntList)this.passengers.get(n);
            if (intList == null) {
                return;
            }
            intList.rem(n2);
            if (intList.isEmpty()) {
                this.passengers.remove(n);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void write(ChannelHandlerContext channelHandlerContext, Object object, ChannelPromise channelPromise) throws Exception {
        int[] nArray;
        if (!(object instanceof ClientboundSetPassengersPacket)) {
            channelHandlerContext.write(object, channelPromise);
            return;
        }
        ClientboundSetPassengersPacket clientboundSetPassengersPacket = (ClientboundSetPassengersPacket)object;
        IntArrayList intArrayList = this.passengers;
        synchronized (intArrayList) {
            IntList object2 = (IntList)this.passengers.get(clientboundSetPassengersPacket.getVehicle());
            nArray = object2 == null ? IntArrays.EMPTY_ARRAY : object2.toIntArray();
        }
        if (nArray.length == 0) {
            channelHandlerContext.write(object, channelPromise);
            return;
        }
        intArrayList = new IntArrayList(clientboundSetPassengersPacket.getPassengers());
        for (int n : nArray) {
            if (intArrayList.contains(n)) continue;
            intArrayList.add(n);
        }
        if (intArrayList.size() == clientboundSetPassengersPacket.getPassengers().length) {
            channelHandlerContext.write(object, channelPromise);
            return;
        }
        FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(channelHandlerContext.alloc().buffer());
        try {
            friendlyByteBuf.writeVarInt(clientboundSetPassengersPacket.getVehicle());
            friendlyByteBuf.writeVarIntArray(intArrayList.toIntArray());
            channelHandlerContext.write(ClientboundSetPassengersPacket.STREAM_CODEC.decode((Object)friendlyByteBuf), channelPromise);
        }
        finally {
            friendlyByteBuf.release();
        }
    }
}

