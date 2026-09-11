package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.WalletDAO;
import com.eaut.canteen.model.WalletTopupRequest;
import com.eaut.canteen.model.WalletTopupStatus;
import com.eaut.canteen.model.WalletTransaction;
import com.eaut.canteen.model.WalletTransactionType;

public class WalletDAOImpl implements WalletDAO {

    private static final String INSERT =
            "INSERT INTO wallet_transactions (user_id, amount, type, order_id, created_by, note) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String FIND_BY_USER =
            "SELECT * FROM wallet_transactions WHERE user_id = ? ORDER BY created_at DESC";

    private static final String INSERT_TOPUP_REQUEST =
            "INSERT INTO wallet_topup_requests (user_id, amount) VALUES (?, ?)";
    private static final String FIND_TOPUP_BY_ID =
            "SELECT * FROM wallet_topup_requests WHERE request_id = ?";
    private static final String FIND_TOPUP_BY_USER =
            "SELECT * FROM wallet_topup_requests WHERE user_id = ? ORDER BY created_at DESC";
    private static final String FIND_PENDING_TOPUPS =
            "SELECT t.*, u.full_name AS customer_name FROM wallet_topup_requests t " +
            "JOIN users u ON u.user_id = t.user_id WHERE t.status = 'PENDING' ORDER BY t.created_at";
    private static final String UPDATE_TOPUP_STATUS =
            "UPDATE wallet_topup_requests SET status = ?, confirmed_by = ?, confirmed_at = NOW() " +
            "WHERE request_id = ? AND status = 'PENDING'";

    @Override
    public int insert(Connection conn, WalletTransaction tx) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tx.getUserId());
            ps.setBigDecimal(2, tx.getAmount());
            ps.setString(3, tx.getType().name());
            setNullableInt(ps, 4, tx.getOrderId());
            setNullableInt(ps, 5, tx.getCreatedBy());
            ps.setString(6, tx.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    @Override
    public List<WalletTransaction> findByUser(Connection conn, int userId) throws SQLException {
        List<WalletTransaction> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_USER)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
        }
        return list;
    }

    @Override
    public int insertTopupRequest(Connection conn, WalletTopupRequest request) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_TOPUP_REQUEST, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, request.getUserId());
            ps.setBigDecimal(2, request.getAmount());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                request.setRequestId(id);
                return id;
            }
        }
    }

    @Override
    public WalletTopupRequest findTopupRequestById(Connection conn, int requestId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_TOPUP_BY_ID)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapTopupRequest(rs) : null;
            }
        }
    }

    @Override
    public List<WalletTopupRequest> findTopupRequestsByUser(Connection conn, int userId) throws SQLException {
        List<WalletTopupRequest> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_TOPUP_BY_USER)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTopupRequest(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<WalletTopupRequest> findPendingTopupRequests(Connection conn) throws SQLException {
        List<WalletTopupRequest> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_PENDING_TOPUPS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                WalletTopupRequest r = mapTopupRequest(rs);
                r.setCustomerName(rs.getString("customer_name"));
                list.add(r);
            }
        }
        return list;
    }

    @Override
    public int updateTopupRequestStatus(Connection conn, int requestId, WalletTopupStatus status, int confirmedBy) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_TOPUP_STATUS)) {
            ps.setString(1, status.name());
            ps.setInt(2, confirmedBy);
            ps.setInt(3, requestId);
            return ps.executeUpdate();
        }
    }

    private WalletTransaction mapTransaction(ResultSet rs) throws SQLException {
        WalletTransaction tx = new WalletTransaction();
        tx.setTransactionId(rs.getInt("transaction_id"));
        tx.setUserId(rs.getInt("user_id"));
        tx.setAmount(rs.getBigDecimal("amount"));
        tx.setType(WalletTransactionType.valueOf(rs.getString("type")));
        int orderId = rs.getInt("order_id");
        tx.setOrderId(rs.wasNull() ? null : orderId);
        int createdBy = rs.getInt("created_by");
        tx.setCreatedBy(rs.wasNull() ? null : createdBy);
        tx.setNote(rs.getString("note"));
        tx.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return tx;
    }

    private WalletTopupRequest mapTopupRequest(ResultSet rs) throws SQLException {
        WalletTopupRequest r = new WalletTopupRequest();
        r.setRequestId(rs.getInt("request_id"));
        r.setUserId(rs.getInt("user_id"));
        r.setAmount(rs.getBigDecimal("amount"));
        r.setStatus(WalletTopupStatus.valueOf(rs.getString("status")));
        int confirmedBy = rs.getInt("confirmed_by");
        r.setConfirmedBy(rs.wasNull() ? null : confirmedBy);
        if (rs.getTimestamp("confirmed_at") != null) {
            r.setConfirmedAt(rs.getTimestamp("confirmed_at").toLocalDateTime());
        }
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return r;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
