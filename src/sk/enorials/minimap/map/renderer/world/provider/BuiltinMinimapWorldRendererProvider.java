/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map.renderer.world.provider;

import sk.enorials.minimap.map.renderer.world.FlatWorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.VanillaWorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.provider.MinimapWorldRendererProviderImpl;

public class BuiltinMinimapWorldRendererProvider
extends MinimapWorldRendererProviderImpl {
    public BuiltinMinimapWorldRendererProvider() {
        this.register("vanilla", VanillaWorldMinimapRenderer::new);
        this.register("flat", FlatWorldMinimapRenderer::new);
    }
}

