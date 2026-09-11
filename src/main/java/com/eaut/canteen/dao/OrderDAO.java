package com.eaut.canteen.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderStatus;

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

    /** Order count per hour-of-day (0-23) for today, only hours with at least one order — backs the admin dashboard's peak-hour chart. */
    Map<Integer, Integer> countOrdersByHourToday(Connection conn) throws SQLException;
}
