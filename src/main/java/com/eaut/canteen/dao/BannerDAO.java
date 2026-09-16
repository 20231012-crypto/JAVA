package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.eaut.canteen.model.Banner;

public interface BannerDAO {

    /** Active banners for one position (HEAD/FOOTER/LEFT/RIGHT), ordered for display. */
    List<Banner> findActiveByPosition(Connection conn, String position) throws SQLException;

    /**
     * Every currently-visible banner, keyed by position. The catalog page needs all four positions
     * and used to ask for them one at a time — four round trips to fetch one small table.
     * Positions with no banner are present as empty lists, so callers never have to null-check.
     */
    Map<String, List<Banner>> findActiveGroupedByPosition(Connection conn) throws SQLException;

    List<Banner> findAllForAdmin(Connection conn) throws SQLException;

    Banner findById(Connection conn, int bannerId) throws SQLException;

    void insert(Connection conn, Banner banner) throws SQLException;

    void update(Connection conn, Banner banner) throws SQLException;

    void updateImage(Connection conn, int bannerId, byte[] imageData, String contentType) throws SQLException;

    void setActive(Connection conn, int bannerId, boolean active) throws SQLException;

    void delete(Connection conn, int bannerId) throws SQLException;

    /** @return {@code null} if the banner has no image set. */
    byte[] findImageData(Connection conn, int bannerId) throws SQLException;

    String findImageContentType(Connection conn, int bannerId) throws SQLException;
}
