/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 */
package sk.enorials.hud.model;

import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public class LayerScale {
    public static final LayerScale UNSCALED = new LayerScale(1.0, 1.0, false);
    private final double xScale;
    private final double yScale;
    private final boolean scaleOffset;

    public LayerScale(double d, double d2, boolean bl) {
        this.xScale = d;
        this.yScale = d2;
        this.scaleOffset = bl;
    }

    public double getX() {
        return this.xScale;
    }

    public double getY() {
        return this.yScale;
    }

    public boolean isScaleOffset() {
        return this.scaleOffset;
    }

    public static LayerScale from(ConfigurationSection configurationSection) {
        if (configurationSection == null) {
            return UNSCALED;
        }
        double d = configurationSection.getDouble("x", 1.0);
        double d2 = configurationSection.getDouble("y", 1.0);
        boolean bl = configurationSection.getBoolean("scale-offset", false);
        return new LayerScale(d, d2, bl);
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object != null && this.getClass() == object.getClass()) {
            LayerScale layerScale = (LayerScale)object;
            return Double.compare(layerScale.xScale, this.xScale) == 0 && Double.compare(layerScale.yScale, this.yScale) == 0;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.xScale, this.yScale);
    }
}

