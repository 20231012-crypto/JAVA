package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eaut.canteen.model.Permission;
import com.eaut.canteen.model.Role;

public interface RoleDAO {

    List<Role> findAll(Connection conn) throws SQLException;

    Role findById(Connection conn, int roleId) throws SQLException;

    /** The role assigned to new self-service Google sign-ups — see GoogleAuthServlet. Exactly one role carries this. */
    Role findCustomerDefaultRole(Connection conn) throws SQLException;

    /** Creates a new role ("đối tượng sử dụng") with no permissions yet; role_key is slugified from displayName. */
    int insert(Connection conn, String displayName) throws SQLException;

    void updateDisplayName(Connection conn, int roleId, String displayName) throws SQLException;

    /** @return affected row count — 0 if the role doesn't exist or is a protected system role. */
    int delete(Connection conn, int roleId) throws SQLException;

    int countUsers(Connection conn, int roleId) throws SQLException;

    /** The fixed permission catalog — one row per real, enforced action (see SecurityFilter). Not admin-editable. */
    List<Permission> findAllPermissions(Connection conn) throws SQLException;

    /**
     * As a Map (not Set) so both this role-editor UI and User#hasPermission can use the same
     * {@code ${map['key']}} EL/Java lookup — see User#permissions.
     */
    Map<String, Boolean> findPermissionMapForRole(Connection conn, int roleId) throws SQLException;

    /** Replaces this role's entire permission set (delete-then-insert in one statement pair). */
    void setRolePermissions(Connection conn, int roleId, Set<String> permissionKeys) throws SQLException;
}
