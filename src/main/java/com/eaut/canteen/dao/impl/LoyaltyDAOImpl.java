package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.LoyaltyDAO;
import com.eaut.canteen.model.LoyaltyTransaction;
import com.eaut.canteen.model.LoyaltyTransactionType;

public class LoyaltyDAOImpl implements LoyaltyDAO {

    private static final String INSERT =
            "INSERT INTO loyalty_transactions (user_id, points, type, order_id, note) VALUES (?, ?, ?, ?, ?)";
    private static final String FIND_BY_USER =
            "SELECT * FROM loyalty_transactions WHERE user_id = ? ORDER BY created_at DESC";

    @Override
    public int insert(Connection conn, LoyaltyTransaction tx) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tx.getUserId());
            ps.setInt(2, tx.getPoints());
            ps.setString(3, tx.getType().name());
            setNullableInt(ps, 4, tx.getOrderId());
            ps.setString(5, tx.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    @Override
    public List<LoyaltyTransaction> findByUser(Connection conn, int userId) throws SQLException {
        List<LoyaltyTransaction> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_USER)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LoyaltyTransaction tx = new LoyaltyTransaction();
                    tx.setTransactionId(rs.getInt("transaction_id"));
                    tx.setUserId(rs.getInt("user_id"));
                    tx.setPoints(rs.getInt("points"));
                    tx.setType(LoyaltyTransactionType.valueOf(rs.getString("type")));
                    int orderId = rs.getInt("order_id");
                    tx.setOrderId(rs.wasNull() ? null : orderId);
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
