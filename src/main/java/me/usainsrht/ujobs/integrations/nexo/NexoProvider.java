package me.usainsrht.ujobs.integrations.nexo;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import me.usainsrht.ujobs.integrations.ActionReporter;
import me.usainsrht.ujobs.integrations.CustomContentProvider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class NexoProvider implements CustomContentProvider {

    @Override
    public String getPrefix() {
        return "nexo";
    }

    @Override
    public @Nullable String getItemId(ItemStack item) {
        return NexoItems.idFromItem(item);
    }

    @Override
    public @Nullable ItemStack createItem(String id) {
        if (!NexoItems.exists(id)) return null;
        ItemBuilder builder = NexoItems.itemFromId(id);
        return builder == null ? null : builder.build();
    }

    @Override
    public void registerListeners(Plugin plugin, ActionReporter reporter) {
        plugin.getServer().getPluginManager().registerEvents(new NexoListener(reporter, this), plugin);
    }
}
