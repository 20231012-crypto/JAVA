package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.OrderStatusHistoryDAO;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.OrderStatusHistory;

public class OrderStatusHistoryDAOImpl implements OrderStatusHistoryDAO {

    private static final String INSERT =
            "INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, note) " +
            "VALUES (?, ?, ?, ?, ?)";
    private static final String FIND_BY_ORDER_ID =
            "SELECT h.*, u.full_name AS changed_by_name FROM order_status_history h " +
            "JOIN users u ON h.changed_by = u.user_id " +
            "WHERE h.order_id = ? ORDER BY h.changed_at";

    @Override
    public void insert(Connection conn, int orderId, OrderStatus oldStatus, OrderStatus newStatus, int changedBy, String note)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setInt(1, orderId);
            if (oldStatus == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, oldStatus.name());
            }
            ps.setString(3, newStatus.name());
            ps.setInt(4, changedBy);
            ps.setString(5, note);
            ps.executeUpdate();
        }
    }

    @Override
    public List<OrderStatusHistory> findByOrderId(Connection conn, int orderId) throws SQLException {
        List<OrderStatusHistory> history = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ORDER_ID)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderStatusHistory entry = new OrderStatusHistory();
                    entry.setHistoryId(rs.getInt("history_id"));
                    entry.setOrderId(rs.getInt("order_id"));
                    String oldStatus = rs.getString("old_status");
                    entry.setOldStatus(oldStatus == null ? null : OrderStatus.valueOf(oldStatus));
                    entry.setNewStatus(OrderStatus.valueOf(rs.getString("new_status")));
                    entry.setChangedBy(rs.getInt("changed_by"));
                    entry.setChangedByName(rs.getString("changed_by_name"));
                    entry.setNote(rs.getString("note"));
                    entry.setChangedAt(rs.getTimestamp("changed_at").toLocalDateTime());
                    history.add(entry);
                }
            }
        }
        return history;
    }
}
