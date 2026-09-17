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

public final class FriendRemoveChat {
    private FriendRemoveChat() {
    }

    public static void sendConfirm(Player player, String string, String string2) {
        if (player != null) {
            String string3 = string != null && !string.isEmpty() ? string : "this friend";
            String string4 = string2 != null && !string2.isEmpty() ? string2 : string3;
            String string5 = "/enohud party friend removeconfirm " + string4;
            String string6 = "/enohud party friend removedeny " + string4;
            Component component = Component.text((String)"[Friends] ", (TextColor)NamedTextColor.LIGHT_PURPLE).append((Component)Component.text((String)("Remove " + string3 + " from friends? "), (TextColor)NamedTextColor.GRAY));
            Component component2 = ((TextComponent)((TextComponent)Component.text((String)"[CONFIRM]", (TextColor)NamedTextColor.GREEN).decorate(TextDecoration.BOLD)).clickEvent(ClickEvent.runCommand((String)string5))).hoverEvent((HoverEventSource)HoverEvent.showText((Component)Component.text((String)"Confirm removal", (TextColor)NamedTextColor.GRAY)));
            Component component3 = ((TextComponent)((TextComponent)Component.text((String)"[DENY]", (TextColor)NamedTextColor.RED).decorate(TextDecoration.BOLD)).clickEvent(ClickEvent.runCommand((String)string6))).hoverEvent((HoverEventSource)HoverEvent.showText((Component)Component.text((String)"Cancel removal", (TextColor)NamedTextColor.GRAY)));
            player.sendMessage(component.append(component2).append((Component)Component.space()).append(component3));
        }
    }

    public static void sendShiftHint(Player player) {
        if (player != null) {
            Component component = Component.text((String)"[Friends] ", (TextColor)NamedTextColor.LIGHT_PURPLE).append((Component)Component.text((String)"Hold Shift and click to remove friends.", (TextColor)NamedTextColor.GRAY));
            player.sendMessage(component);
        }
    }
}

