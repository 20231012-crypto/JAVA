package com.eaut.canteen.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.RecentActivityItem;
import com.eaut.canteen.model.RevenuePoint;

public interface OrderDAO {

    int insert(Connection conn, Order order) throws SQLException;

    Order findById(Connection conn, int orderId) throws SQLException;

    List<Order> findByCustomer(Connection conn, int customerId) throws SQLException;

    List<Order> findByStatus(Connection conn, OrderStatus status) throws SQLException;

    List<Order> findAll(Connection conn) throws SQLException;

    /**
     * Guarded transition: only applies if the order is still in expectedCurrent.
     * @return affected row count — 0 means the order's status already changed under the caller.
     */
    int updateStatus(Connection conn, int orderId, OrderStatus expectedCurrent, OrderStatus newStatus) throws SQLException;

    /**
     * Guarded UNPAID -> PAID transition.
     * @return affected row count — 0 means the order was already paid.
     */
    int markPaid(Connection conn, int orderId, int confirmedBy) throws SQLException;

    /** Sum of total_amount for COMPLETED orders created today — for the admin dashboard's revenue tile. */
    BigDecimal sumRevenueToday(Connection conn) throws SQLException;

    /** How many orders are currently in this status, regardless of date — for "in progress right now" tiles. */
    int countByStatus(Connection conn, OrderStatus status) throws SQLException;

    /**
     * Revenue and order count per day for the last {@code days} days, oldest first. Days with no
     * orders come back as zero rows rather than being omitted, so a chart of this does not quietly
     * close the gap and imply trade was continuous.
     */
    List<RevenuePoint> revenueByDay(Connection conn, int days) throws SQLException;

    /** Completed-order revenue for the current calendar month. */
    BigDecimal sumRevenueThisMonth(Connection conn) throws SQLException;

    /** Order counts keyed by status name, in one query instead of one call per status. */
    Map<String, Integer> countsByStatus(Connection conn) throws SQLException;

    /** Order count per hour-of-day (0-23) for today, only hours with at least one order — backs the admin dashboard's peak-hour chart. */
    Map<Integer, Integer> countOrdersByHourToday(Connection conn) throws SQLException;

    /** Set when an order is confirmed (see OrderActionServlet) — drives the KDS countdown timer on the sales Kanban board. */
    void setEstimatedReadyAt(Connection conn, int orderId, java.time.LocalDateTime estimatedReadyAt) throws SQLException;

    /**
     * The most recent real order items placed (any status past PENDING), newest first — backs
     * the catalog page's "vừa có người đặt món này" toast. Not fabricated: reads actual order_items.
     */
    List<RecentActivityItem> findRecentActivity(Connection conn, int limit) throws SQLException;
}
