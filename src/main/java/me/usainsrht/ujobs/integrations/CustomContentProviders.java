package me.usainsrht.ujobs.integrations;

import me.usainsrht.ujobs.integrations.craftengine.CraftEngineProvider;
import me.usainsrht.ujobs.integrations.itemsadder.ItemsAdderProvider;
import me.usainsrht.ujobs.integrations.nexo.NexoProvider;
import me.usainsrht.ujobs.integrations.oraxen.OraxenProvider;

import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Detects the installed custom content plugins and hooks into them. */
public final class CustomContentProviders {

    private CustomContentProviders() {
    }

    public static List<CustomContentProvider> detect(Plugin plugin) {
        List<CustomContentProvider> providers = new ArrayList<>();
        if (isEnabled(plugin, "ItemsAdder")) hook(plugin, "ItemsAdder", ItemsAdderProvider::new, providers);
        if (isEnabled(plugin, "Oraxen")) hook(plugin, "Oraxen", OraxenProvider::new, providers);
        if (isEnabled(plugin, "Nexo")) hook(plugin, "Nexo", NexoProvider::new, providers);
        if (isEnabled(plugin, "CraftEngine")) hook(plugin, "CraftEngine", CraftEngineProvider::new, providers);
        return providers;
    }

    private static boolean isEnabled(Plugin plugin, String pluginName) {
        return plugin.getServer().getPluginManager().isPluginEnabled(pluginName);
    }

    private static void hook(Plugin plugin, String pluginName, Supplier<CustomContentProvider> factory,
                             List<CustomContentProvider> providers) {
        try {
            providers.add(factory.get());
            plugin.getLogger().info("Hooked into " + pluginName + ".");
        } catch (LinkageError | RuntimeException e) {
            // Missing classes mean the installed version does not have the API this hook was written against
            plugin.getLogger().warning("Could not hook into " + pluginName + " (unsupported version?): " + e);
        }
    }
}
