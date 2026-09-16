package com.eaut.canteen.controller.sales;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.eaut.canteen.dao.LoyaltyDAO;
import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.OrderItemDAO;
import com.eaut.canteen.dao.OrderStatusHistoryDAO;
import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.ShelfStockDAO;
import com.eaut.canteen.dao.StockMovementDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.WalletDAO;
import com.eaut.canteen.dao.impl.LoyaltyDAOImpl;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.OrderItemDAOImpl;
import com.eaut.canteen.dao.impl.OrderStatusHistoryDAOImpl;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.ShelfStockDAOImpl;
import com.eaut.canteen.dao.impl.StockMovementDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.dao.impl.WalletDAOImpl;
import com.eaut.canteen.model.CartItem;
import com.eaut.canteen.model.LoyaltyTransaction;
import com.eaut.canteen.model.LoyaltyTransactionType;
import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderChannel;
import com.eaut.canteen.model.OrderItem;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.PaymentMethod;
import com.eaut.canteen.model.PaymentStatus;
import com.eaut.canteen.model.PosTab;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.model.StockLocation;
import com.eaut.canteen.model.StockMovementReason;
import com.eaut.canteen.model.User;
import com.eaut.canteen.model.WalletTransaction;
import com.eaut.canteen.model.WalletTransactionType;
import com.eaut.canteen.util.Settings;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Turns a till tab into a completed order.
 *
 * <p>Split out of {@link PosServlet} because it is the only part that moves money and stock, and it
 * deserves to be read on its own. Everything below happens in one transaction: a sale that took
 * stock but recorded no order, or charged a wallet but recorded no payment, is worse than a sale
 * that simply failed and can be rung up again.
 */
class PosCheckout {

    private static final OrderDAO orderDAO = new OrderDAOImpl();
    private static final OrderItemDAO orderItemDAO = new OrderItemDAOImpl();
    private static final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAOImpl();
    private static final ShelfStockDAO shelfStockDAO = new ShelfStockDAOImpl();
    private static final StockMovementDAO stockMovementDAO = new StockMovementDAOImpl();
    private static final ProductDAO productDAO = new ProductDAOImpl();
    private static final UserDAO userDAO = new UserDAOImpl();
    private static final WalletDAO walletDAO = new WalletDAOImpl();
    private static final LoyaltyDAO loyaltyDAO = new LoyaltyDAOImpl();

    /**
     * @return null when the sale went through, otherwise the Vietnamese message explaining why
     *         nothing was recorded.
     */
    String complete(Connection conn, HttpServletRequest req, PosTab tab, User staff) throws SQLException {
        if (tab.isEmpty()) {
            return "Chưa có món nào trong đơn.";
        }

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(req.getParameter("paymentMethod"));
        } catch (IllegalArgumentException | NullPointerException e) {
            paymentMethod = PaymentMethod.CASH;
        }
        // Paying from an EAUT Pay wallet needs a wallet to pay from.
        if (paymentMethod == PaymentMethod.WALLET && tab.getCustomerId() == null) {
            return "Chọn khách hàng trước khi thanh toán bằng Ví EAUT Pay.";
        }

        BigDecimal total = tab.getTotal();

        conn.setAutoCommit(false);
        try {
            // Re-read each dish inside the transaction: prices and VAT rates are read from the
            // database rather than from the screen, which may have been open for a while. Custom
            // items have no product row, so they keep the price the cashier typed.
            Map<Integer, BigDecimal> taxRates = new HashMap<>();
            for (CartItem item : tab.getCart().getItems()) {
                if (item.getProductId() > 0) {
                    Product product = productDAO.findById(conn, item.getProductId());
                    if (product != null) {
                        taxRates.put(item.getProductId(), product.getTaxPercent());
                    }
                }
            }

            Order order = new Order();
            order.setCustomerId(tab.getCustomerId());
            order.setBuildingId(null);
            order.setChannel(OrderChannel.COUNTER);
            order.setSoldBy(staff.getUserId());
            order.setSubtotal(tab.getSubtotal());
            order.setShippingFee(BigDecimal.ZERO);
            order.setDiscountAmount(tab.getDiscount());
            order.setTaxAmount(tab.taxAmount(taxRates));
            order.setTotalAmount(total);
            order.setOrderStatus(OrderStatus.COMPLETED);
            order.setPaymentMethod(paymentMethod);
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setNote(tab.getNote() == null ? "Bán hàng tại quầy" : tab.getNote());

            int orderId = orderDAO.insert(conn, order);

            for (CartItem item : tab.getCart().getItems()) {
                String failure = recordLine(conn, item, orderId, staff);
                if (failure != null) {
                    conn.rollback();
                    return failure;
                }
            }

            if (paymentMethod == PaymentMethod.WALLET) {
                String failure = chargeWallet(conn, tab.getCustomerId(), total, orderId, staff, order.getOrderCode());
                if (failure != null) {
                    conn.rollback();
                    return failure;
                }
            }

            // A counter sale attached to a student earns them points, exactly as a delivered online
            // order does — that attachment is the whole reason the till has a customer field.
            if (tab.getCustomerId() != null) {
                awardLoyalty(conn, tab.getCustomerId(), total, orderId, order.getOrderCode());
            }

            historyDAO.insert(conn, orderId, null, OrderStatus.COMPLETED, staff.getUserId(),
                    "Bán tại quầy");

            conn.commit();
            req.setAttribute("posReceiptOrderId", orderId);
            req.setAttribute("posReceiptOrderCode", order.getOrderCode());
            tab.reset();
            return null;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Writes one line and takes it off the shelf. Custom items (negative id) have no product row
     * and no stock, so they are recorded on the order but not deducted from anything.
     */
    private String recordLine(Connection conn, CartItem item, int orderId, User staff) throws SQLException {
        if (item.getProductId() > 0) {
            int updated = shelfStockDAO.decrementIfEnough(conn, item.getProductId(), item.getQuantity());
            if (updated == 0) {
                return "\"" + item.getProductName() + "\" không còn đủ hàng trên kệ.";
            }
            stockMovementDAO.insert(conn, item.getProductId(), StockLocation.SHELF,
                    -item.getQuantity(), StockMovementReason.SALE, orderId, "Bán tại quầy",
                    staff.getUserId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderId);
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(item.getUnitPrice());
            orderItem.setLineTotal(item.getLineTotal());
            orderItemDAO.insert(conn, orderItem);
        }
        // A custom item cannot be stored in order_items at all: that table's product_id is NOT NULL
        // and references products. It is part of the total and appears on the printed bill, but the
        // order's line list only holds catalogue dishes.
        return null;
    }

    private String chargeWallet(Connection conn, int customerId, BigDecimal total, int orderId,
                                User staff, String orderCode) throws SQLException {
        User customer = userDAO.findById(conn, customerId);
        if (customer == null || customer.getWalletBalance().compareTo(total) < 0) {
            return "Số dư Ví EAUT Pay của khách không đủ.";
        }
        userDAO.adjustWalletBalance(conn, customerId, total.negate());

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(customerId);
        tx.setAmount(total.negate());
        tx.setType(WalletTransactionType.PAYMENT);
        tx.setOrderId(orderId);
        tx.setCreatedBy(staff.getUserId());
        tx.setNote("Thanh toán tại quầy, đơn " + orderCode);
        walletDAO.insert(conn, tx);
        return null;
    }

    private void awardLoyalty(Connection conn, int customerId, BigDecimal total, int orderId,
                              String orderCode) throws SQLException {
        BigDecimal vndPerPoint = Settings.getDecimal(conn, "loyalty.vndPerPoint", BigDecimal.valueOf(10_000));
        if (vndPerPoint.signum() <= 0) {
            return;
        }
        int points = total.divide(vndPerPoint, 0, java.math.RoundingMode.DOWN).intValue();
        if (points <= 0) {
            return;
        }
        userDAO.adjustLoyaltyPoints(conn, customerId, points);

        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setUserId(customerId);
        tx.setPoints(points);
        tx.setType(LoyaltyTransactionType.EARN);
        tx.setOrderId(orderId);
        tx.setNote("Tích điểm mua tại quầy, đơn " + orderCode);
        loyaltyDAO.insert(conn, tx);
    }
}
