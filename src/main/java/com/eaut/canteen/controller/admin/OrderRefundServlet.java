package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.eaut.canteen.dao.LoyaltyDAO;
import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.OrderItemDAO;
import com.eaut.canteen.dao.OrderStatusHistoryDAO;
import com.eaut.canteen.dao.ShelfStockDAO;
import com.eaut.canteen.dao.StockMovementDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.WalletDAO;
import com.eaut.canteen.dao.impl.LoyaltyDAOImpl;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.OrderItemDAOImpl;
import com.eaut.canteen.dao.impl.OrderStatusHistoryDAOImpl;
import com.eaut.canteen.dao.impl.ShelfStockDAOImpl;
import com.eaut.canteen.dao.impl.StockMovementDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.dao.impl.WalletDAOImpl;
import com.eaut.canteen.model.LoyaltyTransaction;
import com.eaut.canteen.model.LoyaltyTransactionType;
import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderItem;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.PaymentStatus;
import com.eaut.canteen.model.StockLocation;
import com.eaut.canteen.model.StockMovementReason;
import com.eaut.canteen.model.User;
import com.eaut.canteen.model.WalletTransaction;
import com.eaut.canteen.model.WalletTransactionType;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cancels an order and gives the student back everything the order took: money, loyalty points,
 * and the stock.
 *
 * <p>THE BUG THIS CLOSES: until now cancelling an order paid with EAUT Pay kept the money.
 * CheckoutServlet debits the wallet and spends loyalty points when the order is placed, but
 * OrderActionServlet.cancel() only put the stock back. WalletTransactionType.REFUND existed in the
 * enum and in the database CHECK, and nothing had ever written one.
 *
 * <p>Everything below happens in ONE transaction. A refund that credited the wallet but failed
 * before writing the ledger row would leave the balance and its ledger permanently disagreeing,
 * and there is no way to tell afterwards which of the two is right.
 */
@WebServlet("/admin/orders/refund")
public class OrderRefundServlet extends HttpServlet {

    /**
     * COMPLETED is included deliberately: a student who was delivered the wrong food complains
     * after the fact, and that is exactly when a manager needs this. REJECTED and CANCELLED are
     * not — those already ran the stock-restoring path, and re-running it would double the shelf.
     */
    private static final Set<OrderStatus> REFUNDABLE_FROM = Set.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING, OrderStatus.COMPLETED);

    private static final OrderDAO orderDAO = new OrderDAOImpl();
    private static final OrderItemDAO orderItemDAO = new OrderItemDAOImpl();
    private static final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAOImpl();
    private static final ShelfStockDAO shelfStockDAO = new ShelfStockDAOImpl();
    private static final StockMovementDAO stockMovementDAO = new StockMovementDAOImpl();
    private static final UserDAO userDAO = new UserDAOImpl();
    private static final WalletDAO walletDAO = new WalletDAOImpl();
    private static final LoyaltyDAO loyaltyDAO = new LoyaltyDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer orderId = RequestParams.intOrNull(req.getParameter("orderId"));
        if (orderId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String reason = RequestParams.trimmedOrNull(req.getParameter("reason"));
        String confirmCode = RequestParams.trimmedOrNull(req.getParameter("confirmCode"));
        User admin = (User) req.getSession().getAttribute("user");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String failure = refund(conn, orderId, admin, reason, confirmCode);
                if (failure != null) {
                    conn.rollback();
                    req.getSession().setAttribute("actionError", failure);
                } else {
                    conn.commit();
                    req.getSession().setAttribute("actionMessage", "Đã hủy đơn và hoàn trả cho khách hàng.");
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

        resp.sendRedirect(req.getContextPath() + "/admin/orders/detail?id=" + orderId);
    }

    /** @return null on success, or the Vietnamese message explaining why nothing was done. */
    private String refund(Connection conn, int orderId, User admin, String reason, String confirmCode)
            throws SQLException {
        Order order = orderDAO.findById(conn, orderId);
        if (order == null) {
            return "Không tìm thấy đơn hàng.";
        }
        if (!REFUNDABLE_FROM.contains(order.getOrderStatus())) {
            return "Đơn ở trạng thái " + order.getOrderStatus().getDisplayName()
                    + " nên không hoàn được — hàng đã được trả về kệ trước đó.";
        }

        // The form asks the admin to retype the order code. Checking it here rather than only in
        // the browser is what makes it a real safeguard: it survives JavaScript being off, a
        // resubmitted form, and anything hand-crafting this POST.
        if (!order.getOrderCode().equalsIgnoreCase(confirmCode == null ? "" : confirmCode.trim())) {
            return "Mã đơn xác nhận không khớp — chưa hoàn tiền.";
        }

        OrderStatus previous = order.getOrderStatus();
        if (orderDAO.updateStatus(conn, orderId, previous, OrderStatus.CANCELLED) == 0) {
            return "Trạng thái đơn vừa thay đổi, vui lòng tải lại trang.";
        }

        // Money only moves for an order that was actually paid. An unpaid COD order is cancelled
        // with a refund of zero, which is still recorded so it reads as "settled, nothing owed"
        // rather than "never processed".
        boolean paid = order.getPaymentStatus() == PaymentStatus.PAID;
        BigDecimal refundAmount = paid ? order.getTotalAmount() : BigDecimal.ZERO;

        // Claim the refund BEFORE moving any money. This UPDATE only matches while refunded_at is
        // still null, so two admins clicking at the same moment cannot both pay out — the loser
        // gets 0 rows and the whole transaction rolls back.
        if (orderDAO.markRefunded(conn, orderId, refundAmount, admin.getUserId()) == 0) {
            return "Đơn này đã được hoàn tiền trước đó.";
        }

        Integer customerId = order.getCustomerId();
        StringBuilder summary = new StringBuilder();

        // A COUNTER sale has no customer row to credit — cash was handed over at the till, so the
        // refund there is a physical one. Stock still comes back.
        if (customerId != null && refundAmount.signum() > 0) {
            userDAO.adjustWalletBalance(conn, customerId, refundAmount);
            WalletTransaction tx = new WalletTransaction();
            tx.setUserId(customerId);
            tx.setAmount(refundAmount);
            tx.setType(WalletTransactionType.REFUND);
            tx.setOrderId(orderId);
            tx.setCreatedBy(admin.getUserId());
            tx.setNote("Hoàn tiền đơn " + order.getOrderCode());
            walletDAO.insert(conn, tx);
            summary.append("hoàn ").append(refundAmount.toPlainString()).append("đ vào ví");
        }

        if (customerId != null) {
            int pointsBack = refundLoyalty(conn, order, admin, customerId);
            if (pointsBack != 0) {
                if (summary.length() > 0) {
                    summary.append(", ");
                }
                summary.append(pointsBack > 0 ? "trả lại " + pointsBack : "thu hồi " + (-pointsBack)).append(" điểm");
            }
        }

        restoreStock(conn, order, admin);

        String note = (reason == null || reason.isBlank() ? "Admin hủy đơn & hoàn trả" : reason)
                + (summary.length() > 0 ? " (" + summary + ")" : "");
        historyDAO.insert(conn, orderId, previous, OrderStatus.CANCELLED, admin.getUserId(), note);
        return null;
    }

    /**
     * Points move in both directions here, and they are two different debts:
     * <ul>
     *   <li>points the student SPENT on this order are theirs, and come back;</li>
     *   <li>points the order EARNED them were payment for a meal they are no longer getting, so
     *       they go away again.</li>
     * </ul>
     * The earned figure is read from the ledger rather than recomputed from the current rate,
     * because the rate is an editable setting and may have changed since the order completed.
     *
     * @return the net change applied to the student's balance.
     */
    private int refundLoyalty(Connection conn, Order order, User admin, int customerId) throws SQLException {
        int spent = order.getLoyaltyPointsUsed();
        int earned = orderDAO.sumLoyaltyPointsAwarded(conn, order.getOrderId());
        int net = spent - earned;
        if (net == 0) {
            return 0;
        }

        userDAO.adjustLoyaltyPoints(conn, customerId, net);

        LoyaltyTransaction ltx = new LoyaltyTransaction();
        ltx.setUserId(customerId);
        ltx.setPoints(net);
        // The ledger only has EARN and REDEEM, so the sign carries the direction: giving points
        // back is an EARN, clawing them back is a REDEEM with a note saying why.
        ltx.setType(net > 0 ? LoyaltyTransactionType.EARN : LoyaltyTransactionType.REDEEM);
        ltx.setOrderId(order.getOrderId());
        ltx.setNote(net > 0
                ? "Hoàn điểm đã dùng cho đơn " + order.getOrderCode()
                : "Thu hồi điểm đã tích của đơn " + order.getOrderCode());
        loyaltyDAO.insert(conn, ltx);
        return net;
    }

    /** Puts every line back on the shelf and records why, so the count stays explainable. */
    private void restoreStock(Connection conn, Order order, User admin) throws SQLException { // NOSONAR admin is the ledger actor
        List<OrderItem> items = orderItemDAO.findByOrderId(conn, order.getOrderId());
        for (OrderItem item : items) {
            shelfStockDAO.increment(conn, item.getProductId(), item.getQuantity());
            stockMovementDAO.insert(conn, item.getProductId(), StockLocation.SHELF, item.getQuantity(),
                    StockMovementReason.RESTOCK, order.getOrderId(),
                    "Hoàn hàng do hủy đơn " + order.getOrderCode(), admin.getUserId());
        }
    }
}
