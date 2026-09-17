/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.clientside.impl;

import org.bukkit.entity.Player;
import sk.enorials.minimap.clientside.SteerableLockedView;
import sk.enorials.minimap.clientside.SteerableViewFactory;
import sk.enorials.minimap.clientside.impl.NMSSteerableLockedView;

public class NMSSteerableViewFactory
implements SteerableViewFactory {
    @Override
    public SteerableLockedView lockedView(Player player) {
        return new NMSSteerableLockedView(player);
    }
}

