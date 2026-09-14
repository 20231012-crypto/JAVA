package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.eaut.canteen.model.FavoriteCountItem;

public interface FavoriteDAO {

    boolean isFavorited(Connection conn, int userId, int productId) throws SQLException;

    void add(Connection conn, int userId, int productId) throws SQLException;

    void remove(Connection conn, int userId, int productId) throws SQLException;

    /** All product IDs this user has favorited — used to mark ♥ state across a product listing in one query. */
    Set<Integer> findFavoritedProductIds(Connection conn, int userId) throws SQLException;

    /** Real counts, most-favorited first — backs the admin report. */
    List<FavoriteCountItem> findMostFavorited(Connection conn, int limit) throws SQLException;
}
