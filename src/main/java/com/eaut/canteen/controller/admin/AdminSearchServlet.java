package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.CustomerSummary;
import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderFilter;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Backs the Ctrl+K command palette: one query across dishes, orders and students.
 *
 * <p>Each group is gated by the permission that governs the screen it links to, so the palette can
 * never become a way around the RBAC. A sales account that cannot open /admin/customers gets no
 * student results at all, rather than results that 403 when clicked.
 */
@WebServlet("/admin/search")
public class AdminSearchServlet extends HttpServlet {

    /** Enough to recognise what you were looking for; the full lists have their own screens. */
    private static final int PER_GROUP = 5;
    /** A single character matches most of the menu and is never what anyone meant. */
    private static final int MIN_QUERY_LENGTH = 2;

    private static final ProductDAO productDAO = new ProductDAOImpl();
    private static final OrderDAO orderDAO = new OrderDAOImpl();
    private static final UserDAO userDAO = new UserDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        String query = RequestParams.trimmedOrNull(req.getParameter("q"));
        PrintWriter out = resp.getWriter();
        if (query == null || query.length() < MIN_QUERY_LENGTH) {
            out.write("{\"products\":[],\"orders\":[],\"customers\":[]}");
            return;
        }

        User user = (User) req.getSession().getAttribute("user");

        try (Connection conn = DBConnection.getConnection()) {
            StringBuilder json = new StringBuilder("{\"products\":[");
            if (user.hasPermission("products.manage")) {
                List<Product> products = productDAO.search(conn, query, null, "name", PER_GROUP, 0);
                for (int i = 0; i < products.size(); i++) {
                    Product p = products.get(i);
                    appendComma(json, i);
                    appendHit(json, p.getName(), p.getCategoryName(),
                            "/admin/products/form?id=" + p.getProductId());
                }
            }

            json.append("],\"orders\":[");
            if (user.hasPermission("orders.manage")) {
                OrderFilter filter = new OrderFilter(null, null, null, null, null, query);
                List<Order> orders = orderDAO.findFiltered(conn, filter, PER_GROUP, 0);
                for (int i = 0; i < orders.size(); i++) {
                    Order o = orders.get(i);
                    appendComma(json, i);
                    appendHit(json, o.getOrderCode(),
                            o.getOrderStatus().getDisplayName() + " · " + o.getCreatedAtDisplay(),
                            "/admin/orders/detail?id=" + o.getOrderId());
                }
            }

            json.append("],\"customers\":[");
            if (user.hasPermission("customers.manage")) {
                List<CustomerSummary> customers = userDAO.findCustomers(conn, query, PER_GROUP, 0);
                for (int i = 0; i < customers.size(); i++) {
                    User c = customers.get(i).getUser();
                    appendComma(json, i);
                    String hint = c.getStudentId() == null || c.getStudentId().isBlank()
                            ? c.getEmail() : c.getStudentId();
                    appendHit(json, c.getFullName(), hint,
                            "/admin/customers?q=" + urlEncode(c.getUsername()));
                }
            }

            json.append("]}");
            out.write(json.toString());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void appendComma(StringBuilder json, int index) {
        if (index > 0) {
            json.append(',');
        }
    }

    private void appendHit(StringBuilder json, String label, String hint, String url) {
        json.append("{\"label\":\"").append(escape(label))
            .append("\",\"hint\":\"").append(escape(hint))
            .append("\",\"url\":\"").append(escape(url))
            .append("\"}");
    }

    private String urlEncode(String value) {
        return value == null ? ""
                : java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * These are dish names, student names and order codes — user-supplied text going into a JSON
     * string literal. Unescaped, a dish named with a quote mark breaks the whole response, and one
     * containing a backslash or a control character does worse.
     */
    private String escape(String value) {
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
