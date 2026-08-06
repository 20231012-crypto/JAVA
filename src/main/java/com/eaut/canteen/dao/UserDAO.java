package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.model.User;

public interface UserDAO {

    User findById(Connection conn, int userId) throws SQLException;

    User findByUsername(Connection conn, String username) throws SQLException;

    boolean existsByUsername(Connection conn, String username) throws SQLException;

    boolean existsByEmail(Connection conn, String email) throws SQLException;

    int insert(Connection conn, User user) throws SQLException;
}
