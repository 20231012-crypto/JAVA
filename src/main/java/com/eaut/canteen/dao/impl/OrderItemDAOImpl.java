package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.OrderItemDAO;
import com.eaut.canteen.model.OrderItem;

public class OrderItemDAOImpl implements OrderItemDAO {

    private static final String INSERT =
            "INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total) " +
            "VALUES (?, ?, ?, ?, ?)";
    private static final String FIND_BY_ORDER_ID =
            "SELECT oi.*, p.name AS product_name FROM order_items oi " +
            "JOIN products p ON oi.product_id = p.product_id " +
            "WHERE oi.order_id = ?";

    @Override
    public void insert(Connection conn, OrderItem item) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setInt(1, item.getOrderId());
            ps.setInt(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitPrice());
            ps.setBigDecimal(5, item.getLineTotal());
            ps.executeUpdate();
        }
    }

    @Override
    public List<OrderItem> findByOrderId(Connection conn, int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ORDER_ID)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setOrderItemId(rs.getInt("order_item_id"));
                    item.setOrderId(rs.getInt("order_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setLineTotal(rs.getBigDecimal("line_total"));
                    items.add(item);
                }
            }
        }
        return items;
    }
}
