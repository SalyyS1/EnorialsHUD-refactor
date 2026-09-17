/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.minimap.map.fullscreen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.clientside.ClientsideMinimapFactory;
import sk.enorials.minimap.clientside.MinimapPacketSender;
import sk.enorials.minimap.clientside.gui.GuiMinimapPacketSender;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapProvider;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.fullscreen.FullscreenMinimapLayer;
import sk.enorials.minimap.map.fullscreen.FullscreenSecondaryMinimapLayer;
import sk.enorials.minimap.map.renderer.MinimapLayerRenderer;
import sk.enorials.minimap.map.renderer.encoder.FullscreenMapEncoder;
import sk.enorials.minimap.map.renderer.world.VanillaWorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.WorldMinimapRenderer;

public class FullscreenMinimap {
    private final Minimap baseMinimap;
    private final Player holder;
    private final List<FullscreenMinimapLayer> primaryLayer;
    private final List<FullscreenSecondaryMinimapLayer> secondaryLayers;
    private final MinimapLayer backgroundLayer;
    private final int startX;
    private final int startZ;
    private final int width;
    private final int height;
    private double transitionState;
    private boolean closing;
    private BukkitTask animation;
    private CompletableFuture<Void> animationResult;
    private static final ArrayDeque<Render> QUEUE = new ArrayDeque();
    private static BukkitTask worker;

    private void metadata(MinimapProvider minimapProvider) {
        byte[] byArray = new byte[128];
        FullscreenMapEncoder.encodeBackground(this, byArray);
        minimapProvider.packetSender().updateLayer(this.holder, this.backgroundLayer, 0, 0, 128, 1, byArray);
        for (FullscreenMinimapLayer fullscreenMinimapLayer : this.primaryLayer) {
            byte[] byArray2 = new byte[128];
            FullscreenMapEncoder.encodePrimaryLayer(this, fullscreenMinimapLayer, byArray2);
            minimapProvider.packetSender().updateLayer(this.holder, fullscreenMinimapLayer.base(), 0, 0, 128, 1, byArray2);
        }
    }

    public CompletableFuture<Void> fadeIn(final MinimapProvider minimapProvider, final Function<Double, Double> function, final int n) {
        if (this.animation != null) {
            this.animation.cancel();
        }
        if (this.animationResult != null && !this.animationResult.isDone()) {
            this.animationResult.cancel(false);
        }
        final CompletableFuture<Void> completableFuture = new CompletableFuture<Void>();
        this.animationResult = completableFuture;
        if (n <= 0) {
            this.transitionState = function.apply(1.0);
            this.metadata(minimapProvider);
            completableFuture.complete(null);
            return completableFuture;
        }
        this.animation = new BukkitRunnable(this){
            int elapsed;
            final /* synthetic */ FullscreenMinimap this$0;
            {
                this.this$0 = fullscreenMinimap;
            }

            public void run() {
                if (!this.this$0.holder.isOnline() || completableFuture.isCancelled()) {
                    this.cancel();
                    completableFuture.cancel(false);
                    return;
                }
                try {
                    this.this$0.transitionState = (Double)function.apply((double)(++this.elapsed) / (double)n);
                    this.this$0.metadata(minimapProvider);
                    if (this.elapsed >= n) {
                        this.cancel();
                        completableFuture.complete(null);
                    }
                }
                catch (Throwable throwable) {
                    this.cancel();
                    completableFuture.completeExceptionally(throwable);
                }
            }
        }.runTaskTimer((Plugin)EnorialsMinimap.get().owner(), 0L, 1L);
        return completableFuture;
    }

    public CompletableFuture<Void> fadeOut(MinimapProvider minimapProvider, Function<Double, Double> function, int n) {
        return this.fadeIn(minimapProvider, d -> 1.0 - (Double)function.apply((Double)d), n);
    }

    private static double ease(double d) {
        return 1.0 - Math.pow(1.0 - d, 3.0);
    }

    public void spawn(MinimapProvider minimapProvider) {
        this.closing = false;
        minimapProvider.packetSender().spawnFixedLayer(this.holder, this.backgroundLayer);
        byte[] byArray = new byte[16384];
        Arrays.fill(byArray, (byte)119);
        for (FullscreenMinimapLayer fullscreenMinimapLayer2 : this.primaryLayer) {
            minimapProvider.packetSender().updateLayer(this.holder, fullscreenMinimapLayer2.base(), 0, 0, 128, 128, byArray);
            byte[] byArray2 = new byte[128];
            FullscreenMapEncoder.encodePrimaryLayer(this, fullscreenMinimapLayer2, byArray2);
            minimapProvider.packetSender().updateLayer(this.holder, fullscreenMinimapLayer2.base(), 0, 0, 128, 1, byArray2);
            minimapProvider.packetSender().spawnLayer(this.holder, fullscreenMinimapLayer2.base());
        }
        Location location = this.holder.getLocation();
        this.primaryLayer.stream().sorted(Comparator.comparingDouble(fullscreenMinimapLayer -> {
            double d = (double)((fullscreenMinimapLayer.chunkX() << 7) + 64) - location.getX();
            double d2 = (double)((fullscreenMinimapLayer.chunkZ() << 7) + 64) - location.getZ();
            return d * d + d2 * d2;
        })).forEach(fullscreenMinimapLayer -> QUEUE.addLast(new Render(this, minimapProvider, (FullscreenMinimapLayer)fullscreenMinimapLayer)));
        if (worker == null) {
            worker = Bukkit.getScheduler().runTaskTimer((Plugin)EnorialsMinimap.get().owner(), FullscreenMinimap::renderQueued, 1L, 1L);
        }
        this.fadeIn(minimapProvider, FullscreenMinimap::ease, 20);
    }

    private static void renderQueued() {
        long l = System.nanoTime() + 2000000L;
        int n = 64;
        while (!QUEUE.isEmpty() && n-- > 0 && System.nanoTime() < l) {
            Render render = QUEUE.removeFirst();
            if (render.view.closing || !render.view.holder.isOnline() || !render.world.equals((Object)render.view.holder.getWorld())) continue;
            MinimapLayerRenderer minimapLayerRenderer = render.layer.base().renderer();
            try {
                if (minimapLayerRenderer instanceof VanillaWorldMinimapRenderer) {
                    VanillaWorldMinimapRenderer vanillaWorldMinimapRenderer = (VanillaWorldMinimapRenderer)minimapLayerRenderer;
                    if (render.snapshot == null) {
                        render.snapshot = vanillaWorldMinimapRenderer.snapshot(render.world, render.layer.chunkX() << 7, render.layer.chunkZ() << 7, render.pixels);
                    }
                    if (!render.snapshot.step()) {
                        QUEUE.addLast(render);
                        continue;
                    }
                } else if (minimapLayerRenderer instanceof WorldMinimapRenderer) {
                    WorldMinimapRenderer worldMinimapRenderer = (WorldMinimapRenderer)minimapLayerRenderer;
                    worldMinimapRenderer.renderFully(render.world, render.layer.chunkX() << 7, render.layer.chunkZ() << 7, render.pixels);
                } else {
                    minimapLayerRenderer.render(render.view.baseMinimap, render.layer.base(), render.pixels);
                }
                render.view.sendRendered(render);
            }
            catch (RuntimeException runtimeException) {
                EnorialsMinimap.get().owner().getLogger().log(Level.WARNING, "Fullscreen tile rendering failed", runtimeException);
            }
        }
        if (QUEUE.isEmpty() && worker != null) {
            worker.cancel();
            worker = null;
        }
    }

    private void sendRendered(Render render) {
        byte[] byArray = new byte[16384];
        for (int i = 0; i < 128; ++i) {
            for (int j = 0; j < 128; ++j) {
                byArray[j * 128 + i] = render.pixels[(127 - i) * 128 + j];
            }
        }
        for (FullscreenSecondaryMinimapLayer fullscreenSecondaryMinimapLayer : this.secondaryLayers) {
            SecondaryMinimapLayer secondaryMinimapLayer = fullscreenSecondaryMinimapLayer.base();
            if (secondaryMinimapLayer.getRenderer() == null || secondaryMinimapLayer.getWorld() != null && !render.world.equals((Object)secondaryMinimapLayer.getWorld())) continue;
            secondaryMinimapLayer.getRenderer().renderFullscreen(this, fullscreenSecondaryMinimapLayer, render.layer.chunkX(), render.layer.chunkZ(), byArray);
        }
        MinimapPacketSender minimapPacketSender = render.provider.packetSender();
        if (minimapPacketSender instanceof GuiMinimapPacketSender) {
            GuiMinimapPacketSender guiMinimapPacketSender = (GuiMinimapPacketSender)minimapPacketSender;
            guiMinimapPacketSender.captureFullscreen(this.holder, render.layer.base(), byArray);
        }
        FullscreenMapEncoder.encodePrimaryLayer(this, render.layer, byArray);
        render.provider.packetSender().updateLayer(this.holder, render.layer.base(), 0, 0, 128, 128, byArray);
    }

    public void despawn(MinimapProvider minimapProvider, Consumer<Void> consumer) {
        this.closing = true;
        QUEUE.removeIf(render -> render.view == this);
        if (QUEUE.isEmpty() && worker != null) {
            worker.cancel();
            worker = null;
        }
        this.fadeOut(minimapProvider, FullscreenMinimap::ease, 10).whenComplete((void_, throwable) -> {
            minimapProvider.packetSender().despawnLayer(this.holder, this.backgroundLayer);
            for (FullscreenMinimapLayer fullscreenMinimapLayer : this.primaryLayer) {
                minimapProvider.packetSender().despawnLayer(this.holder, fullscreenMinimapLayer.base());
            }
            if (consumer != null) {
                consumer.accept(null);
            }
        });
    }

    public static FullscreenMinimap create(MinimapProvider minimapProvider, Minimap minimap, int n, int n2) {
        Location location = minimap.holder().getLocation();
        int n3 = n / 2;
        int n4 = n2 / 2;
        int n5 = (int)location.getX() >> 7;
        int n6 = (int)location.getZ() >> 7;
        int n7 = n5 - n3;
        int n8 = n6 - n4;
        int n9 = n3 * 2 + 1;
        int n10 = n4 * 2 + 1;
        ArrayList<FullscreenMinimapLayer> arrayList = new ArrayList<FullscreenMinimapLayer>();
        ClientsideMinimapFactory clientsideMinimapFactory = minimapProvider.clientsideMinimapFactory();
        for (int i = 0; i < n9; ++i) {
            for (int j = 0; j < n10; ++j) {
                arrayList.add(new FullscreenMinimapLayer(clientsideMinimapFactory.createMinimapLayer(location.getWorld(), minimap.primaryLayer().renderer()), n7 + i, n8 + j, i, j));
            }
        }
        ArrayList<FullscreenSecondaryMinimapLayer> arrayList2 = new ArrayList<FullscreenSecondaryMinimapLayer>();
        for (SecondaryMinimapLayer secondaryMinimapLayer : minimap.secondaryLayers().values()) {
            int n11 = secondaryMinimapLayer.getPositionX() + (secondaryMinimapLayer.isTrackLocation() ? 0 : (int)(location.getX() - 64.0));
            int n12 = secondaryMinimapLayer.getPositionZ() + (secondaryMinimapLayer.isTrackLocation() ? 0 : (int)(location.getZ() - 64.0));
            arrayList2.add(new FullscreenSecondaryMinimapLayer(secondaryMinimapLayer, n11 >>= 7, n12 >>= 7, n11 - n7, n12 - n8));
        }
        return new FullscreenMinimap(minimap, minimap.holder(), arrayList, arrayList2, clientsideMinimapFactory.createMinimapLayer(location.getWorld(), null), n7 << 7, n8 << 7, n9, n10);
    }

    public static FullscreenMinimap create(MinimapProvider minimapProvider, Minimap minimap) {
        return FullscreenMinimap.create(minimapProvider, minimap, Config.instance().fullscreen.segmentsX, Config.instance().fullscreen.segmentsZ);
    }

    protected FullscreenMinimap(Minimap minimap, Player player, List<FullscreenMinimapLayer> list, List<FullscreenSecondaryMinimapLayer> list2, MinimapLayer minimapLayer, int n, int n2, int n3, int n4) {
        this.baseMinimap = minimap;
        this.holder = player;
        this.primaryLayer = list;
        this.secondaryLayers = list2;
        this.backgroundLayer = minimapLayer;
        this.startX = n;
        this.startZ = n2;
        this.width = n3;
        this.height = n4;
    }

    public Minimap getBaseMinimap() {
        return this.baseMinimap;
    }

    public Player getHolder() {
        return this.holder;
    }

    public List<FullscreenMinimapLayer> getPrimaryLayer() {
        return this.primaryLayer;
    }

    public List<FullscreenSecondaryMinimapLayer> getSecondaryLayers() {
        return this.secondaryLayers;
    }

    public MinimapLayer getBackgroundLayer() {
        return this.backgroundLayer;
    }

    public int getStartX() {
        return this.startX;
    }

    public int getStartZ() {
        return this.startZ;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public double getTransitionState() {
        return this.transitionState;
    }

    public String toString() {
        return "FullscreenMinimap[" + String.valueOf(this.holder.getUniqueId()) + "," + this.width + "x" + this.height + "]";
    }

    private static final class Render {
        final FullscreenMinimap view;
        final MinimapProvider provider;
        final FullscreenMinimapLayer layer;
        final World world;
        final byte[] pixels = new byte[16384];
        VanillaWorldMinimapRenderer.Snapshot snapshot;

        Render(FullscreenMinimap fullscreenMinimap, MinimapProvider minimapProvider, FullscreenMinimapLayer fullscreenMinimapLayer) {
            this.view = fullscreenMinimap;
            this.provider = minimapProvider;
            this.layer = fullscreenMinimapLayer;
            this.world = fullscreenMinimap.holder.getWorld();
        }
    }
}

