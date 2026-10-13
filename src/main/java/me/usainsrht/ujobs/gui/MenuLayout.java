package me.usainsrht.ujobs.gui;

import me.usainsrht.ujobs.UJobsPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Size of a menu and slots of its jobs. Computed from the job count (see {@link GuiLayout}),
 * unless the menu section of config.yml sets {@code rows} and {@code job_slots}.
 */
final class MenuLayout {

    private static final int COLUMNS = 9;
    private static final int MIN_ROWS = 1;
    private static final int MAX_ROWS = 6;

    private final int size;
    private final List<Integer> jobSlots; // null: default placement

    private MenuLayout(int size, List<Integer> jobSlots) {
        this.size = size;
        this.jobSlots = jobSlots;
    }

    /** Reads the layout of the menu whose config section is {@code menuPath} (for example "gui"). */
    static MenuLayout load(UJobsPlugin plugin, String menuPath, int jobCount) {
        int rows = rows(plugin, menuPath + ".rows", GuiLayout.rows(jobCount));
        int size = rows * COLUMNS;

        String slotsPath = menuPath + ".job_slots";
        if (!plugin.getConfig().isSet(slotsPath)) return new MenuLayout(size, null);

        List<Integer> jobSlots = parseSlots(plugin, slotsPath, plugin.getConfig().getList(slotsPath), size);
        if (jobCount > jobSlots.size()) {
            plugin.getLogger().warning("config.yml at " + slotsPath + " has " + jobSlots.size() + " valid slots for " + jobCount + " jobs, the others are not shown in this menu.");
        }
        return new MenuLayout(size, jobSlots);
    }

    private static int rows(UJobsPlugin plugin, String path, int defaultRows) {
        if (!plugin.getConfig().isSet(path)) return defaultRows;

        int rows = plugin.getConfig().getInt(path, -1);
        if (rows < MIN_ROWS || rows > MAX_ROWS) {
            plugin.getLogger().warning("Invalid rows in config.yml at " + path + " (expected " + MIN_ROWS + " to " + MAX_ROWS + "): " + plugin.getConfig().get(path));
            return defaultRows;
        }
        return rows;
    }

    /** Keeps the entries of {@code raw} that are slots of a menu of {@code size} slots, and warns about the others. */
    static List<Integer> parseSlots(UJobsPlugin plugin, String path, List<?> raw, int size) {
        List<Integer> slots = new ArrayList<>();
        if (raw == null) return slots;

        for (Object entry : raw) {
            if (entry instanceof Integer slot && slot >= 0 && slot < size) {
                slots.add(slot);
            } else {
                plugin.getLogger().warning("Invalid slot in config.yml at " + path + " (menus have " + size + " slots): " + entry);
            }
        }
        return slots;
    }

    int size() {
        return size;
    }

    /** Slot of the job at the given 1-based position, or -1 when the menu has no room for it. */
    int jobSlot(int position) {
        if (jobSlots == null) return GuiLayout.jobSlot(position, size);
        return position <= jobSlots.size() ? jobSlots.get(position - 1) : -1;
    }
}
