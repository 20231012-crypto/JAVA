package com.eaut.canteen.controller.auth;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.Role;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;
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
        req.setAttribute("pageTitle", "Đăng nhập");
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String redirect = req.getParameter("redirect");

        try (Connection conn = DBConnection.getConnection()) {
            User user = userDAO.findByUsername(conn, username);

            if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
                req.setAttribute("pageTitle", "Đăng nhập");
                req.setAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng.");
                req.setAttribute("username", username);
                req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
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
