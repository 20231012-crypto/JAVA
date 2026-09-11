package com.eaut.canteen.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.model.AccountStatus;
import com.eaut.canteen.model.AuthProvider;
import com.eaut.canteen.model.Role;
import com.eaut.canteen.model.User;

public class UserDAOImpl implements UserDAO {

    // Every SELECT joins roles so a User is always built with its full Role (id/key/display
    // name/flags) rather than a bare id — mapRow relies on the joined columns being present.
    private static final String BASE_SELECT =
            "SELECT u.*, r.role_key, r.display_name AS role_display_name, r.is_system AS role_is_system, " +
            "r.is_customer_default AS role_is_customer_default " +
            "FROM users u JOIN roles r ON r.role_id = u.role_id ";
    private static final String FIND_BY_ID =
            BASE_SELECT + "WHERE u.user_id = ?";
    private static final String FIND_BY_USERNAME =
            BASE_SELECT + "WHERE u.username = ?";
    private static final String FIND_BY_EMAIL =
            BASE_SELECT + "WHERE u.email = ?";
    private static final String FIND_BY_GOOGLE_SUB =
            BASE_SELECT + "WHERE u.google_sub = ?";
    private static final String EXISTS_BY_USERNAME =
            "SELECT 1 FROM users WHERE username = ?";
    private static final String EXISTS_BY_EMAIL =
            "SELECT 1 FROM users WHERE email = ?";
    private static final String INSERT =
            "INSERT INTO users (username, password_hash, google_sub, auth_provider, full_name, email, phone, role_id, status, is_eaut_student) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_ALL_STAFF =
            BASE_SELECT + "WHERE r.is_customer_default = FALSE ORDER BY r.display_name, u.full_name";
    private static final String UPDATE_STATUS =
            "UPDATE users SET status = ? WHERE user_id = ?";
    private static final String FIND_BY_ROLE_ID =
            BASE_SELECT + "WHERE u.role_id = ? AND u.status = 'ACTIVE' ORDER BY u.full_name";
    private static final String LINK_GOOGLE_ACCOUNT =
            "UPDATE users SET google_sub = ?, auth_provider = 'GOOGLE' WHERE user_id = ?";
    private static final String ADJUST_WALLET =
            "UPDATE users SET wallet_balance = wallet_balance + ? WHERE user_id = ?";
    private static final String ADJUST_LOYALTY =
            "UPDATE users SET loyalty_points = loyalty_points + ? WHERE user_id = ?";

    @Override
    public User findById(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public User findByUsername(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public User findByEmail(Connection conn, String email) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public User findByGoogleSub(Connection conn, String googleSub) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_GOOGLE_SUB)) {
            ps.setString(1, googleSub);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public boolean existsByUsername(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean existsByEmail(Connection conn, String email) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public int insert(Connection conn, User user) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getGoogleSub());
            ps.setString(4, user.getAuthProvider().name());
            ps.setString(5, user.getFullName());
            ps.setString(6, user.getEmail());
            ps.setString(7, user.getPhone());
            ps.setInt(8, user.getRole().getRoleId());
            ps.setString(9, user.getStatus().name());
            ps.setBoolean(10, user.isEautStudent());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    @Override
    public void linkGoogleAccount(Connection conn, int userId, String googleSub) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(LINK_GOOGLE_ACCOUNT)) {
            ps.setString(1, googleSub);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<User> findAllStaff(Connection conn) throws SQLException {
        List<User> staff = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL_STAFF);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                staff.add(mapRow(rs));
            }
        }
        return staff;
    }

    @Override
    public void updateStatus(Connection conn, int userId, AccountStatus status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status.name());
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<User> findByRole(Connection conn, int roleId) throws SQLException {
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ROLE_ID)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
        }
        return users;
    }

    @Override
    public void adjustWalletBalance(Connection conn, int userId, BigDecimal delta) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(ADJUST_WALLET)) {
            ps.setBigDecimal(1, delta);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public void adjustLoyaltyPoints(Connection conn, int userId, int delta) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(ADJUST_LOYALTY)) {
            ps.setInt(1, delta);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setGoogleSub(rs.getString("google_sub"));
        user.setAuthProvider(AuthProvider.valueOf(rs.getString("auth_provider")));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));

        Role role = new Role();
        role.setRoleId(rs.getInt("role_id"));
        role.setRoleKey(rs.getString("role_key"));
        role.setDisplayName(rs.getString("role_display_name"));
        role.setSystem(rs.getBoolean("role_is_system"));
        role.setCustomerDefault(rs.getBoolean("role_is_customer_default"));
        user.setRole(role);

        user.setStatus(AccountStatus.valueOf(rs.getString("status")));
        int buildingId = rs.getInt("building_id");
        user.setBuildingId(rs.wasNull() ? null : buildingId);
        user.setWalletBalance(rs.getBigDecimal("wallet_balance"));
        user.setEautStudent(rs.getBoolean("is_eaut_student"));
        user.setLoyaltyPoints(rs.getInt("loyalty_points"));
        if (rs.getTimestamp("created_at") != null) {
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return user;
    }
}
