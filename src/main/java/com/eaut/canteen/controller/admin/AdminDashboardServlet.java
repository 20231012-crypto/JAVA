package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.dao.BuildingDAO;
import com.eaut.canteen.dao.CategoryDAO;
import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.BuildingDAOImpl;
import com.eaut.canteen.dao.impl.CategoryDAOImpl;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/admin")
public class AdminDashboardServlet extends HttpServlet {

    private static final ProductDAO productDAO = new ProductDAOImpl();
    private static final CategoryDAO categoryDAO = new CategoryDAOImpl();
    private static final BuildingDAO buildingDAO = new BuildingDAOImpl();
    private static final UserDAO userDAO = new UserDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Product> products = productDAO.findAllForAdmin(conn);
            long lowStockCount = products.stream().filter(p -> p.isActive() && p.getShelfQuantity() < 5).count();

            req.setAttribute("pageTitle", "Tổng quan");
            req.setAttribute("productCount", products.size());
            req.setAttribute("lowStockCount", lowStockCount);
            req.setAttribute("categoryCount", categoryDAO.findAllActive(conn).size());
            req.setAttribute("buildingCount", buildingDAO.findAllActive(conn).size());
            req.setAttribute("staffCount", userDAO.findAllStaff(conn).size());
            req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
