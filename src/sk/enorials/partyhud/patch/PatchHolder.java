/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 */
package sk.enorials.partyhud.patch;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import sk.enorials.partyhud.patch.PatchMenu;

public final class PatchHolder
implements InventoryHolder {
    private final PatchMenu menu;
    private final int page;

    public PatchHolder(PatchMenu patchMenu, int n) {
        this.menu = patchMenu;
        this.page = n;
    }

    public PatchMenu getMenu() {
        return this.menu;
    }

    public int getPage() {
        return this.page;
    }

    public Inventory getInventory() {
        throw new UnsupportedOperationException("PatchHolder does not store inventory");
    }
}

