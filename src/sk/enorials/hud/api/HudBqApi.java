/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.BetonQuest
 *  org.betonquest.betonquest.api.BetonQuestApi
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.identifier.ConversationIdentifier
 *  org.betonquest.betonquest.api.profile.OnlineProfile
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.api;

import java.util.List;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.identifier.ConversationIdentifier;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.api.HudTypes;
import sk.enorials.hud.betonquest.HudBetonQuest;
import sk.enorials.minimap.quest.BetonQuestMarkers;

final class HudBqApi {
    HudBqApi() {
    }

    static boolean track(EnorialsHUD enorialsHUD, Player player, String string) {
        HudBetonQuest hudBetonQuest = enorialsHUD.betonQuest();
        if (hudBetonQuest == null || hudBetonQuest.questLog() == null) {
            return false;
        }
        try {
            hudBetonQuest.questLog().refresh(player);
            if (hudBetonQuest.questLog().entries(player).stream().noneMatch(entry -> entry.key().equals(string))) {
                return false;
            }
            hudBetonQuest.questLog().select(player, string);
            return true;
        }
        catch (QuestException questException) {
            throw new IllegalStateException("Cannot select BetonQuest quest", questException);
        }
        catch (RuntimeException runtimeException) {
            throw runtimeException;
        }
        catch (Exception exception) {
            throw new IllegalStateException("Cannot select BetonQuest quest", exception);
        }
    }

    static List<HudTypes.Quest> quests(EnorialsHUD enorialsHUD, Player player) {
        HudBetonQuest hudBetonQuest = enorialsHUD.betonQuest();
        if (hudBetonQuest == null || hudBetonQuest.questLog() == null) {
            return List.of();
        }
        return hudBetonQuest.questLog().entries(player).stream().map(entry -> new HudTypes.Quest(entry.key(), entry.title(), entry.objective(), entry.accepted(), entry.tracked())).toList();
    }

    static void refresh(EnorialsHUD enorialsHUD, Player player) {
        try {
            BetonQuestMarkers betonQuestMarkers;
            HudBetonQuest hudBetonQuest = enorialsHUD.betonQuest();
            if (hudBetonQuest != null && hudBetonQuest.questLog() != null) {
                hudBetonQuest.questLog().refresh(player);
            }
            if ((betonQuestMarkers = BetonQuestMarkers.get()) != null) {
                betonQuestMarkers.refresh(player);
            }
        }
        catch (Exception exception) {
            throw new IllegalStateException("Cannot refresh BetonQuest projection", exception);
        }
    }

    static boolean start(Player player, String string, Location location) {
        try {
            BetonQuestApi betonQuestApi = BetonQuest.getInstance().getBetonQuestApi();
            OnlineProfile onlineProfile = (OnlineProfile)betonQuestApi.profiles().getProfile(player).getOnlineProfile().orElseThrow();
            ConversationIdentifier conversationIdentifier = (ConversationIdentifier)betonQuestApi.identifiers().getFactory(ConversationIdentifier.class).parseIdentifier(null, string);
            if (betonQuestApi.conversations().hasActive((Profile)onlineProfile) || !betonQuestApi.conversations().canStart((Profile)onlineProfile, conversationIdentifier)) {
                return false;
            }
            betonQuestApi.conversations().start(onlineProfile, conversationIdentifier, location, null);
            return true;
        }
        catch (QuestException questException) {
            throw new IllegalArgumentException("Unknown or invalid conversation: " + string, questException);
        }
    }

    static boolean cancel(EnorialsHUD enorialsHUD, Player player) {
        if (enorialsHUD.betonQuest() != null && enorialsHUD.betonQuest().cancelConversation(player)) {
            return true;
        }
        BetonQuestApi betonQuestApi = BetonQuest.getInstance().getBetonQuestApi();
        OnlineProfile onlineProfile = betonQuestApi.profiles().getProfile(player);
        if (!betonQuestApi.conversations().hasActive((Profile)onlineProfile)) {
            return false;
        }
        betonQuestApi.conversations().cancel((OnlineProfile)onlineProfile.getOnlineProfile().orElseThrow());
        return true;
    }
}

