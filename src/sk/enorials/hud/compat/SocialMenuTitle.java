/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.adventure.PaperAdventure
 *  net.kyori.adventure.text.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
 *  net.minecraft.server.level.ServerPlayer
 *  org.bukkit.craftbukkit.entity.CraftPlayer
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.compat;

import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class SocialMenuTitle {
    private SocialMenuTitle() {
    }

    public static void update(Player player, Component component) {
        ServerPlayer serverPlayer = ((CraftPlayer)player).getHandle();
        serverPlayer.connection.send((Packet)new ClientboundOpenScreenPacket(serverPlayer.containerMenu.containerId, serverPlayer.containerMenu.getType(), PaperAdventure.asVanilla((Component)component)));
        player.updateInventory();
    }
}

