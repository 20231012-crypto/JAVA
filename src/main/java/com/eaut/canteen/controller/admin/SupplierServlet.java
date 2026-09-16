package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.SupplierDAO;
import com.eaut.canteen.dao.impl.SupplierDAOImpl;
import com.eaut.canteen.model.Supplier;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Danh bạ nhà cung cấp.
 *
 * <p>Tồn tại vì phiếu nhập trước đây chỉ có một ô chữ tự do cho tên NCC, nên hệ thống không trả lời
 * được những câu hỏi cơ bản nhất của việc mua hàng: tháng này nhập bao nhiêu tiền hàng từ ai, và
 * còn nợ ai bao nhiêu.
 *
 * <p>Không có xóa cứng. Phiếu nhập cũ trỏ tới hàng này, và tên NCC trên chứng từ năm ngoái vẫn
 * phải đọc được — nên "xóa" ở đây là ngừng hoạt động, giống cách danh mục và tòa nhà đang làm.
 */
@WebServlet({"/admin/suppliers", "/admin/suppliers/save", "/admin/suppliers/toggle"})
public class SupplierServlet extends HttpServlet {

    private static final SupplierDAO supplierDAO = new SupplierDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            req.setAttribute("suppliers", supplierDAO.findAllWithStats(conn));
            req.setAttribute("pageTitle", "Nhà cung cấp");
            req.getRequestDispatcher("/WEB-INF/views/admin/suppliers.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            if ("/admin/suppliers/toggle".equals(req.getServletPath())) {
                toggle(conn, req);
            } else {
                save(conn, req);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/suppliers");
    }

    private void save(Connection conn, HttpServletRequest req) throws SQLException {
        String name = RequestParams.trimmedOrNull(req.getParameter("name"));
        if (name == null) {
            req.getSession().setAttribute("actionError", "Tên nhà cung cấp không được để trống.");
            return;
        }
        Integer supplierId = RequestParams.intOrNull(req.getParameter("supplierId"));
        if (supplierDAO.nameExists(conn, name, supplierId)) {
            req.getSession().setAttribute("actionError",
                    "Đã có nhà cung cấp tên \"" + name + "\". Dùng lại bản ghi đó thay vì tạo trùng.");
            return;
        }

        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setPhone(RequestParams.trimmedOrNull(req.getParameter("phone")));
        supplier.setEmail(RequestParams.trimmedOrNull(req.getParameter("email")));
        supplier.setAddress(RequestParams.trimmedOrNull(req.getParameter("address")));
        supplier.setNote(RequestParams.trimmedOrNull(req.getParameter("note")));

        if (supplierId == null) {
            supplierDAO.insert(conn, supplier);
            req.getSession().setAttribute("actionMessage", "Đã thêm nhà cung cấp " + name + ".");
        } else {
            supplier.setSupplierId(supplierId);
            supplierDAO.update(conn, supplier);
            req.getSession().setAttribute("actionMessage", "Đã cập nhật " + name + ".");
        }
    }

    private void toggle(Connection conn, HttpServletRequest req) throws SQLException {
        Integer supplierId = RequestParams.intOrNull(req.getParameter("supplierId"));
        if (supplierId == null) {
            return;
        }
        Supplier supplier = supplierDAO.findById(conn, supplierId);
        if (supplier == null) {
            return;
        }
        boolean next = !supplier.isActive();
        supplierDAO.setActive(conn, supplierId, next);
        req.getSession().setAttribute("actionMessage",
                (next ? "Đã bật lại " : "Đã ngừng ") + supplier.getName() + ".");
    }
}
