package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.model.Product;

public class ProductDAOImpl implements ProductDAO {

    private static final String BASE_SELECT =
            "SELECT p.*, c.name AS category_name, " +
            "COALESCE(s.quantity, 0) AS shelf_quantity, COALESCE(w.quantity, 0) AS warehouse_quantity, " +
            "COALESCE(sold.sold_qty, 0) AS sold_quantity " +
            "FROM products p " +
            "JOIN categories c ON p.category_id = c.category_id " +
            "LEFT JOIN shelf_stock s ON p.product_id = s.product_id " +
            "LEFT JOIN warehouse_stock w ON p.product_id = w.product_id " +
            "LEFT JOIN (" +
            "  SELECT oi.product_id, SUM(oi.quantity) AS sold_qty FROM order_items oi " +
            "  JOIN orders o ON oi.order_id = o.order_id " +
            "  WHERE o.order_status = 'COMPLETED' GROUP BY oi.product_id" +
            ") sold ON sold.product_id = p.product_id ";

    private static final String FIND_ALL_ACTIVE =
            BASE_SELECT + "WHERE p.is_active = TRUE ORDER BY p.name";
    private static final String FIND_ALL_ACTIVE_BY_CATEGORY =
            BASE_SELECT + "WHERE p.is_active = TRUE AND p.category_id = ? ORDER BY p.name";
    private static final String FIND_BY_ID =
            BASE_SELECT + "WHERE p.product_id = ?";
    private static final String FIND_ALL_FOR_ADMIN =
            BASE_SELECT + "ORDER BY p.name";

    // Only these ORDER BY clauses can ever reach the database — the sort key arrives as a request
    // parameter, so it selects from this map rather than being pasted into the SQL.
    private static final Map<String, String> SORT_CLAUSES = Map.of(
            "gia-tang", "p.price ASC, p.name",
            "gia-giam", "p.price DESC, p.name",
            "ban-chay", "COALESCE(sold.sold_qty, 0) DESC, p.name",
            "moi", "p.created_at DESC, p.name");
    private static final String DEFAULT_SORT = "p.name";

    private static final String COUNT_SEARCH_BASE =
            "SELECT COUNT(*) FROM products p WHERE p.is_active = TRUE";

    private static final String FIND_BEST_SELLERS =
            BASE_SELECT +
            "JOIN (" +
            "  SELECT oi.product_id, SUM(oi.quantity) AS recent_qty FROM order_items oi " +
            "  JOIN orders o ON oi.order_id = o.order_id " +
            "  WHERE o.order_status = 'COMPLETED' AND o.created_at >= CURRENT_DATE - make_interval(days => ?) " +
            "  GROUP BY oi.product_id" +
            ") recent ON recent.product_id = p.product_id " +
            "WHERE p.is_active = TRUE ORDER BY recent.recent_qty DESC, p.name LIMIT ?";

    private static final String FIND_FAVORITES_BY_USER =
            BASE_SELECT +
            "JOIN favorites f ON f.product_id = p.product_id AND f.user_id = ? " +
            "WHERE p.is_active = TRUE ORDER BY f.created_at DESC LIMIT ?";

    private static final String FIND_ON_PROMO =
            BASE_SELECT +
            "WHERE p.is_active = TRUE AND p.original_price IS NOT NULL AND p.original_price > p.price " +
            "ORDER BY (p.original_price - p.price) DESC, p.name LIMIT ?";

    private static final String INSERT_PRODUCT =
            "INSERT INTO products (category_id, name, description, price, original_price, promo_target_quantity, unit, is_active, avg_prep_minutes) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_WAREHOUSE_STOCK =
            "INSERT INTO warehouse_stock (product_id, quantity) VALUES (?, 0)";
    private static final String INSERT_SHELF_STOCK =
            "INSERT INTO shelf_stock (product_id, quantity) VALUES (?, 0)";
    private static final String UPDATE =
            "UPDATE products SET category_id = ?, name = ?, description = ?, price = ?, original_price = ?, " +
            "promo_target_quantity = ?, unit = ?, avg_prep_minutes = ? WHERE product_id = ?";
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
            setNullableBigDecimal(ps, 5, product.getOriginalPrice());
            setNullableInt(ps, 6, product.getPromoTargetQuantity());
            ps.setString(7, product.getUnit());
            ps.setBoolean(8, true);
            ps.setInt(9, product.getAvgPrepMinutes());
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
            setNullableBigDecimal(ps, 5, product.getOriginalPrice());
            setNullableInt(ps, 6, product.getPromoTargetQuantity());
            ps.setString(7, product.getUnit());
            ps.setInt(8, product.getAvgPrepMinutes());
            ps.setInt(9, product.getProductId());
            ps.executeUpdate();
        }
    }

    private void setNullableBigDecimal(PreparedStatement ps, int index, java.math.BigDecimal value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.DECIMAL);
        } else {
            ps.setBigDecimal(index, value);
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

    @Override
    public List<Product> search(Connection conn, String query, Integer categoryId, String sort,
            int limit, int offset) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE p.is_active = TRUE");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, query, categoryId);

        // Map.of() rejects a null key outright (NPE, not a miss), and no ?sort= at all is the
        // common case — so the null check has to come before the lookup.
        String sortClause = sort == null ? DEFAULT_SORT : SORT_CLAUSES.getOrDefault(sort, DEFAULT_SORT);
        sql.append(" ORDER BY ").append(sortClause).append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<Product> products = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
            }
        }
        return products;
    }

    @Override
    public int countSearch(Connection conn, String query, Integer categoryId) throws SQLException {
        StringBuilder sql = new StringBuilder(COUNT_SEARCH_BASE);
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, query, categoryId);

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * The optional name/category filters, shared so the page query and its COUNT can never drift
     * apart and report a page count that does not match the rows.
     */
    private void appendFilters(StringBuilder sql, List<Object> params, String query, Integer categoryId) {
        if (query != null && !query.isBlank()) {
            // unaccent on both sides so a search typed without diacritics still matches.
            sql.append(" AND unaccent(p.name) ILIKE unaccent(?)");
            params.add("%" + query.trim() + "%");
        }
        if (categoryId != null) {
            sql.append(" AND p.category_id = ?");
            params.add(categoryId);
        }
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    @Override
    public List<Product> findBestSellers(Connection conn, int days, int limit) throws SQLException {
        List<Product> products = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BEST_SELLERS)) {
            ps.setInt(1, days);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
            }
        }
        return products;
    }

    @Override
    public List<Product> findFavoritesByUser(Connection conn, int userId, int limit) throws SQLException {
        List<Product> products = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_FAVORITES_BY_USER)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
            }
        }
        return products;
    }

    @Override
    public List<Product> findOnPromo(Connection conn, int limit) throws SQLException {
        List<Product> products = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ON_PROMO)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
            }
        }
        return products;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getInt("product_id"));
        product.setCategoryId(rs.getInt("category_id"));
        product.setCategoryName(rs.getString("category_name"));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setOriginalPrice(rs.getBigDecimal("original_price"));
        int promoTarget = rs.getInt("promo_target_quantity");
        product.setPromoTargetQuantity(rs.wasNull() ? null : promoTarget);
        product.setImageFilename(rs.getString("image_filename"));
        product.setUnit(rs.getString("unit"));
        product.setActive(rs.getBoolean("is_active"));
        product.setAvgPrepMinutes(rs.getInt("avg_prep_minutes"));
        product.setShelfQuantity(rs.getInt("shelf_quantity"));
        product.setWarehouseQuantity(rs.getInt("warehouse_quantity"));
        product.setSoldQuantity(rs.getInt("sold_quantity"));
        return product;
    }
}
