package com.eaut.canteen.controller.customer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eaut.canteen.dao.BannerDAO;
import com.eaut.canteen.dao.CategoryDAO;
import com.eaut.canteen.dao.FavoriteDAO;
import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.ShopStatusDAO;
import com.eaut.canteen.dao.impl.BannerDAOImpl;
import com.eaut.canteen.dao.impl.CategoryDAOImpl;
import com.eaut.canteen.dao.impl.FavoriteDAOImpl;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.ShopStatusDAOImpl;
import com.eaut.canteen.model.Category;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.model.RecentActivityItem;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet({"/products", "/products/detail", "/products/recent-activity", "/products/quick-view", "/products/category-menu"})
public class CatalogServlet extends HttpServlet {

    private final CategoryDAO categoryDAO = new CategoryDAOImpl();
    private final ProductDAO productDAO = new ProductDAOImpl();
    private final OrderDAO orderDAO = new OrderDAOImpl();
    private final ShopStatusDAO shopStatusDAO = new ShopStatusDAOImpl();
    private final FavoriteDAO favoriteDAO = new FavoriteDAOImpl();
    private final BannerDAO bannerDAO = new BannerDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            switch (req.getServletPath()) {
                case "/products/detail" -> showDetail(req, resp, conn);
                case "/products/quick-view" -> showQuickView(req, resp, conn);
                case "/products/recent-activity" -> showRecentActivity(resp, conn);
                case "/products/category-menu" -> showCategoryMenu(req, resp, conn);
                default -> showList(req, resp, conn);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /** The current session's customer, or null for a guest/staff request. */
    private User currentCustomer(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        return user != null && user.getRole().isCustomerDefault() ? user : null;
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
        // Filter chips only make sense for leaf categories — a top-level group (parent_category_id
        // IS NULL) has no products directly under it, so filtering by one would just be empty.
        List<Category> categories = categoryDAO.findAllActive(conn).stream()
                .filter(c -> !c.isTopLevel())
                .toList();

        // A stale or hand-edited ?category= value must not 500 the whole menu — an unparseable one
        // is treated as "no filter", the same as omitting it.
        Integer categoryId = parseIntOrNull(req.getParameter("category"));
        List<Product> products;
        if (categoryId != null) {
            products = productDAO.findAllActiveByCategory(conn, categoryId);
            req.setAttribute("selectedCategory", categoryId);
        } else {
            products = productDAO.findAllActive(conn);
        }
        markFavorited(conn, products, currentCustomer(req));

        req.setAttribute("pageTitle", "Thực đơn");
        req.setAttribute("categories", categories);
        req.setAttribute("products", products);
        req.setAttribute("shopAcceptingOrders", shopStatusDAO.get(conn).isAcceptingOrders());
        req.setAttribute("estimatedWaitMinutes", estimatedWaitMinutes(conn));

        // Layout auto-adjusts to whichever positions actually have an active banner — see
        // catalog.jsp/style.css .catalog-layout — instead of always reserving the space.
        req.setAttribute("headBanners", bannerDAO.findActiveByPosition(conn, "HEAD"));
        req.setAttribute("leftBanners", bannerDAO.findActiveByPosition(conn, "LEFT"));
        req.setAttribute("rightBanners", bannerDAO.findActiveByPosition(conn, "RIGHT"));
        req.setAttribute("footerBanners", bannerDAO.findActiveByPosition(conn, "FOOTER"));

        req.getRequestDispatcher("/WEB-INF/views/customer/catalog.jsp").forward(req, resp);
    }

    /** Returns null for a missing, blank or non-numeric value so callers can treat all three alike. */
    private static Integer parseIntOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** One query for the whole list instead of asking FavoriteDAO once per product. */
    private void markFavorited(Connection conn, List<Product> products, User customer) throws SQLException {
        if (customer == null || products.isEmpty()) {
            return;
        }
        Set<Integer> favoritedIds = favoriteDAO.findFavoritedProductIds(conn, customer.getUserId());
        for (Product product : products) {
            product.setFavoritedByCurrentUser(favoritedIds.contains(product.getProductId()));
        }
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
        Integer productId = parseIntOrNull(req.getParameter("id"));
        Product product = productId == null ? null : productDAO.findById(conn, productId);

        if (product == null || !product.isActive()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User customer = currentCustomer(req);
        if (customer != null) {
            product.setFavoritedByCurrentUser(favoriteDAO.isFavorited(conn, customer.getUserId(), product.getProductId()));
        }

        req.setAttribute("pageTitle", product.getName());
        req.setAttribute("product", product);
        req.getRequestDispatcher("/WEB-INF/views/customer/product-detail.jsp").forward(req, resp);
    }

    /**
     * Same lookup as showDetail, but forwards to a bare content fragment (no header/nav/footer)
     * for the "Xem nhanh" popup — fetched via JS and dropped into a modal instead of navigating
     * away from the catalog grid.
     */
    private void showQuickView(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        Integer productId = parseIntOrNull(req.getParameter("id"));
        Product product = productId == null ? null : productDAO.findById(conn, productId);

        if (product == null || !product.isActive()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User customer = currentCustomer(req);
        if (customer != null) {
            product.setFavoritedByCurrentUser(favoriteDAO.isFavorited(conn, customer.getUserId(), product.getProductId()));
        }

        req.setAttribute("product", product);
        req.getRequestDispatcher("/WEB-INF/views/customer/product-quick-view.jsp").forward(req, resp);
    }

    /**
     * Bare fragment (no header/nav/footer) for the nav bar's "Danh mục sản phẩm" mega-menu —
     * fetched once by JS on first hover/click and cached client-side, rather than every single
     * page needing to look this up just because nav.jsp is included everywhere.
     */
    private void showCategoryMenu(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        List<Category> all = categoryDAO.findAllActive(conn);
        List<Category> topLevel = all.stream().filter(Category::isTopLevel).toList();
        Map<Integer, List<Category>> childrenByParent = new LinkedHashMap<>();
        for (Category category : all) {
            if (!category.isTopLevel()) {
                childrenByParent.computeIfAbsent(category.getParentCategoryId(), k -> new ArrayList<>()).add(category);
            }
        }
        List<CategoryGroup> groups = new ArrayList<>();
        for (Category parent : topLevel) {
            groups.add(new CategoryGroup(parent, childrenByParent.getOrDefault(parent.getCategoryId(), List.of())));
        }
        req.setAttribute("categoryGroups", groups);
        req.getRequestDispatcher("/WEB-INF/views/customer/category-menu.jsp").forward(req, resp);
    }

    /** One mega-menu column: a top-level group and its leaf sub-categories. */
    public record CategoryGroup(Category parent, List<Category> children) {
        public Category getParent() {
            return parent;
        }

        public List<Category> getChildren() {
            return children;
        }
    }
}
