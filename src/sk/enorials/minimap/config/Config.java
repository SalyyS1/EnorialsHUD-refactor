/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.config;

import java.util.ArrayList;
import java.util.List;
import sk.enorials.hud.config.YamlSettings;
import sk.enorials.minimap.map.MinimapScreenPosition;
import sk.enorials.minimap.map.MinimapShape;
import sk.enorials.minimap.map.MinimapSize;

public class Config
extends YamlSettings {
    private static final Config INSTANCE = new Config();
    public boolean enabledByDefault = true;
    public MinimapScreenPosition defaultPosition = MinimapScreenPosition.RIGHT;
    public MinimapSize defaultSize = MinimapSize.LARGE;
    public MinimapShape defaultShape = MinimapShape.CIRCLE;
    public boolean defaultDangerRingEnabled = false;
    public boolean defaultFogStylingEnabled = true;
    public boolean defaultCombatZoomEnabled = false;
    public String defaultMinimapRenderer = "vanilla";
    public Markers markers = new Markers();
    public Fullscreen fullscreen = new Fullscreen();
    public Performance performance = new Performance();
    public Poi poi = new Poi();
    public Coordinates coordinates = new Coordinates();
    public Radar radar = new Radar();
    public boolean playerIconEnabled = true;
    public DataRetention dataRetention = new DataRetention();
    public ResourcePack resourcePack = new ResourcePack();

    public static Config instance() {
        return INSTANCE;
    }

    Config() {
    }

    public static class Markers {
        public DeathMarker deathMarker = new DeathMarker();
        public CustomMarkers customMarkers = new CustomMarkers();

        public static class DeathMarker {
            public boolean enabled = true;
            public boolean stickToBorder = true;
            public double removeRadiusBlocks = 3.0;
            public double removeVerticalRangeBlocks = 3.0;
        }

        public static class CustomMarkers {
            public int limit = 5;
            public int unlimitedLimit = 3;
            public boolean stickToBorder = true;
        }
    }

    public static class Fullscreen {
        public int segmentsX = 5;
        public int segmentsZ = 3;
    }

    public static class Performance {
        public int movementUpdateIntervalTicks = 1;
        public int orientationUpdateIntervalTicks = 1;
    }

    public static class Poi {
        public int defaultDurationMinutes = 30;
    }

    public static class Coordinates {
        public boolean enabled = false;
        public int textY = 112;
    }

    public static class Radar {
        public boolean enabled = true;
        public int updateIntervalTicks = 2;
        public int radiusBlocks = 64;
        public int verticalRangeBlocks = 15;
        public int maxEntities = 48;
        public boolean defaultAggressiveEnabled = true;
        public boolean defaultNeutralEnabled = true;
        public boolean defaultFriendlyEnabled = true;
        public List<String> aggressiveMythicMobs = new ArrayList<String>();
        public List<String> neutralMythicMobs = new ArrayList<String>();
        public List<String> friendlyMythicMobs = new ArrayList<String>();
    }

    public static class DataRetention {
        public int playerDataDays = 45;
        public int cleanupIntervalMinutes = 15;
    }

    public static class ResourcePack {
        public boolean enabled = false;
        public boolean required = false;
        public String renderMode = "gui";
        public boolean includeShaders = false;
        public String prompt = "&6This server recommends the minimap resource pack.";
        public String kickMessage = "&cYou must accept the minimap resource pack to play.";
        public Urls url = new Urls();

        public boolean fallbackMode() {
            return "fallback".equalsIgnoreCase(this.renderMode);
        }

        public boolean guiMode() {
            return "gui".equalsIgnoreCase(this.renderMode);
        }

        public boolean shouldIncludeShaders() {
            return !this.fallbackMode() && this.includeShaders;
        }

        public static class Urls {
            public String v1_19 = "";
            public String v1_20 = "";
            public String v1_21 = "";
        }
    }
}

