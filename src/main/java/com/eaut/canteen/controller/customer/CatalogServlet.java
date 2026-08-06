package com.eaut.canteen.controller.customer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.dao.CategoryDAO;
import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.impl.CategoryDAOImpl;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.model.Category;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet({"/products", "/products/detail"})
public class CatalogServlet extends HttpServlet {

    private final CategoryDAO categoryDAO = new CategoryDAOImpl();
    private final ProductDAO productDAO = new ProductDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            if ("/products/detail".equals(req.getServletPath())) {
                showDetail(req, resp, conn);
            } else {
                showList(req, resp, conn);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        List<Category> categories = categoryDAO.findAllActive(conn);

        String categoryParam = req.getParameter("category");
        List<Product> products;
        if (categoryParam != null && !categoryParam.isBlank()) {
            products = productDAO.findAllActiveByCategory(conn, Integer.parseInt(categoryParam));
            req.setAttribute("selectedCategory", Integer.parseInt(categoryParam));
        } else {
            products = productDAO.findAllActive(conn);
        }

        req.setAttribute("pageTitle", "Thực đơn");
        req.setAttribute("categories", categories);
        req.setAttribute("products", products);
        req.getRequestDispatcher("/WEB-INF/views/customer/catalog.jsp").forward(req, resp);
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        int productId = Integer.parseInt(req.getParameter("id"));
        Product product = productDAO.findById(conn, productId);

        if (product == null || !product.isActive()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        req.setAttribute("pageTitle", product.getName());
        req.setAttribute("product", product);
        req.getRequestDispatcher("/WEB-INF/views/customer/product-detail.jsp").forward(req, resp);
    }
}
