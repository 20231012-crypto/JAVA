package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.WalletTopupRequest;
import com.eaut.canteen.model.WalletTransaction;

/**
 * The auditable ledger backing users.wallet_balance — every top-up/payment/refund is its own row
 * here, so the running balance is always reconstructable instead of a number trusted blindly.
 * Callers are expected to also call UserDAO#adjustWalletBalance in the same transaction (see
 * CheckoutServlet, WalletServlet) — this DAO only appends to the ledger, it never touches the
 * balance column itself.
 *
 * <p>Also owns wallet_topup_requests — the customer-facing "nạp ví qua VietQR" self-service
 * request queue an admin confirms into an actual TOPUP transaction (see WalletTopupServlet,
 * admin WalletServlet).
 */
public interface WalletDAO {

    int insert(Connection conn, WalletTransaction transaction) throws SQLException;

    List<WalletTransaction> findByUser(Connection conn, int userId) throws SQLException;

    int insertTopupRequest(Connection conn, WalletTopupRequest request) throws SQLException;

    WalletTopupRequest findTopupRequestById(Connection conn, int requestId) throws SQLException;

    List<WalletTopupRequest> findTopupRequestsByUser(Connection conn, int userId) throws SQLException;

    /** For the admin confirmation queue — joins in each requester's name. */
    List<WalletTopupRequest> findPendingTopupRequests(Connection conn) throws SQLException;

    /** @return affected row count — 0 means the request was already resolved (confirmed/rejected) by someone else. */
    int updateTopupRequestStatus(Connection conn, int requestId, com.eaut.canteen.model.WalletTopupStatus status, int confirmedBy) throws SQLException;
}
