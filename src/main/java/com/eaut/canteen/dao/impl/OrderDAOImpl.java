package com.eaut.canteen.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static final String NEXT_TICKET_NO = "SELECT nextval('order_ticket_seq')";

    private static final String INSERT =
            "INSERT INTO orders (order_code, customer_id, building_id, channel, sold_by, subtotal, shipping_fee, " +
            "discount_amount, loyalty_points_used, loyalty_discount_amount, total_amount, order_status, " +
            "payment_method, payment_status, note) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_BY_ID = BASE_SELECT + "WHERE o.order_id = ?";
    private static final String FIND_BY_CUSTOMER = BASE_SELECT + "WHERE o.customer_id = ? ORDER BY o.created_at DESC";
    private static final String FIND_BY_STATUS = BASE_SELECT + "WHERE o.order_status = ? ORDER BY o.created_at";
    private static final String FIND_ALL = BASE_SELECT + "ORDER BY o.created_at DESC";
    private static final String UPDATE_STATUS =
            "UPDATE orders SET order_status = ? WHERE order_id = ? AND order_status = ?";
    private static final String MARK_PAID =
            "UPDATE orders SET payment_status = 'PAID', payment_confirmed_by = ?, payment_confirmed_at = NOW() " +
            "WHERE order_id = ? AND payment_status = 'UNPAID'";
    // DATE(...) and CURRENT_DATE are both standard SQL, understood the same way by MySQL (local
    // dev) and PostgreSQL (Render+Neon) — see DBConnection for how the driver itself is picked.
    private static final String SUM_REVENUE_TODAY =
            "SELECT COALESCE(SUM(total_amount), 0) FROM orders " +
            "WHERE order_status = 'COMPLETED' AND DATE(created_at) = CURRENT_DATE";
    private static final String COUNT_BY_STATUS =
            "SELECT COUNT(*) FROM orders WHERE order_status = ?";
    private static final String COUNT_BY_HOUR_TODAY =
            "SELECT EXTRACT(HOUR FROM created_at) AS hour_of_day, COUNT(*) FROM orders " +
            "WHERE DATE(created_at) = CURRENT_DATE GROUP BY hour_of_day ORDER BY hour_of_day";

    @Override
    public int insert(Connection conn, Order order) throws SQLException {
        // A dedicated sequence (rather than deriving the code from order_id after insert) means
        // the code is ready before the row exists — one INSERT, no placeholder-then-UPDATE step,
        // and no unique-constraint contention between concurrent checkouts.
        String prefix = order.getChannel() == OrderChannel.COUNTER ? "C-" : "A-";
        String orderCode;
        try (PreparedStatement seq = conn.prepareStatement(NEXT_TICKET_NO);
             ResultSet rs = seq.executeQuery()) {
            rs.next();
            orderCode = prefix + rs.getLong(1);
        }
        order.setOrderCode(orderCode);

        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, orderCode);
            setNullableInt(ps, 2, order.getCustomerId());
            setNullableInt(ps, 3, order.getBuildingId());
            ps.setString(4, order.getChannel().name());
            setNullableInt(ps, 5, order.getSoldBy());
            ps.setBigDecimal(6, order.getSubtotal());
            ps.setBigDecimal(7, order.getShippingFee());
            ps.setBigDecimal(8, order.getDiscountAmount());
            ps.setInt(9, order.getLoyaltyPointsUsed());
            ps.setBigDecimal(10, order.getLoyaltyDiscountAmount());
            ps.setBigDecimal(11, order.getTotalAmount());
            ps.setString(12, order.getOrderStatus().name());
            ps.setString(13, order.getPaymentMethod().name());
            ps.setString(14, order.getPaymentStatus().name());
            ps.setString(15, order.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int orderId = keys.getInt(1);
                order.setOrderId(orderId);
                return orderId;
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

    @Override
    public List<Order> findByStatus(Connection conn, OrderStatus status) throws SQLException {
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_STATUS)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
        }
        return orders;
    }

    @Override
    public List<Order> findAll(Connection conn) throws SQLException {
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                orders.add(mapRow(rs));
            }
        }
        return orders;
    }

    @Override
    public int updateStatus(Connection conn, int orderId, OrderStatus expectedCurrent, OrderStatus newStatus) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, orderId);
            ps.setString(3, expectedCurrent.name());
            return ps.executeUpdate();
        }
    }

    @Override
    public int markPaid(Connection conn, int orderId, int confirmedBy) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(MARK_PAID)) {
            ps.setInt(1, confirmedBy);
            ps.setInt(2, orderId);
            return ps.executeUpdate();
        }
    }

    @Override
    public BigDecimal sumRevenueToday(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SUM_REVENUE_TODAY);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getBigDecimal(1);
        }
    }

    @Override
    public int countByStatus(Connection conn, OrderStatus status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(COUNT_BY_STATUS)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public Map<Integer, Integer> countOrdersByHourToday(Connection conn) throws SQLException {
        Map<Integer, Integer> byHour = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(COUNT_BY_HOUR_TODAY);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                byHour.put(rs.getInt(1), rs.getInt(2));
            }
        }
        return byHour;
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
        order.setOrderCode(rs.getString("order_code"));

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
        order.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        order.setLoyaltyPointsUsed(rs.getInt("loyalty_points_used"));
        order.setLoyaltyDiscountAmount(rs.getBigDecimal("loyalty_discount_amount"));
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
