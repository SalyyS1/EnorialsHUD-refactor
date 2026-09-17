/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextDecoration
 */
package sk.enorials.hud.compat;

import java.util.ArrayList;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;

public final class LegacyText {
    private LegacyText() {
    }

    public static Component font(Component component, Key key) {
        if (key != null && key.namespace().startsWith("enorials")) {
            component = LegacyText.spaces(component);
        }
        return component.style(component.style().font(key));
    }

    private static Component spaces(Component component2) {
        TextComponent textComponent;
        if (component2 instanceof TextComponent && (textComponent = (TextComponent)component2).content().indexOf(32) >= 0) {
            String[] stringArray = textComponent.content().split(" ", -1);
            ArrayList<Object> arrayList = new ArrayList<Object>();
            for (int i = 1; i < stringArray.length; ++i) {
                arrayList.add(Component.text((String)"\uf8ff").decoration(TextDecoration.BOLD, false));
                if (stringArray[i].isEmpty()) continue;
                arrayList.add(Component.text((String)stringArray[i]));
            }
            arrayList.addAll(component2.children());
            component2 = textComponent.content(stringArray[0]).children(arrayList);
        }
        if (!component2.children().isEmpty()) {
            component2 = component2.children(component2.children().stream().map(component -> component.style().font() == null ? LegacyText.spaces(component) : component).toList());
        }
        return component2;
    }

    public static Key font(Component component) {
        return component.style().font();
    }
}

