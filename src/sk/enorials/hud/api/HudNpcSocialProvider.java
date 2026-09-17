/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.api;

import java.util.Collection;
import java.util.UUID;
import org.bukkit.entity.Player;

public interface HudNpcSocialProvider {
    public Collection<Player> targets(Player var1);

    public void requestFriend(Player var1, UUID var2);

    public void requestTrade(Player var1, UUID var2);
}

