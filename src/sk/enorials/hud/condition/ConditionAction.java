/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.TextColor
 */
package sk.enorials.hud.condition;

import java.util.Locale;
import net.kyori.adventure.text.format.TextColor;

/*
 * Uses 'sealed' constructs - enablewith --sealed true
 */
public enum ConditionAction {
    HIDE,
    TEXTURE{

        @Override
        public void validateVariable(String string) {
        }
    }
    ,
    COLOR{

        @Override
        public void validateVariable(String string) {
            if (string == null || TextColor.fromHexString((String)string) == null) {
                // empty if block
            }
        }
    };


    public static ConditionAction of(String string) {
        if (string == null) {
            return HIDE;
        }
        try {
            return ConditionAction.valueOf(string.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return HIDE;
        }
    }

    public void validateVariable(String string) {
    }
}

