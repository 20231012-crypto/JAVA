package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.OrderItemDAO;
import com.eaut.canteen.dao.OrderStatusHistoryDAO;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.OrderItemDAOImpl;
import com.eaut.canteen.dao.impl.OrderStatusHistoryDAOImpl;
import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderChannel;
import com.eaut.canteen.model.OrderFilter;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.PaymentMethod;
import com.eaut.canteen.util.AppClock;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The admin's view of every order in the system: a filtered, paged list and a detail page with the
 * full status timeline.
 *
 * <p>This is not the same screen as /sales/orders. That one is a working queue — a cashier's
 * Kanban board of what needs doing right now. This one is for looking backwards: finding the order
 * a student is complaining about, across any date, any status, any payment method.
 */
@WebServlet({"/admin/orders", "/admin/orders/detail"})
public class AdminOrderServlet extends HttpServlet {

    private static final int PAGE_SIZE = 25;
    /** Bounds the page number so a hand-edited URL cannot ask the database for a huge OFFSET. */
    private static final int MAX_PAGE = 10_000;

    private static final OrderDAO orderDAO = new OrderDAOImpl();
    private static final OrderItemDAO orderItemDAO = new OrderItemDAOImpl();
    private static final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            if ("/admin/orders/detail".equals(req.getServletPath())) {
                showDetail(conn, req, resp);
            } else {
                showList(conn, req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showList(Connection conn, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException {
        OrderFilter filter = readFilter(conn, req);
        int page = RequestParams.intInRange(req.getParameter("page"), 1, 1, MAX_PAGE);

        int total = orderDAO.countFiltered(conn, filter);
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        // A filter change can leave the user on a page that no longer exists; clamping beats
        // showing an empty table with no explanation.
        page = Math.min(page, totalPages);

        req.setAttribute("orders", orderDAO.findFiltered(conn, filter, PAGE_SIZE, (page - 1) * PAGE_SIZE));
        req.setAttribute("totalOrders", total);
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("filter", filter);
        // Echoed back so the form redraws with what the user typed, and so the pager links can
        // carry the filter forward without the JSP having to rebuild it from the filter object.
        req.setAttribute("qStatus", req.getParameter("status"));
        req.setAttribute("qPayment", req.getParameter("payment"));
        req.setAttribute("qChannel", req.getParameter("channel"));
        req.setAttribute("qFrom", req.getParameter("from"));
        req.setAttribute("qTo", req.getParameter("to"));
        req.setAttribute("qText", req.getParameter("q"));
        req.setAttribute("statuses", OrderStatus.values());
        req.setAttribute("paymentMethods", PaymentMethod.values());
        req.setAttribute("pageTitle", "Quản lý đơn hàng");
        req.getRequestDispatcher("/WEB-INF/views/admin/orders.jsp").forward(req, resp);
    }

    private void showDetail(Connection conn, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException {
        Integer orderId = RequestParams.intOrNull(req.getParameter("id"));
        if (orderId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/orders");
            return;
        }
        Order order = orderDAO.findById(conn, orderId);
        if (order == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setAttribute("order", order);
        req.setAttribute("items", orderItemDAO.findByOrderId(conn, orderId));
        req.setAttribute("history", historyDAO.findByOrderId(conn, orderId));
        req.setAttribute("pageTitle", "Đơn " + order.getOrderCode());
        req.getRequestDispatcher("/WEB-INF/views/admin/order-detail.jsp").forward(req, resp);
    }

    /**
     * Every field is optional and every unparseable value is treated as absent rather than as an
     * error: these arrive from a form that a user can half-fill and from URLs they can edit, and a
     * bad date should narrow nothing, not produce a 500.
     */
    private OrderFilter readFilter(Connection conn, HttpServletRequest req) throws SQLException {
        return new OrderFilter(
                parseEnum(OrderStatus.class, req.getParameter("status")),
                parseDayStart(conn, req.getParameter("from")),
                parseDayEnd(conn, req.getParameter("to")),
                parseEnum(PaymentMethod.class, req.getParameter("payment")),
                parseEnum(OrderChannel.class, req.getParameter("channel")),
                RequestParams.trimmedOrNull(req.getParameter("q")));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The canteen-local start of the given day, converted to the zone the timestamps are stored in. */
    private LocalDateTime parseDayStart(Connection conn, String raw) throws SQLException {
        LocalDate day = parseDate(raw);
        return day == null ? null : AppClock.startOfDay(conn, day);
    }

    /**
     * Exclusive end. The date input is inclusive to a human — "đến 15/09" means orders placed on
     * the 15th count — so the bound is the start of the 16th.
     */
    private LocalDateTime parseDayEnd(Connection conn, String raw) throws SQLException {
        LocalDate day = parseDate(raw);
        return day == null ? null : AppClock.endOfDay(conn, day);
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
