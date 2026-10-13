package me.usainsrht.ujobs.storage;

import me.usainsrht.ujobs.models.PlayerJobData;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** The SQL side of {@link DatabaseStorage}: one row per player and job. */
class JobStatsRepository {

    /** The databases differ in how they update a row that already exists. */
    enum Dialect {
        MYSQL("INSERT INTO %s (uuid, job_id, job_level, job_exp, total_money) VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE job_level = VALUES(job_level), job_exp = VALUES(job_exp), total_money = VALUES(total_money)"),
        POSTGRESQL("INSERT INTO %s (uuid, job_id, job_level, job_exp, total_money) VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT (uuid, job_id) DO UPDATE SET job_level = EXCLUDED.job_level, job_exp = EXCLUDED.job_exp, total_money = EXCLUDED.total_money"),
        SQLITE(POSTGRESQL.upsertSql);

        private final String upsertSql;

        Dialect(String upsertSql) {
            this.upsertSql = upsertSql;
        }

        String upsertSql(String table) {
            return upsertSql.formatted(table);
        }
    }

    private final DataSource dataSource;
    private final String table;
    private final String upsertSql;

    JobStatsRepository(DataSource dataSource, String table, Dialect dialect) {
        this.dataSource = dataSource;
        this.table = table;
        this.upsertSql = dialect.upsertSql(table);
    }

    void createSchema() throws SQLException {
        String createSql = "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "job_id VARCHAR(128) NOT NULL,"
                + "job_level INT NOT NULL,"
                + "job_exp DOUBLE PRECISION NOT NULL,"
                + "total_money DOUBLE PRECISION NOT NULL,"
                + "PRIMARY KEY (uuid, job_id)"
                + ")";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createSql);
        }
    }

    PlayerJobData load(UUID uuid) throws SQLException {
        PlayerJobData playerJobData = new PlayerJobData(uuid);

        String sql = "SELECT job_id, job_level, job_exp, total_money FROM " + table + " WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement select = connection.prepareStatement(sql)) {
            select.setString(1, uuid.toString());

            try (ResultSet resultSet = select.executeQuery()) {
                while (resultSet.next()) {
                    playerJobData.setJobStats(resultSet.getString("job_id"), new PlayerJobData.JobStats(
                            resultSet.getInt("job_level"),
                            resultSet.getDouble("job_exp"),
                            resultSet.getDouble("total_money")));
                }
            }
        }

        return playerJobData;
    }

    /**
     * Writes the player's rows in place instead of deleting and re-inserting them: two saves of the same player
     * running at once (Folia runs the world save and a quit on different threads) would otherwise fail with
     * duplicate keys on PostgreSQL and deadlocks on MySQL. Rows are written in job id order so that concurrent
     * saves lock them in the same order.
     */
    void save(UUID uuid, PlayerJobData playerJobData) throws SQLException {
        String deleteSql = "DELETE FROM " + table + " WHERE uuid = ? AND job_id = ?";

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement upsert = connection.prepareStatement(upsertSql);
                 PreparedStatement delete = connection.prepareStatement(deleteSql)) {
                for (Map.Entry<String, PlayerJobData.JobStats> entry : new TreeMap<>(playerJobData.getJobStats()).entrySet()) {
                    PlayerJobData.JobStats stats = entry.getValue();
                    if (stats.getLevel() == 0 && stats.getExp() == 0 && stats.getTotalMoney() == 0) {
                        // Nothing worth a row, and a job reset to zero must not keep its old row
                        delete.setString(1, uuid.toString());
                        delete.setString(2, entry.getKey());
                        delete.addBatch();
                        continue;
                    }
                    upsert.setString(1, uuid.toString());
                    upsert.setString(2, entry.getKey());
                    upsert.setInt(3, stats.getLevel());
                    upsert.setDouble(4, stats.getExp());
                    upsert.setDouble(5, stats.getTotalMoney());
                    upsert.addBatch();
                }
                upsert.executeBatch();
                delete.executeBatch();
            }

            connection.commit();
        }
    }

    void delete(UUID uuid) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table + " WHERE uuid = ?")) {
            delete.setString(1, uuid.toString());
            delete.executeUpdate();
        }
    }
}
