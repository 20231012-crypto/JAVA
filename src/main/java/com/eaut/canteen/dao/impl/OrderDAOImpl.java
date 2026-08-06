package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderChannel;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.PaymentMethod;
import com.eaut.canteen.model.PaymentStatus;

public class OrderDAOImpl implements OrderDAO {

    private static final String BASE_SELECT =
            "SELECT o.*, b.name AS building_name FROM orders o " +
            "LEFT JOIN buildings b ON o.building_id = b.building_id ";

    private static final String INSERT =
            "INSERT INTO orders (customer_id, building_id, channel, sold_by, subtotal, shipping_fee, " +
            "total_amount, order_status, payment_method, payment_status, note) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_BY_ID = BASE_SELECT + "WHERE o.order_id = ?";
    private static final String FIND_BY_CUSTOMER = BASE_SELECT + "WHERE o.customer_id = ? ORDER BY o.created_at DESC";

    @Override
    public int insert(Connection conn, Order order) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            setNullableInt(ps, 1, order.getCustomerId());
            setNullableInt(ps, 2, order.getBuildingId());
            ps.setString(3, order.getChannel().name());
            setNullableInt(ps, 4, order.getSoldBy());
            ps.setBigDecimal(5, order.getSubtotal());
            ps.setBigDecimal(6, order.getShippingFee());
            ps.setBigDecimal(7, order.getTotalAmount());
            ps.setString(8, order.getOrderStatus().name());
            ps.setString(9, order.getPaymentMethod().name());
            ps.setString(10, order.getPaymentStatus().name());
            ps.setString(11, order.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    @Override
    public Order findById(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public List<Order> findByCustomer(Connection conn, int customerId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_CUSTOMER)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
        }
        return orders;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getInt("order_id"));

        int customerId = rs.getInt("customer_id");
        order.setCustomerId(rs.wasNull() ? null : customerId);

        int buildingId = rs.getInt("building_id");
        order.setBuildingId(rs.wasNull() ? null : buildingId);
        order.setBuildingName(rs.getString("building_name"));

        order.setChannel(OrderChannel.valueOf(rs.getString("channel")));

        int soldBy = rs.getInt("sold_by");
        order.setSoldBy(rs.wasNull() ? null : soldBy);

        order.setSubtotal(rs.getBigDecimal("subtotal"));
        order.setShippingFee(rs.getBigDecimal("shipping_fee"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setOrderStatus(OrderStatus.valueOf(rs.getString("order_status")));
        order.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        order.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));

        int paymentConfirmedBy = rs.getInt("payment_confirmed_by");
        order.setPaymentConfirmedBy(rs.wasNull() ? null : paymentConfirmedBy);
        if (rs.getTimestamp("payment_confirmed_at") != null) {
            order.setPaymentConfirmedAt(rs.getTimestamp("payment_confirmed_at").toLocalDateTime());
        }

        order.setNote(rs.getString("note"));
        order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        order.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return order;
    }
}
