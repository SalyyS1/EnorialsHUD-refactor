/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package sk.enorials.gps.command;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.enorials.gps.gui.GpsGuiService;
import sk.enorials.gps.nav.NavigationManager;
import sk.enorials.gps.util.Message;
import sk.enorials.hud.access.HudAccess;

public final class GpsCommand
implements CommandExecutor,
TabCompleter {
    private final GpsGuiService guiService;
    private final NavigationManager navigationManager;
    private final Message msg;

    public GpsCommand(GpsGuiService gpsGuiService, NavigationManager navigationManager, Message message) {
        this.guiService = gpsGuiService;
        this.navigationManager = navigationManager;
        this.msg = message;
    }

    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String string, @NotNull String[] stringArray) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("This command is only for players.");
            return true;
        }
        Player player = (Player)commandSender;
        if (!HudAccess.has((CommandSender)player, "enorials.gps.use")) {
            this.msg.send((CommandSender)player, "&cYou do not have permission.");
            return true;
        }
        if (stringArray.length > 0 && stringArray[0].equalsIgnoreCase("stop")) {
            this.navigationManager.stopNavigation(player, "manual");
            return true;
        }
        this.guiService.openMain(player);
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String string, @NotNull String[] stringArray) {
        if (stringArray.length == 1) {
            return List.of("stop");
        }
        return Collections.emptyList();
    }
}

