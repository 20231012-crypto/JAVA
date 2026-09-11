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
 * Fills in the one piece of contact info Google sign-up never collects: a phone number. Used by
 * the "nhập số điện thoại" gate on checkout.jsp (Google-only accounts have phone=NULL until this
 * runs once) — a generic account endpoint rather than folding this into CheckoutServlet, since
 * nothing about it is checkout-specific.
 */
@WebServlet("/account/phone")
public class AccountServlet extends HttpServlet {

    private static final UserDAO userDAO = new UserDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User sessionUser = (User) req.getSession().getAttribute("user");
        String phone = normalize(req.getParameter("phone"));
        String redirect = req.getParameter("redirect");
        String destination = (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//"))
                ? redirect : "/checkout";

        if (phone == null) {
            req.getSession().setAttribute("phoneError", "Số điện thoại không hợp lệ — vui lòng nhập 9-11 chữ số.");
            resp.sendRedirect(req.getContextPath() + destination);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            userDAO.updatePhone(conn, sessionUser.getUserId(), phone);
            sessionUser.setPhone(phone);
            resp.sendRedirect(req.getContextPath() + destination);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /** Strips spaces/dashes and requires 9-11 digits (optionally a leading +) — loose on purpose, this only needs to be a reachable contact number, not a verified one. */
    private String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.trim().replaceAll("[\\s-]", "");
        if (!digits.matches("\\+?\\d{9,11}")) {
            return null;
        }
        return digits;
    }
}
