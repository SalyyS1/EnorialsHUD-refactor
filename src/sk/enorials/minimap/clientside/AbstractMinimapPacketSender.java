/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.clientside;

import java.util.function.Consumer;
import sk.enorials.minimap.clientside.MinimapPacketSender;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.SecondaryMinimapLayer;

public abstract class AbstractMinimapPacketSender
implements MinimapPacketSender {
    private void layers(Minimap minimap, boolean bl, Consumer<MinimapLayer> consumer) {
        consumer.accept(minimap.primaryLayer());
        minimap.secondaryLayers().values().stream().filter(secondaryMinimapLayer -> !bl || secondaryMinimapLayer.getWorld() == null || minimap.holder().getWorld().equals((Object)secondaryMinimapLayer.getWorld())).map(SecondaryMinimapLayer::getBaseLayer).forEach(consumer);
    }

    @Override
    public void spawnMinimap(Minimap minimap) {
        this.layers(minimap, true, minimapLayer -> this.spawnLayer(minimap.holder(), (MinimapLayer)minimapLayer));
    }

    @Override
    public void despawnMinimap(Minimap minimap) {
        this.layers(minimap, false, minimapLayer -> this.despawnLayer(minimap.holder(), (MinimapLayer)minimapLayer));
    }
}

