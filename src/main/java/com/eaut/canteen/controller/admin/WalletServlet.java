package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.WalletDAO;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.dao.impl.WalletDAOImpl;
import com.eaut.canteen.model.User;
import com.eaut.canteen.model.WalletTransaction;
import com.eaut.canteen.model.WalletTransactionType;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Admin-only EAUT Pay top-up screen: find a customer by username or email, credit their wallet, see their ledger. */
@WebServlet({"/admin/wallet", "/admin/wallet/topup"})
public class WalletServlet extends HttpServlet {

    private static final UserDAO userDAO = new UserDAOImpl();
    private static final WalletDAO walletDAO = new WalletDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            showForm(req, resp, conn, req.getParameter("q"), null);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int userId = Integer.parseInt(req.getParameter("userId"));
        String amountParam = req.getParameter("amount");
        User admin = (User) req.getSession().getAttribute("user");

        try (Connection conn = DBConnection.getConnection()) {
            User target = userDAO.findById(conn, userId);
            BigDecimal amount = parseAmount(amountParam);

            if (target == null || !target.getRole().isCustomerDefault() || amount == null || amount.signum() <= 0) {
                showForm(req, resp, conn, target == null ? null : target.getUsername(), "Số tiền nạp không hợp lệ.");
                return;
            }

            conn.setAutoCommit(false);
            try {
                userDAO.adjustWalletBalance(conn, userId, amount);
                WalletTransaction tx = new WalletTransaction();
                tx.setUserId(userId);
                tx.setAmount(amount);
                tx.setType(WalletTransactionType.TOPUP);
                tx.setCreatedBy(admin.getUserId());
                tx.setNote("Admin " + admin.getUsername() + " nạp ví thủ công");
                walletDAO.insert(conn, tx);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

            resp.sendRedirect(req.getContextPath() + "/admin/wallet?q=" + target.getUsername());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private BigDecimal parseAmount(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Connection conn, String query, String error)
            throws SQLException, ServletException, IOException {
        req.setAttribute("pageTitle", "Nạp ví EAUT Pay");
        if (error != null) {
            req.setAttribute("error", error);
        }
        if (query != null && !query.isBlank()) {
            User customer = userDAO.findByUsername(conn, query.trim());
            if (customer == null) {
                customer = userDAO.findByEmail(conn, query.trim());
            }
            if (customer == null || !customer.getRole().isCustomerDefault()) {
                req.setAttribute("notFound", true);
            } else {
                req.setAttribute("customer", customer);
                req.setAttribute("transactions", walletDAO.findByUser(conn, customer.getUserId()));
            }
            req.setAttribute("query", query);
        }
        req.getRequestDispatcher("/WEB-INF/views/admin/wallet.jsp").forward(req, resp);
    }
}
