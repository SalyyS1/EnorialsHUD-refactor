/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.gps.input;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.input.ChatInputService;

public final class ChatInputListener
implements Listener {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private final EnorialsGpsPlugin plugin;
    private final ChatInputService service;

    public ChatInputListener(EnorialsGpsPlugin enorialsGpsPlugin, ChatInputService chatInputService) {
        this.plugin = enorialsGpsPlugin;
        this.service = chatInputService;
    }

    @EventHandler
    public void onChat(AsyncChatEvent asyncChatEvent) {
        Player player = asyncChatEvent.getPlayer();
        if (!this.service.hasPending(player.getUniqueId())) {
            return;
        }
        asyncChatEvent.setCancelled(true);
        String string = PLAIN.serialize(asyncChatEvent.message());
        Bukkit.getScheduler().runTask((Plugin)this.plugin.owner(), () -> this.service.handleChat(player, string));
    }
}

