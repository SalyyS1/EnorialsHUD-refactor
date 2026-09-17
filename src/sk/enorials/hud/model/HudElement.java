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
import sk.enorials.hud.model.HudAlignment;
import sk.enorials.hud.model.HudAsset;
import sk.enorials.hud.model.position.OffsetPosition;
import sk.enorials.hud.model.position.ScreenPosition;

public class HudElement {
    private final String key;
    private final HudAsset asset;
    private final HudAlignment alignment;
    private final ScreenPosition screenPosition;

    public HudElement(ConfigurationSection configurationSection) {
        this.key = configurationSection.getName().toLowerCase(Locale.ROOT);
        String string = configurationSection.getString("asset", "");
        this.asset = EnorialsHUD.getInstance().assets().get(string);
        if (this.asset == null) {
            throw new IllegalArgumentException("Asset does not exist: " + string);
        }
        this.alignment = HudAlignment.valueOf(configurationSection.getString("align", "RIGHT").toUpperCase(Locale.ROOT));
        if (!configurationSection.isConfigurationSection("position")) {
            throw new IllegalArgumentException("Element position is not defined.");
        }
        float f = (float)configurationSection.getDouble("position.x", 50.0);
        float f2 = (float)configurationSection.getDouble("position.y", 50.0);
        int n = configurationSection.getInt("offset.x", 0);
        int n2 = configurationSection.getInt("offset.y", 0);
        double d = configurationSection.getDouble("offset.z", 0.0);
        this.screenPosition = new ScreenPosition(f, f2, new OffsetPosition(n, n2, d));
    }

    public String getKey() {
        return this.key;
    }

    public HudAsset getAsset() {
        return this.asset;
    }

    public HudAlignment getAlignment() {
        return this.alignment;
    }

    public ScreenPosition getScreenPosition() {
        return this.screenPosition;
    }
}

