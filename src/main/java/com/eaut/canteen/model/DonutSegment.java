package com.eaut.canteen.model;

import java.math.BigDecimal;

/**
 * One arc of the payment-mix donut, with its stroke geometry precomputed.
 *
 * <p>An SVG donut is a single circle whose dash pattern reveals one arc at a time: dashArray is
 * "&lt;arc length&gt; &lt;the rest of the circumference&gt;" and dashOffset rotates that arc into
 * place. Both are running totals over the preceding segments — exactly the kind of accumulation a
 * view should not be doing — so the servlet resolves them and the JSP prints the numbers.
 *
 * <p>Getters exist because JSTL/EL in Tomcat 10.1 cannot read record accessors.
 */
public record DonutSegment(String label, long count, BigDecimal amount, int percent,
                           String color, String dashArray, String dashOffset) {

    public String getLabel() {
        return label;
    }

    public long getCount() {
        return count;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getPercent() {
        return percent;
    }

    public String getColor() {
        return color;
    }

    public String getDashArray() {
        return dashArray;
    }

    public String getDashOffset() {
        return dashOffset;
    }
}
