package com.eaut.canteen.controller;

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
 * Lets any staff member self-toggle "đang trực" (on duty) — shown as a roster on the sales/store
 * Kanban boards. Not permission-gated in SecurityFilter (any logged-in non-customer can use it,
 * same as /cart is open to any logged-in user) — checked here instead since "which specific
 * functional permission" isn't the right question for a status any staff member sets for
 * themselves regardless of role.
 */
@WebServlet("/duty/toggle")
public class DutyServlet extends HttpServlet {

    private static final UserDAO userDAO = new UserDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        if (user.getRole().isCustomerDefault()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        boolean onDuty = "true".equals(req.getParameter("onDuty"));
        try (Connection conn = DBConnection.getConnection()) {
            userDAO.setOnDuty(conn, user.getUserId(), onDuty);
            user.setOnDuty(onDuty);
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        String redirect = req.getParameter("redirect");
        resp.sendRedirect(req.getContextPath() + (redirect != null && redirect.startsWith("/") ? redirect : "/products"));
    }
}
