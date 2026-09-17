/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 */
package sk.enorials.hud.util;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import sk.enorials.hud.compat.LegacyText;

public final class SpaceUtil {
    private static final Key SPACE_FONT = Key.key((String)"enorialshud:spaces");
    private static final int MAX_STEP = 1280;

    private SpaceUtil() {
    }

    public static Component space(int n) {
        int n2;
        if (n == 0) {
            return Component.empty();
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = Math.abs(n); i > 0; i -= n2) {
            n2 = Math.min(i, 1280);
            char c = (char)((n > 0 ? 57344 : 61440) + n2);
            stringBuilder.append(c);
        }
        return LegacyText.font((Component)Component.text((String)stringBuilder.toString()), SPACE_FONT);
    }
}

