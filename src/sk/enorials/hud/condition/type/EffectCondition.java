/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.potion.PotionEffectType
 */
package sk.enorials.hud.condition.type;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import sk.enorials.hud.condition.AbstractHudCondition;
import sk.enorials.hud.condition.HudLineConfig;
import sk.enorials.migration.LegacyNames;

public class EffectCondition
extends AbstractHudCondition {
    private final List<PotionEffectType> effects = new ArrayList<PotionEffectType>();

    public EffectCondition(HudLineConfig hudLineConfig) {
        super(hudLineConfig);
        String[] stringArray = hudLineConfig.getStringList("e", "effect", "effects");
        if (stringArray == null) {
            throw new IllegalArgumentException("Effects do not exist.");
        }
        for (String string : stringArray) {
            PotionEffectType potionEffectType = LegacyNames.effect(string);
            if (potionEffectType == null) continue;
            this.effects.add(potionEffectType);
        }
    }

    @Override
    protected boolean conditionResult(Player player) {
        for (PotionEffectType potionEffectType : this.effects) {
            if (!player.hasPotionEffect(potionEffectType)) continue;
            return true;
        }
        return false;
    }
}

