package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.eaut.canteen.dao.FavoriteDAO;
import com.eaut.canteen.model.FavoriteCountItem;

public class FavoriteDAOImpl implements FavoriteDAO {

    private static final String IS_FAVORITED =
            "SELECT 1 FROM favorites WHERE user_id = ? AND product_id = ?";
    private static final String ADD =
            "INSERT INTO favorites (user_id, product_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
    private static final String REMOVE =
            "DELETE FROM favorites WHERE user_id = ? AND product_id = ?";
    private static final String FIND_FAVORITED_IDS =
            "SELECT product_id FROM favorites WHERE user_id = ?";
    private static final String FIND_MOST_FAVORITED =
            "SELECT p.product_id, p.name, p.image_filename, COUNT(*) AS favorite_count " +
            "FROM favorites f JOIN products p ON f.product_id = p.product_id " +
            "GROUP BY p.product_id, p.name, p.image_filename " +
            "ORDER BY favorite_count DESC, p.name LIMIT ?";

    @Override
    public boolean isFavorited(Connection conn, int userId, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(IS_FAVORITED)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void add(Connection conn, int userId, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(ADD)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    @Override
    public void remove(Connection conn, int userId, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(REMOVE)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    @Override
    public Set<Integer> findFavoritedProductIds(Connection conn, int userId) throws SQLException {
        Set<Integer> ids = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_FAVORITED_IDS)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    @Override
    public List<FavoriteCountItem> findMostFavorited(Connection conn, int limit) throws SQLException {
        List<FavoriteCountItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_MOST_FAVORITED)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new FavoriteCountItem(
                            rs.getInt("product_id"),
                            rs.getString("name"),
                            rs.getString("image_filename"),
                            rs.getInt("favorite_count")));
                }
            }
        }
        return items;
    }
}
