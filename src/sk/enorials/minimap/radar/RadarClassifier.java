/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Animals
 *  org.bukkit.entity.ArmorStand
 *  org.bukkit.entity.Enemy
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Mob
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Tameable
 */
package sk.enorials.minimap.radar;

import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import sk.enorials.minimap.radar.CitizensRadarBridge;
import sk.enorials.minimap.radar.MythicRadarBridge;
import sk.enorials.minimap.radar.RadarCategory;

public final class RadarClassifier {
    private static final Set<String> NEUTRAL = Set.of("BEE", "ENDERMAN", "ZOMBIFIED_PIGLIN", "PIGLIN", "WOLF", "POLAR_BEAR", "LLAMA", "TRADER_LLAMA", "IRON_GOLEM", "GOAT", "PANDA", "DOLPHIN", "PUFFERFISH");

    public RadarCategory classify(Player player, Entity entity) {
        RadarCategory radarCategory;
        LivingEntity livingEntity;
        if (!(entity instanceof LivingEntity) || (livingEntity = (LivingEntity)entity) instanceof ArmorStand || !livingEntity.isValid() || livingEntity.isDead() || entity.getUniqueId().equals(player.getUniqueId())) {
            return null;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("Citizens") && (radarCategory = CitizensRadarBridge.classify(player, livingEntity)) != null) {
            return radarCategory;
        }
        if (livingEntity instanceof Player) {
            return null;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs") && (radarCategory = MythicRadarBridge.classify(livingEntity)) != null) {
            return radarCategory;
        }
        if (livingEntity instanceof Mob && (radarCategory = (Mob)livingEntity).getTarget() != null && radarCategory.getTarget().getUniqueId().equals(player.getUniqueId())) {
            return RadarCategory.AGGRESSIVE;
        }
        if (livingEntity instanceof Tameable && (radarCategory = (Tameable)livingEntity).isTamed()) {
            return RadarCategory.FRIENDLY;
        }
        if (NEUTRAL.contains(livingEntity.getType().name())) {
            return RadarCategory.NEUTRAL;
        }
        if (livingEntity instanceof Enemy) {
            return RadarCategory.AGGRESSIVE;
        }
        return livingEntity instanceof Animals || livingEntity instanceof Mob ? RadarCategory.FRIENDLY : RadarCategory.NEUTRAL;
    }
}

