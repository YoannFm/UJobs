package me.usainsrht.ujobs.integrations.nexo;

import com.nexomc.nexo.api.events.custom_block.chorusblock.NexoChorusBlockBreakEvent;
import com.nexomc.nexo.api.events.custom_block.chorusblock.NexoChorusBlockPlaceEvent;
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockBreakEvent;
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockPlaceEvent;
import com.nexomc.nexo.api.events.custom_block.stringblock.NexoStringBlockBreakEvent;
import com.nexomc.nexo.api.events.custom_block.stringblock.NexoStringBlockPlaceEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent;
import me.usainsrht.ujobs.integrations.ActionReporter;
import me.usainsrht.ujobs.integrations.CustomContentListener;
import me.usainsrht.ujobs.integrations.CustomContentProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/** Nexo fires one event per kind of block, none of them reaches a listener of the generic NexoBlock*Event. */
class NexoListener extends CustomContentListener {

    NexoListener(ActionReporter reporter, CustomContentProvider provider) {
        super(reporter, provider);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNoteBlockBreak(NexoNoteBlockBreakEvent e) {
        reportBreak(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStringBlockBreak(NexoStringBlockBreakEvent e) {
        reportBreak(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChorusBlockBreak(NexoChorusBlockBreakEvent e) {
        reportBreak(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(NexoFurnitureBreakEvent e) {
        reportBreak(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNoteBlockPlace(NexoNoteBlockPlaceEvent e) {
        reportPlace(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStringBlockPlace(NexoStringBlockPlaceEvent e) {
        reportPlace(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChorusBlockPlace(NexoChorusBlockPlaceEvent e) {
        reportPlace(e.getPlayer(), e.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurniturePlace(NexoFurniturePlaceEvent e) {
        reportPlace(e.getPlayer(), e.getMechanic().getItemID());
    }
}
