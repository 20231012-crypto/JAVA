package com.eaut.canteen.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * "Now" and "today" for a canteen in Hanoi, on a server that is not in Hanoi.
 *
 * <p>THE BUG THIS EXISTS TO FIX: every date figure in the reports was computed in whatever zone the
 * database happened to be in. OrderDAOImpl asked PostgreSQL for {@code CURRENT_DATE}; on Neon that
 * is UTC, and the Dockerfile sets no TZ so Tomcat is UTC too. The result was that "doanh thu hôm
 * nay" rolled over at 07:00 Vietnam time and every order placed between midnight and 07:00 was
 * counted against the previous day. It looked correct in local development — where both the JVM
 * and PostgreSQL run at +07:00 — which is exactly why it survived.
 *
 * <p>The fix is to stop asking the database what day it is. Reporting queries take explicit
 * from/to bounds computed here, so the boundary is the application's decision.
 *
 * <h2>Two zones, and why both are needed</h2>
 * <dl>
 *   <dt>{@link #zone} — the canteen's zone (setting {@code canteen.timezone})</dt>
 *   <dd>What a human means by "today" and by "we close at 18:00".</dd>
 *   <dt>{@link #storageZone} — the zone the TIMESTAMP columns are written in</dt>
 *   <dd>The columns are {@code TIMESTAMP} without a zone, so a value is only interpretable if you
 *       know who wrote it. Two writers do: PostgreSQL's {@code CURRENT_TIMESTAMP} default, and
 *       Java's {@code LocalDateTime.now()}. They agree only when the database session and the JVM
 *       share a zone — true in both environments here (local: both +07:00; Render+Neon: both UTC),
 *       which is why {@code ZoneId.systemDefault()} tracks it correctly in each.</dd>
 * </dl>
 *
 * <p>A query bound therefore has to be built in the canteen zone and then converted into the
 * storage zone before it is handed to JDBC — {@link #toStorage}. Skipping that conversion is the
 * whole bug, just relocated from SQL into Java.
 *
 * <p>Corollary worth knowing: do NOT "fix" this by setting {@code ENV TZ=Asia/Ho_Chi_Minh} in the
 * Dockerfile. That moves the JVM without moving Neon, breaking the invariant above and silently
 * corrupting {@code Order.getElapsedDisplay()}, which compares a stored timestamp against
 * {@code LocalDateTime.now()} and is correct today only because the two zones match.
 */
public final class AppClock {

    /** Used when the setting is missing or names a zone this JVM does not know. */
    private static final ZoneId FALLBACK_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private AppClock() {
    }

    /** The canteen's own zone — what "today" and "closing time" mean to a person standing there. */
    public static ZoneId zone(Connection conn) throws SQLException {
        String configured = Settings.get(conn, "canteen.timezone");
        if (configured == null || configured.isBlank()) {
            return FALLBACK_ZONE;
        }
        try {
            return ZoneId.of(configured.trim());
        } catch (DateTimeException e) {
            // A typo in the settings screen must not break every report on the site.
            return FALLBACK_ZONE;
        }
    }

    /** The zone the TIMESTAMP columns are written in — see the class comment. */
    public static ZoneId storageZone() {
        return ZoneId.systemDefault();
    }

    /**
     * Converts an instant expressed in the canteen's zone into the local-datetime the TIMESTAMP
     * columns store, so it can be compared against them. Every reporting bound goes through here.
     */
    public static LocalDateTime toStorage(ZonedDateTime canteenTime) {
        return canteenTime.withZoneSameInstant(storageZone()).toLocalDateTime();
    }

    /**
     * Converts a wall-clock time an admin typed — which they mean in canteen time — into the value
     * the TIMESTAMP columns store. Needed by anything that saves a scheduled moment: on Render the
     * JVM is UTC, so storing "07:00" verbatim would make it fire at 14:00 in Hanoi.
     */
    public static LocalDateTime fromCanteenInput(Connection conn, LocalDateTime typed) throws SQLException {
        return toStorage(typed.atZone(zone(conn)));
    }

    /** The inverse, for redrawing a stored moment in the form the admin typed it into. */
    public static LocalDateTime toCanteenDisplay(Connection conn, LocalDateTime stored) throws SQLException {
        return stored.atZone(storageZone()).withZoneSameInstant(zone(conn)).toLocalDateTime();
    }

    public static LocalDate today(Connection conn) throws SQLException {
        return LocalDate.now(zone(conn));
    }

    /** Wall-clock time at the canteen — this is what the opening-hours check compares against. */
    public static LocalTime timeOfDay(Connection conn) throws SQLException {
        return LocalTime.now(zone(conn));
    }

    // ---- Query bounds. All half-open [from, to): a row can never land in two adjacent periods,
    // and a day boundary needs no 23:59:59.999 fudge. All already converted to the storage zone.

    public static LocalDateTime startOfDay(Connection conn, LocalDate day) throws SQLException {
        return toStorage(day.atStartOfDay(zone(conn)));
    }

    /** Exclusive end of {@code day}. */
    public static LocalDateTime endOfDay(Connection conn, LocalDate day) throws SQLException {
        return toStorage(day.plusDays(1).atStartOfDay(zone(conn)));
    }

    public static LocalDateTime startOfToday(Connection conn) throws SQLException {
        return startOfDay(conn, today(conn));
    }

    /** Exclusive: the boundary that "today" ends at, i.e. tomorrow's midnight in the canteen. */
    public static LocalDateTime endOfToday(Connection conn) throws SQLException {
        return endOfDay(conn, today(conn));
    }

    /** Monday-based, the way a Vietnamese week is read. */
    public static LocalDateTime startOfThisWeek(Connection conn) throws SQLException {
        LocalDate today = today(conn);
        return startOfDay(conn, today.minusDays(today.getDayOfWeek().getValue() - 1L));
    }

    public static LocalDateTime startOfThisMonth(Connection conn) throws SQLException {
        return startOfDay(conn, today(conn).withDayOfMonth(1));
    }

    /**
     * The comparable previous window for a growth figure: the same length, ending exactly where
     * this one begins. Only the start is returned because the end is always {@code from}.
     */
    public static LocalDateTime previousWindowStart(LocalDateTime from, LocalDateTime to) {
        return from.minus(Duration.between(from, to));
    }
}
