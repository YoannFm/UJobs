package me.usainsrht.ujobs.listeners.job_actions.timber;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.managers.JobManager;
import me.usainsrht.ujobs.models.BuiltInActions;
import me.usainsrht.ujobs.models.Job;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pays the break action for every log of a tree felled by UTimber.
 * <p>
 * UTimber is read through reflection: it is not published on any public Maven repository,
 * so UJobs cannot be compiled against it, and the hook only needs three things from its event
 * (the player, the detected tree and the tree's logs).
 */
public final class UTimberHook {

    private static final String PLUGIN_NAME = "UTimber";
    private static final String EVENT_CLASS = "me.usainsrht.utimber.event.TreeDestroyEvent";

    private final JobManager jobManager;
    private final Logger logger;
    private final Class<? extends Event> eventClass;
    private final Method getPlayer;
    private final Method getDetectedTree;
    private final Field logs;

    private UTimberHook(JobManager jobManager, Logger logger, Class<? extends Event> eventClass) throws ReflectiveOperationException {
        this.jobManager = jobManager;
        this.logger = logger;
        this.eventClass = eventClass;
        this.getPlayer = eventClass.getMethod("getPlayer");
        this.getDetectedTree = eventClass.getMethod("getDetectedTree");
        this.logs = getDetectedTree.getReturnType().getField("logs");
    }

    /** Listens to UTimber's tree event when UTimber is enabled; does nothing otherwise. */
    public static void registerIfPresent(UJobsPlugin plugin, JobManager jobManager) {
        Plugin timber = plugin.getServer().getPluginManager().getPlugin(PLUGIN_NAME);
        if (timber == null || !timber.isEnabled()) return;

        try {
            Class<?> loaded = Class.forName(EVENT_CLASS, true, timber.getClass().getClassLoader());
            UTimberHook hook = new UTimberHook(jobManager, plugin.getLogger(), loaded.asSubclass(Event.class));
            plugin.getServer().getPluginManager().registerEvent(hook.eventClass, new Listener() { },
                    EventPriority.MONITOR, (listener, event) -> hook.onTreeDestroy(event), plugin, true);
        } catch (ReflectiveOperationException | ClassCastException e) {
            plugin.getLogger().log(Level.WARNING, "UTimber is installed but its tree event could not be read, "
                    + "felled trees will not pay the break action (incompatible UTimber version?)", e);
        }
    }

    private void onTreeDestroy(Event event) {
        if (!eventClass.isInstance(event)) return;

        try {
            Player player = (Player) getPlayer.invoke(event);
            if (jobManager.shouldIgnore(player)) return;

            payBreak(player, countLogs(getDetectedTree.invoke(event)));
        } catch (ReflectiveOperationException e) {
            logger.log(Level.WARNING, "Could not read UTimber's tree event", e);
        }
    }

    private Map<Material, Integer> countLogs(Object tree) throws IllegalAccessException {
        Map<Material, Integer> amounts = new EnumMap<>(Material.class);
        if (logs.get(tree) instanceof Iterable<?> blocks) {
            for (Object block : blocks) {
                if (block instanceof Block logBlock) amounts.merge(logBlock.getType(), 1, Integer::sum);
            }
        }
        return amounts;
    }

    private void payBreak(Player player, Map<Material, Integer> amounts) {
        for (Job job : jobManager.getJobsWithAction(BuiltInActions.Material.BREAK)) {
            amounts.forEach((log, amount) ->
                    jobManager.processAction(player, BuiltInActions.Material.BREAK, log.name(), job, amount));
        }
    }
}
