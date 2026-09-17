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
import sk.enorials.hud.model.HudLayer;
import sk.enorials.hud.model.LayerScale;
import sk.enorials.hud.model.position.OffsetPosition;

public class TextLayer
implements HudLayer {
    private final String key;
    private final HudAsset asset;
    private final String text;
    private final String font;
    private final HudAlignment alignment;
    private final boolean outlined;
    private final LayerScale scale;
    private final OffsetPosition offset;
    private final ConditionHolder conditions;

    public TextLayer(HudAsset hudAsset, String string, ConfigurationSection configurationSection, EnorialsHUD enorialsHUD) {
        this.key = string.toLowerCase(Locale.ROOT);
        this.asset = hudAsset;
        this.text = configurationSection.getString("text", "");
        this.font = configurationSection.getString("font", "default");
        this.alignment = HudLayer.parseAlignment(configurationSection);
        this.outlined = configurationSection.getBoolean("outlined", false);
        this.scale = LayerScale.from(configurationSection.getConfigurationSection("scale"));
        int n = configurationSection.getInt("offset.x", 0);
        int n2 = configurationSection.getInt("offset.y", 0);
        double d = configurationSection.getDouble("offset.z", 0.0);
        this.offset = new OffsetPosition(n, n2, d);
        this.conditions = new ConditionHolder(enorialsHUD, "layer:" + hudAsset.getKey() + ":" + this.key, hudAsset, configurationSection.getStringList("conditions"));
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public HudAsset getAsset() {
        return this.asset;
    }

    @Override
    public OffsetPosition getOffset() {
        return this.offset;
    }

    @Override
    public HudAlignment getAlignment() {
        return this.alignment;
    }

    @Override
    public LayerScale getScale() {
        return this.scale;
    }

    @Override
    public boolean isOutlined() {
        return this.outlined;
    }

    @Override
    public ConditionHolder getConditions() {
        return this.conditions;
    }

    public String getText() {
        return this.text;
    }

    public String getFont() {
        return this.font;
    }
}

