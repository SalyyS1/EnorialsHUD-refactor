/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.instruction.Instruction
 *  org.betonquest.betonquest.api.profile.OnlineProfile
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.quest.objective.service.ObjectiveService
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.partyhud.betonquest;

import java.util.ArrayList;
import java.util.Optional;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.objective.service.ObjectiveService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.partyhud.EnorialsPartyPlugin;
import sk.enorials.partyhud.betonquest.EnorialsObjective;
import sk.enorials.partyhud.party.Party;

public final class PartyInObjective
extends EnorialsObjective {
    private final EnorialsPartyPlugin plugin;
    private final int minSize;
    private final BukkitTask task;

    public PartyInObjective(Instruction instruction, ObjectiveService objectiveService, EnorialsPartyPlugin enorialsPartyPlugin, int n) throws QuestException {
        super(objectiveService);
        this.plugin = enorialsPartyPlugin;
        this.minSize = n;
        this.task = Bukkit.getScheduler().runTaskTimer((Plugin)enorialsPartyPlugin.owner(), this::tick, 20L, 20L);
    }

    private void tick() {
        for (Profile profile : new ArrayList(this.getService().getData().keySet())) {
            Optional optional = profile.getOnlineProfile();
            if (optional.isEmpty()) continue;
            Player player = ((OnlineProfile)optional.get()).getPlayer();
            Party party = this.plugin.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
            if (party == null || party.size() < this.minSize || !this.containsPlayer(profile) || !this.checkConditions(profile)) continue;
            this.completeObjectiveForPlayer(profile);
        }
    }

    @Override
    public String getDefaultDataInstruction(Profile profile) {
        return "";
    }

    @Override
    public String getProperty(String string, Profile profile) {
        return "";
    }

    @Override
    public void close() {
        if (this.task != null) {
            this.task.cancel();
        }
        super.close();
    }
}

