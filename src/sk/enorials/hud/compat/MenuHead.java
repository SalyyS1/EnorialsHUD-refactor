/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.NamespacedKey
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.meta.ItemMeta
 */
package sk.enorials.hud.compat;

import java.lang.reflect.Method;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import sk.enorials.hud.compat.ClientRenderProfile;

public final class MenuHead {
    private static final Method ITEM_MODEL = MenuHead.method("setItemModel", NamespacedKey.class);
    private static final Method HIDE_TOOLTIP = MenuHead.method("setHideTooltip", Boolean.TYPE);

    private MenuHead() {
    }

    private static Method method(String string, Class<?> clazz) {
        try {
            return ItemMeta.class.getMethod(string, clazz);
        }
        catch (NoSuchMethodException noSuchMethodException) {
            return null;
        }
    }

    public static int modelId(String string) {
        String string2 = "enorials:menu/head_";
        if (!string.startsWith(string2)) {
            throw new IllegalArgumentException("Unknown menu head: " + string);
        }
        String[] stringArray = string.substring(string2.length()).split("_");
        if (stringArray.length != 3) {
            throw new IllegalArgumentException("Invalid menu head: " + string);
        }
        int n = Integer.parseInt(stringArray[0]);
        int n2 = Integer.parseInt(stringArray[1]);
        int n3 = Integer.parseInt(stringArray[2]);
        if (n < -16 || n > 15 || n2 < -16 || n2 > 15 || n3 < 1 || n3 > 63) {
            throw new IllegalArgumentException("Menu head geometry out of range: " + string);
        }
        return 900000 + (n + 16 << 11) + (n2 + 16 << 6) + n3;
    }

    public static void apply(ItemMeta itemMeta, String string) {
        MenuHead.apply(itemMeta, string, ClientRenderProfile.nativeProfile());
    }

    public static void apply(ItemMeta itemMeta, String string, ClientRenderProfile clientRenderProfile) {
        try {
            if (ITEM_MODEL != null && clientRenderProfile.itemModel()) {
                ITEM_MODEL.invoke((Object)itemMeta, NamespacedKey.fromString((String)string));
            }
            itemMeta.setCustomModelData(Integer.valueOf(MenuHead.modelId(string)));
            if (HIDE_TOOLTIP != null) {
                HIDE_TOOLTIP.invoke((Object)itemMeta, true);
            } else {
                itemMeta.displayName((Component)Component.empty());
                itemMeta.addItemFlags(ItemFlag.values());
            }
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException("Cannot apply menu head model", reflectiveOperationException);
        }
    }
}

