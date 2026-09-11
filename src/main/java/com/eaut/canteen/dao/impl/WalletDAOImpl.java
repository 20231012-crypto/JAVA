package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.WalletDAO;
import com.eaut.canteen.model.WalletTransaction;
import com.eaut.canteen.model.WalletTransactionType;

public class WalletDAOImpl implements WalletDAO {

    private static final String INSERT =
            "INSERT INTO wallet_transactions (user_id, amount, type, order_id, created_by, note) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String FIND_BY_USER =
            "SELECT * FROM wallet_transactions WHERE user_id = ? ORDER BY created_at DESC";

    @Override
    public int insert(Connection conn, WalletTransaction tx) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tx.getUserId());
            ps.setBigDecimal(2, tx.getAmount());
            ps.setString(3, tx.getType().name());
            setNullableInt(ps, 4, tx.getOrderId());
            setNullableInt(ps, 5, tx.getCreatedBy());
            ps.setString(6, tx.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    @Override
    public List<WalletTransaction> findByUser(Connection conn, int userId) throws SQLException {
        List<WalletTransaction> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_USER)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WalletTransaction tx = new WalletTransaction();
                    tx.setTransactionId(rs.getInt("transaction_id"));
                    tx.setUserId(rs.getInt("user_id"));
                    tx.setAmount(rs.getBigDecimal("amount"));
                    tx.setType(WalletTransactionType.valueOf(rs.getString("type")));
                    int orderId = rs.getInt("order_id");
                    tx.setOrderId(rs.wasNull() ? null : orderId);
                    int createdBy = rs.getInt("created_by");
                    tx.setCreatedBy(rs.wasNull() ? null : createdBy);
                    tx.setNote(rs.getString("note"));
                    tx.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(tx);
                }
            }
        }
        return list;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
