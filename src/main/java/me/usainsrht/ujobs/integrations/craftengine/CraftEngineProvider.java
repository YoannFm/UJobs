package me.usainsrht.ujobs.integrations.craftengine;

import me.usainsrht.ujobs.integrations.ActionReporter;
import me.usainsrht.ujobs.integrations.CustomContentProvider;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class CraftEngineProvider implements CustomContentProvider {

    @Override
    public String getPrefix() {
        return "craftengine";
    }

    @Override
    public @Nullable String getItemId(ItemStack item) {
        Key key = CraftEngineItems.getCustomItemId(item);
        return key == null ? null : key.asString();
    }

    @Override
    public @Nullable ItemStack createItem(String id) {
        BukkitItemDefinition definition = CraftEngineItems.byId(Key.of(id));
        return definition == null ? null : definition.buildBukkitItem();
    }

    @Override
    public void registerListeners(Plugin plugin, ActionReporter reporter) {
        plugin.getServer().getPluginManager().registerEvents(new CraftEngineListener(reporter, this), plugin);
    }
}
