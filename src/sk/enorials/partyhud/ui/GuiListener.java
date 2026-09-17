/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.InventoryHolder
 */
package sk.enorials.partyhud.ui;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import sk.enorials.partyhud.ui.GuiHolder;
import sk.enorials.partyhud.ui.GuiService;

public class GuiListener
implements Listener {
    private final GuiService guiService;

    public GuiListener(GuiService guiService) {
        this.guiService = guiService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent inventoryClickEvent) {
        InventoryHolder inventoryHolder = inventoryClickEvent.getInventory().getHolder();
        if (inventoryHolder instanceof GuiHolder) {
            GuiHolder guiHolder = (GuiHolder)inventoryHolder;
            inventoryClickEvent.setCancelled(true);
            HumanEntity humanEntity = inventoryClickEvent.getWhoClicked();
            if (humanEntity instanceof Player) {
                inventoryHolder = (Player)humanEntity;
                this.guiService.handleClick((Player)inventoryHolder, guiHolder, inventoryClickEvent.getSlot(), inventoryClickEvent.getClick(), inventoryClickEvent.getCurrentItem());
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent inventoryCloseEvent) {
        if (inventoryCloseEvent.getInventory().getHolder() instanceof GuiHolder) {
            // empty if block
        }
    }
}

