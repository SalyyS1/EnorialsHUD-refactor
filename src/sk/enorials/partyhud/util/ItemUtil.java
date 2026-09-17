/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 */
package sk.enorials.partyhud.util;

import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class ItemUtil {
    private ItemUtil() {
    }

    public static ItemStack item(Material material, String string, List<String> list) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(string);
            if (list != null) {
                itemMeta.setLore(list);
            }
            itemMeta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_DYE});
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    public static ItemStack item(Material material, String string, String ... stringArray) {
        return ItemUtil.item(material, string, stringArray == null ? null : Arrays.asList(stringArray));
    }

    public static ItemStack icon(Material material, int n, String string, String ... stringArray) {
        if (string == null) {
            string = " ";
        } else if (string.isEmpty()) {
            string = " ";
        }
        String string2 = string;
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(string2);
            if (stringArray != null) {
                itemMeta.setLore(Arrays.asList(stringArray));
            }
            itemMeta.setCustomModelData(Integer.valueOf(n));
            itemMeta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_DYE});
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    public static ItemStack icon(int n, String string, String ... stringArray) {
        return ItemUtil.icon(Material.ECHO_SHARD, n, string, stringArray);
    }

    public static ItemStack bgTile(int n) {
        return ItemUtil.icon(Material.ECHO_SHARD, n, " ", new String[0]);
    }

    public static ItemStack playerHead(OfflinePlayer offlinePlayer, String string, String ... stringArray) {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta)itemStack.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(offlinePlayer);
            skullMeta.setDisplayName(string);
            if (stringArray != null) {
                skullMeta.setLore(Arrays.asList(stringArray));
            }
            itemStack.setItemMeta((ItemMeta)skullMeta);
        }
        return itemStack;
    }
}

