package me.usainsrht.ujobs;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import me.usainsrht.ujobs.commands.MainCommand;
import me.usainsrht.ujobs.integrations.ActionValueResolver;
import me.usainsrht.ujobs.integrations.CustomContentProviders;
import me.usainsrht.ujobs.listeners.InventoryListener;
import me.usainsrht.ujobs.listeners.JobActionListenerRegistry;
import me.usainsrht.ujobs.listeners.JoinListener;
import me.usainsrht.ujobs.listeners.QuitListener;
import me.usainsrht.ujobs.listeners.SaveListener;
import me.usainsrht.ujobs.managers.*;
import me.usainsrht.ujobs.placeholders.JobPlaceholders;
import me.usainsrht.ujobs.storage.DatabaseStorage;
import me.usainsrht.ujobs.storage.PDCStorage;
import me.usainsrht.ujobs.storage.Storage;
import me.usainsrht.ujobs.yaml.YamlCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.morepaperlib.MorePaperLib;

import java.util.Locale;
import java.util.logging.Level;

@Getter
public final class UJobsPlugin extends JavaPlugin {

    public static volatile UJobsPlugin instance;
    private MiniMessage miniMessage;
    private Economy economy;
    private Storage storage;
    private MorePaperLib morePaperLib;
    private ActionValueResolver actionValueResolver;

    // Managers
    private ConfigManager configManager;
    private JobManager jobManager;
    private LeaderboardManager leaderboardManager;
    private JobGUIManager guiManager;
    private BossBarManager bossBarManager;
    private JobActionListenerRegistry actionListenerRegistry;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize Adventure API
        this.miniMessage = MiniMessage.miniMessage();
        this.morePaperLib = new MorePaperLib(this);

        saveDefaultConfig();

        setupEconomy();

        initializeStorage();

        initializeManagers();

        registerEventsAndCommands();

        registerPlaceholders();

        int pluginId = 30282;
        Metrics metrics = new Metrics(this, pluginId);

        getLogger().info("UJobs has been enabled successfully!");
    }

    @Override
    public void onDisable() {

        // Save all data
        if (storage != null) {
            if (storage instanceof PDCStorage pdcStorage) {
                pdcStorage.saveNowAll();
            }
            storage.save();
            storage.close();
        }

        if (leaderboardManager != null) {
            leaderboardManager.save(); //saves to cache (async task submitted)
            leaderboardManager.shutdown(); //waits for async tasks to finish and shuts down
        }
        if (configManager != null) configManager.saveLeaderboard(); //writes it

        // Cancel all boss bars
        if (bossBarManager != null) {
            bossBarManager.removeAllBossBars();
        }

        getLogger().info("UJobs has been disabled!");
    }

    /**
     * Reloads config.yml and jobs.yml, then propagates the job changes
     * to the leaderboards and the job action listeners.
     */
    public void reload() {
        configManager.reload();
        jobManager.loadJobs();
        leaderboardManager.syncJobs();
        actionListenerRegistry.registerUsedActions();
    }

    private void initializeManagers() {
        try {
            this.actionValueResolver = new ActionValueResolver(CustomContentProviders.detect(this));
            this.configManager = new ConfigManager(this);
            this.jobManager = new JobManager(this, actionValueResolver);
            this.leaderboardManager = new LeaderboardManager(this);
            this.bossBarManager = new BossBarManager(this);
            this.guiManager = new JobGUIManager(this);

            // Load configurations
            configManager.loadConfigs();
            jobManager.loadJobs();
            leaderboardManager.load(configManager.getLeaderboardConfig());
            leaderboardManager.validateAndFixLeaderboard();

            getLogger().info("All managers initialized successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize managers", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerEventsAndCommands() {
        // Register event listeners
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new QuitListener(this), this);
        getServer().getPluginManager().registerEvents(new SaveListener(this), this);

        // job listeners
        actionListenerRegistry = new JobActionListenerRegistry(this, jobManager);
        actionListenerRegistry.registerUsedActions();

        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            YamlCommand mainCommand = YamlCommand.builder()
                    .name(getConfig().getString("command.name", "ujobs"))
                    .description(getConfig().getString("command.description", "UJobs main command"))
                    .aliases(getConfig().getStringList("command.aliases"))
                    .permission(getConfig().getString("command.permission"))
                    .build();
            commands.register(getPluginMeta(), MainCommand.create(this, mainCommand), mainCommand.getDescription(), mainCommand.getAliases());
        });
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return;
            }
            economy = rsp.getProvider();
        }
    }

    private void initializeStorage() {
        String storageType = getConfig().getString("storage.type", "pdc").toLowerCase(Locale.ROOT);

        try {
            switch (storageType) {
                case "sqlite", "mysql", "postgresql" -> {
                    if (!hasDatabaseLibraries()) {
                        getLogger().severe("Database libraries are missing from classpath. Falling back to pdc storage.");
                        getLogger().severe("Expected classes: com.zaxxer.hikari.HikariConfig and selected JDBC driver.");
                        this.storage = new PDCStorage(this);
                        return;
                    }
                    this.storage = new DatabaseStorage(this, storageType);
                    getLogger().info("Using " + storageType + " storage backend.");
                }
                case "pdc" -> {
                    this.storage = new PDCStorage(this);
                    getLogger().info("Using pdc storage backend.");
                }
                default -> {
                    getLogger().warning("Unknown storage type '" + storageType + "'. Falling back to pdc.");
                    this.storage = new PDCStorage(this);
                }
            }
        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "Failed to initialize storage backend '" + storageType + "', falling back to pdc", e);
            this.storage = new PDCStorage(this);
        }
    }

    private boolean hasDatabaseLibraries() {
        return hasClass("com.zaxxer.hikari.HikariConfig")
                && (hasClass("org.sqlite.JDBC") || hasClass("com.mysql.cj.jdbc.Driver") || hasClass("org.postgresql.Driver"));
    }

    private boolean hasClass(String className) {
        try {
            Class.forName(className, false, getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new JobPlaceholders(this).register();
        }
    }

}
