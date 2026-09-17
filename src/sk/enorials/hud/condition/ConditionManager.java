/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.condition;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import sk.enorials.hud.condition.HudCondition;
import sk.enorials.hud.condition.HudLineConfig;
import sk.enorials.hud.condition.type.EffectCondition;
import sk.enorials.hud.condition.type.GamemodeCondition;
import sk.enorials.hud.condition.type.PlaceholderCondition;
import sk.enorials.hud.condition.type.ProgressCondition;
import sk.enorials.hud.model.HudAsset;

public class ConditionManager {
    private final Map<String, BiFunction<HudLineConfig, HudAsset, HudCondition>> conditions = new HashMap<String, BiFunction<HudLineConfig, HudAsset, HudCondition>>();

    public ConditionManager() {
        this.addCondition("effect", (hudLineConfig, hudAsset) -> new EffectCondition((HudLineConfig)hudLineConfig));
        this.addCondition("gamemode", (hudLineConfig, hudAsset) -> new GamemodeCondition((HudLineConfig)hudLineConfig));
        this.addCondition("progress", ProgressCondition::new);
        this.addCondition("placeholder", PlaceholderCondition::new);
    }

    public void addCondition(String string, BiFunction<HudLineConfig, HudAsset, HudCondition> biFunction) {
        this.conditions.put(string.toLowerCase(Locale.ROOT), biFunction);
    }

    public boolean isCondition(String string) {
        return this.conditions.containsKey(string.toLowerCase(Locale.ROOT));
    }

    public HudCondition getCondition(String string, HudAsset hudAsset) {
        HudLineConfig hudLineConfig = HudLineConfig.of(string);
        if (this.isCondition(hudLineConfig.getKey())) {
            if ("progress".equalsIgnoreCase(hudLineConfig.getKey()) && hudAsset == null) {
                throw new IllegalArgumentException("Progress condition requires an asset.");
            }
            return this.conditions.get(hudLineConfig.getKey()).apply(hudLineConfig, hudAsset);
        }
        throw new IllegalArgumentException("Condition does not exist.");
    }
}

