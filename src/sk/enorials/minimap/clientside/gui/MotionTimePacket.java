/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.protocol.game.ClientboundSetTimePacket
 */
package sk.enorials.minimap.clientside.gui;

import java.util.Map;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import sk.enorials.minimap.clientside.gui.MotionClockState;

final class MotionTimePacket {
    MotionTimePacket() {
    }

    static MotionClockState read(Object object) {
        if (!(object instanceof ClientboundSetTimePacket)) {
            return null;
        }
        ClientboundSetTimePacket clientboundSetTimePacket = (ClientboundSetTimePacket)object;
        long l = clientboundSetTimePacket.getDayTime();
        return new MotionClockState(clientboundSetTimePacket.getGameTime(), Map.of("day", new MotionClockState.Clock(Math.abs((double)l), l < 0L ? 0.0 : 1.0)));
    }
}

