package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.Product;

public interface ProductDAO {

    List<Product> findAllActive(Connection conn) throws SQLException;

    List<Product> findAllActiveByCategory(Connection conn, int categoryId) throws SQLException;

    Product findById(Connection conn, int productId) throws SQLException;

    /** All products regardless of active status, with both warehouse and shelf quantities — admin view. */
    List<Product> findAllForAdmin(Connection conn) throws SQLException;

    /** Inserts the product and its 1:1 warehouse_stock/shelf_stock rows (quantity 0). */
    void insert(Connection conn, Product product) throws SQLException;

    void update(Connection conn, Product product) throws SQLException;

    void updateImage(Connection conn, int productId, String imageFilename) throws SQLException;

    void setActive(Connection conn, int productId, boolean active) throws SQLException;
}
