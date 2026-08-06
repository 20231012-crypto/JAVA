package com.eaut.canteen.controller.store;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.OrderDAO;
import com.eaut.canteen.dao.OrderStatusHistoryDAO;
import com.eaut.canteen.dao.impl.OrderDAOImpl;
import com.eaut.canteen.dao.impl.OrderStatusHistoryDAOImpl;
import com.eaut.canteen.model.Order;
import com.eaut.canteen.model.OrderStatus;
import com.eaut.canteen.model.PaymentMethod;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Store staff order-fulfillment actions: pick (CONFIRMED -> SHIPPING) and complete (SHIPPING -> COMPLETED). */
@WebServlet({"/store/orders/pick", "/store/orders/complete"})
public class OrderFulfillmentServlet extends HttpServlet {

    private static final OrderDAO orderDAO = new OrderDAOImpl();
    private static final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int orderId = Integer.parseInt(req.getParameter("orderId"));
        User staff = (User) req.getSession().getAttribute("user");
        boolean isComplete = "/store/orders/complete".equals(req.getServletPath());

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean applied = isComplete
                        ? completeOrder(conn, orderId, staff)
                        : pickOrder(conn, orderId, staff);

                if (!applied) {
                    conn.rollback();
                    req.getSession().setAttribute("actionError", "Trạng thái đơn hàng đã thay đổi, vui lòng tải lại trang.");
                } else {
                    conn.commit();
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        resp.sendRedirect(req.getContextPath() + "/store/orders");
    }

    private boolean pickOrder(Connection conn, int orderId, User staff) throws SQLException {
        int updated = orderDAO.updateStatus(conn, orderId, OrderStatus.CONFIRMED, OrderStatus.SHIPPING);
        if (updated == 0) {
            return false;
        }
        historyDAO.insert(conn, orderId, OrderStatus.CONFIRMED, OrderStatus.SHIPPING, staff.getUserId(),
                "Nhân viên cửa hàng lấy hàng và bắt đầu giao");
        return true;
    }

    private boolean completeOrder(Connection conn, int orderId, User staff) throws SQLException {
        int updated = orderDAO.updateStatus(conn, orderId, OrderStatus.SHIPPING, OrderStatus.COMPLETED);
        if (updated == 0) {
            return false;
        }

        Order order = orderDAO.findById(conn, orderId);
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            orderDAO.markPaid(conn, orderId, staff.getUserId());
        }

        historyDAO.insert(conn, orderId, OrderStatus.SHIPPING, OrderStatus.COMPLETED, staff.getUserId(),
                "Giao hàng thành công");
        return true;
    }
}
