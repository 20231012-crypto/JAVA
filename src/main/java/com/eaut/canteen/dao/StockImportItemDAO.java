package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.StockImportItem;

public interface StockImportItemDAO {

    void insert(Connection conn, StockImportItem item) throws SQLException;

    List<StockImportItem> findByImportId(Connection conn, int importId) throws SQLException;
}
