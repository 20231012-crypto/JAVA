package com.eaut.canteen.controller.auth;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.AccountStatus;
import com.eaut.canteen.model.Role;
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

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        showForm(req, resp, null, null);
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
                showForm(req, resp, "Tên đăng nhập hoặc mật khẩu không đúng.", username);
                return;
            }

            if (user.getStatus() == AccountStatus.DISABLED) {
                showForm(req, resp, "Tài khoản của bạn đã bị vô hiệu hoá.", username);
                return;
            }

            user.setPasswordHash(null);
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);

            resp.sendRedirect(req.getContextPath() + resolveDestination(user.getRole(), redirect));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, String error, String username)
            throws ServletException, IOException {
        req.setAttribute("pageTitle", "Đăng nhập");
        req.setAttribute("googleClientId", GoogleAuthUtil.getClientId());
        req.setAttribute("googleAllowedDomain", GoogleAuthUtil.getAllowedDomain());
        if (error != null) {
            req.setAttribute("error", error);
            req.setAttribute("username", username);
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    private String resolveDestination(Role role, String redirect) {
        if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
            return redirect;
        }
        return switch (role) {
            case ADMIN -> "/admin";
            case SALES_STAFF -> "/sales/orders";
            case STORE_STAFF -> "/store/transfers";
            case CUSTOMER -> "/products";
        };
    }
}
