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
import com.eaut.canteen.util.Settings;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet({"/products", "/products/detail", "/products/recent-activity", "/products/quick-view", "/products/category-menu"})
public class CatalogServlet extends HttpServlet {

    /** 5 cards per row on desktop, four rows to a page. */
    private static final int PAGE_SIZE = 20;
    /** Enough for one full row in each of the promo/trending/favourites strips. */
    private static final int SECTION_SIZE = 5;
    /** "Hot hit tuần" — the last seven days of completed orders. */
    private static final int HOT_WINDOW_DAYS = 7;
    /** Stops a typed ?page=999999999 from becoming a pointless OFFSET. */
    private static final int MAX_PAGE = 10_000;

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
        Integer categoryId = RequestParams.intOrNull(req.getParameter("category"));
        String query = RequestParams.trimmedOrNull(req.getParameter("q"));
        String sort = RequestParams.trimmedOrNull(req.getParameter("sort"));
        int page = RequestParams.intInRange(req.getParameter("page"), 1, 1, MAX_PAGE);

        int totalItems = productDAO.countSearch(conn, query, categoryId);
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) PAGE_SIZE));
        // Landing past the end (bookmark from when the menu was longer, or a typed ?page=) should
        // show the last real page rather than an empty grid.
        page = Math.min(page, totalPages);

        List<Product> products = productDAO.search(conn, query, categoryId, sort,
                PAGE_SIZE, (page - 1) * PAGE_SIZE);

        User customer = currentCustomer(req);
        Set<Integer> favoritedIds = favoritedIdsOf(conn, customer);
        markFavorited(products, favoritedIds);

        // Promotions / trending / favourites are for browsing, so they only appear on the plain
        // menu. Stacking them above a search result would bury what the customer actually asked for.
        boolean browsing = query == null && categoryId == null;
        if (browsing) {
            List<Product> promo = productDAO.findOnPromo(conn, SECTION_SIZE);
            List<Product> hot = productDAO.findBestSellers(conn, HOT_WINDOW_DAYS, SECTION_SIZE);
            markFavorited(promo, favoritedIds);
            markFavorited(hot, favoritedIds);
            req.setAttribute("promoProducts", promo);
            req.setAttribute("hotProducts", hot);

            if (customer != null) {
                List<Product> favorites = productDAO.findFavoritesByUser(conn, customer.getUserId(), SECTION_SIZE);
                markFavorited(favorites, favoritedIds);
                req.setAttribute("favoriteProducts", favorites);
            }
        }

        req.setAttribute("pageTitle", query != null ? "Tìm: " + query : "Thực đơn");
        req.setAttribute("categories", categories);
        req.setAttribute("products", products);
        req.setAttribute("selectedCategory", categoryId);
        req.setAttribute("searchQuery", query);
        req.setAttribute("selectedSort", sort);
        req.setAttribute("currentPage", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("totalItems", totalItems);
        req.setAttribute("hotWindowDays", HOT_WINDOW_DAYS);
        req.setAttribute("shopAcceptingOrders", shopStatusDAO.get(conn).isAcceptingOrders());
        req.setAttribute("estimatedWaitMinutes", estimatedWaitMinutes(conn));

        // Layout auto-adjusts to whichever positions actually have an active banner — see
        // catalog.jsp/style.css .catalog-layout — instead of always reserving the space.
        req.setAttribute("headBanners", bannerDAO.findActiveByPosition(conn, "HEAD"));
        // Read once per catalog render rather than per page site-wide: this is the only page the
        // effects are meant for, and Settings caches so the cost is a map lookup.
        req.setAttribute("effectsMode", Settings.getString(conn, "effects.mode", "NONE"));
        req.setAttribute("effectsIntensity", Settings.getInt(conn, "effects.intensity", 2));
        req.setAttribute("leftBanners", bannerDAO.findActiveByPosition(conn, "LEFT"));
        req.setAttribute("rightBanners", bannerDAO.findActiveByPosition(conn, "RIGHT"));
        req.setAttribute("footerBanners", bannerDAO.findActiveByPosition(conn, "FOOTER"));

        req.getRequestDispatcher("/WEB-INF/views/customer/catalog.jsp").forward(req, resp);
    }

    /** One query for the whole list instead of asking FavoriteDAO once per product. */
    /**
     * The catalog page paints heart state on up to four lists (grid, promo, trending, favourites).
     * The id set is fetched once by the caller and reused, because with no connection pool each
     * repeat of that query is a fresh round trip for an answer that cannot have changed.
     */
    private void markFavorited(List<Product> products, Set<Integer> favoritedIds) {
        if (favoritedIds.isEmpty() || products.isEmpty()) {
            return;
        }
        for (Product product : products) {
            product.setFavoritedByCurrentUser(favoritedIds.contains(product.getProductId()));
        }
    }

    /** Empty for a guest or a staff session — nobody to have favourites. */
    private Set<Integer> favoritedIdsOf(Connection conn, User customer) throws SQLException {
        return customer == null ? Set.of() : favoriteDAO.findFavoritedProductIds(conn, customer.getUserId());
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
        Integer productId = RequestParams.intOrNull(req.getParameter("id"));
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
        Integer productId = RequestParams.intOrNull(req.getParameter("id"));
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
