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

import java.util.List;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.quest.objective.Objective;
import org.betonquest.betonquest.api.quest.objective.ObjectiveFactory;
import org.betonquest.betonquest.api.quest.objective.service.ObjectiveService;
import sk.enorials.partyhud.EnorialsPartyPlugin;
import sk.enorials.partyhud.betonquest.PartyInObjective;

public final class PartyInObjectiveFactory
implements ObjectiveFactory {
    private final EnorialsPartyPlugin plugin;

    public PartyInObjectiveFactory(EnorialsPartyPlugin enorialsPartyPlugin) {
        this.plugin = enorialsPartyPlugin;
    }

    public Objective parseInstruction(Instruction instruction, ObjectiveService objectiveService) throws QuestException {
        List list = instruction.getValueParts();
        int n = 1;
        if (!list.isEmpty()) {
            try {
                n = Integer.parseInt((String)list.get(0));
            }
            catch (NumberFormatException numberFormatException) {
                throw new QuestException("Party size must be a number", (Throwable)numberFormatException);
            }
        }
        return new PartyInObjective(instruction, objectiveService, this.plugin, Math.max(1, n));
    }
}

