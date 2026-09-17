/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.quest;

import java.util.List;
import sk.enorials.minimap.quest.QuestMarkerMappings;

public enum QuestMarkerState {
    UNAVAILABLE("quest_unavailable", 10),
    AVAILABLE("quest_available", 20),
    DAILY("quest_daily", 20),
    INCOMPLETE("quest_incomplete", 30),
    ACTIVE("quest_active", 40),
    COMPLETE("quest_complete", 50);

    private final String icon;
    private final int priority;

    private QuestMarkerState(String string2, int n2) {
        this.icon = string2;
        this.priority = n2;
    }

    public String icon() {
        return this.icon;
    }

    public int priority() {
        return this.priority;
    }

    public boolean visibleOnMap() {
        return this != UNAVAILABLE;
    }

    public boolean keepOnMapEdge() {
        return this == ACTIVE || this == INCOMPLETE || this == COMPLETE;
    }

    public static QuestMarkerState fromLines(List<String> list) {
        return QuestMarkerMappings.defaults().fromLines(list);
    }
}

