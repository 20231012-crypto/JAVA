package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.eaut.canteen.dao.FavoriteDAO;
import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.FavoriteDAOImpl;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.ChartPoint;
import com.eaut.canteen.model.DonutSegment;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.PaymentMethod;
import com.eaut.canteen.util.AppClock;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The admin overview: today's trading against yesterday's, the shape of the day, what students pay
 * with, and who is spending.
 *
 * <p>All the chart geometry is computed here rather than in the JSP. EL cannot do the arithmetic
 * without turning the view into a calculator, and a view that calculates has stopped being
 * display-only — so this class emits points already scaled into an SVG viewBox and arcs with their
 * stroke offsets resolved, and the JSP just prints them.
 *
 * <p>No chart library is loaded. The pages have never depended on a CDN and are not going to start;
 * a line is a polyline and a donut is a dashed circle.
 */
@WebServlet("/admin")
public class AdminDashboardServlet extends HttpServlet {

    /** The trading window worth plotting. Outside it every canteen reads zero and the line is flat. */
    private static final int CHART_START_HOUR = 6;
    private static final int CHART_END_HOUR = 21;

    /** Circumference of the donut's r=40 circle, to 2dp — the dash lengths are fractions of this. */
    private static final double DONUT_CIRCUMFERENCE = 251.33;

    /** From the validated categorical palette; see the dataviz palette reference. Order matters:
     *  it keeps orange and yellow from ever becoming adjacent arcs. */
    private static final String[] SERIES_COLORS = {"#2a78d6", "#eb6834", "#1baf7a", "#eda100"};

    private static final String[] WEEKDAY_LABELS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private static final OrderDAO orderDAO = new OrderDAOImpl();
    private static final ProductDAO productDAO = new ProductDAOImpl();
    private static final UserDAO userDAO = new UserDAOImpl();
    private static final FavoriteDAO favoriteDAO = new FavoriteDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            LocalDateTime startOfToday = AppClock.startOfToday(conn);
            LocalDateTime endOfToday = AppClock.endOfToday(conn);
            LocalDateTime startOfWeek = AppClock.startOfThisWeek(conn);
            LocalDateTime startOfMonth = AppClock.startOfThisMonth(conn);

            putRevenueWithTrends(req, conn, startOfToday, startOfWeek, startOfMonth, endOfToday);

            Map<String, Integer> outcomes = orderDAO.orderOutcomeCounts(conn, startOfMonth, endOfToday);
            req.setAttribute("completedThisMonth", outcomes.getOrDefault("completed", 0));
            req.setAttribute("cancelledThisMonth", outcomes.getOrDefault("cancelled", 0));

            req.setAttribute("activeCustomers", userDAO.countActiveCustomers(conn, startOfToday.minusDays(30)));
            req.setAttribute("walletFloat", userDAO.sumWalletFloat(conn));

            // Live operational counts — deliberately not date-bounded: "how many orders are
            // waiting right now" has nothing to do with which day they were placed. One grouped
            // query rather than three, for the same reason as the revenue windows above.
            Map<String, Integer> byStatus = orderDAO.countsByStatus(conn);
            req.setAttribute("pendingCount", byStatus.getOrDefault(OrderStatus.PENDING.name(), 0));
            req.setAttribute("confirmedCount", byStatus.getOrDefault(OrderStatus.CONFIRMED.name(), 0));
            req.setAttribute("shippingCount", byStatus.getOrDefault(OrderStatus.SHIPPING.name(), 0));

            req.setAttribute("hourPoints", hourSeries(conn, startOfToday, endOfToday));
            req.setAttribute("weekdayPoints", weekdaySeries(conn, startOfWeek.minusDays(21), endOfToday));
            req.setAttribute("paymentSegments", paymentDonut(conn, startOfMonth, endOfToday));

            req.setAttribute("bestSellers", productDAO.findBestSellers(conn, 30, 5));
            req.setAttribute("topCustomers", userDAO.findTopCustomers(conn, startOfMonth, 5));
            req.setAttribute("lowStock", productDAO.findLowStock(conn));
            req.setAttribute("mostFavorited", favoriteDAO.findMostFavorited(conn, 5));

            req.setAttribute("pageTitle", "Tổng quan");
            req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /**
     * The three revenue figures and how each compares with the same length of time immediately
     * before it. The comparison is what makes the number mean anything; 2.4 million đồng on its own
     * says nothing about whether trade is up.
     *
     * <p>All six come back from one statement. Asked separately they were six round trips to a
     * database on the other side of the world, for six numbers it computes in microseconds.
     */
    private void putRevenueWithTrends(HttpServletRequest req, Connection conn,
                                      LocalDateTime startOfToday, LocalDateTime startOfWeek,
                                      LocalDateTime startOfMonth, LocalDateTime now) throws SQLException {
        String[] keys = {"Today", "Week", "Month"};
        LocalDateTime[] starts = {startOfToday, startOfWeek, startOfMonth};

        // Current window then its predecessor, for each of the three, in a fixed order the reader
        // below relies on.
        List<LocalDateTime[]> windows = new ArrayList<>();
        for (LocalDateTime start : starts) {
            windows.add(new LocalDateTime[]{start, now});
            windows.add(new LocalDateTime[]{AppClock.previousWindowStart(start, now), start});
        }

        List<BigDecimal> sums = orderDAO.sumRevenueForWindows(conn, windows);
        for (int i = 0; i < keys.length; i++) {
            BigDecimal current = sums.get(i * 2);
            BigDecimal previous = sums.get(i * 2 + 1);
            req.setAttribute("revenue" + keys[i], current);
            req.setAttribute("revenue" + keys[i] + "Trend", percentChange(previous, current));
        }
    }

    /**
     * @return the percent change, or null when there is no previous figure to compare against —
     *         "up from zero" is not a percentage, and rendering it as +100% would be a fiction.
     */
    private Integer percentChange(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    /** Revenue through the trading day, one point per hour, scaled into a 0-100 viewBox. */
    private List<ChartPoint> hourSeries(Connection conn, LocalDateTime from, LocalDateTime to)
            throws SQLException {
        Map<Integer, BigDecimal> byHour = orderDAO.revenueByHour(conn, from, to);
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        for (int hour = CHART_START_HOUR; hour <= CHART_END_HOUR; hour++) {
            labels.add(String.format("%02d:00", hour));
            // Absent means no trade, which is a real zero — skipping the hour would close the gap
            // and imply the canteen was busy straight through.
            values.add(byHour.getOrDefault(hour, BigDecimal.ZERO));
        }
        return scale(labels, values);
    }

    private List<ChartPoint> weekdaySeries(Connection conn, LocalDateTime from, LocalDateTime to)
            throws SQLException {
        Map<Integer, BigDecimal> byDow = orderDAO.revenueByWeekday(conn, from, to);
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        for (int dow = 1; dow <= 7; dow++) {
            labels.add(WEEKDAY_LABELS[dow - 1]);
            values.add(byDow.getOrDefault(dow, BigDecimal.ZERO));
        }
        return scale(labels, values);
    }

    /**
     * Maps values onto the 0-100 viewBox. The y axis starts at zero rather than at the smallest
     * value: this is revenue, and a truncated baseline exaggerates every wobble into a cliff.
     */
    private List<ChartPoint> scale(List<String> labels, List<BigDecimal> values) {
        BigDecimal max = values.stream().reduce(BigDecimal.ZERO, (a, b) -> a.max(b));
        List<ChartPoint> points = new ArrayList<>();
        int lastIndex = Math.max(1, values.size() - 1);
        for (int i = 0; i < values.size(); i++) {
            double x = i * 100.0 / lastIndex;
            double y = max.signum() == 0 ? 100.0
                    : 100.0 - values.get(i).multiply(BigDecimal.valueOf(100))
                            .divide(max, 2, RoundingMode.HALF_UP).doubleValue();
            points.add(new ChartPoint(labels.get(i), values.get(i),
                    trim(x), trim(y)));
        }
        return points;
    }

    /** Two decimals is plenty of precision for a 100-unit viewBox, and keeps the markup readable. */
    private String trim(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * The payment mix as donut arcs. An SVG donut is one circle whose dash pattern reveals a single
     * arc; each arc needs the running total of everything before it, so the sum happens here.
     */
    private List<DonutSegment> paymentDonut(Connection conn, LocalDateTime from, LocalDateTime to)
            throws SQLException {
        List<Object[]> rows = orderDAO.paymentMethodMix(conn, from, to);
        long totalCount = rows.stream().mapToLong(r -> (Long) r[1]).sum();
        if (totalCount == 0) {
            return List.of();
        }

        List<DonutSegment> segments = new ArrayList<>();
        double consumed = 0;
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            long count = (Long) row[1];
            double share = count / (double) totalCount;
            double arc = share * DONUT_CIRCUMFERENCE;

            // A 2px gap between arcs, per the mark spec: touching fills of similar lightness read
            // as one shape, and the gap is also what keeps the colour-blind separation legible.
            double drawn = Math.max(0, arc - 2);
            segments.add(new DonutSegment(
                    displayName(row[0].toString()),
                    count,
                    (BigDecimal) row[2],
                    (int) Math.round(share * 100),
                    SERIES_COLORS[i % SERIES_COLORS.length],
                    trim(drawn) + " " + trim(DONUT_CIRCUMFERENCE - drawn),
                    trim(-consumed)));
            consumed += arc;
        }
        return segments;
    }

    private String displayName(String paymentMethod) {
        try {
            return PaymentMethod.valueOf(paymentMethod).getDisplayName();
        } catch (IllegalArgumentException e) {
            return paymentMethod;
        }
    }
}
