package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.eaut.canteen.dao.ShelfStockDAO;

public class ShelfStockDAOImpl implements ShelfStockDAO {

    private static final String DECREMENT_IF_ENOUGH =
            "UPDATE shelf_stock SET quantity = quantity - ? WHERE product_id = ? AND quantity >= ?";
    private static final String INCREMENT =
            "UPDATE shelf_stock SET quantity = quantity + ? WHERE product_id = ?";

    @Override
    public int decrementIfEnough(Connection conn, int productId, int quantity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DECREMENT_IF_ENOUGH)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            return ps.executeUpdate();
        }
    }

    @Override
    public void increment(Connection conn, int productId, int quantity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INCREMENT)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }
}
