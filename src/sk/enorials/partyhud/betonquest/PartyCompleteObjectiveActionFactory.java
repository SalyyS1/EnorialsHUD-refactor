/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.BetonQuest
 *  org.betonquest.betonquest.api.BetonQuestApi
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.identifier.ObjectiveIdentifier
 *  org.betonquest.betonquest.api.instruction.Instruction
 *  org.betonquest.betonquest.api.profile.OnlineProfile
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.quest.action.PlayerAction
 *  org.betonquest.betonquest.api.quest.action.PlayerActionFactory
 *  org.betonquest.betonquest.api.quest.objective.Objective
 *  org.betonquest.betonquest.api.service.objective.ObjectiveManager
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package sk.enorials.partyhud.betonquest;

import java.util.List;
import java.util.UUID;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.identifier.ObjectiveIdentifier;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.action.PlayerActionFactory;
import org.betonquest.betonquest.api.quest.objective.Objective;
import org.betonquest.betonquest.api.service.objective.ObjectiveManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sk.enorials.partyhud.EnorialsPartyPlugin;
import sk.enorials.partyhud.party.Party;

public final class PartyCompleteObjectiveActionFactory
implements PlayerActionFactory {
    private final EnorialsPartyPlugin plugin;
    private final BetonQuest betonQuest;

    public PartyCompleteObjectiveActionFactory(EnorialsPartyPlugin enorialsPartyPlugin, BetonQuest betonQuest) {
        this.plugin = enorialsPartyPlugin;
        this.betonQuest = betonQuest;
    }

    public PlayerAction parsePlayer(Instruction instruction) throws QuestException {
        List list = instruction.getValueParts();
        if (list.isEmpty()) {
            throw new QuestException("Objective id required");
        }
        BetonQuestApi betonQuestApi = this.betonQuest.getBetonQuestApi();
        ObjectiveIdentifier objectiveIdentifier = (ObjectiveIdentifier)betonQuestApi.identifiers().getFactory(ObjectiveIdentifier.class).parseIdentifier(instruction.getPackage(), (String)list.get(0));
        return profile -> {
            Player player = profile.getOnlineProfile().map(onlineProfile -> onlineProfile.getPlayer()).orElse(null);
            if (player == null) {
                return;
            }
            Party party = this.plugin.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
            if (party == null) {
                return;
            }
            for (UUID uUID : List.copyOf(party.getMembers())) {
                Player player2 = Bukkit.getPlayer((UUID)uUID);
                if (player2 == null || !player2.isOnline()) continue;
                OnlineProfile onlineProfile2 = betonQuestApi.profiles().getProfile(player2);
                Objective objective = ((ObjectiveManager)betonQuestApi.objectives().manager()).getObjective(objectiveIdentifier);
                if (!objective.getService().containsProfile((Profile)onlineProfile2)) continue;
                objective.getService().complete((Profile)onlineProfile2);
            }
        };
    }
}

