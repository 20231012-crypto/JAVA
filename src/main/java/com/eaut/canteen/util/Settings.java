package com.eaut.canteen.util;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Business configuration, read from the app_settings table and editable at runtime from
 * /admin/settings.
 *
 * <p>This is deliberately a different thing from {@link AppConfig}. AppConfig holds what the
 * <em>deployment</em> owns — database credentials, the Google client id, the upload directory —
 * values that only change when someone redeploys, and some of which are secrets that have no
 * business being in a table an admin screen can edit. Settings holds what the <em>canteen</em>
 * owns: opening hours, the loyalty rate, discount percentages. A manager has to be able to change
 * those without a developer.
 *
 * <p>AppConfig stays the fallback for every key, so a setting that is missing from the table still
 * resolves to whatever app.properties or the environment says. That is what makes this migration
 * safe: the rows seeded by migration 013 carry exactly the values the Java defaults already used,
 * and a database that has not run that migration behaves as it did before.
 */
public final class Settings {

    /**
     * There is no connection pool in this app, so every page already pays for its own connection;
     * re-reading thirteen rows on every request on top of that is waste. A short TTL rather than
     * cache-until-written because a second Tomcat instance (or a direct SQL edit) would otherwise
     * never be noticed — thirty seconds bounds how long a stale rate can be charged.
     */
    private static final long TTL_MILLIS = 30_000L;

    private static final String SELECT_ALL = "SELECT setting_key, setting_value FROM app_settings";
    private static final String UPDATE_ONE =
            "UPDATE app_settings SET setting_value = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP "
            + "WHERE setting_key = ?";

    /**
     * Replaced wholesale on every reload rather than mutated, so a reader iterating it can never
     * see a half-refreshed map. Both fields are volatile because the reader and the refresher are
     * different request threads.
     */
    private static volatile Map<String, String> cache = Map.of();
    private static volatile long loadedAt = 0L;

    private Settings() {
    }

    /** DB value, else AppConfig, else null. */
    public static String get(Connection conn, String key) throws SQLException {
        String value = snapshot(conn).get(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return AppConfig.get(key);
    }

    public static String getString(Connection conn, String key, String fallback) throws SQLException {
        String value = get(conn, key);
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * A malformed value falls back rather than throwing: these rows are edited through a form, and
     * one bad entry must not take down checkout. The settings screen validates on the way in, so
     * this is the second line of defence, not the first.
     */
    public static int getInt(Connection conn, String key, int fallback) throws SQLException {
        String value = get(conn, key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static BigDecimal getDecimal(Connection conn, String key, BigDecimal fallback) throws SQLException {
        String value = get(conn, key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static boolean getBool(Connection conn, String key, boolean fallback) throws SQLException {
        String value = get(conn, key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        if (normalized.equalsIgnoreCase("true") || normalized.equals("1")) {
            return true;
        }
        if (normalized.equalsIgnoreCase("false") || normalized.equals("0")) {
            return false;
        }
        return fallback;
    }

    /** Accepts "HH:mm" and "HH:mm:ss" — the HTML time input submits the first form. */
    public static LocalTime getTime(Connection conn, String key, LocalTime fallback) throws SQLException {
        String value = get(conn, key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    /**
     * Writes one setting and drops the cache so the new value is live immediately for this
     * instance — the TTL only has to cover changes made somewhere else.
     *
     * @return true if the key existed and was updated. Unknown keys are refused rather than
     *         inserted: the seed defines the catalogue of settings, and silently creating a row
     *         from a typo'd form field would put a key in the table that nothing ever reads.
     */
    public static boolean set(Connection conn, String key, String value, int updatedBy) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_ONE)) {
            ps.setString(1, value);
            ps.setInt(2, updatedBy);
            ps.setString(3, key);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                invalidate();
            }
            return updated > 0;
        }
    }

    /**
     * Forces the next read to hit the database. Call after writing settings through anything other
     * than {@link #set} — a batch update in a transaction, for instance, where the values must not
     * become visible until the commit.
     */
    public static void invalidate() {
        loadedAt = 0L;
    }

    private static Map<String, String> snapshot(Connection conn) throws SQLException {
        long age = System.currentTimeMillis() - loadedAt;
        if (age < TTL_MILLIS) {
            return cache;
        }
        Map<String, String> fresh = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                fresh.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        }
        cache = Map.copyOf(fresh);
        loadedAt = System.currentTimeMillis();
        return cache;
    }
}
