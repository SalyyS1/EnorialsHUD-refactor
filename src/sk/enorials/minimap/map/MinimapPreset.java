/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map;

import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapScreenPosition;
import sk.enorials.minimap.map.MinimapShape;
import sk.enorials.minimap.map.MinimapSize;
import sk.enorials.minimap.radar.RadarCategory;

public record MinimapPreset(MinimapScreenPosition position, MinimapSize size, MinimapShape shape, boolean dangerRingEnabled, boolean fogStylingEnabled, Boolean coordinatesEnabled, Integer frameStyle, Integer colorProfile, Boolean combatZoomEnabled, Boolean radarMasterEnabled, Boolean radarAggressiveEnabled, Boolean radarNeutralEnabled, Boolean radarFriendlyEnabled, Integer zoomLevel) {
    public MinimapPreset(MinimapScreenPosition minimapScreenPosition, MinimapSize minimapSize, MinimapShape minimapShape, boolean bl, boolean bl2, Boolean bl3, Integer n, Integer n2, Boolean bl4, Boolean bl5, Boolean bl6, Boolean bl7, Boolean bl8) {
        this(minimapScreenPosition, minimapSize, minimapShape, bl, bl2, bl3, n, n2, bl4, bl5, bl6, bl7, bl8, null);
    }

    public MinimapPreset(MinimapScreenPosition minimapScreenPosition, MinimapSize minimapSize, MinimapShape minimapShape, boolean bl, boolean bl2) {
        this(minimapScreenPosition, minimapSize, minimapShape, bl, bl2, null, null, null, null, null, null, null, null);
    }

    public static MinimapPreset capture(Minimap minimap) {
        return new MinimapPreset(minimap.screenPosition(), minimap.size(), minimap.shape(), minimap.dangerRingEnabled(), minimap.fogStylingEnabled(), minimap.coordinatesEnabled(), minimap.frameStyle(), minimap.colorProfile(), minimap.combatZoomEnabled(), minimap.radarMasterEnabled(), minimap.radarEnabled(RadarCategory.AGGRESSIVE), minimap.radarEnabled(RadarCategory.NEUTRAL), minimap.radarEnabled(RadarCategory.FRIENDLY), minimap.zoomLevel());
    }

    public void apply(Minimap minimap) {
        minimap.screenPosition(this.position);
        minimap.size(this.size);
        minimap.shape(this.shape);
        minimap.dangerRingEnabled(this.dangerRingEnabled);
        minimap.fogStylingEnabled(this.fogStylingEnabled);
        if (this.coordinatesEnabled != null) {
            minimap.coordinatesEnabled(this.coordinatesEnabled);
        }
        if (this.frameStyle != null) {
            minimap.frameStyle(this.frameStyle);
        }
        if (this.colorProfile != null) {
            minimap.colorProfile(this.colorProfile);
        }
        if (this.combatZoomEnabled != null) {
            minimap.combatZoomEnabled(this.combatZoomEnabled);
        }
        if (this.radarMasterEnabled != null) {
            minimap.radarMasterEnabled(this.radarMasterEnabled);
        }
        if (this.radarAggressiveEnabled != null) {
            minimap.radarEnabled(RadarCategory.AGGRESSIVE, this.radarAggressiveEnabled);
        }
        if (this.radarNeutralEnabled != null) {
            minimap.radarEnabled(RadarCategory.NEUTRAL, this.radarNeutralEnabled);
        }
        if (this.radarFriendlyEnabled != null) {
            minimap.radarEnabled(RadarCategory.FRIENDLY, this.radarFriendlyEnabled);
        }
        if (this.zoomLevel != null) {
            minimap.zoomLevel(this.zoomLevel);
        }
    }

    private static String value(Object object) {
        Boolean bl;
        String string = object == null ? "" : (object instanceof Boolean ? ((bl = (Boolean)object).booleanValue() ? "1" : "0") : object.toString());
        return string;
    }

    public String serialize() {
        return this.position.name() + "|" + this.size.name() + "|" + this.shape.name() + "|" + (this.dangerRingEnabled ? "1" : "0") + "|" + (this.fogStylingEnabled ? "1" : "0") + "|" + String.join((CharSequence)"|", MinimapPreset.value(this.coordinatesEnabled), MinimapPreset.value(this.frameStyle), MinimapPreset.value(this.colorProfile), MinimapPreset.value(this.combatZoomEnabled), MinimapPreset.value(this.radarMasterEnabled), MinimapPreset.value(this.radarAggressiveEnabled), MinimapPreset.value(this.radarNeutralEnabled), MinimapPreset.value(this.radarFriendlyEnabled), MinimapPreset.value(this.zoomLevel));
    }

    public static MinimapPreset deserialize(String string) {
        if (string != null && !string.isBlank()) {
            String[] stringArray = string.split("\\|", -1);
            if (stringArray.length < 5) {
                return null;
            }
            try {
                return new MinimapPreset(MinimapScreenPosition.valueOf(stringArray[0]), MinimapSize.valueOf(stringArray[1]), MinimapShape.valueOf(stringArray[2]), "1".equals(stringArray[3]) || "true".equalsIgnoreCase(stringArray[3]), "1".equals(stringArray[4]) || "true".equalsIgnoreCase(stringArray[4]), MinimapPreset.bool(stringArray, 5), MinimapPreset.number(stringArray, 6), MinimapPreset.number(stringArray, 7), MinimapPreset.bool(stringArray, 8), MinimapPreset.bool(stringArray, 9), MinimapPreset.bool(stringArray, 10), MinimapPreset.bool(stringArray, 11), MinimapPreset.bool(stringArray, 12), MinimapPreset.number(stringArray, 13));
            }
            catch (IllegalArgumentException illegalArgumentException) {
                return null;
            }
        }
        return null;
    }

    private static Boolean bool(String[] stringArray, int n) {
        return n >= stringArray.length || stringArray[n].isBlank() ? null : Boolean.valueOf("1".equals(stringArray[n]) || "true".equalsIgnoreCase(stringArray[n]));
    }

    private static Integer number(String[] stringArray, int n) {
        return n >= stringArray.length || stringArray[n].isBlank() ? null : Integer.valueOf(stringArray[n]);
    }
}

