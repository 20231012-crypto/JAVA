package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.eaut.canteen.dao.BuildingDAO;
import com.eaut.canteen.dao.CategoryDAO;
import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.BuildingDAOImpl;
import com.eaut.canteen.dao.impl.CategoryDAOImpl;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/admin")
public class AdminDashboardServlet extends HttpServlet {

    private static final ProductDAO productDAO = new ProductDAOImpl();
    private static final CategoryDAO categoryDAO = new CategoryDAOImpl();
    private static final BuildingDAO buildingDAO = new BuildingDAOImpl();
    private static final UserDAO userDAO = new UserDAOImpl();
    private static final OrderDAO orderDAO = new OrderDAOImpl();

    /** One bar per hour in this range — outside typical canteen operating hours the chart would be empty anyway. */
    private static final int CHART_START_HOUR = 6;
    private static final int CHART_END_HOUR = 21;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Product> products = productDAO.findAllForAdmin(conn);
            long lowStockCount = products.stream().filter(p -> p.isActive() && p.getShelfQuantity() < 5).count();

            req.setAttribute("pageTitle", "Tổng quan");
            req.setAttribute("productCount", products.size());
            req.setAttribute("lowStockCount", lowStockCount);
            req.setAttribute("categoryCount", categoryDAO.findAllActive(conn).size());
            req.setAttribute("buildingCount", buildingDAO.findAllActive(conn).size());
            req.setAttribute("staffCount", userDAO.findAllStaff(conn).size());

            req.setAttribute("revenueToday", orderDAO.sumRevenueToday(conn));
            req.setAttribute("pendingCount", orderDAO.countByStatus(conn, OrderStatus.PENDING));
            req.setAttribute("confirmedCount", orderDAO.countByStatus(conn, OrderStatus.CONFIRMED));
            req.setAttribute("shippingCount", orderDAO.countByStatus(conn, OrderStatus.SHIPPING));

            Map<Integer, Integer> countsByHour = orderDAO.countOrdersByHourToday(conn);
            req.setAttribute("hourlyChart", buildHourlyChart(countsByHour));
            req.setAttribute("peakHourLabel", peakHourLabel(countsByHour));

            req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private String peakHourLabel(Map<Integer, Integer> countsByHour) {
        return countsByHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + "h (" + e.getValue() + " đơn)")
                .orElse("Chưa có đơn hôm nay");
    }

    /** One row per hour in the display range, bar height as a 0-100 percent of that day's busiest hour — ready for the JSP to render directly as CSS height%. */
    private List<HourBar> buildHourlyChart(Map<Integer, Integer> countsByHour) {
        int max = countsByHour.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<HourBar> bars = new ArrayList<>();
        for (int hour = CHART_START_HOUR; hour <= CHART_END_HOUR; hour++) {
            int count = countsByHour.getOrDefault(hour, 0);
            int heightPercent = max == 0 ? 0 : Math.round(count * 100f / max);
            bars.add(new HourBar(hour, count, heightPercent));
        }
        return bars;
    }

    /** One bar of the admin dashboard's peak-hour chart (see admin/dashboard.jsp). */
    public record HourBar(int hour, int count, int heightPercent) {
        public String getLabel() {
            return hour + "h";
        }

        public int getHour() {
            return hour;
        }

        public int getCount() {
            return count;
        }

        public int getHeightPercent() {
            return heightPercent;
        }
    }
}
