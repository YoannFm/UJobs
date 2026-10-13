package me.usainsrht.ujobs.models;

import lombok.Getter;
import me.usainsrht.ujobs.utils.MathUtil;
import me.usainsrht.ujobs.yaml.YamlMessage;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentIteratorType;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Job {
    private final String id;
    private Component name;
    private TextColor firstColor;
    private TextColor lastColor;
    private String icon;
    private String levelEquation;
    private YamlMessage levelUpMessage;
    private List<String> levelUpCommands;
    private BossBarConfig bossBarConfig;
    // Swapped as a whole by update(): listeners on other threads (Folia) read them while a reload runs.
    // A job being loaded is filled in place (addAction, getInfoLines().add) before anything else can see it.
    private volatile Map<Action, Map<String, ActionReward>> actions;
    private volatile List<JobInfoLine> infoLines;

    public Job(String id, Component name, String icon, String levelEquation,
               YamlMessage levelUpMessage, List<String> levelUpCommands, BossBarConfig bossBarConfig) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.levelEquation = levelEquation;
        this.levelUpMessage = levelUpMessage;
        this.levelUpCommands = levelUpCommands;
        this.bossBarConfig = bossBarConfig;
        this.actions = new HashMap<>();
        this.infoLines = new ArrayList<>();
    }

    public void update(Job otherJobInstance) {
        if (otherJobInstance == null || !this.id.equalsIgnoreCase(otherJobInstance.id)) return;

        this.name = otherJobInstance.name;
        // Colors are derived lazily from the name, force them to be recomputed
        this.firstColor = null;
        this.lastColor = null;
        this.icon = otherJobInstance.icon;
        this.levelEquation = otherJobInstance.levelEquation;
        this.levelUpCommands = otherJobInstance.levelUpCommands;
        this.levelUpMessage = otherJobInstance.levelUpMessage;
        this.bossBarConfig = otherJobInstance.bossBarConfig;
        this.actions = otherJobInstance.actions;
        this.infoLines = otherJobInstance.infoLines;
    }

    public void setColors() {
        TextColor firstColor = null;
        TextColor lastColor = null;

        // Iterate through all parts of the component tree
        for (Component part : name.iterable(ComponentIteratorType.DEPTH_FIRST)) {
            if (part instanceof TextComponent text && !text.content().isEmpty()) {
                TextColor currentColor = text.color();

                // The first non-empty text component we hit
                if (firstColor == null) {
                    firstColor = currentColor;
                }

                // Keep updating lastColor; the last one found will be the final one
                lastColor = currentColor;
            }
        }
        this.firstColor = firstColor;
        this.lastColor = lastColor;
    }

    public TextColor getFirstColor() {
        if (firstColor == null) setColors();
        return firstColor != null ? firstColor : TextColor.color(0xFFFFFF);
    }

    public TextColor getLastColor() {
        if (lastColor == null) setColors();
        return lastColor != null ? lastColor : TextColor.color(0xFFFFFF);
    }

    public void addAction(Action action, String value, ActionReward reward) {
        actions.computeIfAbsent(action, k -> new HashMap<>()).put(value, reward);
    }

    public ActionReward getActionReward(Action action, String value) {
        Map<String, ActionReward> actionMap = actions.get(action);
        return actionMap != null ? actionMap.get(value) : null;
    }

    public long calculateExpForLevel(int level) {
        String equation = levelEquation.replace("<level>", String.valueOf(level))
                .replace("<next_level>", String.valueOf(level + 1));

        // Simple equation evaluator for basic math
        try {
            return (long) evaluateExpression(equation);
        } catch (Exception e) {
            // Fallback to default calculation
            return (long) Math.pow(level + 1, 2) * 5;
        }
    }

    private double evaluateExpression(String expression) {
        try {
            return MathUtil.eval(expression);
        } catch (Exception e) {
            throw new RuntimeException("Invalid expression: " + expression, e);
        }
    }

    @Getter
    public static class BossBarConfig {
        private final String titleTemplate;
        private final String levelUpTemplate;
        private final BossBar.Color color;
        private final BossBar.Overlay overlay;

        public BossBarConfig(String titleTemplate, String levelUpTemplate,
                             BossBar.Color color, BossBar.Overlay overlay) {
            this.titleTemplate = titleTemplate;
            this.levelUpTemplate = levelUpTemplate;
            this.color = color;
            this.overlay = overlay;
        }

    }

    @Getter
    public static class ActionReward {
        private final double exp;
        private final double money;

        public ActionReward(double exp, double money) {
            this.exp = exp;
            this.money = money;
        }

    }
}