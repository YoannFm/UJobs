package me.usainsrht.ujobs.listeners;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.listeners.job_actions.*;
import me.usainsrht.ujobs.listeners.job_actions.timber.UTimberHook;
import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.Action;
import me.usainsrht.ujobs.models.BuiltInActions;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Registers the job action listeners only for the actions used by at least one job.
 * Can be called again after a reload: listeners of newly used actions get registered,
 * already registered ones are kept (each listener ignores actions no job uses anymore).
 */
public class JobActionListenerRegistry {

    private final UJobsPlugin plugin;
    private final JobManager jobManager;
    private final Map<Action, Supplier<List<Listener>>> listenerFactories = new LinkedHashMap<>();
    private final Set<Action> registeredActions = new HashSet<>();
    private boolean customContentRegistered;

    public JobActionListenerRegistry(UJobsPlugin plugin, JobManager jobManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;

        listenerFactories.put(BuiltInActions.Material.BREAK, this::createBreakListeners);
        listenerFactories.put(BuiltInActions.Material.PLACE, () -> List.of(new PlaceListener(jobManager)));
        listenerFactories.put(BuiltInActions.Entity.TAME, () -> List.of(new TameListener(jobManager)));
        listenerFactories.put(BuiltInActions.Entity.BREED, () -> List.of(new BreedListener(jobManager)));
        listenerFactories.put(BuiltInActions.Entity.KILL, () -> List.of(new KillListener(jobManager)));
        listenerFactories.put(BuiltInActions.Special.ENCHANT, () -> List.of(new EnchantListener(jobManager)));
        listenerFactories.put(BuiltInActions.Material.FISH, () -> List.of(new FishListener(jobManager)));
        listenerFactories.put(BuiltInActions.Special.RAID, () -> List.of(new RaidListener(jobManager)));
        listenerFactories.put(BuiltInActions.Special.GENERATE_LOOT, () -> List.of(new LootGenerateListener(jobManager)));
        listenerFactories.put(BuiltInActions.Material.TRADE, () -> List.of(new TradeListener(jobManager)));
        listenerFactories.put(BuiltInActions.Material.ANVIL_MERGE, () -> List.of(new AnvilMergeListener(jobManager)));
        listenerFactories.put(BuiltInActions.Material.CRAFT, () -> List.of(new CraftListener(jobManager)));
        listenerFactories.put(BuiltInActions.Material.HARVEST, () -> List.of(new HarvestListener(jobManager)));
        listenerFactories.put(BuiltInActions.Material.SMELT, () -> List.of(new SmeltListener(jobManager)));
    }

    public void registerUsedActions() {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        listenerFactories.forEach((action, factory) -> {
            if (registeredActions.contains(action) || !jobManager.getActionJobMap().containsKey(action)) return;
            factory.get().forEach(listener -> pluginManager.registerEvents(listener, plugin));
            registeredActions.add(action);
        });
        registerCustomContentListeners();
    }

    /** Custom blocks and furniture are placed and broken like vanilla blocks, so they follow the same actions. */
    private void registerCustomContentListeners() {
        boolean used = jobManager.getActionJobMap().containsKey(BuiltInActions.Material.BREAK)
                || jobManager.getActionJobMap().containsKey(BuiltInActions.Material.PLACE);
        if (customContentRegistered || !used) return;

        jobManager.getActionValueResolver().registerListeners(plugin, jobManager::processAction);
        customContentRegistered = true;
    }

    /** Also hooks UTimber's tree event, so trees felled by it pay the break action like single logs. */
    private List<Listener> createBreakListeners() {
        UTimberHook.registerIfPresent(plugin, jobManager);
        return List.of(new BreakListener(jobManager));
    }
}
