package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.AttendanceDAO;
import com.eaut.canteen.model.AttendanceRecord;

public class AttendanceDAOImpl implements AttendanceDAO {

    /** Postgres SQLSTATE for a unique-constraint violation. */
    private static final String UNIQUE_VIOLATION = "23505";

    private static final String BASE_SELECT =
            "SELECT a.attendance_id, a.user_id, a.check_in_at, a.check_out_at, " +
            "u.full_name, r.display_name AS role_display_name " +
            "FROM staff_attendance a " +
            "JOIN users u ON u.user_id = a.user_id " +
            "JOIN roles r ON r.role_id = u.role_id ";

    private static final String CHECK_IN =
            "INSERT INTO staff_attendance (user_id) VALUES (?)";
    // Guarded: only closes a shift that is actually open, and the affected-row count tells the
    // caller whether there was one. A second clock-out therefore changes nothing and reports it.
    private static final String CHECK_OUT =
            "UPDATE staff_attendance SET check_out_at = CURRENT_TIMESTAMP " +
            "WHERE user_id = ? AND check_out_at IS NULL";
    private static final String FIND_OPEN =
            BASE_SELECT + "WHERE a.user_id = ? AND a.check_out_at IS NULL";
    private static final String FIND_RECENT_BY_USER =
            BASE_SELECT + "WHERE a.user_id = ? ORDER BY a.check_in_at DESC LIMIT ?";
    private static final String FIND_BY_RANGE =
            BASE_SELECT + "WHERE a.check_in_at >= ? AND a.check_in_at < ? ";
    private static final String FIND_BY_RANGE_USER =
            FIND_BY_RANGE + "AND a.user_id = ? ";
    private static final String RANGE_ORDER = "ORDER BY a.check_in_at DESC";

    @Override
    public boolean checkIn(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(CHECK_IN)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // The partial unique index (one open shift per user) is the real guard against a
            // double clock-in from two tabs; treat its violation as "already clocked in", not a
            // server error.
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public boolean checkOut(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(CHECK_OUT)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public AttendanceRecord findOpenShift(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_OPEN)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public List<AttendanceRecord> findRecentByUser(Connection conn, int userId, int limit) throws SQLException {
        List<AttendanceRecord> records = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_RECENT_BY_USER)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRow(rs));
                }
            }
        }
        return records;
    }

    @Override
    public List<AttendanceRecord> findByDateRange(Connection conn, Integer userId, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = (userId == null ? FIND_BY_RANGE : FIND_BY_RANGE_USER) + RANGE_ORDER;
        List<AttendanceRecord> records = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, from.atStartOfDay());
            // Exclusive upper bound on the next day, so a shift started at 23:30 on the last day
            // of the range is still included.
            ps.setObject(2, to.plusDays(1).atStartOfDay());
            if (userId != null) {
                ps.setInt(3, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRow(rs));
                }
            }
        }
        return records;
    }

    private AttendanceRecord mapRow(ResultSet rs) throws SQLException {
        return new AttendanceRecord(
                rs.getInt("attendance_id"),
                rs.getInt("user_id"),
                rs.getString("full_name"),
                rs.getString("role_display_name"),
                rs.getTimestamp("check_in_at").toLocalDateTime(),
                rs.getTimestamp("check_out_at") == null
                        ? null : rs.getTimestamp("check_out_at").toLocalDateTime());
    }
}
