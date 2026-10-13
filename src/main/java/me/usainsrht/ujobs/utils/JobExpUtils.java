package me.usainsrht.ujobs.utils;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerJobData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

public class JobExpUtils {

    /** Keeps a huge gain (an admin adding millions of exp) from running level-up commands thousands of times at once. */
    private static final int MAX_LEVELS_PER_GAIN = 1000;

    public static void processJobExp(Player player, Job job, Job.ActionReward reward, int amount) {
        UJobsPlugin plugin = UJobsPlugin.instance;
        if (plugin == null || player == null || job == null || reward == null || amount <= 0) {
            return;
        }

        PlayerJobData playerData = plugin.getStorage().getCached(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        PlayerJobData.JobStats stats = playerData.getJobStats(job.getId());

        // Calculate rewards
        double expGain = reward.getExp() * amount;
        double moneyGain = reward.getMoney() * amount;

        int previousLevel;
        int newLevel;
        double newExp;

        synchronized (stats) {
            previousLevel = stats.getLevel();
            newLevel = previousLevel;
            newExp = stats.getExp() + expGain;

            // A single gain can cover several levels; excess exp carries over each time and the rest
            // is kept for the next gain once the cap is reached.
            // A non-positive requirement (bad equation) would loop forever, so it stops leveling.
            long requiredExp = job.calculateExpForLevel(newLevel);
            while (requiredExp > 0 && newExp >= requiredExp && newLevel - previousLevel < MAX_LEVELS_PER_GAIN) {
                newExp -= requiredExp;
                newLevel++;
                requiredExp = job.calculateExpForLevel(newLevel);
            }

            stats.setLevel(newLevel);
            stats.setExp(newExp);
        }

        boolean leveledUp = newLevel > previousLevel;
        if (leveledUp) {
            handleLevelUp(player, job, previousLevel, newLevel);
        }

        // Add money
        if (moneyGain > 0 && plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(player, moneyGain);
            synchronized (stats) {
                stats.setTotalMoney(stats.getTotalMoney() + moneyGain);
            }
        }

        // Show boss bar
        if (plugin.getConfig().getBoolean("progress_bossbars", true)) {
            showJobBossBar(player, job, newLevel, newExp, expGain);
        }
    }

    /**
     * Level-up commands run once per level gained so per-level rewards are not lost;
     * the message, animation and leaderboard update only concern the final level.
     */
    public static void handleLevelUp(Player player, Job job, int previousLevel, int newLevel) {
        UJobsPlugin plugin = UJobsPlugin.instance;

        MessageUtil.send(player, job.getLevelUpMessage(),
                Placeholder.unparsed("player", player.getName()),
                Formatter.number("level", newLevel));

        for (int level = previousLevel + 1; level <= newLevel; level++) {
            dispatchLevelUpCommands(plugin, player, job, level);
        }

        // Show level up boss bar animation
        showLevelUpAnimation(player, job, newLevel);

        // Check leaderboard changes
        plugin.getLeaderboardManager().checkLeaderboardChange(player.getUniqueId(), job, newLevel);
    }

    private static void dispatchLevelUpCommands(UJobsPlugin plugin, Player player, Job job, int level) {
        for (String cmd : job.getLevelUpCommands()) {
            String command = LevelUpCommand.format(cmd, level, player.getName());
            plugin.getMorePaperLib().scheduling().globalRegionalScheduler().run(() ->
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command));
        }
    }

    public static void showJobBossBar(Player player, Job job, int level, double exp, double expGain) {
        UJobsPlugin plugin = UJobsPlugin.instance;
        long requiredExp = job.calculateExpForLevel(level);
        // A level costing nothing (or less) is complete, dividing by it would give NaN
        double progress = requiredExp > 0 ? Math.min(1.0, exp / requiredExp) : 1.0;

        // Create boss bar title with placeholders
        String titleTemplate = job.getBossBarConfig().getTitleTemplate();
        Component title = plugin.getMiniMessage().deserialize(titleTemplate,
                Formatter.number("level", level),
                Placeholder.component("job", job.getName()),
                Placeholder.unparsed("symbol_exp", plugin.getConfig().getString("symbols.exp", "xp")),
                Formatter.number("gained_exp", expGain),
                Formatter.number("exp", exp),
                Formatter.number("next_exp", requiredExp)
        );

        BossBar bossBar = BossBar.bossBar(
                title,
                (float) progress,
                job.getBossBarConfig().getColor(),
                job.getBossBarConfig().getOverlay()
        );

        plugin.getBossBarManager().showBossBar(player, job.getId(), bossBar, 3);
    }

    public static void showLevelUpAnimation(Player player, Job job, int newLevel) {
        UJobsPlugin plugin = UJobsPlugin.instance;
        String levelUpTemplate = job.getBossBarConfig().getLevelUpTemplate();

        AtomicInteger ticks = new AtomicInteger(0);

        plugin.getMorePaperLib().scheduling().entitySpecificScheduler(player).runAtFixedRate(task -> {
            if (ticks.getAndIncrement() >= 60) { // 3 seconds
                task.cancel();
                return;
            }

            // Create animated phase value
            double phase = Math.random();
            String animatedTemplate = levelUpTemplate.replace("{phase}", String.valueOf(phase % 1.0));

            Component title = plugin.getMiniMessage().deserialize(animatedTemplate,
                    Placeholder.component("job", job.getName()),
                    Formatter.number("level", newLevel)
            );

            BossBar levelUpBar = BossBar.bossBar(
                    title,
                    1.0f,
                    job.getBossBarConfig().getColor(),
                    job.getBossBarConfig().getOverlay()
            );

            plugin.getBossBarManager().showBossBar(player, job.getId() + "_levelup", levelUpBar, 1);

        }, () -> {}, 1L, 1L);
    }
}
