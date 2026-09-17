/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 */
package sk.enorials.minimap.quest;

import org.bukkit.Location;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;

public final class QuestMarkerLayer
extends SecondaryMinimapLayer {
    private final String label;

    public QuestMarkerLayer(MinimapLayer minimapLayer, Location location, MinimapIcon minimapIcon, String string) {
        super(minimapLayer, new MinimapIconRenderer(minimapIcon), true, true, location.getWorld(), location.getBlockX(), location.getBlockZ(), 0.065f);
        this.label = string;
    }

    public String label() {
        return this.label;
    }
}

