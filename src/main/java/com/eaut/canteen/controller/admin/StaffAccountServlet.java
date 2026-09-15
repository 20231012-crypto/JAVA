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
import com.eaut.canteen.util.RequestParams;
import com.eaut.canteen.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet({"/admin/staff", "/admin/staff/save", "/admin/staff/toggle",
             "/admin/staff/update", "/admin/staff/password"})
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
            String path = req.getServletPath();
            if ("/admin/staff/toggle".equals(path)) {
                disableOrEnable(req, conn);
                resp.sendRedirect(req.getContextPath() + "/admin/staff");
                return;
            }
            if ("/admin/staff/update".equals(path)) {
                String failure = updateStaff(req, conn);
                if (failure != null) {
                    req.getSession().setAttribute("actionError", failure);
                }
                resp.sendRedirect(req.getContextPath() + "/admin/staff");
                return;
            }
            if ("/admin/staff/password".equals(path)) {
                String failure = resetPassword(req, conn);
                req.getSession().setAttribute(failure == null ? "actionMessage" : "actionError",
                        failure == null ? "Đã đặt lại mật khẩu." : failure);
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

    /**
     * Disabling an account is the only way to retire staff — a users row cannot be deleted once it
     * has signed anything, because order_status_history.changed_by and the stock ledger both
     * reference it with ON DELETE RESTRICT.
     *
     * <p>Refuses the change that would leave nobody able to administer permissions. Without this
     * an admin can disable the last roles.manage holder and lock the whole system's access control
     * behind a door only that account could open.
     */
    private void disableOrEnable(HttpServletRequest req, Connection conn) throws SQLException {
        Integer userId = RequestParams.intOrNull(req.getParameter("userId"));
        if (userId == null) {
            return;
        }
        AccountStatus status;
        try {
            status = AccountStatus.valueOf(req.getParameter("status"));
        } catch (IllegalArgumentException | NullPointerException e) {
            return;
        }
        if (status == AccountStatus.DISABLED && wouldOrphanRoleAdmin(conn, userId)) {
            req.getSession().setAttribute("actionError",
                    "Không thể khóa tài khoản này — đây là người cuối cùng còn quyền quản lý vai trò.");
            return;
        }
        userDAO.updateStatus(conn, userId, status);
    }

    private String updateStaff(HttpServletRequest req, Connection conn) throws SQLException {
        Integer userId = RequestParams.intOrNull(req.getParameter("userId"));
        Integer roleId = RequestParams.intOrNull(req.getParameter("roleId"));
        String fullName = RequestParams.trimmedOrNull(req.getParameter("fullName"));
        String email = RequestParams.trimmedOrNull(req.getParameter("email"));
        String phone = RequestParams.trimmedOrNull(req.getParameter("phone"));

        if (userId == null || fullName == null || email == null) {
            return "Họ tên và email không được để trống.";
        }
        User target = userDAO.findById(conn, userId);
        if (target == null || target.getRole().isCustomerDefault()) {
            return "Không tìm thấy tài khoản nhân viên.";
        }
        userDAO.updateStaffDetails(conn, userId, fullName, email, phone);

        if (roleId != null && roleId != target.getRole().getRoleId()) {
            Role role = roleDAO.findById(conn, roleId);
            if (role == null || role.isCustomerDefault()) {
                return "Vai trò không hợp lệ.";
            }
            // Same guard as disabling: moving the last permission-admin into a role without
            // roles.manage locks access control away just as effectively as disabling them.
            if (wouldOrphanRoleAdmin(conn, userId) && !roleGrants(conn, roleId, "roles.manage")) {
                return "Không thể đổi vai trò — đây là người cuối cùng còn quyền quản lý vai trò.";
            }
            userDAO.updateRole(conn, userId, roleId);
        }
        return null;
    }

    private String resetPassword(HttpServletRequest req, Connection conn) throws SQLException {
        Integer userId = RequestParams.intOrNull(req.getParameter("userId"));
        String password = req.getParameter("newPassword");
        if (userId == null || password == null || password.length() < 6) {
            return "Mật khẩu mới phải có ít nhất 6 ký tự.";
        }
        User target = userDAO.findById(conn, userId);
        if (target == null || target.getRole().isCustomerDefault()) {
            return "Không tìm thấy tài khoản nhân viên.";
        }
        userDAO.updatePassword(conn, userId, PasswordUtil.hash(password));
        return null;
    }

    /** True when this account is the only ACTIVE one that can still manage permissions. */
    private boolean wouldOrphanRoleAdmin(Connection conn, int userId) throws SQLException {
        User target = userDAO.findById(conn, userId);
        if (target == null || !roleGrants(conn, target.getRole().getRoleId(), "roles.manage")) {
            return false;
        }
        return userDAO.countActiveHoldersOfPermission(conn, "roles.manage") <= 1;
    }

    private boolean roleGrants(Connection conn, int roleId, String permissionKey) throws SQLException {
        return Boolean.TRUE.equals(roleDAO.findPermissionMapForRole(conn, roleId).get(permissionKey));
    }
}
