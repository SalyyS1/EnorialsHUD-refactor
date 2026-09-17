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
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package sk.enorials.partyhud.betonquest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.condition.PlayerCondition;
import org.betonquest.betonquest.api.quest.condition.PlayerConditionFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sk.enorials.partyhud.EnorialsPartyPlugin;
import sk.enorials.partyhud.party.Party;

public final class PartySameConditionFactory
implements PlayerConditionFactory {
    private final EnorialsPartyPlugin plugin;

    public PartySameConditionFactory(EnorialsPartyPlugin enorialsPartyPlugin) {
        this.plugin = enorialsPartyPlugin;
    }

    public PlayerCondition parsePlayer(Instruction instruction) throws QuestException {
        List list = instruction.getValueParts();
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Player name or UUID required");
        }
        final String string = (String)list.get(0);
        return new PlayerCondition(){
            final /* synthetic */ PartySameConditionFactory this$0;
            {
                this.this$0 = partySameConditionFactory;
            }

            public boolean check(Profile profile) {
                Player player = profile.getOnlineProfile().map(OnlineProfile::getPlayer).orElse(null);
                if (player == null) {
                    return false;
                }
                UUID uUID = this.this$0.resolveUuid(string);
                if (uUID == null) {
                    return false;
                }
                Optional<Party> optional = this.this$0.plugin.getPartyService().getPartyOf(player.getUniqueId());
                if (optional.isEmpty()) {
                    return false;
                }
                Optional<Party> optional2 = this.this$0.plugin.getPartyService().getPartyOf(uUID);
                return optional2.isPresent() && optional.get().getId().equals(optional2.get().getId());
            }
        };
    }

    private UUID resolveUuid(String string) {
        if (string != null && !string.isBlank()) {
            try {
                return UUID.fromString(string);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                Player player = Bukkit.getPlayerExact((String)string);
                return player != null ? player.getUniqueId() : null;
            }
        }
        return null;
    }
}

