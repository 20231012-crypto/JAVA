package com.eaut.canteen.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One row of the admin customer list: the account plus what it has actually spent.
 *
 * Kept separate from User rather than adding transient fields to it, because these three numbers
 * only exist for this one screen — every other User in the app (sessions, staff management, order
 * lookups) would carry dead weight for them. The getters exist because JSTL/EL cannot read record
 * accessors.
 */
public record CustomerSummary(
        User user,
        int orderCount,
        BigDecimal totalSpent,
        LocalDateTime lastOrderAt) {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public User getUser() {
        return user;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public LocalDateTime getLastOrderAt() {
        return lastOrderAt;
    }

    /** Empty rather than "null" for a customer who has never ordered. */
    public String getLastOrderAtDisplay() {
        return lastOrderAt == null ? "" : lastOrderAt.format(DISPLAY_FORMAT);
    }
}
