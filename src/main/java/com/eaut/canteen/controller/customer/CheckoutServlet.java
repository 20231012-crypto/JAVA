package com.eaut.canteen.controller.customer;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.dao.BuildingDAO;
import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.OrderItemDAO;
import com.eaut.canteen.dao.OrderStatusHistoryDAO;
import com.eaut.canteen.dao.ShelfStockDAO;
import com.eaut.canteen.dao.impl.BuildingDAOImpl;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.OrderItemDAOImpl;
import com.eaut.canteen.dao.impl.OrderStatusHistoryDAOImpl;
import com.eaut.canteen.dao.impl.ShelfStockDAOImpl;
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
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet({"/checkout", "/checkout/place"})
public class CheckoutServlet extends HttpServlet {

    private final BuildingDAO buildingDAO = new BuildingDAOImpl();
    private final OrderDAO orderDAO = new OrderDAOImpl();
    private final OrderItemDAO orderItemDAO = new OrderItemDAOImpl();
    private final ShelfStockDAO shelfStockDAO = new ShelfStockDAOImpl();
    private final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Cart cart = getCart(req);
        if (cart.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/cart");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            List<Building> buildings = buildingDAO.findAllActive(conn);
            req.setAttribute("pageTitle", "Thanh toán");
            req.setAttribute("cart", cart);
            req.setAttribute("buildings", buildings);
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

        try (Connection conn = DBConnection.getConnection()) {
            Building building = buildingDAO.findById(conn, buildingId);
            if (building == null) {
                req.setAttribute("error", "Vui lòng chọn tòa nhà nhận hàng hợp lệ.");
                forwardToCheckout(req, resp, conn, cart);
                return;
            }

            conn.setAutoCommit(false);
            try {
                BigDecimal subtotal = cart.getSubtotal();
                BigDecimal shippingFee = building.getShippingFee();

                Order order = new Order();
                order.setCustomerId(customer.getUserId());
                order.setBuildingId(buildingId);
                order.setChannel(OrderChannel.ONLINE);
                order.setSubtotal(subtotal);
                order.setShippingFee(shippingFee);
                order.setTotalAmount(subtotal.add(shippingFee));
                order.setOrderStatus(OrderStatus.PENDING);
                order.setPaymentMethod(PaymentMethod.COD);
                order.setPaymentStatus(PaymentStatus.UNPAID);
                order.setNote(note);

                int orderId = orderDAO.insert(conn, order);

                for (CartItem cartItem : cart.getItems()) {
                    int updated = shelfStockDAO.decrementIfEnough(conn, cartItem.getProductId(), cartItem.getQuantity());
                    if (updated == 0) {
                        conn.rollback();
                        req.setAttribute("error", "Sản phẩm \"" + cartItem.getProductName() + "\" hiện không đủ hàng trên kệ.");
                        forwardToCheckout(req, resp, conn, cart);
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

    private void forwardToCheckout(HttpServletRequest req, HttpServletResponse resp, Connection conn, Cart cart)
            throws SQLException, ServletException, IOException {
        req.setAttribute("pageTitle", "Thanh toán");
        req.setAttribute("cart", cart);
        req.setAttribute("buildings", buildingDAO.findAllActive(conn));
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
