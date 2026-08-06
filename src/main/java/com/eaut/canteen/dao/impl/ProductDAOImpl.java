package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.model.Product;

public class ProductDAOImpl implements ProductDAO {

    private static final String BASE_SELECT =
            "SELECT p.*, c.name AS category_name, " +
            "COALESCE(s.quantity, 0) AS shelf_quantity, COALESCE(w.quantity, 0) AS warehouse_quantity " +
            "FROM products p " +
            "JOIN categories c ON p.category_id = c.category_id " +
            "LEFT JOIN shelf_stock s ON p.product_id = s.product_id " +
            "LEFT JOIN warehouse_stock w ON p.product_id = w.product_id ";

    private static final String FIND_ALL_ACTIVE =
            BASE_SELECT + "WHERE p.is_active = TRUE ORDER BY p.name";
    private static final String FIND_ALL_ACTIVE_BY_CATEGORY =
            BASE_SELECT + "WHERE p.is_active = TRUE AND p.category_id = ? ORDER BY p.name";
    private static final String FIND_BY_ID =
            BASE_SELECT + "WHERE p.product_id = ?";
    private static final String FIND_ALL_FOR_ADMIN =
            BASE_SELECT + "ORDER BY p.name";

    private static final String INSERT_PRODUCT =
            "INSERT INTO products (category_id, name, description, price, unit, is_active) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String INSERT_WAREHOUSE_STOCK =
            "INSERT INTO warehouse_stock (product_id, quantity) VALUES (?, 0)";
    private static final String INSERT_SHELF_STOCK =
            "INSERT INTO shelf_stock (product_id, quantity) VALUES (?, 0)";
    private static final String UPDATE =
            "UPDATE products SET category_id = ?, name = ?, description = ?, price = ?, unit = ? WHERE product_id = ?";
    private static final String UPDATE_IMAGE =
            "UPDATE products SET image_filename = ? WHERE product_id = ?";
    private static final String SET_ACTIVE =
            "UPDATE products SET is_active = ? WHERE product_id = ?";

    @Override
    public List<Product> findAllActive(Connection conn) throws SQLException {
        return query(conn, FIND_ALL_ACTIVE);
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

    @Override
    public List<Product> findAllForAdmin(Connection conn) throws SQLException {
        return query(conn, FIND_ALL_FOR_ADMIN);
    }

    private List<Product> query(Connection conn, String sql) throws SQLException {
        List<Product> products = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(mapRow(rs));
            }
        }
        return products;
    }

    @Override
    public void insert(Connection conn, Product product) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_PRODUCT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, product.getCategoryId());
            ps.setString(2, product.getName());
            ps.setString(3, product.getDescription());
            ps.setBigDecimal(4, product.getPrice());
            ps.setString(5, product.getUnit());
            ps.setBoolean(6, true);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                product.setProductId(keys.getInt(1));
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(INSERT_WAREHOUSE_STOCK)) {
            ps.setInt(1, product.getProductId());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SHELF_STOCK)) {
            ps.setInt(1, product.getProductId());
            ps.executeUpdate();
        }
    }

    @Override
    public void update(Connection conn, Product product) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setInt(1, product.getCategoryId());
            ps.setString(2, product.getName());
            ps.setString(3, product.getDescription());
            ps.setBigDecimal(4, product.getPrice());
            ps.setString(5, product.getUnit());
            ps.setInt(6, product.getProductId());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateImage(Connection conn, int productId, String imageFilename) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_IMAGE)) {
            ps.setString(1, imageFilename);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    @Override
    public void setActive(Connection conn, int productId, boolean active) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SET_ACTIVE)) {
            ps.setBoolean(1, active);
            ps.setInt(2, productId);
            ps.executeUpdate();
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
        product.setWarehouseQuantity(rs.getInt("warehouse_quantity"));
        return product;
    }
}
