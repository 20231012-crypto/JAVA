package com.eaut.canteen.model;

import java.math.BigDecimal;

/**
 * A wallet whose stored balance disagrees with the sum of its own ledger.
 *
 * <p>users.wallet_balance is a denormalised running total and wallet_transactions is the ledger
 * behind it. Nothing in the database enforces that they agree — no constraint, no trigger — so the
 * only way to know is to compare them. A non-empty result here means some code path moved money
 * without writing its ledger row (or the reverse), which is exactly the class of bug that is
 * invisible until a student complains about their balance.
 *
 * <p>Getters exist because JSTL/EL in Tomcat 10.1 cannot read record accessors.
 */
public record WalletDrift(int userId, String fullName, String username, String studentId,
                          BigDecimal storedBalance, BigDecimal ledgerTotal) {

    public int getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getStudentId() {
        return studentId;
    }

    public BigDecimal getStoredBalance() {
        return storedBalance;
    }

    public BigDecimal getLedgerTotal() {
        return ledgerTotal;
    }

    /** Positive means the wallet holds more than the ledger can account for. */
    public BigDecimal getDifference() {
        return storedBalance.subtract(ledgerTotal);
    }
}
