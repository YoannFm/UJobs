package me.usainsrht.ujobs.managers;

import lombok.Getter;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.gui.GuiLayout;
import me.usainsrht.ujobs.integrations.ActionValueResolver;
import me.usainsrht.ujobs.models.*;
import me.usainsrht.ujobs.storage.JobIdValidator;
import me.usainsrht.ujobs.utils.JobExpUtils;
import me.usainsrht.ujobs.yaml.YamlMessage;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

@Getter
public class JobManager {
    private static final String DEFAULT_LEVEL_EQUATION = "<next_level>*<next_level>*5";

    private final UJobsPlugin plugin;
    private final ActionValueResolver actionValueResolver;
    // Replaced as a whole by loadJobs(): listeners on other threads (Folia) read them while a reload runs
    private volatile Map<String, Job> jobs = Map.of();
    private volatile Map<Action, Set<String>> actionJobMap = Map.of();

    public JobManager(UJobsPlugin plugin, ActionValueResolver actionValueResolver) {
        this.plugin = plugin;
        this.actionValueResolver = actionValueResolver;
    }

    public void loadJobs() {
        ConfigurationSection jobsSection = plugin.getConfigManager().getJobsConfig()
                .getConfigurationSection("jobs");

        if (jobsSection == null) {
            plugin.getLogger().warning("No jobs section found in config!");
            return;
        }

        Map<String, Job> newJobs = new LinkedHashMap<>();
        Map<Action, Set<String>> newActionJobMap = new HashMap<>();

        for (String jobId : jobsSection.getKeys(false)) {
            try {
                Job job = loadJob(jobId, jobsSection.getConfigurationSection(jobId), newActionJobMap);
                if (job != null) {
                    newJobs.put(jobId, job);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load job: " + jobId + " - " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Jobs removed from the config disappear; kept ones are updated in place
        // because other components (leaderboard, GUIs) hold references to the Job instances.
        Map<String, Job> loadedJobs = new LinkedHashMap<>();
        for (Job newJob : newJobs.values()) {
            Job existingJob = jobs.get(newJob.getId());
            if (existingJob != null) {
                existingJob.update(newJob);
                loadedJobs.put(existingJob.getId(), existingJob);
            } else {
                loadedJobs.put(newJob.getId(), newJob);
            }
        }
        this.jobs = Collections.unmodifiableMap(loadedJobs);
        this.actionJobMap = newActionJobMap;

        if (jobs.size() > GuiLayout.MAX_JOBS) {
            plugin.getLogger().warning("The menus only have room for " + GuiLayout.MAX_JOBS + " jobs, the others are not shown in them.");
        }
        plugin.getLogger().info("Loaded " + jobs.size() + " jobs successfully!");
    }

    private Job loadJob(String jobId, ConfigurationSection jobSection, Map<Action, Set<String>> jobsByAction) {
        if (jobSection == null) return null;

        // Load basic job info
        String nameText = jobSection.getString("name", jobId);
        Component name = plugin.getMiniMessage().deserialize(nameText).compact();

        warnIfProgressCannotBeSaved(jobId);

        String icon = loadIcon(jobId, jobSection.getString("icon", "STONE"));

        String levelEquation = jobSection.getString("level_equation", DEFAULT_LEVEL_EQUATION);

        YamlMessage levelUpMessage = new YamlMessage(jobSection.get("levelup_message"));

        List<String> levelUpCommands = jobSection.getStringList("levelup_commands");

        // Load boss bar config
        Job.BossBarConfig bossBarConfig = loadBossBarConfig(jobSection.getConfigurationSection("bossbar"));

        Job job = new Job(jobId, name, icon, levelEquation, levelUpMessage, levelUpCommands, bossBarConfig);

        // Load actions
        ConfigurationSection actionsSection = jobSection.getConfigurationSection("actions");
        if (actionsSection != null) {
            loadJobActions(job, actionsSection, jobsByAction);
        }

        return job;
    }

    /** The pdc storage silently skips a job whose id is not a valid key, its players would lose their progress. */
    private void warnIfProgressCannotBeSaved(String jobId) {
        boolean usesPdc = plugin.getConfig().getString("storage.type", "pdc").equalsIgnoreCase("pdc")
                || plugin.getConfig().getBoolean("storage.pdc_fallback", true);
        if (usesPdc && !JobIdValidator.isStorableInPdc(jobId)) {
            plugin.getLogger().warning("Job id '" + jobId + "' cannot be saved in the player data (pdc storage): "
                    + "use only lowercase letters, digits and _ - . / in job ids, or disable storage.pdc_fallback.");
        }
    }

    /**
     * Custom icons are kept as written: custom items may not be loaded yet,
     * so they are only built when a GUI needs them.
     */
    private String loadIcon(String jobId, String iconValue) {
        if (actionValueResolver.isCustomValue(iconValue)) return iconValue;
        try {
            return Material.valueOf(iconValue.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid icon material for job " + jobId + ": " + iconValue);
            return Material.STONE.name();
        }
    }

    private Job.BossBarConfig loadBossBarConfig(ConfigurationSection bossBarSection) {
        if (bossBarSection == null) {
            return new Job.BossBarConfig(
                    "<yellow><level> <gray>level <job> <gradient:#6D6666:#938B8B:#777676>+<gained_exp><symbol_exp> <exp>/<next_exp>",
                    "<gradient:#5DFF00:#E8FF00:{phase}>You have reached <bold><level></bold>. level in <job> skill.",
                    BossBar.Color.BLUE,
                    BossBar.Overlay.NOTCHED_10
            );
        }

        String titleTemplate = bossBarSection.getString("title",
                "<yellow><level> <gray>level <job> <gradient:#6D6666:#938B8B:#777676>+<gained_exp><symbol_exp> <exp>/<next_exp>");
        String levelUpTemplate = bossBarSection.getString("levelup",
                "<gradient:#5DFF00:#E8FF00:{phase}>You have reached <bold><level></bold>. level in <job> skill.");

        BossBar.Color color;
        try {
            color = BossBar.Color.valueOf(bossBarSection.getString("color", "BLUE").toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BossBar.Color.BLUE;
        }

        BossBar.Overlay overlay;
        try {
            overlay = BossBar.Overlay.valueOf(bossBarSection.getString("overlay", "NOTCHED_10").toUpperCase());
        } catch (IllegalArgumentException e) {
            overlay = BossBar.Overlay.NOTCHED_10;
        }

        return new Job.BossBarConfig(titleTemplate, levelUpTemplate, color, overlay);
    }

    private void loadJobActions(Job job, ConfigurationSection actionsSection, Map<Action, Set<String>> jobsByAction) {
        for (String actionName : actionsSection.getKeys(false)) {
            Action action = BuiltInActions.get(actionName);
            if (action == null) {
                plugin.getLogger().warning("Unknown action '" + actionName + "' in job " + job.getId() + ", skipping it.");
                continue;
            }
            ConfigurationSection actionSection = actionsSection.getConfigurationSection(actionName);
            if (actionSection != null) {
                for (String value : actionSection.getKeys(false)) {
                    ConfigurationSection rewardSection = actionSection.getConfigurationSection(value);
                    if (rewardSection != null) {
                        double exp = rewardSection.getDouble("exp", 0.0);
                        double money = rewardSection.getDouble("money", 0.0);
                        Job.ActionReward actionReward = new Job.ActionReward(exp, money);
                        job.addAction(action, value.toLowerCase(Locale.ROOT), actionReward);
                        JobInfoLine infoLine = new JobInfoLine(action, value, actionReward);
                        job.getInfoLines().add(infoLine);

                        jobsByAction.computeIfAbsent(action, k -> new HashSet<>()).add(job.getId());
                    }
                }
            }
        }
    }

    public Collection<Job> getJobsWithAction(Action action) {
        return actionJobMap.getOrDefault(action, Collections.emptySet())
                .stream()
                .map(jobs::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Rewards the action in every job that has it. */
    public void processAction(Player player, Action action, String value) {
        if (shouldIgnore(player)) return;
        for (Job job : getJobsWithAction(action)) {
            processAction(player, action, value, job, 1);
        }
    }

    public void processAction(Player player, Action action, String value, Job job, int amount) {
        if (player == null || action == null || job == null || value == null || value.isBlank() || amount <= 0) {
            return;
        }

        PlayerJobData playerJobData = plugin.getStorage().getCached(player.getUniqueId());
        if (playerJobData == null) return; //job data has to be loaded at this point

        Job.ActionReward reward = job.getActionReward(action, value.toLowerCase(Locale.ROOT));
        if (reward == null) return;

        JobExpUtils.processJobExp(player, job, reward, amount);

    }

    public boolean shouldIgnore(Player player) {
        if (!plugin.getConfig().isSet("ignored_gamemodes")) return false;
        List<String> ignoredGamemodes = plugin.getConfig().getStringList("ignored_gamemodes");
        return ignoredGamemodes.contains(player.getGameMode().toString());
    }

    public Job getWeeklyJob() {
        if (jobs.isEmpty()) {
            return null;
        }
        List<Job> jobList = new ArrayList<>(jobs.values());
        Calendar calendar = Calendar.getInstance();
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        int index = (weekOfYear - 1) % jobList.size();
        if (index < 0) {
            index += jobList.size();
        }
        return jobList.get(index);
    }
}