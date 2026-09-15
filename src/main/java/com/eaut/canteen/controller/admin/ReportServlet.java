package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.model.RevenuePoint;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Revenue reporting (reports.view). The /admin/reports path and its permission already existed in
 * SecurityFilter but had no servlet behind them, so this fills in the screen they were pointing at.
 */
@WebServlet("/admin/reports")
public class ReportServlet extends HttpServlet {

    private static final int DEFAULT_DAYS = 14;
    private static final int MIN_DAYS = 7;
    private static final int MAX_DAYS = 90;

    private final OrderDAO orderDAO = new OrderDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int days = RequestParams.intInRange(req.getParameter("days"), DEFAULT_DAYS, MIN_DAYS, MAX_DAYS);

        try (Connection conn = DBConnection.getConnection()) {
            List<RevenuePoint> series = orderDAO.revenueByDay(conn, days);

            // Totals are summed from the same series the chart draws, so the headline numbers can
            // never disagree with the bars underneath them.
            BigDecimal windowRevenue = BigDecimal.ZERO;
            int windowOrders = 0;
            BigDecimal peak = BigDecimal.ZERO;
            for (RevenuePoint point : series) {
                windowRevenue = windowRevenue.add(point.revenue());
                windowOrders += point.orderCount();
                if (point.revenue().compareTo(peak) > 0) {
                    peak = point.revenue();
                }
            }

            req.setAttribute("pageTitle", "Thống kê doanh thu");
            req.setAttribute("series", series);
            req.setAttribute("days", days);
            req.setAttribute("windowRevenue", windowRevenue);
            req.setAttribute("windowOrders", windowOrders);
            // The chart scales bar heights against this; zero would divide by zero in the view.
            req.setAttribute("peakRevenue", peak.signum() == 0 ? BigDecimal.ONE : peak);
            req.setAttribute("revenueToday", orderDAO.sumRevenueToday(conn));
            req.setAttribute("revenueThisMonth", orderDAO.sumRevenueThisMonth(conn));
            req.setAttribute("statusCounts", orderDAO.countsByStatus(conn));
            req.getRequestDispatcher("/WEB-INF/views/admin/reports.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
