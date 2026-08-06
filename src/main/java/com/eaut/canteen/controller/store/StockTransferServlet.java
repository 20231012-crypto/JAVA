package com.eaut.canteen.controller.store;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.ProductDAO;
import com.eaut.canteen.dao.ShelfStockDAO;
import com.eaut.canteen.dao.StockTransferDAO;
import com.eaut.canteen.dao.WarehouseStockDAO;
import com.eaut.canteen.dao.impl.ProductDAOImpl;
import com.eaut.canteen.dao.impl.ShelfStockDAOImpl;
import com.eaut.canteen.dao.impl.StockTransferDAOImpl;
import com.eaut.canteen.dao.impl.WarehouseStockDAOImpl;
import com.eaut.canteen.model.StockTransfer;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet({"/store/transfers", "/store/transfers/save"})
public class StockTransferServlet extends HttpServlet {

    private static final ProductDAO productDAO = new ProductDAOImpl();
    private static final WarehouseStockDAO warehouseStockDAO = new WarehouseStockDAOImpl();
    private static final ShelfStockDAO shelfStockDAO = new ShelfStockDAOImpl();
    private static final StockTransferDAO stockTransferDAO = new StockTransferDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            req.setAttribute("pageTitle", "Chuyển hàng lên kệ");
            req.setAttribute("products", productDAO.findAllActive(conn));
            req.setAttribute("transfers", stockTransferDAO.findRecent(conn, 20));
            req.getRequestDispatcher("/WEB-INF/views/store/stock-transfer.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int productId = Integer.parseInt(req.getParameter("productId"));
        int quantity = Integer.parseInt(req.getParameter("quantity"));
        User staff = (User) req.getSession().getAttribute("user");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int updated = warehouseStockDAO.decrementIfEnough(conn, productId, quantity);
                if (updated == 0) {
                    conn.rollback();
                    req.getSession().setAttribute("actionError", "Kho không đủ hàng để chuyển lên kệ.");
                    resp.sendRedirect(req.getContextPath() + "/store/transfers");
                    return;
                }

                shelfStockDAO.increment(conn, productId, quantity);

                StockTransfer transfer = new StockTransfer();
                transfer.setStoreStaffId(staff.getUserId());
                transfer.setProductId(productId);
                transfer.setQuantity(quantity);
                stockTransferDAO.insert(conn, transfer);

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

        resp.sendRedirect(req.getContextPath() + "/store/transfers");
    }
}
