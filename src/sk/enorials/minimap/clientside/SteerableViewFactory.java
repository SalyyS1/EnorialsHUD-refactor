/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.clientside;

import org.bukkit.entity.Player;
import sk.enorials.minimap.clientside.SteerableLockedView;

public interface SteerableViewFactory {
    public SteerableLockedView lockedView(Player var1);
}

