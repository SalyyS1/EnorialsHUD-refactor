/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.instruction.Instruction
 *  org.betonquest.betonquest.api.profile.OnlineProfile
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.quest.action.PlayerAction
 *  org.betonquest.betonquest.api.quest.action.PlayerActionFactory
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 */
package sk.enorials.partyhud.betonquest;

import java.util.List;
import java.util.UUID;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.action.PlayerActionFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import sk.enorials.partyhud.EnorialsPartyPlugin;
import sk.enorials.partyhud.party.Party;

public final class PartyMessageActionFactory
implements PlayerActionFactory {
    private final EnorialsPartyPlugin plugin;

    public PartyMessageActionFactory(EnorialsPartyPlugin enorialsPartyPlugin) {
        this.plugin = enorialsPartyPlugin;
    }

    public PlayerAction parsePlayer(Instruction instruction) throws QuestException {
        List list = instruction.getValueParts();
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Message required");
        }
        int n = 0;
        boolean bl = true;
        String string = (String)list.get(0);
        if ("self".equalsIgnoreCase(string)) {
            bl = false;
            n = 1;
        } else if ("party".equalsIgnoreCase(string)) {
            bl = true;
            n = 1;
        }
        if (n >= list.size()) {
            throw new IllegalArgumentException("Message required");
        }
        final String string2 = String.join((CharSequence)" ", list.subList(n, list.size()));
        final boolean bl2 = bl;
        return new PlayerAction(){
            final /* synthetic */ PartyMessageActionFactory this$0;
            {
                this.this$0 = partyMessageActionFactory;
            }

            public void execute(Profile profile) {
                Player player = profile.getOnlineProfile().map(OnlineProfile::getPlayer).orElse(null);
                if (player != null) {
                    String string = ChatColor.translateAlternateColorCodes((char)'&', (String)string2);
                    if (!bl2) {
                        player.sendMessage(string);
                    } else {
                        Party party = this.this$0.plugin.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
                        if (party == null) {
                            player.sendMessage(string);
                        } else {
                            for (UUID uUID : party.getMembers()) {
                                Player player2 = Bukkit.getPlayer((UUID)uUID);
                                if (player2 == null || !player2.isOnline()) continue;
                                player2.sendMessage(string);
                            }
                        }
                    }
                }
            }

            public boolean isPrimaryThreadEnforced() {
                return true;
            }
        };
    }
}

