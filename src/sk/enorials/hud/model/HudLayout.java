/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 */
package sk.enorials.hud.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;
import sk.enorials.hud.model.HudElement;
import sk.enorials.hud.pack.BuildablePack;

public class HudLayout
implements BuildablePack.HolderLike {
    private final String key;
    private final List<HudElement> elements = new ArrayList<HudElement>();

    public HudLayout(ConfigurationSection configurationSection) {
        this.key = configurationSection.getName().toLowerCase(Locale.ROOT);
        if (!configurationSection.isConfigurationSection("elements")) {
            throw new IllegalArgumentException("No elements defined.");
        }
        ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection("elements");
        for (String string : configurationSection2.getKeys(false)) {
            ConfigurationSection configurationSection3 = configurationSection2.getConfigurationSection(string);
            if (configurationSection3 == null) continue;
            this.elements.add(new HudElement(configurationSection3));
        }
    }

    @Override
    public String getKey() {
        return this.key;
    }

    public List<HudElement> getElements() {
        return this.elements;
    }
}

