package me.usainsrht.ujobs.integrations;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Translates game objects to the values written in jobs.yml (and back), whether they are vanilla
 * ({@code DIAMOND}) or provided by a custom content plugin ({@code oraxen:ruby}).
 */
public class ActionValueResolver {

    private static final char PREFIX_SEPARATOR = ':';

    private final List<CustomContentProvider> providers;

    public ActionValueResolver(List<CustomContentProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public String itemValue(ItemStack item) {
        for (CustomContentProvider provider : providers) {
            String id = provider.getItemId(item);
            if (id != null) return provider.toValue(id);
        }
        return item.getType().name();
    }

    public String entityValue(Entity entity) {
        for (CustomContentProvider provider : providers) {
            String id = provider.getEntityId(entity);
            if (id != null) return provider.toValue(id);
        }
        return entity.getType().name();
    }

    /** Whether the value designates custom content of one of the installed providers. */
    public boolean isCustomValue(String value) {
        return findProvider(value) != null;
    }

    /** Builds the item a value designates, vanilla or custom, or null when there is none. */
    public @Nullable ItemStack createItem(String value) {
        CustomContentProvider provider = findProvider(value);
        if (provider != null) return provider.createItem(idOf(value));

        Material material = Material.matchMaterial(value);
        return material != null && material.isItem() ? new ItemStack(material) : null;
    }

    /** Same as {@link #createItem(String)} but falls back to stone, for items that must always exist. */
    public ItemStack createIcon(String value) {
        ItemStack item = createItem(value);
        return item != null ? item : new ItemStack(Material.STONE);
    }

    /** Display name of the custom content a value designates, or null when the value is not custom. */
    public @Nullable Component describeCustomValue(String value) {
        CustomContentProvider provider = findProvider(value);
        if (provider == null) return null;

        ItemStack item = provider.createItem(idOf(value));
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.displayName();
        return Component.text(idOf(value));
    }

    public void registerListeners(Plugin plugin, ActionReporter reporter) {
        for (CustomContentProvider provider : providers) {
            try {
                provider.registerListeners(plugin, reporter);
            } catch (LinkageError | RuntimeException e) {
                // The installed version of the plugin lacks some events this hook listens to
                plugin.getLogger().warning("Could not listen to the " + provider.getPrefix() + " events (unsupported version?): " + e);
            }
        }
    }

    private @Nullable CustomContentProvider findProvider(String value) {
        int separator = value.indexOf(PREFIX_SEPARATOR);
        if (separator <= 0) return null;

        String prefix = value.substring(0, separator).toLowerCase(Locale.ROOT);
        for (CustomContentProvider provider : providers) {
            if (provider.getPrefix().equals(prefix)) return provider;
        }
        return null;
    }

    private static String idOf(String value) {
        return value.substring(value.indexOf(PREFIX_SEPARATOR) + 1);
    }
}
