package com.eaut.canteen.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One staff shift: clocked in at {@code checkInAt}, clocked out at {@code checkOutAt} (null while
 * the shift is still open).
 *
 * Worked hours are derived here rather than stored, so a shift can never disagree with its own
 * timestamps. Getters exist because JSTL/EL cannot read record accessors.
 */
public record AttendanceRecord(
        int attendanceId,
        int userId,
        String fullName,
        String roleDisplayName,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt) {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public int getAttendanceId() {
        return attendanceId;
    }

    public int getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRoleDisplayName() {
        return roleDisplayName;
    }

    public LocalDateTime getCheckInAt() {
        return checkInAt;
    }

    public LocalDateTime getCheckOutAt() {
        return checkOutAt;
    }

    public boolean isOpen() {
        return checkOutAt == null;
    }

    public String getDateDisplay() {
        return checkInAt.format(DATE_FORMAT);
    }

    public String getCheckInDisplay() {
        return checkInAt.format(TIME_FORMAT);
    }

    /** An em dash rather than a blank, so an open shift reads as "still running", not "missing". */
    public String getCheckOutDisplay() {
        return checkOutAt == null ? "—" : checkOutAt.format(TIME_FORMAT);
    }

    /** Elapsed time so far for an open shift, total worked for a closed one. */
    public String getDurationDisplay() {
        Duration worked = Duration.between(checkInAt, checkOutAt == null ? LocalDateTime.now() : checkOutAt);
        long minutes = Math.max(0, worked.toMinutes());
        return (minutes / 60) + "h" + String.format("%02d", minutes % 60);
    }
}
