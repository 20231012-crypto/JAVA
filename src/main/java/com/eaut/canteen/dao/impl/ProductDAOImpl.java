package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.model.Product;

public class ProductDAOImpl implements ProductDAO {

    private static final String BASE_SELECT =
            "SELECT p.*, c.name AS category_name, COALESCE(s.quantity, 0) AS shelf_quantity " +
            "FROM products p " +
            "JOIN categories c ON p.category_id = c.category_id " +
            "LEFT JOIN shelf_stock s ON p.product_id = s.product_id ";

    private static final String FIND_ALL_ACTIVE =
            BASE_SELECT + "WHERE p.is_active = TRUE ORDER BY p.name";
    private static final String FIND_ALL_ACTIVE_BY_CATEGORY =
            BASE_SELECT + "WHERE p.is_active = TRUE AND p.category_id = ? ORDER BY p.name";
    private static final String FIND_BY_ID =
            BASE_SELECT + "WHERE p.product_id = ?";

    @Override
    public List<Product> findAllActive(Connection conn) throws SQLException {
        List<Product> products = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL_ACTIVE);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(mapRow(rs));
            }
        }
        return products;
    }

    @Override
    public List<Product> findAllActiveByCategory(Connection conn, int categoryId) throws SQLException {
        List<Product> products = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL_ACTIVE_BY_CATEGORY)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
            }
        }
        return products;
    }

    @Override
    public Product findById(Connection conn, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getInt("product_id"));
        product.setCategoryId(rs.getInt("category_id"));
        product.setCategoryName(rs.getString("category_name"));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setImageFilename(rs.getString("image_filename"));
        product.setUnit(rs.getString("unit"));
        product.setActive(rs.getBoolean("is_active"));
        product.setShelfQuantity(rs.getInt("shelf_quantity"));
        return product;
    }
}
