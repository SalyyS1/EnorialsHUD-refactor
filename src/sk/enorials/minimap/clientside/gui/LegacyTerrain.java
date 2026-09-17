/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.clientside.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sk.enorials.common.pack.ResourcePackGuard;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.clientside.gui.GuiMinimapPacketSender;
import sk.enorials.minimap.clientside.gui.GuiMinimapWire;
import sk.enorials.minimap.clientside.gui.LegacyMotion;
import sk.enorials.minimap.clientside.gui.LegacyPlayerPose;
import sk.enorials.minimap.clientside.impl.NMSClientsideMinimapFactory;
import sk.enorials.minimap.clientside.impl.NMSMinimapPacketSender;
import sk.enorials.minimap.map.MinimapLayer;

final class LegacyTerrain
implements AutoCloseable {
    static final int GUTTER = 11;
    static final int TILE = 72;
    static final int PIXELS = 94;
    static final int ROWS = 102;
    private final NMSClientsideMinimapFactory factory = new NMSClientsideMinimapFactory();
    private final NMSMinimapPacketSender packets;
    private final Map<UUID, Tiles> viewers = new HashMap<UUID, Tiles>();

    LegacyTerrain(EnorialsMinimap enorialsMinimap) {
        this.packets = new NMSMinimapPacketSender(enorialsMinimap);
    }

    /*
     * WARNING - void declaration
     */
    void render(Player player, GuiMinimapPacketSender.Session session) {
        boolean bl;
        GuiMinimapPacketSender.LayerState layerState = null;
        if (!ResourcePackGuard.isLoading(player) && GuiMinimapPacketSender.fullscreenBackground(session) == null) {
            for (GuiMinimapPacketSender.LayerState object2 : session.layers.values()) {
                if (!object2.visible || object2.raw == null || object2.raw.length != 20736 || GuiMinimapWire.unsigned(object2.metadata, 105, 0) != 10) continue;
                layerState = object2;
                break;
            }
        }
        if (layerState == null) {
            this.forget(player);
            return;
        }
        Object object3 = this.viewers.get(player.getUniqueId());
        if (object3 != null && !((Tiles)object3).world.equals(player.getWorld().getUID())) {
            this.forget(player);
            object3 = null;
        }
        if (object3 == null) {
            void var5_7;
            object3 = new Tiles(player.getWorld().getUID());
            boolean i = false;
            while (var5_7 < 4) {
                ((Tiles)object3).layers[var5_7] = this.factory.createMinimapLayer(player.getWorld(), (minimap, minimapLayer, byArray) -> {});
                ++var5_7;
            }
            this.viewers.put(player.getUniqueId(), (Tiles)object3);
        }
        GuiMinimapWire.TerrainPose terrainPose = GuiMinimapWire.terrainPose(session.pose.x, session.pose.z, layerState.originX, layerState.originZ, session.pose.dx, session.pose.dz, session.pose.tick, layerState.metadata);
        int n = GuiMinimapWire.variant(layerState.metadata);
        int n2 = Math.min(5, GuiMinimapWire.unsigned(layerState.metadata, 41, 0));
        boolean bl2 = bl = ((Tiles)object3).raw != layerState.raw;
        if (!bl && ((Tiles)object3).poseKey == terrainPose.key() && ((Tiles)object3).variant == n && ((Tiles)object3).frame == n2 && ((Tiles)object3).x == session.pose.x() && ((Tiles)object3).z == session.pose.z() && ((Tiles)object3).previousX == session.pose.previousX() && ((Tiles)object3).previousZ == session.pose.previousZ()) {
            return;
        }
        for (int i = 0; i < 4; ++i) {
            byte[] byArray2 = new byte[bl ? 13056 : 768];
            LegacyTerrain.write(byArray2, 0, 69);
            LegacyTerrain.write(byArray2, 8, 78);
            LegacyTerrain.write(byArray2, 16, 79);
            LegacyTerrain.write(byArray2, 24, 55);
            LegacyTerrain.write(byArray2, 32, n2);
            LegacyTerrain.write(byArray2, 128, i);
            LegacyTerrain.write(byArray2, 136, n);
            int n3 = terrainPose.shadowArgb() >>> 1;
            for (int j = 0; j < 4; ++j) {
                LegacyTerrain.write(byArray2, 144 + j * 8, n3 >>> j * 8);
            }
            LegacyTerrain.write(byArray2, 176, terrainPose.alias());
            LegacyTerrain.write(byArray2, 184, terrainPose.alias() >>> 8);
            if (session.pose instanceof LegacyPlayerPose) {
                LegacyMotion.write(byArray2, session.pose.x() - (double)layerState.originX, session.pose.z() - (double)layerState.originZ, session.pose.previousX() - (double)layerState.originX, session.pose.previousZ() - (double)layerState.originZ, session.pose.tick);
            }
            if (bl) {
                LegacyTerrain.copyTile(layerState.raw, i, byArray2);
            }
            this.packets.updateLayer(player, ((Tiles)object3).layers[i], 0, 0, 128, bl ? 102 : 6, byArray2);
            if (((Tiles)object3).raw != null) continue;
            this.packets.spawnLayer(player, ((Tiles)object3).layers[i]);
        }
        ((Tiles)object3).raw = layerState.raw;
        ((Tiles)object3).poseKey = terrainPose.key();
        ((Tiles)object3).variant = n;
        ((Tiles)object3).frame = n2;
        ((Tiles)object3).x = session.pose.x();
        ((Tiles)object3).z = session.pose.z();
        ((Tiles)object3).previousX = session.pose.previousX();
        ((Tiles)object3).previousZ = session.pose.previousZ();
    }

    static void copyTile(byte[] byArray, int n, byte[] byArray2) {
        for (int i = 0; i < 94; ++i) {
            for (int j = 0; j < 94; ++j) {
                int n2 = Math.max(0, Math.min(143, n % 2 * 72 + j - 11));
                int n3 = Math.max(0, Math.min(143, n / 2 * 72 + i - 11));
                byArray2[(i + 8) * 128 + j] = byArray[n3 * 144 + n2];
            }
        }
    }

    private static void write(byte[] byArray, int n, int n2) {
        for (int i = 0; i < 8; ++i) {
            byArray[n + i] = (n2 & 1 << i) == 0 ? 0 : 4;
        }
    }

    void forget(Player player) {
        Tiles tiles = this.viewers.remove(player.getUniqueId());
        if (tiles != null) {
            for (MinimapLayer minimapLayer : tiles.layers) {
                this.packets.despawnLayer(player, minimapLayer);
            }
        }
    }

    void relocate(Player player) {
        Tiles tiles = this.viewers.get(player.getUniqueId());
        if (tiles == null) {
            return;
        }
        if (!tiles.world.equals(player.getWorld().getUID())) {
            this.forget(player);
            return;
        }
        for (MinimapLayer minimapLayer : tiles.layers) {
            this.packets.despawnLayer(player, minimapLayer);
            this.packets.spawnLayer(player, minimapLayer);
        }
    }

    @Override
    public void close() {
        for (UUID uUID : List.copyOf(this.viewers.keySet())) {
            Player player = Bukkit.getPlayer((UUID)uUID);
            if (player == null) continue;
            this.forget(player);
        }
        this.viewers.clear();
    }

    private static final class Tiles {
        final UUID world;
        final MinimapLayer[] layers = new MinimapLayer[4];
        byte[] raw;
        long poseKey = Long.MIN_VALUE;
        int variant = -1;
        int frame = -1;
        double x = Double.NaN;
        double z;
        double previousX;
        double previousZ;

        Tiles(UUID uUID) {
            this.world = uUID;
        }
    }
}

