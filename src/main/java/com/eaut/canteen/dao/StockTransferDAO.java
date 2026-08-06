package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.StockTransfer;

public interface StockTransferDAO {

    void insert(Connection conn, StockTransfer transfer) throws SQLException;

    List<StockTransfer> findRecent(Connection conn, int limit) throws SQLException;
}
