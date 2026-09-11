package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.WalletTransaction;

/**
 * The auditable ledger backing users.wallet_balance — every top-up/payment/refund is its own row
 * here, so the running balance is always reconstructable instead of a number trusted blindly.
 * Callers are expected to also call UserDAO#adjustWalletBalance in the same transaction (see
 * CheckoutServlet, WalletServlet) — this DAO only appends to the ledger, it never touches the
 * balance column itself.
 */
public interface WalletDAO {

    int insert(Connection conn, WalletTransaction transaction) throws SQLException;

    List<WalletTransaction> findByUser(Connection conn, int userId) throws SQLException;
}
