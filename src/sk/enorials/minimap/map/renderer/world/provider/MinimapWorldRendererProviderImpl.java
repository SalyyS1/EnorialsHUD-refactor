/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map.renderer.world.provider;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import sk.enorials.minimap.map.renderer.world.WorldMinimapRenderer;
import sk.enorials.minimap.map.renderer.world.provider.MinimapWorldRendererProvider;

public class MinimapWorldRendererProviderImpl
implements MinimapWorldRendererProvider {
    private final Map<String, Supplier<WorldMinimapRenderer>> suppliers = new ConcurrentHashMap<String, Supplier<WorldMinimapRenderer>>();

    @Override
    public Supplier<WorldMinimapRenderer> supplier(String string) {
        return this.suppliers.get(string);
    }

    @Override
    public void register(String string, Supplier<WorldMinimapRenderer> supplier) {
        this.suppliers.put(string, supplier);
    }

    @Override
    public Set<String> keys() {
        return this.suppliers.keySet();
    }
}

