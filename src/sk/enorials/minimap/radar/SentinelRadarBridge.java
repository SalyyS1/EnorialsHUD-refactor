/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.citizensnpcs.api.npc.NPC
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.mcmonkey.sentinel.SentinelTrait
 */
package sk.enorials.minimap.radar;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.mcmonkey.sentinel.SentinelTrait;

final class SentinelRadarBridge {
    SentinelRadarBridge() {
    }

    static boolean targets(NPC nPC, Player player) {
        SentinelTrait sentinelTrait = (SentinelTrait)nPC.getTraitNullable(SentinelTrait.class);
        return sentinelTrait != null && !sentinelTrait.allIgnores.isTarget((LivingEntity)player, sentinelTrait) && sentinelTrait.allTargets.isTarget((LivingEntity)player, sentinelTrait);
    }
}

