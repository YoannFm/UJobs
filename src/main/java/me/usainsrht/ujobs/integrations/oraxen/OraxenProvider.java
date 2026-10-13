package me.usainsrht.ujobs.integrations.oraxen;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import me.usainsrht.ujobs.integrations.ActionReporter;
import me.usainsrht.ujobs.integrations.CustomContentProvider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class OraxenProvider implements CustomContentProvider {

    @Override
    public String getPrefix() {
        return "oraxen";
    }

    @Override
    public @Nullable String getItemId(ItemStack item) {
        return OraxenItems.getIdByItem(item);
    }

    @Override
    public @Nullable ItemStack createItem(String id) {
        ItemBuilder builder = OraxenItems.getItemById(id);
        return builder == null ? null : builder.build();
    }

    @Override
    public void registerListeners(Plugin plugin, ActionReporter reporter) {
        plugin.getServer().getPluginManager().registerEvents(new OraxenListener(reporter, this), plugin);
    }
}
