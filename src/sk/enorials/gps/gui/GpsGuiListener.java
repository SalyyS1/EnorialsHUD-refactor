/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 */
package sk.enorials.gps.gui;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import sk.enorials.gps.gui.GpsGuiService;

public final class GpsGuiListener
implements Listener {
    private final GpsGuiService guiService;

    public GpsGuiListener(GpsGuiService gpsGuiService) {
        this.guiService = gpsGuiService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {
        Inventory inventory = inventoryClickEvent.getInventory();
        InventoryHolder inventoryHolder = inventory.getHolder();
        if (!(inventoryHolder instanceof GpsGuiService.Holder)) {
            return;
        }
        GpsGuiService.Holder holder = (GpsGuiService.Holder)inventoryHolder;
        inventoryClickEvent.setCancelled(true);
        HumanEntity humanEntity = inventoryClickEvent.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (inventoryClickEvent.getRawSlot() < 0 || inventoryClickEvent.getRawSlot() >= inventory.getSize()) {
            return;
        }
        this.guiService.handleClick(player, holder.kind(), inventoryClickEvent.getRawSlot(), inventoryClickEvent.getCurrentItem(), inventoryClickEvent.isShiftClick());
    }
}

