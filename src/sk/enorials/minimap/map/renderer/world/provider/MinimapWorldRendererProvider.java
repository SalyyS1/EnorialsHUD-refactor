/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map.renderer.world.provider;

import java.util.Set;
import java.util.function.Supplier;
import sk.enorials.minimap.map.renderer.world.WorldMinimapRenderer;

public interface MinimapWorldRendererProvider {
    default public WorldMinimapRenderer create(String string) {
        Supplier<WorldMinimapRenderer> supplier = this.supplier(string);
        return supplier != null ? supplier.get() : null;
    }

    public Supplier<WorldMinimapRenderer> supplier(String var1);

    public void register(String var1, Supplier<WorldMinimapRenderer> var2);

    public Set<String> keys();
}

