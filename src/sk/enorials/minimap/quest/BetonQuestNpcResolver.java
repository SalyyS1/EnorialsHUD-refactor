/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.citizensnpcs.api.CitizensAPI
 *  net.citizensnpcs.api.npc.NPC
 *  org.betonquest.betonquest.api.BetonQuestApi
 *  org.betonquest.betonquest.api.config.quest.QuestPackage
 *  org.betonquest.betonquest.api.identifier.NpcIdentifier
 *  org.betonquest.betonquest.api.profile.OnlineProfile
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.quest.npc.Npc
 *  org.betonquest.betonquest.api.service.npc.NpcManager
 *  org.betonquest.betonquest.api.service.npc.NpcRegistry
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 */
package sk.enorials.minimap.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.identifier.NpcIdentifier;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.npc.Npc;
import org.betonquest.betonquest.api.service.npc.NpcManager;
import org.betonquest.betonquest.api.service.npc.NpcRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import sk.enorials.minimap.quest.QuestMarkerMappings;

public final class BetonQuestNpcResolver {
    private final BetonQuestApi api;
    private final BiConsumer<String, Throwable> warn;
    private QuestMarkerMappings.NpcSources settings = QuestMarkerMappings.defaults().npcSources();
    private final Map<Object, String> identities = new WeakHashMap<Object, String>();
    private long nextIdentity;

    public BetonQuestNpcResolver(BetonQuestApi betonQuestApi, BiConsumer<String, Throwable> biConsumer) {
        this.api = betonQuestApi;
        this.warn = biConsumer;
    }

    public void configure(QuestMarkerMappings.NpcSources npcSources) {
        this.settings = npcSources;
    }

    public List<Reference> references(QuestPackage questPackage, String string) {
        ArrayList<Reference> arrayList = new ArrayList<Reference>();
        for (String string2 : string.split(",")) {
            String string3 = string2.trim();
            if (string3.isEmpty()) continue;
            try {
                if (string3.matches("[0-9]+") && questPackage.getConfig().get("npcs." + string3) == null) {
                    if (!this.settings.legacyCitizensIds() || !this.settings.allows("citizens")) continue;
                    arrayList.add(new Reference(null, Integer.parseInt(string3)));
                    continue;
                }
                NpcIdentifier npcIdentifier = (NpcIdentifier)this.api.identifiers().getFactory(NpcIdentifier.class).parseIdentifier(questPackage, string3);
                String string4 = npcIdentifier.readRawInstruction().trim().split("\\s+", 2)[0];
                if (!this.settings.allows(string4)) continue;
                ((NpcRegistry)this.api.npcs().registry()).getFactory(string4);
                arrayList.add(new Reference(npcIdentifier, null));
            }
            catch (Exception | LinkageError throwable) {
                this.warn.accept("npc-definition/" + questPackage.getQuestPath() + ">" + string3, throwable);
            }
        }
        return List.copyOf(arrayList);
    }

    public List<Located> locations(Reference reference, OnlineProfile onlineProfile) {
        try {
            if (reference.legacyId() != null) {
                if (!(this.settings.legacyCitizensIds() && this.settings.allows("citizens") && Bukkit.getPluginManager().isPluginEnabled("Citizens"))) {
                    return List.of();
                }
                return LegacyCitizens.locate(reference.legacyId());
            }
            NpcIdentifier npcIdentifier = reference.identifier();
            if (((NpcManager)this.api.npcs().manager()).isHidden(npcIdentifier, onlineProfile)) {
                return List.of();
            }
            ArrayList<Located> arrayList = new ArrayList<Located>();
            for (Npc npc : ((NpcManager)this.api.npcs().manager()).getAll((Profile)onlineProfile, npcIdentifier)) {
                Object object2;
                Location location;
                if (((NpcManager)this.api.npcs().manager()).isHidden(npc, onlineProfile.getPlayer()) || (location = (Location)npc.getLocation().orElse(null)) == null || location.getWorld() == null || (object2 = npc.getOriginal()) == null) continue;
                String string = null;
                if (Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
                    string = LegacyCitizens.key(object2);
                }
                if (string == null) {
                    string = this.identities.computeIfAbsent(object2, object -> "npc/api/" + ++this.nextIdentity);
                }
                arrayList.add(new Located(string, location.clone()));
            }
            return List.copyOf(arrayList);
        }
        catch (Exception | LinkageError throwable) {
            this.warn.accept("npc-location/" + String.valueOf(reference.identifier() == null ? reference.legacyId() : reference.identifier().getFull()), throwable);
            return List.of();
        }
    }

    public record Reference(NpcIdentifier identifier, Integer legacyId) {
    }

    private static final class LegacyCitizens {
        private LegacyCitizens() {
        }

        static String key(Object object) {
            String string;
            if (object instanceof NPC) {
                NPC nPC = (NPC)object;
                string = "npc/" + nPC.getId();
            } else {
                string = null;
            }
            return string;
        }

        static List<Located> locate(int n) {
            NPC nPC = CitizensAPI.getNPCRegistry().getById(n);
            if (nPC == null) {
                return List.of();
            }
            Location location = nPC.isSpawned() ? nPC.getEntity().getLocation() : nPC.getStoredLocation();
            return location == null || location.getWorld() == null ? List.of() : List.of(new Located("npc/" + n, location.clone()));
        }
    }

    public record Located(String key, Location location) {
    }
}

