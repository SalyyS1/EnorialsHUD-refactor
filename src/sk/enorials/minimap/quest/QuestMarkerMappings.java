/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.InvalidConfigurationException
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.minimap.quest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.gps.EnorialsGpsPlugin;
import sk.enorials.minimap.quest.QuestMarkerState;

public final class QuestMarkerMappings {
    public static final String RESOURCE = "configs/quest-markers.yml";
    private final Map<String, QuestMarkerState> states;
    private final Set<String> ambiguous;
    private NpcSources npcSources = new NpcSources(Set.of("citizens"), true);

    public NpcSources npcSources() {
        return this.npcSources;
    }

    private QuestMarkerMappings(Map<String, QuestMarkerState> map, Set<String> set) {
        this.states = Map.copyOf(map);
        this.ambiguous = Set.copyOf(set);
    }

    public static QuestMarkerMappings defaults() {
        return Defaults.MAPPING;
    }

    public static QuestMarkerMappings fromConfig(YamlConfiguration yamlConfiguration, Consumer<String> consumer) {
        Object object4;
        List list;
        SequencedCollection<String> sequencedCollection;
        Object object2;
        if (yamlConfiguration.contains("marker-items") && !yamlConfiguration.isConfigurationSection("marker-items")) {
            throw new IllegalArgumentException("marker-items must be a section of state names and item ID lists");
        }
        QuestMarkerMappings questMarkerMappings = QuestMarkerMappings.compile(yamlConfiguration.getConfigurationSection("marker-items"), Defaults.CONFIG.getConfigurationSection("marker-items"), consumer);
        if (yamlConfiguration.contains("npc-sources") && !yamlConfiguration.isConfigurationSection("npc-sources")) {
            throw new IllegalArgumentException("npc-sources must be a section");
        }
        Object object3 = yamlConfiguration.get("npc-sources.providers", Defaults.CONFIG.get("npc-sources.providers"));
        if (object3 instanceof String) {
            object2 = (String)object3;
            v0 = List.of(object2);
        } else if (object3 instanceof List) {
            sequencedCollection = (List)object3;
            v0 = sequencedCollection;
        } else {
            v0 = list = null;
        }
        if (list == null) {
            throw new IllegalArgumentException("npc-sources.providers must be a list of BetonQuest NPC selector names");
        }
        sequencedCollection = new LinkedHashSet();
        for (Object object4 : list) {
            String string;
            if (!(object4 instanceof String) || !(string = (String)object4).trim().matches("[a-zA-Z0-9_:-]+")) {
                throw new IllegalArgumentException("Invalid NPC provider: " + String.valueOf(object4));
            }
            sequencedCollection.add(string.trim().toLowerCase(Locale.ROOT));
        }
        object2 = yamlConfiguration.get("npc-sources.legacy-citizens-ids", Defaults.CONFIG.get("npc-sources.legacy-citizens-ids"));
        if (!(object2 instanceof Boolean)) {
            throw new IllegalArgumentException("npc-sources.legacy-citizens-ids must be true or false");
        }
        object4 = (Boolean)object2;
        questMarkerMappings.npcSources = new NpcSources((Set<String>)((Object)sequencedCollection), (Boolean)object4);
        return questMarkerMappings;
    }

    private static QuestMarkerMappings compile(ConfigurationSection configurationSection, ConfigurationSection configurationSection2, Consumer<String> consumer) {
        LinkedHashMap<String, QuestMarkerState> linkedHashMap = new LinkedHashMap<String, QuestMarkerState>();
        HashSet<String> hashSet = new HashSet<String>();
        HashSet<String> hashSet2 = new HashSet<String>();
        for (QuestMarkerState questMarkerState : QuestMarkerState.values()) {
            List list;
            Object object;
            String string = questMarkerState.name().toLowerCase(Locale.ROOT);
            hashSet2.add(string);
            Object object2 = configurationSection != null && configurationSection.contains(string) ? configurationSection.get(string) : (object = configurationSection2 == null ? null : configurationSection2.get(string));
            if (object == null) continue;
            if (object instanceof String) {
                String string2 = (String)object;
                list = List.of(string2);
            } else if (object instanceof List) {
                List list2;
                list = list2 = (List)object;
            } else {
                consumer.accept("marker-items." + string + " must be an item ID or a list; this state mapping is ignored");
                continue;
            }
            for (Object e : list) {
                QuestMarkerState questMarkerState2;
                if (!(e instanceof String)) {
                    consumer.accept("marker-items." + string + " contains a non-text item ID; entry ignored");
                    continue;
                }
                String string3 = (String)e;
                String string4 = QuestMarkerMappings.normalize(string3);
                if (string4.isEmpty() || string4.chars().anyMatch(Character::isWhitespace) || string4.endsWith(">")) {
                    consumer.accept("Invalid item ID in marker-items." + string + ": '" + string3 + "'; entry ignored");
                    continue;
                }
                if (hashSet.contains(string4) || (questMarkerState2 = linkedHashMap.putIfAbsent(string4, questMarkerState)) == null || questMarkerState2 == questMarkerState) continue;
                linkedHashMap.remove(string4);
                hashSet.add(string4);
                consumer.accept("Item ID '" + string4 + " is assigned to both " + questMarkerState2.name().toLowerCase(Locale.ROOT) + " and " + string + "; ambiguous ID ignored until corrected");
            }
        }
        if (configurationSection != null) {
            for (String string : configurationSection.getKeys(false)) {
                if (hashSet2.contains(string)) continue;
                consumer.accept("Unknown quest marker state '" + string + "'; use " + String.valueOf(new TreeSet(hashSet2)));
            }
        }
        return new QuestMarkerMappings(linkedHashMap, hashSet);
    }

    private static String normalize(String string) {
        String string2 = string.trim().toLowerCase(Locale.ROOT);
        return string2.startsWith("item:") ? string2.substring(5).trim() : string2;
    }

    public QuestMarkerState fromLines(List<String> list) {
        return this.fromLines(list, null);
    }

    public QuestMarkerState fromLines(List<String> list, String string) {
        for (String string2 : list) {
            if (string2 == null || !string2.trim().regionMatches(true, 0, "item:", 0, 5)) continue;
            Object object = QuestMarkerMappings.normalize(string2);
            if (((String)object).indexOf(62) < 0 && string != null && !string.isBlank()) {
                object = QuestMarkerMappings.normalize(string) + ">" + (String)object;
            }
            if (this.ambiguous.contains(object)) continue;
            QuestMarkerState questMarkerState = this.states.get(object);
            if (questMarkerState == null && ((String)object).lastIndexOf(62) >= 0) {
                questMarkerState = this.states.get(((String)object).substring(((String)object).lastIndexOf(62) + 1));
            }
            if (questMarkerState == null) continue;
            return questMarkerState;
        }
        return null;
    }

    public record NpcSources(Set<String> providers, boolean legacyCitizensIds) {
        public NpcSources {
            set = Set.copyOf(set);
        }

        public boolean allows(String string) {
            String string2 = string.toLowerCase(Locale.ROOT);
            return this.providers.contains(string2) || string2.startsWith("betonquest:") && this.providers.contains(string2.substring(11));
        }
    }

    private static final class Defaults {
        static final YamlConfiguration CONFIG = Defaults.read();
        static final QuestMarkerMappings MAPPING = QuestMarkerMappings.compile(CONFIG.getConfigurationSection("marker-items"), null, string -> {
            throw new IllegalStateException("Invalid bundled quest marker mapping: " + string);
        });

        private Defaults() {
        }

        private static YamlConfiguration read() {
            YamlConfiguration yamlConfiguration;
            block9: {
                InputStream inputStream = QuestMarkerMappings.class.getClassLoader().getResourceAsStream("modules/gps/configs/quest-markers.yml");
                try {
                    if (inputStream == null) {
                        throw new IOException("Missing quest marker defaults");
                    }
                    YamlConfiguration yamlConfiguration2 = new YamlConfiguration();
                    yamlConfiguration2.loadFromString(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
                    yamlConfiguration = yamlConfiguration2;
                    if (inputStream == null) break block9;
                }
                catch (Throwable throwable) {
                    try {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            }
                            catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }
                        throw throwable;
                    }
                    catch (IOException | InvalidConfigurationException throwable3) {
                        throw new IllegalStateException(throwable3);
                    }
                }
                inputStream.close();
            }
            return yamlConfiguration;
        }
    }

    public static final class Store {
        private final EnorialsGpsPlugin plugin;
        private final Path file;
        private byte[] lastRead;
        private String lastIoError;
        private QuestMarkerMappings current = QuestMarkerMappings.defaults();

        public Store(EnorialsGpsPlugin enorialsGpsPlugin) {
            this.plugin = enorialsGpsPlugin;
            this.file = enorialsGpsPlugin.getDataFolder().toPath().resolve(QuestMarkerMappings.RESOURCE);
            this.reloadIfChanged();
        }

        public QuestMarkerMappings reloadIfChanged() {
            try {
                if (!Files.exists(this.file, new LinkOption[0])) {
                    this.plugin.saveResource(QuestMarkerMappings.RESOURCE, false);
                }
                byte[] byArray = Files.readAllBytes(this.file);
                this.lastIoError = null;
                if (Arrays.equals(this.lastRead, byArray)) {
                    return this.current;
                }
                this.lastRead = byArray;
                YamlConfiguration yamlConfiguration = new YamlConfiguration();
                yamlConfiguration.loadFromString(new String(byArray, StandardCharsets.UTF_8).replaceFirst("^\ufeff", ""));
                this.current = QuestMarkerMappings.fromConfig(yamlConfiguration, string -> this.plugin.getLogger().warning("Quest marker config: " + string));
            }
            catch (IllegalArgumentException | InvalidConfigurationException throwable) {
                this.plugin.getLogger().warning("Cannot read configs/quest-markers.yml; keeping the last valid marker mapping: " + throwable.getMessage());
            }
            catch (IOException | IllegalStateException exception) {
                String string2 = exception.getMessage();
                if (!Objects.equals(this.lastIoError, string2)) {
                    this.plugin.getLogger().warning("Cannot read configs/quest-markers.yml; keeping the last valid marker mapping: " + string2);
                }
                this.lastIoError = string2;
            }
            return this.current;
        }
    }
}

