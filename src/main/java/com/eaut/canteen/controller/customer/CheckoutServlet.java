package com.eaut.canteen.controller.customer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.dao.BuildingDAO;
import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.OrderItemDAO;
import com.eaut.canteen.dao.OrderStatusHistoryDAO;
import com.eaut.canteen.dao.ShelfStockDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.WalletDAO;
import com.eaut.canteen.dao.impl.BuildingDAOImpl;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.OrderItemDAOImpl;
import com.eaut.canteen.dao.impl.OrderStatusHistoryDAOImpl;
import com.eaut.canteen.dao.impl.ShelfStockDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.dao.impl.WalletDAOImpl;
import com.eaut.canteen.model.Building;
import com.eaut.canteen.model.Cart;
import com.eaut.canteen.model.CartItem;
import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderChannel;
import com.eaut.canteen.model.OrderItem;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.PaymentMethod;
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

    private final BuildingDAO buildingDAO = new BuildingDAOImpl();
    private final OrderDAO orderDAO = new OrderDAOImpl();
    private final OrderItemDAO orderItemDAO = new OrderItemDAOImpl();
    private final ShelfStockDAO shelfStockDAO = new ShelfStockDAOImpl();
    private final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();
    private final WalletDAO walletDAO = new WalletDAOImpl();

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
            List<Building> buildings = buildingDAO.findAllActive(conn);
            req.setAttribute("pageTitle", "Thanh toán");
            req.setAttribute("cart", cart);
            req.setAttribute("buildings", buildings);
            if (customer.isEautStudent()) {
                req.setAttribute("smartIdDiscount", smartIdDiscount(cart.getSubtotal()));
            }
            req.setAttribute("walletBalance", customer.getWalletBalance());
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

        User customer = (User) req.getSession().getAttribute("user");
        int buildingId = Integer.parseInt(req.getParameter("buildingId"));
        String note = req.getParameter("note");
        PaymentMethod paymentMethod = parsePaymentMethod(req.getParameter("paymentMethod"));

        try (Connection conn = DBConnection.getConnection()) {
            Building building = buildingDAO.findById(conn, buildingId);
            if (building == null) {
                req.setAttribute("error", "Vui lòng chọn tòa nhà nhận hàng hợp lệ.");
                forwardToCheckout(req, resp, conn, cart, customer);
                return;
            }

            BigDecimal subtotal = cart.getSubtotal();
            BigDecimal shippingFee = building.getShippingFee();
            BigDecimal discount = customer.isEautStudent() ? smartIdDiscount(subtotal) : BigDecimal.ZERO;
            BigDecimal total = subtotal.add(shippingFee).subtract(discount);

            // Re-read the customer's live balance rather than trusting the session copy — it can
            // be stale if a top-up/previous purchase happened in another tab since login.
            User freshCustomer = paymentMethod == PaymentMethod.WALLET ? userDAO.findById(conn, customer.getUserId()) : null;
            if (paymentMethod == PaymentMethod.WALLET && freshCustomer.getWalletBalance().compareTo(total) < 0) {
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
                order.setDiscountAmount(discount);
                order.setTotalAmount(total);
                order.setOrderStatus(OrderStatus.PENDING);
                order.setPaymentMethod(paymentMethod);
                order.setPaymentStatus(paymentMethod == PaymentMethod.WALLET ? PaymentStatus.PAID : PaymentStatus.UNPAID);
                order.setNote(note);

                int orderId = orderDAO.insert(conn, order);

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
                    item.setUnitPrice(cartItem.getUnitPrice());
                    item.setLineTotal(cartItem.getLineTotal());
                    orderItemDAO.insert(conn, item);
                }

                historyDAO.insert(conn, orderId, null, OrderStatus.PENDING, customer.getUserId(), "Khách đặt hàng qua web");

                conn.commit();
                cart.clear();
                // The session's wallet balance is now stale after a WALLET payment; refresh it so
                // the header badge (see nav.jsp) doesn't show a pre-payment number until re-login.
                if (paymentMethod == PaymentMethod.WALLET) {
                    customer.setWalletBalance(customer.getWalletBalance().subtract(total));
                }
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

    private void forwardToCheckout(HttpServletRequest req, HttpServletResponse resp, Connection conn, Cart cart, User customer)
            throws SQLException, ServletException, IOException {
        req.setAttribute("pageTitle", "Thanh toán");
        req.setAttribute("cart", cart);
        req.setAttribute("buildings", buildingDAO.findAllActive(conn));
        if (customer.isEautStudent()) {
            req.setAttribute("smartIdDiscount", smartIdDiscount(cart.getSubtotal()));
        }
        req.setAttribute("walletBalance", customer.getWalletBalance());
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
