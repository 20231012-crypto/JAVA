package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.CategoryDAO;
import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.impl.CategoryDAOImpl;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.FileUploadUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@WebServlet({"/admin/products", "/admin/products/form", "/admin/products/save", "/admin/products/toggle"})
@MultipartConfig(maxFileSize = 5 * 1024 * 1024)
public class ProductServlet extends HttpServlet {

    private static final ProductDAO productDAO = new ProductDAOImpl();
    private static final CategoryDAO categoryDAO = new CategoryDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            if ("/admin/products/form".equals(req.getServletPath())) {
                showForm(req, resp, conn);
            } else {
                req.setAttribute("pageTitle", "Sản phẩm");
                req.setAttribute("products", productDAO.findAllForAdmin(conn));
                req.getRequestDispatcher("/WEB-INF/views/admin/products.jsp").forward(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        String idParam = req.getParameter("id");
        Product product = null;
        if (idParam != null && !idParam.isBlank()) {
            product = productDAO.findById(conn, Integer.parseInt(idParam));
        }
        req.setAttribute("pageTitle", product == null ? "Thêm sản phẩm" : "Sửa sản phẩm");
        req.setAttribute("product", product);
        req.setAttribute("categories", categoryDAO.findAllActive(conn));
        req.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            if ("/admin/products/toggle".equals(req.getServletPath())) {
                int productId = Integer.parseInt(req.getParameter("productId"));
                boolean active = Boolean.parseBoolean(req.getParameter("active"));
                productDAO.setActive(conn, productId, active);
                resp.sendRedirect(req.getContextPath() + "/admin/products");
                return;
            }
            saveProduct(req, resp, conn);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void saveProduct(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        String idParam = req.getParameter("productId");
        String name = req.getParameter("name");
        if (name == null || name.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/admin/products");
            return;
        }

        Product product = new Product();
        product.setName(name.trim());
        product.setDescription(req.getParameter("description"));
        product.setPrice(new BigDecimal(req.getParameter("price")));
        product.setUnit(req.getParameter("unit"));
        product.setCategoryId(Integer.parseInt(req.getParameter("categoryId")));
        String avgPrepMinutes = req.getParameter("avgPrepMinutes");
        product.setAvgPrepMinutes(avgPrepMinutes == null || avgPrepMinutes.isBlank() ? 10 : Integer.parseInt(avgPrepMinutes));

        boolean isNew = idParam == null || idParam.isBlank();
        if (isNew) {
            productDAO.insert(conn, product);
        } else {
            product.setProductId(Integer.parseInt(idParam));
            productDAO.update(conn, product);
        }

        Part imagePart = req.getPart("image");
        try {
            String savedFilename = FileUploadUtil.saveProductImage(imagePart);
            if (savedFilename != null) {
                productDAO.updateImage(conn, product.getProductId(), savedFilename);
            }
        } catch (IOException e) {
            req.setAttribute("pageTitle", isNew ? "Thêm sản phẩm" : "Sửa sản phẩm");
            req.setAttribute("product", product);
            req.setAttribute("categories", categoryDAO.findAllActive(conn));
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(req, resp);
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/admin/products");
    }
}
