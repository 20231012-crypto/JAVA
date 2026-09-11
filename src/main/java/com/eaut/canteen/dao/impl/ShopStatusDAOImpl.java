package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.eaut.canteen.dao.ShopStatusDAO;
import com.eaut.canteen.model.ShopStatus;

public class ShopStatusDAOImpl implements ShopStatusDAO {

    private static final String GET =
            "SELECT s.*, u.full_name AS updated_by_name FROM shop_status s " +
            "LEFT JOIN users u ON u.user_id = s.updated_by WHERE s.status_id = 1";
    private static final String SET_ACCEPTING =
            "UPDATE shop_status SET is_accepting_orders = ?, updated_by = ?, updated_at = NOW() WHERE status_id = 1";

    @Override
    public ShopStatus get(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(GET);
             ResultSet rs = ps.executeQuery()) {
            ShopStatus status = new ShopStatus();
            if (rs.next()) {
                status.setAcceptingOrders(rs.getBoolean("is_accepting_orders"));
                int updatedBy = rs.getInt("updated_by");
                status.setUpdatedBy(rs.wasNull() ? null : updatedBy);
                status.setUpdatedByName(rs.getString("updated_by_name"));
                if (rs.getTimestamp("updated_at") != null) {
                    status.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
            // No row yet (schema created before this feature) -> defaults to acceptingOrders=true,
            // matching the same "open by default" behavior the seeded row itself starts with.
            return status;
        }
    }

    @Override
    public void setAcceptingOrders(Connection conn, boolean acceptingOrders, int updatedBy) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SET_ACCEPTING)) {
            ps.setBoolean(1, acceptingOrders);
            ps.setInt(2, updatedBy);
            ps.executeUpdate();
        }
    }
}
