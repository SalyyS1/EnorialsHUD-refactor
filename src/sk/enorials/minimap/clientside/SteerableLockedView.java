/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.clientside;

import java.util.function.Consumer;

public interface SteerableLockedView {
    public void onSneak(Consumer<Void> var1);

    public void destroy();
}

