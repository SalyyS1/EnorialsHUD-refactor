/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.util;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sk.enorials.partyhud.mana.ManaService;
import sk.enorials.partyhud.party.Party;
import sk.enorials.partyhud.party.PartyService;

public class PapiHook
extends PlaceholderExpansion {
    private final Plugin plugin;
    private final PartyService partyService;
    private final ManaService manaService;

    public PapiHook(Plugin plugin, PartyService partyService, ManaService manaService) {
        this.plugin = plugin;
        this.partyService = partyService;
        this.manaService = manaService;
    }

    public String getIdentifier() {
        return "enorials";
    }

    public String getAuthor() {
        return "EnorialsWorld";
    }

    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public boolean persist() {
        return true;
    }

    public String onPlaceholderRequest(Player player, String string) {
        if (player == null) {
            return "";
        }
        String string2 = string.toLowerCase();
        switch (string.toLowerCase()) {
            case "party_in": {
                return String.valueOf(this.partyService.getPartyOf(player.getUniqueId()).isPresent());
            }
            case "party_size": {
                Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
                return optional.map(party -> String.valueOf(party.size())).orElse("0");
            }
            case "party_leader": {
                Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
                return optional.map(party -> {
                    Player player2 = player.getServer().getPlayer(party.getLeader());
                    return player2 != null ? player2.getName() : party.getLeader().toString();
                }).orElse("-");
            }
            case "party_is_leader": {
                Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
                return optional.map(party -> String.valueOf(party.getLeader().equals(player.getUniqueId()))).orElse("false");
            }
            case "party_open": {
                Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
                return optional.map(party -> String.valueOf(party.isOpen())).orElse("false");
            }
            case "party_id": {
                Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
                return optional.map(party -> party.getId().toString()).orElse("");
            }
            case "party_members_online": {
                Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
                return optional.map(party -> {
                    int n = 0;
                    for (UUID uUID : party.getMembers()) {
                        Player player2 = player.getServer().getPlayer(uUID);
                        if (player2 == null || !player2.isOnline()) continue;
                        ++n;
                    }
                    return String.valueOf(n);
                }).orElse("0");
            }
            case "party_votekick_active": {
                Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
                return optional.map(party -> String.valueOf(party.getVoteKick() != null)).orElse("false");
            }
            case "party_votekick_target": {
                Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
                return optional.map(party -> {
                    Party.VoteKick voteKick = party.getVoteKick();
                    if (voteKick == null) {
                        return "";
                    }
                    Player player2 = player.getServer().getPlayer(voteKick.target());
                    return player2 != null ? player2.getName() : voteKick.target().toString();
                }).orElse("");
            }
            case "party_votekick_votes_kick": {
                Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
                return optional.map(party -> {
                    Party.VoteKick voteKick = party.getVoteKick();
                    return voteKick == null ? "0" : String.valueOf(voteKick.kickVoters().size());
                }).orElse("0");
            }
            case "party_votekick_votes_stay": {
                Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
                return optional.map(party -> {
                    Party.VoteKick voteKick = party.getVoteKick();
                    return voteKick == null ? "0" : String.valueOf(voteKick.stayVoters().size());
                }).orElse("0");
            }
            case "party_votekick_required": {
                Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
                return optional.map(party -> String.valueOf(this.requiredKickVotes(party.size()))).orElse("0");
            }
            case "party_votekick_seconds_left": {
                Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
                return optional.map(party -> {
                    Party.VoteKick voteKick = party.getVoteKick();
                    if (voteKick == null) {
                        return "0";
                    }
                    long l = Math.max(0L, voteKick.expiresAtMillis() - System.currentTimeMillis());
                    return String.valueOf((int)Math.ceil((double)l / 1000.0));
                }).orElse("0");
            }
            case "mana": {
                return String.valueOf(this.manaService.getMana(player));
            }
            case "mana_max": {
                return String.valueOf(this.manaService.getMaxMana(player));
            }
        }
        if (string2.startsWith("party_member_")) {
            try {
                int n = Integer.parseInt(string2.substring("party_member_".length())) - 1;
                Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
                if (optional.isEmpty()) {
                    return "";
                }
                Party party2 = optional.get();
                if (n >= 0 && n < party2.getMembers().size()) {
                    UUID uUID = new ArrayList<UUID>(party2.getMembers()).get(n);
                    Player player2 = player.getServer().getPlayer(uUID);
                    return player2 != null ? player2.getName() : uUID.toString();
                }
                return "";
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        return null;
    }

    private int requiredKickVotes(int n) {
        if (n >= 4) {
            return 3;
        }
        return n == 3 ? 2 : 1;
    }
}

