/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 */
package sk.enorials.partyhud.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import sk.enorials.partyhud.ui.GuiType;

public class GuiHolder
implements InventoryHolder {
    private final GuiType type;
    private final int page;

    public GuiHolder(GuiType guiType, int n) {
        this.type = guiType;
        this.page = n;
    }

    public GuiType getType() {
        return this.type;
    }

    public int getPage() {
        return this.page;
    }

    public Inventory getInventory() {
        return null;
    }
}

