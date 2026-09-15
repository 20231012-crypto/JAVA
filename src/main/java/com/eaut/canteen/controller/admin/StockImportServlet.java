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
import com.eaut.canteen.dao.StockMovementDAO;
import com.eaut.canteen.dao.impl.StockMovementDAOImpl;
import com.eaut.canteen.model.StockLocation;
import com.eaut.canteen.model.StockMovementReason;
import com.eaut.canteen.util.DBConnection;
import java.util.List;
import java.util.ArrayList;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet({"/admin/stock-imports", "/admin/stock-imports/save"})
public class StockImportServlet extends HttpServlet {

    private static final StockImportDAO stockImportDAO = new StockImportDAOImpl();
    private static final StockMovementDAO stockMovementDAO = new StockMovementDAOImpl();
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
        // A delivery is a receipt with several lines on it. The form used to accept exactly one,
        // so a van arriving with eight products meant filling the form eight times and ending up
        // with eight unrelated receipts that no longer looked like one delivery.
        String[] productIds = req.getParameterValues("productId");
        String[] quantities = req.getParameterValues("quantity");
        String[] unitCosts = req.getParameterValues("unitCost");
        String supplierName = RequestParams.trimmedOrNull(req.getParameter("supplierName"));
        String note = RequestParams.trimmedOrNull(req.getParameter("note"));

        List<int[]> lines = new ArrayList<>();
        List<BigDecimal> costs = new ArrayList<>();
        if (productIds != null && quantities != null && unitCosts != null) {
            int count = Math.min(productIds.length, Math.min(quantities.length, unitCosts.length));
            for (int i = 0; i < count; i++) {
                Integer pid = RequestParams.intOrNull(productIds[i]);
                Integer qty = RequestParams.intOrNull(quantities[i]);
                BigDecimal cost = parseMoney(unitCosts[i]);
                // Blank rows are how the form offers spare lines, so they are skipped rather than
                // rejected — only a row someone actually filled in has to be valid.
                if (pid == null || qty == null || qty <= 0 || cost == null || cost.signum() < 0) {
                    continue;
                }
                lines.add(new int[]{pid, qty});
                costs.add(cost);
            }
        }

        if (lines.isEmpty()) {
            req.getSession().setAttribute("actionError",
                    "Cần ít nhất một dòng hợp lệ: chọn món, số lượng lớn hơn 0 và đơn giá.");
            resp.sendRedirect(req.getContextPath() + "/admin/stock-imports");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                StockImport stockImport = new StockImport();
                stockImport.setAdminId(admin.getUserId());
                stockImport.setSupplierName(supplierName);
                stockImport.setNote(note);
                int importId = stockImportDAO.insert(conn, stockImport);

                // One transaction for the whole receipt: a delivery that recorded four of its eight
                // lines would leave the warehouse count wrong with no way to tell which half landed.
                for (int i = 0; i < lines.size(); i++) {
                    int productId = lines.get(i)[0];
                    int quantity = lines.get(i)[1];

                    StockImportItem item = new StockImportItem();
                    item.setImportId(importId);
                    item.setProductId(productId);
                    item.setQuantity(quantity);
                    item.setUnitCost(costs.get(i));
                    stockImportItemDAO.insert(conn, item);

                    warehouseStockDAO.increment(conn, productId, quantity);
                    stockMovementDAO.insert(conn, productId, StockLocation.WAREHOUSE, quantity,
                            StockMovementReason.IMPORT, null,
                            supplierName == null ? "Nhập kho" : "Nhập từ " + supplierName,
                            admin.getUserId());
                }

                conn.commit();
                req.getSession().setAttribute("actionMessage",
                        "Đã nhập " + lines.size() + " dòng vào kho.");
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

    /** Accepts "12000" and "12.000" — staff type thousands separators out of habit. */
    private BigDecimal parseMoney(String raw) {
        String trimmed = RequestParams.trimmedOrNull(raw);
        if (trimmed == null) {
            return null;
        }
        try {
            return new BigDecimal(trimmed.replace(".", "").replace(",", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
