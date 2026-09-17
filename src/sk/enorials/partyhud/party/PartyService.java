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
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.party;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.module.PartyConfig;
import sk.enorials.partyhud.event.PartyCreateEvent;
import sk.enorials.partyhud.event.PartyDisbandEvent;
import sk.enorials.partyhud.event.PartyJoinEvent;
import sk.enorials.partyhud.event.PartyLeaveEvent;
import sk.enorials.partyhud.party.Party;
import sk.enorials.partyhud.util.PartyNotify;

public class PartyService {
    private final Plugin plugin;
    private final Map<UUID, UUID> membership = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, Party> parties = new ConcurrentHashMap<UUID, Party>();

    public PartyService(Plugin plugin) {
        this.plugin = plugin;
    }

    public Optional<Party> getPartyOf(UUID uUID) {
        UUID uUID2 = this.membership.get(uUID);
        if (uUID2 == null) {
            return Optional.empty();
        }
        Party party = this.parties.get(uUID2);
        if (party == null) {
            this.membership.remove(uUID);
            return Optional.empty();
        }
        return Optional.of(party);
    }

    public boolean isInParty(UUID uUID) {
        return this.membership.containsKey(uUID);
    }

    public Party createParty(Player player, boolean bl) {
        HudAccess.require((CommandSender)player, "enorials.party.use");
        this.getPartyOf(player.getUniqueId()).ifPresent(party -> this.leaveParty(player.getUniqueId(), true));
        Party party2 = new Party(player.getUniqueId(), bl);
        this.parties.put(party2.getId(), party2);
        this.membership.put(player.getUniqueId(), party2.getId());
        try {
            Bukkit.getPluginManager().callEvent((Event)new PartyCreateEvent(party2.getId(), player));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        if (bl) {
            try {
                this.broadcastPublicPartyCreated(party2, player);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return party2;
    }

    public boolean joinParty(Party party, Player player, int n) {
        EnorialsHUD enorialsHUD = EnorialsHUD.getInstance();
        if (enorialsHUD != null && enorialsHUD.api() != null) {
            n = enorialsHUD.api().partyLimit(party.getId(), n);
        }
        if (!HudAccess.has((CommandSender)player, "enorials.party.use")) {
            return false;
        }
        if (this.isInParty(player.getUniqueId())) {
            return false;
        }
        if (!party.addMember(player.getUniqueId(), n)) {
            return false;
        }
        this.membership.put(player.getUniqueId(), party.getId());
        PartyNotify.memberJoined(party, player.getUniqueId());
        try {
            Bukkit.getPluginManager().callEvent((Event)new PartyJoinEvent(party.getId(), player));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return true;
    }

    public void disbandParty(Party party) {
        for (UUID uUID : new HashSet<UUID>(party.getMembers())) {
            this.membership.remove(uUID);
        }
        this.parties.remove(party.getId());
        try {
            Bukkit.getPluginManager().callEvent((Event)new PartyDisbandEvent(party.getId()));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public boolean leaveParty(UUID uUID, boolean bl) {
        Object object;
        Optional<Party> optional = this.getPartyOf(uUID);
        if (optional.isEmpty()) {
            return false;
        }
        Party party = optional.get();
        boolean bl2 = party.getLeader().equals(uUID);
        party.removeMember(uUID);
        PartyNotify.memberLeft(party, uUID);
        this.membership.remove(uUID);
        try {
            object = Bukkit.getPlayer((UUID)uUID);
            if (object != null) {
                Bukkit.getPluginManager().callEvent((Event)new PartyLeaveEvent(party.getId(), (Player)object));
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        if (party.size() <= 0) {
            this.parties.remove(party.getId());
            return true;
        }
        if (bl2 && (object = (UUID)party.getMembers().stream().findFirst().orElse(null)) != null) {
            party.setLeader((UUID)object);
            Player player = Bukkit.getPlayer((UUID)object);
            if (player != null && player.isOnline() && !bl) {
                player.sendMessage("\u00a76[Party] \u00a7eYou are now the leader.");
            }
        }
        if (PartyConfig.get(this.plugin).getBoolean("party.disband-when-alone", false) && party.size() <= 1) {
            this.disbandParty(party);
        }
        return true;
    }

    public boolean kick(Player player, UUID uUID) {
        Optional<Party> optional = this.getPartyOf(player.getUniqueId());
        if (optional.isEmpty()) {
            return false;
        }
        Party party = optional.get();
        return !party.getLeader().equals(player.getUniqueId()) && !HudAccess.has((CommandSender)player, "enorials.party.admin") ? false : this.forceKick(party, uUID);
    }

    public boolean forceKick(Party party, UUID uUID) {
        if (!party.isMember(uUID)) {
            return false;
        }
        if (uUID.equals(party.getLeader())) {
            return false;
        }
        party.removeMember(uUID);
        this.membership.remove(uUID);
        return true;
    }

    public boolean promote(Player player, UUID uUID) {
        Optional<Party> optional = this.getPartyOf(player.getUniqueId());
        if (optional.isEmpty()) {
            return false;
        }
        Party party = optional.get();
        if (!party.getLeader().equals(player.getUniqueId()) && !HudAccess.has((CommandSender)player, "enorials.party.admin")) {
            return false;
        }
        if (!party.isMember(uUID)) {
            return false;
        }
        party.setLeader(uUID);
        return true;
    }

    private void broadcastPublicPartyCreated(Party party, Player player) {
        boolean bl = PartyConfig.get(this.plugin).getBoolean("party.public-broadcast.enabled", true);
        if (bl) {
            UUID uUID2;
            String string = player.getName() != null ? player.getName() : player.getUniqueId().toString();
            TextComponent textComponent = Component.text((String)"Members:", (TextColor)NamedTextColor.GRAY);
            for (UUID uUID2 : party.getMembers()) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uUID2);
                String string2 = offlinePlayer.getName() != null ? offlinePlayer.getName() : uUID2.toString();
                textComponent = (TextComponent)((TextComponent)textComponent.append((Component)Component.newline())).append((Component)Component.text((String)(" - " + string2), (TextColor)NamedTextColor.WHITE));
            }
            String string3 = "/enohud party public";
            uUID2 = ((TextComponent)((TextComponent)((TextComponent)((TextComponent)((TextComponent)((TextComponent)((TextComponent)Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)"Vytvoren\u00e1 ", (TextColor)NamedTextColor.YELLOW))).append((Component)Component.text((String)"Public Party", (TextColor)NamedTextColor.GREEN))).append((Component)Component.text((String)" od ", (TextColor)NamedTextColor.YELLOW))).append((Component)Component.text((String)string, (TextColor)NamedTextColor.AQUA))).append((Component)Component.text((String)" \u2014 ", (TextColor)NamedTextColor.YELLOW))).append((Component)Component.text((String)"[PUBLIC PARTIES]", (TextColor)NamedTextColor.GREEN))).clickEvent(ClickEvent.runCommand((String)string3))).hoverEvent((HoverEventSource)HoverEvent.showText((Component)textComponent));
            for (String string2 : Bukkit.getOnlinePlayers()) {
                if (this.isInParty(string2.getUniqueId())) continue;
                string2.sendMessage((Component)uUID2);
            }
        }
    }

    public Collection<Party> getParties() {
        return this.parties.values();
    }

    public List<Party> getOpenParties() {
        ArrayList<Party> arrayList = new ArrayList<Party>();
        for (Party party3 : this.parties.values()) {
            if (!party3.isOpen() || party3.size() <= 0) continue;
            arrayList.add(party3);
        }
        arrayList.sort((party, party2) -> {
            int n = Integer.compare(party2.size(), party.size());
            return n != 0 ? n : this.name(party.getLeader()).compareToIgnoreCase(this.name(party2.getLeader()));
        });
        return arrayList;
    }

    private String name(UUID uUID) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uUID);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : uUID.toString();
    }

    public VoteKickResult startVoteKick(Player player, UUID uUID) {
        Optional<Party> optional = this.getPartyOf(player.getUniqueId());
        if (optional.isEmpty()) {
            return VoteKickResult.NOT_IN_PARTY;
        }
        Party party = optional.get();
        if (!party.isMember(uUID)) {
            return VoteKickResult.TARGET_NOT_IN_PARTY;
        }
        if (uUID.equals(player.getUniqueId())) {
            return VoteKickResult.CANNOT_TARGET_SELF;
        }
        long l = System.currentTimeMillis();
        int n = PartyConfig.get(this.plugin).getInt("party.votekick-timeout-seconds", 45);
        Party.VoteKick voteKick = party.getVoteKick();
        if (voteKick == null || voteKick.expiresAtMillis() < l || !voteKick.target().equals(uUID)) {
            voteKick = new Party.VoteKick(uUID, new HashSet<UUID>(), new HashSet<UUID>(), l + (long)n * 1000L);
        }
        voteKick.kickVoters().add(player.getUniqueId());
        voteKick.stayVoters().remove(player.getUniqueId());
        party.setVoteKick(voteKick);
        int n2 = this.requiredKickVotes(party);
        if (voteKick.kickVoters().size() >= n2) {
            boolean bl = this.forceKick(party, uUID);
            party.setVoteKick(null);
            return bl ? VoteKickResult.KICKED : VoteKickResult.FAILED;
        }
        return VoteKickResult.VOTED(voteKick.kickVoters().size(), n2);
    }

    public VoteKickResult castVoteKick(Player player, UUID uUID, boolean bl) {
        Optional<Party> optional = this.getPartyOf(player.getUniqueId());
        if (optional.isEmpty()) {
            return VoteKickResult.NOT_IN_PARTY;
        }
        Party party = optional.get();
        Party.VoteKick voteKick = party.getVoteKick();
        long l = System.currentTimeMillis();
        if (voteKick == null || voteKick.expiresAtMillis() < l || !voteKick.target().equals(uUID)) {
            return VoteKickResult.FAILED;
        }
        if (player.getUniqueId().equals(uUID)) {
            return VoteKickResult.FAILED;
        }
        if (bl) {
            voteKick.kickVoters().add(player.getUniqueId());
            voteKick.stayVoters().remove(player.getUniqueId());
        } else {
            voteKick.stayVoters().add(player.getUniqueId());
            voteKick.kickVoters().remove(player.getUniqueId());
        }
        int n = this.requiredKickVotes(party);
        if (voteKick.kickVoters().size() >= n) {
            boolean bl2 = this.forceKick(party, uUID);
            party.setVoteKick(null);
            return bl2 ? VoteKickResult.KICKED : VoteKickResult.FAILED;
        }
        return VoteKickResult.VOTED(voteKick.kickVoters().size(), n);
    }

    public VoteKickResult voteKick(Player player, UUID uUID) {
        return this.startVoteKick(player, uUID);
    }

    private int requiredKickVotes(Party party) {
        int n = party.size();
        if (n >= 4) {
            return 3;
        }
        return n == 3 ? 2 : 1;
    }

    public static interface VoteKickResult {
        public static final VoteKickResult NOT_IN_PARTY = new Simple("NOT_IN_PARTY");
        public static final VoteKickResult TARGET_NOT_IN_PARTY = new Simple("TARGET_NOT_IN_PARTY");
        public static final VoteKickResult CANNOT_TARGET_SELF = new Simple("CANNOT_TARGET_SELF");
        public static final VoteKickResult FAILED = new Simple("FAILED");
        public static final VoteKickResult KICKED = new Simple("KICKED");

        public static VoteKickResult VOTED(int n, int n2) {
            return new Voted(n, n2);
        }

        public record Voted(int votes, int required) implements VoteKickResult
        {
        }

        public record Simple(String code) implements VoteKickResult
        {
        }
    }
}

