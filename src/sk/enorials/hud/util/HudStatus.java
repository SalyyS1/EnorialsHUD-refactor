/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectTypeCategory
 */
package sk.enorials.hud.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectTypeCategory;

public final class HudStatus {
    public static final int UNKNOWN_DURATION = -2;
    public static final String BURNING = "enorials:burning";
    public static final String DROWNING = "enorials:drowning";
    public static final String FROZEN = "enorials:frozen";
    public static final int HARMFUL_FRAME_RGB = 14048358;
    public static final int BENEFICIAL_FRAME_RGB = 6476998;

    private HudStatus() {
    }

    public static Snapshot snapshot(Player player) {
        if (player == null || player.isDead()) {
            return new Snapshot(HealthTone.NORMAL, List.of());
        }
        ArrayList<Effect> arrayList = new ArrayList<Effect>();
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            arrayList.add(new Effect(potionEffect.getType().getKey().toString(), potionEffect.getType().getCategory() == PotionEffectTypeCategory.HARMFUL, potionEffect.getAmplifier(), potionEffect.isInfinite() ? -1 : potionEffect.getDuration(), potionEffect.hasIcon()));
        }
        return HudStatus.snapshot(arrayList, player.getFireTicks(), player.isUnderWater(), player.getRemainingAir(), player.canBreatheUnderwater(), player.isFrozen());
    }

    public static Snapshot snapshot(Collection<Effect> collection, int n, boolean bl, int n2, boolean bl2, boolean bl3) {
        boolean bl4;
        ArrayList<Effect> arrayList = new ArrayList<Effect>();
        for (Effect effect2 : collection) {
            if (effect2.remainingTicks() <= 0 && effect2.remainingTicks() != -1) continue;
            arrayList.add(effect2);
        }
        boolean bl5 = HudStatus.contains(arrayList, "minecraft:fire_resistance");
        boolean bl6 = bl2 || HudStatus.contains(arrayList, "minecraft:water_breathing") || HudStatus.contains(arrayList, "minecraft:conduit_power");
        boolean bl7 = bl && n2 <= 0 && !bl6;
        boolean bl8 = bl4 = n > 0 && !bl5;
        HealthTone healthTone = HudStatus.contains(arrayList, "minecraft:wither") ? HealthTone.WITHER : (HudStatus.contains(arrayList, "minecraft:poison") ? HealthTone.POISON : (bl7 ? HealthTone.DROWNING : (bl4 ? HealthTone.BURNING : (bl3 ? HealthTone.FROZEN : (arrayList.stream().anyMatch(Effect::harmful) ? HealthTone.HARMFUL : HealthTone.NORMAL)))));
        ArrayList<Effect> arrayList2 = new ArrayList<Effect>(arrayList);
        if (bl4) {
            arrayList2.add(new Effect(BURNING, true, 0, n, true));
        }
        if (bl7) {
            arrayList2.add(new Effect(DROWNING, true, 0, -2, true));
        }
        if (bl3) {
            arrayList2.add(new Effect(FROZEN, true, 0, -2, true));
        }
        arrayList2.sort(Comparator.comparing(Effect::harmful).reversed().thenComparingInt(effect -> HudStatus.priority(effect.key())).thenComparing(Effect::key));
        return new Snapshot(healthTone, List.copyOf(arrayList2));
    }

    private static boolean contains(List<Effect> list, String string) {
        return list.stream().anyMatch(effect -> effect.key().equals(string));
    }

    private static int priority(String string) {
        return switch (string) {
            case "minecraft:wither" -> 0;
            case "minecraft:poison" -> 1;
            case DROWNING -> 2;
            case BURNING -> 3;
            case FROZEN -> 4;
            default -> 5;
        };
    }

    public static String timer(int n) {
        if (n == -1) {
            return "\u221e";
        }
        if (n <= 0) {
            return "";
        }
        long l = ((long)n + 19L) / 20L;
        return l < 60L ? l + "s" : l / 60L + "m";
    }

    public record Snapshot(HealthTone tone, List<Effect> effects) {
    }

    public static enum HealthTone {
        NORMAL(12398641),
        WITHER(9202079),
        POISON(8894275),
        DROWNING(5617371),
        BURNING(15696703),
        FROZEN(9754599),
        HARMFUL(13263741);

        private final int rgb;

        private HealthTone(int n2) {
            this.rgb = n2;
        }

        public int rgb() {
            return this.rgb;
        }
    }

    public record Effect(String key, boolean harmful, int amplifier, int remainingTicks, boolean showIcon) {
        public String timer() {
            return HudStatus.timer(this.remainingTicks);
        }

        public int frameRgb() {
            return this.harmful ? 14048358 : 6476998;
        }
    }
}

