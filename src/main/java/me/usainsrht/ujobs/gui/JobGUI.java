package me.usainsrht.ujobs.gui;

import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

public interface JobGUI extends InventoryHolder {

    void onClick(InventoryClickEvent e);

    /**
     * Clicks in the player's own inventory while the menu is open. Shift-clicks would push items into
     * the menu and a double-click would collect the menu's items, so both are cancelled.
     */
    default void onBottomClick(InventoryClickEvent e) {
        InventoryAction action = e.getAction();
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
        }
    }

    void onDrag(InventoryDragEvent e);

    void onOpen(InventoryOpenEvent e);

    void onClose(InventoryCloseEvent e);

}
