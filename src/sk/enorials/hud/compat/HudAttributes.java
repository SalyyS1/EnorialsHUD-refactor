/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.attribute.Attribute
 */
package sk.enorials.hud.compat;

import org.bukkit.attribute.Attribute;

public final class HudAttributes {
    public static final Attribute MAX_HEALTH = HudAttributes.find("MAX_HEALTH", "GENERIC_MAX_HEALTH");
    public static final Attribute ARMOR = HudAttributes.find("ARMOR", "GENERIC_ARMOR");

    private HudAttributes() {
    }

    private static Attribute find(String string, String string2) {
        for (String string3 : new String[]{string, string2}) {
            try {
                return (Attribute)Attribute.class.getField(string3).get(null);
            }
            catch (NoSuchFieldException noSuchFieldException) {
            }
            catch (ReflectiveOperationException reflectiveOperationException) {
                throw new ExceptionInInitializerError(reflectiveOperationException);
            }
        }
        throw new ExceptionInInitializerError("Missing Bukkit attribute: " + string);
    }
}

