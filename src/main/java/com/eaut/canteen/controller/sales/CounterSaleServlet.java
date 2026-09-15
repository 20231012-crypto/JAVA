package com.eaut.canteen.controller.sales;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

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
import com.eaut.canteen.model.Cart;
import com.eaut.canteen.model.CartItem;
import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderChannel;
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
import jakarta.servlet.http.HttpSession;

@WebServlet({"/sales/counter-sale", "/sales/counter-sale/add", "/sales/counter-sale/remove", "/sales/counter-sale/complete"})
public class CounterSaleServlet extends HttpServlet {

    private static final ProductDAO productDAO = new ProductDAOImpl();
    private static final OrderDAO orderDAO = new OrderDAOImpl();
    private static final OrderItemDAO orderItemDAO = new OrderItemDAOImpl();
    private static final StockMovementDAO stockMovementDAO = new StockMovementDAOImpl();
    private static final ShelfStockDAO shelfStockDAO = new ShelfStockDAOImpl();
    private static final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            req.setAttribute("pageTitle", "Bán hàng tại quầy");
            req.setAttribute("products", productDAO.findAllActive(conn));
            req.setAttribute("cart", getCounterCart(req));
            req.getRequestDispatcher("/WEB-INF/views/sales/counter-sale.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        Cart cart = getCounterCart(req);

        try {
            switch (path) {
                case "/sales/counter-sale/add" -> addToCart(req, cart);
                case "/sales/counter-sale/remove" -> cart.removeItem(Integer.parseInt(req.getParameter("productId")));
                case "/sales/counter-sale/complete" -> {
                    completeSale(req, resp, cart);
                    return;
                }
                default -> { }
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        resp.sendRedirect(req.getContextPath() + "/sales/counter-sale");
    }

    private void addToCart(HttpServletRequest req, Cart cart) throws SQLException {
        int productId = Integer.parseInt(req.getParameter("productId"));
        int quantity = Math.max(1, parseIntOrDefault(req.getParameter("quantity"), 1));

        try (Connection conn = DBConnection.getConnection()) {
            Product product = productDAO.findById(conn, productId);
            if (product != null && product.isActive()) {
                cart.addOrIncrement(productId, product.getName(), product.getPrice(), product.getImageFilename(), quantity);
            }
        }
    }

    private void completeSale(HttpServletRequest req, HttpServletResponse resp, Cart cart)
            throws ServletException, IOException {
        if (cart.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/sales/counter-sale");
            return;
        }

        User staff = (User) req.getSession().getAttribute("user");
        PaymentMethod paymentMethod = PaymentMethod.valueOf(req.getParameter("paymentMethod"));

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Order order = new Order();
                order.setCustomerId(null);
                order.setBuildingId(null);
                order.setChannel(OrderChannel.COUNTER);
                order.setSoldBy(staff.getUserId());
                order.setSubtotal(cart.getSubtotal());
                order.setShippingFee(java.math.BigDecimal.ZERO);
                order.setTotalAmount(cart.getSubtotal());
                order.setOrderStatus(OrderStatus.COMPLETED);
                order.setPaymentMethod(paymentMethod);
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setNote("Bán hàng tại quầy");

                int orderId = orderDAO.insert(conn, order);

                for (CartItem cartItem : cart.getItems()) {
                    int updated = shelfStockDAO.decrementIfEnough(conn, cartItem.getProductId(), cartItem.getQuantity());
                    if (updated > 0) {
                        stockMovementDAO.insert(conn, cartItem.getProductId(), StockLocation.SHELF,
                                -cartItem.getQuantity(), StockMovementReason.SALE, orderId,
                                "Bán tại quầy", staff.getUserId());
                    }
                    if (updated == 0) {
                        conn.rollback();
                        req.getSession().setAttribute("actionError",
                                "Sản phẩm \"" + cartItem.getProductName() + "\" hiện không đủ hàng trên kệ.");
                        resp.sendRedirect(req.getContextPath() + "/sales/counter-sale");
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

                historyDAO.insert(conn, orderId, null, OrderStatus.COMPLETED, staff.getUserId(), "Bán hàng tại quầy");

                conn.commit();
                cart.clear();
                resp.sendRedirect(req.getContextPath() + "/sales/orders/detail?id=" + orderId);
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

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    private Cart getCounterCart(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        Cart cart = (Cart) session.getAttribute("counterCart");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("counterCart", cart);
        }
        return cart;
    }
}
