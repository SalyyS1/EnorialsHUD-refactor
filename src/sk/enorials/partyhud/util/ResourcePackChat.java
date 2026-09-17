/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package sk.enorials.partyhud.util;

import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;

public final class ResourcePackChat {
    private ResourcePackChat() {
    }

    public static void handle(Player player, String[] stringArray) {
        if (player != null) {
            EnorialsHUD.getInstance().avatars().requestPack(player, false);
        }
    }
}

