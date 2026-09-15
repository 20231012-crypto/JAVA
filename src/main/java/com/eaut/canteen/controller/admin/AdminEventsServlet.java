package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.Settings;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * What the admin header polls to learn that something new arrived: an order waiting to be
 * confirmed, or a top-up request waiting to be approved.
 *
 * <p>Polling rather than a WebSocket. There is a precedent — the catalog already polls
 * /products/recent-activity — and this costs one small query on an interval the admin can tune in
 * settings, against a container that would otherwise need connection-upgrade handling for a
 * notification bell. For a canteen, the difference between "instant" and "within twenty seconds"
 * does not justify that.
 *
 * <p>The response carries the newest arrival's timestamp rather than a boolean, so the client
 * decides for itself whether it has already seen this one. The server keeps no per-session state,
 * which is what lets two admins with several tabs open each get their own alerts without
 * interfering.
 */
@WebServlet("/admin/events")
public class AdminEventsServlet extends HttpServlet {

    // Epoch millis so the client can compare without parsing dates. COALESCE keeps the response
    // shape stable when there is nothing waiting at all.
    private static final String QUERY =
            "SELECT "
            + "(SELECT COUNT(*) FROM orders WHERE order_status = 'PENDING') AS pending_orders, "
            + "(SELECT COALESCE(MAX(EXTRACT(EPOCH FROM created_at) * 1000), 0) FROM orders "
            + " WHERE order_status = 'PENDING') AS newest_order_at, "
            + "(SELECT COUNT(*) FROM wallet_topup_requests WHERE status = 'PENDING') AS pending_topups, "
            + "(SELECT COALESCE(MAX(EXTRACT(EPOCH FROM created_at) * 1000), 0) FROM wallet_topup_requests "
            + " WHERE status = 'PENDING') AS newest_topup_at";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        // Polled every few seconds; a cached response would defeat the whole point.
        resp.setHeader("Cache-Control", "no-store");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(QUERY);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            long pendingOrders = rs.getLong("pending_orders");
            long newestOrderAt = rs.getLong("newest_order_at");
            long pendingTopups = rs.getLong("pending_topups");
            long newestTopupAt = rs.getLong("newest_topup_at");
            boolean soundEnabled = Settings.getBool(conn, "alert.soundEnabled", true);
            int pollSeconds = Settings.getInt(conn, "alert.pollSeconds", 20);

            // Written by hand rather than through a JSON library: the project has no JSON
            // dependency, and every value here is a long or a boolean this method produced, so
            // there is nothing that could need escaping.
            resp.getWriter().write("{"
                    + "\"pendingOrders\":" + pendingOrders
                    + ",\"newestOrderAt\":" + newestOrderAt
                    + ",\"pendingTopups\":" + pendingTopups
                    + ",\"newestTopupAt\":" + newestTopupAt
                    + ",\"soundEnabled\":" + soundEnabled
                    + ",\"pollSeconds\":" + pollSeconds
                    + "}");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
