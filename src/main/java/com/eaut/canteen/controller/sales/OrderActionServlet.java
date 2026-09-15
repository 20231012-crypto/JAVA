package com.eaut.canteen.controller.sales;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.OrderItemDAO;
import com.eaut.canteen.dao.OrderStatusHistoryDAO;
import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.ShelfStockDAO;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.OrderItemDAOImpl;
import com.eaut.canteen.dao.impl.OrderStatusHistoryDAOImpl;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.ShelfStockDAOImpl;
import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderItem;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.PaymentMethod;
import com.eaut.canteen.model.PaymentStatus;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.model.User;
import com.eaut.canteen.dao.StockMovementDAO;
import com.eaut.canteen.dao.impl.StockMovementDAOImpl;
import com.eaut.canteen.model.StockLocation;
import com.eaut.canteen.model.StockMovementReason;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet({"/sales/orders/confirm", "/sales/orders/reject", "/sales/orders/cancel"})
public class OrderActionServlet extends HttpServlet {

    private static final Set<OrderStatus> CANCELLABLE_FROM = Set.of(OrderStatus.PENDING, OrderStatus.CONFIRMED);

    private static final OrderDAO orderDAO = new OrderDAOImpl();
    private static final OrderItemDAO orderItemDAO = new OrderItemDAOImpl();
    private static final ShelfStockDAO shelfStockDAO = new ShelfStockDAOImpl();
    private static final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAOImpl();
    private static final ProductDAO productDAO = new ProductDAOImpl();
    private static final StockMovementDAO stockMovementDAO = new StockMovementDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int orderId = Integer.parseInt(req.getParameter("orderId"));
        String note = req.getParameter("note");
        User staff = (User) req.getSession().getAttribute("user");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Order order = orderDAO.findById(conn, orderId);
                if (order == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                String path = req.getServletPath();
                boolean applied = switch (path) {
                    case "/sales/orders/confirm" -> confirm(conn, order, staff, note);
                    case "/sales/orders/reject" -> reject(conn, order, staff, note);
                    case "/sales/orders/cancel" -> cancel(conn, order, staff, note);
                    default -> false;
                };

                if (!applied) {
                    conn.rollback();
                    req.getSession().setAttribute("actionError", "Trạng thái đơn hàng đã thay đổi, vui lòng tải lại trang.");
                } else {
                    conn.commit();
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        resp.sendRedirect(req.getContextPath() + "/sales/orders/detail?id=" + orderId);
    }

    private boolean confirm(Connection conn, Order order, User staff, String note) throws SQLException {
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            return false;
        }
        if (order.getPaymentMethod() == PaymentMethod.VIETQR && order.getPaymentStatus() != PaymentStatus.PAID) {
            return false;
        }
        int updated = orderDAO.updateStatus(conn, order.getOrderId(), OrderStatus.PENDING, OrderStatus.CONFIRMED);
        if (updated == 0) {
            return false;
        }

        // KDS countdown timer: estimate ready-at as now + the slowest item in the order (dishes
        // cook in parallel at a real kitchen, not sequentially, so max — not sum — of prep times).
        int maxPrepMinutes = 0;
        for (OrderItem item : orderItemDAO.findByOrderId(conn, order.getOrderId())) {
            Product product = productDAO.findById(conn, item.getProductId());
            if (product != null) {
                maxPrepMinutes = Math.max(maxPrepMinutes, product.getAvgPrepMinutes());
            }
        }
        if (maxPrepMinutes == 0) {
            maxPrepMinutes = 10; // fallback only if the order's products couldn't be looked up at all
        }
        orderDAO.setEstimatedReadyAt(conn, order.getOrderId(), java.time.LocalDateTime.now().plusMinutes(maxPrepMinutes));

        historyDAO.insert(conn, order.getOrderId(), OrderStatus.PENDING, OrderStatus.CONFIRMED, staff.getUserId(),
                note == null || note.isBlank() ? "Nhân viên bán hàng xác nhận đơn" : note);
        return true;
    }

    private boolean reject(Connection conn, Order order, User staff, String note) throws SQLException {
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            return false;
        }
        int updated = orderDAO.updateStatus(conn, order.getOrderId(), OrderStatus.PENDING, OrderStatus.REJECTED);
        if (updated == 0) {
            return false;
        }
        restoreStock(conn, order.getOrderId(), staff, "Hoàn hàng do từ chối đơn " + order.getOrderCode());
        historyDAO.insert(conn, order.getOrderId(), OrderStatus.PENDING, OrderStatus.REJECTED, staff.getUserId(),
                note == null || note.isBlank() ? "Từ chối đơn" : note);
        return true;
    }

    private boolean cancel(Connection conn, Order order, User staff, String note) throws SQLException {
        if (!CANCELLABLE_FROM.contains(order.getOrderStatus())) {
            return false;
        }
        OrderStatus current = order.getOrderStatus();
        int updated = orderDAO.updateStatus(conn, order.getOrderId(), current, OrderStatus.CANCELLED);
        if (updated == 0) {
            return false;
        }
        restoreStock(conn, order.getOrderId(), staff, "Hoàn hàng do hủy đơn " + order.getOrderCode());
        historyDAO.insert(conn, order.getOrderId(), current, OrderStatus.CANCELLED, staff.getUserId(),
                note == null || note.isBlank() ? "Hủy đơn" : note);
        return true;
    }

    private void restoreStock(Connection conn, int orderId, User staff, String why) throws SQLException {
        List<OrderItem> items = orderItemDAO.findByOrderId(conn, orderId);
        for (OrderItem item : items) {
            shelfStockDAO.increment(conn, item.getProductId(), item.getQuantity());
            stockMovementDAO.insert(conn, item.getProductId(), StockLocation.SHELF, item.getQuantity(),
                    StockMovementReason.RESTOCK, orderId, why, staff.getUserId());
        }
    }
}
