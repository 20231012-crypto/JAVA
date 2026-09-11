package com.eaut.canteen.controller.customer;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.LoyaltyDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.WalletDAO;
import com.eaut.canteen.dao.impl.LoyaltyDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.dao.impl.WalletDAOImpl;
import com.eaut.canteen.model.User;
import com.eaut.canteen.model.WalletTopupRequest;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.VietQRUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Customer-facing "Ví của tôi": EAUT Pay balance + tích điểm + transaction history, and a
 * self-service "nạp ví qua VietQR" request. There is no real bank API link (see
 * WalletTopupRequest) — a request just produces a QR code; an admin still has to look at the bank
 * account and confirm before the balance actually moves (see admin WalletServlet).
 */
@WebServlet({"/wallet", "/wallet/topup-request"})
public class WalletServlet extends HttpServlet {

    private static final UserDAO userDAO = new UserDAOImpl();
    private static final WalletDAO walletDAO = new WalletDAOImpl();
    private static final LoyaltyDAO loyaltyDAO = new LoyaltyDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            showWallet(req, resp, conn, null);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User customer = (User) req.getSession().getAttribute("user");
        BigDecimal amount = parseAmount(req.getParameter("amount"));

        try (Connection conn = DBConnection.getConnection()) {
            if (amount == null || amount.compareTo(BigDecimal.valueOf(10_000)) < 0) {
                showWallet(req, resp, conn, "Số tiền nạp tối thiểu là 10.000đ.");
                return;
            }

            WalletTopupRequest request = new WalletTopupRequest();
            request.setUserId(customer.getUserId());
            request.setAmount(amount);
            walletDAO.insertTopupRequest(conn, request);

            resp.sendRedirect(req.getContextPath() + "/wallet?newRequest=" + request.getRequestId());
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

    private void showWallet(HttpServletRequest req, HttpServletResponse resp, Connection conn, String error)
            throws SQLException, ServletException, IOException {
        User sessionCustomer = (User) req.getSession().getAttribute("user");
        User customer = userDAO.findById(conn, sessionCustomer.getUserId());

        req.setAttribute("pageTitle", "Ví của tôi");
        req.setAttribute("customer", customer);
        req.setAttribute("walletTransactions", walletDAO.findByUser(conn, customer.getUserId()));
        req.setAttribute("loyaltyTransactions", loyaltyDAO.findByUser(conn, customer.getUserId()));
        req.setAttribute("topupRequests", walletDAO.findTopupRequestsByUser(conn, customer.getUserId()));
        if (error != null) {
            req.setAttribute("error", error);
        }

        String newRequestId = req.getParameter("newRequest");
        if (newRequestId != null) {
            WalletTopupRequest request = walletDAO.findTopupRequestById(conn, Integer.parseInt(newRequestId));
            if (request != null && request.getUserId() == customer.getUserId()) {
                req.setAttribute("newRequest", request);
                req.setAttribute("qrImageUrl", VietQRUtil.qrImageUrl(request.getTransferNote(), request.getAmount()));
            }
        }

        req.getRequestDispatcher("/WEB-INF/views/customer/wallet.jsp").forward(req, resp);
    }
}
