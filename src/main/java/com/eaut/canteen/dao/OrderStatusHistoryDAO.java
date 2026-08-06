package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.OrderStatusHistory;

public interface OrderStatusHistoryDAO {

    void insert(Connection conn, int orderId, OrderStatus oldStatus, OrderStatus newStatus, int changedBy, String note)
            throws SQLException;

    List<OrderStatusHistory> findByOrderId(Connection conn, int orderId) throws SQLException;
}
