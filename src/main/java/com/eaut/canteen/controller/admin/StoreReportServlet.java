package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.dao.FavoriteDAO;
import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.impl.FavoriteDAOImpl;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Store report (reports.view): what is selling, what is running low, what customers favourite.
 * Best sellers use the same completed-orders-only definition as the catalog's "Hot hit tuần" and
 * the "Đã bán" progress bar, so the three never contradict each other.
 */
@WebServlet("/admin/store-report")
public class StoreReportServlet extends HttpServlet {

    private static final int DEFAULT_DAYS = 30;
    private static final int MIN_DAYS = 7;
    private static final int MAX_DAYS = 365;
    private static final int TOP_LIMIT = 15;
    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductDAO productDAO = new ProductDAOImpl();
    private final FavoriteDAO favoriteDAO = new FavoriteDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int days = RequestParams.intInRange(req.getParameter("days"), DEFAULT_DAYS, MIN_DAYS, MAX_DAYS);

        try (Connection conn = DBConnection.getConnection()) {
            List<Product> bestSellers = productDAO.findBestSellers(conn, days, TOP_LIMIT);

            // Shelf stock is what customers can actually buy, so that — not warehouse stock — is
            // what "sắp hết hàng" has to mean here.
            List<Product> lowStock = productDAO.findAllForAdmin(conn).stream()
                    .filter(p -> p.isActive() && p.getShelfQuantity() < LOW_STOCK_THRESHOLD)
                    .toList();

            req.setAttribute("pageTitle", "Quản lý cửa hàng");
            req.setAttribute("bestSellers", bestSellers);
            req.setAttribute("lowStock", lowStock);
            req.setAttribute("mostFavorited", favoriteDAO.findMostFavorited(conn, TOP_LIMIT));
            req.setAttribute("days", days);
            req.setAttribute("lowStockThreshold", LOW_STOCK_THRESHOLD);
            req.getRequestDispatcher("/WEB-INF/views/admin/store-report.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
