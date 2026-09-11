package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.LoyaltyTransaction;

/**
 * The auditable ledger backing users.loyalty_points — every earn/redeem is its own row here, so
 * the running balance is always reconstructable. Callers must also call
 * UserDAO#adjustLoyaltyPoints in the same transaction (see OrderFulfillmentServlet,
 * CheckoutServlet) — this DAO only appends to the ledger, it never touches the balance column.
 */
public interface LoyaltyDAO {

    int insert(Connection conn, LoyaltyTransaction transaction) throws SQLException;

    List<LoyaltyTransaction> findByUser(Connection conn, int userId) throws SQLException;
}
