package com.eaut.canteen.model;

import java.math.BigDecimal;

/**
 * A row of the "biggest spenders" table on the dashboard.
 *
 * <p>Getters exist because JSTL/EL in Tomcat 10.1 cannot read record accessors.
 */
public record TopCustomer(int userId, String fullName, String studentId,
                          int orderCount, BigDecimal totalSpent, int loyaltyPoints) {

    public int getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getStudentId() {
        return studentId;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }
}
