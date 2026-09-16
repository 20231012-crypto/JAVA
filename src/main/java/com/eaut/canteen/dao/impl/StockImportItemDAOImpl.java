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
            "INSERT INTO stock_import_items (import_id, product_id, quantity, unit_cost, received_quantity) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String FIND_BY_IMPORT =
            "SELECT it.import_item_id, it.import_id, it.product_id, it.quantity, it.unit_cost, " +
            "       it.received_quantity, p.name AS product_name, " +
            "       COALESCE(w.quantity, 0) AS current_stock " +
            "FROM stock_import_items it " +
            "JOIN products p ON it.product_id = p.product_id " +
            "LEFT JOIN warehouse_stock w ON w.product_id = it.product_id " +
            "WHERE it.import_id = ? ORDER BY it.import_item_id";

    private static final String UPDATE_RECEIVED =
            "UPDATE stock_import_items SET received_quantity = ? " +
            "WHERE import_item_id = ? AND received_quantity = ?";

    private static final String DELETE_BY_IMPORT =
            "DELETE FROM stock_import_items WHERE import_id = ?";

    @Override
    public void insert(Connection conn, StockImportItem item) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setInt(1, item.getImportId());
            ps.setInt(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitCost());
            ps.setInt(5, item.getReceivedQuantity());
            ps.executeUpdate();
        }
    }

    @Override
    public List<StockImportItem> findByImportId(Connection conn, int importId) throws SQLException {
        List<StockImportItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_IMPORT)) {
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
                    item.setReceivedQuantity(rs.getInt("received_quantity"));
                    item.setCurrentStock(rs.getInt("current_stock"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    @Override
    public int updateReceived(Connection conn, int importItemId, int expectedReceived, int newReceived)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_RECEIVED)) {
            ps.setInt(1, newReceived);
            ps.setInt(2, importItemId);
            ps.setInt(3, expectedReceived);
            return ps.executeUpdate();
        }
    }

    @Override
    public void deleteByImportId(Connection conn, int importId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_BY_IMPORT)) {
            ps.setInt(1, importId);
            ps.executeUpdate();
        }
    }
}
