package me.usainsrht.ujobs.integrations;

import me.usainsrht.ujobs.models.Action;
import org.bukkit.entity.Player;

/** Receives the actions players perform on custom content, so providers do not depend on how jobs reward them. */
@FunctionalInterface
public interface ActionReporter {

    void report(Player player, Action action, String value);
}
