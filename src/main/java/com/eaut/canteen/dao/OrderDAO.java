package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

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
}
