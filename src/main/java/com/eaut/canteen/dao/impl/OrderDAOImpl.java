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
import com.eaut.canteen.model.OrderFilter;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.PaymentMethod;
import com.eaut.canteen.model.PaymentStatus;
import com.eaut.canteen.model.RevenuePoint;

public class OrderDAOImpl implements OrderDAO {

    private static final String BASE_SELECT =
            "SELECT o.*, b.name AS building_name, " +
            "u.full_name AS customer_name, u.phone AS customer_phone, " +
            "u.student_id AS customer_student_id, u.class_name AS customer_class_name " +
            "FROM orders o " +
            "LEFT JOIN buildings b ON o.building_id = b.building_id " +
            "LEFT JOIN users u ON o.customer_id = u.user_id ";

    private static final String NEXT_TICKET_NO = "SELECT nextval('order_ticket_seq')";

    private static final String INSERT =
            "INSERT INTO orders (order_code, customer_id, building_id, channel, sold_by, subtotal, shipping_fee, " +
            "discount_amount, wallet_discount_amount, loyalty_points_used, loyalty_discount_amount, " +
            "total_amount, order_status, payment_method, payment_status, note) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
    // generate_series gives every day in the window a row, so a day with no trade shows as a real
    // zero on the chart instead of being skipped and making the gap invisible.
    private static final String REVENUE_BY_DAY =
            "SELECT d.day::date AS day, " +
            "COALESCE(SUM(o.total_amount) FILTER (WHERE o.order_status = 'COMPLETED'), 0) AS revenue, " +
            "COUNT(o.order_id) FILTER (WHERE o.order_status = 'COMPLETED') AS order_count " +
            "FROM generate_series(CURRENT_DATE - make_interval(days => ?), CURRENT_DATE, INTERVAL '1 day') AS d(day) " +
            "LEFT JOIN orders o ON DATE(o.created_at) = d.day::date " +
            "GROUP BY d.day ORDER BY d.day";
    private static final String SUM_REVENUE_THIS_MONTH =
            "SELECT COALESCE(SUM(total_amount), 0) FROM orders " +
            "WHERE order_status = 'COMPLETED' AND created_at >= date_trunc('month', CURRENT_DATE)";
    // The double-refund guard: only matches while the order has never been refunded, so of two
    // simultaneous clicks exactly one gets a row back and the other rolls back. See OrderDAO.
    private static final String MARK_REFUNDED =
            "UPDATE orders SET refunded_amount = ?, refunded_at = CURRENT_TIMESTAMP, refunded_by = ? " +
            "WHERE order_id = ? AND refunded_at IS NULL";
    // Read the points actually granted rather than recomputing them: the earn rate is a setting
    // now, so the rate in force today need not be the one this order was rewarded under.
    private static final String SUM_LOYALTY_AWARDED =
            "SELECT COALESCE(SUM(points), 0) FROM loyalty_transactions " +
            "WHERE order_id = ? AND type = 'EARN'";

    // --- Analytics. All bounded by explicit parameters, never CURRENT_DATE: the reporting day
    // must be the canteen's, not whatever zone the database happens to run in.
    private static final String SUM_REVENUE_BETWEEN =
            "SELECT COALESCE(SUM(total_amount), 0) FROM orders " +
            "WHERE order_status = 'COMPLETED' AND created_at >= ? AND created_at < ?";
    // One pass with FILTER rather than two COUNT queries — the two figures are always read
    // together and would otherwise be able to disagree across a concurrent status change.
    private static final String OUTCOME_COUNTS =
            "SELECT COUNT(*) FILTER (WHERE order_status = 'COMPLETED') AS completed, " +
            "COUNT(*) FILTER (WHERE order_status IN ('CANCELLED', 'REJECTED')) AS cancelled " +
            "FROM orders WHERE created_at >= ? AND created_at < ?";
    private static final String REVENUE_BY_HOUR =
            "SELECT EXTRACT(HOUR FROM created_at)::int AS h, COALESCE(SUM(total_amount), 0) AS revenue " +
            "FROM orders WHERE order_status = 'COMPLETED' AND created_at >= ? AND created_at < ? " +
            "GROUP BY h ORDER BY h";
    private static final String REVENUE_BY_WEEKDAY =
            "SELECT EXTRACT(ISODOW FROM created_at)::int AS dow, COALESCE(SUM(total_amount), 0) AS revenue " +
            "FROM orders WHERE order_status = 'COMPLETED' AND created_at >= ? AND created_at < ? " +
            "GROUP BY dow ORDER BY dow";
    private static final String PAYMENT_MIX =
            "SELECT payment_method, COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS revenue " +
            "FROM orders WHERE order_status = 'COMPLETED' AND created_at >= ? AND created_at < ? " +
            "GROUP BY payment_method ORDER BY cnt DESC";

    private static final String COUNTS_BY_STATUS =
            "SELECT order_status, COUNT(*) AS total FROM orders GROUP BY order_status";

    private static final String COUNT_BY_HOUR_TODAY =
            "SELECT EXTRACT(HOUR FROM created_at) AS hour_of_day, COUNT(*) FROM orders " +
            "WHERE DATE(created_at) = CURRENT_DATE GROUP BY hour_of_day ORDER BY hour_of_day";
    private static final String SET_ESTIMATED_READY_AT =
            "UPDATE orders SET estimated_ready_at = ? WHERE order_id = ?";
    private static final String FIND_RECENT_ACTIVITY =
            "SELECT p.name AS product_name, b.name AS building_name, o.created_at " +
            "FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.order_id " +
            "JOIN products p ON oi.product_id = p.product_id " +
            "LEFT JOIN buildings b ON o.building_id = b.building_id " +
            "WHERE o.order_status NOT IN ('CANCELLED', 'REJECTED') " +
            "ORDER BY o.created_at DESC LIMIT ?";

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
            ps.setBigDecimal(9, order.getWalletDiscountAmount());
            ps.setInt(10, order.getLoyaltyPointsUsed());
            ps.setBigDecimal(11, order.getLoyaltyDiscountAmount());
            ps.setBigDecimal(12, order.getTotalAmount());
            ps.setString(13, order.getOrderStatus().name());
            ps.setString(14, order.getPaymentMethod().name());
            ps.setString(15, order.getPaymentStatus().name());
            ps.setString(16, order.getNote());
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
    public List<RevenuePoint> revenueByDay(Connection conn, int days) throws SQLException {
        List<RevenuePoint> points = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(REVENUE_BY_DAY)) {
            // days - 1: the series is inclusive at both ends, so "7 days" means today plus six back.
            ps.setInt(1, Math.max(0, days - 1));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    points.add(new RevenuePoint(
                            rs.getDate("day").toLocalDate(),
                            rs.getBigDecimal("revenue"),
                            rs.getInt("order_count")));
                }
            }
        }
        return points;
    }

    @Override
    public BigDecimal sumRevenueThisMonth(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SUM_REVENUE_THIS_MONTH);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getBigDecimal(1);
        }
    }

    @Override
    public Map<String, Integer> countsByStatus(Connection conn) throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(COUNTS_BY_STATUS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("order_status"), rs.getInt("total"));
            }
        }
        return counts;
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

    @Override
    public void setEstimatedReadyAt(Connection conn, int orderId, java.time.LocalDateTime estimatedReadyAt) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SET_ESTIMATED_READY_AT)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(estimatedReadyAt));
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<com.eaut.canteen.model.RecentActivityItem> findRecentActivity(Connection conn, int limit) throws SQLException {
        List<com.eaut.canteen.model.RecentActivityItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_RECENT_ACTIVITY)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.time.LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                    int minutesAgo = (int) java.time.Duration.between(createdAt, java.time.LocalDateTime.now()).toMinutes();
                    String buildingName = rs.getString("building_name");
                    items.add(new com.eaut.canteen.model.RecentActivityItem(
                            rs.getString("product_name"),
                            buildingName != null ? buildingName : "quầy",
                            Math.max(0, minutesAgo)));
                }
            }
        }
        return items;
    }

    // ---- Admin order management ---------------------------------------------------------------

    /**
     * Builds the WHERE for one OrderFilter, appending a placeholder per condition and the matching
     * value to {@code params} in the same order. Only ever concatenates fixed SQL fragments — every
     * value the user supplied travels as a bind parameter, so a filter can narrow the query but
     * can never change its shape.
     */
    private String buildFilterWhere(OrderFilter filter, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (filter.status() != null) {
            where.append(" AND o.order_status = ?");
            params.add(filter.status().name());
        }
        if (filter.from() != null) {
            where.append(" AND o.created_at >= ?");
            params.add(java.sql.Timestamp.valueOf(filter.from()));
        }
        if (filter.to() != null) {
            // Exclusive, matching the half-open bounds AppClock produces.
            where.append(" AND o.created_at < ?");
            params.add(java.sql.Timestamp.valueOf(filter.to()));
        }
        if (filter.paymentMethod() != null) {
            where.append(" AND o.payment_method = ?");
            params.add(filter.paymentMethod().name());
        }
        if (filter.channel() != null) {
            where.append(" AND o.channel = ?");
            params.add(filter.channel().name());
        }
        if (filter.hasQuery()) {
            // unaccent on both sides so "hoa" finds "Hòa" — the same treatment ProductDAO.search
            // gives the menu, since staff type order lookups without diacritics too.
            where.append(" AND (unaccent(o.order_code) ILIKE unaccent(?)")
                 .append(" OR unaccent(u.full_name) ILIKE unaccent(?)")
                 .append(" OR u.phone ILIKE ?")
                 .append(" OR u.student_id ILIKE ?)");
            String like = "%" + filter.query().trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        return where.toString();
    }

    private void bindAll(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    @Override
    public List<Order> findFiltered(Connection conn, OrderFilter filter, int limit, int offset) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = BASE_SELECT + buildFilterWhere(filter, params)
                + " ORDER BY o.created_at DESC LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);

        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindAll(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
        }
        return orders;
    }

    @Override
    public int countFiltered(Connection conn, OrderFilter filter) throws SQLException {
        List<Object> params = new ArrayList<>();
        // Same FROM/JOIN as BASE_SELECT because the free-text filter reaches into users; counting
        // off a narrower FROM would break the moment someone searches by student id.
        String sql = "SELECT COUNT(*) FROM orders o "
                + "LEFT JOIN buildings b ON o.building_id = b.building_id "
                + "LEFT JOIN users u ON o.customer_id = u.user_id"
                + buildFilterWhere(filter, params);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindAll(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public int markRefunded(Connection conn, int orderId, BigDecimal amount, int refundedBy) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(MARK_REFUNDED)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, refundedBy);
            ps.setInt(3, orderId);
            return ps.executeUpdate();
        }
    }

    @Override
    public int sumLoyaltyPointsAwarded(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SUM_LOYALTY_AWARDED)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ---- Analytics ------------------------------------------------------------------------

    private void bindRange(PreparedStatement ps, java.time.LocalDateTime from, java.time.LocalDateTime to)
            throws SQLException {
        ps.setTimestamp(1, java.sql.Timestamp.valueOf(from));
        ps.setTimestamp(2, java.sql.Timestamp.valueOf(to));
    }

    @Override
    public BigDecimal sumRevenueBetween(Connection conn, java.time.LocalDateTime from,
                                        java.time.LocalDateTime to) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SUM_REVENUE_BETWEEN)) {
            bindRange(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    @Override
    public List<BigDecimal> sumRevenueForWindows(Connection conn,
            java.util.List<java.time.LocalDateTime[]> windows) throws SQLException {
        if (windows.isEmpty()) {
            return List.of();
        }

        // One SUM ... FILTER per window, so N windows come back as N columns of one row. The WHERE
        // narrows to the widest window first, which keeps the scan to the rows that can contribute
        // to any of them.
        StringBuilder sql = new StringBuilder("SELECT ");
        java.time.LocalDateTime earliest = windows.get(0)[0];
        java.time.LocalDateTime latest = windows.get(0)[1];
        for (int i = 0; i < windows.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("COALESCE(SUM(total_amount) FILTER (WHERE created_at >= ? AND created_at < ?), 0) AS w")
               .append(i);
            if (windows.get(i)[0].isBefore(earliest)) {
                earliest = windows.get(i)[0];
            }
            if (windows.get(i)[1].isAfter(latest)) {
                latest = windows.get(i)[1];
            }
        }
        sql.append(" FROM orders WHERE order_status = 'COMPLETED' AND created_at >= ? AND created_at < ?");

        List<BigDecimal> sums = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int index = 1;
            for (java.time.LocalDateTime[] window : windows) {
                ps.setTimestamp(index++, java.sql.Timestamp.valueOf(window[0]));
                ps.setTimestamp(index++, java.sql.Timestamp.valueOf(window[1]));
            }
            ps.setTimestamp(index++, java.sql.Timestamp.valueOf(earliest));
            ps.setTimestamp(index, java.sql.Timestamp.valueOf(latest));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                for (int i = 0; i < windows.size(); i++) {
                    sums.add(rs.getBigDecimal("w" + i));
                }
            }
        }
        return sums;
    }

    @Override
    public Map<String, Integer> orderOutcomeCounts(Connection conn, java.time.LocalDateTime from,
                                                   java.time.LocalDateTime to) throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(OUTCOME_COUNTS)) {
            bindRange(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    counts.put("completed", rs.getInt("completed"));
                    counts.put("cancelled", rs.getInt("cancelled"));
                }
            }
        }
        return counts;
    }

    private Map<Integer, BigDecimal> bucketedRevenue(Connection conn, String sql, String keyColumn,
                                                     java.time.LocalDateTime from, java.time.LocalDateTime to)
            throws SQLException {
        Map<Integer, BigDecimal> buckets = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindRange(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    buckets.put(rs.getInt(keyColumn), rs.getBigDecimal("revenue"));
                }
            }
        }
        return buckets;
    }

    @Override
    public Map<Integer, BigDecimal> revenueByHour(Connection conn, java.time.LocalDateTime from,
                                                  java.time.LocalDateTime to) throws SQLException {
        return bucketedRevenue(conn, REVENUE_BY_HOUR, "h", from, to);
    }

    @Override
    public Map<Integer, BigDecimal> revenueByWeekday(Connection conn, java.time.LocalDateTime from,
                                                     java.time.LocalDateTime to) throws SQLException {
        return bucketedRevenue(conn, REVENUE_BY_WEEKDAY, "dow", from, to);
    }

    @Override
    public List<Object[]> paymentMethodMix(Connection conn, java.time.LocalDateTime from,
                                           java.time.LocalDateTime to) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(PAYMENT_MIX)) {
            bindRange(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{rs.getString("payment_method"), rs.getLong("cnt"), rs.getBigDecimal("revenue")});
                }
            }
        }
        return rows;
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
        order.setCustomerName(rs.getString("customer_name"));
        order.setCustomerPhone(rs.getString("customer_phone"));
        order.setCustomerStudentId(rs.getString("customer_student_id"));
        order.setCustomerClassName(rs.getString("customer_class_name"));

        order.setChannel(OrderChannel.valueOf(rs.getString("channel")));

        int soldBy = rs.getInt("sold_by");
        order.setSoldBy(rs.wasNull() ? null : soldBy);

        order.setSubtotal(rs.getBigDecimal("subtotal"));
        order.setShippingFee(rs.getBigDecimal("shipping_fee"));
        order.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        order.setWalletDiscountAmount(rs.getBigDecimal("wallet_discount_amount"));
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
        if (rs.getTimestamp("estimated_ready_at") != null) {
            order.setEstimatedReadyAt(rs.getTimestamp("estimated_ready_at").toLocalDateTime());
        }

        order.setNote(rs.getString("note"));

        order.setRefundedAmount(rs.getBigDecimal("refunded_amount"));
        if (rs.getTimestamp("refunded_at") != null) {
            order.setRefundedAt(rs.getTimestamp("refunded_at").toLocalDateTime());
        }
        int refundedBy = rs.getInt("refunded_by");
        order.setRefundedBy(rs.wasNull() ? null : refundedBy);

        order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        order.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return order;
    }
}
