package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import com.eaut.canteen.model.AttendanceRecord;

public interface AttendanceDAO {

    /**
     * Opens a shift. Returns false when this staff member already has one open — the database
     * enforces that with a partial unique index, so two simultaneous clock-ins cannot both win.
     */
    boolean checkIn(Connection conn, int userId) throws SQLException;

    /**
     * Closes this staff member's open shift. Returns false when there was none to close, which is
     * what a double clock-out or a clock-out with no clock-in looks like.
     */
    boolean checkOut(Connection conn, int userId) throws SQLException;

    /** This staff member's currently open shift, or null if they are not clocked in. */
    AttendanceRecord findOpenShift(Connection conn, int userId) throws SQLException;

    /** This staff member's own recent shifts, newest first. */
    List<AttendanceRecord> findRecentByUser(Connection conn, int userId, int limit) throws SQLException;

    /**
     * Every shift that started within [from, to], newest first, for the admin report. A null
     * userId means all staff.
     */
    List<AttendanceRecord> findByDateRange(Connection conn, Integer userId, LocalDate from, LocalDate to)
            throws SQLException;
}
