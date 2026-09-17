/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map.icon.provider;

import java.util.HashSet;
import java.util.Set;
import sk.enorials.minimap.map.icon.MinimapIcon;

public interface MinimapIconProvider {
    public MinimapIcon getIcon(String var1);

    public void registerIcon(String var1, MinimapIcon var2);

    public Set<String> allKeys();

    public Set<String> specialIconKeys();

    default public Set<String> genericIconKeys() {
        HashSet<String> hashSet = new HashSet<String>(this.allKeys());
        hashSet.removeAll(this.specialIconKeys());
        return hashSet;
    }
}

