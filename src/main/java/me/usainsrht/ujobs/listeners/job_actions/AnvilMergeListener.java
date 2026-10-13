package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Rewards taking the result of an anvil when an item was merged into another one: the value is the item
 * in the right slot (the one that gets merged), for example {@code enchanted_book}. Renaming alone is not a merge.
 */
public class AnvilMergeListener implements Listener {

    /** Slot of the result in the anvil view; unlike getSlot(), the raw slot cannot be one of the player's own slots. */
    private static final int RESULT_RAW_SLOT = 2;

    JobManager jobManager;

    public AnvilMergeListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvilMerge(InventoryClickEvent e) {
        if (!(e.getInventory() instanceof AnvilInventory anvil)) return;
        if (e.getRawSlot() != RESULT_RAW_SLOT) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (jobManager.shouldIgnore(player)) return;

        ItemStack merged = anvil.getSecondItem();
        if (isEmpty(e.getCurrentItem()) || isEmpty(merged)) return;
        String value = jobManager.getActionValueResolver().itemValue(merged);

        // The click is only a request: the server refuses it when the player cannot pay the levels or has no room.
        // Once it is processed, taking the result has consumed the left item.
        jobManager.getPlugin().getMorePaperLib().scheduling().entitySpecificScheduler(player).run(
                () -> {
                    if (isEmpty(anvil.getFirstItem())) {
                        jobManager.processAction(player, BuiltInActions.Material.ANVIL_MERGE, value);
                    }
                },
                () -> {
                }
        );
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.isEmpty();
    }

}
