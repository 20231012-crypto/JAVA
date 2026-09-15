package com.eaut.canteen.controller.customer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.eaut.canteen.dao.BuildingDAO;
import com.eaut.canteen.dao.LoyaltyDAO;
import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.OrderItemDAO;
import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.OrderStatusHistoryDAO;
import com.eaut.canteen.dao.ShelfStockDAO;
import com.eaut.canteen.dao.ShopStatusDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.WalletDAO;
import com.eaut.canteen.dao.impl.BuildingDAOImpl;
import com.eaut.canteen.dao.impl.LoyaltyDAOImpl;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.OrderItemDAOImpl;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.OrderStatusHistoryDAOImpl;
import com.eaut.canteen.dao.impl.ShelfStockDAOImpl;
import com.eaut.canteen.dao.impl.ShopStatusDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.dao.impl.WalletDAOImpl;
import com.eaut.canteen.model.Building;
import com.eaut.canteen.model.Cart;
import com.eaut.canteen.model.CartItem;
import com.eaut.canteen.model.LoyaltyTransaction;
import com.eaut.canteen.model.LoyaltyTransactionType;
import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderChannel;
import com.eaut.canteen.model.OrderItem;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.PaymentMethod;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.model.PaymentStatus;
import com.eaut.canteen.model.User;
import com.eaut.canteen.model.WalletTransaction;
import com.eaut.canteen.model.WalletTransactionType;
import com.eaut.canteen.util.AppConfig;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet({"/checkout", "/checkout/place"})
public class CheckoutServlet extends HttpServlet {

    private static final BigDecimal DEFAULT_SMART_ID_DISCOUNT_PERCENT = BigDecimal.TEN;
    private static final BigDecimal DEFAULT_REDEEM_VALUE_PER_POINT = BigDecimal.valueOf(500);

    private final BuildingDAO buildingDAO = new BuildingDAOImpl();
    private final OrderDAO orderDAO = new OrderDAOImpl();
    private final OrderItemDAO orderItemDAO = new OrderItemDAOImpl();
    private final ProductDAO productDAO = new ProductDAOImpl();
    private final ShelfStockDAO shelfStockDAO = new ShelfStockDAOImpl();
    private final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();
    private final WalletDAO walletDAO = new WalletDAOImpl();
    private final LoyaltyDAO loyaltyDAO = new LoyaltyDAOImpl();
    private final ShopStatusDAO shopStatusDAO = new ShopStatusDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Cart cart = getCart(req);
        if (cart.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/cart");
            return;
        }

        User customer = (User) req.getSession().getAttribute("user");
        try (Connection conn = DBConnection.getConnection()) {
            // Re-read rather than trust the session copy — wallet/points can change in another tab.
            User fresh = userDAO.findById(conn, customer.getUserId());
            List<Building> buildings = buildingDAO.findAllActive(conn);
            req.setAttribute("pageTitle", "Thanh toán");
            req.setAttribute("cart", cart);
            req.setAttribute("buildings", buildings);
            if (fresh.isEautStudent()) {
                req.setAttribute("smartIdDiscount", smartIdDiscount(cart.getSubtotal()));
            }
            req.setAttribute("walletBalance", fresh.getWalletBalance());
            req.setAttribute("loyaltyPoints", fresh.getLoyaltyPoints());
            req.setAttribute("redeemValuePerPoint", redeemValuePerPoint());
            req.setAttribute("customerPhone", fresh.getPhone());
            req.setAttribute("customerStudentId", fresh.getStudentId());
            req.setAttribute("customerClassName", fresh.getClassName());
            req.setAttribute("customerIsEautStudent", fresh.isEautStudent());
            req.setAttribute("shopAcceptingOrders", shopStatusDAO.get(conn).isAcceptingOrders());
            req.getRequestDispatcher("/WEB-INF/views/customer/checkout.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Cart cart = getCart(req);
        if (cart.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/cart");
            return;
        }

        User sessionCustomer = (User) req.getSession().getAttribute("user");
        int buildingId = Integer.parseInt(req.getParameter("buildingId"));
        String note = req.getParameter("note");
        PaymentMethod paymentMethod = parsePaymentMethod(req.getParameter("paymentMethod"));
        boolean wantsRedeemPoints = req.getParameter("useLoyaltyPoints") != null;

        try (Connection conn = DBConnection.getConnection()) {
            // Re-read the customer's live wallet/points/phone rather than trusting the session
            // copy — stale if a top-up/previous purchase/the phone-number gate happened in
            // another tab since login.
            User customer = userDAO.findById(conn, sessionCustomer.getUserId());

            // Server-side backstop for the checkout info gate (phone, + MSSV/lớp for EAUT
            // students) — that gate is a blocking modal in normal use, but nothing stops a
            // request built by hand from skipping it.
            boolean missingPhone = customer.getPhone() == null || customer.getPhone().isBlank();
            boolean missingStudentInfo = customer.isEautStudent()
                    && (customer.getStudentId() == null || customer.getStudentId().isBlank());
            if (missingPhone || missingStudentInfo) {
                resp.sendRedirect(req.getContextPath() + "/checkout");
                return;
            }

            if (!shopStatusDAO.get(conn).isAcceptingOrders()) {
                req.setAttribute("error", "Căng tin đang tạm ngưng nhận đơn (quá tải hoặc ngoài giờ phục vụ). Vui lòng thử lại sau.");
                forwardToCheckout(req, resp, conn, cart, customer);
                return;
            }

            Building building = buildingDAO.findById(conn, buildingId);
            if (building == null) {
                req.setAttribute("error", "Vui lòng chọn tòa nhà nhận hàng hợp lệ.");
                forwardToCheckout(req, resp, conn, cart, customer);
                return;
            }

            // Price the order from the products table, not from the cart. The cart's prices were
            // captured when each item was added, so an admin price change (or a promotion starting
            // or ending) between then and checkout would otherwise be charged at the stale figure.
            // The cart decides which products and how many; the database decides what they cost.
            Map<Integer, Product> livePrices = new HashMap<>();
            for (CartItem cartItem : cart.getItems()) {
                Product live = productDAO.findById(conn, cartItem.getProductId());
                if (live == null || !live.isActive()) {
                    req.setAttribute("error", "Sản phẩm \"" + cartItem.getProductName()
                            + "\" không còn được bán. Vui lòng xóa khỏi giỏ hàng và đặt lại.");
                    forwardToCheckout(req, resp, conn, cart, customer);
                    return;
                }
                livePrices.put(cartItem.getProductId(), live);
            }

            BigDecimal subtotal = BigDecimal.ZERO;
            for (CartItem cartItem : cart.getItems()) {
                subtotal = subtotal.add(livePrices.get(cartItem.getProductId()).getPrice()
                        .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            }
            BigDecimal shippingFee = building.getShippingFee();
            BigDecimal smartIdDiscount = customer.isEautStudent() ? smartIdDiscount(subtotal) : BigDecimal.ZERO;
            BigDecimal payableBeforeLoyalty = subtotal.add(shippingFee).subtract(smartIdDiscount);

            BigDecimal redeemValue = redeemValuePerPoint();
            int maxRedeemable = payableBeforeLoyalty.divide(redeemValue, 0, RoundingMode.DOWN).intValue();
            int pointsUsed = wantsRedeemPoints ? Math.min(customer.getLoyaltyPoints(), maxRedeemable) : 0;
            BigDecimal loyaltyDiscount = redeemValue.multiply(BigDecimal.valueOf(pointsUsed));

            BigDecimal total = payableBeforeLoyalty.subtract(loyaltyDiscount);

            if (paymentMethod == PaymentMethod.WALLET && customer.getWalletBalance().compareTo(total) < 0) {
                req.setAttribute("error", "Số dư ví EAUT Pay không đủ để thanh toán đơn này.");
                forwardToCheckout(req, resp, conn, cart, customer);
                return;
            }

            conn.setAutoCommit(false);
            try {
                Order order = new Order();
                order.setCustomerId(customer.getUserId());
                order.setBuildingId(buildingId);
                order.setChannel(OrderChannel.ONLINE);
                order.setSubtotal(subtotal);
                order.setShippingFee(shippingFee);
                order.setDiscountAmount(smartIdDiscount);
                order.setLoyaltyPointsUsed(pointsUsed);
                order.setLoyaltyDiscountAmount(loyaltyDiscount);
                order.setTotalAmount(total);
                order.setOrderStatus(OrderStatus.PENDING);
                order.setPaymentMethod(paymentMethod);
                order.setPaymentStatus(paymentMethod == PaymentMethod.WALLET ? PaymentStatus.PAID : PaymentStatus.UNPAID);
                order.setNote(note);

                int orderId = orderDAO.insert(conn, order);

                if (pointsUsed > 0) {
                    userDAO.adjustLoyaltyPoints(conn, customer.getUserId(), -pointsUsed);
                    LoyaltyTransaction ltx = new LoyaltyTransaction();
                    ltx.setUserId(customer.getUserId());
                    ltx.setPoints(-pointsUsed);
                    ltx.setType(LoyaltyTransactionType.REDEEM);
                    ltx.setOrderId(orderId);
                    ltx.setNote("Đổi điểm cho đơn " + order.getOrderCode());
                    loyaltyDAO.insert(conn, ltx);
                }

                if (paymentMethod == PaymentMethod.WALLET) {
                    userDAO.adjustWalletBalance(conn, customer.getUserId(), total.negate());
                    WalletTransaction tx = new WalletTransaction();
                    tx.setUserId(customer.getUserId());
                    tx.setAmount(total.negate());
                    tx.setType(WalletTransactionType.PAYMENT);
                    tx.setOrderId(orderId);
                    tx.setNote("Thanh toán đơn " + order.getOrderCode());
                    walletDAO.insert(conn, tx);
                }

                for (CartItem cartItem : cart.getItems()) {
                    int updated = shelfStockDAO.decrementIfEnough(conn, cartItem.getProductId(), cartItem.getQuantity());
                    if (updated == 0) {
                        conn.rollback();
                        req.setAttribute("error", "Sản phẩm \"" + cartItem.getProductName() + "\" hiện không đủ hàng trên kệ.");
                        forwardToCheckout(req, resp, conn, cart, customer);
                        return;
                    }

                    OrderItem item = new OrderItem();
                    item.setOrderId(orderId);
                    item.setProductId(cartItem.getProductId());
                    item.setQuantity(cartItem.getQuantity());
                    BigDecimal unitPrice = livePrices.get(cartItem.getProductId()).getPrice();
                    item.setUnitPrice(unitPrice);
                    item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                    orderItemDAO.insert(conn, item);
                }

                historyDAO.insert(conn, orderId, null, OrderStatus.PENDING, customer.getUserId(), "Khách đặt hàng qua web");

                conn.commit();
                cart.clear();
                // The session's wallet/points are now stale after this order; refresh both so the
                // header badge (see nav.jsp) doesn't show pre-order numbers until re-login.
                sessionCustomer.setWalletBalance(customer.getWalletBalance().subtract(
                        paymentMethod == PaymentMethod.WALLET ? total : BigDecimal.ZERO));
                sessionCustomer.setLoyaltyPoints(customer.getLoyaltyPoints() - pointsUsed);
                resp.sendRedirect(req.getContextPath() + "/orders/detail?id=" + orderId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private PaymentMethod parsePaymentMethod(String value) {
        if ("VIETQR".equals(value)) {
            return PaymentMethod.VIETQR;
        }
        if ("WALLET".equals(value)) {
            return PaymentMethod.WALLET;
        }
        return PaymentMethod.COD;
    }

    /** EAUT Smart ID automatic discount on subtotal — see AppConfig "smartId.discountPercent" (default 10%). Rounded down to whole đồng. */
    private BigDecimal smartIdDiscount(BigDecimal subtotal) {
        String configured = AppConfig.get("smartId.discountPercent");
        BigDecimal percent = configured == null || configured.isBlank()
                ? DEFAULT_SMART_ID_DISCOUNT_PERCENT
                : new BigDecimal(configured.trim());
        return subtotal.multiply(percent).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
    }

    /** Tích điểm redemption rate — see AppConfig "loyalty.redeemValuePerPoint" (default 500đ/point). */
    private BigDecimal redeemValuePerPoint() {
        String configured = AppConfig.get("loyalty.redeemValuePerPoint");
        return configured == null || configured.isBlank()
                ? DEFAULT_REDEEM_VALUE_PER_POINT
                : new BigDecimal(configured.trim());
    }

    private void forwardToCheckout(HttpServletRequest req, HttpServletResponse resp, Connection conn, Cart cart, User customer)
            throws SQLException, ServletException, IOException {
        req.setAttribute("pageTitle", "Thanh toán");
        req.setAttribute("cart", cart);
        req.setAttribute("buildings", buildingDAO.findAllActive(conn));
        if (customer.isEautStudent()) {
            req.setAttribute("smartIdDiscount", smartIdDiscount(cart.getSubtotal()));
        }
        req.setAttribute("walletBalance", customer.getWalletBalance());
        req.setAttribute("loyaltyPoints", customer.getLoyaltyPoints());
        req.setAttribute("redeemValuePerPoint", redeemValuePerPoint());
        req.setAttribute("customerPhone", customer.getPhone());
        req.setAttribute("customerStudentId", customer.getStudentId());
        req.setAttribute("customerClassName", customer.getClassName());
        req.setAttribute("customerIsEautStudent", customer.isEautStudent());
        req.setAttribute("shopAcceptingOrders", shopStatusDAO.get(conn).isAcceptingOrders());
        req.getRequestDispatcher("/WEB-INF/views/customer/checkout.jsp").forward(req, resp);
    }

    private Cart getCart(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("cart", cart);
        }
        return cart;
    }
}
