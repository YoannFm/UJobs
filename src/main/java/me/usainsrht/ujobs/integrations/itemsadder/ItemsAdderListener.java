package me.usainsrht.ujobs.integrations.itemsadder;

import dev.lone.itemsadder.api.Events.ComplexFurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.ComplexFurniturePlacedEvent;
import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurniturePlacedEvent;
import me.usainsrht.ujobs.integrations.ActionReporter;
import me.usainsrht.ujobs.integrations.CustomContentListener;
import me.usainsrht.ujobs.integrations.CustomContentProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

class ItemsAdderListener extends CustomContentListener {

    ItemsAdderListener(ActionReporter reporter, CustomContentProvider provider) {
        super(reporter, provider);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(CustomBlockBreakEvent e) {
        reportBreak(e.getPlayer(), e.getNamespacedID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent e) {
        reportBreak(e.getPlayer(), e.getNamespacedID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onComplexFurnitureBreak(ComplexFurnitureBreakEvent e) {
        reportBreak(e.getPlayer(), e.getNamespacedID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(CustomBlockPlaceEvent e) {
        reportPlace(e.getPlayer(), e.getNamespacedID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurniturePlace(FurniturePlacedEvent e) {
        reportPlace(e.getPlayer(), e.getNamespacedID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onComplexFurniturePlace(ComplexFurniturePlacedEvent e) {
        reportPlace(e.getPlayer(), e.getNamespacedID());
    }
}
