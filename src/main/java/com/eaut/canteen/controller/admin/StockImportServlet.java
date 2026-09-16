package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.StockImportDAO;
import com.eaut.canteen.dao.StockImportItemDAO;
import com.eaut.canteen.dao.SupplierDAO;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.StockImportDAOImpl;
import com.eaut.canteen.dao.impl.StockImportItemDAOImpl;
import com.eaut.canteen.dao.impl.SupplierDAOImpl;
import com.eaut.canteen.model.StockImport;
import com.eaut.canteen.model.StockImportFilter;
import com.eaut.canteen.model.StockImportItem;
import com.eaut.canteen.model.StockImportStatus;
import com.eaut.canteen.util.AppClock;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Nhập hàng: danh sách phiếu, form tạo/sửa, và trang chi tiết kiêm màn kiểm hàng.
 *
 * <p>Màn hình cũ là một form duy nhất: điền món và số lượng, bấm lưu, kho cộng ngay. Nó ghi lại
 * được việc nhập hàng nhưng không quản lý được việc nhập hàng — không có chỗ nào nói "đã đặt 10
 * thùng, chiều nay về", và không có bước nào để đối chiếu số đặt với số thực sự đếm được lúc hàng
 * xuống xe. Sổ sách vì thế luôn khớp với đơn đặt, kể cả khi trong kho không khớp.
 *
 * <p>Quy trình mới tách làm hai thời điểm, như Sapo: lập phiếu (kho chưa động) rồi kiểm hàng và
 * xác nhận (kho mới cộng đúng số đếm được). Phần ghi kho nằm ở {@link StockImportActionServlet}.
 */
@WebServlet({"/admin/stock-imports", "/admin/stock-imports/form", "/admin/stock-imports/detail"})
public class StockImportServlet extends HttpServlet {

    private static final int PAGE_SIZE = 20;
    private static final int MAX_PAGE = 10_000;

    private static final StockImportDAO stockImportDAO = new StockImportDAOImpl();
    private static final StockImportItemDAO stockImportItemDAO = new StockImportItemDAOImpl();
    private static final SupplierDAO supplierDAO = new SupplierDAOImpl();
    private static final ProductDAO productDAO = new ProductDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            switch (req.getServletPath()) {
                case "/admin/stock-imports/form" -> showForm(conn, req, resp);
                case "/admin/stock-imports/detail" -> showDetail(conn, req, resp);
                default -> showList(conn, req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showList(Connection conn, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException {
        StockImportFilter filter = readFilter(conn, req);
        int page = RequestParams.intInRange(req.getParameter("page"), 1, 1, MAX_PAGE);

        int total = stockImportDAO.countFiltered(conn, filter);
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        page = Math.min(page, totalPages);

        req.setAttribute("imports",
                stockImportDAO.findFiltered(conn, filter, PAGE_SIZE, (page - 1) * PAGE_SIZE));
        req.setAttribute("totalImports", total);
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("filter", filter);
        req.setAttribute("statusCounts", stockImportDAO.countByStatus(conn));
        req.setAttribute("suppliers", supplierDAO.findAllActive(conn));
        req.setAttribute("statuses", StockImportStatus.values());
        // Echo lại nguyên văn để form vẽ lại đúng thứ người dùng đã gõ, và để link phân trang mang
        // bộ lọc đi tiếp mà JSP không phải dựng lại từ đối tượng filter.
        req.setAttribute("qStatus", req.getParameter("status"));
        req.setAttribute("qSupplier", req.getParameter("supplier"));
        req.setAttribute("qFrom", req.getParameter("from"));
        req.setAttribute("qTo", req.getParameter("to"));
        req.setAttribute("qText", req.getParameter("q"));
        req.setAttribute("pageTitle", "Nhập hàng");
        req.getRequestDispatcher("/WEB-INF/views/admin/stock-imports.jsp").forward(req, resp);
    }

    /** Tạo mới khi không có id; sửa khi có — và chỉ khi phiếu còn ở trạng thái cho sửa. */
    private void showForm(Connection conn, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException {
        Integer importId = RequestParams.intOrNull(req.getParameter("id"));
        if (importId != null) {
            StockImport stockImport = stockImportDAO.findById(conn, importId);
            if (stockImport == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy phiếu nhập.");
                return;
            }
            if (!stockImport.getStatus().isEditable()) {
                // Chặn ngay ở đây chứ không chỉ ẩn nút trên danh sách: một link cũ hoặc một tab mở
                // từ trước vẫn dẫn tới đây được, và lúc đó phiếu đã cộng kho rồi.
                req.getSession().setAttribute("actionError",
                        "Phiếu " + stockImport.getCode() + " đã nhận hàng nên không sửa được nữa.");
                resp.sendRedirect(req.getContextPath() + "/admin/stock-imports/detail?id=" + importId);
                return;
            }
            req.setAttribute("stockImport", stockImport);
            req.setAttribute("items", stockImportItemDAO.findByImportId(conn, importId));
            req.setAttribute("pageTitle", "Sửa phiếu " + stockImport.getCode());
        } else {
            req.setAttribute("pageTitle", "Tạo phiếu nhập hàng");
        }
        req.setAttribute("suppliers", supplierDAO.findAllActive(conn));
        req.setAttribute("products", productDAO.findAllActive(conn));
        req.getRequestDispatcher("/WEB-INF/views/admin/stock-import-form.jsp").forward(req, resp);
    }

    private void showDetail(Connection conn, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException {
        Integer importId = RequestParams.intOrNull(req.getParameter("id"));
        if (importId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/stock-imports");
            return;
        }
        StockImport stockImport = stockImportDAO.findById(conn, importId);
        if (stockImport == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy phiếu nhập.");
            return;
        }
        List<StockImportItem> items = stockImportItemDAO.findByImportId(conn, importId);
        req.setAttribute("stockImport", stockImport);
        req.setAttribute("items", items);
        // Mở sẵn ô kiểm hàng khi phiếu còn nhận được, để người đang cầm hàng trên tay không phải
        // tìm thêm một nút nữa mới gõ được số.
        req.setAttribute("receiving", stockImport.getStatus().isOpen());
        req.setAttribute("pageTitle", "Phiếu " + stockImport.getCode());
        req.getRequestDispatcher("/WEB-INF/views/admin/stock-import-detail.jsp").forward(req, resp);
    }

    private StockImportFilter readFilter(Connection conn, HttpServletRequest req) throws SQLException {
        StockImportStatus status = null;
        String rawStatus = RequestParams.trimmedOrNull(req.getParameter("status"));
        if (rawStatus != null) {
            try {
                status = StockImportStatus.valueOf(rawStatus);
            } catch (IllegalArgumentException ignored) {
                // Giá trị lạ trong URL coi như không lọc, thay vì ném lỗi vào mặt người dùng.
            }
        }
        Integer supplierId = RequestParams.intOrNull(req.getParameter("supplier"));
        // Ngày người dùng gõ là ngày theo giờ căng tin; AppClock đổi sang múi giờ lưu trữ để bộ lọc
        // không lệch 7 tiếng như các báo cáo trước khi sửa múi giờ.
        LocalDate fromDate = parseDate(req.getParameter("from"));
        LocalDate toDate = parseDate(req.getParameter("to"));
        return new StockImportFilter(
                status,
                supplierId,
                fromDate == null ? null : AppClock.startOfDay(conn, fromDate),
                toDate == null ? null : AppClock.endOfDay(conn, toDate),
                RequestParams.trimmedOrNull(req.getParameter("q")));
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
}
