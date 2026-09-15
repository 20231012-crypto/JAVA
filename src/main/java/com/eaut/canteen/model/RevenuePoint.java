package com.eaut.canteen.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Revenue and order count for a single day — one bar of the admin revenue chart.
 *
 * Days with no orders are included with zeros by the query that builds these (via a generated
 * date series), so the chart shows a real gap instead of silently closing it up and implying
 * business was continuous. Getters exist because JSTL/EL cannot read record accessors.
 */
public record RevenuePoint(LocalDate day, BigDecimal revenue, int orderCount) {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM");

    public LocalDate getDay() {
        return day;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public String getDayDisplay() {
        return day.format(DAY_FORMAT);
    }
}
