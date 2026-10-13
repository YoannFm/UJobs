package me.usainsrht.ujobs.listeners.job_actions;

import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.raid.RaidSpawnWaveEvent;

import java.util.UUID;

public class RaidListener implements Listener {

    JobManager jobManager;

    public RaidListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    /** The event also fires when the raid is lost, with no winners: only a won raid pays "win". */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaidFinish(RaidFinishEvent e) {
        if (!jobManager.getActionJobMap().containsKey(BuiltInActions.Special.RAID)) return;

        for (Player player : e.getWinners()) {
            reward(player, "win");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaidWave(RaidSpawnWaveEvent e) {
        if (!jobManager.getActionJobMap().containsKey(BuiltInActions.Special.RAID)) return;

        for (UUID uuid : e.getRaid().getHeroes()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                reward(player, "wave");
            }
        }
    }

    private void reward(Player player, String value) {
        jobManager.getPlugin().getMorePaperLib().scheduling().entitySpecificScheduler(player).run(
                () -> {
                    if (jobManager.shouldIgnore(player)) return;
                    for (Job job : jobManager.getJobsWithAction(BuiltInActions.Special.RAID)) {
                        jobManager.processAction(player, BuiltInActions.Special.RAID, value, job, 1);
                    }
                },
                () -> {
                }
        );
    }

}
