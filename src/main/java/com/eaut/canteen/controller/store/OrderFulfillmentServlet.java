package com.eaut.canteen.controller.store;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.LoyaltyDAO;
import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.OrderStatusHistoryDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.LoyaltyDAOImpl;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.OrderStatusHistoryDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.LoyaltyTransaction;
import com.eaut.canteen.model.LoyaltyTransactionType;
import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.PaymentMethod;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.AppConfig;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Store staff order-fulfillment actions: pick (CONFIRMED -> SHIPPING) and complete (SHIPPING -> COMPLETED). */
@WebServlet({"/store/orders/pick", "/store/orders/complete"})
public class OrderFulfillmentServlet extends HttpServlet {

    private static final BigDecimal DEFAULT_VND_PER_POINT = BigDecimal.valueOf(10_000);

    private static final OrderDAO orderDAO = new OrderDAOImpl();
    private static final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAOImpl();
    private static final UserDAO userDAO = new UserDAOImpl();
    private static final LoyaltyDAO loyaltyDAO = new LoyaltyDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int orderId = Integer.parseInt(req.getParameter("orderId"));
        User staff = (User) req.getSession().getAttribute("user");
        boolean isComplete = "/store/orders/complete".equals(req.getServletPath());

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean applied = isComplete
                        ? completeOrder(conn, orderId, staff)
                        : pickOrder(conn, orderId, staff);

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

        resp.sendRedirect(req.getContextPath() + "/store/orders");
    }

    private boolean pickOrder(Connection conn, int orderId, User staff) throws SQLException {
        int updated = orderDAO.updateStatus(conn, orderId, OrderStatus.CONFIRMED, OrderStatus.SHIPPING);
        if (updated == 0) {
            return false;
        }
        historyDAO.insert(conn, orderId, OrderStatus.CONFIRMED, OrderStatus.SHIPPING, staff.getUserId(),
                "Nhân viên cửa hàng lấy hàng và bắt đầu giao");
        return true;
    }

    private boolean completeOrder(Connection conn, int orderId, User staff) throws SQLException {
        int updated = orderDAO.updateStatus(conn, orderId, OrderStatus.SHIPPING, OrderStatus.COMPLETED);
        if (updated == 0) {
            return false;
        }

        Order order = orderDAO.findById(conn, orderId);
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            orderDAO.markPaid(conn, orderId, staff.getUserId());
        }

        // Tích điểm: only web orders with a known customer earn points — a COUNTER sale has no
        // customer_id to credit, and points are only awarded once the order is actually delivered
        // (not merely placed), same trust boundary as everything else "completed" implies here.
        if (order.getCustomerId() != null) {
            awardLoyaltyPoints(conn, order);
        }

        historyDAO.insert(conn, orderId, OrderStatus.SHIPPING, OrderStatus.COMPLETED, staff.getUserId(),
                "Giao hàng thành công");
        return true;
    }

    private void awardLoyaltyPoints(Connection conn, Order order) throws SQLException {
        String configured = AppConfig.get("loyalty.vndPerPoint");
        BigDecimal vndPerPoint = configured == null || configured.isBlank()
                ? DEFAULT_VND_PER_POINT
                : new BigDecimal(configured.trim());
        int points = order.getTotalAmount().divide(vndPerPoint, 0, RoundingMode.DOWN).intValue();
        if (points <= 0) {
            return;
        }
        userDAO.adjustLoyaltyPoints(conn, order.getCustomerId(), points);
        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setUserId(order.getCustomerId());
        tx.setPoints(points);
        tx.setType(LoyaltyTransactionType.EARN);
        tx.setOrderId(order.getOrderId());
        tx.setNote("Tích điểm từ đơn " + order.getOrderCode());
        loyaltyDAO.insert(conn, tx);
    }
}
