/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.clientside.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.imageio.ImageIO;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sk.enorials.common.pack.ResourcePackGuard;
import sk.enorials.hud.avatar.SkinHead;
import sk.enorials.hud.party.PartyMapTracker;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.clientside.gui.GuiMinimapPacketSender;
import sk.enorials.minimap.clientside.gui.GuiMinimapWire;
import sk.enorials.minimap.clientside.gui.GuiPlayerPose;
import sk.enorials.minimap.clientside.gui.LegacyMotion;
import sk.enorials.minimap.clientside.gui.LegacyPlayerPose;
import sk.enorials.minimap.clientside.gui.LegacyRgba;
import sk.enorials.minimap.clientside.impl.NMSClientsideMinimapFactory;
import sk.enorials.minimap.clientside.impl.NMSMinimapPacketSender;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.icon.MarkerIcons;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.minimap.radar.RadarLayer;

final class LegacyMarkers
implements AutoCloseable {
    private final EnorialsMinimap plugin;
    private final NMSClientsideMinimapFactory factory = new NMSClientsideMinimapFactory();
    private final NMSMinimapPacketSender packets;
    private final Map<String, Sprite> sprites = new HashMap<String, Sprite>();
    private final Map<Integer, List<FrameTile>> frames = new HashMap<Integer, List<FrameTile>>();
    private final Map<SkinHead, Sprite> headSprites = new WeakHashMap<SkinHead, Sprite>();
    private final Map<UUID, View> viewers = new HashMap<UUID, View>();

    LegacyMarkers(EnorialsMinimap enorialsMinimap) {
        this.plugin = enorialsMinimap;
        this.packets = new NMSMinimapPacketSender(enorialsMinimap);
        try (InputStream inputStream = enorialsMinimap.owner().getResource("default/pack/assets/enorials_gui/textures/font/hq_markers.png");){
            if (inputStream == null) {
                throw new IOException("Missing HQ marker atlas");
            }
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage == null) {
                throw new IOException("Invalid HQ marker atlas");
            }
            for (int i = 0; i < MarkerIcons.GLYPHS.size(); ++i) {
                int n = bufferedImage.getRGB(0, i * 32);
                int n2 = n >>> 8 & 0xFF;
                int n3 = n & 0xFF;
                this.sprites.put("3:" + MarkerIcons.GLYPHS.get(i), new Sprite(n2, n3, LegacyRgba.encode(bufferedImage.getRGB(2, i * 32 + 2, n2, n3, null, 0, n2))));
            }
        }
        catch (IOException iOException) {
            throw new UncheckedIOException(iOException);
        }
    }

    void begin(Player player) {
        View view = this.viewers.get(player.getUniqueId());
        if (view != null && !Objects.equals(view.world, player.getWorld().getUID())) {
            this.forget(player);
            view = null;
        }
        if (view == null) {
            view = new View();
            view.world = player.getWorld().getUID();
            this.viewers.put(player.getUniqueId(), view);
        }
        view.seen.clear();
        view.order = 0;
        view.frameSeen = false;
    }

    void begin(Player player, GuiPlayerPose guiPlayerPose) {
        this.begin(player);
        this.viewers.get((Object)player.getUniqueId()).pose = guiPlayerPose;
    }

    void frame(Player player, int n, int n2) {
        int n3;
        if (ResourcePackGuard.isLoading(player)) {
            return;
        }
        View view = this.viewers.get(player.getUniqueId());
        if (view == null) {
            return;
        }
        view.frameSeen = true;
        view.frame = n;
        int n4 = n2 / 3 % 2;
        int n5 = n * 2 + n4;
        if (view.frameKey == n5 && view.frameVariant == n2) {
            return;
        }
        List list = this.frames.computeIfAbsent(n5, this::frameTiles);
        for (n3 = list.size(); n3 < view.frameCount; ++n3) {
            this.packets.despawnLayer(player, view.frameLayers.get(n3));
        }
        for (n3 = 0; n3 < list.size(); ++n3) {
            boolean bl;
            FrameTile frameTile = (FrameTile)list.get(n3);
            boolean bl2 = bl = n3 >= view.frameCount;
            if (n3 == view.frameLayers.size()) {
                view.frameLayers.add(this.factory.createMinimapLayer(player.getWorld(), (minimap, minimapLayer, byArray) -> {}));
            }
            boolean bl3 = bl || view.frameKey != n5;
            byte[] byArray2 = bl3 ? (byte[])frameTile.pixels.clone() : new byte[512];
            LegacyMarkers.write(byArray2, 0, 69);
            LegacyMarkers.write(byArray2, 8, 78);
            LegacyMarkers.write(byArray2, 16, 79);
            LegacyMarkers.write(byArray2, 24, 50);
            LegacyMarkers.write(byArray2, 32, n);
            LegacyMarkers.write(byArray2, 128, 7);
            LegacyMarkers.write(byArray2, 136, n2);
            LegacyMarkers.write(byArray2, 144, 42);
            LegacyMarkers.write(byArray2, 152, 42);
            LegacyMarkers.write(byArray2, 256, frameTile.x);
            LegacyMarkers.write(byArray2, 264, frameTile.x >>> 8);
            LegacyMarkers.write(byArray2, 272, frameTile.y);
            LegacyMarkers.write(byArray2, 280, frameTile.y >>> 8);
            LegacyMarkers.write(byArray2, 384, frameTile.width);
            LegacyMarkers.write(byArray2, 392, frameTile.height);
            this.packets.updateLayer(player, view.frameLayers.get(n3), 0, 0, 128, byArray2.length / 128, byArray2);
            if (!bl) continue;
            this.packets.spawnLayer(player, view.frameLayers.get(n3));
        }
        view.frameKey = n5;
        view.frameVariant = n2;
        view.frameCount = list.size();
    }

    private List<FrameTile> frameTiles(int n) {
        List<FrameTile> list;
        block14: {
            String string = "default/pack/assets/enorials_gui/textures/font/frame_" + n / 2 + "_" + (n % 2 == 1 ? "square" : "circle") + ".png";
            InputStream inputStream = this.plugin.owner().getResource(string);
            try {
                if (inputStream == null) {
                    throw new IOException("Missing minimap frame: " + string);
                }
                BufferedImage bufferedImage = ImageIO.read(inputStream);
                if (bufferedImage == null) {
                    throw new IOException("Invalid minimap frame: " + string);
                }
                ArrayList<FrameTile> arrayList = new ArrayList<FrameTile>();
                for (int i = 0; i < 500; i += 40) {
                    for (int j = 0; j < 500; j += 40) {
                        int[] nArray = new int[1764];
                        boolean bl = false;
                        for (int k = 0; k < 42; ++k) {
                            for (int i2 = 0; i2 < 42; ++i2) {
                                int n2;
                                int n3 = j + i2 - 1;
                                int n4 = i + k - 1;
                                if (n3 < 0 || n3 >= 500 || n4 < 0 || n4 >= 500) continue;
                                nArray[k * 42 + i2] = n2 = bufferedImage.getRGB(n3 / 250 * 254 + 3 + n3 % 250, n4 / 250 * 254 + 3 + n4 % 250);
                                if (i2 <= 0 || i2 > 40 || k <= 0 || k > 40 || n2 >>> 24 <= 0) continue;
                                bl = true;
                            }
                        }
                        if (!bl) continue;
                        arrayList.add(new FrameTile(j, i, Math.min(40, 500 - j), Math.min(40, 500 - i), LegacyRgba.encode(nArray)));
                    }
                }
                list = List.copyOf(arrayList);
                if (inputStream == null) break block14;
            }
            catch (Throwable throwable) {
                try {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException iOException) {
                    throw new UncheckedIOException(iOException);
                }
            }
            inputStream.close();
        }
        return list;
    }

    void show(Player player, SecondaryMinimapLayer secondaryMinimapLayer, MinimapIconRenderer minimapIconRenderer, String string, GuiMinimapWire.DataGlyph dataGlyph, int n) {
        boolean bl;
        SkinHead skinHead2;
        if (ResourcePackGuard.isLoading(player)) {
            return;
        }
        View view = this.viewers.get(player.getUniqueId());
        if (view == null) {
            return;
        }
        if (secondaryMinimapLayer instanceof PartyMapTracker.Layer) {
            PartyMapTracker.Layer layer = (PartyMapTracker.Layer)secondaryMinimapLayer;
            v0 = layer.head;
        } else {
            v0 = skinHead2 = null;
        }
        if (skinHead2 == null && string.startsWith("pose_icon/") && !MarkerIcons.hasGlyph(minimapIconRenderer.icon().key())) {
            this.showCustom(player, view, secondaryMinimapLayer, minimapIconRenderer, dataGlyph, Integer.parseInt(string.substring(string.lastIndexOf(118) + 1)));
            return;
        }
        int n2 = skinHead2 != null ? 6 : (string.startsWith("pose_radar/") ? 1 : (string.startsWith("player/") ? 2 : (string.startsWith("hq_marker/") ? 3 : 4)));
        int n3 = Integer.parseInt(string.substring(string.lastIndexOf(118) + 1));
        Sprite sprite = skinHead2 != null ? this.headSprites.computeIfAbsent(skinHead2, skinHead -> new Sprite(32, 32, LegacyRgba.encode(skinHead.rgba()))) : (n2 == 1 ? null : this.sprite(n2, n, minimapIconRenderer));
        int n4 = secondaryMinimapLayer.getBaseLayer().mapId();
        view.seen.add(n4);
        Entry entry = view.entries.get(n4);
        boolean bl2 = bl = entry == null;
        if (bl) {
            entry = view.idle.pollFirst();
            if (entry == null) {
                entry = new Entry();
                entry.layer = this.factory.createMinimapLayer(player.getWorld(), (minimap, minimapLayer, byArray) -> {});
            }
            view.entries.put(n4, entry);
        }
        byte[] byArray2 = new byte[768];
        LegacyMarkers.write(byArray2, 0, 69);
        LegacyMarkers.write(byArray2, 8, 78);
        LegacyMarkers.write(byArray2, 16, 79);
        LegacyMarkers.write(byArray2, 24, 50);
        LegacyMarkers.write(byArray2, 32, view.frame);
        LegacyMarkers.write(byArray2, 128, n2);
        LegacyMarkers.write(byArray2, 136, n3);
        LegacyMarkers.write(byArray2, 144, sprite == null ? 1 : sprite.width);
        LegacyMarkers.write(byArray2, 152, sprite == null ? 1 : sprite.height);
        LegacyMarkers.write(byArray2, 160, Math.min(255, view.order++));
        long l = (long)(dataGlyph.shadowArgb() >>> 1) & Integer.MAX_VALUE;
        l |= ((long)(dataGlyph.cursor() + 0x800000) & 0xFFFFFFL) << 31;
        l |= ((long)dataGlyph.alias() & 0x1FFL) << 55;
        for (int i = 0; i < 8; ++i) {
            LegacyMarkers.write(byArray2, 256 + i * 8, (int)(l >>> i * 8));
        }
        LegacyMarkers.write(byArray2, 320, dataGlyph.alias() >>> 9 & 1);
        if (n2 != 2) {
            LegacyMarkers.motion(byArray2, view, secondaryMinimapLayer);
        }
        if (bl || sprite != entry.sprite) {
            byte[] byArray3 = sprite == null ? new byte[1152] : (byte[])sprite.pixels.clone();
            System.arraycopy(byArray2, 0, byArray3, 0, byArray2.length);
            this.packets.updateLayer(player, entry.layer, 0, 0, 128, byArray3.length / 128, byArray3);
            if (bl) {
                this.packets.spawnLayer(player, entry.layer);
            }
        } else if (!Arrays.equals(byArray2, entry.header)) {
            this.packets.updateLayer(player, entry.layer, 0, 0, 128, 6, byArray2);
        }
        entry.sprite = sprite;
        entry.header = byArray2;
    }

    private void showCustom(Player player, View view, SecondaryMinimapLayer secondaryMinimapLayer, MinimapIconRenderer minimapIconRenderer, GuiMinimapWire.DataGlyph dataGlyph, int n2) {
        int n3;
        MinimapIcon minimapIcon = minimapIconRenderer.icon();
        int n4 = minimapIcon.width();
        int n5 = minimapIcon.height();
        if (n4 < 1 || n5 < 1 || n4 > 128 || n5 > 128 || minimapIcon.data().length != n4 * n5) {
            return;
        }
        int n6 = secondaryMinimapLayer.getBaseLayer().mapId();
        view.seen.add(n6);
        Custom custom = view.custom.computeIfAbsent(n6, n -> new Custom());
        byte[] byArray2 = new byte[768];
        LegacyMarkers.write(byArray2, 0, 69);
        LegacyMarkers.write(byArray2, 8, 78);
        LegacyMarkers.write(byArray2, 16, 79);
        LegacyMarkers.write(byArray2, 24, 50);
        LegacyMarkers.write(byArray2, 32, view.frame);
        LegacyMarkers.write(byArray2, 128, 5);
        LegacyMarkers.write(byArray2, 136, n2);
        LegacyMarkers.write(byArray2, 144, n4);
        LegacyMarkers.write(byArray2, 152, n5);
        LegacyMarkers.write(byArray2, 160, Math.min(255, view.order++));
        long l = (long)(dataGlyph.shadowArgb() >>> 1) & Integer.MAX_VALUE;
        l |= ((long)(dataGlyph.cursor() + 0x800000) & 0xFFFFFFL) << 31;
        l |= ((long)dataGlyph.alias() & 0x1FFL) << 55;
        for (n3 = 0; n3 < 8; ++n3) {
            LegacyMarkers.write(byArray2, 256 + n3 * 8, (int)(l >>> n3 * 8));
        }
        LegacyMarkers.write(byArray2, 320, dataGlyph.alias() >>> 9 & 1);
        LegacyMarkers.motion(byArray2, view, secondaryMinimapLayer);
        n3 = (n5 + 119) / 120;
        while (custom.layers.size() > n3) {
            this.packets.despawnLayer(player, custom.layers.remove(custom.layers.size() - 1));
        }
        for (int i = 0; i < n3; ++i) {
            boolean bl;
            boolean bl2 = bl = i == custom.layers.size();
            if (bl) {
                custom.layers.add(this.factory.createMinimapLayer(player.getWorld(), (minimap, minimapLayer, byArray) -> {}));
            }
            int n7 = i * 120;
            int n8 = Math.min(120, n5 - n7);
            if (bl || custom.icon != minimapIcon || !Arrays.equals(custom.header, byArray2)) {
                boolean bl3 = bl || custom.icon != minimapIcon;
                byte[] byArray3 = new byte[128 * (bl3 ? n8 + 8 : 6)];
                System.arraycopy(byArray2, 0, byArray3, 0, byArray2.length);
                LegacyMarkers.write(byArray3, 384, n7);
                LegacyMarkers.write(byArray3, 392, n8);
                if (bl3) {
                    for (int j = 0; j < n8; ++j) {
                        System.arraycopy(minimapIcon.data(), (n7 + j) * n4, byArray3, (8 + j) * 128, n4);
                    }
                }
                this.packets.updateLayer(player, custom.layers.get(i), 0, 0, 128, byArray3.length / 128, byArray3);
            }
            if (!bl) continue;
            this.packets.spawnLayer(player, custom.layers.get(i));
        }
        custom.icon = minimapIcon;
        custom.header = byArray2;
    }

    private static void motion(byte[] byArray, View view, SecondaryMinimapLayer secondaryMinimapLayer) {
        double d;
        GuiPlayerPose guiPlayerPose = view.pose;
        if (!(guiPlayerPose instanceof LegacyPlayerPose) || !secondaryMinimapLayer.isTrackLocation()) {
            return;
        }
        double d2 = secondaryMinimapLayer.getPositionX();
        double d3 = secondaryMinimapLayer.getPositionZ();
        double d4 = d2;
        double d5 = d3;
        if (secondaryMinimapLayer instanceof PartyMapTracker.Layer) {
            PartyMapTracker.Layer layer = (PartyMapTracker.Layer)secondaryMinimapLayer;
            d2 = layer.x;
            d3 = layer.z;
            d4 = layer.previousX;
            d5 = layer.previousZ;
        } else if (secondaryMinimapLayer instanceof RadarLayer) {
            RadarLayer radarLayer = (RadarLayer)secondaryMinimapLayer;
            d = GuiMinimapPacketSender.radarRemaining(radarLayer, guiPlayerPose.tick);
            double d6 = GuiMinimapPacketSender.radarRemaining(radarLayer, guiPlayerPose.tick - 1L);
            d2 = radarLayer.exactX() - radarLayer.deltaX() * d;
            d3 = radarLayer.exactZ() - radarLayer.deltaZ() * d;
            d4 = radarLayer.exactX() - radarLayer.deltaX() * d6;
            d5 = radarLayer.exactZ() - radarLayer.deltaZ() * d6;
        }
        double d7 = Math.min(1.0, 512.0 / Math.max(1.0, Math.hypot(d2 -= guiPlayerPose.x(), d3 -= guiPlayerPose.z())));
        d = Math.min(1.0, 512.0 / Math.max(1.0, Math.hypot(d4 -= guiPlayerPose.previousX(), d5 -= guiPlayerPose.previousZ())));
        LegacyMotion.write(byArray, d2 * d7, d3 * d7, d4 * d, d5 * d, guiPlayerPose.tick);
    }

    private Sprite sprite(int n, int n2, MinimapIconRenderer minimapIconRenderer) {
        Sprite sprite;
        block11: {
            String string = n == 2 ? "player" : (n == 3 ? MarkerIcons.GLYPHS.get((n2 - 59392) / 16) : minimapIconRenderer.icon().key());
            String string2 = n + ":" + string;
            Sprite sprite2 = this.sprites.get(string2);
            if (sprite2 != null) {
                return sprite2;
            }
            String string3 = "default/pack/assets/enorials_gui/textures/font/" + (String)(n == 3 ? "hq_markers.png" : "icon/" + MarkerIcons.textureKey(string) + ".png");
            InputStream inputStream = this.plugin.owner().getResource(string3);
            try {
                if (inputStream == null) {
                    throw new IOException("Missing marker sprite: " + string3);
                }
                BufferedImage bufferedImage = ImageIO.read(inputStream);
                if (bufferedImage == null) {
                    throw new IOException("Invalid marker sprite: " + string3);
                }
                int n3 = n == 3 ? MarkerIcons.GLYPHS.indexOf(string) * 32 : 0;
                int n4 = bufferedImage.getRGB(0, n3);
                int n5 = n4 >>> 8 & 0xFF;
                int n6 = n4 & 0xFF;
                int[] nArray = bufferedImage.getRGB(2, n3 + 2, n5, n6, null, 0, n5);
                Sprite sprite3 = new Sprite(n5, n6, LegacyRgba.encode(nArray));
                this.sprites.put(string2, sprite3);
                sprite = sprite3;
                if (inputStream == null) break block11;
            }
            catch (Throwable throwable) {
                try {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException iOException) {
                    throw new UncheckedIOException(iOException);
                }
            }
            inputStream.close();
        }
        return sprite;
    }

    void end(Player player) {
        Map.Entry<Integer, Entry> entry;
        View view = this.viewers.get(player.getUniqueId());
        if (view == null) {
            return;
        }
        if (!view.frameSeen) {
            for (int i = 0; i < view.frameCount; ++i) {
                this.packets.despawnLayer(player, view.frameLayers.get(i));
            }
            view.frameCount = 0;
            view.frameKey = -1;
            view.frameVariant = -1;
        }
        Iterator<Map.Entry<Integer, Entry>> iterator = view.entries.entrySet().iterator();
        while (iterator.hasNext()) {
            entry = iterator.next();
            if (view.seen.contains(entry.getKey())) continue;
            this.packets.despawnLayer(player, ((Entry)entry.getValue()).layer);
            if (view.idle.size() < 128) {
                view.idle.addLast((Entry)entry.getValue());
            }
            iterator.remove();
        }
        entry = view.custom.entrySet().iterator();
        while (entry.hasNext()) {
            Map.Entry entry2 = (Map.Entry)entry.next();
            if (view.seen.contains(entry2.getKey())) continue;
            for (MinimapLayer minimapLayer : ((Custom)entry2.getValue()).layers) {
                this.packets.despawnLayer(player, minimapLayer);
            }
            entry.remove();
        }
    }

    private static void write(byte[] byArray, int n, int n2) {
        for (int i = 0; i < 8; ++i) {
            byArray[n + i] = (n2 & 1 << i) == 0 ? 0 : 4;
        }
    }

    void hide(Player player) {
        if (this.viewers.containsKey(player.getUniqueId())) {
            this.begin(player);
            this.end(player);
        }
    }

    void forget(Player player) {
        View view = this.viewers.remove(player.getUniqueId());
        if (view != null) {
            for (MinimapLayer object : view.frameLayers) {
                this.packets.despawnLayer(player, object);
            }
            for (Entry entry : view.entries.values()) {
                this.packets.despawnLayer(player, entry.layer);
            }
            for (Custom custom : view.custom.values()) {
                for (MinimapLayer minimapLayer : custom.layers) {
                    this.packets.despawnLayer(player, minimapLayer);
                }
            }
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
        for (int i = 0; i < view.frameCount; ++i) {
            MinimapLayer object = view.frameLayers.get(i);
            this.packets.despawnLayer(player, object);
            this.packets.spawnLayer(player, object);
        }
        for (Entry entry : view.entries.values()) {
            this.packets.despawnLayer(player, entry.layer);
            this.packets.spawnLayer(player, entry.layer);
        }
        for (Custom custom : view.custom.values()) {
            for (MinimapLayer minimapLayer : custom.layers) {
                this.packets.despawnLayer(player, minimapLayer);
                this.packets.spawnLayer(player, minimapLayer);
            }
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
        this.sprites.clear();
        this.frames.clear();
        this.headSprites.clear();
    }

    private record Sprite(int width, int height, byte[] pixels) {
    }

    private static final class View {
        UUID world;
        int order;
        int frame;
        GuiPlayerPose pose;
        final Map<Integer, Entry> entries = new HashMap<Integer, Entry>();
        final Deque<Entry> idle = new ArrayDeque<Entry>();
        final Map<Integer, Custom> custom = new HashMap<Integer, Custom>();
        final Set<Integer> seen = new HashSet<Integer>();
        final List<MinimapLayer> frameLayers = new ArrayList<MinimapLayer>();
        int frameKey = -1;
        int frameVariant = -1;
        int frameCount;
        boolean frameSeen;

        private View() {
        }
    }

    private record FrameTile(int x, int y, int width, int height, byte[] pixels) {
    }

    private static final class Entry {
        MinimapLayer layer;
        Sprite sprite;
        byte[] header;

        private Entry() {
        }
    }

    private static final class Custom {
        final List<MinimapLayer> layers = new ArrayList<MinimapLayer>();
        MinimapIcon icon;
        byte[] header;

        private Custom() {
        }
    }
}

