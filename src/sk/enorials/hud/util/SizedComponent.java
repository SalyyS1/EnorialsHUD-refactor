/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 */
package sk.enorials.hud.util;

import net.kyori.adventure.text.Component;

public class SizedComponent {
    public static final SizedComponent EMPTY = new SizedComponent((Component)Component.empty(), 0);
    private final Component component;
    private final int width;

    public SizedComponent(Component component, int n) {
        this.component = component;
        this.width = n;
    }

    public Component getComponent() {
        return this.component;
    }

    public int getWidth() {
        return this.width;
    }
}

