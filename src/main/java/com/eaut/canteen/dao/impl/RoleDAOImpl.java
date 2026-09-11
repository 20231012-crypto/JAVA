package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eaut.canteen.dao.RoleDAO;
import com.eaut.canteen.model.Permission;
import com.eaut.canteen.model.Role;

public class RoleDAOImpl implements RoleDAO {

    private static final String FIND_ALL =
            "SELECT * FROM roles ORDER BY role_id";
    private static final String FIND_BY_ID =
            "SELECT * FROM roles WHERE role_id = ?";
    private static final String FIND_CUSTOMER_DEFAULT =
            "SELECT * FROM roles WHERE is_customer_default = TRUE";
    private static final String INSERT =
            "INSERT INTO roles (role_key, display_name) VALUES (?, ?)";
    private static final String UPDATE_DISPLAY_NAME =
            "UPDATE roles SET display_name = ? WHERE role_id = ?";
    private static final String DELETE =
            "DELETE FROM roles WHERE role_id = ? AND is_system = FALSE";
    private static final String COUNT_USERS =
            "SELECT COUNT(*) FROM users WHERE role_id = ?";
    private static final String FIND_ALL_PERMISSIONS =
            "SELECT * FROM permissions ORDER BY sort_order";
    private static final String FIND_PERMISSIONS_FOR_ROLE =
            "SELECT permission_key FROM role_permissions WHERE role_id = ?";
    private static final String DELETE_ROLE_PERMISSIONS =
            "DELETE FROM role_permissions WHERE role_id = ?";
    private static final String INSERT_ROLE_PERMISSION =
            "INSERT INTO role_permissions (role_id, permission_key) VALUES (?, ?)";
    private static final String EXISTS_ROLE_KEY =
            "SELECT 1 FROM roles WHERE role_key = ?";

    @Override
    public List<Role> findAll(Connection conn) throws SQLException {
        List<Role> roles = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roles.add(mapRow(rs));
            }
        }
        return roles;
    }

    @Override
    public Role findById(Connection conn, int roleId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public Role findCustomerDefaultRole(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_CUSTOMER_DEFAULT);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapRow(rs) : null;
        }
    }

    @Override
    public int insert(Connection conn, String displayName) throws SQLException {
        String roleKey = uniqueRoleKey(conn, displayName);
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, roleKey);
            ps.setString(2, displayName);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    @Override
    public void updateDisplayName(Connection conn, int roleId, String displayName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_DISPLAY_NAME)) {
            ps.setString(1, displayName);
            ps.setInt(2, roleId);
            ps.executeUpdate();
        }
    }

    @Override
    public int delete(Connection conn, int roleId) throws SQLException {
        if (countUsers(conn, roleId) > 0) {
            return 0; // caller shows "role still has users" instead of a raw FK-violation error
        }
        try (PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setInt(1, roleId);
            return ps.executeUpdate(); // 0 if roleId doesn't exist or is_system=TRUE
        }
    }

    @Override
    public int countUsers(Connection conn, int roleId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(COUNT_USERS)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public List<Permission> findAllPermissions(Connection conn) throws SQLException {
        List<Permission> permissions = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL_PERMISSIONS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Permission p = new Permission();
                p.setPermissionKey(rs.getString("permission_key"));
                p.setGroupName(rs.getString("group_name"));
                p.setDisplayName(rs.getString("display_name"));
                p.setSortOrder(rs.getInt("sort_order"));
                permissions.add(p);
            }
        }
        return permissions;
    }

    @Override
    public Map<String, Boolean> findPermissionMapForRole(Connection conn, int roleId) throws SQLException {
        Map<String, Boolean> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_PERMISSIONS_FOR_ROLE)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString(1), Boolean.TRUE);
                }
            }
        }
        return map;
    }

    @Override
    public void setRolePermissions(Connection conn, int roleId, Set<String> permissionKeys) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement(DELETE_ROLE_PERMISSIONS)) {
            del.setInt(1, roleId);
            del.executeUpdate();
        }
        try (PreparedStatement ins = conn.prepareStatement(INSERT_ROLE_PERMISSION)) {
            for (String key : permissionKeys) {
                ins.setInt(1, roleId);
                ins.setString(2, key);
                ins.addBatch();
            }
            if (!permissionKeys.isEmpty()) {
                ins.executeBatch();
            }
        }
    }

    /** ADMIN, "Nhân viên bán hàng" -> NHAN_VIEN_BAN_HANG (accents already gone from typical input; any remaining non A-Z0-9 collapses to "_"), de-duplicated against existing role_keys. */
    private String uniqueRoleKey(Connection conn, String displayName) throws SQLException {
        String base = displayName == null ? "" : displayName.trim().toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (base.isEmpty()) {
            base = "ROLE";
        }
        String candidate = base;
        int suffix = 2;
        while (roleKeyExists(conn, candidate)) {
            candidate = base + "_" + suffix;
            suffix++;
        }
        return candidate;
    }

    private boolean roleKeyExists(Connection conn, String roleKey) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_ROLE_KEY)) {
            ps.setString(1, roleKey);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Role mapRow(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setRoleId(rs.getInt("role_id"));
        role.setRoleKey(rs.getString("role_key"));
        role.setDisplayName(rs.getString("display_name"));
        role.setSystem(rs.getBoolean("is_system"));
        role.setCustomerDefault(rs.getBoolean("is_customer_default"));
        return role;
    }
}
