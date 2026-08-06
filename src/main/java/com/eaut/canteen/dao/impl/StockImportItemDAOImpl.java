package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.StockImportItemDAO;
import com.eaut.canteen.model.StockImportItem;

public class StockImportItemDAOImpl implements StockImportItemDAO {

    private static final String INSERT =
            "INSERT INTO stock_import_items (import_id, product_id, quantity, unit_cost) VALUES (?, ?, ?, ?)";
    private static final String FIND_BY_IMPORT_ID =
            "SELECT si.*, p.name AS product_name FROM stock_import_items si " +
            "JOIN products p ON si.product_id = p.product_id WHERE si.import_id = ?";

    @Override
    public void insert(Connection conn, StockImportItem item) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setInt(1, item.getImportId());
            ps.setInt(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitCost());
            ps.executeUpdate();
        }
    }

    @Override
    public List<StockImportItem> findByImportId(Connection conn, int importId) throws SQLException {
        List<StockImportItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_IMPORT_ID)) {
            ps.setInt(1, importId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockImportItem item = new StockImportItem();
                    item.setImportItemId(rs.getInt("import_item_id"));
                    item.setImportId(rs.getInt("import_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitCost(rs.getBigDecimal("unit_cost"));
                    items.add(item);
                }
            }
        }
        return items;
    }
}
