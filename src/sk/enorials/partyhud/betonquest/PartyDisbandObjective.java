/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.instruction.Instruction
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.quest.objective.service.ObjectiveService
 *  org.bukkit.Bukkit
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.partyhud.betonquest;

import java.util.ArrayList;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.objective.service.ObjectiveService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import sk.enorials.partyhud.EnorialsPartyPlugin;
import sk.enorials.partyhud.betonquest.EnorialsObjective;
import sk.enorials.partyhud.event.PartyDisbandEvent;

public final class PartyDisbandObjective
extends EnorialsObjective
implements Listener {
    private final EnorialsPartyPlugin plugin;

    public PartyDisbandObjective(Instruction instruction, ObjectiveService objectiveService, EnorialsPartyPlugin enorialsPartyPlugin) throws QuestException {
        super(objectiveService);
        this.plugin = enorialsPartyPlugin;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsPartyPlugin.owner());
    }

    @EventHandler
    public void onDisband(PartyDisbandEvent partyDisbandEvent) {
        for (Profile profile : new ArrayList(this.getService().getData().keySet())) {
            if (!this.containsPlayer(profile) || !this.checkConditions(profile)) continue;
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
        HandlerList.unregisterAll((Listener)this);
        super.close();
    }
}

