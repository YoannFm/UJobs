package me.usainsrht.ujobs.integrations.craftengine;

import me.usainsrht.ujobs.integrations.ActionReporter;
import me.usainsrht.ujobs.integrations.CustomContentListener;
import me.usainsrht.ujobs.integrations.CustomContentProvider;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockPlaceEvent;
import net.momirealms.craftengine.bukkit.api.event.FurnitureBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.FurniturePlaceEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

class CraftEngineListener extends CustomContentListener {

    CraftEngineListener(ActionReporter reporter, CustomContentProvider provider) {
        super(reporter, provider);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(CustomBlockBreakEvent e) {
        reportBreak(e.player().platformPlayer(), e.customBlock().id().asString());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent e) {
        reportBreak(e.player(), e.furniture().id().asString());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(CustomBlockPlaceEvent e) {
        reportPlace(e.player(), e.customBlock().id().asString());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurniturePlace(FurniturePlaceEvent e) {
        reportPlace(e.player(), e.furniture().id().asString());
    }
}
