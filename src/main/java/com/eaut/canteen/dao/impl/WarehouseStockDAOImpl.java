package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.eaut.canteen.dao.WarehouseStockDAO;

public class WarehouseStockDAOImpl implements WarehouseStockDAO {

    private static final String INCREMENT =
            "UPDATE warehouse_stock SET quantity = quantity + ? WHERE product_id = ?";
    private static final String DECREMENT_IF_ENOUGH =
            "UPDATE warehouse_stock SET quantity = quantity - ? WHERE product_id = ? AND quantity >= ?";

    @Override
    public void increment(Connection conn, int productId, int quantity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INCREMENT)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    @Override
    public int decrementIfEnough(Connection conn, int productId, int quantity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DECREMENT_IF_ENOUGH)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            return ps.executeUpdate();
        }
    }
}
