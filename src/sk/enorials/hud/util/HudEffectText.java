/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextColor
 */
package sk.enorials.hud.util;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.util.HudFontMetrics;
import sk.enorials.hud.util.SpaceUtil;

public final class HudEffectText {
    private static final String CHARS = "0123456789smh\u221e";
    public static final float BADGE_CENTER = 5.5f;

    private HudEffectText() {
    }

    public static Component timer(String string, String string2) {
        int n;
        int n2 = string.length();
        int n3 = n2 == 3 ? 59712 : (n = n2 <= 5 ? 59744 : 59776);
        int n4 = n2 == 3 || n2 == 5 ? -5 : (n2 >= 6 ? -8 : 0);
        TextComponent textComponent = Component.empty();
        for (int i = 0; i < n2; ++i) {
            if (i > 0 && n4 != 0) {
                textComponent = textComponent.append(HudEffectText.space(n4, string2));
            }
            int n5 = CHARS.indexOf(string.charAt(i));
            char c = n2 < 3 || n5 < 0 ? string.charAt(i) : (char)(n + n5);
            textComponent = textComponent.append(((TextComponent)LegacyText.font((Component)Component.text((char)c), Key.key((String)string2))).color(TextColor.color((int)0xFFFFFF)));
        }
        return textComponent;
    }

    public static Component center(Component component, String string, HudFontMetrics hudFontMetrics) {
        int n = Math.round((5.5f - hudFontMetrics.measure(component).center()) * 10.0f);
        return HudEffectText.space(n, string).append(component).append(HudEffectText.space(-n, string));
    }

    private static Component space(int n, String string) {
        int n2 = n / 10;
        int n3 = n % 10;
        Component component = SpaceUtil.space(n2);
        if (n3 != 0) {
            int n4 = n3 > 0 ? 59680 : 59696;
            component = component.append(LegacyText.font((Component)Component.text((char)((char)(n4 + Math.abs(n3)))), Key.key((String)string)));
        }
        return component;
    }
}

