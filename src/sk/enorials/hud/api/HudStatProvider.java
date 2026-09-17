/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.api;

import org.bukkit.entity.Player;
import sk.enorials.hud.api.HudValue;

@FunctionalInterface
public interface HudStatProvider {
    public HudValue value(Player var1, String var2);
}

