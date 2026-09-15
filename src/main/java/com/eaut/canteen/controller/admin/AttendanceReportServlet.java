package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eaut.canteen.dao.AttendanceDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.AttendanceDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.AttendanceRecord;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Admin attendance report (attendance.view): who worked when, and how long, over a date range. */
@WebServlet("/admin/attendance")
public class AttendanceReportServlet extends HttpServlet {

    private static final int DEFAULT_RANGE_DAYS = 7;

    private final AttendanceDAO attendanceDAO = new AttendanceDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LocalDate today = LocalDate.now();
        LocalDate from = parseDateOr(req.getParameter("from"), today.minusDays(DEFAULT_RANGE_DAYS - 1L));
        LocalDate to = parseDateOr(req.getParameter("to"), today);
        if (to.isBefore(from)) {
            // A backwards range would silently return nothing; swapping is what the user meant.
            LocalDate swap = from;
            from = to;
            to = swap;
        }
        Integer staffId = RequestParams.intOrNull(req.getParameter("staffId"));

        try (Connection conn = DBConnection.getConnection()) {
            List<AttendanceRecord> records = attendanceDAO.findByDateRange(conn, staffId, from, to);

            req.setAttribute("pageTitle", "Chấm công nhân viên");
            req.setAttribute("records", records);
            req.setAttribute("totalsByStaff", totalHoursByStaff(records));
            req.setAttribute("staffList", userDAO.findAllStaff(conn));
            req.setAttribute("from", from);
            req.setAttribute("to", to);
            req.setAttribute("selectedStaffId", staffId);
            req.getRequestDispatcher("/WEB-INF/views/admin/attendance.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Hours per staff member over the range, summed in Java from the same rows the table shows
     * rather than by a second query, so the totals cannot disagree with the list under them.
     * An open shift counts up to now, matching what the row itself displays.
     */
    private Map<String, String> totalHoursByStaff(List<AttendanceRecord> records) {
        Map<String, Long> minutesByStaff = new LinkedHashMap<>();
        for (AttendanceRecord record : records) {
            LocalDateTime end = record.checkOutAt() == null ? LocalDateTime.now() : record.checkOutAt();
            long minutes = Math.max(0, Duration.between(record.checkInAt(), end).toMinutes());
            minutesByStaff.merge(record.fullName(), minutes, Long::sum);
        }

        Map<String, String> display = new LinkedHashMap<>();
        minutesByStaff.forEach((name, minutes) ->
                display.put(name, (minutes / 60) + "h" + String.format("%02d", minutes % 60)));
        return display;
    }

    /** A missing or unparseable date falls back rather than 500-ing the report. */
    private LocalDate parseDateOr(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
