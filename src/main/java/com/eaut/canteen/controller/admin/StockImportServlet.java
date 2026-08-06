package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.StockImportDAO;
import com.eaut.canteen.dao.StockImportItemDAO;
import com.eaut.canteen.dao.WarehouseStockDAO;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.StockImportDAOImpl;
import com.eaut.canteen.dao.impl.StockImportItemDAOImpl;
import com.eaut.canteen.dao.impl.WarehouseStockDAOImpl;
import com.eaut.canteen.model.StockImport;
import com.eaut.canteen.model.StockImportItem;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet({"/admin/stock-imports", "/admin/stock-imports/save"})
public class StockImportServlet extends HttpServlet {

    private static final StockImportDAO stockImportDAO = new StockImportDAOImpl();
    private static final StockImportItemDAO stockImportItemDAO = new StockImportItemDAOImpl();
    private static final WarehouseStockDAO warehouseStockDAO = new WarehouseStockDAOImpl();
    private static final ProductDAO productDAO = new ProductDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            req.setAttribute("pageTitle", "Nhập hàng");
            req.setAttribute("imports", stockImportDAO.findAll(conn));
            req.setAttribute("products", productDAO.findAllActive(conn));
            req.getRequestDispatcher("/WEB-INF/views/admin/stock-imports.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User admin = (User) req.getSession().getAttribute("user");
        int productId = Integer.parseInt(req.getParameter("productId"));
        int quantity = Integer.parseInt(req.getParameter("quantity"));
        BigDecimal unitCost = new BigDecimal(req.getParameter("unitCost"));
        String supplierName = req.getParameter("supplierName");
        String note = req.getParameter("note");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                StockImport stockImport = new StockImport();
                stockImport.setAdminId(admin.getUserId());
                stockImport.setSupplierName(supplierName);
                stockImport.setNote(note);
                int importId = stockImportDAO.insert(conn, stockImport);

                StockImportItem item = new StockImportItem();
                item.setImportId(importId);
                item.setProductId(productId);
                item.setQuantity(quantity);
                item.setUnitCost(unitCost);
                stockImportItemDAO.insert(conn, item);

                warehouseStockDAO.increment(conn, productId, quantity);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        resp.sendRedirect(req.getContextPath() + "/admin/stock-imports");
    }
}
