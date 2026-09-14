package com.eaut.canteen.controller.customer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.dao.CategoryDAO;
import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.ShopStatusDAO;
import com.eaut.canteen.dao.impl.CategoryDAOImpl;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.ShopStatusDAOImpl;
import com.eaut.canteen.model.Category;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.model.RecentActivityItem;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet({"/products", "/products/detail", "/products/recent-activity"})
public class CatalogServlet extends HttpServlet {

    private final CategoryDAO categoryDAO = new CategoryDAOImpl();
    private final ProductDAO productDAO = new ProductDAOImpl();
    private final OrderDAO orderDAO = new OrderDAOImpl();
    private final ShopStatusDAO shopStatusDAO = new ShopStatusDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            switch (req.getServletPath()) {
                case "/products/detail" -> showDetail(req, resp, conn);
                case "/products/recent-activity" -> showRecentActivity(resp, conn);
                default -> showList(req, resp, conn);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Small hand-rolled JSON endpoint (no JSON library in this project — plain JDBC only, see
     * README) polled by the catalog page's "vừa có người đặt món này" toast. Real data, not
     * fabricated social proof: the last few genuinely placed order items.
     */
    private void showRecentActivity(HttpServletResponse resp, Connection conn) throws SQLException, IOException {
        List<RecentActivityItem> items = orderDAO.findRecentActivity(conn, 5);
        resp.setContentType("application/json;charset=UTF-8");
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            RecentActivityItem item = items.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"product\":\"").append(escapeJson(item.getProductName())).append("\",")
                .append("\"building\":\"").append(escapeJson(item.getBuildingName())).append("\",")
                .append("\"minutesAgo\":").append(item.getMinutesAgo()).append('}');
        }
        json.append(']');
        resp.getWriter().write(json.toString());
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        List<Category> categories = categoryDAO.findAllActive(conn);

        String categoryParam = req.getParameter("category");
        List<Product> products;
        if (categoryParam != null && !categoryParam.isBlank()) {
            products = productDAO.findAllActiveByCategory(conn, Integer.parseInt(categoryParam));
            req.setAttribute("selectedCategory", Integer.parseInt(categoryParam));
        } else {
            products = productDAO.findAllActive(conn);
        }

        req.setAttribute("pageTitle", "Thực đơn");
        req.setAttribute("categories", categories);
        req.setAttribute("products", products);
        req.setAttribute("shopAcceptingOrders", shopStatusDAO.get(conn).isAcceptingOrders());
        req.setAttribute("estimatedWaitMinutes", estimatedWaitMinutes(conn));
        req.getRequestDispatcher("/WEB-INF/views/customer/catalog.jsp").forward(req, resp);
    }

    /**
     * A real (not fabricated) heuristic from the current queue depth: 2 minutes per order already
     * confirmed/cooking, 3 minutes per order still waiting to even be looked at — not a measured
     * average, just a transparent estimate so the number moves with genuine order-volume data.
     */
    private int estimatedWaitMinutes(Connection conn) throws SQLException {
        int confirmed = orderDAO.countByStatus(conn, OrderStatus.CONFIRMED);
        int pending = orderDAO.countByStatus(conn, OrderStatus.PENDING);
        return 5 + confirmed * 2 + pending * 3;
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        int productId = Integer.parseInt(req.getParameter("id"));
        Product product = productDAO.findById(conn, productId);

        if (product == null || !product.isActive()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        req.setAttribute("pageTitle", product.getName());
        req.setAttribute("product", product);
        req.getRequestDispatcher("/WEB-INF/views/customer/product-detail.jsp").forward(req, resp);
    }
}
