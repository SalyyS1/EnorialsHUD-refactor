/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.module.PartyConfig;
import sk.enorials.partyhud.effects.PartyFX;
import sk.enorials.partyhud.friends.FriendService;
import sk.enorials.partyhud.hud.HudService;
import sk.enorials.partyhud.invite.InviteService;
import sk.enorials.partyhud.party.Party;
import sk.enorials.partyhud.party.PartyService;
import sk.enorials.partyhud.patch.FriendRequestBridge;
import sk.enorials.partyhud.ui.GuiService;
import sk.enorials.partyhud.util.ResourcePackChat;
import sk.enorials.partyhud.util.VoteKickMessenger;

public class PartyCommand
implements CommandExecutor,
TabCompleter {
    private final Plugin plugin;
    private final PartyService partyService;
    private final InviteService inviteService;
    private final FriendService friendService;
    private final GuiService guiService;
    private final HudService hudService;
    private final PartyFX fx;

    public PartyCommand(Plugin plugin, PartyService partyService, InviteService inviteService, FriendService friendService, GuiService guiService, HudService hudService, PartyFX partyFX) {
        this.plugin = plugin;
        this.partyService = partyService;
        this.inviteService = inviteService;
        this.friendService = friendService;
        this.guiService = guiService;
        this.hudService = hudService;
        this.fx = partyFX;
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        Player player;
        if (commandSender instanceof Player) {
            player = (Player)commandSender;
            if (!HudAccess.has((CommandSender)player, "enorials.party.use") && !HudAccess.has((CommandSender)player, "enorials.party.admin")) {
                player.sendMessage("[Party] You don't have permission.");
                return true;
            }
        } else {
            commandSender.sendMessage("Players only.");
            return true;
        }
        player = (Player)commandSender;
        if (stringArray.length == 0) {
            ((EnorialsHUD)this.plugin).menu().party(player);
            return true;
        }
        String string2 = stringArray[0].toLowerCase(Locale.ROOT);
        if (string2.equals("friend") && !HudAccess.check((CommandSender)player, "enorials.party.friends")) {
            return true;
        }
        if (string2.equals("rp") || string2.equals("resourcepack")) {
            ResourcePackChat.handle(player, stringArray);
            return true;
        }
        if (string2.equals("friend") && !PartyConfig.get(this.plugin).getBoolean("friends.enabled", true)) {
            player.sendMessage("[Party] Friends are disabled.");
            return true;
        }
        switch (string2) {
            case "create": {
                boolean bl = stringArray.length >= 2 && (stringArray[1].equalsIgnoreCase("open") || stringArray[1].equalsIgnoreCase("public"));
                this.partyService.createParty(player, bl);
                this.fx.partyCreated(player);
                player.sendMessage(Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)(bl ? "Public party created." : "Private party created."), (TextColor)NamedTextColor.YELLOW)));
                this.hudService.renderFor(player);
                return true;
            }
            case "open": 
            case "public": {
                ((EnorialsHUD)this.plugin).menu().party(player);
                return true;
            }
            case "join": {
                if (stringArray.length < 2) {
                    player.sendMessage((Component)Component.text((String)"[Party] Usage: /enohud party join <leader>", (TextColor)NamedTextColor.RED));
                    return true;
                }
                String string3 = stringArray[1];
                Party party2 = null;
                for (Party party3 : this.partyService.getOpenParties()) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)party3.getLeader());
                    String string4 = offlinePlayer.getName();
                    if (string4 == null || !string4.equalsIgnoreCase(string3)) continue;
                    party2 = party3;
                    break;
                }
                if (party2 == null) {
                    player.sendMessage((Component)Component.text((String)"[Party] No public party found for that leader.", (TextColor)NamedTextColor.RED));
                    return true;
                }
                int n = PartyConfig.get(this.plugin).getInt("party.max-size", 4);
                boolean bl = this.partyService.joinParty(party2, player, n);
                if (!bl) {
                    player.sendMessage((Component)Component.text((String)"[Party] Could not join (maybe full or already in a party).", (TextColor)NamedTextColor.RED));
                    return true;
                }
                this.fx.inviteAccepted(player);
                player.sendMessage(Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)"Joined Public Party.", (TextColor)NamedTextColor.YELLOW)));
                this.hudService.renderFor(player);
                return true;
            }
            case "invite": {
                if (stringArray.length < 2) {
                    player.sendMessage((Component)Component.text((String)"Usage: /enohud party invite <player>", (TextColor)NamedTextColor.RED));
                    return true;
                }
                Player player2 = Bukkit.getPlayerExact((String)stringArray[1]);
                if (player2 == null) {
                    player.sendMessage((Component)Component.text((String)"Player not found.", (TextColor)NamedTextColor.RED));
                    return true;
                }
                if (!this.partyService.isInParty(player.getUniqueId())) {
                    player.sendMessage((Component)Component.text((String)"Create a party first.", (TextColor)NamedTextColor.RED));
                    return true;
                }
                this.inviteService.sendInvite(player, player2);
                return true;
            }
            case "accept": {
                String string5;
                String string6 = string5 = stringArray.length >= 2 ? stringArray[1] : null;
                if (!this.inviteService.acceptInvite(player, string5)) {
                    player.sendMessage((Component)Component.text((String)"[Party] No valid invite.", (TextColor)NamedTextColor.RED));
                }
                this.hudService.renderFor(player);
                return true;
            }
            case "decline": {
                String string7;
                String string8 = string7 = stringArray.length >= 2 ? stringArray[1] : null;
                if (!this.inviteService.declineInvite(player, string7)) {
                    player.sendMessage((Component)Component.text((String)"[Party] No valid invite.", (TextColor)NamedTextColor.RED));
                }
                return true;
            }
            case "leave": {
                this.fx.memberLeft(player);
                if (!this.partyService.leaveParty(player.getUniqueId(), false)) {
                    player.sendMessage((Component)Component.text((String)"[Party] You are not in a party.", (TextColor)NamedTextColor.RED));
                }
                this.hudService.renderFor(player);
                return true;
            }
            case "kick": {
                if (stringArray.length < 2) {
                    player.sendMessage((Component)Component.text((String)"Usage: /enohud party kick <player>", (TextColor)NamedTextColor.RED));
                    return true;
                }
                Player player3 = Bukkit.getPlayerExact((String)stringArray[1]);
                if (player3 == null) {
                    player.sendMessage((Component)Component.text((String)"Player not found.", (TextColor)NamedTextColor.RED));
                    return true;
                }
                boolean bl = this.partyService.kick(player, player3.getUniqueId());
                if (!bl) {
                    player.sendMessage((Component)Component.text((String)"[Party] You cannot kick that player.", (TextColor)NamedTextColor.RED));
                } else {
                    player3.sendMessage((Component)Component.text((String)"[Party] You were kicked.", (TextColor)NamedTextColor.RED));
                    this.fx.kicked(player3);
                }
                return true;
            }
            case "promote": {
                if (stringArray.length < 2) {
                    player.sendMessage((Component)Component.text((String)"Usage: /enohud party promote <player>", (TextColor)NamedTextColor.RED));
                    return true;
                }
                Player player4 = Bukkit.getPlayerExact((String)stringArray[1]);
                if (player4 == null) {
                    player.sendMessage((Component)Component.text((String)"Player not found.", (TextColor)NamedTextColor.RED));
                    return true;
                }
                boolean bl = this.partyService.promote(player, player4.getUniqueId());
                if (bl) {
                    this.fx.promoted(player);
                    this.fx.promoted(player4);
                    player.sendMessage((Component)Component.text((String)("[Party] New leader: " + player4.getName()), (TextColor)NamedTextColor.YELLOW));
                } else {
                    player.sendMessage((Component)Component.text((String)"[Party] You cannot promote that player.", (TextColor)NamedTextColor.RED));
                }
                return true;
            }
            case "votekick": {
                PartyService.VoteKickResult voteKickResult;
                Object object;
                Object object2;
                Optional<Party> optional;
                if (stringArray.length < 2) {
                    player.sendMessage((Component)Component.text((String)"Usage: /enohud party votekick <player> [kick|stay]", (TextColor)NamedTextColor.RED));
                    return true;
                }
                String string9 = stringArray[1];
                Object object3 = null;
                Player player5 = Bukkit.getPlayerExact((String)string9);
                if (player5 != null) {
                    object3 = player5.getUniqueId();
                }
                if (object3 == null) {
                    this.partyService.getPartyOf(player.getUniqueId()).ifPresent(party -> {
                        for (UUID uUID : party.getMembers()) {
                            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uUID);
                            if (offlinePlayer.getName() != null && !offlinePlayer.getName().equalsIgnoreCase(string9)) continue;
                        }
                    });
                }
                if ((optional = this.partyService.getPartyOf(player.getUniqueId())).isEmpty()) {
                    player.sendMessage((Component)Component.text((String)"[Party] You are not in a party.", (TextColor)NamedTextColor.RED));
                    return true;
                }
                Party party4 = optional.get();
                Object object4 = party4.getMembers().iterator();
                while (object4.hasNext()) {
                    object2 = object4.next();
                    object = Bukkit.getOfflinePlayer((UUID)object2);
                    if (object.getName() == null || !object.getName().equalsIgnoreCase(string9)) continue;
                    object3 = object2;
                    break;
                }
                if (object3 == null) {
                    player.sendMessage((Component)Component.text((String)"[Party] Player not found in your party.", (TextColor)NamedTextColor.RED));
                    return true;
                }
                object2 = object4 = stringArray.length >= 3 ? stringArray[2].toLowerCase(Locale.ROOT) : "start";
                if (((String)object4).equals("kick") || ((String)object4).equals("stay")) {
                    voteKickResult = this.partyService.castVoteKick(player, (UUID)object3, ((String)object4).equals("kick"));
                } else {
                    voteKickResult = this.partyService.startVoteKick(player, (UUID)object3);
                    VoteKickMessenger.broadcast(party4, (UUID)object3);
                }
                if (voteKickResult instanceof PartyService.VoteKickResult.Voted) {
                    object = (PartyService.VoteKickResult.Voted)voteKickResult;
                    player.sendMessage(Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)("Vote-kick: " + this.name((UUID)object3) + " (" + ((PartyService.VoteKickResult.Voted)object).votes() + "/" + ((PartyService.VoteKickResult.Voted)object).required() + ")"), (TextColor)NamedTextColor.YELLOW)));
                    this.fx.voteCast(player);
                } else if (voteKickResult instanceof PartyService.VoteKickResult.Simple) {
                    object = (PartyService.VoteKickResult.Simple)voteKickResult;
                    if (((PartyService.VoteKickResult.Simple)object).code().equals("KICKED")) {
                        player.sendMessage(Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)("Vote passed. " + this.name((UUID)object3) + " was kicked."), (TextColor)NamedTextColor.YELLOW)));
                        this.fx.votePassed(player);
                        Player player6 = Bukkit.getPlayer((UUID)object3);
                        if (player6 != null) {
                            this.fx.kicked(player6);
                        }
                    } else {
                        player.sendMessage((Component)Component.text((String)"[Party] Vote-kick failed.", (TextColor)NamedTextColor.RED));
                    }
                }
                return true;
            }
            case "friend": {
                if (stringArray.length < 3) {
                    player.sendMessage((Component)Component.text((String)"Usage: /enohud party friend <add|remove> <player>", (TextColor)NamedTextColor.RED));
                    return true;
                }
                String string10 = stringArray[1].toLowerCase(Locale.ROOT);
                if (string10.equals("accept") || string10.equals("deny")) {
                    if (string10.equals("accept")) {
                        FriendRequestBridge.accept(player, stringArray[2]);
                    } else {
                        FriendRequestBridge.deny(player, stringArray[2]);
                    }
                    return true;
                }
                if (string10.equals("removedeny")) {
                    return true;
                }
                if (string10.equals("remove") || string10.equals("removeconfirm")) {
                    for (UUID uUID : this.friendService.getFriends(player.getUniqueId())) {
                        if (!this.friendService.name(uUID).equalsIgnoreCase(stringArray[2]) && !uUID.toString().equals(stringArray[2])) continue;
                        this.friendService.removeFriend(player.getUniqueId(), uUID);
                        player.sendMessage("Friend removed.");
                        return true;
                    }
                    player.sendMessage("Friend not found.");
                    return true;
                }
                Player player7 = Bukkit.getPlayerExact((String)stringArray[2]);
                if (player7 == null) {
                    player.sendMessage((Component)Component.text((String)"Player must be online for this quick command.", (TextColor)NamedTextColor.RED));
                    return true;
                }
                if (string10.equals("add")) {
                    FriendRequestBridge.send(player, player7);
                    player.sendMessage((Component)Component.text((String)("[Friends] Requested: " + player7.getName()), (TextColor)NamedTextColor.LIGHT_PURPLE));
                } else if (string10.equals("remove")) {
                    this.friendService.removeFriend(player.getUniqueId(), player7.getUniqueId());
                    player.sendMessage((Component)Component.text((String)("[Friends] Removed: " + player7.getName()), (TextColor)NamedTextColor.LIGHT_PURPLE));
                } else {
                    player.sendMessage((Component)Component.text((String)"Usage: /enohud party friend <add|remove> <player>", (TextColor)NamedTextColor.RED));
                }
                return true;
            }
            case "hud": {
                this.hudService.renderFor(player);
                player.sendMessage((Component)Component.text((String)"[Party] HUD refreshed.", (TextColor)NamedTextColor.GRAY));
                return true;
            }
        }
        ((EnorialsHUD)this.plugin).menu().party(player);
        return true;
    }

    public List<String> onTabComplete(CommandSender commandSender, Command command, String string, String[] stringArray) {
        if (stringArray.length == 1) {
            return this.prefix(stringArray[0], Arrays.asList("create", "open", "join", "invite", "accept", "decline", "leave", "kick", "promote", "votekick", "friend", "hud"));
        }
        if (stringArray.length == 2) {
            String string2 = stringArray[0].toLowerCase(Locale.ROOT);
            if (string2.equals("friend") && !HudAccess.has(commandSender, "enorials.party.friends")) {
                return List.of();
            }
            if (string2.equals("create")) {
                return this.prefix(stringArray[1], Arrays.asList("public", "private"));
            }
            if (string2.equals("join") || string2.equals("accept") || string2.equals("decline")) {
                return this.onlineNames(stringArray[1]);
            }
            if (string2.equals("invite") || string2.equals("kick") || string2.equals("promote") || string2.equals("votekick")) {
                return this.onlineNames(stringArray[1]);
            }
            if (string2.equals("friend")) {
                return this.prefix(stringArray[1], Arrays.asList("add", "remove", "list"));
            }
        }
        return Collections.emptyList();
    }

    private String name(UUID uUID) {
        String string;
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uUID);
        String string2 = string = offlinePlayer != null ? offlinePlayer.getName() : null;
        if (string == null || string.isBlank()) {
            return uUID.toString().substring(0, 8);
        }
        return string;
    }

    private List<String> onlineNames(String string) {
        ArrayList<String> arrayList = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            arrayList.add(player.getName());
        }
        return this.prefix(string, arrayList);
    }

    private List<String> prefix(String string, List<String> list) {
        String string2 = string.toLowerCase(Locale.ROOT);
        ArrayList<String> arrayList = new ArrayList<String>();
        for (String string3 : list) {
            if (!string3.toLowerCase(Locale.ROOT).startsWith(string2)) continue;
            arrayList.add(string3);
        }
        return arrayList;
    }
}

