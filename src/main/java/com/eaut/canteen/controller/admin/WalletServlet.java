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
import com.eaut.canteen.model.WalletTopupRequest;
import com.eaut.canteen.model.WalletTopupStatus;
import com.eaut.canteen.model.WalletTransaction;
import com.eaut.canteen.model.WalletTransactionType;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Admin EAUT Pay screen: confirm/reject customers' self-service "nạp ví" requests (see customer
 * WalletServlet) after checking the bank account by hand — there is no real bank API link, this
 * confirmation step IS the trust boundary — plus a manual top-up by username/email lookup for
 * cases with no bank transfer at all.
 */
@WebServlet({"/admin/wallet", "/admin/wallet/topup", "/admin/wallet/topup-requests/confirm", "/admin/wallet/topup-requests/reject"})
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
        switch (req.getServletPath()) {
            case "/admin/wallet/topup-requests/confirm" -> resolveTopupRequest(req, resp, WalletTopupStatus.CONFIRMED);
            case "/admin/wallet/topup-requests/reject" -> resolveTopupRequest(req, resp, WalletTopupStatus.REJECTED);
            default -> manualTopup(req, resp);
        }
    }

    private void manualTopup(HttpServletRequest req, HttpServletResponse resp)
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
                creditWallet(conn, userId, amount, admin.getUserId(),
                        "Admin " + admin.getUsername() + " nạp ví thủ công", null);
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

    private void resolveTopupRequest(HttpServletRequest req, HttpServletResponse resp, WalletTopupStatus resolution)
            throws ServletException, IOException {
        int requestId = Integer.parseInt(req.getParameter("requestId"));
        User admin = (User) req.getSession().getAttribute("user");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int updated = walletDAO.updateTopupRequestStatus(conn, requestId, resolution, admin.getUserId());
                if (updated > 0 && resolution == WalletTopupStatus.CONFIRMED) {
                    WalletTopupRequest request = walletDAO.findTopupRequestById(conn, requestId);
                    creditWallet(conn, request.getUserId(), request.getAmount(), admin.getUserId(),
                            "Xác nhận yêu cầu nạp " + request.getTransferNote(), null);
                }
                if (updated == 0) {
                    req.getSession().setAttribute("actionError", "Yêu cầu này đã được xử lý trước đó.");
                }
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

        resp.sendRedirect(req.getContextPath() + "/admin/wallet");
    }

    private void creditWallet(Connection conn, int userId, BigDecimal amount, int adminId, String note, Integer orderId) throws SQLException {
        userDAO.adjustWalletBalance(conn, userId, amount);
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setType(WalletTransactionType.TOPUP);
        tx.setCreatedBy(adminId);
        tx.setOrderId(orderId);
        tx.setNote(note);
        walletDAO.insert(conn, tx);
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
        req.setAttribute("pendingRequests", walletDAO.findPendingTopupRequests(conn));
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
