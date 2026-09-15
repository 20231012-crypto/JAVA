package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.AccountStatus;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Admin customer management (customers.manage): search, page through, and lock/unlock accounts. */
@WebServlet({"/admin/customers", "/admin/customers/toggle"})
public class CustomerServlet extends HttpServlet {

    private static final int PAGE_SIZE = 25;
    private static final int MAX_PAGE = 10_000;

    private final UserDAO userDAO = new UserDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String search = RequestParams.trimmedOrNull(req.getParameter("q"));
        int page = RequestParams.intInRange(req.getParameter("page"), 1, 1, MAX_PAGE);

        try (Connection conn = DBConnection.getConnection()) {
            int total = userDAO.countCustomers(conn, search);
            int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
            page = Math.min(page, totalPages);

            req.setAttribute("pageTitle", "Quản lý khách hàng");
            req.setAttribute("customers", userDAO.findCustomers(conn, search, PAGE_SIZE, (page - 1) * PAGE_SIZE));
            req.setAttribute("searchQuery", search);
            req.setAttribute("currentPage", page);
            req.setAttribute("totalPages", totalPages);
            req.setAttribute("totalCustomers", total);
            req.getRequestDispatcher("/WEB-INF/views/admin/customers.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer userId = RequestParams.intOrNull(req.getParameter("userId"));
        if (userId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            User target = userDAO.findById(conn, userId);
            // This screen manages customers only — a crafted userId must not become a way to
            // disable staff or an admin from here, which has its own screen and its own permission.
            if (target == null || !target.getRole().isCustomerDefault()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            AccountStatus next = "DISABLED".equals(req.getParameter("status"))
                    ? AccountStatus.DISABLED : AccountStatus.ACTIVE;
            userDAO.updateStatus(conn, userId, next);
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        String query = RequestParams.trimmedOrNull(req.getParameter("q"));
        resp.sendRedirect(req.getContextPath() + "/admin/customers"
                + (query == null ? "" : "?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)));
    }
}
