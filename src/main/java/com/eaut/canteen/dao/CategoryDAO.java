package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.Category;

public interface CategoryDAO {

    List<Category> findAllActive(Connection conn) throws SQLException;

    List<Category> findAll(Connection conn) throws SQLException;

    void insert(Connection conn, Category category) throws SQLException;

    void update(Connection conn, Category category) throws SQLException;

    void setActive(Connection conn, int categoryId, boolean active) throws SQLException;
}
