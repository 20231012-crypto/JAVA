package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.Order;

public interface OrderDAO {

    int insert(Connection conn, Order order) throws SQLException;

    Order findById(Connection conn, int orderId) throws SQLException;

    List<Order> findByCustomer(Connection conn, int customerId) throws SQLException;
}
