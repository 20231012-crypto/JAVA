package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eaut.canteen.dao.RoleDAO;
import com.eaut.canteen.dao.impl.RoleDAOImpl;
import com.eaut.canteen.model.Permission;
import com.eaut.canteen.model.Role;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Admin screen for the dynamic RBAC system: create/rename/delete roles ("đối tượng sử dụng") and
 * edit, in one matrix, which permissions each role holds. The customer-default role is excluded
 * from the matrix — customer-facing routes aren't permission-gated (see SecurityFilter), so it
 * has nothing configurable here.
 */
@WebServlet({"/admin/roles", "/admin/roles/save", "/admin/roles/permissions", "/admin/roles/delete"})
public class RoleServlet extends HttpServlet {

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
            switch (req.getServletPath()) {
                case "/admin/roles/save" -> createRole(req, resp, conn);
                case "/admin/roles/permissions" -> savePermissions(req, resp, conn);
                case "/admin/roles/delete" -> deleteRole(req, resp, conn);
                default -> resp.sendRedirect(req.getContextPath() + "/admin/roles");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void createRole(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        String displayName = req.getParameter("displayName");
        if (displayName == null || displayName.isBlank()) {
            showForm(req, resp, conn, "Vui lòng nhập tên vai trò.");
            return;
        }
        roleDAO.insert(conn, displayName.trim());
        resp.sendRedirect(req.getContextPath() + "/admin/roles");
    }

    private void deleteRole(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        int roleId = Integer.parseInt(req.getParameter("roleId"));
        Role role = roleDAO.findById(conn, roleId);
        if (role == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/roles");
            return;
        }
        if (role.isSystem()) {
            showForm(req, resp, conn, "Không thể xoá vai trò hệ thống (\"" + role.getDisplayName() + "\").");
            return;
        }
        int deleted = roleDAO.delete(conn, roleId);
        if (deleted == 0) {
            showForm(req, resp, conn, "Vai trò \"" + role.getDisplayName()
                    + "\" đang có " + roleDAO.countUsers(conn, roleId) + " tài khoản — chuyển tài khoản sang vai trò khác trước khi xoá.");
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/admin/roles");
    }

    /**
     * One form covers every editable role's permission checkboxes at once (checkbox name
     * "perm_{roleId}_{permissionKey}"); a role with no boxes checked simply submits none of its
     * keys, which is indistinguishable from "field absent" in HTML forms — so every editable role
     * is rebuilt from scratch here, not just the ones with at least one checked box.
     */
    private void savePermissions(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        List<Role> editableRoles = roleDAO.findAll(conn).stream().filter(r -> !r.isCustomerDefault()).toList();
        List<Permission> allPermissions = roleDAO.findAllPermissions(conn);

        Map<Integer, Set<String>> newPermissionsByRole = new LinkedHashMap<>();
        for (Role role : editableRoles) {
            Set<String> keys = new HashSet<>();
            for (Permission p : allPermissions) {
                if (req.getParameter("perm_" + role.getRoleId() + "_" + p.getPermissionKey()) != null) {
                    keys.add(p.getPermissionKey());
                }
            }
            newPermissionsByRole.put(role.getRoleId(), keys);
        }

        if (!keepsRolesManageable(conn, editableRoles, newPermissionsByRole)) {
            showForm(req, resp, conn, "Không thể lưu: phải luôn có ít nhất một vai trò đang có "
                    + "người dùng và giữ quyền \"Quản lý vai trò & phân quyền\", nếu không sẽ không ai "
                    + "còn sửa được phân quyền nữa.");
            return;
        }

        for (Map.Entry<Integer, Set<String>> entry : newPermissionsByRole.entrySet()) {
            roleDAO.setRolePermissions(conn, entry.getKey(), entry.getValue());
        }
        resp.sendRedirect(req.getContextPath() + "/admin/roles");
    }

    /** Refuses a save that would leave zero roles-with-users able to manage roles — that state can only be fixed by editing the database directly. */
    private boolean keepsRolesManageable(Connection conn, List<Role> roles, Map<Integer, Set<String>> newPermissionsByRole)
            throws SQLException {
        for (Role role : roles) {
            if (newPermissionsByRole.get(role.getRoleId()).contains("roles.manage")
                    && roleDAO.countUsers(conn, role.getRoleId()) > 0) {
                return true;
            }
        }
        return false;
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Connection conn, String error)
            throws SQLException, ServletException, IOException {
        List<Role> allRoles = roleDAO.findAll(conn);
        List<Role> editableRoles = allRoles.stream().filter(r -> !r.isCustomerDefault()).toList();
        List<Permission> permissions = roleDAO.findAllPermissions(conn);

        Map<Integer, Map<String, Boolean>> permissionsByRole = new LinkedHashMap<>();
        Map<Integer, Integer> userCountByRole = new LinkedHashMap<>();
        for (Role role : allRoles) {
            permissionsByRole.put(role.getRoleId(), roleDAO.findPermissionMapForRole(conn, role.getRoleId()));
            userCountByRole.put(role.getRoleId(), roleDAO.countUsers(conn, role.getRoleId()));
        }

        req.setAttribute("pageTitle", "Vai trò & phân quyền");
        req.setAttribute("allRoles", allRoles);
        req.setAttribute("editableRoles", editableRoles);
        req.setAttribute("permissions", permissions);
        req.setAttribute("permissionsByRole", permissionsByRole);
        req.setAttribute("userCountByRole", userCountByRole);
        if (error != null) {
            req.setAttribute("error", error);
        }
        req.getRequestDispatcher("/WEB-INF/views/admin/roles.jsp").forward(req, resp);
    }
}
