/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.clientside;

import org.bukkit.entity.Player;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;

public interface MinimapPacketSender {
    public void updateLayer(Player var1, MinimapLayer var2, int var3, int var4, int var5, int var6, byte[] var7);

    public void spawnFixedLayer(Player var1, MinimapLayer var2);

    public void spawnLayer(Player var1, MinimapLayer var2);

    public void spawnMinimap(Minimap var1);

    public void despawnLayer(Player var1, MinimapLayer var2);

    public void despawnMinimap(Minimap var1);
}

