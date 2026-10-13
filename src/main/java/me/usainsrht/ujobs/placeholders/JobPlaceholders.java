package me.usainsrht.ujobs.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.Job;
import me.usainsrht.ujobs.models.PlayerJobData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class JobPlaceholders extends PlaceholderExpansion {

    private static final String WEEKLY = "weekly";
    private static final List<String> WEEKLY_JOB_PREFIXES = List.of("weekly_job_", "weeklyjob_");

    private final UJobsPlugin plugin;

    public JobPlaceholders(UJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "ujobs";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    /*
    * ujobs_job_name_<job>                       - Job name                                             - Hunter
    * ujobs_job_displayname_<job>                - Job name with colors                                 - <red>Hunter
    * ujobs_job_legacydisplayname_<job>          - Job name with legacy colors                          - &cHunter
    *
    * ujobs_player_level_<job>                   - Player's level in the job                            - 5
    * ujobs_player_exp_<job>                     - Player's exp in the job                              - 1260.8
    * ujobs_player_position_<job>                - Player's position in the job leaderboard             - 12
    * ujobs_player_totalmoney_<job>              - Total amount of money player has gained in the job   - 14179.5
    *
    * ujobs_leaderboard_name_<job>_<position>    - Player name at position in job leaderboard           - UsainSrht
    * ujobs_leaderboard_level_<job>_<position>   - Player level at position in job leaderboard          - 31
    * */

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String weeklyField = weeklyJobField(params);
        if (weeklyField != null) {
            Job weeklyJob = plugin.getJobManager().getWeeklyJob();
            return weeklyJob == null ? "" : jobField(weeklyJob, weeklyField);
        }

        PlaceholderParams request = PlaceholderParams.parse(params);
        if (request == null) return null;

        Job job = request.jobId().equalsIgnoreCase(WEEKLY)
                ? plugin.getJobManager().getWeeklyJob()
                : plugin.getJobManager().getJobs().get(request.jobId());
        if (job == null) return null;

        return switch (request.type()) {
            case "job" -> jobField(job, request.field());
            case "player" -> playerField(player, job, request.field());
            case "leaderboard" -> leaderboardField(job, request.field(), request.position());
            default -> null;
        };
    }

    /** The field requested from the weekly job in {@code weekly_job_<field>} (or {@code weeklyjob_<field>}), or null. */
    private static String weeklyJobField(String params) {
        String lower = params.toLowerCase(Locale.ROOT);
        for (String prefix : WEEKLY_JOB_PREFIXES) {
            if (lower.startsWith(prefix)) return lower.substring(prefix.length());
        }
        return null;
    }

    private String jobField(Job job, String field) {
        return switch (field) {
            case "name" -> PlainTextComponentSerializer.plainText().serialize(job.getName());
            case "displayname" -> plugin.getMiniMessage().serialize(job.getName());
            case "legacydisplayname" -> LegacyComponentSerializer.legacySection().serialize(job.getName());
            default -> null;
        };
    }

    private String playerField(OfflinePlayer player, Job job, String field) {
        if (player == null || !player.isOnline()) return null;
        PlayerJobData playerJobData = plugin.getStorage().getCached(player.getUniqueId());
        if (playerJobData == null) return null;

        PlayerJobData.JobStats jobStats = playerJobData.getJobStats(job.getId());
        return switch (field) {
            case "level" -> String.valueOf(jobStats.getLevel());
            case "exp" -> new DecimalFormat().format(jobStats.getExp());
            case "position" -> String.valueOf(plugin.getLeaderboardManager().getPosition(player.getUniqueId(), job) + 1);
            case "totalmoney" -> String.valueOf(jobStats.getTotalMoney());
            default -> null;
        };
    }

    private String leaderboardField(Job job, String field, int position) {
        UUID target = plugin.getLeaderboardManager().getPlayerByPosition(position - 1, job);
        if (target == null) return "?";

        return switch (field) {
            case "name" -> {
                String name = plugin.getServer().getOfflinePlayer(target).getName();
                yield name != null ? name : "?";
            }
            case "level" -> String.valueOf(plugin.getLeaderboardManager().getLevel(target, job));
            default -> null;
        };
    }

}
