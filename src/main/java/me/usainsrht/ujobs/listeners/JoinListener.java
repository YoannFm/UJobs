package me.usainsrht.ujobs.listeners;

import me.usainsrht.ujobs.UJobsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class JoinListener implements Listener {

    UJobsPlugin plugin;

    public JoinListener(UJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        plugin.getStorage().load(uuid).thenAccept(data -> {
            // The player may have left while the data was loading: nothing would ever remove it from the cache
            if (plugin.getServer().getPlayer(uuid) == null) {
                plugin.getStorage().removeFromCache(uuid);
            }
        });
    }

}
