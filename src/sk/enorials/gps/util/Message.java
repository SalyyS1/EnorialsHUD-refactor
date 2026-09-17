/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 */
package sk.enorials.gps.util;

import org.bukkit.command.CommandSender;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.gps.util.Color;

public final class Message {
    private final EnorialsGpsPlugin plugin;

    public Message(EnorialsGpsPlugin enorialsGpsPlugin) {
        this.plugin = enorialsGpsPlugin;
    }

    public void send(CommandSender commandSender, String string) {
        String string2 = this.plugin.config().getString("language.prefix", "&6[GPS] &r");
        commandSender.sendMessage(Color.colorize(string2 + string));
    }
}

