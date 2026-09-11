package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.AccountStatus;
import com.eaut.canteen.model.User;

public interface UserDAO {

    User findById(Connection conn, int userId) throws SQLException;

    User findByUsername(Connection conn, String username) throws SQLException;

    User findByEmail(Connection conn, String email) throws SQLException;

    User findByGoogleSub(Connection conn, String googleSub) throws SQLException;

    boolean existsByUsername(Connection conn, String username) throws SQLException;

    boolean existsByEmail(Connection conn, String email) throws SQLException;

    int insert(Connection conn, User user) throws SQLException;

    /** Links an existing LOCAL account to a Google identity on first Google sign-in (email match). */
    void linkGoogleAccount(Connection conn, int userId, String googleSub) throws SQLException;

    /** Every non-customer role, for the admin staff-management screen. */
    List<User> findAllStaff(Connection conn) throws SQLException;

    void updateStatus(Connection conn, int userId, AccountStatus status) throws SQLException;

    List<User> findByRole(Connection conn, int roleId) throws SQLException;

    /** Records a wallet top-up/payment/refund and adjusts users.wallet_balance in one call — see WalletDAO for the ledger read side. */
    void adjustWalletBalance(Connection conn, int userId, java.math.BigDecimal delta) throws SQLException;
}
