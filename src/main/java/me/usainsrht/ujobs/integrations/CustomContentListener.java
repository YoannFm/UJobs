package me.usainsrht.ujobs.integrations;

import me.usainsrht.ujobs.models.BuiltInActions;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/** Base of the listeners translating a provider's block and furniture events into job actions. */
public abstract class CustomContentListener implements Listener {

    private final ActionReporter reporter;
    private final CustomContentProvider provider;

    protected CustomContentListener(ActionReporter reporter, CustomContentProvider provider) {
        this.reporter = reporter;
        this.provider = provider;
    }

    protected void reportBreak(Player player, String id) {
        reporter.report(player, BuiltInActions.Material.BREAK, provider.toValue(id));
    }

    protected void reportPlace(Player player, String id) {
        reporter.report(player, BuiltInActions.Material.PLACE, provider.toValue(id));
    }
}
