package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.RoleDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.RoleDAOImpl;
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

@WebServlet({"/admin/staff", "/admin/staff/save", "/admin/staff/toggle"})
public class StaffAccountServlet extends HttpServlet {

    private static final UserDAO userDAO = new UserDAOImpl();
    private static final RoleDAO roleDAO = new RoleDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            showForm(req, resp, conn, null);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            if ("/admin/staff/toggle".equals(req.getServletPath())) {
                int userId = Integer.parseInt(req.getParameter("userId"));
                AccountStatus status = AccountStatus.valueOf(req.getParameter("status"));
                userDAO.updateStatus(conn, userId, status);
                resp.sendRedirect(req.getContextPath() + "/admin/staff");
                return;
            }
            createStaff(req, resp, conn);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void createStaff(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String phone = req.getParameter("phone");
        int roleId = Integer.parseInt(req.getParameter("roleId"));
        Role role = roleDAO.findById(conn, roleId);

        if (username == null || username.isBlank() || password == null || password.length() < 6
                || fullName == null || fullName.isBlank() || email == null || email.isBlank()
                || role == null || role.isCustomerDefault()) {
            showForm(req, resp, conn, "Vui lòng nhập đầy đủ thông tin hợp lệ, mật khẩu tối thiểu 6 ký tự.");
            return;
        }
        if (userDAO.existsByUsername(conn, username) || userDAO.existsByEmail(conn, email)) {
            showForm(req, resp, conn, "Tên đăng nhập hoặc email đã được sử dụng.");
            return;
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        userDAO.insert(conn, user);

        resp.sendRedirect(req.getContextPath() + "/admin/staff");
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Connection conn, String error)
            throws SQLException, ServletException, IOException {
        req.setAttribute("pageTitle", "Tài khoản nhân viên");
        req.setAttribute("staff", userDAO.findAllStaff(conn));
        // Customers self-register via Google, not this form — offer every other role, including
        // any custom ones an admin has created in /admin/roles.
        req.setAttribute("assignableRoles", roleDAO.findAll(conn).stream()
                .filter(r -> !r.isCustomerDefault()).toList());
        if (error != null) {
            req.setAttribute("error", error);
        }
        req.getRequestDispatcher("/WEB-INF/views/admin/staff-accounts.jsp").forward(req, resp);
    }
}
