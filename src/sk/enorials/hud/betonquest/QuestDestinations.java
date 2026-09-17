/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.betonquest.betonquest.api.BetonQuestApi
 *  org.betonquest.betonquest.api.compass.QuestCompass
 *  org.betonquest.betonquest.api.config.quest.QuestPackage
 *  org.betonquest.betonquest.api.identifier.ActionIdentifier
 *  org.betonquest.betonquest.api.identifier.CompassIdentifier
 *  org.betonquest.betonquest.api.identifier.ConditionIdentifier
 *  org.betonquest.betonquest.api.identifier.ConversationIdentifier
 *  org.betonquest.betonquest.api.identifier.JournalEntryIdentifier
 *  org.betonquest.betonquest.api.identifier.ObjectiveIdentifier
 *  org.betonquest.betonquest.api.profile.OnlineProfile
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.service.condition.ConditionManager
 *  org.bukkit.Location
 *  org.bukkit.configuration.ConfigurationSection
 */
package sk.enorials.hud.betonquest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.compass.QuestCompass;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.identifier.ActionIdentifier;
import org.betonquest.betonquest.api.identifier.CompassIdentifier;
import org.betonquest.betonquest.api.identifier.ConditionIdentifier;
import org.betonquest.betonquest.api.identifier.ConversationIdentifier;
import org.betonquest.betonquest.api.identifier.JournalEntryIdentifier;
import org.betonquest.betonquest.api.identifier.ObjectiveIdentifier;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.service.condition.ConditionManager;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.betonquest.HudQuestLog;
import sk.enorials.hud.betonquest.QuestLogSources;
import sk.enorials.minimap.quest.BetonQuestNpcResolver;
import sk.enorials.minimap.quest.QuestMarkerMappings;

final class QuestDestinations {
    private final BetonQuestApi api;
    private final HudQuestLog log;
    private final EnorialsHUD hud;
    private final BetonQuestNpcResolver npcs;
    private final List<Conversation> conversations = new ArrayList<Conversation>();
    private final Map<String, Set<Signal>> actionSignals = new HashMap<String, Set<Signal>>();
    private final Set<String> questObjectives = new HashSet<String>();
    private final Set<String> questJournals = new HashSet<String>();
    private final Map<String, Set<Signal>> conversationSignals = new HashMap<String, Set<Signal>>();
    private final Map<String, Boolean> conditionResults = new HashMap<String, Boolean>();
    private final Map<String, Set<Signal>> handInResults = new HashMap<String, Set<Signal>>();

    void begin() {
        this.conditionResults.clear();
        this.handInResults.clear();
    }

    QuestDestinations(BetonQuestApi betonQuestApi, HudQuestLog hudQuestLog, EnorialsHUD enorialsHUD) {
        this.api = betonQuestApi;
        this.log = hudQuestLog;
        this.hud = enorialsHUD;
        this.npcs = new BetonQuestNpcResolver(betonQuestApi, hudQuestLog::warn);
    }

    void reload() {
        this.conversations.clear();
        this.actionSignals.clear();
        this.conversationSignals.clear();
        this.questObjectives.clear();
        this.questJournals.clear();
        this.begin();
        this.npcs.configure((this.hud.gps() == null ? QuestMarkerMappings.defaults() : this.hud.gps().questMarkerMappings()).npcSources());
        for (QuestPackage questPackage : this.api.packages().getPackages().values()) {
            Object object3;
            Object object4;
            ConfigurationSection configurationSection;
            ConfigurationSection configurationSection2 = questPackage.getConfig().getConfigurationSection("cancel");
            if (configurationSection2 != null) {
                configurationSection = configurationSection2.getKeys(false).iterator();
                while (configurationSection.hasNext()) {
                    object4 = (String)configurationSection.next();
                    object3 = configurationSection2.getConfigurationSection((String)object4);
                    if (object3 == null) continue;
                    for (String string : QuestLogSources.list((ConfigurationSection)object3, "objectives")) {
                        try {
                            this.questObjectives.add(((ObjectiveIdentifier)this.api.identifiers().getFactory(ObjectiveIdentifier.class).parseIdentifier(questPackage, string)).getFull());
                        }
                        catch (Exception exception) {
                            this.log.warn("quest-objective:" + string, exception);
                        }
                    }
                    for (String string : QuestLogSources.list((ConfigurationSection)object3, "actions")) {
                        for (Signal signal : this.effects(questPackage, string, new HashSet<String>())) {
                            if (!signal.kind().startsWith("journal-")) continue;
                            this.questJournals.add(signal.reference());
                        }
                    }
                }
            }
            if ((configurationSection = questPackage.getConfig().getConfigurationSection("journal")) != null) {
                object4 = configurationSection.getKeys(false).iterator();
                while (object4.hasNext()) {
                    String string;
                    String string2;
                    object3 = (String)object4.next();
                    Object object6 = configurationSection.get((String)object3);
                    if (object6 instanceof ConfigurationSection) {
                        Object object = (ConfigurationSection)object6;
                        string2 = object.getValues(false).toString();
                    } else {
                        string2 = Objects.toString(object6, "");
                    }
                    if (HudQuestLog.title(HudQuestLog.clean(string = string2)) == null) continue;
                    this.questJournals.add(questPackage.getQuestPath() + ">" + (String)object3);
                }
            }
            if ((object4 = questPackage.getConfig().getConfigurationSection("npc_conversations")) == null) continue;
            for (Object object6 : object4.getKeys(false)) {
                for (Object object : QuestLogSources.list((ConfigurationSection)object4, (String)object6)) {
                    try {
                        List<BetonQuestNpcResolver.Reference> list;
                        ConversationIdentifier conversationIdentifier = (ConversationIdentifier)this.api.identifiers().getFactory(ConversationIdentifier.class).parseIdentifier(questPackage, (String)object);
                        ConfigurationSection configurationSection3 = conversationIdentifier.getPackage().getConfig().getConfigurationSection("conversations." + conversationIdentifier.get());
                        if (configurationSection3 == null || (list = this.npcs.references(questPackage, (String)object6)).isEmpty()) continue;
                        this.conversations.add(new Conversation(conversationIdentifier.getPackage(), conversationIdentifier.getFull(), configurationSection3, list));
                        HashSet<Signal> hashSet = new HashSet<Signal>();
                        for (String string : List.of("NPC_options", "player_options")) {
                            ConfigurationSection configurationSection4 = configurationSection3.getConfigurationSection(string);
                            if (configurationSection4 == null) continue;
                            for (String string3 : configurationSection4.getKeys(false)) {
                                ConfigurationSection configurationSection5 = configurationSection4.getConfigurationSection(string3);
                                if (configurationSection5 == null) continue;
                                for (String string4 : QuestLogSources.list(configurationSection5, "actions")) {
                                    hashSet.addAll(this.effects(conversationIdentifier.getPackage(), string4, new HashSet<String>()));
                                }
                            }
                        }
                        this.conversationSignals.put(conversationIdentifier.getFull(), Set.copyOf(hashSet));
                    }
                    catch (Exception exception) {
                        this.log.warn("conversation-source:" + questPackage.getQuestPath() + ">" + (String)object, exception);
                    }
                }
            }
        }
    }

    private Set<Signal> effects(QuestPackage questPackage, String string, Set<String> set) {
        LinkedHashSet<Signal> linkedHashSet = new LinkedHashSet<Signal>();
        if (set.size() > 128) {
            return linkedHashSet;
        }
        try {
            ActionIdentifier actionIdentifier = (ActionIdentifier)this.api.identifiers().getFactory(ActionIdentifier.class).parseIdentifier(questPackage, string);
            Set<Signal> set2 = this.actionSignals.get(actionIdentifier.getFull());
            if (set2 != null) {
                return set2;
            }
            if (!set.add(actionIdentifier.getFull())) {
                return linkedHashSet;
            }
            String[] stringArray = actionIdentifier.getPackage().getConfig().getString("actions." + actionIdentifier.get(), "").strip().split("\\s+");
            if (stringArray.length >= 3 && stringArray[0].equalsIgnoreCase("journal") && Set.of("add", "delete", "remove").contains(stringArray[1])) {
                linkedHashSet.add(new Signal("journal-" + stringArray[1], ((JournalEntryIdentifier)this.api.identifiers().getFactory(JournalEntryIdentifier.class).parseIdentifier(actionIdentifier.getPackage(), stringArray[2])).getFull()));
            }
            if (stringArray.length >= 3 && stringArray[0].equalsIgnoreCase("objective") && stringArray[1].equalsIgnoreCase("start")) {
                for (String string2 : stringArray[2].split(",")) {
                    linkedHashSet.add(new Signal("objective-start", ((ObjectiveIdentifier)this.api.identifiers().getFactory(ObjectiveIdentifier.class).parseIdentifier(actionIdentifier.getPackage(), string2)).getFull()));
                }
            }
            if (stringArray.length >= 2 && stringArray[0].equalsIgnoreCase("folder")) {
                for (String string2 : stringArray[1].split(",")) {
                    linkedHashSet.addAll(this.effects(actionIdentifier.getPackage(), string2, set));
                }
            }
            this.actionSignals.put(actionIdentifier.getFull(), Set.copyOf(linkedHashSet));
        }
        catch (Exception exception) {
            this.log.warn("quest-action:" + string, exception);
        }
        return linkedHashSet;
    }

    private boolean allowed(QuestPackage questPackage, ConfigurationSection configurationSection, Profile profile) throws Exception {
        for (String string : QuestLogSources.list(configurationSection, "conditions")) {
            String string2 = questPackage.getQuestPath() + ">" + string;
            Boolean bl = this.conditionResults.get(string2);
            if (bl == null) {
                bl = ((ConditionManager)this.api.conditions().manager()).test(profile, (ConditionIdentifier)this.api.identifiers().getFactory(ConditionIdentifier.class).parseIdentifier(questPackage, string));
                this.conditionResults.put(string2, bl);
            }
            if (bl.booleanValue()) continue;
            return false;
        }
        return true;
    }

    private Set<Signal> reachable(Conversation conversation, Profile profile) throws Exception {
        LinkedHashSet<Signal> linkedHashSet = new LinkedHashSet<Signal>();
        for (String string : QuestLogSources.list(conversation.section(), "first")) {
            ConfigurationSection configurationSection = conversation.section().getConfigurationSection("NPC_options." + string);
            if (configurationSection == null || !this.allowed(conversation.pack(), configurationSection, profile)) continue;
            this.walk(conversation, string, true, profile, new HashSet<String>(), linkedHashSet);
            break;
        }
        return linkedHashSet;
    }

    private Set<Signal> handIns(Conversation conversation, Profile profile) throws Exception {
        Set<Signal> set = this.handInResults.get(conversation.key());
        if (set != null) {
            return set;
        }
        LinkedHashSet<Signal> linkedHashSet = new LinkedHashSet<Signal>();
        for (String string : QuestLogSources.list(conversation.section(), "first")) {
            ConfigurationSection configurationSection = conversation.section().getConfigurationSection("NPC_options." + string);
            if (configurationSection == null || !this.allowed(conversation.pack(), configurationSection, profile)) continue;
            this.walk(conversation, string, true, profile, new HashSet<String>(), linkedHashSet);
        }
        Set set2 = Set.copyOf(linkedHashSet);
        this.handInResults.put(conversation.key(), set2);
        return set2;
    }

    private boolean removesJournal(Signal signal, Set<String> set) {
        return (signal.kind().equals("journal-delete") || signal.kind().equals("journal-remove")) && set.contains(signal.reference());
    }

    private void walk(Conversation conversation, String string, boolean bl, Profile profile, Set<String> set, Set<Signal> set2) throws Exception {
        if (set.size() >= 96 || !set.add(bl + ":" + string)) {
            return;
        }
        ConfigurationSection configurationSection = conversation.section().getConfigurationSection((bl ? "NPC_options." : "player_options.") + string);
        if (configurationSection == null || !this.allowed(conversation.pack(), configurationSection, profile)) {
            return;
        }
        for (String string2 : QuestLogSources.list(configurationSection, "actions")) {
            set2.addAll(this.effects(conversation.pack(), string2, new HashSet<String>()));
        }
        for (String string2 : QuestLogSources.list(configurationSection, "pointers")) {
            this.walk(conversation, string2, !bl, profile, set, set2);
        }
    }

    Location target(HudQuestLog.Stage stage, Profile profile, Set<String> set, Set<String> set2) {
        Object object;
        Iterator<BetonQuestNpcResolver.Located> iterator;
        Object object2;
        if (!stage.compass().isEmpty()) {
            try {
                CompassIdentifier compassIdentifier = (CompassIdentifier)this.api.identifiers().getFactory(CompassIdentifier.class).parseIdentifier(null, stage.compass());
                object2 = (QuestCompass)this.api.compasses().forProfile(profile).get(compassIdentifier);
                if (object2 != null) {
                    return (Location)object2.location().getValue(profile);
                }
            }
            catch (Exception exception) {
                this.log.warn("quest-compass:" + stage.compass(), exception);
            }
        }
        boolean bl = false;
        for (String object3 : stage.objectives()) {
            if (!set.contains(object3)) continue;
            bl = true;
            try {
                ObjectiveIdentifier objectiveIdentifier = (ObjectiveIdentifier)this.api.identifiers().getFactory(ObjectiveIdentifier.class).parseIdentifier(null, object3);
                String string = objectiveIdentifier.getPackage().getConfig().getString("objectives." + objectiveIdentifier.get(), "");
                String[] stringArray = string.strip().split("\\s+");
                iterator = stringArray[0].toLowerCase(Locale.ROOT);
                object = null;
                if (Set.of("location", "chestput", "chesttake").contains(iterator) && stringArray.length > 1) {
                    object = stringArray[1];
                }
                for (String string2 : stringArray) {
                    if (!string2.startsWith("loc:") && !string2.startsWith("hookLocation:")) continue;
                    object = string2.substring(string2.indexOf(58) + 1);
                }
                if (object != null) {
                    return (Location)this.api.instructions().createForArgument(objectiveIdentifier.getPackage(), (String)object).location().get().getValue(profile);
                }
                if (!Set.of("npcinteract", "npckill").contains(iterator) || stringArray.length <= 1 || !profile.getOnlineProfile().isPresent()) continue;
                for (BetonQuestNpcResolver.Reference reference : this.npcs.references(objectiveIdentifier.getPackage(), stringArray[1])) {
                    Iterator<BetonQuestNpcResolver.Located> iterator2 = this.npcs.locations(reference, (OnlineProfile)profile.getOnlineProfile().get()).iterator();
                    if (!iterator2.hasNext()) continue;
                    BetonQuestNpcResolver.Located located = iterator2.next();
                    return located.location();
                }
            }
            catch (Exception exception) {
                this.log.warn("quest-destination:" + object3, exception);
            }
        }
        if (bl) {
            return null;
        }
        object2 = new HashSet<String>(stage.journals());
        object2.retainAll(set2);
        if (profile.getOnlineProfile().isEmpty() || object2.isEmpty()) {
            return null;
        }
        for (Conversation conversation : this.conversations) {
            try {
                if (this.conversationSignals.getOrDefault(conversation.key(), Set.of()).stream().noneMatch(arg_0 -> this.lambda$target$0((Set)object2, arg_0)) || this.handIns(conversation, profile).stream().noneMatch(arg_0 -> this.lambda$target$1((Set)object2, arg_0))) continue;
                for (BetonQuestNpcResolver.Reference reference : conversation.npcs()) {
                    iterator = this.npcs.locations(reference, (OnlineProfile)profile.getOnlineProfile().get()).iterator();
                    if (!iterator.hasNext()) continue;
                    object = (BetonQuestNpcResolver.Located)iterator.next();
                    return ((BetonQuestNpcResolver.Located)object).location();
                }
            }
            catch (Exception exception) {
                this.log.warn("return-destination:" + conversation.key(), exception);
            }
        }
        return null;
    }

    List<Hint> hints(Profile profile, Set<String> set, Set<String> set2) {
        if (profile.getOnlineProfile().isEmpty()) {
            return List.of();
        }
        ArrayList<Hint> arrayList = new ArrayList<Hint>();
        for (Conversation conversation : this.conversations) {
            try {
                Set<Signal> set3 = this.reachable(conversation, profile);
                boolean bl = this.conversationSignals.getOrDefault(conversation.key(), Set.of()).stream().anyMatch(signal -> this.questJournals.contains(signal.reference()) && this.removesJournal((Signal)signal, set2));
                boolean bl2 = bl && this.handIns(conversation, profile).stream().anyMatch(signal -> this.questJournals.contains(signal.reference()) && this.removesJournal((Signal)signal, set2));
                boolean bl3 = set3.stream().anyMatch(signal -> signal.kind().equals("objective-start") && this.questObjectives.contains(signal.reference()) && !set.contains(signal.reference()) || signal.kind().equals("journal-add") && this.questJournals.contains(signal.reference()) && !set2.contains(signal.reference()));
                boolean bl4 = this.conversationSignals.getOrDefault(conversation.key(), Set.of()).stream().anyMatch(signal -> signal.kind().equals("objective-start") && this.questObjectives.contains(signal.reference()) && set.contains(signal.reference()) || signal.kind().equals("journal-add") && this.questJournals.contains(signal.reference()) && set2.contains(signal.reference()));
                if (!bl2 && !bl3 && !bl4) continue;
                String string = this.log.render(conversation.pack(), "conversations." + conversation.key().substring(conversation.key().lastIndexOf(62) + 1) + ".quester", profile);
                for (BetonQuestNpcResolver.Reference reference : conversation.npcs()) {
                    for (BetonQuestNpcResolver.Located located : this.npcs.locations(reference, (OnlineProfile)profile.getOnlineProfile().get())) {
                        arrayList.add(new Hint(located.key(), located.location(), string, bl3, bl2, bl4));
                    }
                }
            }
            catch (Exception exception) {
                this.log.warn("quest-hint:" + conversation.key(), exception);
            }
        }
        return List.copyOf(arrayList);
    }

    private /* synthetic */ boolean lambda$target$1(Set set, Signal signal) {
        return this.removesJournal(signal, set);
    }

    private /* synthetic */ boolean lambda$target$0(Set set, Signal signal) {
        return this.removesJournal(signal, set);
    }

    record Signal(String kind, String reference) {
    }

    private record Conversation(QuestPackage pack, String key, ConfigurationSection section, List<BetonQuestNpcResolver.Reference> npcs) {
    }

    record Hint(String key, Location location, String title, boolean available, boolean complete, boolean incomplete) {
    }
}

