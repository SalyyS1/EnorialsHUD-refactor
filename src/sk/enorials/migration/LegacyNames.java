/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Particle
 *  org.bukkit.Registry
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.potion.PotionEffectType
 */
package sk.enorials.migration;

import java.util.Locale;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

public final class LegacyNames {
    private LegacyNames() {
    }

    private static String normalize(String string) {
        return string.toUpperCase(Locale.ROOT).replace("MINECRAFT:", "");
    }

    public static Particle particle(String string) {
        String string2;
        return Particle.valueOf((String)(switch (string2 = LegacyNames.normalize(string)) {
            case "BLOCK_CRACK" -> "BLOCK";
            case "BLOCK_DUST" -> "BLOCK";
            case "SPELL" -> "EFFECT";
            case "SPELL_INSTANT" -> "INSTANT_EFFECT";
            case "SPELL_MOB" -> "ENTITY_EFFECT";
            case "SPELL_MOB_AMBIENT" -> "ENTITY_EFFECT";
            case "CRIT_MAGIC" -> "ENCHANTED_HIT";
            case "VILLAGER_ANGRY" -> "ANGRY_VILLAGER";
            case "VILLAGER_HAPPY" -> "HAPPY_VILLAGER";
            case "ENCHANTMENT_TABLE" -> "ENCHANT";
            case "REDSTONE" -> "DUST";
            case "SMOKE_NORMAL" -> "SMOKE";
            case "SMOKE_LARGE" -> "LARGE_SMOKE";
            case "EXPLOSION_NORMAL" -> "POOF";
            case "EXPLOSION_LARGE" -> "EXPLOSION";
            case "EXPLOSION_HUGE" -> "EXPLOSION_EMITTER";
            case "FIREWORKS_SPARK" -> "FIREWORK";
            case "TOTEM" -> "TOTEM_OF_UNDYING";
            case "WATER_BUBBLE" -> "BUBBLE";
            case "WATER_WAKE" -> "FISHING";
            case "WATER_DROP" -> "RAIN";
            case "SUSPENDED" -> "UNDERWATER";
            default -> string2;
        }));
    }

    public static Enchantment enchantment(String string) {
        String string2;
        string2 = switch (string2 = LegacyNames.normalize(string)) {
            case "DURABILITY" -> "UNBREAKING";
            case "DAMAGE_ALL" -> "SHARPNESS";
            case "DAMAGE_UNDEAD" -> "SMITE";
            case "DAMAGE_ARTHROPODS" -> "BANE_OF_ARTHROPODS";
            case "DIG_SPEED" -> "EFFICIENCY";
            case "LOOT_BONUS_BLOCKS" -> "FORTUNE";
            case "LOOT_BONUS_MOBS" -> "LOOTING";
            case "ARROW_DAMAGE" -> "POWER";
            case "ARROW_KNOCKBACK" -> "PUNCH";
            case "ARROW_FIRE" -> "FLAME";
            case "ARROW_INFINITE" -> "INFINITY";
            case "PROTECTION_ENVIRONMENTAL" -> "PROTECTION";
            case "PROTECTION_FIRE" -> "FIRE_PROTECTION";
            case "PROTECTION_FALL" -> "FEATHER_FALLING";
            case "PROTECTION_EXPLOSIONS" -> "BLAST_PROTECTION";
            case "PROTECTION_PROJECTILE" -> "PROJECTILE_PROTECTION";
            case "OXYGEN" -> "RESPIRATION";
            case "WATER_WORKER" -> "AQUA_AFFINITY";
            case "LUCK" -> "LUCK_OF_THE_SEA";
            case "SWEEPING_EDGE" -> "SWEEPING_EDGE";
            default -> string2;
        };
        return (Enchantment)Registry.ENCHANTMENT.get(NamespacedKey.minecraft((String)string2.toLowerCase(Locale.ROOT)));
    }

    public static PotionEffectType effect(String string) {
        String string2;
        string2 = switch (string2 = LegacyNames.normalize(string)) {
            case "SLOW" -> "SLOWNESS";
            case "SLOW_DIGGING" -> "MINING_FATIGUE";
            case "FAST_DIGGING" -> "HASTE";
            case "INCREASE_DAMAGE" -> "STRENGTH";
            case "HEAL" -> "INSTANT_HEALTH";
            case "HARM" -> "INSTANT_DAMAGE";
            case "JUMP" -> "JUMP_BOOST";
            case "CONFUSION" -> "NAUSEA";
            case "DAMAGE_RESISTANCE" -> "RESISTANCE";
            default -> string2;
        };
        return (PotionEffectType)Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft((String)string2.toLowerCase(Locale.ROOT)));
    }
}

