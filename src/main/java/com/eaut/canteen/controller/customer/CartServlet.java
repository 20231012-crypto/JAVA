package com.eaut.canteen.controller.customer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.model.Cart;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet({"/cart", "/cart/add", "/cart/update", "/cart/remove"})
public class CartServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("pageTitle", "Giỏ hàng");
        req.setAttribute("cart", getCart(req));
        req.getRequestDispatcher("/WEB-INF/views/customer/cart.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        Cart cart = getCart(req);

        try {
            switch (path) {
                case "/cart/add" -> addToCart(req, cart);
                case "/cart/update" -> cart.updateQuantity(
                        Integer.parseInt(req.getParameter("productId")),
                        Integer.parseInt(req.getParameter("quantity")));
                case "/cart/remove" -> cart.removeItem(Integer.parseInt(req.getParameter("productId")));
                default -> { }
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        resp.sendRedirect(req.getContextPath() + "/cart");
    }

    private void addToCart(HttpServletRequest req, Cart cart) throws SQLException {
        int productId = Integer.parseInt(req.getParameter("productId"));
        int quantity = Math.max(1, parseIntOrDefault(req.getParameter("quantity"), 1));

        try (Connection conn = DBConnection.getConnection()) {
            Product product = productDAO.findById(conn, productId);
            if (product != null && product.isActive()) {
                cart.addOrIncrement(productId, product.getName(), product.getPrice(),
                        product.getImageFilename(), quantity);
            }
        }
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
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
