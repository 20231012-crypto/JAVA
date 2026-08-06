package com.eaut.canteen.controller.sales;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.OrderItemDAO;
import com.eaut.canteen.dao.OrderStatusHistoryDAO;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.OrderItemDAOImpl;
import com.eaut.canteen.dao.impl.OrderStatusHistoryDAOImpl;
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
        List<Order> orders;
        String selectedStatus;

        if (statusParam == null || statusParam.isBlank() || "PENDING".equals(statusParam)) {
            orders = orderDAO.findByStatus(conn, OrderStatus.PENDING);
            selectedStatus = "PENDING";
        } else if ("ALL".equals(statusParam)) {
            orders = orderDAO.findAll(conn);
            selectedStatus = "ALL";
        } else {
            orders = orderDAO.findByStatus(conn, OrderStatus.valueOf(statusParam));
            selectedStatus = statusParam;
        }

        req.setAttribute("pageTitle", "Đơn hàng");
        req.setAttribute("orders", orders);
        req.setAttribute("selectedStatus", selectedStatus);
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

        req.setAttribute("pageTitle", "Đơn hàng #" + order.getOrderId());
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
