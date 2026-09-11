package com.eaut.canteen.controller.sales;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.OrderItemDAO;
import com.eaut.canteen.dao.OrderStatusHistoryDAO;
import com.eaut.canteen.dao.ShopStatusDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.OrderItemDAOImpl;
import com.eaut.canteen.dao.impl.OrderStatusHistoryDAOImpl;
import com.eaut.canteen.dao.impl.ShopStatusDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.PaymentMethod;
import com.eaut.canteen.model.PaymentStatus;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.VietQRUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet({"/sales/orders", "/sales/orders/detail"})
public class OrderQueueServlet extends HttpServlet {

    private static final OrderDAO orderDAO = new OrderDAOImpl();
    private static final OrderItemDAO orderItemDAO = new OrderItemDAOImpl();
    private static final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAOImpl();
    private static final ShopStatusDAO shopStatusDAO = new ShopStatusDAOImpl();
    private static final UserDAO userDAO = new UserDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            if ("/sales/orders/detail".equals(req.getServletPath())) {
                showDetail(req, resp, conn);
            } else {
                showQueue(req, resp, conn);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showQueue(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        String statusParam = req.getParameter("status");
        req.setAttribute("pageTitle", "Đơn hàng");

        // Default view: a live-feeling 3-column board (PENDING/CONFIRMED/SHIPPING) instead of one
        // flat table filtered to a single status — the filter chips below still switch to the
        // flat table for a specific status or the full history ("ALL").
        if (statusParam == null || statusParam.isBlank()) {
            req.setAttribute("boardMode", true);
            req.setAttribute("pendingOrders", orderDAO.findByStatus(conn, OrderStatus.PENDING));
            req.setAttribute("confirmedOrders", orderDAO.findByStatus(conn, OrderStatus.CONFIRMED));
            req.setAttribute("shippingOrders", orderDAO.findByStatus(conn, OrderStatus.SHIPPING));
            req.setAttribute("selectedStatus", "");
            req.setAttribute("shopStatus", shopStatusDAO.get(conn));
            req.setAttribute("onDutyStaff", userDAO.findOnDutyStaff(conn));
        } else {
            req.setAttribute("boardMode", false);
            List<Order> orders = "ALL".equals(statusParam)
                    ? orderDAO.findAll(conn)
                    : orderDAO.findByStatus(conn, OrderStatus.valueOf(statusParam));
            req.setAttribute("orders", orders);
            req.setAttribute("selectedStatus", statusParam);
        }
        req.getRequestDispatcher("/WEB-INF/views/sales/order-queue.jsp").forward(req, resp);
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        int orderId = Integer.parseInt(req.getParameter("id"));
        Order order = orderDAO.findById(conn, orderId);
        if (order == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        req.setAttribute("pageTitle", "Đơn hàng " + order.getOrderCode());
        req.setAttribute("order", order);
        req.setAttribute("items", orderItemDAO.findByOrderId(conn, orderId));
        req.setAttribute("history", historyDAO.findByOrderId(conn, orderId));
        if (order.getPaymentMethod() == PaymentMethod.VIETQR && order.getPaymentStatus() == PaymentStatus.UNPAID) {
            req.setAttribute("qrImageUrl", VietQRUtil.qrImageUrl(orderId, order.getTotalAmount()));
            req.setAttribute("transferNote", VietQRUtil.transferNote(orderId));
        }
        req.getRequestDispatcher("/WEB-INF/views/sales/order-detail.jsp").forward(req, resp);
    }
}
