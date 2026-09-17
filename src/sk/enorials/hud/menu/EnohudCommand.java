/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.menu;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import sk.enorials.gps.command.PlacesCommand;
import sk.enorials.hud.EnorialsHUD;

public final class EnohudCommand
implements CommandExecutor,
TabCompleter {
    private final EnorialsHUD plugin;

    public EnohudCommand(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        if (stringArray.length > 0 && stringArray[0].equalsIgnoreCase("trade")) {
            if (this.plugin.trades() == null) {
                commandSender.sendMessage("Trading is unavailable. Check the server log.");
                return true;
            }
            return this.plugin.trades().command(commandSender, Arrays.copyOfRange(stringArray, 1, stringArray.length));
        }
        if (stringArray.length > 0 && stringArray[0].equalsIgnoreCase("access")) {
            return this.plugin.access().command(commandSender, Arrays.copyOfRange(stringArray, 1, stringArray.length));
        }
        if (stringArray.length >= 2 && stringArray[0].equalsIgnoreCase("gps") && stringArray[1].equalsIgnoreCase("place")) {
            return PlacesCommand.execute(this.plugin.gps(), commandSender, Arrays.copyOfRange(stringArray, 2, stringArray.length));
        }
        if (stringArray.length > 0 && stringArray[0].equalsIgnoreCase("gpsapi")) {
            return this.plugin.gps().apiCommand().onCommand(commandSender, command, string, Arrays.copyOfRange(stringArray, 1, stringArray.length));
        }
        if (!(commandSender instanceof Player)) {
            return this.plugin.hudCommand().onCommand(commandSender, command, string, stringArray.length > 0 && stringArray[0].equalsIgnoreCase("hud") ? Arrays.copyOfRange(stringArray, 1, stringArray.length) : stringArray);
        }
        Player player = (Player)commandSender;
        if (stringArray.length == 0) {
            this.plugin.menu().open(player);
            return true;
        }
        String string2 = String.join((CharSequence)" ", Arrays.copyOfRange(stringArray, 1, stringArray.length));
        switch (stringArray[0].toLowerCase(Locale.ROOT)) {
            case "party": {
                if (string2.isBlank()) {
                    this.plugin.menu().party(player);
                    break;
                }
                this.plugin.partyCommand(player, string2);
                break;
            }
            case "map": {
                if (string2.isBlank()) {
                    this.plugin.menu().map(player);
                    break;
                }
                this.plugin.mapCommand(player, string2);
                break;
            }
            case "gps": {
                if (string2.equalsIgnoreCase("stop")) {
                    this.plugin.gps().navigation().stopNavigation(player, "manual");
                    break;
                }
                this.plugin.menu().gps(player);
                break;
            }
            case "effects": {
                this.plugin.menu().effects(player);
                break;
            }
            case "friends": {
                this.plugin.menu().friends(player);
                break;
            }
            case "markers": {
                this.plugin.menu().markers(player);
                break;
            }
            case "hud": {
                if (string2.isBlank()) {
                    this.plugin.menu().hud(player);
                    break;
                }
                this.plugin.hudCommand().onCommand(commandSender, command, string, Arrays.copyOfRange(stringArray, 1, stringArray.length));
                break;
            }
            default: {
                this.plugin.hudCommand().onCommand(commandSender, command, string, stringArray);
            }
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender commandSender, Command command, String string2, String[] stringArray) {
        if (stringArray.length == 1) {
            return List.of("hud", "map", "party", "trade", "effects", "friends", "markers", "gps", "gpsapi").stream().filter(string -> string.startsWith(stringArray[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (stringArray[0].equalsIgnoreCase("trade") && this.plugin.trades() != null) {
            return this.plugin.trades().complete(commandSender, Arrays.copyOfRange(stringArray, 1, stringArray.length));
        }
        if (stringArray[0].equalsIgnoreCase("party")) {
            return this.plugin.party().command().onTabComplete(commandSender, command, "enohud party", Arrays.copyOfRange(stringArray, 1, stringArray.length));
        }
        if (stringArray[0].equalsIgnoreCase("hud")) {
            return this.plugin.hudCommand().onTabComplete(commandSender, command, string2, Arrays.copyOfRange(stringArray, 1, stringArray.length));
        }
        return List.of();
    }
}

