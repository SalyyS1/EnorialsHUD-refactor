/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.gps.api;

import sk.enorials.gps.NavigationMode;
import sk.enorials.gps.api.GpsTargetType;

public record GpsQuestRoute(String id, String displayName, GpsTargetType targetType, String targetValue, NavigationMode mode, String source) {
}

