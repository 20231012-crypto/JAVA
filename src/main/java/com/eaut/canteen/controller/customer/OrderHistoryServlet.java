package com.eaut.canteen.controller.customer;

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
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet({"/orders", "/orders/detail"})
public class OrderHistoryServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAOImpl();
    private final OrderItemDAO orderItemDAO = new OrderItemDAOImpl();
    private final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User customer = (User) req.getSession().getAttribute("user");

        try (Connection conn = DBConnection.getConnection()) {
            if ("/orders/detail".equals(req.getServletPath())) {
                showDetail(req, resp, conn, customer);
            } else {
                showList(req, resp, conn, customer);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp, Connection conn, User customer)
            throws SQLException, ServletException, IOException {
        List<Order> orders = orderDAO.findByCustomer(conn, customer.getUserId());
        req.setAttribute("pageTitle", "Đơn hàng của tôi");
        req.setAttribute("orders", orders);
        req.getRequestDispatcher("/WEB-INF/views/customer/order-history.jsp").forward(req, resp);
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp, Connection conn, User customer)
            throws SQLException, ServletException, IOException {
        int orderId = Integer.parseInt(req.getParameter("id"));
        Order order = orderDAO.findById(conn, orderId);

        // Ownership check: a customer may only ever view their own orders by ID.
        if (order == null || order.getCustomerId() == null || order.getCustomerId() != customer.getUserId()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        req.setAttribute("pageTitle", "Đơn hàng #" + order.getOrderId());
        req.setAttribute("order", order);
        req.setAttribute("items", orderItemDAO.findByOrderId(conn, orderId));
        req.setAttribute("history", historyDAO.findByOrderId(conn, orderId));
        req.getRequestDispatcher("/WEB-INF/views/customer/order-detail.jsp").forward(req, resp);
    }
}
