package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.CategoryDAO;
import com.eaut.canteen.model.Category;

public class CategoryDAOImpl implements CategoryDAO {

    private static final String FIND_ALL_ACTIVE =
            "SELECT * FROM categories WHERE is_active = TRUE ORDER BY name";

    @Override
    public List<Category> findAllActive(Connection conn) throws SQLException {
        List<Category> categories = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL_ACTIVE);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category category = new Category();
                category.setCategoryId(rs.getInt("category_id"));
                category.setName(rs.getString("name"));
                category.setActive(rs.getBoolean("is_active"));
                categories.add(category);
            }
        }
        return categories;
    }
}
