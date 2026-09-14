package com.eaut.canteen.controller.customer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.FavoriteDAO;
import com.eaut.canteen.dao.impl.FavoriteDAOImpl;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Toggle a product in/out of the current customer's wishlist. AJAX-only (no non-JS fallback needed — it's a hover icon, not a form). */
@WebServlet("/favorites/toggle")
public class FavoriteServlet extends HttpServlet {

    private final FavoriteDAO favoriteDAO = new FavoriteDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || !user.getRole().isCustomerDefault()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        int productId;
        try {
            productId = Integer.parseInt(req.getParameter("productId"));
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean nowFavorited;
            if (favoriteDAO.isFavorited(conn, user.getUserId(), productId)) {
                favoriteDAO.remove(conn, user.getUserId(), productId);
                nowFavorited = false;
            } else {
                favoriteDAO.add(conn, user.getUserId(), productId);
                nowFavorited = true;
            }
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"favorited\":" + nowFavorited + "}");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
