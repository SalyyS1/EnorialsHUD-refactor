/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.event.ClickEvent
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.partyhud.invite;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.module.PartyConfig;
import sk.enorials.partyhud.effects.PartyFX;
import sk.enorials.partyhud.invite.PartyInvite;
import sk.enorials.partyhud.party.Party;
import sk.enorials.partyhud.party.PartyService;

public class InviteService {
    private final Plugin plugin;
    private final PartyService partyService;
    private final PartyFX fx;
    private final Map<UUID, PartyInvite> pending = new ConcurrentHashMap<UUID, PartyInvite>();
    private BukkitTask expiryTask;

    public InviteService(Plugin plugin, PartyService partyService, PartyFX partyFX) {
        this.plugin = plugin;
        this.partyService = partyService;
        this.fx = partyFX;
    }

    public Optional<PartyInvite> getInvite(UUID uUID) {
        PartyInvite partyInvite = this.pending.get(uUID);
        if (partyInvite == null) {
            return Optional.empty();
        }
        if (partyInvite.isExpired()) {
            this.pending.remove(uUID, partyInvite);
            return Optional.empty();
        }
        return Optional.of(partyInvite);
    }

    public void removeInvite(UUID uUID) {
        this.pending.remove(uUID);
    }

    public boolean sendInvite(Player player, Player player2) {
        if (!(player.isOnline() && player2.isOnline() && HudAccess.has((CommandSender)player, "enorials.party.use") && HudAccess.has((CommandSender)player2, "enorials.party.use") && !this.partyService.getPartyOf(player2.getUniqueId()).isPresent())) {
            return false;
        }
        if (this.getInvite(player2.getUniqueId()).isPresent()) {
            player.sendMessage((Component)Component.text((String)"[Party] That player already has a pending invitation.", (TextColor)NamedTextColor.YELLOW));
            return false;
        }
        if (player.getUniqueId().equals(player2.getUniqueId())) {
            return false;
        }
        Optional<Party> optional = this.partyService.getPartyOf(player.getUniqueId());
        if (optional.isEmpty()) {
            return false;
        }
        Party party = optional.get();
        if (!party.getLeader().equals(player.getUniqueId())) {
            return false;
        }
        long l = Math.max(5L, Math.min(600L, PartyConfig.get(this.plugin).getLong("party.invite-timeout-seconds", 60L)));
        long l2 = Instant.now().toEpochMilli() + l * 1000L;
        PartyInvite partyInvite = new PartyInvite(player.getUniqueId(), player2.getUniqueId(), l2, party.getId());
        this.pending.put(player2.getUniqueId(), partyInvite);
        Component component = ((TextComponent)Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)"You have been invited by ", (TextColor)NamedTextColor.YELLOW))).append((Component)Component.text((String)player.getName(), (TextColor)NamedTextColor.AQUA));
        Component component2 = ((TextComponent)Component.text((String)"[ACCEPT]", (TextColor)NamedTextColor.GREEN).decorate(TextDecoration.BOLD)).clickEvent(ClickEvent.runCommand((String)("/enohud party accept @" + String.valueOf(partyInvite.getId()))));
        Component component3 = ((TextComponent)Component.text((String)"[DECLINE]", (TextColor)NamedTextColor.RED).decorate(TextDecoration.BOLD)).clickEvent(ClickEvent.runCommand((String)("/enohud party decline @" + String.valueOf(partyInvite.getId()))));
        Component component4 = component2.append((Component)Component.space()).append(component3).append((Component)Component.space()).append((Component)Component.text((String)("(expires in " + l + "s)"), (TextColor)NamedTextColor.GRAY));
        player2.sendMessage(component);
        player2.sendMessage(component4);
        this.showPopup(player2, partyInvite, false);
        this.fx.inviteReceived(player2);
        player.sendMessage(((TextComponent)Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)"Invite sent to ", (TextColor)NamedTextColor.YELLOW))).append((Component)Component.text((String)player2.getName(), (TextColor)NamedTextColor.AQUA)));
        this.fx.inviteSent(player);
        return true;
    }

    public boolean acceptInvite(Player player, String string) {
        Player player2;
        if (!HudAccess.check((CommandSender)player, "enorials.party.use")) {
            return false;
        }
        Optional<PartyInvite> optional = this.getInvite(player.getUniqueId());
        if (optional.isEmpty()) {
            return false;
        }
        PartyInvite partyInvite = optional.get();
        if (!this.matches(partyInvite, player2 = Bukkit.getPlayer((UUID)partyInvite.getLeader()), string)) {
            return false;
        }
        if (player2 != null && player2.isOnline()) {
            if (!this.matches(partyInvite, player2, string)) {
                player.sendMessage((Component)Component.text((String)"[Party] This invite is not from that player.", (TextColor)NamedTextColor.RED));
                return false;
            }
            Optional<Party> optional2 = this.partyService.getPartyOf(player2.getUniqueId());
            if (optional2.isEmpty() || !optional2.get().getLeader().equals(partyInvite.getLeader()) || partyInvite.getPartyId() != null && !optional2.get().getId().equals(partyInvite.getPartyId())) {
                this.pending.remove(player.getUniqueId());
                player.sendMessage((Component)Component.text((String)"[Party] That party no longer exists.", (TextColor)NamedTextColor.RED));
                return false;
            }
            int n = PartyConfig.get(this.plugin).getInt("party.max-size", 7);
            boolean bl = this.partyService.joinParty(optional2.get(), player, n);
            if (!bl) {
                player.sendMessage((Component)Component.text((String)"[Party] Could not join (already in party or party full).", (TextColor)NamedTextColor.RED));
                return false;
            }
            this.pending.remove(player.getUniqueId());
            this.broadcastToParty(optional2.get(), Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)(player.getName() + " joined the party."), (TextColor)NamedTextColor.YELLOW)));
            this.fx.memberJoined(player);
            return true;
        }
        this.pending.remove(player.getUniqueId());
        player.sendMessage((Component)Component.text((String)"[Party] Leader is offline.", (TextColor)NamedTextColor.RED));
        return false;
    }

    public boolean declineInvite(Player player, String string) {
        Player player2;
        Optional<PartyInvite> optional = this.getInvite(player.getUniqueId());
        if (optional.isEmpty()) {
            return false;
        }
        PartyInvite partyInvite = optional.get();
        if (!this.matches(partyInvite, player2 = Bukkit.getPlayer((UUID)partyInvite.getLeader()), string)) {
            return false;
        }
        if (player2 != null && player2.isOnline()) {
            player2.sendMessage(Component.text((String)"[Party] ", (TextColor)NamedTextColor.GOLD).append((Component)Component.text((String)(player.getName() + " declined your invite."), (TextColor)NamedTextColor.YELLOW)));
        }
        this.pending.remove(player.getUniqueId());
        player.sendMessage((Component)Component.text((String)"[Party] Invite declined.", (TextColor)NamedTextColor.GRAY));
        return true;
    }

    private void broadcastToParty(Party party, Component component) {
        for (UUID uUID : party.getMembers()) {
            Player player = Bukkit.getPlayer((UUID)uUID);
            if (player == null || !player.isOnline()) continue;
            player.sendMessage(component);
        }
    }

    private boolean matches(PartyInvite partyInvite, Player player, String string) {
        return string == null || string.equals("@" + String.valueOf(partyInvite.getId())) || !string.startsWith("@") && player != null && player.getName().equalsIgnoreCase(string);
    }

    public boolean current(Player player, PartyInvite partyInvite) {
        if (this.getInvite(player.getUniqueId()).orElse(null) != partyInvite || !player.isOnline() || !HudAccess.has((CommandSender)player, "enorials.party.use") || this.partyService.getPartyOf(player.getUniqueId()).isPresent()) {
            return false;
        }
        Player player2 = Bukkit.getPlayer((UUID)partyInvite.getLeader());
        Party party = this.partyService.getPartyOf(partyInvite.getLeader()).orElse(null);
        return player2 != null && player2.isOnline() && party != null && party.getLeader().equals(partyInvite.getLeader()) && (partyInvite.getPartyId() == null || party.getId().equals(partyInvite.getPartyId()));
    }

    public void showPopup(Player player, PartyInvite partyInvite, boolean bl2) {
        EnorialsHUD enorialsHUD;
        Plugin plugin = this.plugin;
        if (!(plugin instanceof EnorialsHUD) || (enorialsHUD = (EnorialsHUD)plugin).prompts() == null || !this.current(player, partyInvite)) {
            return;
        }
        if (!bl2 && !enorialsHUD.cfg().getBoolean("social.party-invite-popup", true)) {
            return;
        }
        plugin = Bukkit.getPlayer((UUID)partyInvite.getLeader());
        enorialsHUD.prompts().offer(player, "PARTY INVITATION", partyInvite, plugin.getName(), "invites you to their party", partyInvite.getExpiresAtMs(), () -> this.current(player, partyInvite), bl -> {
            if (!this.current(player, partyInvite)) {
                return;
            }
            if (bl.booleanValue()) {
                this.acceptInvite(player, "@" + String.valueOf(partyInvite.getId()));
            } else {
                this.declineInvite(player, "@" + String.valueOf(partyInvite.getId()));
            }
        });
        if (bl2) {
            enorialsHUD.prompts().show(player);
        }
    }

    public void close() {
        if (this.expiryTask != null) {
            this.expiryTask.cancel();
            this.expiryTask = null;
        }
        this.pending.clear();
    }

    public void startExpiryTask() {
        if (this.expiryTask != null) {
            this.expiryTask.cancel();
        }
        this.expiryTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (!this.pending.isEmpty()) {
                long l = Instant.now().toEpochMilli();
                this.pending.entrySet().removeIf(entry -> ((PartyInvite)entry.getValue()).getExpiresAtMs() < l);
            }
        }, 40L, 40L);
    }
}

