/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.citizensnpcs.api.CitizensAPI
 *  net.citizensnpcs.api.npc.NPC
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.radar;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import sk.enorials.minimap.radar.RadarCategory;
import sk.enorials.minimap.radar.SentinelRadarBridge;

final class CitizensRadarBridge {
    CitizensRadarBridge() {
    }

    static RadarCategory classify(Player player, LivingEntity livingEntity) {
        NPC nPC = CitizensAPI.getNPCRegistry().getNPC((Entity)livingEntity);
        if (nPC == null) {
            return null;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("Sentinel") && SentinelRadarBridge.targets(nPC, player)) {
            return RadarCategory.AGGRESSIVE;
        }
        return RadarCategory.FRIENDLY;
    }
}

