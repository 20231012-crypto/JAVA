package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.CategoryDAO;
import com.eaut.canteen.model.Category;

public class CategoryDAOImpl implements CategoryDAO {

    private static final String FIND_ALL_ACTIVE =
            "SELECT * FROM categories WHERE is_active = TRUE ORDER BY name";
    private static final String FIND_ALL =
            "SELECT * FROM categories ORDER BY name";
    private static final String INSERT =
            "INSERT INTO categories (name, parent_category_id) VALUES (?, ?)";
    private static final String UPDATE =
            "UPDATE categories SET name = ?, parent_category_id = ? WHERE category_id = ?";
    private static final String SET_ACTIVE =
            "UPDATE categories SET is_active = ? WHERE category_id = ?";

    @Override
    public List<Category> findAllActive(Connection conn) throws SQLException {
        return query(conn, FIND_ALL_ACTIVE);
    }

    @Override
    public List<Category> findAll(Connection conn) throws SQLException {
        return query(conn, FIND_ALL);
    }

    private List<Category> query(Connection conn, String sql) throws SQLException {
        List<Category> categories = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category category = new Category();
                category.setCategoryId(rs.getInt("category_id"));
                category.setName(rs.getString("name"));
                int parentId = rs.getInt("parent_category_id");
                category.setParentCategoryId(rs.wasNull() ? null : parentId);
                category.setActive(rs.getBoolean("is_active"));
                categories.add(category);
            }
        }
        return categories;
    }

    @Override
    public void insert(Connection conn, Category category) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            setNullableInt(ps, 2, category.getParentCategoryId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                category.setCategoryId(keys.getInt(1));
            }
        }
    }

    @Override
    public void update(Connection conn, Category category) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, category.getName());
            setNullableInt(ps, 2, category.getParentCategoryId());
            ps.setInt(3, category.getCategoryId());
            ps.executeUpdate();
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    @Override
    public void setActive(Connection conn, int categoryId, boolean active) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SET_ACTIVE)) {
            ps.setBoolean(1, active);
            ps.setInt(2, categoryId);
            ps.executeUpdate();
        }
    }
}
