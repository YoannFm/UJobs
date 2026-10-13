package me.usainsrht.ujobs.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fills in the placeholders of a level-up command and evaluates the math expressions written in it. */
public final class LevelUpCommand {

    private static final Pattern MATH_EXPRESSION = Pattern.compile("([0-9()+\\-*/.]+)");
    private static final String OPERATORS = "+-*/()";

    private LevelUpCommand() {
    }

    /** {@code <level>} and {@code <player>} are replaced, expressions such as {@code <level>*10} are evaluated. */
    public static String format(String command, int level, String playerName) {
        String formatted = command.replace("<level>", String.valueOf(level));

        List<String> expressions = new ArrayList<>();
        Matcher matcher = MATH_EXPRESSION.matcher(formatted);
        while (matcher.find()) {
            // A plain number is an argument of the command, not something to calculate
            if (containsOperator(matcher.group(1))) expressions.add(matcher.group(1));
        }
        for (String expression : expressions) {
            try {
                formatted = formatted.replace(expression, formatNumber(MathUtil.eval(expression)));
            } catch (RuntimeException e) {
                // Not a math expression (a hyphenated name for example), leave it as written
            }
        }

        return formatted.replace("<player>", playerName);
    }

    /** Whole numbers are written without decimals, commands rarely accept {@code 70.0} where {@code 70} is expected. */
    private static String formatNumber(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) return String.valueOf((long) value);
        return String.valueOf(value);
    }

    private static boolean containsOperator(String text) {
        for (char c : text.toCharArray()) {
            if (OPERATORS.indexOf(c) >= 0) return true;
        }
        return false;
    }
}
