package com.eaut.canteen.controller.sales;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.ShopStatusDAO;
import com.eaut.canteen.dao.impl.ShopStatusDAOImpl;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Toggles the shop-wide "Mở đơn / Tạm ngưng nhận đơn" switch — see CheckoutServlet, which refuses new orders while paused. */
@WebServlet("/sales/shop-status")
public class ShopStatusServlet extends HttpServlet {

    private static final ShopStatusDAO shopStatusDAO = new ShopStatusDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User staff = (User) req.getSession().getAttribute("user");
        boolean accepting = "true".equals(req.getParameter("accepting"));

        try (Connection conn = DBConnection.getConnection()) {
            shopStatusDAO.setAcceptingOrders(conn, accepting, staff.getUserId());
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        String redirect = req.getParameter("redirect");
        resp.sendRedirect(req.getContextPath() + (redirect != null && redirect.startsWith("/") ? redirect : "/sales/orders"));
    }
}
