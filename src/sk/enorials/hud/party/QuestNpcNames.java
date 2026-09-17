/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.citizensnpcs.api.CitizensAPI
 *  net.citizensnpcs.api.npc.NPC
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.betonquest.betonquest.BetonQuest
 *  org.betonquest.betonquest.api.BetonQuestApi
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.config.quest.QuestPackage
 *  org.betonquest.betonquest.api.identifier.ConversationIdentifier
 *  org.betonquest.betonquest.api.identifier.NpcIdentifier
 *  org.betonquest.betonquest.api.profile.OnlineProfile
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.quest.npc.Npc
 *  org.betonquest.betonquest.api.reload.ReloadPhase
 *  org.betonquest.betonquest.api.service.npc.NpcRegistry
 *  org.betonquest.betonquest.compatibility.npc.citizens.CitizensAdapter
 *  org.betonquest.betonquest.kernel.processor.feature.ConversationProcessor
 *  org.bukkit.Bukkit
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.party;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.identifier.ConversationIdentifier;
import org.betonquest.betonquest.api.identifier.NpcIdentifier;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.npc.Npc;
import org.betonquest.betonquest.api.reload.ReloadPhase;
import org.betonquest.betonquest.api.service.npc.NpcRegistry;
import org.betonquest.betonquest.compatibility.npc.citizens.CitizensAdapter;
import org.betonquest.betonquest.kernel.processor.feature.ConversationProcessor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class QuestNpcNames {
    private Bridge bridge;

    public String name(Player player, LivingEntity livingEntity) {
        if (player == null || !Bukkit.getPluginManager().isPluginEnabled("BetonQuest") || !Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            return null;
        }
        try {
            if (this.bridge == null) {
                this.bridge = new Bridge();
            }
            return this.bridge.name(player, livingEntity);
        }
        catch (LinkageError | RuntimeException throwable) {
            return null;
        }
    }

    private static final class Bridge {
        private final BetonQuestApi api = BetonQuest.getInstance().getBetonQuestApi();
        private Map<NpcIdentifier, List<ConversationIdentifier>> index;
        private final Map<UUID, Cached> names = new LinkedHashMap<UUID, Cached>();

        Bridge() {
            this.api.reloader().register(ReloadPhase.POST, () -> {
                this.index = null;
                this.names.clear();
            });
        }

        void index() {
            this.index = new LinkedHashMap<NpcIdentifier, List<ConversationIdentifier>>();
            for (QuestPackage questPackage : new TreeMap(this.api.packages().getPackages()).values()) {
                ConfigurationSection configurationSection = questPackage.getConfig().getConfigurationSection("npc_conversations");
                if (configurationSection == null) continue;
                for (String string : new TreeSet(configurationSection.getKeys(false))) {
                    try {
                        String string2 = configurationSection.getString(string);
                        if (string2 == null || string2.isBlank()) continue;
                        NpcIdentifier npcIdentifier2 = (NpcIdentifier)this.api.identifiers().getFactory(NpcIdentifier.class).parseIdentifier(questPackage, string);
                        ConversationIdentifier conversationIdentifier = (ConversationIdentifier)this.api.identifiers().getFactory(ConversationIdentifier.class).parseIdentifier(questPackage, string2);
                        this.index.computeIfAbsent(npcIdentifier2, npcIdentifier -> new ArrayList()).add(conversationIdentifier);
                    }
                    catch (QuestException questException) {}
                }
            }
        }

        String name(Player player, LivingEntity livingEntity) {
            NPC nPC = CitizensAPI.getNPCRegistry().getNPC((Entity)livingEntity);
            if (nPC == null) {
                return null;
            }
            long l = System.currentTimeMillis();
            Cached cached = this.names.get(player.getUniqueId());
            if (cached != null && cached.npc().equals(nPC.getUniqueId()) && cached.until() > l) {
                return cached.name();
            }
            if (this.index == null) {
                this.index();
            }
            String string = null;
            try {
                OnlineProfile onlineProfile = this.api.profiles().getProfile(player);
                CitizensAdapter citizensAdapter = new CitizensAdapter(nPC);
                Set set = ((NpcRegistry)this.api.npcs().registry()).getIdentifier((Npc)citizensAdapter, onlineProfile);
                ConversationProcessor conversationProcessor = (ConversationProcessor)this.api.conversations();
                LinkedHashSet linkedHashSet = new LinkedHashSet();
                for (Map.Entry<NpcIdentifier, List<ConversationIdentifier>> object : this.index.entrySet()) {
                    if (!set.contains(object.getKey())) continue;
                    linkedHashSet.addAll(object.getValue());
                }
                Optional optional = this.api.conversations().getActive((Profile)onlineProfile);
                if (optional.isPresent() && linkedHashSet.remove(optional.get())) {
                    LinkedHashSet<Object> linkedHashSet2 = new LinkedHashSet<Object>();
                    linkedHashSet2.add((ConversationIdentifier)optional.get());
                    linkedHashSet2.addAll(linkedHashSet);
                    linkedHashSet = linkedHashSet2;
                }
                for (ConversationIdentifier conversationIdentifier : linkedHashSet) {
                    try {
                        Component component = conversationProcessor.getData(conversationIdentifier).getPublicData().quester().asComponent((Profile)onlineProfile);
                        String string2 = PlainTextComponentSerializer.plainText().serialize(component);
                        if (string2.isBlank()) continue;
                        string = string2;
                        break;
                    }
                    catch (QuestException questException) {
                    }
                }
            }
            catch (RuntimeException runtimeException) {
                return null;
            }
            if (this.names.size() >= 256 && !this.names.containsKey(player.getUniqueId())) {
                this.names.remove(this.names.keySet().iterator().next());
            }
            this.names.put(player.getUniqueId(), new Cached(nPC.getUniqueId(), l + 1000L, string));
            return string;
        }

        private record Cached(UUID npc, long until, String name) {
        }
    }
}

