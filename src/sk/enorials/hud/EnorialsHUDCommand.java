/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.model.HudLayout;
import sk.enorials.hud.util.PackHostingSetup;

public class EnorialsHUDCommand
implements CommandExecutor,
TabCompleter {
    private final EnorialsHUD plugin;

    public EnorialsHUDCommand(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
    }

    private void rebuildAndRefreshAvatars() {
        this.plugin.packs().requestBuildWithCallback(() -> {
            this.plugin.avatars().invalidatePacks();
            if (this.plugin.cfg().getBoolean("resource-pack.local.enabled", false)) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    this.plugin.avatars().handleJoin(player);
                }
            }
        }, true);
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        Player player;
        boolean bl;
        if (stringArray.length > 0 && stringArray[0].equalsIgnoreCase("packcheck")) {
            if (!HudAccess.check(commandSender, "enorials.hud.admin")) {
                return true;
            }
            this.plugin.clientProfiles().describe(commandSender);
            if (this.plugin.singlePack() != null && this.plugin.singlePack().single()) {
                this.plugin.singlePack().check(commandSender);
            } else if (!PackHostingSetup.ready()) {
                PackHostingSetup.explain(commandSender);
            } else {
                commandSender.sendMessage("Managed hosting is selected. Check resource-pack.static-url or local.base-url and a real generated ZIP URL. /enohud hud pack rebuilds it.");
            }
            return true;
        }
        boolean bl2 = bl = stringArray.length > 0 && stringArray[0].equalsIgnoreCase("pack") && PackHostingSetup.canBuild();
        if (!bl && !PackHostingSetup.ready()) {
            PackHostingSetup.explain(commandSender);
            return true;
        }
        if (stringArray.length == 0) {
            commandSender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/enohud hud reload | /enohud hud pack | /enohud hud packcheck | /enohud hud layout [name|list|reset] | /enohud hud popup <name> [args...] | /enohud hud packurl <set|reset|show> [url] | /enohud hud avatarrefresh");
            return true;
        }
        if (stringArray[0].equalsIgnoreCase("reload")) {
            if (!HudAccess.has(commandSender, "enorialshud.admin")) {
                commandSender.sendMessage(String.valueOf(ChatColor.RED) + "No permission.");
                return true;
            }
            this.plugin.reloadAll(false);
            this.rebuildAndRefreshAvatars();
            commandSender.sendMessage(String.valueOf(ChatColor.GREEN) + "EnorialsHUD reloaded.");
            return true;
        }
        if (stringArray[0].equalsIgnoreCase("pack")) {
            if (!HudAccess.has(commandSender, "enorialshud.admin")) {
                commandSender.sendMessage(String.valueOf(ChatColor.RED) + "No permission.");
                return true;
            }
            this.rebuildAndRefreshAvatars();
            commandSender.sendMessage(String.valueOf(ChatColor.GREEN) + "HUD pack rebuild queued.");
            return true;
        }
        if (stringArray[0].equalsIgnoreCase("layout")) {
            if (!HudAccess.has(commandSender, "enorialshud.layout")) {
                commandSender.sendMessage(String.valueOf(ChatColor.RED) + "No permission.");
                return true;
            }
            if (stringArray.length == 1) {
                if (commandSender instanceof Player) {
                    Player player2 = (Player)commandSender;
                    this.plugin.renderer().openLayoutGui(player2);
                } else {
                    commandSender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Layouts: " + String.join((CharSequence)", ", this.plugin.layouts().getLayouts().stream().map(hudLayout -> hudLayout.getKey()).toList()));
                }
                return true;
            }
            if (stringArray[1].equalsIgnoreCase("list")) {
                commandSender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Layouts: " + String.join((CharSequence)", ", this.plugin.layouts().getLayouts().stream().map(hudLayout -> hudLayout.getKey()).toList()));
                return true;
            }
            if (stringArray[1].equalsIgnoreCase("reset")) {
                if (!(commandSender instanceof Player)) {
                    commandSender.sendMessage(String.valueOf(ChatColor.RED) + "Only players.");
                    return true;
                }
                Player player3 = (Player)commandSender;
                this.plugin.storage().clearLayout(player3.getUniqueId());
                commandSender.sendMessage(String.valueOf(ChatColor.YELLOW) + "HUD layout reset to default.");
                return true;
            }
            String string2 = stringArray[1];
            if (!this.plugin.layouts().has(string2)) {
                commandSender.sendMessage(String.valueOf(ChatColor.RED) + "Unknown layout: " + string2);
                commandSender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Layouts: " + String.join((CharSequence)", ", this.plugin.layouts().getLayouts().stream().map(hudLayout -> hudLayout.getKey()).toList()));
                return true;
            }
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage(String.valueOf(ChatColor.RED) + "Only players.");
                return true;
            }
            Player player4 = (Player)commandSender;
            this.plugin.storage().setLayout(player4.getUniqueId(), string2);
            commandSender.sendMessage(String.valueOf(ChatColor.GREEN) + "HUD layout set to " + string2 + ".");
            return true;
        }
        if (stringArray[0].equalsIgnoreCase("popup")) {
            if (!HudAccess.check(commandSender, "enorials.hud.admin")) {
                return true;
            }
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage(String.valueOf(ChatColor.RED) + "Only players.");
                return true;
            }
            Player player5 = (Player)commandSender;
            if (stringArray.length < 2) {
                commandSender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /enohud hud popup <name> [args...]");
                return true;
            }
            String[] stringArray2 = new String[Math.max(0, stringArray.length - 2)];
            if (stringArray2.length > 0) {
                System.arraycopy(stringArray, 2, stringArray2, 0, stringArray2.length);
            }
            this.plugin.renderer().showPopup(player5, stringArray[1], stringArray2, 60L);
            return true;
        }
        if (stringArray[0].equalsIgnoreCase("packurl")) {
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage(String.valueOf(ChatColor.RED) + "Only players.");
                return true;
            }
            player = (Player)commandSender;
            if (!HudAccess.has((CommandSender)player, "enorialshud.packurl")) {
                commandSender.sendMessage(String.valueOf(ChatColor.RED) + "No permission.");
                return true;
            }
            if (!this.plugin.cfg().getBoolean("resource-pack.allow-player-url", false)) {
                commandSender.sendMessage(String.valueOf(ChatColor.RED) + "Player pack URL override is disabled.");
                return true;
            }
            if (stringArray.length < 2) {
                commandSender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /enohud hud packurl <set|reset|show> [url]");
                return true;
            }
            if (stringArray[1].equalsIgnoreCase("show")) {
                String string3 = this.plugin.storage().getPackUrl(player.getUniqueId());
                if (string3 == null) {
                    commandSender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Pack URL: <server default>");
                } else {
                    commandSender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Pack URL: " + string3);
                }
                return true;
            }
            if (stringArray[1].equalsIgnoreCase("reset")) {
                this.plugin.storage().setPackUrl(player.getUniqueId(), null);
                commandSender.sendMessage(String.valueOf(ChatColor.GREEN) + "Pack URL reset to server default.");
                return true;
            }
            if (stringArray[1].equalsIgnoreCase("set")) {
                if (stringArray.length < 3) {
                    commandSender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /enohud hud packurl set <url>");
                    return true;
                }
                String string4 = stringArray[2];
                this.plugin.storage().setPackUrl(player.getUniqueId(), string4);
                commandSender.sendMessage(String.valueOf(ChatColor.GREEN) + "Pack URL set to: " + string4);
                return true;
            }
        }
        if (stringArray[0].equalsIgnoreCase("avatarrefresh") || stringArray[0].equalsIgnoreCase("skinrefresh") || stringArray[0].equalsIgnoreCase("refreshavatar") || stringArray[0].equalsIgnoreCase("refreshskin") || stringArray[0].equalsIgnoreCase("avatar") && stringArray.length >= 2 && stringArray[1].equalsIgnoreCase("refresh")) {
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage(String.valueOf(ChatColor.RED) + "Only players.");
                return true;
            }
            player = (Player)commandSender;
            if (!HudAccess.has((CommandSender)player, "enorialshud.avatarrefresh")) {
                commandSender.sendMessage(String.valueOf(ChatColor.RED) + "No permission.");
                return true;
            }
            if (!this.plugin.avatars().refreshAvatar(player)) {
                commandSender.sendMessage(String.valueOf(ChatColor.RED) + "Avatar refresh failed.");
                return true;
            }
            commandSender.sendMessage(String.valueOf(ChatColor.GREEN) + "Avatar refresh queued. The HUD updates without reloading its resource pack.");
            return true;
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender commandSender, Command command, String string, String[] stringArray) {
        ArrayList<String> arrayList = new ArrayList<String>();
        if (stringArray.length == 1) {
            arrayList.add("reload");
            arrayList.add("pack");
            arrayList.add("layout");
            arrayList.add("popup");
            arrayList.add("packurl");
            arrayList.add("avatarrefresh");
            arrayList.add("avatar");
        }
        if (stringArray.length == 2 && stringArray[0].equalsIgnoreCase("layout")) {
            arrayList.add("list");
            arrayList.add("reset");
            for (HudLayout hudLayout : this.plugin.layouts().getLayouts()) {
                arrayList.add(hudLayout.getKey());
            }
        }
        if (stringArray.length == 2 && stringArray[0].equalsIgnoreCase("packurl")) {
            arrayList.add("set");
            arrayList.add("reset");
            arrayList.add("show");
        }
        if (stringArray.length == 2 && stringArray[0].equalsIgnoreCase("avatar")) {
            arrayList.add("refresh");
        }
        return arrayList;
    }
}

