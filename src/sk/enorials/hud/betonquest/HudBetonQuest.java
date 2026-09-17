/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.instruction.Argument
 *  org.betonquest.betonquest.api.instruction.Instruction
 *  org.betonquest.betonquest.api.integration.IntegrationService
 *  org.betonquest.betonquest.api.integration.policy.Policy
 *  org.betonquest.betonquest.api.profile.OnlineProfile
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.quest.action.PlayerAction
 *  org.betonquest.betonquest.api.quest.condition.PlayerCondition
 *  org.betonquest.betonquest.api.service.action.ActionRegistry
 *  org.betonquest.betonquest.api.service.condition.ConditionRegistry
 *  org.betonquest.betonquest.api.service.objective.ObjectiveRegistry
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 *  sk.enorials.spells.api.EnorialsSpellsApi
 *  sk.enorials.spells.api.EnorialsSpellsApi$Stats
 */
package sk.enorials.hud.betonquest;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.instruction.Argument;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.integration.IntegrationService;
import org.betonquest.betonquest.api.integration.policy.Policy;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.action.PlayerAction;
import org.betonquest.betonquest.api.quest.condition.PlayerCondition;
import org.betonquest.betonquest.api.service.action.ActionRegistry;
import org.betonquest.betonquest.api.service.condition.ConditionRegistry;
import org.betonquest.betonquest.api.service.objective.ObjectiveRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.gps.api.GpsTargetType;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.betonquest.HudConversations;
import sk.enorials.hud.betonquest.HudQuestLog;
import sk.enorials.hud.betonquest.QuestMarkerObjective;
import sk.enorials.hud.party.PartyMapTracker;
import sk.enorials.partyhud.party.Party;
import sk.enorials.spells.api.EnorialsSpellsApi;

public final class HudBetonQuest
implements AutoCloseable {
    final EnorialsHUD hud;
    final Set<QuestMarkerObjective> objectives = new LinkedHashSet<QuestMarkerObjective>();
    private BukkitTask task;
    private boolean registered;
    private final HudConversations conversations;
    private HudQuestLog questLog;

    public HudQuestLog questLog() {
        return this.questLog;
    }

    public boolean cancelConversation(Player player) {
        return this.conversations.cancel(player);
    }

    public HudBetonQuest(EnorialsHUD enorialsHUD) {
        this.hud = enorialsHUD;
        this.conversations = new HudConversations(enorialsHUD);
    }

    public void register() {
        IntegrationService integrationService = (IntegrationService)Bukkit.getServicesManager().load(IntegrationService.class);
        if (integrationService == null) {
            throw new IllegalStateException("BetonQuest IntegrationService is unavailable");
        }
        integrationService.withPolicies(new Policy[0]).builder().enable(betonQuestApi -> {
            ((ActionRegistry)betonQuestApi.actions().registry()).register("enohud", this::parseAction);
            ((ConditionRegistry)betonQuestApi.conditions().registry()).register("enohud", this::parseCondition);
            ((ObjectiveRegistry)betonQuestApi.objectives().registry()).register("enohud_marker", (instruction, objectiveService) -> new QuestMarkerObjective(this, instruction, objectiveService));
            this.registered = true;
            try {
                this.conversations.register();
            }
            catch (LinkageError | RuntimeException throwable) {
                this.hud.getLogger().warning("BQ conversation window unavailable; keeping original dialogue: " + throwable.getMessage());
            }
        }).postEnable(betonQuestApi -> this.start()).disable(this::close).integrate((Plugin)this.hud);
    }

    public void start() {
        if (!this.registered || this.task != null || !this.hud.isEnabled() || this.hud.api() == null) {
            return;
        }
        this.conversations.start();
        if (this.questLog == null) {
            try {
                this.questLog = new HudQuestLog(this.hud);
            }
            catch (Exception | LinkageError throwable) {
                this.hud.getLogger().warning("Quest log unavailable: " + throwable.getMessage());
            }
        }
        this.task = Bukkit.getScheduler().runTaskTimer((Plugin)this.hud, () -> {
            for (QuestMarkerObjective questMarkerObjective : List.copyOf(this.objectives)) {
                questMarkerObjective.tick();
            }
        }, 1L, 10L);
        this.hud.getLogger().info("BetonQuest HUD actions, conditions and quest-owned markers enabled.");
    }

    Plugin owner() {
        return Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("BetonQuest"));
    }

    static String markerId(String string) {
        return "bq-" + String.valueOf(UUID.nameUUIDFromBytes(string.getBytes(StandardCharsets.UTF_8)));
    }

    private String actionMarkerId(Instruction instruction, String string) {
        return HudBetonQuest.markerId(instruction.getPackage().getQuestPath() + ">manual>" + string);
    }

    private PlayerAction parseAction(Instruction instruction) throws QuestException {
        String string;
        return switch (string = instruction.nextElement().toLowerCase(Locale.ROOT)) {
            case "menu" -> {
                Argument var5_5 = instruction.string().get();
                yield this.action((profile, player) -> this.hud.api().openMenu(player, (String)var5_5.getValue(profile)));
            }
            case "set" -> {
                Argument var5_6 = instruction.string().get();
                Argument var6_11 = instruction.string().get();
                yield this.action((profile, player) -> this.hud.api().setSetting(player, (String)var5_6.getValue(profile), (String)var6_11.getValue(profile)));
            }
            case "gps" -> this.gpsAction(instruction);
            case "marker" -> this.markerAction(instruction);
            case "dungeon" -> {
                if (!instruction.nextElement().equals("start")) {
                    throw new QuestException("Dungeon action: start");
                }
                Argument var5_7 = instruction.string().get();
                yield this.action((profile, player) -> this.hud.dungeons().start(player, (String)var5_7.getValue(profile)));
            }
            case "party" -> {
                String var5_8 = instruction.nextElement().toLowerCase(Locale.ROOT);
                if (!Set.of("create", "join", "invite", "leave", "disband", "promote", "kick").contains(var5_8)) {
                    throw new QuestException("Unknown party action");
                }
                Argument var6_12 = instruction.string().get("target", (Object)"");
                Argument var7_15 = instruction.string().get("type", (Object)"public");
                yield this.action((profile, player) -> {
                    String string2;
                    if (var5_8.equals("disband")) {
                        Party party = this.hud.getPartyService().getPartyOf(player.getUniqueId()).orElse(null);
                        if (party == null) {
                            return;
                        }
                        if (!party.getLeader().equals(player.getUniqueId())) {
                            throw new QuestException("Only the party leader can disband the party");
                        }
                        this.hud.getPartyService().disbandParty(party);
                        return;
                    }
                    String string3 = string2 = var5_8.equals("create") ? (String)var7_15.getValue(profile) : (String)var6_12.getValue(profile);
                    if (var5_8.equals("create") && !Set.of("public", "private").contains(string2)) {
                        throw new QuestException("Party type: public or private");
                    }
                    if (Set.of("join", "invite", "promote", "kick").contains(var5_8) && !string2.matches("[A-Za-z0-9_]{1,16}")) {
                        throw new QuestException("Party target must be an online player name");
                    }
                    this.hud.partyCommand(player, var5_8 + (String)(string2.isEmpty() ? "" : " " + string2));
                });
            }
            case "resource" -> {
                String var5_9 = instruction.nextElement();
                if (!Set.of("set", "add").contains(var5_9)) {
                    throw new QuestException("Resource operation: set or add");
                }
                Argument var6_13 = instruction.string().get();
                Argument var7_16 = instruction.number().get();
                yield this.action((profile, player) -> ResourceActions.resource(player, var5_9, (String)var6_13.getValue(profile), ((Number)var7_16.getValue(profile)).doubleValue()));
            }
            case "stat" -> {
                Argument var5_10 = instruction.string().get();
                Argument var6_14 = instruction.number().inRange((Number)0, (Number)100000).get();
                yield this.action((profile, player) -> ResourceActions.stat(player, (String)var5_10.getValue(profile), (Number)var6_14.getValue(profile)));
            }
            default -> throw new QuestException("Unknown enohud action: " + string);
        };
    }

    private PlayerAction gpsAction(Instruction instruction) throws QuestException {
        GpsTargetType gpsTargetType;
        String string = instruction.nextElement().toLowerCase(Locale.ROOT);
        if (string.equals("stop")) {
            return this.action((profile, player) -> this.hud.gps().api().stopNavigation(player, "quest"));
        }
        if (string.equals("here")) {
            Argument argument = instruction.number().get();
            Argument argument2 = instruction.number().get();
            Argument argument3 = instruction.number().get();
            return this.action((profile, player) -> {
                Location location = new Location(player.getWorld(), ((Number)argument.getValue(profile)).doubleValue(), ((Number)argument2.getValue(profile)).doubleValue(), ((Number)argument3.getValue(profile)).doubleValue());
                if (!this.hud.gps().api().startNavigation(player, RouteDescriptor.location(location), this.hud.gps().api().getPlayerMode(player), "betonquest")) {
                    throw new QuestException("GPS could not start for this destination");
                }
            });
        }
        if (string.equals("start")) {
            Argument argument = instruction.location().get();
            return this.action((profile, player) -> {
                if (!this.hud.gps().api().startNavigation(player, RouteDescriptor.location((Location)argument.getValue(profile)), this.hud.gps().api().getPlayerMode(player), "betonquest")) {
                    throw new QuestException("GPS could not start for this destination");
                }
            });
        }
        try {
            gpsTargetType = GpsTargetType.valueOf(string.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new QuestException("GPS action: start, player, poi, npc, mob or stop", (Throwable)illegalArgumentException);
        }
        Argument argument = instruction.string().get();
        return this.action((profile, player) -> {
            if (!this.hud.gps().api().startNavigation(player, gpsTargetType, (String)argument.getValue(profile))) {
                throw new QuestException("GPS target unavailable");
            }
        });
    }

    private PlayerAction markerAction(Instruction instruction) throws QuestException {
        String string = instruction.nextElement();
        Argument argument = instruction.string().get();
        if (string.equals("remove")) {
            return this.action((profile, player) -> this.hud.api().removeMarker(this.owner(), player, this.actionMarkerId(instruction, (String)argument.getValue(profile))));
        }
        if (!string.equals("add")) {
            throw new QuestException("Marker action: add or remove");
        }
        Argument argument2 = instruction.location().get();
        Argument argument3 = instruction.string().get();
        Argument argument4 = instruction.string().get("label", (Object)"Quest");
        return this.action((profile, player) -> this.hud.api().putMarker(this.owner(), player, this.actionMarkerId(instruction, (String)argument.getValue(profile)), (Location)argument2.getValue(profile), (String)argument3.getValue(profile), (String)argument4.getValue(profile)));
    }

    private PlayerCondition parseCondition(Instruction instruction) throws QuestException {
        return switch (instruction.nextElement().toLowerCase(Locale.ROOT)) {
            case "setting" -> {
                Argument var4_4 = instruction.string().get();
                Argument var5_8 = instruction.string().get();
                yield this.condition((profile, player) -> this.hud.api().getSetting(player, (String)var4_4.getValue(profile)).equalsIgnoreCase((String)var5_8.getValue(profile)));
            }
            case "gps" -> {
                if (!instruction.nextElement().equals("active")) {
                    throw new QuestException("GPS condition: active");
                }
                yield this.condition((profile, player) -> this.hud.gps().api().hasActiveNavigation(player));
            }
            case "party" -> {
                String var4_5 = instruction.nextElement();
                if (var4_5.equals("size")) {
                    Argument var5_9 = instruction.number().inRange((Number)1, (Number)100).get();
                    yield this.condition((profile, player) -> {
                        int n = ((Number)var5_9.getValue(profile)).intValue();
                        return this.hud.getPartyService().getPartyOf(player.getUniqueId()).map(party -> PartyMapTracker.members(party).size() >= n).orElse(false);
                    });
                }
                if (!Set.of("joined", "leader").contains(var4_5)) {
                    throw new QuestException("Party condition: joined, leader, size");
                }
                yield this.condition((profile, player) -> this.hud.getPartyService().getPartyOf(player.getUniqueId()).map(party -> var4_5.equals("joined") || party.getLeader().equals(player.getUniqueId())).orElse(false));
            }
            case "marker" -> {
                Argument var4_6 = instruction.string().get();
                yield this.condition((profile, player) -> this.hud.api().hasMarker(this.owner(), player, this.actionMarkerId(instruction, (String)var4_6.getValue(profile))));
            }
            case "near" -> {
                Argument var4_7 = instruction.location().get();
                Argument var5_10 = instruction.number().inRange((Number)0, (Number)100000).get();
                yield this.condition((profile, player) -> HudBetonQuest.near(player, (Location)var4_7.getValue(profile), ((Number)var5_10.getValue(profile)).doubleValue()));
            }
            default -> throw new QuestException("Unknown enohud condition");
        };
    }

    static boolean near(Player player, Location location, double d) {
        return Double.isFinite(d) && location.getWorld() != null && player.getWorld().equals((Object)location.getWorld()) && player.getLocation().distanceSquared(location) <= d * d;
    }

    private PlayerAction action(final Action action) {
        return new PlayerAction(){
            final /* synthetic */ HudBetonQuest this$0;
            {
                this.this$0 = hudBetonQuest;
            }

            public boolean isPrimaryThreadEnforced() {
                return true;
            }

            public void execute(Profile profile) throws QuestException {
                Optional optional = profile.getOnlineProfile();
                if (optional.isEmpty()) {
                    return;
                }
                try {
                    action.run(profile, ((OnlineProfile)optional.get()).getPlayer());
                }
                catch (RuntimeException runtimeException) {
                    throw new QuestException("Enorials HUD: " + runtimeException.getMessage(), (Throwable)runtimeException);
                }
            }
        };
    }

    private PlayerCondition condition(final Condition condition) {
        return new PlayerCondition(){
            final /* synthetic */ HudBetonQuest this$0;
            {
                this.this$0 = hudBetonQuest;
            }

            public boolean isPrimaryThreadEnforced() {
                return true;
            }

            public boolean check(Profile profile) throws QuestException {
                Optional optional = profile.getOnlineProfile();
                if (optional.isEmpty()) {
                    return false;
                }
                try {
                    return condition.check(profile, ((OnlineProfile)optional.get()).getPlayer());
                }
                catch (RuntimeException runtimeException) {
                    throw new QuestException("Enorials HUD: " + runtimeException.getMessage(), (Throwable)runtimeException);
                }
            }
        };
    }

    @Override
    public void close() {
        this.conversations.close();
        if (this.questLog != null) {
            this.questLog.close();
            this.questLog = null;
        }
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        for (QuestMarkerObjective questMarkerObjective : List.copyOf(this.objectives)) {
            questMarkerObjective.close();
        }
        if (this.hud.api() != null) {
            for (QuestMarkerObjective questMarkerObjective : Bukkit.getOnlinePlayers()) {
                this.hud.api().clearMarkers(this.owner(), (Player)questMarkerObjective);
            }
        }
    }

    @FunctionalInterface
    private static interface Action {
        public void run(Profile var1, Player var2) throws QuestException;
    }

    @FunctionalInterface
    private static interface Condition {
        public boolean check(Profile var1, Player var2) throws QuestException;
    }

    private static final class ResourceActions {
        private ResourceActions() {
        }

        private static EnorialsSpellsApi api() throws QuestException {
            if (!Bukkit.getPluginManager().isPluginEnabled("EnorialsSpells")) {
                throw new QuestException("This action needs EnorialsSpells");
            }
            EnorialsSpellsApi enorialsSpellsApi = (EnorialsSpellsApi)Bukkit.getServicesManager().load(EnorialsSpellsApi.class);
            if (enorialsSpellsApi == null) {
                throw new QuestException("Spells resource API unavailable");
            }
            return enorialsSpellsApi;
        }

        static void resource(Player player, String string, String string2, double d) throws QuestException {
            if (!Double.isFinite(d)) {
                throw new QuestException("Resource amount must be finite");
            }
            if (string.equals("set")) {
                ResourceActions.api().setResource(player, string2, d);
            } else {
                ResourceActions.api().addResource(player, string2, d);
            }
        }

        static void stat(Player player, String string, Number number) throws QuestException {
            double d = number.doubleValue();
            if (!Double.isFinite(d) || d != Math.rint(d) || d < 0.0 || d > 100000.0) {
                throw new QuestException("Stat value must be an integer in 0..100000");
            }
            EnorialsSpellsApi enorialsSpellsApi = ResourceActions.api();
            EnorialsSpellsApi.Stats stats = enorialsSpellsApi.getStats(player);
            int n = (int)d;
            if (!Set.of("strength", "agility", "mana-control", "stealth", "nature").contains(string)) {
                throw new QuestException("Unknown Spells stat");
            }
            enorialsSpellsApi.setStats(player, new EnorialsSpellsApi.Stats(string.equals("strength") ? n : stats.strength(), string.equals("agility") ? n : stats.agility(), string.equals("mana-control") ? n : stats.manaControl(), string.equals("stealth") ? n : stats.stealth(), string.equals("nature") ? n : stats.nature()));
        }
    }
}

