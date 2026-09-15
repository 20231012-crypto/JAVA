package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.AccountStatus;
import com.eaut.canteen.model.CustomerSummary;
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

    /**
     * One page of customer accounts with what each has spent, for the admin customer screen.
     * Includes DISABLED accounts — an admin managing accounts has to be able to see the ones
     * they have locked. A null/blank search matches everyone; otherwise it matches name, username,
     * email or phone, ignoring case and Vietnamese diacritics.
     */
    List<CustomerSummary> findCustomers(Connection conn, String search, int limit, int offset) throws SQLException;

    /** Total matches for the same search as {@link #findCustomers}, for the page count. */
    int countCustomers(Connection conn, String search) throws SQLException;

    void updateStatus(Connection conn, int userId, AccountStatus status) throws SQLException;

    List<User> findByRole(Connection conn, int roleId) throws SQLException;

    /** Records a wallet top-up/payment/refund and adjusts users.wallet_balance in one call — see WalletDAO for the ledger read side. */
    void adjustWalletBalance(Connection conn, int userId, java.math.BigDecimal delta) throws SQLException;

    /** Adjusts users.loyalty_points — see LoyaltyDAO for the ledger read side. */
    void adjustLoyaltyPoints(Connection conn, int userId, int delta) throws SQLException;

    /** Sets a customer's contact phone — Google sign-up never collects one, so this is how the checkout phone-number gate (see AccountServlet) fills it in. */
    void updatePhone(Connection conn, int userId, String phone) throws SQLException;

    /** Sets MSSV/Khoa-lớp — collected via the same checkout info gate as phone, only for EAUT-student customers. */
    void updateStudentInfo(Connection conn, int userId, String studentId, String className) throws SQLException;

    /** Staff self-toggles their own "đang trực" status — see DutyServlet. */
    void setOnDuty(Connection conn, int userId, boolean onDuty) throws SQLException;

    /** Edits the fields an admin may change on a staff account. Username and auth provider are not
     *  among them: the username is how the account is identified in every audit row. */
    void updateStaffDetails(Connection conn, int userId, String fullName, String email,
                            String phone) throws SQLException;

    /** Moves an account to a different role. */
    void updateRole(Connection conn, int userId, int roleId) throws SQLException;

    /** Sets a new bcrypt hash. Callers must hash with PasswordUtil — never pass a plaintext. */
    void updatePassword(Connection conn, int userId, String passwordHash) throws SQLException;

    /** How many ACTIVE accounts hold a given permission, via their role. Used to refuse the change
     *  that would lock the last administrator out of /admin/roles. */
    int countActiveHoldersOfPermission(Connection conn, String permissionKey) throws SQLException;

    /** Every non-customer user currently on duty, for the "Nhân sự đang trực" roster on the Kanban boards. */
    List<User> findOnDutyStaff(Connection conn) throws SQLException;

    /**
     * Customers who have actually ordered since {@code from}. "Active" is defined as having placed
     * an order rather than merely having an account, because a registration count only ever goes
     * up and so tells a manager nothing.
     */
    int countActiveCustomers(Connection conn, java.time.LocalDateTime from) throws SQLException;

    /** Total EAUT Pay money held across every wallet — the float the canteen owes its students. */
    java.math.BigDecimal sumWalletFloat(Connection conn) throws SQLException;

    /** Biggest spenders since {@code from}, highest first. */
    List<com.eaut.canteen.model.TopCustomer> findTopCustomers(Connection conn,
            java.time.LocalDateTime from, int limit) throws SQLException;
}
