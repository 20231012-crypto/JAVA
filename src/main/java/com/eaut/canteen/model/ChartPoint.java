package com.eaut.canteen.model;

import java.math.BigDecimal;

/**
 * One point on a line chart, with its position already scaled into the SVG's 0-100 viewBox.
 *
 * <p>The coordinates are computed in the servlet rather than in the view because EL cannot do the
 * arithmetic without turning the JSP into a calculator — and a JSP that calculates has stopped
 * being display-only. The view just prints {@code ${p.x},${p.y}} into a polyline.
 *
 * <p>Getters exist because JSTL/EL in Tomcat 10.1 (Jakarta EL 5.0) cannot read record accessors —
 * the same reason RevenuePoint carries them.
 *
 * @param label the x-axis caption ("09:00", "T2")
 * @param value the real figure, kept for the table fallback
 * @param x     0-100 across the plot
 * @param y     0-100 down the plot, already flipped so 0 is the top as SVG expects
 */
public record ChartPoint(String label, BigDecimal value, String x, String y) {

    public String getLabel() {
        return label;
    }

    public BigDecimal getValue() {
        return value;
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }
}
