package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.ShelfStockDAO;
import com.eaut.canteen.dao.StockImportDAO;
import com.eaut.canteen.dao.StockMovementDAO;
import com.eaut.canteen.dao.WarehouseStockDAO;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.ShelfStockDAOImpl;
import com.eaut.canteen.dao.impl.StockImportDAOImpl;
import com.eaut.canteen.dao.impl.StockMovementDAOImpl;
import com.eaut.canteen.dao.impl.WarehouseStockDAOImpl;
import com.eaut.canteen.model.Product;
import com.eaut.canteen.model.StockLocation;
import com.eaut.canteen.model.StockMovementReason;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Stock: what is on hand, what the warning level is, the ledger of everything that moved it, and
 * the two adjustments a human makes by hand.
 *
 * <p>Write-off and stock-take are the only reasons an admin may record directly. Every other
 * movement (sales, transfers, restocks, imports) is a side effect of an operation that actually
 * happened, so offering them here would let someone log a sale nobody made.
 *
 * <p>A stock-take sets an absolute count — the number someone just finished counting on the shelf —
 * and the ledger stores the difference. That is the right way round: the counter knows "there are
 * 14", not "there are 3 fewer than the computer thinks".
 */
@WebServlet({"/admin/inventory", "/admin/inventory/threshold", "/admin/inventory/adjust"})
public class InventoryServlet extends HttpServlet {

    private static final int LEDGER_PAGE_SIZE = 40;
    private static final int MAX_PAGE = 10_000;
    /** Guards against a typo adding a million portions in one keystroke. */
    private static final int MAX_ADJUSTMENT = 100_000;

    private static final ProductDAO productDAO = new ProductDAOImpl();
    private static final StockImportDAO stockImportDAO = new StockImportDAOImpl();
    private static final StockMovementDAO movementDAO = new StockMovementDAOImpl();
    private static final ShelfStockDAO shelfStockDAO = new ShelfStockDAOImpl();
    private static final WarehouseStockDAO warehouseStockDAO = new WarehouseStockDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            Integer productId = RequestParams.intOrNull(req.getParameter("productId"));
            StockMovementReason reason = parseReason(req.getParameter("reason"));
            int page = RequestParams.intInRange(req.getParameter("page"), 1, 1, MAX_PAGE);

            int total = movementDAO.countFiltered(conn, productId, reason, null, null);
            int totalPages = Math.max(1, (int) Math.ceil(total / (double) LEDGER_PAGE_SIZE));
            page = Math.min(page, totalPages);

            req.setAttribute("products", productDAO.findAllForAdmin(conn));
            req.setAttribute("lowStock", productDAO.findLowStock(conn));
            // "Hàng đang về" phân biệt "hết hàng, phải đặt gấp" với "hết hàng nhưng chiều nay
            // có" — trước đây trang này không có nên nhìn đâu cũng thấy phải đặt thêm.
            req.setAttribute("incoming", stockImportDAO.incomingByProduct(conn));
            req.setAttribute("movements",
                    movementDAO.findFiltered(conn, productId, reason, null, null,
                            LEDGER_PAGE_SIZE, (page - 1) * LEDGER_PAGE_SIZE));
            req.setAttribute("totalMovements", total);
            req.setAttribute("page", page);
            req.setAttribute("totalPages", totalPages);
            req.setAttribute("qProductId", req.getParameter("productId"));
            req.setAttribute("qReason", req.getParameter("reason"));
            req.setAttribute("reasons", StockMovementReason.values());
            req.setAttribute("pageTitle", "Tồn kho & sổ kho");
            req.getRequestDispatcher("/WEB-INF/views/admin/inventory.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User admin = (User) req.getSession().getAttribute("user");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String failure = "/admin/inventory/threshold".equals(req.getServletPath())
                        ? saveThreshold(conn, req)
                        : adjust(conn, req, admin);
                if (failure != null) {
                    conn.rollback();
                    req.getSession().setAttribute("actionError", failure);
                } else {
                    conn.commit();
                    req.getSession().setAttribute("actionMessage", "Đã cập nhật tồn kho.");
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

        resp.sendRedirect(req.getContextPath() + "/admin/inventory");
    }

    private String saveThreshold(Connection conn, HttpServletRequest req) throws SQLException {
        Integer productId = RequestParams.intOrNull(req.getParameter("productId"));
        Integer threshold = RequestParams.intOrNull(req.getParameter("threshold"));
        if (productId == null || threshold == null || threshold < 0) {
            return "Ngưỡng cảnh báo phải là số không âm.";
        }
        productDAO.updateLowStockThreshold(conn, productId, threshold);
        return null;
    }

    /**
     * Applies a write-off or a stock-take and records it. The counter and the ledger row are
     * written in one transaction — a ledger that can be missing entries stops being an explanation
     * of the count and becomes a second number to reconcile.
     */
    private String adjust(Connection conn, HttpServletRequest req, User admin) throws SQLException {
        Integer productId = RequestParams.intOrNull(req.getParameter("productId"));
        Integer amount = RequestParams.intOrNull(req.getParameter("amount"));
        StockMovementReason reason = parseReason(req.getParameter("reason"));
        StockLocation location = "WAREHOUSE".equals(req.getParameter("location"))
                ? StockLocation.WAREHOUSE : StockLocation.SHELF;
        String note = RequestParams.trimmedOrNull(req.getParameter("note"));

        if (productId == null || amount == null || reason == null || !reason.isManual()) {
            return "Thiếu thông tin hoặc loại điều chỉnh không hợp lệ.";
        }
        if (amount < 0 || amount > MAX_ADJUSTMENT) {
            return "Số lượng phải từ 0 đến " + MAX_ADJUSTMENT + ".";
        }

        Product product = productDAO.findById(conn, productId);
        if (product == null) {
            return "Không tìm thấy sản phẩm.";
        }
        int current = location == StockLocation.SHELF
                ? product.getShelfQuantity() : product.getWarehouseQuantity();

        int delta;
        if (reason == StockMovementReason.STOCK_TAKE) {
            // The form carries the counted total; the ledger carries the correction.
            delta = amount - current;
            if (delta == 0) {
                return "Số kiểm kê trùng với số hệ thống — không có gì để ghi nhận.";
            }
        } else {
            if (amount == 0) {
                return "Số lượng hủy phải lớn hơn 0.";
            }
            if (amount > current) {
                return "Không thể hủy " + amount + " khi chỉ còn " + current + ".";
            }
            delta = -amount;
        }

        applyDelta(conn, location, productId, delta);
        movementDAO.insert(conn, productId, location, delta, reason, null,
                note != null ? note : reason.getHint(), admin.getUserId());
        return null;
    }

    /** increment/decrementIfEnough are the only writers; the CHECK (quantity >= 0) is the backstop. */
    private void applyDelta(Connection conn, StockLocation location, int productId, int delta)
            throws SQLException {
        boolean shelf = location == StockLocation.SHELF;
        if (delta > 0) {
            if (shelf) {
                shelfStockDAO.increment(conn, productId, delta);
            } else {
                warehouseStockDAO.increment(conn, productId, delta);
            }
        } else {
            int affected = shelf
                    ? shelfStockDAO.decrementIfEnough(conn, productId, -delta)
                    : warehouseStockDAO.decrementIfEnough(conn, productId, -delta);
            if (affected == 0) {
                throw new SQLException("Tồn kho đã thay đổi trước khi điều chỉnh được ghi nhận.");
            }
        }
    }

    private StockMovementReason parseReason(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return StockMovementReason.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
