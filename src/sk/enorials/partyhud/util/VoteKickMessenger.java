/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.event.ClickEvent
 *  net.kyori.adventure.text.event.HoverEvent
 *  net.kyori.adventure.text.event.HoverEventSource
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 */
package sk.enorials.partyhud.util;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import sk.enorials.partyhud.party.Party;

public final class VoteKickMessenger {
    private VoteKickMessenger() {
    }

    public static void broadcast(Party party, UUID uUID) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uUID);
        String string = offlinePlayer.getName() != null ? offlinePlayer.getName() : uUID.toString();
        Component component = ((TextComponent)Component.text((String)"[KICK]", (TextColor)NamedTextColor.RED).hoverEvent((HoverEventSource)HoverEvent.showText((Component)Component.text((String)("Vote to kick " + string), (TextColor)NamedTextColor.RED)))).clickEvent(ClickEvent.runCommand((String)("/enohud party votekick " + string + " kick")));
        Component component2 = ((TextComponent)Component.text((String)"[STAY]", (TextColor)NamedTextColor.GREEN).hoverEvent((HoverEventSource)HoverEvent.showText((Component)Component.text((String)("Vote to keep " + string), (TextColor)NamedTextColor.GREEN)))).clickEvent(ClickEvent.runCommand((String)("/enohud party votekick " + string + " stay")));
        Component component3 = ((TextComponent)((TextComponent)((TextComponent)Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)("Vote to kick " + string + ": "), (TextColor)NamedTextColor.YELLOW))).append(component)).append((Component)Component.space())).append(component2);
        for (UUID uUID2 : party.getMembers()) {
            Player player;
            if (uUID2.equals(uUID) || (player = Bukkit.getPlayer((UUID)uUID2)) == null || !player.isOnline()) continue;
            player.sendMessage(component3);
        }
    }
}

