/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.profile.ProfileProvider
 *  org.betonquest.betonquest.api.quest.objective.Objective
 *  org.betonquest.betonquest.api.quest.objective.service.ObjectiveService
 */
package sk.enorials.partyhud.betonquest;

import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.profile.ProfileProvider;
import org.betonquest.betonquest.api.quest.objective.Objective;
import org.betonquest.betonquest.api.quest.objective.service.ObjectiveService;

public abstract class EnorialsObjective
implements Objective {
    protected final ProfileProvider profileProvider;
    private final ObjectiveService service;

    protected EnorialsObjective(ObjectiveService objectiveService) {
        this.service = objectiveService;
        this.profileProvider = objectiveService.getProfileProvider();
        objectiveService.setDefaultData(this::getDefaultDataInstruction);
    }

    public final ObjectiveService getService() {
        return this.service;
    }

    protected final boolean containsPlayer(Profile profile) {
        return this.service.containsProfile(profile);
    }

    protected final boolean checkConditions(Profile profile) {
        return (Boolean)this.service.getExceptionHandler().handle(() -> this.service.checkConditions(profile), (Object)false);
    }

    protected final void completeObjectiveForPlayer(Profile profile) {
        this.service.complete(profile);
    }

    public abstract String getDefaultDataInstruction(Profile var1);

    public abstract String getProperty(String var1, Profile var2);

    public void close() {
    }
}

