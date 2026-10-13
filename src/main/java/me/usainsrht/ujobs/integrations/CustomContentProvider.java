package me.usainsrht.ujobs.integrations;

import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A plugin adding custom content (items, blocks, furniture, mobs) that jobs can reward.
 * <p>
 * Custom content is written in jobs.yml as {@code <prefix>:<id>}, for example {@code itemsadder:my_pack:ruby_ore}.
 */
public interface CustomContentProvider {

    /** Prefix identifying this provider's content in jobs.yml. */
    String getPrefix();

    /** Id of the custom item the stack is, or null when it is not a custom item of this provider. */
    @Nullable String getItemId(ItemStack item);

    /** Builds the custom item with the given id, or null when it is unknown (or not loaded yet). */
    @Nullable ItemStack createItem(String id);

    /** Id of the custom mob the entity is, or null when it is not a custom mob of this provider. */
    default @Nullable String getEntityId(Entity entity) {
        return null;
    }

    /**
     * Registers the listeners rewarding the custom blocks and furniture players place and break.
     * Vanilla block events cannot be used for them: providers may already have removed the block
     * when those events reach the monitor priority, and furniture has no vanilla event at all.
     */
    default void registerListeners(Plugin plugin, ActionReporter reporter) {
    }

    /** The jobs.yml value of the custom content with the given id. */
    default String toValue(String id) {
        return getPrefix() + ":" + id.toLowerCase(Locale.ROOT);
    }
}
