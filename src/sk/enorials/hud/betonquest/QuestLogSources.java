/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.betonquest.betonquest.api.BetonQuestApi
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.compass.QuestCompass
 *  org.betonquest.betonquest.api.config.quest.QuestPackage
 *  org.betonquest.betonquest.api.identifier.ActionIdentifier
 *  org.betonquest.betonquest.api.identifier.CompassIdentifier
 *  org.betonquest.betonquest.api.identifier.ConditionIdentifier
 *  org.betonquest.betonquest.api.identifier.JournalEntryIdentifier
 *  org.betonquest.betonquest.api.identifier.ObjectiveIdentifier
 *  org.betonquest.betonquest.api.instruction.Instruction
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.quest.objective.Objective
 *  org.betonquest.betonquest.api.service.condition.ConditionManager
 *  org.betonquest.betonquest.api.service.objective.ObjectiveManager
 *  org.betonquest.betonquest.api.text.Text
 *  org.betonquest.betonquest.feature.journal.Pointer
 *  org.bukkit.Location
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.hud.betonquest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.compass.QuestCompass;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.identifier.ActionIdentifier;
import org.betonquest.betonquest.api.identifier.CompassIdentifier;
import org.betonquest.betonquest.api.identifier.ConditionIdentifier;
import org.betonquest.betonquest.api.identifier.JournalEntryIdentifier;
import org.betonquest.betonquest.api.identifier.ObjectiveIdentifier;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.objective.Objective;
import org.betonquest.betonquest.api.service.condition.ConditionManager;
import org.betonquest.betonquest.api.service.objective.ObjectiveManager;
import org.betonquest.betonquest.api.text.Text;
import org.betonquest.betonquest.feature.journal.Pointer;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.betonquest.HudQuestLog;

final class QuestLogSources {
    private final EnorialsHUD hud;
    private final BetonQuestApi api;
    private final HudQuestLog log;
    private final List<Definition> definitions = new ArrayList<Definition>();
    private final Set<String> claimed = new HashSet<String>();

    QuestLogSources(EnorialsHUD enorialsHUD, BetonQuestApi betonQuestApi, HudQuestLog hudQuestLog) {
        this.hud = enorialsHUD;
        this.api = betonQuestApi;
        this.log = hudQuestLog;
    }

    void reload() {
        ConfigurationSection configurationSection;
        this.definitions.clear();
        this.claimed.clear();
        ConfigurationSection configurationSection2 = this.hud.cfg().getConfigurationSection("quest-log.quests");
        if (configurationSection2 != null) {
            for (String string : configurationSection2.getKeys(false)) {
                configurationSection = configurationSection2.getConfigurationSection(string);
                if (configurationSection == null) continue;
                this.add(new Definition(string, (QuestPackage)this.api.packages().getPackages().get(configurationSection.getString("package", "")), configurationSection, "explicit", string));
            }
        }
        for (String string : this.api.packages().getPackages().values()) {
            ConfigurationSection configurationSection3;
            ConfigurationSection configurationSection4;
            configurationSection = string.getConfig().getConfigurationSection("enorials_quest_log");
            if (configurationSection != null) {
                configurationSection4 = configurationSection.getKeys(false).iterator();
                while (configurationSection4.hasNext()) {
                    String string2 = (String)configurationSection4.next();
                    Object object = configurationSection.getConfigurationSection(string2);
                    if (object == null) continue;
                    this.add(new Definition(string.getQuestPath() + ">" + (String)string2, (QuestPackage)string, (ConfigurationSection)object, "explicit", string2));
                }
            }
            if (this.hud.cfg().getBoolean("quest-log.discover-cancelers", true) && (configurationSection4 = string.getConfig().getConfigurationSection("cancel")) != null) {
                for (Object object : configurationSection4.getKeys(false)) {
                    configurationSection3 = configurationSection4.getConfigurationSection((String)object);
                    if (configurationSection3 == null) continue;
                    this.add(new Definition(string.getQuestPath() + ">cancel:" + (String)object, (QuestPackage)string, this.enrich((QuestPackage)string, configurationSection3), "cancel", (String)object));
                }
            }
            if (!this.hud.cfg().getBoolean("quest-log.discover-structured-main-page", true) || (configurationSection4 = string.getConfig().getConfigurationSection("journal_main_page")) == null) continue;
            for (Object object : configurationSection4.getKeys(false)) {
                configurationSection3 = configurationSection4.getConfigurationSection((String)object);
                if (configurationSection3 == null || QuestLogSources.list(configurationSection3, "conditions").isEmpty()) continue;
                this.definitions.add(new Definition(string.getQuestPath() + ">main:" + (String)object, (QuestPackage)string, configurationSection3, "main", (String)object));
            }
        }
        this.definitions.sort(Comparator.comparingInt(definition -> definition.kind().equals("explicit") ? 0 : (definition.kind().equals("cancel") ? 1 : 2)));
    }

    private ConfigurationSection enrich(QuestPackage questPackage, ConfigurationSection configurationSection) {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        configurationSection.getValues(true).forEach((arg_0, arg_1) -> ((YamlConfiguration)yamlConfiguration).set(arg_0, arg_1));
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>(QuestLogSources.list(configurationSection, "journal"));
        HashSet<String> hashSet = new HashSet<String>();
        for (String string : QuestLogSources.list(configurationSection, "actions")) {
            this.collectJournals(questPackage, string, linkedHashSet, hashSet);
        }
        for (String string : QuestLogSources.list(configurationSection, "objectives")) {
            try {
                ObjectiveIdentifier objectiveIdentifier = (ObjectiveIdentifier)this.api.identifiers().getFactory(ObjectiveIdentifier.class).parseIdentifier(questPackage, string);
                String string2 = objectiveIdentifier.getPackage().getConfig().getString("objectives." + objectiveIdentifier.get(), "");
                Matcher matcher = Pattern.compile("(?:^|\\s)actions:([^\\s]+)").matcher(string2);
                if (!matcher.find()) continue;
                for (String string3 : matcher.group(1).split(",")) {
                    this.collectJournals(objectiveIdentifier.getPackage(), string3, linkedHashSet, hashSet);
                }
            }
            catch (Exception exception) {
                this.log.warn("objective-source:" + string, exception);
            }
        }
        yamlConfiguration.set("journal", new ArrayList<String>(linkedHashSet));
        return yamlConfiguration;
    }

    private void collectJournals(QuestPackage questPackage, String string, Set<String> set, Set<String> set2) {
        if (set2.size() >= 128) {
            return;
        }
        try {
            ActionIdentifier actionIdentifier = (ActionIdentifier)this.api.identifiers().getFactory(ActionIdentifier.class).parseIdentifier(questPackage, string);
            if (!set2.add(actionIdentifier.getFull())) {
                return;
            }
            String[] stringArray = actionIdentifier.getPackage().getConfig().getString("actions." + actionIdentifier.get(), "").strip().split("\\s+");
            if (stringArray.length >= 3 && stringArray[0].equalsIgnoreCase("journal") && Set.of("add", "delete", "remove").contains(stringArray[1].toLowerCase(Locale.ROOT))) {
                set.add(((JournalEntryIdentifier)this.api.identifiers().getFactory(JournalEntryIdentifier.class).parseIdentifier(actionIdentifier.getPackage(), stringArray[2])).getFull());
            } else if (stringArray.length >= 2 && stringArray[0].equalsIgnoreCase("folder")) {
                for (String string2 : stringArray[1].split(",")) {
                    this.collectJournals(actionIdentifier.getPackage(), string2, set, set2);
                }
            }
        }
        catch (Exception exception) {
            this.log.warn("action-source:" + string, exception);
        }
    }

    private void add(Definition definition) {
        this.definitions.add(definition);
        for (String string : QuestLogSources.list(definition.section(), definition.kind().equals("cancel") ? "journal" : "journals")) {
            try {
                this.claimed.add(((JournalEntryIdentifier)this.api.identifiers().getFactory(JournalEntryIdentifier.class).parseIdentifier(definition.pack(), string)).getFull());
            }
            catch (Exception exception) {
                this.log.warn("source:" + definition.key(), exception);
            }
        }
    }

    Set<String> claimed() {
        return this.claimed;
    }

    static List<String> list(ConfigurationSection configurationSection, String string2) {
        if (configurationSection.isList(string2)) {
            return configurationSection.getStringList(string2);
        }
        String string3 = configurationSection.getString(string2, "");
        return string3.isBlank() ? List.of() : Arrays.stream(string3.split(",")).map(String::strip).filter(string -> !string.isEmpty()).toList();
    }

    private boolean conditions(QuestPackage questPackage, ConfigurationSection configurationSection, Profile profile) throws Exception {
        for (String string : QuestLogSources.list(configurationSection, "conditions")) {
            ConditionIdentifier conditionIdentifier = (ConditionIdentifier)this.api.identifiers().getFactory(ConditionIdentifier.class).parseIdentifier(questPackage, string);
            if (((ConditionManager)this.api.conditions().manager()).test(profile, conditionIdentifier)) continue;
            return false;
        }
        return true;
    }

    private String inline(QuestPackage questPackage, String string, Profile profile) throws Exception {
        Component component = (Component)this.api.instructions().createForArgument(questPackage, string).component().get().getValue(profile);
        return HudQuestLog.clean(PlainTextComponentSerializer.plainText().serialize(component));
    }

    private String ref(QuestPackage questPackage, String string, Profile profile) throws Exception {
        QuestPackage questPackage2;
        int n = string.lastIndexOf(62);
        QuestPackage questPackage3 = questPackage2 = n < 0 ? questPackage : (QuestPackage)this.api.packages().getPackages().get(string.substring(0, n));
        if (questPackage2 == null) {
            throw new IllegalArgumentException("Missing text package: " + string);
        }
        return this.log.render(questPackage2, n < 0 ? string : string.substring(n + 1), profile);
    }

    private String sectionText(Definition definition, ConfigurationSection configurationSection, String string, Profile profile) throws Exception {
        Object object;
        ConfigurationSection configurationSection2;
        if (!configurationSection.contains(string)) {
            return null;
        }
        Object object2 = configurationSection.get(string);
        if (object2 instanceof String) {
            String string2 = (String)object2;
            return this.inline(definition.pack(), string2, profile);
        }
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        if (object2 instanceof ConfigurationSection) {
            configurationSection2 = (ConfigurationSection)object2;
            object = configurationSection2.getValues(false);
        } else {
            object = object2;
        }
        yamlConfiguration.set("text", object);
        configurationSection2 = (Text)this.api.instructions().createSection(definition.pack(), (ConfigurationSection)yamlConfiguration).read().value(new String[]{"text"}).translationSection().get().getValue(profile);
        return HudQuestLog.clean(PlainTextComponentSerializer.plainText().serialize(configurationSection2.asComponent(profile)));
    }

    List<HudQuestLog.Stage> read(Profile profile, Map<String, Pointer> map) {
        Object object22;
        ArrayList<HudQuestLog.Stage> arrayList = new ArrayList<HudQuestLog.Stage>();
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        for (Object object22 : ((ObjectiveManager)this.api.objectives().manager()).getForProfile(profile)) {
            hashMap.put(object22.getObjectiveID().getFull(), object22);
        }
        HashSet hashSet = new HashSet();
        object22 = new HashSet();
        HashSet<Object> hashSet2 = new HashSet<Object>();
        for (Definition definition : this.definitions) {
            try {
                Object object;
                Object object3;
                Object object4;
                Object object5;
                QuestPackage questPackage;
                int n;
                Object object6;
                String string3;
                boolean bl;
                ConfigurationSection configurationSection = definition.section();
                if (!configurationSection.getBoolean("enabled", true) || !this.conditions(definition.pack(), configurationSection, profile)) continue;
                ArrayList<String> arrayList2 = new ArrayList<String>();
                for (String string4 : QuestLogSources.list(configurationSection, definition.kind().equals("cancel") ? "journal" : "journals")) {
                    arrayList2.add(((JournalEntryIdentifier)this.api.identifiers().getFactory(JournalEntryIdentifier.class).parseIdentifier(definition.pack(), string4)).getFull());
                }
                ArrayList arrayList3 = new ArrayList();
                for (String string5 : QuestLogSources.list(configurationSection, "objectives")) {
                    arrayList3.add(((ObjectiveIdentifier)this.api.identifiers().getFactory(ObjectiveIdentifier.class).parseIdentifier(definition.pack(), string5)).getFull());
                }
                Pointer object92 = arrayList2.stream().map(map::get).filter(Objects::nonNull).max(Comparator.comparingLong(Pointer::timestamp)).orElse(null);
                List<Objective> list = arrayList3.stream().map(hashMap::get).filter(Objects::nonNull).toList();
                String string6 = configurationSection.getString("compass", "");
                QuestCompass questCompass = null;
                if (!string6.isEmpty()) {
                    CompassIdentifier compassIdentifier = (CompassIdentifier)this.api.identifiers().getFactory(CompassIdentifier.class).parseIdentifier(definition.pack(), string6);
                    string6 = compassIdentifier.getFull();
                    questCompass = (QuestCompass)this.api.compasses().forProfile(profile).get(compassIdentifier);
                    if (questCompass == null) continue;
                }
                if (!(bl = !QuestLogSources.list(configurationSection, "conditions").isEmpty() || object92 != null || !list.isEmpty() || questCompass != null) || definition.kind().equals("cancel") && (!arrayList3.isEmpty() && hashSet.containsAll(arrayList3) || !arrayList2.isEmpty() && object22.containsAll(arrayList2))) continue;
                String string7 = string3 = object92 == null ? "" : this.log.render(object92.pointer().getPackage(), "journal." + object92.pointer().get(), profile);
                if (definition.kind().equals("main")) {
                    if (hashSet2.contains(definition.pack().getQuestPath() + ">" + definition.source())) continue;
                    string3 = this.log.render(definition.pack(), "journal_main_page." + definition.source() + ".text", profile);
                }
                if (configurationSection.contains("main-page")) {
                    object6 = configurationSection.getString("main-page");
                    n = ((String)object6).lastIndexOf(62);
                    questPackage = n < 0 ? definition.pack() : (QuestPackage)this.api.packages().getPackages().get(((String)object6).substring(0, n));
                    Object object7 = object5 = n < 0 ? object6 : ((String)object6).substring(n + 1);
                    object4 = questPackage.getConfig().getConfigurationSection("journal_main_page." + (String)object5);
                    if (object4 == null || !this.conditions(questPackage, (ConfigurationSection)object4, profile)) continue;
                    string3 = this.log.render(questPackage, "journal_main_page." + (String)object5 + ".text", profile);
                }
                if (configurationSection.contains("menu-item")) {
                    String string8;
                    object6 = configurationSection.getString("menu-item");
                    n = ((String)object6).lastIndexOf(62);
                    questPackage = n < 0 ? definition.pack() : (QuestPackage)this.api.packages().getPackages().get(((String)object6).substring(0, n));
                    Object object8 = object5 = n < 0 ? object6 : ((String)object6).substring(n + 1);
                    object4 = questPackage.getConfig().getConfigurationSection("menu_items." + (String)object5);
                    if (object4 == null || !this.conditions(questPackage, (ConfigurationSection)object4, profile)) continue;
                    object3 = object4.get("text");
                    if (object3 instanceof List) {
                        object = (List)object3;
                        string8 = this.inline(questPackage, String.join((CharSequence)"\n", object.stream().map(Object::toString).toList()), profile);
                    } else {
                        string8 = this.log.render(questPackage, "menu_items." + (String)object5 + ".text", profile);
                    }
                    string3 = string8;
                }
                object6 = definition.kind().equals("cancel") ? this.log.render(definition.pack(), "cancel." + definition.source() + ".name", profile) : this.sectionText(definition, configurationSection, "title", profile);
                Object object9 = null;
                questPackage = configurationSection.getConfigurationSection("stages");
                if (questPackage != null) {
                    object5 = questPackage.getKeys(false).iterator();
                    while (object5.hasNext()) {
                        object4 = (String)object5.next();
                        object3 = questPackage.getConfigurationSection((String)object4);
                        if (object3 == null || !this.conditions(definition.pack(), (ConfigurationSection)object3, profile)) continue;
                        object9 = object3;
                        break;
                    }
                }
                Object object10 = object5 = object9 == null ? null : this.sectionText(definition, (ConfigurationSection)object9, "objective", profile);
                if (object9 != null && object9.contains("objective-ref")) {
                    object5 = this.ref(definition.pack(), object9.getString("objective-ref"), profile);
                }
                if (object5 == null) {
                    object5 = this.sectionText(definition, configurationSection, "objective", profile);
                    if (configurationSection.contains("objective-ref")) {
                        object5 = this.ref(definition.pack(), configurationSection.getString("objective-ref"), profile);
                    }
                }
                if (configurationSection.contains("title-ref")) {
                    object6 = this.ref(definition.pack(), configurationSection.getString("title-ref"), profile);
                }
                if (object9 != null) {
                    object4 = this.sectionText(definition, (ConfigurationSection)object9, "title", profile);
                    if (object9.contains("title-ref")) {
                        object4 = this.ref(definition.pack(), object9.getString("title-ref"), profile);
                    }
                    if (object4 != null && !((String)object4).isBlank()) {
                        object6 = object4;
                    }
                }
                if (object6 == null) {
                    object6 = HudQuestLog.title(string3);
                }
                if (object6 == null && !string3.isBlank()) {
                    object6 = string3.lines().filter(string -> !string.isBlank()).findFirst().orElse(null);
                }
                if (object6 == null && questCompass != null) {
                    object6 = HudQuestLog.clean(PlainTextComponentSerializer.plainText().serialize(questCompass.name().asComponent(profile)));
                }
                if (object6 == null) {
                    this.log.warn("title:" + definition.key(), new IllegalArgumentException("Add a player-facing title or title-ref"));
                    continue;
                }
                if (object5 == null && !string3.isBlank()) {
                    Object object11 = object5 = HudQuestLog.title(string3) != null ? HudQuestLog.objective(string3) : string3.lines().skip(1L).reduce((string, string2) -> string + " " + string2).orElse("");
                }
                if (object5 == null || ((String)object5).isBlank()) {
                    object5 = this.describe(list, profile);
                }
                if ((object5 == null || ((String)object5).isBlank()) && questCompass != null) {
                    object5 = "Reach " + (String)object6;
                }
                if (object5 == null || ((String)object5).isBlank()) {
                    object5 = "Continue this quest";
                }
                long l = object92 == null ? System.currentTimeMillis() : object92.timestamp();
                object = definition.key();
                arrayList.add(new HudQuestLog.Stage((String)object, (String)object6, (String)object5, l, configurationSection.getString("gps-quest", definition.key()), List.copyOf(arrayList3), List.copyOf(arrayList2), string6));
                hashSet.addAll(arrayList3);
                object22.addAll(arrayList2);
                if (!configurationSection.contains("main-page")) continue;
                String string9 = configurationSection.getString("main-page");
                hashSet2.add(string9.contains(">") ? string9 : definition.pack().getQuestPath() + ">" + string9);
            }
            catch (Exception exception) {
                this.log.warn("source:" + definition.key(), exception);
            }
        }
        return arrayList;
    }

    private String describe(List<Objective> list, Profile profile) throws Exception {
        ArrayList<Object> arrayList = new ArrayList<Object>();
        for (Objective objective : list) {
            Object object;
            String string;
            String[] stringArray;
            String string2 = this.hud.cfg().getString("quest-log.objective-texts." + objective.getObjectiveID().getFull());
            if (string2 != null) {
                arrayList.add(this.inline(objective.getPackage(), string2, profile));
                continue;
            }
            Instruction instruction = this.api.instructions().create(objective.getPackage(), objective.getPackage().getConfig().getString("objectives." + objective.getObjectiveID().get(), ""));
            String string3 = instruction.current().toLowerCase(Locale.ROOT);
            if (string3.equals("location")) {
                stringArray = (Location)instruction.location().get().getValue(profile);
                arrayList.add("Reach " + stringArray.getBlockX() + ", " + stringArray.getBlockY() + ", " + stringArray.getBlockZ());
                continue;
            }
            if (Set.of("mobkill", "mmobkill").contains(string3)) {
                stringArray = ((String)instruction.string().get().getValue(profile)).replace('_', ' ');
                string = "Defeat";
                object = "";
                try {
                    object = " (" + objective.getService().getProperties().getProperty("left", profile) + " left)";
                }
                catch (QuestException questException) {
                    // empty catch block
                }
                arrayList.add(string + " " + (String)stringArray + (String)object);
                continue;
            }
            if (!Set.of("block", "craft", "smelt", "fish", "breed", "tame", "pickup", "consume").contains(string3)) continue;
            stringArray = objective.getPackage().getConfig().getString("objectives." + objective.getObjectiveID().get(), "").split("\\s+");
            string = stringArray.length > 1 ? stringArray[1].replace("minecraft:", "").replace('_', ' ') : "items";
            object = switch (string3) {
                case "block" -> {
                    if (stringArray.length > 2 && stringArray[2].startsWith("-")) {
                        yield "Mine";
                    }
                    yield "Place";
                }
                case "smelt" -> "Cook";
                case "fish" -> "Catch";
                default -> Character.toUpperCase(string3.charAt(0)) + string3.substring(1);
            };
            Object object2 = "";
            try {
                object2 = " (" + objective.getService().getProperties().getProperty("left", profile) + " left)";
            }
            catch (QuestException questException) {
                // empty catch block
            }
            arrayList.add((String)object + " " + string + (String)object2);
        }
        return String.join((CharSequence)"; ", arrayList);
    }

    record Definition(String key, QuestPackage pack, ConfigurationSection section, String kind, String source) {
    }
}

