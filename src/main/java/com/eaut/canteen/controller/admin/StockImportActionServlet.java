package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.StockImportDAO;
import com.eaut.canteen.dao.StockImportItemDAO;
import com.eaut.canteen.dao.StockMovementDAO;
import com.eaut.canteen.dao.SupplierDAO;
import com.eaut.canteen.dao.WarehouseStockDAO;
import com.eaut.canteen.dao.impl.StockImportDAOImpl;
import com.eaut.canteen.dao.impl.StockImportItemDAOImpl;
import com.eaut.canteen.dao.impl.StockMovementDAOImpl;
import com.eaut.canteen.dao.impl.SupplierDAOImpl;
import com.eaut.canteen.dao.impl.WarehouseStockDAOImpl;
import com.eaut.canteen.model.StockImport;
import com.eaut.canteen.model.StockImportItem;
import com.eaut.canteen.model.StockImportStatus;
import com.eaut.canteen.model.StockLocation;
import com.eaut.canteen.model.StockMovementReason;
import com.eaut.canteen.model.Supplier;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Mọi thao tác làm thay đổi phiếu nhập hoặc kho.
 *
 * <p>Tách khỏi {@link StockImportServlet} vì đây là nơi tiền và hàng thực sự chuyển động, nên mỗi
 * hành động là một giao dịch có chốt trạng thái. Đọc và ghi ở hai lớp khác nhau cũng khiến quy tắc
 * "GET không được đổi dữ liệu" nhìn thấy được ngay từ cấu trúc file.
 */
@WebServlet({"/admin/stock-imports/save", "/admin/stock-imports/receive",
             "/admin/stock-imports/finish", "/admin/stock-imports/cancel",
             "/admin/stock-imports/delete", "/admin/stock-imports/pay"})
public class StockImportActionServlet extends HttpServlet {

    private static final StockImportDAO stockImportDAO = new StockImportDAOImpl();
    private static final StockImportItemDAO stockImportItemDAO = new StockImportItemDAOImpl();
    private static final StockMovementDAO stockMovementDAO = new StockMovementDAOImpl();
    private static final WarehouseStockDAO warehouseStockDAO = new WarehouseStockDAOImpl();
    private static final SupplierDAO supplierDAO = new SupplierDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User admin = (User) req.getSession().getAttribute("user");
        try (Connection conn = DBConnection.getConnection()) {
            switch (req.getServletPath()) {
                case "/admin/stock-imports/save" -> save(conn, req, resp, admin);
                case "/admin/stock-imports/receive" -> receive(conn, req, resp, admin);
                case "/admin/stock-imports/finish" -> finish(conn, req, resp, admin);
                case "/admin/stock-imports/cancel" -> cancel(conn, req, resp, admin);
                case "/admin/stock-imports/delete" -> delete(conn, req, resp);
                case "/admin/stock-imports/pay" -> pay(conn, req, resp);
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    // ============================================================
    // Tạo / sửa phiếu — không đụng tới kho
    // ============================================================

    private void save(Connection conn, HttpServletRequest req, HttpServletResponse resp, User admin)
            throws IOException, SQLException {
        Integer importId = RequestParams.intOrNull(req.getParameter("importId"));
        List<StockImportItem> lines = readLines(req);

        if (lines.isEmpty()) {
            fail(req, resp, "Phiếu phải có ít nhất một dòng: chọn món, số lượng lớn hơn 0 và đơn giá.",
                    importId == null ? "/admin/stock-imports/form"
                                     : "/admin/stock-imports/form?id=" + importId);
            return;
        }

        Integer supplierId = RequestParams.intOrNull(req.getParameter("supplierId"));
        String note = RequestParams.trimmedOrNull(req.getParameter("note"));
        LocalDate expected = parseDate(req.getParameter("expectedDate"));
        BigDecimal discount = parseMoneyOrZero(req.getParameter("discountAmount"));
        BigDecimal otherCost = parseMoneyOrZero(req.getParameter("otherCost"));
        BigDecimal paid = parseMoneyOrZero(req.getParameter("paidAmount"));

        conn.setAutoCommit(false);
        try {
            String supplierName = null;
            if (supplierId != null) {
                Supplier supplier = supplierDAO.findById(conn, supplierId);
                if (supplier == null) {
                    conn.rollback();
                    fail(req, resp, "Nhà cung cấp không tồn tại.", "/admin/stock-imports/form");
                    return;
                }
                // Chụp lại tên ngay lúc lập phiếu: đổi tên NCC về sau không được viết lại chứng từ
                // đã phát sinh, cùng lý do order_items giữ unit_price.
                supplierName = supplier.getName();
            }

            StockImport stockImport = new StockImport();
            stockImport.setAdminId(admin.getUserId());
            stockImport.setSupplierId(supplierId);
            stockImport.setSupplierName(supplierName);
            stockImport.setExpectedDate(expected);
            stockImport.setDiscountAmount(discount);
            stockImport.setOtherCost(otherCost);
            stockImport.setPaidAmount(paid);
            stockImport.setNote(note);

            int targetId;
            String message;
            if (importId == null) {
                stockImport.setStatus(StockImportStatus.DRAFT);
                targetId = stockImportDAO.insert(conn, stockImport);
                message = "Đã tạo phiếu " + stockImport.getCode()
                        + ". Kho chưa cộng — bấm Kiểm hàng khi hàng về.";
            } else {
                StockImport existing = stockImportDAO.findById(conn, importId);
                if (existing == null || !existing.getStatus().isEditable()) {
                    conn.rollback();
                    fail(req, resp, "Phiếu đã nhận hàng hoặc không còn tồn tại nên không sửa được.",
                            "/admin/stock-imports");
                    return;
                }
                stockImport.setImportId(importId);
                stockImportDAO.updateHeader(conn, stockImport);
                // Xóa hết rồi chèn lại đơn giản và đúng hơn là đối chiếu từng dòng một. An toàn vì
                // phiếu còn ở DRAFT nên chưa dòng nào sinh ra bút toán kho để mà mồ côi.
                stockImportItemDAO.deleteByImportId(conn, importId);
                targetId = importId;
                message = "Đã cập nhật phiếu " + existing.getCode() + ".";
            }

            for (StockImportItem line : lines) {
                line.setImportId(targetId);
                stockImportItemDAO.insert(conn, line);
            }

            conn.commit();
            req.getSession().setAttribute("actionMessage", message);
            resp.sendRedirect(req.getContextPath() + "/admin/stock-imports/detail?id=" + targetId);
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ============================================================
    // Kiểm hàng — đây là lúc kho thay đổi
    // ============================================================

    private void receive(Connection conn, HttpServletRequest req, HttpServletResponse resp, User admin)
            throws IOException, SQLException {
        Integer importId = RequestParams.intOrNull(req.getParameter("importId"));
        if (importId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/stock-imports");
            return;
        }

        conn.setAutoCommit(false);
        try {
            StockImport stockImport = stockImportDAO.findById(conn, importId);
            if (stockImport == null || !stockImport.getStatus().isOpen()) {
                conn.rollback();
                fail(req, resp, "Phiếu này đã chốt hoặc đã hủy, không nhận thêm hàng được.",
                        "/admin/stock-imports");
                return;
            }
            StockImportStatus before = stockImport.getStatus();
            List<StockImportItem> items = stockImportItemDAO.findByImportId(conn, importId);

            int changedLines = 0;
            int addedUnits = 0;
            boolean allReceived = true;

            for (StockImportItem item : items) {
                Integer typed = RequestParams.intOrNull(
                        req.getParameter("received_" + item.getImportItemId()));
                int newReceived = typed == null ? item.getReceivedQuantity() : typed;

                // Không cho nhận quá số đặt: gõ nhầm 100 thay vì 10 sẽ âm thầm thổi phồng kho, và
                // đây là màn hình mà người dùng vừa đếm hàng vừa gõ nên gõ nhầm là chuyện thường.
                if (newReceived > item.getQuantity()) {
                    conn.rollback();
                    fail(req, resp, "Món \"" + item.getProductName() + "\": số nhận ("
                            + newReceived + ") lớn hơn số đặt (" + item.getQuantity()
                            + "). Nếu nhà cung cấp giao dư, hãy lập một phiếu nhập khác cho phần dư.",
                            "/admin/stock-imports/detail?id=" + importId);
                    return;
                }
                // Cũng không cho giảm: giảm nghĩa là rút hàng khỏi kho, mà đó là nghiệp vụ kiểm kê
                // hoặc hủy hàng — cả hai đã có ở /admin/inventory và đều ghi lý do riêng vào sổ kho.
                if (newReceived < item.getReceivedQuantity()) {
                    conn.rollback();
                    fail(req, resp, "Món \"" + item.getProductName() + "\": đã nhận "
                            + item.getReceivedQuantity() + ", không giảm xuống " + newReceived
                            + " được. Hàng đã vào kho thì phải chỉnh bằng Kiểm kê hoặc Hủy hàng.",
                            "/admin/stock-imports/detail?id=" + importId);
                    return;
                }

                int delta = newReceived - item.getReceivedQuantity();
                if (delta > 0) {
                    int updated = stockImportItemDAO.updateReceived(
                            conn, item.getImportItemId(), item.getReceivedQuantity(), newReceived);
                    if (updated == 0) {
                        // Ai đó vừa kiểm chính dòng này ở tab khác. Bỏ cả giao dịch thay vì cộng kho
                        // chồng lên phần họ đã cộng.
                        conn.rollback();
                        fail(req, resp, "Phiếu vừa được người khác cập nhật. Hãy tải lại và kiểm lại.",
                                "/admin/stock-imports/detail?id=" + importId);
                        return;
                    }
                    warehouseStockDAO.increment(conn, item.getProductId(), delta);
                    stockMovementDAO.insert(conn, item.getProductId(), StockLocation.WAREHOUSE, delta,
                            StockMovementReason.IMPORT, null,
                            "Phiếu " + stockImport.getCode()
                                    + (stockImport.getSupplierName() == null ? ""
                                        : " — " + stockImport.getSupplierName()),
                            admin.getUserId());
                    changedLines++;
                    addedUnits += delta;
                }
                if (newReceived < item.getQuantity()) {
                    allReceived = false;
                }
            }

            if (changedLines == 0) {
                conn.rollback();
                fail(req, resp, "Chưa có dòng nào thay đổi số nhận, nên không có gì để nhập kho.",
                        "/admin/stock-imports/detail?id=" + importId);
                return;
            }

            StockImportStatus next = allReceived ? StockImportStatus.RECEIVED : StockImportStatus.PARTIAL;
            if (stockImportDAO.updateStatus(conn, importId, before, next, admin.getUserId()) == 0) {
                conn.rollback();
                fail(req, resp, "Phiếu vừa đổi trạng thái ở nơi khác. Hãy tải lại trang.",
                        "/admin/stock-imports/detail?id=" + importId);
                return;
            }

            conn.commit();
            req.getSession().setAttribute("actionMessage",
                    "Đã nhập kho " + addedUnits + " sản phẩm từ " + changedLines + " dòng."
                    + (next == StockImportStatus.PARTIAL
                        ? " Phiếu còn hàng đang về nên vẫn để mở." : " Phiếu đã hoàn tất."));
            resp.sendRedirect(req.getContextPath() + "/admin/stock-imports/detail?id=" + importId);
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ============================================================
    // Chốt / hủy / xóa / thanh toán
    // ============================================================

    /** "Kết thúc": phần còn thiếu sẽ không về nữa, đóng phiếu mà không cộng thêm kho. */
    private void finish(Connection conn, HttpServletRequest req, HttpServletResponse resp, User admin)
            throws IOException, SQLException {
        Integer importId = RequestParams.intOrNull(req.getParameter("importId"));
        if (importId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/stock-imports");
            return;
        }
        StockImport stockImport = stockImportDAO.findById(conn, importId);
        if (stockImport == null || stockImport.getStatus() != StockImportStatus.PARTIAL) {
            fail(req, resp, "Chỉ phiếu đang nhập dở mới kết thúc được.", "/admin/stock-imports");
            return;
        }
        if (stockImportDAO.updateStatus(conn, importId, StockImportStatus.PARTIAL,
                StockImportStatus.RECEIVED, admin.getUserId()) == 0) {
            fail(req, resp, "Phiếu vừa đổi trạng thái ở nơi khác.",
                    "/admin/stock-imports/detail?id=" + importId);
            return;
        }
        req.getSession().setAttribute("actionMessage",
                "Đã kết thúc phiếu " + stockImport.getCode() + ". Phần hàng còn thiếu không tính là đang về nữa.");
        resp.sendRedirect(req.getContextPath() + "/admin/stock-imports/detail?id=" + importId);
    }

    private void cancel(Connection conn, HttpServletRequest req, HttpServletResponse resp, User admin)
            throws IOException, SQLException {
        Integer importId = RequestParams.intOrNull(req.getParameter("importId"));
        if (importId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/stock-imports");
            return;
        }
        StockImport stockImport = stockImportDAO.findById(conn, importId);
        if (stockImport == null || stockImport.getStatus() != StockImportStatus.DRAFT) {
            // Chỉ hủy được phiếu chưa nhận món nào. Phiếu đã nhận một phần mà hủy thì số hàng đã
            // vào kho sẽ không còn chứng từ nào giải thích — muốn đóng thì dùng "Kết thúc".
            fail(req, resp, "Chỉ hủy được phiếu chưa nhận hàng. Phiếu đã nhận một phần thì dùng Kết thúc.",
                    "/admin/stock-imports");
            return;
        }
        if (stockImportDAO.updateStatus(conn, importId, StockImportStatus.DRAFT,
                StockImportStatus.CANCELLED, admin.getUserId()) == 0) {
            fail(req, resp, "Phiếu vừa đổi trạng thái ở nơi khác.",
                    "/admin/stock-imports/detail?id=" + importId);
            return;
        }
        req.getSession().setAttribute("actionMessage", "Đã hủy phiếu " + stockImport.getCode() + ".");
        resp.sendRedirect(req.getContextPath() + "/admin/stock-imports/detail?id=" + importId);
    }

    private void delete(Connection conn, HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SQLException {
        Integer importId = RequestParams.intOrNull(req.getParameter("importId"));
        if (importId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/stock-imports");
            return;
        }
        StockImport stockImport = stockImportDAO.findById(conn, importId);
        if (stockImport == null || !stockImport.getStatus().isDeletable()) {
            fail(req, resp, "Phiếu đã nhận hàng là chứng từ kho, chỉ xem lại được chứ không xóa.",
                    "/admin/stock-imports");
            return;
        }
        stockImportDAO.delete(conn, importId);
        req.getSession().setAttribute("actionMessage", "Đã xóa phiếu " + stockImport.getCode() + ".");
        resp.sendRedirect(req.getContextPath() + "/admin/stock-imports");
    }

    private void pay(Connection conn, HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SQLException {
        Integer importId = RequestParams.intOrNull(req.getParameter("importId"));
        if (importId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/stock-imports");
            return;
        }
        StockImport stockImport = stockImportDAO.findById(conn, importId);
        if (stockImport == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        BigDecimal paid = parseMoneyOrZero(req.getParameter("paidAmount"));
        // Chặn trên ở tổng phải trả: trả dư nhà cung cấp là một nghiệp vụ khác (tạm ứng), không
        // phải thứ nên lặng lẽ xuất hiện ở đây dưới dạng công nợ âm.
        if (paid.compareTo(stockImport.getTotal()) > 0) {
            paid = stockImport.getTotal();
        }
        stockImportDAO.updatePayment(conn, importId, paid);
        req.getSession().setAttribute("actionMessage", "Đã cập nhật thanh toán cho phiếu "
                + stockImport.getCode() + ".");
        resp.sendRedirect(req.getContextPath() + "/admin/stock-imports/detail?id=" + importId);
    }

    // ============================================================
    // Đọc tham số
    // ============================================================

    /** Dòng trống là cách form mời thêm hàng, nên bỏ qua; chỉ dòng có người điền mới phải hợp lệ. */
    private List<StockImportItem> readLines(HttpServletRequest req) {
        String[] productIds = req.getParameterValues("productId");
        String[] quantities = req.getParameterValues("quantity");
        String[] unitCosts = req.getParameterValues("unitCost");

        List<StockImportItem> lines = new ArrayList<>();
        if (productIds == null || quantities == null || unitCosts == null) {
            return lines;
        }
        int count = Math.min(productIds.length, Math.min(quantities.length, unitCosts.length));
        for (int i = 0; i < count; i++) {
            Integer productId = RequestParams.intOrNull(productIds[i]);
            Integer quantity = RequestParams.intOrNull(quantities[i]);
            BigDecimal cost = parseMoney(unitCosts[i]);
            if (productId == null || quantity == null || quantity <= 0 || cost == null || cost.signum() < 0) {
                continue;
            }
            StockImportItem item = new StockImportItem();
            item.setProductId(productId);
            item.setQuantity(quantity);
            item.setUnitCost(cost);
            item.setReceivedQuantity(0);
            lines.add(item);
        }
        return lines;
    }

    /** Nhận cả "12000" lẫn "12.000" — nhân viên gõ dấu phân cách hàng nghìn theo thói quen. */
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

    private BigDecimal parseMoneyOrZero(String raw) {
        BigDecimal value = parseMoney(raw);
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private LocalDate parseDate(String raw) {
        String trimmed = RequestParams.trimmedOrNull(raw);
        if (trimmed == null) {
            return null;
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void fail(HttpServletRequest req, HttpServletResponse resp, String message, String path)
            throws IOException {
        req.getSession().setAttribute("actionError", message);
        resp.sendRedirect(req.getContextPath() + path);
    }
}
