/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.instruction.Instruction
 *  org.betonquest.betonquest.api.quest.objective.Objective
 *  org.betonquest.betonquest.api.quest.objective.ObjectiveFactory
 *  org.betonquest.betonquest.api.quest.objective.service.ObjectiveService
 */
package sk.enorials.partyhud.betonquest;

import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.quest.objective.Objective;
import org.betonquest.betonquest.api.quest.objective.ObjectiveFactory;
import org.betonquest.betonquest.api.quest.objective.service.ObjectiveService;
import sk.enorials.partyhud.EnorialsPartyPlugin;
import sk.enorials.partyhud.betonquest.PartyJoinObjective;

public final class PartyJoinObjectiveFactory
implements ObjectiveFactory {
    private final EnorialsPartyPlugin plugin;

    public PartyJoinObjectiveFactory(EnorialsPartyPlugin enorialsPartyPlugin) {
        this.plugin = enorialsPartyPlugin;
    }

    public Objective parseInstruction(Instruction instruction, ObjectiveService objectiveService) throws QuestException {
        return new PartyJoinObjective(instruction, objectiveService, this.plugin);
    }
}

