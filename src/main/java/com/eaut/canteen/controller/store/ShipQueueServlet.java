package com.eaut.canteen.controller.store;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Orders ready to pick (CONFIRMED) and orders out for delivery (SHIPPING), for store staff. */
@WebServlet("/store/orders")
public class ShipQueueServlet extends HttpServlet {

    private static final OrderDAO orderDAO = new OrderDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            req.setAttribute("pageTitle", "Đơn cần giao");
            req.setAttribute("confirmedOrders", orderDAO.findByStatus(conn, OrderStatus.CONFIRMED));
            req.setAttribute("shippingOrders", orderDAO.findByStatus(conn, OrderStatus.SHIPPING));
            req.getRequestDispatcher("/WEB-INF/views/store/ship-queue.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
