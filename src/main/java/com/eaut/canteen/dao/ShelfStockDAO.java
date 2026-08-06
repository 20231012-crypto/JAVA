package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;

public interface ShelfStockDAO {

    /**
     * Atomically decrements shelf stock only if enough is available.
     * @return affected row count — 0 means insufficient stock (caller must roll back).
     */
    int decrementIfEnough(Connection conn, int productId, int quantity) throws SQLException;

    void increment(Connection conn, int productId, int quantity) throws SQLException;
}
