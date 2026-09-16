package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.eaut.canteen.dao.BannerDAO;
import com.eaut.canteen.model.Banner;

public class BannerDAOImpl implements BannerDAO {

    // image_content_type comes along on listing queries (it is a short varchar, unlike image_data)
    // because the view has to know whether to render an <img> or a <video> for this banner.
    private static final String BASE_SELECT =
            "SELECT banner_id, position, title, subtitle, link_url, sort_order, is_active, " +
            "start_at, end_at, " +
            "image_content_type, (image_data IS NOT NULL) AS has_image FROM banners ";
    // Visible means active AND inside its window. The window is AND-ed with is_active rather than
    // replacing it, so the manual switch still takes a banner down immediately regardless of dates,
    // and a banner with no dates behaves exactly as it did before scheduling existed.
    // end_at is exclusive so a banner set to end at midnight is gone the instant that date starts.
    // Same visibility rule as FIND_ACTIVE_BY_POSITION below, minus the position filter: one round
    // trip for the whole (small) table instead of one per position.
    private static final String FIND_ACTIVE_ALL_POSITIONS =
            BASE_SELECT + "WHERE is_active = TRUE " +
            "AND (start_at IS NULL OR start_at <= CURRENT_TIMESTAMP) " +
            "AND (end_at IS NULL OR end_at > CURRENT_TIMESTAMP) " +
            "ORDER BY position, sort_order, banner_id";

    private static final String FIND_ACTIVE_BY_POSITION =
            BASE_SELECT + "WHERE is_active = TRUE AND position = ? " +
            "AND (start_at IS NULL OR start_at <= CURRENT_TIMESTAMP) " +
            "AND (end_at IS NULL OR end_at > CURRENT_TIMESTAMP) " +
            "ORDER BY sort_order, banner_id";
    private static final String FIND_ALL_FOR_ADMIN =
            BASE_SELECT + "ORDER BY position, sort_order, banner_id";
    private static final String FIND_BY_ID =
            BASE_SELECT + "WHERE banner_id = ?";
    private static final String INSERT =
            "INSERT INTO banners (position, title, subtitle, link_url, sort_order, start_at, end_at, is_active) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)";
    private static final String UPDATE =
            "UPDATE banners SET position = ?, title = ?, subtitle = ?, link_url = ?, sort_order = ?, " +
            "start_at = ?, end_at = ? WHERE banner_id = ?";
    private static final String UPDATE_IMAGE =
            "UPDATE banners SET image_data = ?, image_content_type = ? WHERE banner_id = ?";
    private static final String SET_ACTIVE =
            "UPDATE banners SET is_active = ? WHERE banner_id = ?";
    private static final String DELETE =
            "DELETE FROM banners WHERE banner_id = ?";
    private static final String FIND_IMAGE_DATA =
            "SELECT image_data FROM banners WHERE banner_id = ?";
    private static final String FIND_IMAGE_CONTENT_TYPE =
            "SELECT image_content_type FROM banners WHERE banner_id = ?";

    @Override
    public List<Banner> findActiveByPosition(Connection conn, String position) throws SQLException {
        List<Banner> banners = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ACTIVE_BY_POSITION)) {
            ps.setString(1, position);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    banners.add(mapRow(rs));
                }
            }
        }
        return banners;
    }

    @Override
    public Map<String, List<Banner>> findActiveGroupedByPosition(Connection conn) throws SQLException {
        Map<String, List<Banner>> byPosition = new java.util.HashMap<>();
        // Seeded with every position so a caller can read one that has no banners without a
        // null check, and so the JSP's "is there a rail?" test stays a simple emptiness test.
        for (String position : new String[]{"HEAD", "FOOTER", "LEFT", "RIGHT"}) {
            byPosition.put(position, new ArrayList<>());
        }
        try (PreparedStatement ps = conn.prepareStatement(FIND_ACTIVE_ALL_POSITIONS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Banner banner = mapRow(rs);
                byPosition.computeIfAbsent(banner.getPosition(), key -> new ArrayList<>()).add(banner);
            }
        }
        return byPosition;
    }

    @Override
    public List<Banner> findAllForAdmin(Connection conn) throws SQLException {
        List<Banner> banners = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL_FOR_ADMIN);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                banners.add(mapRow(rs));
            }
        }
        return banners;
    }

    @Override
    public Banner findById(Connection conn, int bannerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, bannerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public void insert(Connection conn, Banner banner) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, banner.getPosition());
            ps.setString(2, banner.getTitle());
            ps.setString(3, banner.getSubtitle());
            ps.setString(4, banner.getLinkUrl());
            ps.setInt(5, banner.getSortOrder());
            setNullableTimestamp(ps, 6, banner.getStartAt());
            setNullableTimestamp(ps, 7, banner.getEndAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                banner.setBannerId(keys.getInt(1));
            }
        }
    }

    @Override
    public void update(Connection conn, Banner banner) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, banner.getPosition());
            ps.setString(2, banner.getTitle());
            ps.setString(3, banner.getSubtitle());
            ps.setString(4, banner.getLinkUrl());
            ps.setInt(5, banner.getSortOrder());
            setNullableTimestamp(ps, 6, banner.getStartAt());
            setNullableTimestamp(ps, 7, banner.getEndAt());
            ps.setInt(8, banner.getBannerId());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateImage(Connection conn, int bannerId, byte[] imageData, String contentType) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_IMAGE)) {
            if (imageData == null) {
                ps.setNull(1, Types.BINARY);
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setBytes(1, imageData);
                ps.setString(2, contentType);
            }
            ps.setInt(3, bannerId);
            ps.executeUpdate();
        }
    }

    @Override
    public void setActive(Connection conn, int bannerId, boolean active) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SET_ACTIVE)) {
            ps.setBoolean(1, active);
            ps.setInt(2, bannerId);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Connection conn, int bannerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setInt(1, bannerId);
            ps.executeUpdate();
        }
    }

    @Override
    public byte[] findImageData(Connection conn, int bannerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_IMAGE_DATA)) {
            ps.setInt(1, bannerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBytes(1) : null;
            }
        }
    }

    @Override
    public String findImageContentType(Connection conn, int bannerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_IMAGE_CONTENT_TYPE)) {
            ps.setInt(1, bannerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private Banner mapRow(ResultSet rs) throws SQLException {
        Banner banner = new Banner();
        banner.setBannerId(rs.getInt("banner_id"));
        banner.setPosition(rs.getString("position"));
        banner.setTitle(rs.getString("title"));
        banner.setSubtitle(rs.getString("subtitle"));
        banner.setLinkUrl(rs.getString("link_url"));
        banner.setSortOrder(rs.getInt("sort_order"));
        banner.setActive(rs.getBoolean("is_active"));
        if (rs.getTimestamp("start_at") != null) {
            banner.setStartAt(rs.getTimestamp("start_at").toLocalDateTime());
        }
        if (rs.getTimestamp("end_at") != null) {
            banner.setEndAt(rs.getTimestamp("end_at").toLocalDateTime());
        }
        banner.setHasImage(rs.getBoolean("has_image"));
        banner.setMediaContentType(rs.getString("image_content_type"));
        return banner;
    }

    /** A null window bound means "no limit", which has to reach the column as SQL NULL. */
    private void setNullableTimestamp(PreparedStatement ps, int index, java.time.LocalDateTime value)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            ps.setTimestamp(index, java.sql.Timestamp.valueOf(value));
        }
    }
}
