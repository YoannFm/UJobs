package me.usainsrht.ujobs.gui;

import lombok.Getter;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerJobData;
import me.usainsrht.ujobs.utils.ProgressBarUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Getter
public class MainJobGUI implements JobGUI {

    private final UJobsPlugin plugin;
    private final MenuLayout layout;
    public Inventory inventory;
    public UUID uuid;

    public static final NamespacedKey jobKey = new NamespacedKey("ujobs", "job_id");

    public MainJobGUI(UJobsPlugin plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;

        this.layout = MenuLayout.load(plugin, "gui", plugin.getJobManager().getJobs().size());
        Component title = plugin.getMiniMessage().deserialize(plugin.getConfig().getString("gui.title"));
        this.inventory = Bukkit.createInventory(this, layout.size(), title);

        GuiItems.fillBlank(plugin, inventory, "gui.blank_material");
        GuiItems.addDecorations(plugin, inventory, "gui");

        if (uuid == null) {
            plugin.getLogger().warning("MainJobGUI was opened with a null UUID.");
            return;
        }

        PlayerJobData playerJobData;
        if (plugin.getStorage().isCached(uuid)) {
            playerJobData = plugin.getStorage().getCached(uuid);
            if (playerJobData != null) {
                fill(plugin, playerJobData);
            }
        } else {
            plugin.getStorage().load(uuid).thenAccept(data -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    plugin.getMorePaperLib().scheduling().entitySpecificScheduler(player).run(
                            () -> fill(plugin, data),
                            () -> {
                            }
                    );
                }
            });
        }

    }

    public void fill(UJobsPlugin plugin, PlayerJobData playerJobData) {
        int i = 1;
        for (Job job : plugin.getJobManager().getJobs().values()) {
            int slot = layout.jobSlot(i);
            if (slot == -1) break; // the menu is full, JobManager warned about it when loading the jobs

            String jobId = job.getId();
            PlayerJobData.JobStats jobStats = playerJobData.getJobStats(jobId);

            double exp = jobStats.getExp();
            int level = jobStats.getLevel();
            double nextExp = job.calculateExpForLevel(level);
            double totalMoney = jobStats.getTotalMoney();
            String progress = ProgressBarUtil.getProgressBar(exp, nextExp, 25, "", "|", "<color:dark_gray>", "|");
            TextColor primaryColor = job.getFirstColor();
            TextColor secondaryColor = job.getLastColor();

            Set<TagResolver> placeholderSet = new HashSet<>();
            placeholderSet.add(Placeholder.component("job", job.getName()));
            placeholderSet.add(Formatter.number("level", level));
            placeholderSet.add(Formatter.number("next_level", level+1));
            placeholderSet.add(Formatter.number("exp", exp));
            placeholderSet.add(Formatter.number("next_exp", nextExp));
            placeholderSet.add(Formatter.number("total_money", totalMoney));
            placeholderSet.add(Placeholder.parsed("progress", progress));
            placeholderSet.add(Placeholder.parsed("money_symbol", plugin.getConfig().getString("symbols.money", "$")));
            placeholderSet.add(Placeholder.parsed("exp_symbol", plugin.getConfig().getString("symbols.exp", "xp")));
            placeholderSet.add(Placeholder.styling("primary", primaryColor));
            placeholderSet.add(Placeholder.styling("secondary", secondaryColor));
            int position = plugin.getLeaderboardManager().getPosition(playerJobData.getUuid(), job);
            String positionText = position == -1 ? plugin.getConfig().getString("leaderboard.calculate_top", "100")+"+" : String.valueOf(position+1);
            placeholderSet.add(Placeholder.unparsed("position", positionText));
            TagResolver[] placeholders = placeholderSet.toArray(new TagResolver[]{});

            ItemStack item = plugin.getActionValueResolver().createIcon(job.getIcon());
            item.editMeta(meta -> {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                Component name = plugin.getMiniMessage().deserialize(plugin.getConfig().getString("gui.jobitem.name"), placeholders)
                        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                meta.displayName(name);

                List<Component> lore = new ArrayList<>();
                plugin.getConfig().getStringList("gui.jobitem.lore").forEach(line -> {
                    Component component = plugin.getMiniMessage().deserialize(line, placeholders)
                            .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                    lore.add(component);
                });
                meta.lore(lore);

                meta.getPersistentDataContainer().set(jobKey, PersistentDataType.STRING, jobId);
            });

            inventory.setItem(slot, item);

            i++;
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack itemStack = e.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR) return;
        if (itemStack.getItemMeta() != null && itemStack.getItemMeta().getPersistentDataContainer().has(jobKey)) {
            UUID viewerUuid = uuid != null ? uuid : player.getUniqueId();
            if (e.getClick().isLeftClick()) {
                new LeaderboardGUI(plugin, viewerUuid).open(player);
            } else if (e.getClick().isRightClick()) {
                new JobInfoGUI(plugin).open(player);
            }
        }
    }

    @Override
    public void onDrag(InventoryDragEvent e) {
        e.setCancelled(true);
    }

    @Override
    public void onOpen(InventoryOpenEvent e) {

    }

    @Override
    public void onClose(InventoryCloseEvent e) {

    }
}
