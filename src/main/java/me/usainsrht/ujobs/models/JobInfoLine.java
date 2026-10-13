package me.usainsrht.ujobs.models;

import lombok.AccessLevel;
import lombok.Getter;
import me.usainsrht.ujobs.UJobsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Locale;

@Getter
public class JobInfoLine {

    final Action action;
    final String value;
    final Job.ActionReward reward;
    @Getter(AccessLevel.NONE)
    private final String lineTemplate;

    public JobInfoLine(Action action, String value, Job.ActionReward reward) {
        this.action = action;
        this.value = value;
        this.reward = reward;
        String line = UJobsPlugin.instance.getConfig().getString("actions." + action.getName().toLowerCase(Locale.ROOT));
        if (line == null) {
            UJobsPlugin.instance.getLogger().severe("Please add " + action.getName().toLowerCase(Locale.ROOT) + " to the config.yml under 'actions:'");
            line = "<gray>" + action.getName() + "</gray> <value>"; //default value to prevent npe
        }
        this.lineTemplate = line;
    }

    /**
     * Built on demand: the name of a custom item is only known once its plugin has loaded it,
     * which may happen after the jobs are loaded.
     */
    public Component getActionValue() {
        return UJobsPlugin.instance.getMiniMessage().deserialize(lineTemplate, Placeholder.component("value", describeValue()));
    }

    private Component describeValue() {
        UJobsPlugin plugin = UJobsPlugin.instance;

        Component custom = plugin.getActionValueResolver().describeCustomValue(value);
        if (custom != null) return custom;

        if (action instanceof EntityAction) {
            try {
                return Component.translatable(EntityType.valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return Component.text(value);
            }
        }
        if (action instanceof MaterialAction) {
            String numbersRemoved = value.replaceAll("\\d+", "");
            Material material = Material.matchMaterial(numbersRemoved);
            return material != null ? Component.translatable(material) : Component.text(value);
        }
        if (action instanceof SpecialAction) {
            String configValue = plugin.getConfig().getString("special_values." + value);
            return configValue != null ? plugin.getMiniMessage().deserialize(configValue) : Component.text(value);
        }
        return Component.text(value);
    }

}
