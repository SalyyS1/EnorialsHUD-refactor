/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package sk.enorials.partyhud.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.enorials.partyhud.ui.GuiService;

public final class MainMenuCommand
implements CommandExecutor {
    private final GuiService guiService;

    public MainMenuCommand(GuiService guiService) {
        this.guiService = guiService;
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Players only.");
            return true;
        }
        Player player = (Player)commandSender;
        this.guiService.openHubRoot(player);
        return true;
    }
}

