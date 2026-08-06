package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.AccountStatus;
import com.eaut.canteen.model.Role;
import com.eaut.canteen.model.User;

public interface UserDAO {

    User findById(Connection conn, int userId) throws SQLException;

    User findByUsername(Connection conn, String username) throws SQLException;

    boolean existsByUsername(Connection conn, String username) throws SQLException;

    boolean existsByEmail(Connection conn, String email) throws SQLException;

    int insert(Connection conn, User user) throws SQLException;

    /** Every non-customer role, for the admin staff-management screen. */
    List<User> findAllStaff(Connection conn) throws SQLException;

    void updateStatus(Connection conn, int userId, AccountStatus status) throws SQLException;

    List<User> findByRole(Connection conn, Role role) throws SQLException;
}
