/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.betonquest.betonquest.BetonQuest
 *  org.betonquest.betonquest.api.BetonQuestApi
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.bukkit.event.PlayerJournalAddEvent
 *  org.betonquest.betonquest.api.bukkit.event.PlayerJournalDeleteEvent
 *  org.betonquest.betonquest.api.bukkit.event.PlayerObjectiveChangeEvent
 *  org.betonquest.betonquest.api.bukkit.event.PlayerTagAddEvent
 *  org.betonquest.betonquest.api.bukkit.event.PlayerTagRemoveEvent
 *  org.betonquest.betonquest.api.config.quest.QuestPackage
 *  org.betonquest.betonquest.api.config.section.multi.MultiConfiguration
 *  org.betonquest.betonquest.api.data.PersistentDataHolder
 *  org.betonquest.betonquest.api.identifier.ConditionIdentifier
 *  org.betonquest.betonquest.api.identifier.JournalEntryIdentifier
 *  org.betonquest.betonquest.api.instruction.Argument
 *  org.betonquest.betonquest.api.profile.OnlineProfile
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.reload.ReloadPhase
 *  org.betonquest.betonquest.api.service.objective.ObjectiveManager
 *  org.betonquest.betonquest.api.text.Text
 *  org.betonquest.betonquest.database.PlayerData
 *  org.betonquest.betonquest.feature.journal.Pointer
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.hud.betonquest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.bukkit.event.PlayerJournalAddEvent;
import org.betonquest.betonquest.api.bukkit.event.PlayerJournalDeleteEvent;
import org.betonquest.betonquest.api.bukkit.event.PlayerObjectiveChangeEvent;
import org.betonquest.betonquest.api.bukkit.event.PlayerTagAddEvent;
import org.betonquest.betonquest.api.bukkit.event.PlayerTagRemoveEvent;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.config.section.multi.MultiConfiguration;
import org.betonquest.betonquest.api.data.PersistentDataHolder;
import org.betonquest.betonquest.api.identifier.ConditionIdentifier;
import org.betonquest.betonquest.api.identifier.JournalEntryIdentifier;
import org.betonquest.betonquest.api.instruction.Argument;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.reload.ReloadPhase;
import org.betonquest.betonquest.api.service.objective.ObjectiveManager;
import org.betonquest.betonquest.api.text.Text;
import org.betonquest.betonquest.database.PlayerData;
import org.betonquest.betonquest.feature.journal.Pointer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.api.event.GpsNavigationStopEvent;
import sk.enorials.gps.integration.IntegrationManager;
import sk.enorials.gps.nav.NavigationSession;
import sk.enorials.gps.nav.RouteDescriptor;
import sk.enorials.gps.nav.target.TargetLocator;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.betonquest.QuestDestinations;
import sk.enorials.hud.betonquest.QuestLogSources;
import sk.enorials.hud.betonquest.QuestLogWindow;
import sk.enorials.hud.protocol.QuestSidebar;
import sk.enorials.hud.util.PackHostingSetup;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapScreenPosition;

public final class HudQuestLog
implements Listener,
AutoCloseable {
    private static final Pattern TITLE = Pattern.compile("(?im)(?:Daily\\s+)?Quest\\s+name\\s*:[ \\t]*(?:\\R[ \\t]*)?([^\\r\\n]+)");
    private static final Pattern TODO = Pattern.compile("(?im)^\\s*(?:To do|Go to|Chest location)\\s*:\\s*");
    private static final Pattern OBJECTIVE = Pattern.compile("(?im)Quest\\s+objective\\s*:\\s*");
    private final EnorialsHUD hud;
    private final BetonQuestApi api;
    private final QuestLogWindow window;
    private final QuestSidebar sidebar;
    private final QuestLogSources sources;
    private final QuestDestinations destinations;
    private final Map<UUID, State> players = new HashMap<UUID, State>();
    private final Map<String, Text> texts = new HashMap<String, Text>();
    private final Map<String, InlineText> inlineTexts = new HashMap<String, InlineText>();
    private final Map<String, String> journalRoutes = new HashMap<String, String>();
    private final Set<String> warned = new HashSet<String>();
    private final Set<UUID> refreshQueued = ConcurrentHashMap.newKeySet();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "enorials-quest-log-save");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<UUID, String> pendingWrites = new ConcurrentHashMap<UUID, String>();
    private BukkitTask task;
    private int ticks;
    private volatile boolean closed;

    public HudQuestLog(EnorialsHUD enorialsHUD) throws IOException {
        this.hud = enorialsHUD;
        this.api = BetonQuest.getInstance().getBetonQuestApi();
        this.sources = new QuestLogSources(enorialsHUD, this.api, this);
        this.destinations = new QuestDestinations(this.api, this, enorialsHUD);
        this.window = new QuestLogWindow(enorialsHUD.getResource("dialogue-widths.properties"));
        this.sidebar = new QuestSidebar((Plugin)enorialsHUD);
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
        this.api.reloader().register(ReloadPhase.POST, () -> {
            if (!this.closed) {
                if (Bukkit.isPrimaryThread()) {
                    this.reloadDefinitions();
                } else {
                    Bukkit.getScheduler().runTask((Plugin)enorialsHUD, this::reloadDefinitions);
                }
            }
        });
        this.reloadDefinitions();
        this.task = Bukkit.getScheduler().runTaskTimer((Plugin)enorialsHUD, this::tick, 20L, 10L);
    }

    public static boolean available(EnorialsHUD enorialsHUD) {
        return Bukkit.getPluginManager().isPluginEnabled("BetonQuest") && enorialsHUD.cfg().getBoolean("quest-log.enabled", true);
    }

    public static boolean selected(EnorialsHUD enorialsHUD, Player player) {
        return HudAccess.has((CommandSender)player, "enorials.hud.questlog") && HudAccess.hud(player) && enorialsHUD.storage().preference(player.getUniqueId(), "quest-log", enorialsHUD.cfg().getBoolean("quest-log.default-visible", true));
    }

    public List<QuestLogWindow.Entry> entries(Player player) {
        State state = this.players.get(player.getUniqueId());
        return state == null ? List.of() : state.entries;
    }

    public List<NpcHint> npcHints(Player player) {
        State state = this.players.get(player.getUniqueId());
        return state == null ? List.of() : state.hints.stream().map(hint -> new NpcHint(hint.key(), hint.location().clone(), hint.title(), hint.available(), hint.complete(), hint.incomplete())).toList();
    }

    public boolean tracking(Player player) {
        State state = this.players.get(player.getUniqueId());
        return state != null && !state.selected.isEmpty();
    }

    public void stopTracking(Player player) {
        State state = this.players.get(player.getUniqueId());
        if (state != null) {
            state.selected = "";
            state.lastDestination = "";
        }
    }

    public void navigationStarted(Player player, String string) {
        State state = this.players.get(player.getUniqueId());
        if (state == null || state.selected.isEmpty()) {
            return;
        }
        Stage stage2 = state.stages.stream().filter(stage -> stage.key().equals(state.selected)).findFirst().orElse(null);
        if (stage2 == null || !string.equals("betonquest-quest:" + (stage2.gps().isEmpty() ? stage2.key() : stage2.gps()))) {
            this.stopTracking(player);
        }
    }

    public void select(Player player, String string) throws Exception {
        HudAccess.require((CommandSender)player, "enorials.gps.quests");
        this.refresh(player);
        State state = this.players.get(player.getUniqueId());
        if (state.stages.stream().noneMatch(stage -> stage.key().equals(string))) {
            throw new IllegalArgumentException("This quest is no longer accepted.");
        }
        if (this.hud.gps().navigation().hasActiveNavigation(player)) {
            this.hud.gps().navigation().stopNavigation(player, "quest-selection");
        }
        state.selected = string;
        state.lastDestination = "";
        this.syncNavigation(player, state);
        if (!state.destinations.containsKey(string)) {
            player.sendMessage("Quest selected. This stage has no fixed destination; follow its objective in the quest log.");
        }
        this.refresh(player);
    }

    private void syncNavigation(Player player, State state) {
        if (state.selected.isEmpty() || this.hud.gps() == null) {
            return;
        }
        final Stage stage2 = state.stages.stream().filter(stage -> stage.key().equals(state.selected)).findFirst().orElse(null);
        if (stage2 == null) {
            if (this.hud.gps().navigation().session(player) != null && this.hud.gps().navigation().session(player).isTrackedQuest()) {
                this.hud.gps().navigation().stopNavigation(player, "quest-ended");
            }
            this.stopTracking(player);
            return;
        }
        Location location = state.destinations.get(state.selected);
        if (location == null) {
            NavigationSession navigationSession = this.hud.gps().navigation().session(player);
            if (navigationSession != null && navigationSession.isTrackedQuest()) {
                this.hud.gps().navigation().stopNavigation(player, "quest-stage");
            }
            state.lastDestination = "";
            return;
        }
        String string = String.valueOf(location.getWorld().getUID()) + "|" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        NavigationSession navigationSession = this.hud.gps().navigation().session(player);
        if (navigationSession != null && navigationSession.locator().id().equals("quest:" + stage2.key())) {
            state.lastDestination = string;
            return;
        }
        if (string.equals(state.lastDestination)) {
            return;
        }
        final String string2 = stage2.key();
        TargetLocator targetLocator = new TargetLocator(){
            final /* synthetic */ HudQuestLog this$0;
            {
                this.this$0 = hudQuestLog;
            }

            @Override
            public String id() {
                return "quest:" + string2;
            }

            @Override
            public String displayName() {
                return stage2.title();
            }

            @Override
            public Location resolve(Player player) {
                State state = this.this$0.players.get(player.getUniqueId());
                Location location = state == null ? null : state.destinations.get(string2);
                return location == null ? null : location.clone();
            }
        };
        if (this.hud.gps().navigation().startNavigation(player, targetLocator, NavigationMode.HUD, "betonquest-quest:" + (stage2.gps().isEmpty() ? string2 : stage2.gps()), RouteDescriptor.location(location))) {
            state.lastDestination = string;
        }
    }

    public void reloadDefinitions() {
        this.texts.clear();
        this.inlineTexts.clear();
        this.journalRoutes.clear();
        this.warned.clear();
        this.sources.reload();
        this.destinations.reload();
        if (this.hud.gps() == null) {
            return;
        }
        ConfigurationSection configurationSection = this.hud.gps().config().getConfigurationSection("betonquest.auto_routes");
        if (configurationSection == null) {
            return;
        }
        for (String string : configurationSection.getKeys(false)) {
            ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection(string);
            if (configurationSection2 == null) continue;
            for (String string2 : configurationSection2.getStringList("conditions")) {
                if (!string2.startsWith("betonquest:") || string2.startsWith("betonquest:!")) continue;
                try {
                    ConditionIdentifier conditionIdentifier = (ConditionIdentifier)this.api.identifiers().getFactory(ConditionIdentifier.class).parseIdentifier(null, string2.substring(11));
                    String[] stringArray = conditionIdentifier.getPackage().getConfig().getString("conditions." + conditionIdentifier.get(), "").trim().split("\\s+");
                    if (stringArray.length < 2 || !stringArray[0].equalsIgnoreCase("journal")) continue;
                    JournalEntryIdentifier journalEntryIdentifier = (JournalEntryIdentifier)this.api.identifiers().getFactory(JournalEntryIdentifier.class).parseIdentifier(conditionIdentifier.getPackage(), stringArray[1]);
                    this.journalRoutes.put(journalEntryIdentifier.getFull(), configurationSection2.getString("quest", string));
                }
                catch (Exception exception) {
                    this.warn("route:" + string, exception);
                }
            }
        }
    }

    String render(QuestPackage questPackage, String string, Profile profile2) throws QuestException {
        String string2 = questPackage.getQuestPath() + ">" + string;
        Object object = questPackage.getConfig().get(string);
        if (object instanceof String) {
            String string3 = (String)object;
            object = this.inlineTexts.get(string2);
            if (object == null) {
                Argument argument = this.api.instructions().createForArgument(questPackage, string3).component().get();
                object = profile -> HudQuestLog.clean(PlainTextComponentSerializer.plainText().serialize((Component)argument.getValue(profile)));
                this.inlineTexts.put(string2, (InlineText)object);
            }
            return object.read(profile2);
        }
        Text text = this.texts.get(string2);
        if (text == null) {
            text = (Text)this.api.instructions().createSection(questPackage, (ConfigurationSection)questPackage.getConfig()).read().value(string.split("\\.")).translationSection().get().getValue(profile2);
            this.texts.put(string2, text);
        }
        return HudQuestLog.clean(PlainTextComponentSerializer.plainText().serialize(text.asComponent(profile2)));
    }

    public static String clean(String string) {
        return string.replace("\\n", "\n").replaceAll("(?i)[&\u00a7][0-9A-FK-ORX]", "").replace("\u25ba", "").replace("\u25c4", "").replace('\r', '\n').strip();
    }

    public static String title(String string) {
        Matcher matcher = TITLE.matcher(string);
        return matcher.find() ? matcher.group(1).strip() : null;
    }

    public static String objective(String string) {
        Matcher matcher;
        Matcher matcher2 = TODO.matcher(string);
        String string2 = matcher2.find() ? string.substring(matcher2.end()) : ((matcher = OBJECTIVE.matcher(string)).find() ? string.substring(matcher.end()) : string);
        return string2.replaceAll("\\s+", " ").strip();
    }

    private Path file(UUID uUID) {
        return this.hud.getDataFolder().toPath().resolve("quest-log-history").resolve(String.valueOf(uUID) + ".yml");
    }

    private State state(Player player, Profile profile) {
        State state = this.players.get(player.getUniqueId());
        if (state != null && state.profile.equals(profile.getProfileUUID())) {
            return state;
        }
        if (state != null) {
            this.save(state);
        }
        state = new State();
        state.profile = profile.getProfileUUID();
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)this.file(state.profile).toFile());
        for (String string : yamlConfiguration.getKeys(false)) {
            try {
                state.order.put(new String(Base64.getUrlDecoder().decode(string), StandardCharsets.UTF_8), new Stamp(yamlConfiguration.getLong(string)));
            }
            catch (IllegalArgumentException illegalArgumentException) {}
        }
        this.players.put(player.getUniqueId(), state);
        return state;
    }

    private List<Stage> stages(Player player, Profile profile) throws Exception {
        Object object;
        LinkedHashMap<Object, Stage> linkedHashMap = new LinkedHashMap<Object, Stage>();
        PersistentDataHolder persistentDataHolder = this.api.persistence().of(profile);
        HashMap<String, Pointer> hashMap = new HashMap<String, Pointer>();
        if (persistentDataHolder instanceof PlayerData) {
            object = (PlayerData)persistentDataHolder;
            for (Pointer object2 : List.copyOf(object.getEntries())) {
                hashMap.put(object2.pointer().getFull(), object2);
            }
        }
        object = this.sources.claimed();
        for (Stage stage : this.sources.read(profile, hashMap)) {
            linkedHashMap.put(stage.key(), stage);
        }
        for (Pointer pointer : hashMap.values()) {
            if (object.contains(pointer.pointer().getFull())) continue;
            try {
                String string;
                Object object2;
                String string2;
                JournalEntryIdentifier journalEntryIdentifier = pointer.pointer();
                String string3 = this.render(journalEntryIdentifier.getPackage(), "journal." + journalEntryIdentifier.get(), profile);
                String string4 = HudQuestLog.title(string3);
                if (string4 == null) continue;
                MultiConfiguration multiConfiguration = journalEntryIdentifier.getPackage().getConfig();
                String string5 = multiConfiguration.getString("journal." + journalEntryIdentifier.get());
                if (string5 == null && (string2 = multiConfiguration.getConfigurationSection("journal." + journalEntryIdentifier.get())) != null) {
                    object2 = string2.getKeys(false).iterator();
                    while (object2.hasNext() && (string5 = string2.getString(string = (String)object2.next())) == null) {
                    }
                }
                string2 = string5 == null ? string4 : Objects.requireNonNullElse(HudQuestLog.title(HudQuestLog.clean(string5)), string4);
                object2 = journalEntryIdentifier.getPackage().getQuestPath() + ">title:" + string2.toLowerCase(Locale.ROOT);
                string = this.journalRoutes.getOrDefault(journalEntryIdentifier.getFull(), "");
                Stage stage = (Stage)linkedHashMap.get(object2);
                if (stage != null && stage.timestamp() > pointer.timestamp()) continue;
                linkedHashMap.put(object2, new Stage((String)object2, string4, HudQuestLog.objective(string3), pointer.timestamp(), string, List.of(), List.of(journalEntryIdentifier.getFull()), ""));
            }
            catch (Exception exception) {
                this.warn("journal:" + pointer.pointer().getFull(), exception);
            }
        }
        return List.copyOf(linkedHashMap.values());
    }

    private void tick() {
        if (this.closed) {
            return;
        }
        ++this.ticks;
        if (this.ticks % 120 == 0) {
            this.reloadDefinitions();
        }
        boolean bl = PackHostingSetup.ready() && HudQuestLog.available(this.hud);
        for (Player object : Bukkit.getOnlinePlayers()) {
            try {
                int n;
                if (!PackHostingSetup.ready(object) || this.hud.packLoadGuard() != null && this.hud.packLoadGuard().isWaiting(object)) {
                    this.hide(object);
                    continue;
                }
                OnlineProfile onlineProfile = this.api.profiles().getProfile(object);
                State state = this.state(object, (Profile)onlineProfile);
                if (this.ticks % 2 == 0 || state.entries.isEmpty()) {
                    this.refresh(object, state, (Profile)onlineProfile);
                }
                boolean bl2 = bl && HudQuestLog.selected(this.hud, object) && this.hud.storage().preference(object.getUniqueId(), "hud", true);
                this.sidebar.hide(object, bl2);
                Minimap minimap = this.hud.getPlayerMinimap(object);
                int n2 = n = minimap != null && minimap.screenPosition() == MinimapScreenPosition.RIGHT ? minimap.size().ordinal() + 1 + (minimap.coordinatesEnabled() ? 3 : 0) : 0;
                Object object2 = bl2 && !object.hasMetadata("enorials.conversation") && this.hud.getFullscreenMinimap(object) == null ? this.window.render(state.entries.subList(0, Math.min(5, state.entries.size())), n, this.hud.storage().questLogSize(object.getUniqueId())) : Component.empty();
                if (object2.equals((Object)state.painted)) continue;
                state.painted = object2;
                this.hud.renderer().setQuestLogOverlay(object, (Component)object2);
            }
            catch (Exception | LinkageError throwable) {
                this.hide(object);
                this.warn("player:" + String.valueOf(object.getUniqueId()), throwable);
            }
        }
        if (this.ticks % 10 == 0) {
            for (State state : this.players.values()) {
                if (!state.dirty) continue;
                this.save(state);
            }
        }
    }

    public void refresh(Player player) throws Exception {
        OnlineProfile onlineProfile = this.api.profiles().getProfile(player);
        this.refresh(player, this.state(player, (Profile)onlineProfile), (Profile)onlineProfile);
    }

    private void refresh(Player player, State state, Profile profile) throws Exception {
        Object object;
        Pointer pointer22;
        Object object3;
        Object object422;
        List<Stage> list = this.stages(player, profile);
        long l = System.currentTimeMillis();
        HashSet<String> hashSet = new HashSet<String>();
        ArrayList<QuestLogWindow.Entry> arrayList = new ArrayList<QuestLogWindow.Entry>();
        state.stages = list;
        this.destinations.begin();
        HashSet<String> hashSet2 = new HashSet<String>();
        HashSet<String> hashSet3 = new HashSet<String>();
        for (Object object422 : ((ObjectiveManager)this.api.objectives().manager()).getForProfile(profile)) {
            hashSet2.add(object422.getObjectiveID().getFull());
        }
        object422 = this.api.persistence().of(profile);
        if (object422 instanceof PlayerData) {
            object3 = (PlayerData)object422;
            for (Pointer pointer22 : List.copyOf(object3.getEntries())) {
                hashSet3.add(pointer22.pointer().getFull());
            }
        }
        object3 = new HashMap();
        object422 = this.hud.gps() == null ? Map.of() : this.hud.gps().navigation().integrations().getAutoRoutes(player);
        for (Stage object22 : list) {
            object = this.destinations.target(object22, profile, hashSet2, hashSet3);
            if (object == null && !object22.gps().isEmpty()) {
                for (Object object2 : object422.values()) {
                    if (!((IntegrationManager.AutoRoute)object2).source().equals("betonquest-quest:" + object22.gps())) continue;
                    Optional<TargetLocator> optional = this.hud.gps().navigation().integrations().resolveAutoRoute(player, (IntegrationManager.AutoRoute)object2);
                    if (!optional.isPresent()) break;
                    object = optional.get().resolve(player);
                    break;
                }
            }
            if (object == null || object.getWorld() == null) continue;
            object3.put(object22.key(), object.clone());
        }
        state.destinations = Map.copyOf(object3);
        state.hints = this.destinations.hints(profile, hashSet2, hashSet3);
        this.syncNavigation(player, state);
        pointer22 = this.hud.gps() == null ? null : this.hud.gps().navigation().session(player);
        String string = pointer22 != null && pointer22.isTrackedQuest() ? pointer22.source().substring("betonquest-quest:".length()) : "";
        for (Stage stage : list) {
            Object object2;
            hashSet.add(stage.key());
            object2 = state.order.get(stage.key());
            if (object2 == null) {
                object2 = new Stamp(Math.min(l, stage.timestamp()));
                state.order.put(stage.key(), (Stamp)object2);
                state.dirty = true;
            }
            ((Stamp)object2).missing = 0L;
            arrayList.add(new QuestLogWindow.Entry(stage.key(), stage.title(), stage.objective(), ((Stamp)object2).accepted, stage.key().equals(state.selected) || !string.isEmpty() && string.equals(stage.gps())));
        }
        object = state.order.entrySet().iterator();
        while (object.hasNext()) {
            Map.Entry entry = (Map.Entry)object.next();
            if (hashSet.contains(entry.getKey())) continue;
            if (((Stamp)entry.getValue()).missing == 0L) {
                ((Stamp)entry.getValue()).missing = l;
            }
            if (l - ((Stamp)entry.getValue()).missing < 3000L) continue;
            object.remove();
            state.dirty = true;
        }
        arrayList.sort(Comparator.comparingLong(QuestLogWindow.Entry::accepted).reversed().thenComparing(QuestLogWindow.Entry::key));
        state.entries = List.copyOf(arrayList);
    }

    private void hide(Player player) {
        this.sidebar.hide(player, false);
        State state = this.players.get(player.getUniqueId());
        if (state != null) {
            state.painted = Component.empty();
        }
        this.hud.renderer().setQuestLogOverlay(player, (Component)Component.empty());
    }

    private void changed(Profile profile) {
        UUID uUID = profile.getPlayerUUID();
        if (this.closed || !this.refreshQueued.add(uUID)) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this.hud, () -> {
            this.refreshQueued.remove(uUID);
            Player player = Bukkit.getPlayer((UUID)uUID);
            if (this.closed || player == null || !HudQuestLog.available(this.hud)) {
                return;
            }
            try {
                this.refresh(player);
            }
            catch (Exception exception) {
                this.warn("refresh:" + String.valueOf(uUID), exception);
            }
        });
    }

    @EventHandler
    public void journal(PlayerJournalAddEvent playerJournalAddEvent) {
        this.changed(playerJournalAddEvent.getProfile());
    }

    @EventHandler
    public void journal(PlayerJournalDeleteEvent playerJournalDeleteEvent) {
        this.changed(playerJournalDeleteEvent.getProfile());
    }

    @EventHandler
    public void objective(PlayerObjectiveChangeEvent playerObjectiveChangeEvent) {
        this.changed(playerObjectiveChangeEvent.getProfile());
    }

    @EventHandler
    public void tag(PlayerTagAddEvent playerTagAddEvent) {
        this.changed(playerTagAddEvent.getProfile());
    }

    @EventHandler
    public void tag(PlayerTagRemoveEvent playerTagRemoveEvent) {
        this.changed(playerTagRemoveEvent.getProfile());
    }

    @EventHandler
    public void navigationStopped(GpsNavigationStopEvent gpsNavigationStopEvent) {
        if (!Set.of("arrived", "quest-stage", "quest-selection").contains(gpsNavigationStopEvent.getReason())) {
            this.stopTracking(gpsNavigationStopEvent.getPlayer());
        }
    }

    private void save(State state) {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        state.order.forEach((string, stamp) -> yamlConfiguration.set(Base64.getUrlEncoder().withoutPadding().encodeToString(string.getBytes(StandardCharsets.UTF_8)), (Object)stamp.accepted));
        state.dirty = false;
        UUID uUID = state.profile;
        String string2 = yamlConfiguration.saveToString();
        String string3 = this.pendingWrites.put(uUID, string2);
        if (string3 != null) {
            return;
        }
        this.writer.execute(() -> {
            String string;
            while ((string = this.pendingWrites.remove(uUID)) != null) {
                try {
                    Path path = this.file(uUID);
                    Files.createDirectories(path.getParent(), new FileAttribute[0]);
                    Path path2 = Files.createTempFile(path.getParent(), uUID.toString(), ".tmp", new FileAttribute[0]);
                    try {
                        Files.writeString(path2, (CharSequence)string, StandardCharsets.UTF_8, new OpenOption[0]);
                        try {
                            Files.move(path2, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                        }
                        catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
                            Files.move(path2, path, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    finally {
                        Files.deleteIfExists(path2);
                    }
                }
                catch (Exception exception) {
                    this.hud.getLogger().warning("Cannot save quest-log history: " + exception.getMessage());
                }
            }
        });
    }

    void warn(String string, Throwable throwable) {
        if (this.warned.add(string)) {
            this.hud.getLogger().warning("Quest log skipped " + string + ": " + throwable.getMessage());
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        State state = this.players.remove(playerQuitEvent.getPlayer().getUniqueId());
        if (state != null) {
            this.save(state);
        }
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.task != null) {
            this.task.cancel();
        }
        Bukkit.getOnlinePlayers().forEach(this::hide);
        this.sidebar.close();
        HandlerList.unregisterAll((Listener)this);
        this.players.values().forEach(this::save);
        this.players.clear();
        this.writer.shutdown();
        try {
            if (!this.writer.awaitTermination(5L, TimeUnit.SECONDS)) {
                this.hud.getLogger().warning("Quest-log history save is still finishing.");
            }
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class State {
        UUID profile;
        final Map<String, Stamp> order = new HashMap<String, Stamp>();
        List<QuestLogWindow.Entry> entries = List.of();
        List<Stage> stages = List.of();
        Map<String, Location> destinations = Map.of();
        List<QuestDestinations.Hint> hints = List.of();
        String selected = "";
        String lastDestination = "";
        Component painted = Component.empty();
        boolean dirty;

        private State() {
        }
    }

    record Stage(String key, String title, String objective, long timestamp, String gps, List<String> objectives, List<String> journals, String compass) {
        Stage(String string, String string2, String string3, long l, String string4) {
            this(string, string2, string3, l, string4, List.of(), List.of(), "");
        }
    }

    @FunctionalInterface
    private static interface InlineText {
        public String read(Profile var1) throws QuestException;
    }

    private static final class Stamp {
        long accepted;
        long missing;

        Stamp(long l) {
            this.accepted = l;
        }
    }

    public record NpcHint(String key, Location location, String title, boolean available, boolean complete, boolean incomplete) {
    }
}

