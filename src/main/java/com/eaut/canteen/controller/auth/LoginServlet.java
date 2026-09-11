package com.eaut.canteen.controller.auth;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.RoleDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.RoleDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.AccountStatus;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.GoogleAuthUtil;
import com.eaut.canteen.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Serves both the login chooser ("/login") and the two separate screens it links to
 * ("/login/customer", "/login/staff") — one servlet, path-dispatched on getPathInfo(), rather
 * than three servlet classes for what is otherwise the exact same show/submit flow. Staff sign-in
 * (username+password) posts back to plain "/login"; Google sign-in posts to GoogleAuthServlet.
 */
@WebServlet({"/login", "/login/*"})
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAOImpl();
    private final RoleDAO roleDAO = new RoleDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo(); // null for "/login", "/customer" or "/staff" for the split screens
        if ("/staff".equals(pathInfo)) {
            showStaffForm(req, resp, null, null);
        } else if ("/customer".equals(pathInfo)) {
            showCustomerForm(req, resp, null);
        } else {
            req.setAttribute("pageTitle", "Đăng nhập");
            req.getRequestDispatcher("/WEB-INF/views/auth/login-choice.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String redirect = req.getParameter("redirect");

        try (Connection conn = DBConnection.getConnection()) {
            User user = userDAO.findByUsername(conn, username);

            // password_hash is null for Google-only accounts (no local password to check against);
            // guard against a null-hash NPE the same way as any other login failure.
            if (user == null || user.getPasswordHash() == null
                    || !PasswordUtil.verify(password, user.getPasswordHash())) {
                showStaffForm(req, resp, "Tên đăng nhập hoặc mật khẩu không đúng.", username);
                return;
            }

            if (user.getStatus() == AccountStatus.DISABLED) {
                showStaffForm(req, resp, "Tài khoản của bạn đã bị vô hiệu hoá.", username);
                return;
            }

            user.setPasswordHash(null);
            user.setPermissions(roleDAO.findPermissionMapForRole(conn, user.getRole().getRoleId()));
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);

            resp.sendRedirect(req.getContextPath() + resolveDestination(user, redirect));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showStaffForm(HttpServletRequest req, HttpServletResponse resp, String error, String username)
            throws ServletException, IOException {
        req.setAttribute("pageTitle", "Đăng nhập nhân viên");
        if (error != null) {
            req.setAttribute("error", error);
            req.setAttribute("username", username);
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/login-staff.jsp").forward(req, resp);
    }

    static void showCustomerForm(HttpServletRequest req, HttpServletResponse resp, String error)
            throws ServletException, IOException {
        req.setAttribute("pageTitle", "Đăng nhập khách hàng");
        req.setAttribute("googleClientId", GoogleAuthUtil.getClientId());
        if (error != null) {
            req.setAttribute("error", error);
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/login-customer.jsp").forward(req, resp);
    }

    /** First permission-gated area this user can actually reach, in a sensible priority order; falls back to the menu. */
    private String resolveDestination(User user, String redirect) {
        if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
            return redirect;
        }
        if (user.getRole().isCustomerDefault()) {
            return "/products";
        }
        if (user.hasPermission("admin.dashboard")) {
            return "/admin";
        }
        if (user.hasPermission("orders.queue") || user.hasPermission("sales.counter")) {
            return "/sales/orders";
        }
        if (user.hasPermission("store.transfer") || user.hasPermission("store.fulfillment")) {
            return "/store/transfers";
        }
        return "/products";
    }
}
