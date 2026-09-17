/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.entity.Player
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent
 */
package sk.enorials.hud.compat;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import sk.enorials.hud.compat.ClientProfiles;

public final class PackTransport {
    public static final boolean STACKED = PackTransport.stacked();

    private PackTransport() {
    }

    private static boolean stacked() {
        try {
            Player.class.getMethod("setResourcePack", UUID.class, String.class, byte[].class, Component.class, Boolean.TYPE);
            return true;
        }
        catch (NoSuchMethodException noSuchMethodException) {
            return false;
        }
    }

    public static boolean stacked(Player player) {
        return STACKED && ClientProfiles.of(player).stackPacks();
    }

    public static void main(Player player, UUID uUID, String string, byte[] byArray, Component component, boolean bl) {
        if (PackTransport.stacked(player)) {
            player.setResourcePack(uUID, string, byArray, component, bl);
        } else {
            player.setResourcePack(string, byArray, component, bl);
        }
    }

    public static UUID id(PlayerResourcePackStatusEvent playerResourcePackStatusEvent, UUID uUID) {
        return PackTransport.stacked(playerResourcePackStatusEvent.getPlayer()) ? playerResourcePackStatusEvent.getID() : uUID;
    }
}

