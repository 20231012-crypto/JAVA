package com.eaut.canteen.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.AttendanceDAO;
import com.eaut.canteen.dao.impl.AttendanceDAOImpl;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Staff clock in/out (chấm công). Any signed-in staff member manages their own shifts here; the
 * admin-facing report over everyone's shifts lives at /admin/attendance.
 *
 * Deliberately not permission-gated beyond "is staff": recording your own working hours is not an
 * administrative action, and gating it would mean an admin has to grant every new hire a
 * permission before they can clock in at all.
 */
@WebServlet({"/attendance", "/attendance/clock"})
public class AttendanceServlet extends HttpServlet {

    private static final int HISTORY_LIMIT = 30;

    private final AttendanceDAO attendanceDAO = new AttendanceDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User staff = requireStaff(req, resp);
        if (staff == null) {
            return;
        }
        try (Connection conn = DBConnection.getConnection()) {
            showPage(req, resp, conn, staff);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User staff = requireStaff(req, resp);
        if (staff == null) {
            return;
        }

        HttpSession session = req.getSession();
        try (Connection conn = DBConnection.getConnection()) {
            if ("out".equals(req.getParameter("action"))) {
                session.setAttribute("attendanceMessage", attendanceDAO.checkOut(conn, staff.getUserId())
                        ? "Đã ghi nhận tan làm."
                        : "Bạn chưa vào làm nên không thể tan làm.");
            } else {
                session.setAttribute("attendanceMessage", attendanceDAO.checkIn(conn, staff.getUserId())
                        ? "Đã ghi nhận vào làm."
                        : "Bạn đang trong ca làm việc rồi.");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        // Redirect after POST so a refresh does not replay the clock action.
        resp.sendRedirect(req.getContextPath() + "/attendance");
    }

    private void showPage(HttpServletRequest req, HttpServletResponse resp, Connection conn, User staff)
            throws SQLException, ServletException, IOException {
        HttpSession session = req.getSession();
        Object message = session.getAttribute("attendanceMessage");
        if (message != null) {
            req.setAttribute("attendanceMessage", message);
            session.removeAttribute("attendanceMessage");
        }

        req.setAttribute("pageTitle", "Chấm công");
        req.setAttribute("openShift", attendanceDAO.findOpenShift(conn, staff.getUserId()));
        req.setAttribute("myShifts", attendanceDAO.findRecentByUser(conn, staff.getUserId(), HISTORY_LIMIT));
        req.getRequestDispatcher("/WEB-INF/views/staff/attendance.jsp").forward(req, resp);
    }

    /** Customers have no shifts to record; SecurityFilter has already established there is a session. */
    private User requireStaff(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || user.getRole().isCustomerDefault()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return user;
    }
}
