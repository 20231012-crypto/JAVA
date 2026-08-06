package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.StockImport;

public interface StockImportDAO {

    int insert(Connection conn, StockImport stockImport) throws SQLException;

    List<StockImport> findAll(Connection conn) throws SQLException;
}
