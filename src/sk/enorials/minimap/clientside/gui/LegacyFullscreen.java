/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.clientside.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sk.enorials.common.pack.ResourcePackGuard;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.clientside.gui.GuiMinimapPacketSender;
import sk.enorials.minimap.clientside.gui.GuiMinimapWire;
import sk.enorials.minimap.clientside.impl.NMSClientsideMinimapFactory;
import sk.enorials.minimap.clientside.impl.NMSMinimapPacketSender;
import sk.enorials.minimap.map.MinimapLayer;

final class LegacyFullscreen
implements AutoCloseable {
    private final NMSClientsideMinimapFactory factory = new NMSClientsideMinimapFactory();
    private final NMSMinimapPacketSender packets;
    private final Map<UUID, View> viewers = new HashMap<UUID, View>();

    LegacyFullscreen(EnorialsMinimap enorialsMinimap) {
        this.packets = new NMSMinimapPacketSender(enorialsMinimap);
    }

    void render(Player player, GuiMinimapPacketSender.Session session) {
        GuiMinimapPacketSender.LayerState layerState = GuiMinimapPacketSender.fullscreenBackground(session);
        View view = this.viewers.get(player.getUniqueId());
        if (view != null && !view.world.equals(player.getWorld().getUID())) {
            this.forget(player);
            view = null;
        }
        if (layerState == null || ResourcePackGuard.isLoading(player)) {
            if (view != null) {
                this.hide(player, view);
            }
            return;
        }
        if (view == null) {
            view = new View();
            view.world = player.getWorld().getUID();
            this.viewers.put(player.getUniqueId(), view);
        }
        int n2 = 0;
        int n3 = GuiMinimapWire.unsigned(layerState.metadata, 8, 0);
        for (GuiMinimapPacketSender.LayerState iterator : session.layers.values()) {
            if (iterator.raw == null || iterator.raw.length != 20736) continue;
            n2 = Math.min(2, GuiMinimapWire.unsigned(iterator.metadata, 57, 0));
            break;
        }
        HashSet hashSet = new HashSet();
        for (GuiMinimapPacketSender.LayerState layerState2 : session.layers.values()) {
            if (!layerState2.visible || layerState2.fullscreenGeneration != layerState.fullscreenGeneration || GuiMinimapPacketSender.fullscreenMagic(layerState2) != 178 || layerState2.raw == null || layerState2.raw.length != 16384) continue;
            int n4 = GuiMinimapWire.unsigned(layerState2.metadata, 8, 0);
            int n5 = GuiMinimapWire.unsigned(layerState2.metadata, 16, 0);
            int n6 = GuiMinimapWire.unsigned(layerState2.metadata, 24, 0);
            int n7 = GuiMinimapWire.unsigned(layerState2.metadata, 32, 0);
            if (n6 < 1 || n7 < 1 || n4 >= n6 || n5 >= n7 || (long)n6 * (long)n7 * 16384L > 4000000L) continue;
            int n8 = n4 | n5 << 8;
            hashSet.add(n8);
            Tile tile = view.tiles.computeIfAbsent(n8, n -> new Tile());
            int n9 = n6 | n7 << 8 | n2 << 16 | n3 << 18;
            boolean bl = tile.raw != layerState2.raw;
            for (int i = 0; i < 2; ++i) {
                if (tile.layers[i] == null) {
                    tile.layers[i] = this.factory.createMinimapLayer(player.getWorld(), (minimap, minimapLayer, byArray) -> {});
                }
                if (bl || tile.header != n9) {
                    int n10 = i == 0 ? 0 : 120;
                    int n11 = i == 0 ? 120 : 8;
                    byte[] byArray2 = new byte[128 * (bl ? n11 + 8 : 3)];
                    LegacyFullscreen.write(byArray2, 0, 69);
                    LegacyFullscreen.write(byArray2, 8, 78);
                    LegacyFullscreen.write(byArray2, 16, 79);
                    LegacyFullscreen.write(byArray2, 24, 51);
                    LegacyFullscreen.write(byArray2, 128, n4);
                    LegacyFullscreen.write(byArray2, 136, n5);
                    LegacyFullscreen.write(byArray2, 144, n6);
                    LegacyFullscreen.write(byArray2, 152, n7);
                    LegacyFullscreen.write(byArray2, 256, n10);
                    LegacyFullscreen.write(byArray2, 264, n11);
                    LegacyFullscreen.write(byArray2, 272, n2);
                    LegacyFullscreen.write(byArray2, 280, n3);
                    if (bl) {
                        System.arraycopy(layerState2.raw, n10 * 128, byArray2, 1024, n11 * 128);
                    }
                    this.packets.updateLayer(player, tile.layers[i], 0, 0, 128, byArray2.length / 128, byArray2);
                }
                if (tile.visible) continue;
                this.packets.spawnLayer(player, tile.layers[i]);
            }
            tile.raw = layerState2.raw;
            tile.header = n9;
            tile.visible = true;
        }
        for (Map.Entry<Integer, Tile> entry : view.tiles.entrySet()) {
            if (hashSet.contains(entry.getKey()) || !entry.getValue().visible) continue;
            this.hide(player, entry.getValue());
        }
    }

    private static void write(byte[] byArray, int n, int n2) {
        for (int i = 0; i < 8; ++i) {
            byArray[n + i] = (n2 & 1 << i) == 0 ? 0 : 4;
        }
    }

    private void hide(Player player, Tile tile) {
        for (MinimapLayer minimapLayer : tile.layers) {
            if (minimapLayer == null) continue;
            this.packets.despawnLayer(player, minimapLayer);
        }
        tile.visible = false;
    }

    private void hide(Player player, View view) {
        for (Tile tile : view.tiles.values()) {
            if (!tile.visible) continue;
            this.hide(player, tile);
        }
    }

    void relocate(Player player) {
        View view = this.viewers.get(player.getUniqueId());
        if (view == null) {
            return;
        }
        if (!view.world.equals(player.getWorld().getUID())) {
            this.forget(player);
            return;
        }
        for (Tile tile : view.tiles.values()) {
            if (!tile.visible) continue;
            for (MinimapLayer minimapLayer : tile.layers) {
                this.packets.despawnLayer(player, minimapLayer);
                this.packets.spawnLayer(player, minimapLayer);
            }
        }
    }

    void forget(Player player) {
        View view = this.viewers.remove(player.getUniqueId());
        if (view != null) {
            this.hide(player, view);
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

    private static final class View {
        UUID world;
        final Map<Integer, Tile> tiles = new HashMap<Integer, Tile>();

        private View() {
        }
    }

    private static final class Tile {
        final MinimapLayer[] layers = new MinimapLayer[2];
        byte[] raw;
        int header = -1;
        boolean visible;

        private Tile() {
        }
    }
}

