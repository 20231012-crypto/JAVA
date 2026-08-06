package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.OrderItem;

public interface OrderItemDAO {

    void insert(Connection conn, OrderItem item) throws SQLException;

    List<OrderItem> findByOrderId(Connection conn, int orderId) throws SQLException;
}
