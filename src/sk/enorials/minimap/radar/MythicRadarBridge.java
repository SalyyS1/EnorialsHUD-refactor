/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.lumine.mythic.api.mobs.MythicMob
 *  io.lumine.mythic.bukkit.MythicBukkit
 *  io.lumine.mythic.core.mobs.ActiveMob
 *  org.bukkit.entity.LivingEntity
 */
package sk.enorials.minimap.radar;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import java.util.List;
import java.util.Locale;
import org.bukkit.entity.LivingEntity;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.radar.RadarCategory;

final class MythicRadarBridge {
    MythicRadarBridge() {
    }

    static RadarCategory classify(LivingEntity livingEntity) {
        ActiveMob activeMob = MythicBukkit.inst().getMobManager().getActiveMob(livingEntity.getUniqueId()).orElse(null);
        if (activeMob == null) {
            return null;
        }
        MythicMob mythicMob = activeMob.getType();
        String string = mythicMob.getInternalName();
        Config.Radar radar = Config.instance().radar;
        if (MythicRadarBridge.contains(radar.aggressiveMythicMobs, string)) {
            return RadarCategory.AGGRESSIVE;
        }
        if (MythicRadarBridge.contains(radar.neutralMythicMobs, string)) {
            return RadarCategory.NEUTRAL;
        }
        if (MythicRadarBridge.contains(radar.friendlyMythicMobs, string)) {
            return RadarCategory.FRIENDLY;
        }
        String string2 = mythicMob.getConfig().getString("Options.MinimapCategory", "").toUpperCase(Locale.ROOT);
        try {
            if (!string2.isEmpty()) {
                return RadarCategory.valueOf(string2);
            }
        }
        catch (IllegalArgumentException illegalArgumentException) {
            // empty catch block
        }
        List list = mythicMob.getAITargetSelectors();
        if (list != null) {
            boolean bl = false;
            for (String string3 : list) {
                String string4 = string3.toLowerCase(Locale.ROOT).replaceFirst("^\\s*\\d+\\s+", "").trim();
                if (string4.matches("players?(?:\\s|\\{|$).*") || string4.startsWith("nearestattackabletarget{type=player")) {
                    return RadarCategory.AGGRESSIVE;
                }
                if (!string4.equals("clear")) continue;
                bl = true;
            }
            if (bl) {
                return RadarCategory.NEUTRAL;
            }
        }
        if (mythicMob.getConfig().getBoolean("Options.NoAI", false)) {
            return RadarCategory.NEUTRAL;
        }
        return null;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private static boolean contains(List<String> list, String string) {
        if (list == null) return false;
        if (!list.stream().anyMatch(string::equalsIgnoreCase)) return false;
        return true;
    }
}

