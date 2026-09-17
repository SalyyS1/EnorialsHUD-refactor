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
 *  net.kyori.adventure.text.format.TextDecoration
 *  org.bukkit.entity.Player
 */
package sk.enorials.partyhud.friends;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public final class FriendRequestChat {
    private FriendRequestChat() {
    }

    public static void sendRequest(Player player, String string) {
        if (player != null && string != null && !string.isEmpty()) {
            String string2 = "/enohud party friend accept " + string;
            String string3 = "/enohud party friend deny " + string;
            Component component = Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)("Friend request from " + string + " "), (TextColor)NamedTextColor.YELLOW));
            Component component2 = ((TextComponent)((TextComponent)Component.text((String)"[ACCEPT]", (TextColor)NamedTextColor.GREEN).decorate(TextDecoration.BOLD)).clickEvent(ClickEvent.runCommand((String)string2))).hoverEvent((HoverEventSource)HoverEvent.showText((Component)Component.text((String)"Accept friend request", (TextColor)NamedTextColor.GRAY)));
            Component component3 = ((TextComponent)((TextComponent)Component.text((String)"[DENY]", (TextColor)NamedTextColor.RED).decorate(TextDecoration.BOLD)).clickEvent(ClickEvent.runCommand((String)string3))).hoverEvent((HoverEventSource)HoverEvent.showText((Component)Component.text((String)"Decline friend request", (TextColor)NamedTextColor.GRAY)));
            player.sendMessage(component.append(component2).append((Component)Component.space()).append(component3));
        }
    }
}

