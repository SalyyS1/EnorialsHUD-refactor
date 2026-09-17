/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.module.HudModule;
import sk.enorials.partyhud.betonquest.BetonQuestHook;
import sk.enorials.partyhud.command.PartyCommand;
import sk.enorials.partyhud.effects.PartyFX;
import sk.enorials.partyhud.friends.FriendPresenceListener;
import sk.enorials.partyhud.friends.FriendService;
import sk.enorials.partyhud.hud.HudService;
import sk.enorials.partyhud.invite.InviteService;
import sk.enorials.partyhud.locator.LocatorService;
import sk.enorials.partyhud.mana.ManaService;
import sk.enorials.partyhud.party.Party;
import sk.enorials.partyhud.party.PartyFriendlyFireListener;
import sk.enorials.partyhud.party.PartyService;
import sk.enorials.partyhud.patch.GuiPatchListener;
import sk.enorials.partyhud.sharedexp.SharedExpService;
import sk.enorials.partyhud.ui.GuiListener;
import sk.enorials.partyhud.ui.GuiService;
import sk.enorials.partyhud.util.PapiHook;
import sk.enorials.partyhud.util.ServerJoinQuitSilencer;
import sk.enorials.partyhud.util.Watermark;

public final class EnorialsPartyPlugin
extends HudModule {
    private PartyCommand partyCommand;
    private PartyService partyService;
    private InviteService inviteService;
    private FriendService friendService;
    private ManaService manaService;
    private GuiService guiService;
    private HudService hudService;
    private SharedExpService sharedExpService;
    private LocatorService locatorService;
    private PartyFX partyFX;
    private Object papiHook;

    public EnorialsPartyPlugin(EnorialsHUD enorialsHUD) {
        super(enorialsHUD, "party", "EnorialsParty");
    }

    public FriendService friends() {
        return this.friendService;
    }

    public InviteService invites() {
        return this.inviteService;
    }

    public PartyCommand command() {
        return this.partyCommand;
    }

    public void onEnable() {
        Watermark.touch();
        this.saveDefaultConfig();
        FileConfiguration fileConfiguration = this.getConfig();
        if (!fileConfiguration.contains("race.icons.dead")) {
            fileConfiguration.set("race.icons.dead", (Object)"\ue106");
            this.saveConfig();
        }
        this.partyService = new PartyService((Plugin)this.owner());
        this.partyFX = new PartyFX((Plugin)this.owner());
        this.inviteService = new InviteService((Plugin)this.owner(), this.partyService, this.partyFX);
        this.friendService = new FriendService((Plugin)this.owner());
        Bukkit.getPluginManager().registerEvents((Listener)new FriendPresenceListener(this.friendService), (Plugin)this.owner());
        Bukkit.getPluginManager().registerEvents((Listener)new PartyFriendlyFireListener(this.partyService), (Plugin)this.owner());
        Bukkit.getPluginManager().registerEvents((Listener)new ServerJoinQuitSilencer(), (Plugin)this.owner());
        this.manaService = new ManaService((Plugin)this.owner());
        this.locatorService = new LocatorService((Plugin)this.owner(), this.partyService);
        this.guiService = new GuiService((Plugin)this.owner(), this.partyService, this.inviteService, this.friendService, this.locatorService, this.partyFX);
        this.hudService = new HudService((Plugin)this.owner(), this.partyService, this.locatorService);
        this.sharedExpService = new SharedExpService((Plugin)this.owner(), this.partyService);
        Bukkit.getPluginManager().registerEvents((Listener)new GuiListener(this.guiService), (Plugin)this.owner());
        Bukkit.getPluginManager().registerEvents((Listener)new GuiPatchListener((Plugin)this.owner(), this.guiService, this.friendService), (Plugin)this.owner());
        Bukkit.getPluginManager().registerEvents((Listener)this.sharedExpService, (Plugin)this.owner());
        Bukkit.getPluginManager().registerEvents((Listener)this.hudService, (Plugin)this.owner());
        Bukkit.getPluginManager().registerEvents((Listener)this.manaService, (Plugin)this.owner());
        Bukkit.getPluginManager().registerEvents((Listener)this.locatorService, (Plugin)this.owner());
        this.partyCommand = new PartyCommand((Plugin)this.owner(), this.partyService, this.inviteService, this.friendService, this.guiService, this.hudService, this.partyFX);
        this.hudService.start();
        this.manaService.start();
        this.inviteService.startExpiryTask();
        this.locatorService.start();
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.papiHook = new PapiHook((Plugin)this.owner(), this.partyService, this.manaService);
            ((PapiHook)((Object)this.papiHook)).register();
            this.getLogger().info("Hooked into PlaceholderAPI.");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            this.getLogger().info("ProtocolLib detected (reserved for advanced HUD/pack features). ");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("BetonQuest")) {
            BetonQuestHook.register(this);
        }
        this.getLogger().info("Party module enabled.");
    }

    public void onDisable() {
        if (this.inviteService != null) {
            this.inviteService.close();
        }
        if (this.papiHook != null) {
            ((PapiHook)((Object)this.papiHook)).unregister();
        }
        if (this.friendService != null) {
            this.friendService.shutdown();
        }
        if (this.hudService != null) {
            this.hudService.stop();
        }
        if (this.locatorService != null) {
            this.locatorService.stop();
        }
        if (this.manaService != null) {
            this.manaService.stop();
        }
        this.getLogger().info("Party module disabled.");
    }

    public PartyService getPartyService() {
        return this.partyService;
    }

    public boolean areInSameParty(UUID uUID, UUID uUID2) {
        if (uUID != null && uUID2 != null) {
            Optional<Party> optional = this.partyService.getPartyOf(uUID);
            if (optional.isEmpty()) {
                return false;
            }
            Optional<Party> optional2 = this.partyService.getPartyOf(uUID2);
            return optional2.isPresent() && optional.get().getId().equals(optional2.get().getId());
        }
        return false;
    }

    public List<UUID> getPartyMemberIds(UUID uUID) {
        if (uUID == null) {
            return Collections.emptyList();
        }
        Optional<Party> optional = this.partyService.getPartyOf(uUID);
        return optional.isEmpty() ? Collections.emptyList() : new ArrayList<UUID>(optional.get().getMembers());
    }
}

