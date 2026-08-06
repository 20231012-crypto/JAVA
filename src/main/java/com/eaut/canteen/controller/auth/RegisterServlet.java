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
import com.eaut.canteen.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("pageTitle", "Đăng ký");
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = trim(req.getParameter("username"));
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");
        String fullName = trim(req.getParameter("fullName"));
        String email = trim(req.getParameter("email"));
        String phone = trim(req.getParameter("phone"));

        req.setAttribute("pageTitle", "Đăng ký");
        req.setAttribute("username", username);
        req.setAttribute("fullName", fullName);
        req.setAttribute("email", email);
        req.setAttribute("phone", phone);

        try (Connection conn = DBConnection.getConnection()) {
            String error = validate(conn, username, password, confirmPassword, fullName, email);
            if (error != null) {
                req.setAttribute("error", error);
                req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
                return;
            }

            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(PasswordUtil.hash(password));
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phone.isEmpty() ? null : phone);
            user.setRole(Role.CUSTOMER);
            user.setStatus(AccountStatus.ACTIVE);

            int userId = userDAO.insert(conn, user);
            user.setUserId(userId);
            user.setPasswordHash(null);

            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            resp.sendRedirect(req.getContextPath() + "/products");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private String validate(Connection conn, String username, String password, String confirmPassword,
            String fullName, String email) throws SQLException {
        if (username.length() < 3 || username.length() > 50) {
            return "Tên đăng nhập phải từ 3 đến 50 ký tự.";
        }
        if (password == null || password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự.";
        }
        if (!password.equals(confirmPassword)) {
            return "Mật khẩu xác nhận không khớp.";
        }
        if (fullName.isEmpty()) {
            return "Vui lòng nhập họ tên.";
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return "Địa chỉ email không hợp lệ.";
        }
        if (userDAO.existsByUsername(conn, username)) {
            return "Tên đăng nhập đã được sử dụng.";
        }
        if (userDAO.existsByEmail(conn, email)) {
            return "Email đã được sử dụng.";
        }
        return null;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
