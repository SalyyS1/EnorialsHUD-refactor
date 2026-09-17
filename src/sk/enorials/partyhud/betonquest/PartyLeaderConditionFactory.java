/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.instruction.Instruction
 *  org.betonquest.betonquest.api.profile.OnlineProfile
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.quest.condition.PlayerCondition
 *  org.betonquest.betonquest.api.quest.condition.PlayerConditionFactory
 *  org.bukkit.entity.Player
 */
package sk.enorials.partyhud.betonquest;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.condition.PlayerCondition;
import org.betonquest.betonquest.api.quest.condition.PlayerConditionFactory;
import org.bukkit.entity.Player;
import sk.enorials.partyhud.EnorialsPartyPlugin;
import sk.enorials.partyhud.party.Party;

public final class PartyLeaderConditionFactory
implements PlayerConditionFactory {
    private final EnorialsPartyPlugin plugin;

    public PartyLeaderConditionFactory(EnorialsPartyPlugin enorialsPartyPlugin) {
        this.plugin = enorialsPartyPlugin;
    }

    public PlayerCondition parsePlayer(Instruction instruction) throws QuestException {
        return new PlayerCondition(){

            public boolean check(Profile profile) {
                Player player = profile.getOnlineProfile().map(OnlineProfile::getPlayer).orElse(null);
                if (player == null) {
                    return false;
                }
                Party party = PartyLeaderConditionFactory.this.plugin.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
                return party != null && player.getUniqueId().equals(party.getLeader());
            }
        };
    }
}

