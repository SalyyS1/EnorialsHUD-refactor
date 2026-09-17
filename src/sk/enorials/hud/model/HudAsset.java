/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 */
package sk.enorials.hud.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.condition.ConditionHolder;
import sk.enorials.hud.model.HudLayer;

public class HudAsset {
    private final String key;
    private final Map<String, HudLayer> layers = new LinkedHashMap<String, HudLayer>();
    private final ConditionHolder conditions;

    public HudAsset(EnorialsHUD enorialsHUD, ConfigurationSection configurationSection) {
        this.key = configurationSection.getName().toLowerCase(Locale.ROOT);
        if (!configurationSection.isConfigurationSection("layers")) {
            throw new IllegalArgumentException("No layers defined: " + this.key);
        }
        this.conditions = new ConditionHolder(enorialsHUD, "asset:" + this.key, this, configurationSection.getStringList("conditions"));
        ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection("layers");
        for (String string : configurationSection2.getKeys(false)) {
            ConfigurationSection configurationSection3 = configurationSection2.getConfigurationSection(string);
            if (configurationSection3 == null) continue;
            HudLayer hudLayer = HudLayer.from(this, string, configurationSection3, enorialsHUD);
            this.layers.put(hudLayer.getKey(), hudLayer);
        }
    }

    public String getKey() {
        return this.key;
    }

    public Collection<HudLayer> getLayers() {
        return this.layers.values();
    }

    public HudLayer getLayer(String string) {
        return this.layers.get(string.toLowerCase(Locale.ROOT));
    }

    public ConditionHolder getConditions() {
        return this.conditions;
    }
}

