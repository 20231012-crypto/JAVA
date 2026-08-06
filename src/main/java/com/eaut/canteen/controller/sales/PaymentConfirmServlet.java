package com.eaut.canteen.controller.sales;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Sales staff manually confirms a VietQR bank transfer after checking it against their bank app. */
@WebServlet("/sales/orders/mark-paid")
public class PaymentConfirmServlet extends HttpServlet {

    private static final OrderDAO orderDAO = new OrderDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int orderId = Integer.parseInt(req.getParameter("orderId"));
        User staff = (User) req.getSession().getAttribute("user");

        try (Connection conn = DBConnection.getConnection()) {
            int updated = orderDAO.markPaid(conn, orderId, staff.getUserId());
            if (updated == 0) {
                req.getSession().setAttribute("actionError", "Đơn hàng đã được đánh dấu thanh toán trước đó.");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        resp.sendRedirect(req.getContextPath() + "/sales/orders/detail?id=" + orderId);
    }
}
