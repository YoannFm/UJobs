package me.usainsrht.ujobs.integrations.itemsadder;

import dev.lone.itemsadder.api.CustomEntity;
import dev.lone.itemsadder.api.CustomStack;
import me.usainsrht.ujobs.integrations.ActionReporter;
import me.usainsrht.ujobs.integrations.CustomContentProvider;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class ItemsAdderProvider implements CustomContentProvider {

    @Override
    public String getPrefix() {
        return "itemsadder";
    }

    @Override
    public @Nullable String getItemId(ItemStack item) {
        CustomStack stack = CustomStack.byItemStack(item);
        return stack == null ? null : stack.getNamespacedID();
    }

    @Override
    public @Nullable ItemStack createItem(String id) {
        CustomStack stack = CustomStack.getInstance(id);
        return stack == null ? null : stack.getItemStack();
    }

    @Override
    public @Nullable String getEntityId(Entity entity) {
        if (!CustomEntity.isCustomEntity(entity)) return null;
        CustomEntity customEntity = CustomEntity.byAlreadySpawned(entity);
        return customEntity == null ? null : customEntity.getNamespacedID();
    }

    @Override
    public void registerListeners(Plugin plugin, ActionReporter reporter) {
        plugin.getServer().getPluginManager().registerEvents(new ItemsAdderListener(reporter, this), plugin);
    }
}
