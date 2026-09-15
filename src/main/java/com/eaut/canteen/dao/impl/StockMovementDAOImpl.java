package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.StockMovementDAO;
import com.eaut.canteen.model.StockLocation;
import com.eaut.canteen.model.StockMovement;
import com.eaut.canteen.model.StockMovementReason;

public class StockMovementDAOImpl implements StockMovementDAO {

    private static final String INSERT =
            "INSERT INTO stock_movements (product_id, location, delta, reason, ref_order_id, note, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    // The joins are display-only: the ledger is read as a table of "who moved what, when", and
    // resolving names per row in Java would be one query per line.
    private static final String BASE_SELECT =
            "SELECT m.*, p.name AS product_name, u.full_name AS created_by_name, o.order_code AS ref_order_code " +
            "FROM stock_movements m " +
            "JOIN products p ON m.product_id = p.product_id " +
            "JOIN users u ON m.created_by = u.user_id " +
            "LEFT JOIN orders o ON m.ref_order_id = o.order_id ";

    @Override
    public void insert(Connection conn, int productId, StockLocation location, int delta,
                       StockMovementReason reason, Integer refOrderId, String note, int createdBy)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setInt(1, productId);
            ps.setString(2, location.name());
            ps.setInt(3, delta);
            ps.setString(4, reason.name());
            if (refOrderId == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, refOrderId);
            }
            ps.setString(6, note);
            ps.setInt(7, createdBy);
            ps.executeUpdate();
        }
    }

    /** Shared by the page query and the count so the two can never drift apart. */
    private String buildWhere(Integer productId, StockMovementReason reason,
                              LocalDateTime from, LocalDateTime to, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (productId != null) {
            where.append(" AND m.product_id = ?");
            params.add(productId);
        }
        if (reason != null) {
            where.append(" AND m.reason = ?");
            params.add(reason.name());
        }
        if (from != null) {
            where.append(" AND m.created_at >= ?");
            params.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            where.append(" AND m.created_at < ?");
            params.add(Timestamp.valueOf(to));
        }
        return where.toString();
    }

    private void bindAll(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    @Override
    public List<StockMovement> findFiltered(Connection conn, Integer productId, StockMovementReason reason,
                                            LocalDateTime from, LocalDateTime to, int limit, int offset)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = BASE_SELECT + buildWhere(productId, reason, from, to, params)
                + " ORDER BY m.created_at DESC, m.movement_id DESC LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);

        List<StockMovement> movements = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindAll(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movements.add(mapRow(rs));
                }
            }
        }
        return movements;
    }

    @Override
    public int countFiltered(Connection conn, Integer productId, StockMovementReason reason,
                             LocalDateTime from, LocalDateTime to) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM stock_movements m" + buildWhere(productId, reason, from, to, params);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindAll(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private StockMovement mapRow(ResultSet rs) throws SQLException {
        StockMovement movement = new StockMovement();
        movement.setMovementId(rs.getInt("movement_id"));
        movement.setProductId(rs.getInt("product_id"));
        movement.setLocation(StockLocation.valueOf(rs.getString("location")));
        movement.setDelta(rs.getInt("delta"));
        movement.setReason(StockMovementReason.valueOf(rs.getString("reason")));

        int refOrderId = rs.getInt("ref_order_id");
        movement.setRefOrderId(rs.wasNull() ? null : refOrderId);

        movement.setNote(rs.getString("note"));
        movement.setCreatedBy(rs.getInt("created_by"));
        movement.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        movement.setProductName(rs.getString("product_name"));
        movement.setCreatedByName(rs.getString("created_by_name"));
        movement.setRefOrderCode(rs.getString("ref_order_code"));
        return movement;
    }
}
