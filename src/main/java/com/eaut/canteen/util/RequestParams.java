package com.eaut.canteen.util;

/**
 * Parsing helpers for request parameters.
 *
 * Request parameters are attacker-controllable strings, not numbers: a bare
 * Integer.parseInt(req.getParameter(...)) turns any stale link, typo or hand-edited URL into an
 * uncaught NumberFormatException, which the container renders as a 500 error page. These helpers
 * make the failure a value the caller has to deal with (null / a default) instead.
 */
public final class RequestParams {

    private RequestParams() {
    }

    /** Null for a missing, blank or non-numeric value, so callers can treat all three the same. */
    public static Integer intOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The fallback for a missing, blank or non-numeric value. */
    public static int intOrDefault(String raw, int fallback) {
        Integer parsed = intOrNull(raw);
        return parsed == null ? fallback : parsed;
    }

    /**
     * Clamped into [min, max] after parsing, for values that index into something (page numbers,
     * page sizes) where an out-of-range number is as damaging as an unparseable one.
     */
    public static int intInRange(String raw, int fallback, int min, int max) {
        int value = intOrDefault(raw, fallback);
        return Math.min(max, Math.max(min, value));
    }

    /** Trimmed, or null when the value is absent or only whitespace. */
    public static String trimmedOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
