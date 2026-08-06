package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.StockTransferDAO;
import com.eaut.canteen.model.StockTransfer;

public class StockTransferDAOImpl implements StockTransferDAO {

    private static final String INSERT =
            "INSERT INTO stock_transfers (store_staff_id, product_id, quantity) VALUES (?, ?, ?)";
    private static final String FIND_RECENT =
            "SELECT t.*, u.full_name AS store_staff_name, p.name AS product_name FROM stock_transfers t " +
            "JOIN users u ON t.store_staff_id = u.user_id " +
            "JOIN products p ON t.product_id = p.product_id " +
            "ORDER BY t.transferred_at DESC LIMIT ?";

    @Override
    public void insert(Connection conn, StockTransfer transfer) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setInt(1, transfer.getStoreStaffId());
            ps.setInt(2, transfer.getProductId());
            ps.setInt(3, transfer.getQuantity());
            ps.executeUpdate();
        }
    }

    @Override
    public List<StockTransfer> findRecent(Connection conn, int limit) throws SQLException {
        List<StockTransfer> transfers = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_RECENT)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockTransfer transfer = new StockTransfer();
                    transfer.setTransferId(rs.getInt("transfer_id"));
                    transfer.setStoreStaffId(rs.getInt("store_staff_id"));
                    transfer.setStoreStaffName(rs.getString("store_staff_name"));
                    transfer.setProductId(rs.getInt("product_id"));
                    transfer.setProductName(rs.getString("product_name"));
                    transfer.setQuantity(rs.getInt("quantity"));
                    transfer.setTransferredAt(rs.getTimestamp("transferred_at").toLocalDateTime());
                    transfers.add(transfer);
                }
            }
        }
        return transfers;
    }
}
