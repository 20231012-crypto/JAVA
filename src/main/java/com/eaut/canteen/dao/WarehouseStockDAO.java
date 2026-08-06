package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;

public interface WarehouseStockDAO {

    void increment(Connection conn, int productId, int quantity) throws SQLException;

    /**
     * Atomically decrements warehouse stock only if enough is available (used when store
     * staff transfers stock kho -> kệ).
     * @return affected row count — 0 means insufficient warehouse stock.
     */
    int decrementIfEnough(Connection conn, int productId, int quantity) throws SQLException;
}
