/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.citizensnpcs.api.CitizensAPI
 *  net.citizensnpcs.api.npc.NPC
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 */
package sk.enorials.hud.party;

import java.util.Set;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public final class TargetNpcNames {
    private TargetNpcNames() {
    }

    public static boolean hidden(LivingEntity livingEntity, Set<String> set) {
        if (set.isEmpty() || !Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            return false;
        }
        try {
            return Citizens.hidden(livingEntity, set);
        }
        catch (LinkageError | RuntimeException throwable) {
            return false;
        }
    }

    private static final class Citizens {
        private Citizens() {
        }

        static boolean hidden(LivingEntity livingEntity, Set<String> set) {
            NPC nPC = CitizensAPI.getNPCRegistry().getNPC((Entity)livingEntity);
            return nPC != null && (set.contains(nPC.getUniqueId().toString()) || set.contains(Integer.toString(nPC.getId())));
        }
    }
}

