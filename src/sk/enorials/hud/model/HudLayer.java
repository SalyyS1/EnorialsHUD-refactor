/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 */
package sk.enorials.hud.model;

import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.condition.ConditionHolder;
import sk.enorials.hud.model.HudAlignment;
import sk.enorials.hud.model.HudAsset;
import sk.enorials.hud.model.LayerScale;
import sk.enorials.hud.model.TextLayer;
import sk.enorials.hud.model.TextureLayer;
import sk.enorials.hud.model.position.OffsetPosition;

public interface HudLayer {
    public String getKey();

    public HudAsset getAsset();

    public OffsetPosition getOffset();

    public HudAlignment getAlignment();

    public LayerScale getScale();

    public boolean isOutlined();

    public ConditionHolder getConditions();

    public static HudLayer from(HudAsset hudAsset, String string, ConfigurationSection configurationSection, EnorialsHUD enorialsHUD) {
        if (configurationSection.isConfigurationSection("texture")) {
            return new TextureLayer(hudAsset, string, configurationSection, enorialsHUD);
        }
        if (configurationSection.isSet("text")) {
            return new TextLayer(hudAsset, string, configurationSection, enorialsHUD);
        }
        throw new IllegalArgumentException("Invalid layer config: " + string);
    }

    public static HudAlignment parseAlignment(ConfigurationSection configurationSection) {
        return HudAlignment.valueOf(configurationSection.getString("align", "RIGHT").toUpperCase(Locale.ROOT));
    }
}

