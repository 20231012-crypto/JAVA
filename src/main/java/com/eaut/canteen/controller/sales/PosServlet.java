package com.eaut.canteen.controller.sales;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.CustomerSummary;
import com.eaut.canteen.model.PosTab;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * The till. A full-screen point-of-sale screen for the counter, replacing the old product-grid
 * version of /sales/counter-sale.
 *
 * <h2>Several orders at once</h2>
 * The tabs across the top are the point of the rewrite. A student reaches the front, then realises
 * they left their card at the table; with one cart the queue waits. Each tab is a {@link PosTab}
 * with its own items, customer, note and discount, held in the session.
 *
 * <h2>Why every change is a form post</h2>
 * Adding an item, changing a quantity and taking payment are all ordinary POSTs to this servlet, so
 * the till keeps working if JavaScript fails — which at a counter matters more than anywhere else
 * in the app, because a dead till is a closed canteen. assets/js/pos.js then intercepts those same
 * forms and swaps in the re-rendered panel, so in normal use nothing reloads.
 *
 * <p>No tax control and no e-invoice checkbox. VAT is a property of the dish, set once when the
 * product is created, so there is nothing for a cashier to choose; and there is no e-invoice
 * provider wired up, so a checkbox for it would be a button that does nothing.
 */
@WebServlet({"/pos", "/pos/cart", "/pos/tab", "/pos/search", "/pos/customers", "/pos/pay"})
public class PosServlet extends HttpServlet {

    private static final String TABS_ATTRIBUTE = "posTabs";
    private static final String ACTIVE_TAB_ATTRIBUTE = "posActiveTab";

    /** Enough for a counter queue; beyond this the tab strip stops being readable anyway. */
    private static final int MAX_TABS = 8;

    /** A single character matches most of the menu, which is never what the cashier meant. */
    private static final int MIN_SEARCH_LENGTH = 1;
    private static final int SEARCH_LIMIT = 12;

    private static final ProductDAO productDAO = new ProductDAOImpl();
    private static final UserDAO userDAO = new UserDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        try (Connection conn = DBConnection.getConnection()) {
            switch (path) {
                case "/pos/search" -> searchProducts(conn, req, resp);
                case "/pos/customers" -> searchCustomers(conn, req, resp);
                default -> render(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession();
        PosTab tab = activeTab(session);

        try (Connection conn = DBConnection.getConnection()) {
            String failure = switch (req.getServletPath()) {
                case "/pos/tab" -> handleTab(session, req);
                case "/pos/pay" -> new PosCheckout().complete(conn, req, tab, currentStaff(req));
                default -> handleCart(conn, req, tab);
            };
            if (failure != null) {
                req.setAttribute("posError", failure);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        // A fetch caller gets the re-rendered screen body and swaps it in; a plain form post gets a
        // redirect, so the browser's back button and reload behave normally.
        if (isFetch(req)) {
            render(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/pos");
        }
    }

    // ---- rendering ---------------------------------------------------------------------------

    private void render(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession();
        req.setAttribute("tabs", tabs(session).values());
        req.setAttribute("tab", activeTab(session));
        req.setAttribute("staff", currentStaff(req));
        req.setAttribute("pageTitle", "Bán hàng tại quầy");
        req.getRequestDispatcher("/WEB-INF/views/sales/pos.jsp").forward(req, resp);
    }

    private boolean isFetch(HttpServletRequest req) {
        return "fetch".equals(req.getHeader("X-Requested-With"));
    }

    private User currentStaff(HttpServletRequest req) {
        return (User) req.getSession().getAttribute("user");
    }

    // ---- tabs --------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, PosTab> tabs(HttpSession session) {
        Map<String, PosTab> tabs = (Map<String, PosTab>) session.getAttribute(TABS_ATTRIBUTE);
        if (tabs == null || tabs.isEmpty()) {
            // LinkedHashMap: the strip must render in the order the cashier opened them, not in
            // whatever order a hash produces.
            tabs = new LinkedHashMap<>();
            tabs.put("1", new PosTab("1", "Đơn 1"));
            session.setAttribute(TABS_ATTRIBUTE, tabs);
            session.setAttribute(ACTIVE_TAB_ATTRIBUTE, "1");
        }
        return tabs;
    }

    private PosTab activeTab(HttpSession session) {
        Map<String, PosTab> tabs = tabs(session);
        String activeId = (String) session.getAttribute(ACTIVE_TAB_ATTRIBUTE);
        PosTab tab = activeId == null ? null : tabs.get(activeId);
        if (tab == null) {
            tab = tabs.values().iterator().next();
            session.setAttribute(ACTIVE_TAB_ATTRIBUTE, tab.getId());
        }
        return tab;
    }

    private String handleTab(HttpSession session, HttpServletRequest req) {
        Map<String, PosTab> tabs = tabs(session);
        String action = req.getParameter("action");
        String id = RequestParams.trimmedOrNull(req.getParameter("tabId"));

        switch (action == null ? "" : action) {
            case "new" -> {
                if (tabs.size() >= MAX_TABS) {
                    return "Đang mở tối đa " + MAX_TABS + " đơn cùng lúc.";
                }
                // Numbered by the smallest free number rather than by count, so closing Đơn 2 and
                // opening a new one gives Đơn 2 again instead of Đơn 4.
                int number = 1;
                while (tabs.containsKey(String.valueOf(number))) {
                    number++;
                }
                String newId = String.valueOf(number);
                tabs.put(newId, new PosTab(newId, "Đơn " + number));
                session.setAttribute(ACTIVE_TAB_ATTRIBUTE, newId);
            }
            case "close" -> {
                if (id == null || !tabs.containsKey(id)) {
                    return null;
                }
                if (tabs.size() == 1) {
                    // Closing the only tab would leave the till with no order to ring up; emptying
                    // it is what the cashier actually wants.
                    tabs.get(id).reset();
                    return null;
                }
                tabs.remove(id);
                if (id.equals(session.getAttribute(ACTIVE_TAB_ATTRIBUTE))) {
                    session.setAttribute(ACTIVE_TAB_ATTRIBUTE, tabs.keySet().iterator().next());
                }
            }
            case "switch" -> {
                if (id != null && tabs.containsKey(id)) {
                    session.setAttribute(ACTIVE_TAB_ATTRIBUTE, id);
                }
            }
            default -> {
                return null;
            }
        }
        return null;
    }

    // ---- cart --------------------------------------------------------------------------------

    private String handleCart(Connection conn, HttpServletRequest req, PosTab tab) throws SQLException {
        String action = req.getParameter("action");
        return switch (action == null ? "" : action) {
            case "add" -> addProduct(conn, req, tab);
            case "custom" -> addCustomItem(req, tab);
            case "qty" -> changeQuantity(req, tab);
            case "remove" -> {
                Integer productId = RequestParams.intOrNull(req.getParameter("productId"));
                if (productId != null) {
                    tab.getCart().removeItem(productId);
                    // The discount was capped against the old subtotal; re-apply so removing items
                    // cannot leave a discount larger than the bill.
                    tab.setDiscount(tab.getDiscount());
                }
                yield null;
            }
            case "note" -> {
                tab.setNote(RequestParams.trimmedOrNull(req.getParameter("note")));
                yield null;
            }
            case "split" -> {
                tab.setSplitLines(req.getParameter("splitLines") != null);
                yield null;
            }
            case "discount" -> {
                tab.setDiscount(parseMoney(req.getParameter("discount")));
                yield null;
            }
            case "customer" -> setCustomer(conn, req, tab);
            case "clear-customer" -> {
                tab.clearCustomer();
                yield null;
            }
            default -> null;
        };
    }

    private String addProduct(Connection conn, HttpServletRequest req, PosTab tab) throws SQLException {
        Integer productId = RequestParams.intOrNull(req.getParameter("productId"));
        int quantity = RequestParams.intInRange(req.getParameter("quantity"), 1, 1, 999);
        if (productId == null) {
            return null;
        }

        // Priced from the database, never from the form: the page the cashier is looking at may
        // have been open since before a price change.
        Product product = productDAO.findById(conn, productId);
        if (product == null || !product.isActive() || !product.isAvailable()) {
            return "Món này hiện không bán.";
        }
        if (product.getShelfQuantity() < quantity) {
            return "\"" + product.getName() + "\" chỉ còn " + product.getShelfQuantity() + " phần trên kệ.";
        }

        if (tab.isSplitLines()) {
            // A separate line per add, so two people ordering the same dish can be told apart.
            tab.getCart().addOrIncrement(tab.nextCustomItemId(), product.getName(),
                    product.getPrice(), product.getImageFilename(), quantity);
        } else {
            tab.getCart().addOrIncrement(productId, product.getName(),
                    product.getPrice(), product.getImageFilename(), quantity);
        }
        return null;
    }

    /**
     * An off-menu item — a one-off, or something the kitchen made that is not in the catalogue. It
     * has no product row, so it gets a synthetic negative id, holds no stock and carries no VAT.
     */
    private String addCustomItem(HttpServletRequest req, PosTab tab) {
        String name = RequestParams.trimmedOrNull(req.getParameter("name"));
        BigDecimal price = parseMoney(req.getParameter("price"));
        int quantity = RequestParams.intInRange(req.getParameter("quantity"), 1, 1, 999);
        if (name == null || price == null || price.signum() <= 0) {
            return "Sản phẩm tùy chỉnh cần có tên và giá lớn hơn 0.";
        }
        tab.getCart().addOrIncrement(tab.nextCustomItemId(), name, price, null, quantity);
        return null;
    }

    private String changeQuantity(HttpServletRequest req, PosTab tab) {
        Integer productId = RequestParams.intOrNull(req.getParameter("productId"));
        Integer quantity = RequestParams.intOrNull(req.getParameter("quantity"));
        if (productId == null || quantity == null) {
            return null;
        }
        if (quantity <= 0) {
            tab.getCart().removeItem(productId);
        } else {
            tab.getCart().updateQuantity(productId, Math.min(quantity, 999));
        }
        tab.setDiscount(tab.getDiscount());
        return null;
    }

    private String setCustomer(Connection conn, HttpServletRequest req, PosTab tab) throws SQLException {
        Integer userId = RequestParams.intOrNull(req.getParameter("customerId"));
        if (userId == null) {
            return null;
        }
        User customer = userDAO.findById(conn, userId);
        if (customer == null || !customer.getRole().isCustomerDefault()) {
            return "Không tìm thấy khách hàng.";
        }
        tab.setCustomer(customer.getUserId(), customer.getFullName());
        return null;
    }

    // ---- JSON lookups used by the search boxes ------------------------------------------------

    private void searchProducts(Connection conn, HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        String query = RequestParams.trimmedOrNull(req.getParameter("q"));
        if (query == null || query.length() < MIN_SEARCH_LENGTH) {
            resp.getWriter().write("[]");
            return;
        }

        List<Product> products = productDAO.search(conn, query, null, "name", SEARCH_LIMIT, 0);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"id\":").append(p.getProductId())
                .append(",\"name\":\"").append(Json.escape(p.getName()))
                .append("\",\"price\":").append(p.getPrice().toPlainString())
                .append(",\"stock\":").append(p.getShelfQuantity())
                .append(",\"sellable\":").append(p.isSellable())
                .append('}');
        }
        resp.getWriter().write(json.append(']').toString());
    }

    private void searchCustomers(Connection conn, HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        String query = RequestParams.trimmedOrNull(req.getParameter("q"));
        if (query == null || query.length() < 2) {
            resp.getWriter().write("[]");
            return;
        }

        List<CustomerSummary> matches = userDAO.findCustomers(conn, query, 8, 0);
        List<String> rows = new ArrayList<>();
        for (CustomerSummary row : matches) {
            User u = row.getUser();
            String hint = u.getStudentId() == null || u.getStudentId().isBlank()
                    ? (u.getPhone() == null ? u.getEmail() : u.getPhone())
                    : u.getStudentId();
            rows.add("{\"id\":" + u.getUserId()
                    + ",\"name\":\"" + Json.escape(u.getFullName())
                    + "\",\"hint\":\"" + Json.escape(hint) + "\"}");
        }
        resp.getWriter().write("[" + String.join(",", rows) + "]");
    }

    private BigDecimal parseMoney(String raw) {
        String trimmed = RequestParams.trimmedOrNull(raw);
        if (trimmed == null) {
            return null;
        }
        try {
            // Cashiers type thousands separators out of habit, and the number pad produces dots.
            return new BigDecimal(trimmed.replace(".", "").replace(",", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Minimal JSON string escaping — these are dish and student names typed by people. */
    static final class Json {
        private Json() {
        }

        static String escape(String value) {
            if (value == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder(value.length() + 8);
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                    }
                }
            }
            return sb.toString();
        }
    }
}
