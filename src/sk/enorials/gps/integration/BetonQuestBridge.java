/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.BetonQuest
 *  org.betonquest.betonquest.api.BetonQuestApi
 *  org.betonquest.betonquest.api.identifier.ActionIdentifier
 *  org.betonquest.betonquest.api.identifier.ConditionIdentifier
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.service.action.ActionManager
 *  org.betonquest.betonquest.api.service.condition.ConditionManager
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.ConsoleCommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package sk.enorials.gps.integration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.identifier.ActionIdentifier;
import org.betonquest.betonquest.api.identifier.ConditionIdentifier;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.service.action.ActionManager;
import org.betonquest.betonquest.api.service.condition.ConditionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sk.enorials.gps.EnorialsGpsPlugin;

public final class BetonQuestBridge {
    private final EnorialsGpsPlugin plugin;
    private final boolean configuredEnabled;
    private final Set<String> invalidConditions = new HashSet<String>();

    public BetonQuestBridge(EnorialsGpsPlugin enorialsGpsPlugin) {
        this.plugin = enorialsGpsPlugin;
        this.configuredEnabled = enorialsGpsPlugin.config().getBoolean("betonquest.enabled", true);
    }

    public boolean isEnabled() {
        if (!this.configuredEnabled) {
            return false;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("BetonQuest");
        return plugin != null && plugin.isEnabled();
    }

    public void fire(Player player, String string) {
        this.fire(player, string, Map.of());
    }

    public void fire(Player player, String string, Map<String, String> map) {
        if (!this.isEnabled()) {
            return;
        }
        try {
            String string2 = this.applyContext(string, map);
            ApiCalls.fire(player, string2);
        }
        catch (Exception exception) {
            this.plugin.getLogger().warning("Invalid configured GPS BetonQuest action: " + string + " (" + exception.getMessage() + ")");
        }
    }

    public boolean test(Player player, String string) {
        if (!this.isEnabled()) {
            return false;
        }
        try {
            return ApiCalls.test(player, string);
        }
        catch (Exception | LinkageError throwable) {
            if (this.invalidConditions.add(string)) {
                this.plugin.getLogger().warning("Invalid GPS BetonQuest condition: " + string + " (" + throwable.getMessage() + ")");
            }
            return false;
        }
    }

    public void objective(Player player, String string) {
        this.objective(player, string, Map.of());
    }

    public void objective(Player player, String string, Map<String, String> map) {
        if (!this.isEnabled()) {
            return;
        }
        this.dispatch("betonquest.command_templates.objective", player, string, map);
    }

    public void tag(Player player, String string) {
        this.tag(player, string, Map.of());
    }

    public void tag(Player player, String string, Map<String, String> map) {
        if (!this.isEnabled()) {
            return;
        }
        this.dispatch("betonquest.command_templates.tag", player, string, map);
    }

    public void condition(Player player, String string) {
        this.condition(player, string, Map.of());
    }

    public void condition(Player player, String string, Map<String, String> map) {
        if (!this.isEnabled()) {
            return;
        }
        this.dispatch("betonquest.command_templates.condition", player, string, map);
    }

    public void onStart(Player player, String string, String string2) {
        Map<String, String> map = this.hookContext("start", string, null, string2, null);
        String string3 = this.plugin.config().getString("betonquest.hooks.start_event", "gps_start");
        String string4 = string3 + ":" + string + ":" + string2;
        this.fireHook(player, "start", this.contextWithPayload(map, string4));
        this.runConfiguredHookValues(player, "start", this.contextWithPayload(map, string4));
    }

    public void onStop(Player player, String string, String string2) {
        Map<String, String> map = this.hookContext("stop", null, string, string2, null);
        String string3 = this.plugin.config().getString("betonquest.hooks.stop_event", "gps_stop");
        String string4 = string3 + ":" + string + ":" + string2;
        this.fireHook(player, "stop", this.contextWithPayload(map, string4));
        this.runConfiguredHookValues(player, "stop", this.contextWithPayload(map, string4));
    }

    public void onReach(Player player, String string) {
        Map<String, String> map = this.hookContext("reach", null, null, string, null);
        String string2 = this.plugin.config().getString("betonquest.hooks.reach_event", "gps_reach");
        String string3 = string2 + ":" + string;
        this.fireHook(player, "reach", this.contextWithPayload(map, string3));
        this.runConfiguredHookValues(player, "reach", this.contextWithPayload(map, string3));
    }

    public void onModeChange(Player player, String string) {
        Map<String, String> map = this.hookContext("mode_change", null, null, null, string);
        String string2 = this.plugin.config().getString("betonquest.hooks.mode_change_event", "gps_mode_change");
        String string3 = string2 + ":" + string;
        this.fireHook(player, "mode_change", this.contextWithPayload(map, string3));
        this.runConfiguredHookValues(player, "mode_change", this.contextWithPayload(map, string3));
    }

    private void fireHook(Player player, String string, Map<String, String> map) {
        String string2 = this.plugin.config().getString("betonquest.hooks." + string + "_event", "").trim();
        if (!string2.isEmpty() && !Set.of("gps_start", "gps_stop", "gps_reach", "gps_mode_change").contains(string2)) {
            this.fire(player, string2, map);
        }
    }

    private void runConfiguredHookValues(Player player, String string, Map<String, String> map) {
        String string2;
        for (String string3 : this.getConfigValues("betonquest.extra_event_hooks." + string)) {
            string2 = this.applyContext(string3, map);
            if (string2.isBlank()) continue;
            this.fire(player, string2, this.contextWithPayload(map, string2));
        }
        for (String string3 : this.getConfigValues("betonquest.objective_hooks." + string)) {
            string2 = this.applyContext(string3, map);
            if (string2.isBlank()) continue;
            this.objective(player, string2, map);
        }
        for (String string3 : this.getConfigValues("betonquest.tag_hooks." + string)) {
            string2 = this.applyContext(string3, map);
            if (string2.isBlank()) continue;
            this.tag(player, string2, map);
        }
        for (String string3 : this.getConfigValues("betonquest.condition_hooks." + string)) {
            string2 = this.applyContext(string3, map);
            if (string2.isBlank()) continue;
            this.condition(player, string2, map);
        }
    }

    private List<String> getConfigValues(String string) {
        Object object = this.plugin.config().get(string);
        if (object == null) {
            return List.of();
        }
        if (object instanceof String) {
            String string2 = (String)object;
            String string3 = string2.trim();
            return string3.isEmpty() ? List.of() : List.of(string3);
        }
        if (object instanceof List) {
            List list = (List)object;
            ArrayList<String> arrayList = new ArrayList<String>();
            for (Object e : list) {
                String string4;
                if (e == null || (string4 = String.valueOf(e).trim()).isEmpty()) continue;
                arrayList.add(string4);
            }
            return arrayList;
        }
        return List.of();
    }

    private Map<String, String> hookContext(String string, String string2, String string3, String string4, String string5) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        linkedHashMap.put("hook", this.safe(string));
        linkedHashMap.put("source", this.safe(string2));
        linkedHashMap.put("reason", this.safe(string3));
        linkedHashMap.put("target", this.safe(string4));
        linkedHashMap.put("mode", this.safe(string5));
        return linkedHashMap;
    }

    private Map<String, String> contextWithPayload(Map<String, String> map, String string) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>(map);
        linkedHashMap.put("payload", this.safe(string));
        return linkedHashMap;
    }

    private void dispatch(String string, Player player, String string2, Map<String, String> map) {
        String string3 = this.plugin.config().getString(string, this.defaultTemplateFor(string));
        String string4 = string3.replace("{player}", player.getName()).replace("{uuid}", player.getUniqueId().toString()).replace("{value}", this.sanitize(string2));
        for (Map.Entry<String, String> entry : map.entrySet()) {
            string4 = string4.replace("{" + entry.getKey() + "}", this.sanitize(entry.getValue()));
        }
        ConsoleCommandSender consoleCommandSender = Bukkit.getServer().getConsoleSender();
        Bukkit.dispatchCommand((CommandSender)consoleCommandSender, (String)string4);
    }

    private String applyContext(String string, Map<String, String> map) {
        String string2 = string;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            string2 = string2.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return string2;
    }

    private String sanitize(String string) {
        if (string == null) {
            return "";
        }
        return string.replace(' ', '_');
    }

    private String safe(String string) {
        return string == null ? "" : string;
    }

    private String defaultTemplateFor(String string) {
        if (string.endsWith(".objective")) {
            return "bq objective add {player} {value}";
        }
        if (string.endsWith(".tag")) {
            return "bq tag add {player} {value}";
        }
        if (string.endsWith(".condition")) {
            return "bq condition {player} {value}";
        }
        return "bq action {player} {value}";
    }

    private static final class ApiCalls {
        private ApiCalls() {
        }

        static boolean test(Player player, String string) throws Exception {
            BetonQuestApi betonQuestApi = BetonQuest.getInstance().getBetonQuestApi();
            ConditionIdentifier conditionIdentifier = (ConditionIdentifier)betonQuestApi.identifiers().getFactory(ConditionIdentifier.class).parseIdentifier(null, string);
            return ((ConditionManager)betonQuestApi.conditions().manager()).test((Profile)betonQuestApi.profiles().getProfile(player), conditionIdentifier);
        }

        static void fire(Player player, String string) throws Exception {
            BetonQuestApi betonQuestApi = BetonQuest.getInstance().getBetonQuestApi();
            ActionIdentifier actionIdentifier = (ActionIdentifier)betonQuestApi.identifiers().getFactory(ActionIdentifier.class).parseIdentifier(null, string);
            ((ActionManager)betonQuestApi.actions().manager()).run((Profile)betonQuestApi.profiles().getProfile(player), actionIdentifier);
        }
    }
}

