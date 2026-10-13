package me.usainsrht.ujobs.gui;

import me.usainsrht.ujobs.UJobsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Items shared by the job menus, built from config.yml. Every material may be vanilla or a custom item (prefix:id). */
final class GuiItems {

    private static final String LEFT = "gui.navigation.left";
    private static final String RIGHT = "gui.navigation.right";
    private static final Material DEFAULT_NAVIGATION_MATERIAL = Material.ARROW;

    private GuiItems() {
    }

    /** Fills the whole menu with the item found at {@code configPath}, if there is one. */
    static void fillBlank(UJobsPlugin plugin, Inventory inventory, String configPath) {
        String value = plugin.getConfig().getString(configPath, null);
        if (value == null || value.isEmpty() || value.equalsIgnoreCase("air")) return;

        ItemStack blankItem = plugin.getActionValueResolver().createItem(value);
        if (blankItem == null) {
            plugin.getLogger().warning("Invalid item in config.yml at " + configPath + ": " + value);
            return;
        }

        blankItem.editMeta(meta -> meta.setHideTooltip(true));
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, blankItem);
        }
    }

    /**
     * Places the items of the {@code decorations} list of a menu section: each one has a material,
     * the slots it goes in, and optionally a name and a lore.
     */
    static void addDecorations(UJobsPlugin plugin, Inventory inventory, String menuPath) {
        String path = menuPath + ".decorations";
        for (Map<?, ?> decoration : plugin.getConfig().getMapList(path)) {
            Object material = decoration.get("material");
            ItemStack item = material == null ? null : plugin.getActionValueResolver().createItem(material.toString());
            if (item == null) {
                plugin.getLogger().warning("Invalid item in config.yml at " + path + ": " + material);
                continue;
            }

            Object name = decoration.get("name");
            Object lore = decoration.get("lore");
            applyText(plugin, item, name == null ? null : name.toString(), lore instanceof List<?> lines ? lines : null);

            Object slots = decoration.get("slots");
            for (int slot : MenuLayout.parseSlots(plugin, path + ".slots", slots instanceof List<?> list ? list : null, inventory.getSize())) {
                inventory.setItem(slot, item);
            }
        }
    }

    static void addNavigationButtons(UJobsPlugin plugin, Inventory inventory) {
        inventory.setItem(leftButtonSlot(plugin, inventory.getSize()), navigationButton(plugin, LEFT));
        inventory.setItem(rightButtonSlot(plugin, inventory.getSize()), navigationButton(plugin, RIGHT));
    }

    /** Slot of the left button: {@code gui.navigation.left.slot} when valid, the bottom left corner otherwise. */
    static int leftButtonSlot(UJobsPlugin plugin, int inventorySize) {
        return buttonSlot(plugin, LEFT, inventorySize, GuiLayout.leftButtonSlot(inventorySize));
    }

    /** Slot of the right button: {@code gui.navigation.right.slot} when valid, the bottom right corner otherwise. */
    static int rightButtonSlot(UJobsPlugin plugin, int inventorySize) {
        return buttonSlot(plugin, RIGHT, inventorySize, GuiLayout.rightButtonSlot(inventorySize));
    }

    private static int buttonSlot(UJobsPlugin plugin, String configPath, int inventorySize, int defaultSlot) {
        String path = configPath + ".slot";
        if (!plugin.getConfig().isSet(path)) return defaultSlot;

        int slot = plugin.getConfig().getInt(path, -1);
        if (slot < 0 || slot >= inventorySize) {
            plugin.getLogger().warning("Invalid slot in config.yml at " + path + " (menus have " + inventorySize + " slots): " + plugin.getConfig().get(path));
            return defaultSlot;
        }
        return slot;
    }

    private static ItemStack navigationButton(UJobsPlugin plugin, String configPath) {
        String value = plugin.getConfig().getString(configPath + ".material", DEFAULT_NAVIGATION_MATERIAL.name());
        ItemStack button = plugin.getActionValueResolver().createItem(value);
        if (button == null) {
            plugin.getLogger().warning("Invalid item in config.yml at " + configPath + ".material: " + value);
            button = new ItemStack(DEFAULT_NAVIGATION_MATERIAL);
        }

        List<?> lore = plugin.getConfig().isList(configPath + ".lore") ? plugin.getConfig().getList(configPath + ".lore") : null;
        applyText(plugin, button, plugin.getConfig().getString(configPath + ".name"), lore);
        return button;
    }

    /** Sets the name and lore (MiniMessage, no italics by default) of a menu item, and hides its attributes. */
    private static void applyText(UJobsPlugin plugin, ItemStack item, String name, List<?> lore) {
        item.editMeta(meta -> {
            if (name != null) {
                meta.displayName(plain(plugin, name));
            }
            if (lore != null) {
                List<Component> lines = new ArrayList<>();
                lore.forEach(line -> lines.add(plain(plugin, String.valueOf(line))));
                meta.lore(lines);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
    }

    private static Component plain(UJobsPlugin plugin, String text) {
        return plugin.getMiniMessage().deserialize(text).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
