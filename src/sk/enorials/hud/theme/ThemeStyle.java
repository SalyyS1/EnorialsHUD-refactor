/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 */
package sk.enorials.hud.theme;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.theme.HudTheme;

public final class ThemeStyle {
    private static final Map<Key, Key[]> FONTS = ThemeStyle.fonts();

    private ThemeStyle() {
    }

    private static Map<Key, Key[]> fonts() {
        HashMap<Key, Key[]> hashMap;
        LinkedHashSet<Object> linkedHashSet = new LinkedHashSet<Object>();
        for (String iterator : List.of("player-panel", "target-panel", "party-panel", "gps-panel")) {
            linkedHashSet.add("enorialshud:layout/" + iterator + "/textures");
        }
        for (String string : List.of("player", "player-compact")) {
            linkedHashSet.add("enorialshud:layout/player-panel/fonts/" + string + "/head");
        }
        linkedHashSet.add("enorials:quest_log/ui");
        linkedHashSet.add("enorials:conversation/ui");
        try {
            hashMap = ThemeStyle.class.getResourceAsStream("/theme-fonts.properties");
            try {
                if (hashMap != null) {
                    Properties properties = new Properties();
                    properties.load((InputStream)((Object)hashMap));
                    linkedHashSet.addAll(properties.stringPropertyNames());
                }
            }
            finally {
                if (hashMap != null) {
                    ((InputStream)((Object)hashMap)).close();
                }
            }
        }
        catch (IOException iOException) {
            throw new IllegalStateException("Cannot load theme font index", iOException);
        }
        hashMap = new HashMap<Key, Key[]>();
        for (String string : linkedHashSet) {
            Key key = Key.key((String)string);
            Key[] keyArray = new Key[HudTheme.values().length];
            for (HudTheme hudTheme : HudTheme.values()) {
                keyArray[hudTheme.ordinal()] = hudTheme == HudTheme.ORIGINAL ? key : Key.key((String)key.namespace(), (String)("themes/" + hudTheme.id() + "/" + key.value()));
            }
            hashMap.put(key, keyArray);
        }
        return Map.copyOf(hashMap);
    }

    public static Component paint(Component component, HudTheme hudTheme) {
        if (hudTheme == HudTheme.ORIGINAL || component.equals((Object)Component.empty())) {
            return component;
        }
        Key[] keyArray = LegacyText.font(component) == null ? null : FONTS.get(LegacyText.font(component));
        Component component2 = keyArray == null ? component : LegacyText.font(component, keyArray[hudTheme.ordinal()]);
        List list = component.children();
        ArrayList<Component> arrayList = null;
        for (int i = 0; i < list.size(); ++i) {
            Component component3 = (Component)list.get(i);
            Component component4 = ThemeStyle.paint(component3, hudTheme);
            if (component4 == component3) continue;
            if (arrayList == null) {
                arrayList = new ArrayList<Component>(list);
            }
            arrayList.set(i, component4);
        }
        return arrayList == null ? component2 : component2.children(arrayList);
    }
}

