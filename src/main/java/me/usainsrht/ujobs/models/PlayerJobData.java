package me.usainsrht.ujobs.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class PlayerJobData {
    private final UUID uuid;
    private final Map<String, JobStats> jobStats;

    public PlayerJobData(UUID playerId) {
        this.uuid = playerId;
        this.jobStats = new ConcurrentHashMap<>();
    }

    public JobStats getJobStats(String jobId) {
        return jobStats.computeIfAbsent(jobId, k -> new JobStats());
    }

    public void setJobStats(String jobId, JobStats stats) {
        jobStats.put(jobId, stats);
    }

    public boolean hasJobStats(String jobId) {
        return jobStats.containsKey(jobId);
    }

    @Getter
    @Setter
    public static class JobStats {
        private volatile int level;
        private volatile double exp;
        private volatile double totalMoney;

        public JobStats() {
            this(0, 0, 0.0);
        }

        public JobStats(int level, double exp, double totalMoney) {
            this.level = level;
            this.exp = exp;
            this.totalMoney = totalMoney;
        }
    }
}