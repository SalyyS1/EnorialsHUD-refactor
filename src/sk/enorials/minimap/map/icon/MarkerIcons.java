/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.minimap.map.icon;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import sk.enorials.minimap.map.icon.provider.MinimapIconProvider;

public final class MarkerIcons {
    public static final List<String> BUILTIN = List.of("home", "village", "chest", "pig", "quest_!", "quest_questionmark", "camp", "cave", "mine", "portal", "shop", "bank", "anvil", "fishing", "farm", "stable", "boss", "danger", "waypoint", "treasure");
    public static final int PAGE_SIZE = 6;
    public static final List<String> QUEST_STATES = List.of("quest_available", "quest_daily", "quest_active", "quest_incomplete", "quest_complete", "quest_unavailable");
    public static final List<String> GLYPHS = Stream.concat(Stream.concat(BUILTIN.stream(), List.of("death", "offscreen_player", "party_1", "party_2", "party_3", "party_4", "party_5", "party_6", "party_7", "party_8").stream()), QUEST_STATES.stream()).toList();

    private MarkerIcons() {
    }

    public static String textureKey(String string) {
        return string.replace("quest_!", "quest_exclamation");
    }

    public static List<String> selectable(MinimapIconProvider minimapIconProvider) {
        Set<String> set = minimapIconProvider.genericIconKeys();
        ArrayList<String> arrayList = new ArrayList<String>();
        for (String string2 : BUILTIN) {
            if (!set.contains(string2)) continue;
            arrayList.add(string2);
        }
        set.stream().filter(string -> !BUILTIN.contains(string)).sorted(String.CASE_INSENSITIVE_ORDER).forEach(arrayList::add);
        return List.copyOf(arrayList);
    }

    public static String label(String string) {
        return switch (string) {
            case "quest_!" -> "Quest";
            case "quest_questionmark" -> "Quest help";
            case "cave" -> "Dungeon";
            case "camp" -> "Camp";
            case "death" -> "Death location";
            case "quest_available" -> "Available quest";
            case "quest_daily" -> "Daily quest";
            case "quest_active" -> "Quest target";
            case "quest_incomplete" -> "In progress";
            case "quest_complete" -> "Ready to turn in";
            case "quest_unavailable" -> "Locked quest";
            default -> string.isEmpty() ? "Marker" : Character.toUpperCase(string.charAt(0)) + string.substring(1).replace('_', ' ');
        };
    }

    public static boolean hasGlyph(String string) {
        return GLYPHS.contains(string);
    }
}

