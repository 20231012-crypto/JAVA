package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.model.AccountStatus;
import com.eaut.canteen.model.Role;
import com.eaut.canteen.model.User;

public class UserDAOImpl implements UserDAO {

    private static final String FIND_BY_ID =
            "SELECT * FROM users WHERE user_id = ?";
    private static final String FIND_BY_USERNAME =
            "SELECT * FROM users WHERE username = ?";
    private static final String EXISTS_BY_USERNAME =
            "SELECT 1 FROM users WHERE username = ?";
    private static final String EXISTS_BY_EMAIL =
            "SELECT 1 FROM users WHERE email = ?";
    private static final String INSERT =
            "INSERT INTO users (username, password_hash, full_name, email, phone, role, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_ALL_STAFF =
            "SELECT * FROM users WHERE role <> 'CUSTOMER' ORDER BY role, full_name";
    private static final String UPDATE_STATUS =
            "UPDATE users SET status = ? WHERE user_id = ?";
    private static final String FIND_BY_ROLE =
            "SELECT * FROM users WHERE role = ? AND status = 'ACTIVE' ORDER BY full_name";

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
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole().name());
            ps.setString(7, user.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
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
    public List<User> findByRole(Connection conn, Role role) throws SQLException {
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ROLE)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
        }
        return users;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setStatus(AccountStatus.valueOf(rs.getString("status")));
        int buildingId = rs.getInt("building_id");
        user.setBuildingId(rs.wasNull() ? null : buildingId);
        if (rs.getTimestamp("created_at") != null) {
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return user;
    }
}
