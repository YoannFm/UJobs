package me.usainsrht.ujobs.placeholders;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;

/**
 * A parsed placeholder: {@code <type>_<field>_<job>} or, for the leaderboard, {@code leaderboard_<field>_<job>_<position>}.
 * Job ids may contain underscores, so the job is everything between the field and the optional position.
 */
record PlaceholderParams(String type, String field, String jobId, int position) {

    static final int NO_POSITION = -1;

    private static final String LEADERBOARD = "leaderboard";
    private static final String SEPARATOR = "_";

    /** Returns null when the placeholder is malformed. */
    static @Nullable PlaceholderParams parse(String params) {
        String[] parts = params.split(SEPARATOR);
        if (parts.length < 3) return null;

        String type = parts[0].toLowerCase(Locale.ROOT);
        String field = parts[1].toLowerCase(Locale.ROOT);

        if (!type.equals(LEADERBOARD)) {
            return new PlaceholderParams(type, field, join(parts, 2, parts.length), NO_POSITION);
        }

        if (parts.length < 4) return null;
        try {
            int position = Integer.parseInt(parts[parts.length - 1]);
            return new PlaceholderParams(type, field, join(parts, 2, parts.length - 1), position);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String join(String[] parts, int from, int to) {
        return String.join(SEPARATOR, Arrays.copyOfRange(parts, from, to));
    }
}
