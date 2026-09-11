package com.eaut.canteen.controller.customer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Fills in the contact/identity info Google sign-up never collects: phone for every customer,
 * plus MSSV/Khoa-lớp for EAUT-student customers (shown on kitchen order cards). Used by the
 * blocking info-gate modal on checkout.jsp — a generic account endpoint rather than folding this
 * into CheckoutServlet, since nothing about it is checkout-specific.
 */
@WebServlet("/account/phone")
public class AccountServlet extends HttpServlet {

    private static final UserDAO userDAO = new UserDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User sessionUser = (User) req.getSession().getAttribute("user");
        String phone = normalizePhone(req.getParameter("phone"));
        String redirect = req.getParameter("redirect");
        String destination = (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//"))
                ? redirect : "/checkout";

        if (phone == null) {
            req.getSession().setAttribute("phoneError", "Số điện thoại không hợp lệ — vui lòng nhập 9-11 chữ số.");
            resp.sendRedirect(req.getContextPath() + destination);
            return;
        }

        String studentId = trimToNull(req.getParameter("studentId"));
        String className = trimToNull(req.getParameter("className"));
        if (sessionUser.isEautStudent() && studentId == null) {
            req.getSession().setAttribute("phoneError", "Vui lòng nhập mã số sinh viên (MSSV).");
            resp.sendRedirect(req.getContextPath() + destination);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            userDAO.updatePhone(conn, sessionUser.getUserId(), phone);
            sessionUser.setPhone(phone);
            if (sessionUser.isEautStudent()) {
                userDAO.updateStudentInfo(conn, sessionUser.getUserId(), studentId, className);
                sessionUser.setStudentId(studentId);
                sessionUser.setClassName(className);
            }
            resp.sendRedirect(req.getContextPath() + destination);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /** Strips spaces/dashes and requires 9-11 digits (optionally a leading +) — loose on purpose, this only needs to be a reachable contact number, not a verified one. */
    private String normalizePhone(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.trim().replaceAll("[\\s-]", "");
        if (!digits.matches("\\+?\\d{9,11}")) {
            return null;
        }
        return digits;
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
