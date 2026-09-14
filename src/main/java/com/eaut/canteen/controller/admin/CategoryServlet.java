package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.CategoryDAO;
import com.eaut.canteen.dao.impl.CategoryDAOImpl;
import com.eaut.canteen.model.Category;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet({"/admin/categories", "/admin/categories/save", "/admin/categories/toggle"})
public class CategoryServlet extends HttpServlet {

    private static final CategoryDAO categoryDAO = new CategoryDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            req.setAttribute("pageTitle", "Danh mục sản phẩm");
            req.setAttribute("categories", categoryDAO.findAll(conn));
            req.getRequestDispatcher("/WEB-INF/views/admin/categories.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            if ("/admin/categories/toggle".equals(req.getServletPath())) {
                int categoryId = Integer.parseInt(req.getParameter("categoryId"));
                boolean active = Boolean.parseBoolean(req.getParameter("active"));
                categoryDAO.setActive(conn, categoryId, active);
            } else {
                saveCategory(req, conn);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/categories");
    }

    private void saveCategory(HttpServletRequest req, Connection conn) throws SQLException {
        String name = req.getParameter("name");
        if (name == null || name.isBlank()) {
            return;
        }
        String idParam = req.getParameter("categoryId");
        Category category = new Category();
        category.setName(name.trim());
        String parentIdParam = req.getParameter("parentCategoryId");
        category.setParentCategoryId(parentIdParam == null || parentIdParam.isBlank() ? null : Integer.parseInt(parentIdParam));

        if (idParam == null || idParam.isBlank()) {
            categoryDAO.insert(conn, category);
        } else {
            category.setCategoryId(Integer.parseInt(idParam));
            categoryDAO.update(conn, category);
        }
    }
}
