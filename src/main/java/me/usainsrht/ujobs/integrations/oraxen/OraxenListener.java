package me.usainsrht.ujobs.integrations.oraxen;

import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockBreakEvent;
import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockPlaceEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurniturePlaceEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockPlaceEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockBreakEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockPlaceEvent;
import me.usainsrht.ujobs.integrations.ActionReporter;
import me.usainsrht.ujobs.integrations.CustomContentListener;
import me.usainsrht.ujobs.integrations.CustomContentProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

class OraxenListener extends CustomContentListener {

    OraxenListener(ActionReporter reporter, CustomContentProvider provider) {
        super(reporter, provider);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNoteBlockBreak(OraxenNoteBlockBreakEvent e) {
        reportBreak(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStringBlockBreak(OraxenStringBlockBreakEvent e) {
        reportBreak(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChorusBlockBreak(OraxenChorusBlockBreakEvent e) {
        reportBreak(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(OraxenFurnitureBreakEvent e) {
        reportBreak(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNoteBlockPlace(OraxenNoteBlockPlaceEvent e) {
        reportPlace(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStringBlockPlace(OraxenStringBlockPlaceEvent e) {
        reportPlace(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChorusBlockPlace(OraxenChorusBlockPlaceEvent e) {
        reportPlace(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurniturePlace(OraxenFurniturePlaceEvent e) {
        reportPlace(e.getPlayer(), e.getMechanic().getItemID());
    }
}
