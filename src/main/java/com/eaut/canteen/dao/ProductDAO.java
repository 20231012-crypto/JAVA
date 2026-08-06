package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.Product;

public interface ProductDAO {

    List<Product> findAllActive(Connection conn) throws SQLException;

    List<Product> findAllActiveByCategory(Connection conn, int categoryId) throws SQLException;

    Product findById(Connection conn, int productId) throws SQLException;
}
