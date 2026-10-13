package me.usainsrht.ujobs.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.models.PlayerJobData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DatabaseStorage implements Storage {

    private static final String TABLE_NAME = "ujobs_player_jobs";

    private final UJobsPlugin plugin;
    private final HikariDataSource dataSource;
    private final JobStatsRepository repository;
    private final BackgroundWriter writer = new BackgroundWriter("UJobs-Database");
    private final Map<UUID, PlayerJobData> cache;
    private final PDCStorage pdcStorage;
    private final boolean pdcFallback;

    public DatabaseStorage(UJobsPlugin plugin, String storageType) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        this.pdcStorage = new PDCStorage(plugin);
        this.pdcFallback = plugin.getConfig().getBoolean("storage.pdc_fallback", true);
        this.dataSource = createDataSource(storageType);
        this.repository = new JobStatsRepository(dataSource, TABLE_NAME, dialectOf(storageType));
        initializeSchema();
    }

    @Override
    public void save(UUID uuid) {
        if (uuid == null) {
            return;
        }
        PlayerJobData playerJobData = cache.get(uuid);
        if (playerJobData == null) {
            return;
        }

        writer.execute(() -> persist(uuid, playerJobData));

        if (pdcFallback) {
            pdcStorage.set(uuid, playerJobData);
        }
    }

    public void saveNow(UUID uuid) {
        if (uuid == null) {
            return;
        }

        PlayerJobData playerJobData = cache.get(uuid);
        if (playerJobData == null) {
            return;
        }

        persist(uuid, playerJobData);

        if (pdcFallback) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                pdcStorage.saveNow(player, playerJobData);
            }
        }
    }

    /**
     * Saves every cached player. While the plugin runs this happens on the writer thread: the world save that
     * triggers it runs on the server thread, which would freeze for as long as the database needs.
     * When the plugin is disabling it happens right away, the data must be written before the connection closes.
     */
    @Override
    public void save() {
        Map<UUID, PlayerJobData> snapshot = new HashMap<>(cache);
        if (plugin.isEnabled()) {
            writer.execute(() -> snapshot.forEach(this::persist));
        } else {
            snapshot.forEach(this::persist);
        }

        if (pdcFallback) {
            pdcStorage.save();
        }
    }

    @Override
    public CompletableFuture<PlayerJobData> load(UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(new PlayerJobData(new UUID(0L, 0L)));
        }

        PlayerJobData cached = cache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        CompletableFuture<PlayerJobData> future = new CompletableFuture<>();

        CompletableFuture.supplyAsync(() -> {
            try {
                PlayerJobData playerJobData = repository.load(uuid);

                if (playerJobData.getJobStats().isEmpty() && pdcFallback) {
                    PlayerJobData fallback = loadFromPdc(uuid);
                    if (!fallback.getJobStats().isEmpty()) {
                        try {
                            repository.save(uuid, fallback);
                            playerJobData = fallback;
                        } catch (SQLException e) {
                            plugin.getLogger().log(Level.WARNING, "Failed to migrate PDC data to database for " + uuid, e);
                        }
                    }
                }

                cache.put(uuid, playerJobData);
                return playerJobData;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load player data from database for " + uuid, e);
                PlayerJobData fallback = pdcFallback ? loadFromPdc(uuid) : new PlayerJobData(uuid);
                cache.put(uuid, fallback);
                return fallback;
            }
        }).thenAccept(data -> completeLoadFuture(uuid, future, data));

        return future;
    }

    @Override
    public boolean isCached(UUID uuid) {
        return uuid != null && cache.containsKey(uuid);
    }

    @Override
    public @Nullable PlayerJobData getCached(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return cache.get(uuid);
    }

    @Override
    public void set(UUID uuid, PlayerJobData playerJobData) {
        if (uuid == null || playerJobData == null) {
            return;
        }
        cache.put(uuid, playerJobData);
        save(uuid);
    }

    @Override
    public void remove(UUID uuid) {
        if (uuid == null) {
            return;
        }
        removeFromCache(uuid);

        writer.execute(() -> {
            try {
                repository.delete(uuid);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to remove player data from database for " + uuid, e);
            }
        });

        if (pdcFallback) {
            pdcStorage.remove(uuid);
        }
    }

    @Override
    public void removeFromCache(UUID uuid) {
        if (uuid == null) {
            return;
        }
        cache.remove(uuid);
        if (pdcFallback) {
            pdcStorage.removeFromCache(uuid);
        }
    }

    @Override
    public Collection<PlayerJobData> getAllCachedData() {
        return cache.values();
    }

    @Override
    public void close() {
        writer.close();
        dataSource.close();
    }

    private HikariDataSource createDataSource(String storageType) {
        ConfigurationSection poolSection = plugin.getConfig().getConfigurationSection("storage.pool");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("UJobsPool");

        if (poolSection != null) {
            hikariConfig.setMaximumPoolSize(poolSection.getInt("maximum_pool_size", 10));
            hikariConfig.setMinimumIdle(poolSection.getInt("minimum_idle", 2));
            hikariConfig.setConnectionTimeout(poolSection.getLong("connection_timeout_ms", 10000L));
            hikariConfig.setMaxLifetime(poolSection.getLong("max_lifetime_ms", 1800000L));
            hikariConfig.setIdleTimeout(poolSection.getLong("idle_timeout_ms", 600000L));
            hikariConfig.setKeepaliveTime(poolSection.getLong("keepalive_time_ms", 0L));
        }

        switch (storageType.toLowerCase()) {
            case "mysql" -> configureMysql(hikariConfig);
            case "postgresql" -> configurePostgresql(hikariConfig);
            case "sqlite" -> configureSqlite(hikariConfig);
            default -> throw new IllegalArgumentException("Unsupported database type: " + storageType);
        }

        return new HikariDataSource(hikariConfig);
    }

    private void configureSqlite(HikariConfig hikariConfig) {
        String relativeFile = plugin.getConfig().getString("storage.sqlite.file", "storage/ujobs.db");
        File dbFile = new File(plugin.getDataFolder(), relativeFile);
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Failed to create sqlite folder: " + parent.getAbsolutePath());
        }

        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
    }

    private void configureMysql(HikariConfig hikariConfig) {
        ConfigurationSection mysql = plugin.getConfig().getConfigurationSection("storage.mysql");
        if (mysql == null) {
            throw new IllegalStateException("Missing storage.mysql configuration section");
        }

        String host = mysql.getString("host", "localhost");
        int port = mysql.getInt("port", 3306);
        String database = mysql.getString("database", "ujobs");
        boolean ssl = mysql.getBoolean("ssl", false);

        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + ssl + "&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                // one round trip per save instead of one per job, the difference is what makes a save fast over a network
                + "&rewriteBatchedStatements=true");
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setUsername(mysql.getString("username", "root"));
        hikariConfig.setPassword(mysql.getString("password", ""));
    }

    private void configurePostgresql(HikariConfig hikariConfig) {
        ConfigurationSection postgresql = plugin.getConfig().getConfigurationSection("storage.postgresql");
        if (postgresql == null) {
            throw new IllegalStateException("Missing storage.postgresql configuration section");
        }

        String host = postgresql.getString("host", "localhost");
        int port = postgresql.getInt("port", 5432);
        String database = postgresql.getString("database", "ujobs");
        boolean ssl = postgresql.getBoolean("ssl", false);
        String sslMode = ssl ? "require" : "disable";

        hikariConfig.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=" + sslMode);
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setUsername(postgresql.getString("username", "postgres"));
        hikariConfig.setPassword(postgresql.getString("password", ""));
    }

    private void initializeSchema() {
        try {
            repository.createSchema();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }

    private static JobStatsRepository.Dialect dialectOf(String storageType) {
        return switch (storageType.toLowerCase()) {
            case "mysql" -> JobStatsRepository.Dialect.MYSQL;
            case "postgresql" -> JobStatsRepository.Dialect.POSTGRESQL;
            case "sqlite" -> JobStatsRepository.Dialect.SQLITE;
            default -> throw new IllegalArgumentException("Unsupported database type: " + storageType);
        };
    }

    private void persist(UUID uuid, PlayerJobData playerJobData) {
        try {
            repository.save(uuid, playerJobData);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save player data to database for " + uuid, e);
        }
    }

    private PlayerJobData loadFromPdc(UUID uuid) {
        try {
            return pdcStorage.load(uuid).exceptionally(throwable -> new PlayerJobData(uuid)).join();
        } catch (Exception ignored) {
            return new PlayerJobData(uuid);
        }
    }

    private void completeLoadFuture(UUID uuid, CompletableFuture<PlayerJobData> future, PlayerJobData data) {
        if (uuid == null) {
            future.complete(data);
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            plugin.getMorePaperLib().scheduling().entitySpecificScheduler(player).run(
                    () -> future.complete(data),
                    () -> future.complete(data)
            );
            return;
        }
        future.complete(data);
    }
}
