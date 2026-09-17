/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.BetonQuest
 *  org.betonquest.betonquest.api.BetonQuestApi
 *  org.betonquest.betonquest.api.quest.PlayerQuestFactory
 *  org.betonquest.betonquest.api.service.action.ActionRegistry
 *  org.betonquest.betonquest.api.service.condition.ConditionRegistry
 *  org.betonquest.betonquest.api.service.objective.ObjectiveRegistry
 *  org.bukkit.Bukkit
 */
package sk.enorials.partyhud.betonquest;

import java.util.logging.Level;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.quest.PlayerQuestFactory;
import org.betonquest.betonquest.api.service.action.ActionRegistry;
import org.betonquest.betonquest.api.service.condition.ConditionRegistry;
import org.betonquest.betonquest.api.service.objective.ObjectiveRegistry;
import org.bukkit.Bukkit;
import sk.enorials.partyhud.EnorialsPartyPlugin;
import sk.enorials.partyhud.betonquest.PartyCompleteObjectiveActionFactory;
import sk.enorials.partyhud.betonquest.PartyCreateObjectiveFactory;
import sk.enorials.partyhud.betonquest.PartyDisbandObjectiveFactory;
import sk.enorials.partyhud.betonquest.PartyInConditionFactory;
import sk.enorials.partyhud.betonquest.PartyInObjectiveFactory;
import sk.enorials.partyhud.betonquest.PartyJoinObjectiveFactory;
import sk.enorials.partyhud.betonquest.PartyLeaderConditionFactory;
import sk.enorials.partyhud.betonquest.PartyLeaderObjectiveFactory;
import sk.enorials.partyhud.betonquest.PartyLeaveObjectiveFactory;
import sk.enorials.partyhud.betonquest.PartyMessageActionFactory;
import sk.enorials.partyhud.betonquest.PartyRunActionFactory;
import sk.enorials.partyhud.betonquest.PartySameConditionFactory;

public final class BetonQuestHook {
    private BetonQuestHook() {
    }

    public static void register(EnorialsPartyPlugin enorialsPartyPlugin) {
        if (enorialsPartyPlugin == null || !Bukkit.getPluginManager().isPluginEnabled("BetonQuest")) {
            return;
        }
        try {
            BetonQuest betonQuest = BetonQuest.getInstance();
            BetonQuestApi betonQuestApi = betonQuest.getBetonQuestApi();
            ((ConditionRegistry)betonQuestApi.conditions().registry()).register("enoparty", (PlayerQuestFactory)new PartyInConditionFactory(enorialsPartyPlugin));
            ((ConditionRegistry)betonQuestApi.conditions().registry()).register("enoparty_leader", (PlayerQuestFactory)new PartyLeaderConditionFactory(enorialsPartyPlugin));
            ((ConditionRegistry)betonQuestApi.conditions().registry()).register("enoparty_same", (PlayerQuestFactory)new PartySameConditionFactory(enorialsPartyPlugin));
            ((ActionRegistry)betonQuestApi.actions().registry()).register("enoparty_message", (PlayerQuestFactory)new PartyMessageActionFactory(enorialsPartyPlugin));
            ((ActionRegistry)betonQuestApi.actions().registry()).register("enoparty_complete_objective", (PlayerQuestFactory)new PartyCompleteObjectiveActionFactory(enorialsPartyPlugin, betonQuest));
            ((ActionRegistry)betonQuestApi.actions().registry()).register("enoparty_run_action", (PlayerQuestFactory)new PartyRunActionFactory(enorialsPartyPlugin, betonQuest));
            ((ObjectiveRegistry)betonQuestApi.objectives().registry()).register("enoparty_in", (Object)new PartyInObjectiveFactory(enorialsPartyPlugin));
            ((ObjectiveRegistry)betonQuestApi.objectives().registry()).register("enoparty_leader", (Object)new PartyLeaderObjectiveFactory(enorialsPartyPlugin));
            ((ObjectiveRegistry)betonQuestApi.objectives().registry()).register("enoparty_join", (Object)new PartyJoinObjectiveFactory(enorialsPartyPlugin));
            ((ObjectiveRegistry)betonQuestApi.objectives().registry()).register("enoparty_leave", (Object)new PartyLeaveObjectiveFactory(enorialsPartyPlugin));
            ((ObjectiveRegistry)betonQuestApi.objectives().registry()).register("enoparty_create", (Object)new PartyCreateObjectiveFactory(enorialsPartyPlugin));
            ((ObjectiveRegistry)betonQuestApi.objectives().registry()).register("enoparty_disband", (Object)new PartyDisbandObjectiveFactory(enorialsPartyPlugin));
            enorialsPartyPlugin.getLogger().info("BetonQuest integration for the party module enabled.");
        }
        catch (Exception | LinkageError throwable) {
            enorialsPartyPlugin.getLogger().log(Level.SEVERE, "Cannot register BetonQuest integration", throwable);
        }
    }
}

