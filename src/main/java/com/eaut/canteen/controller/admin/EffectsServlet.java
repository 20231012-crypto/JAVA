package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;
import com.eaut.canteen.util.Settings;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Seasonal effects on the student-facing site: snow for Christmas, fireworks for Tết, falling
 * leaves for autumn.
 *
 * <p>The setting is read by header.jsp and published as a data attribute on &lt;body&gt;, so
 * turning an effect on takes effect on the next page load with no deploy and no cache to clear.
 * Storing it in app_settings rather than in its own table is deliberate — it is two values, and
 * the settings screen's generic renderer already knows how to edit them; this screen exists only
 * because picking an effect wants a preview and radio buttons rather than a text field.
 */
@WebServlet({"/admin/effects", "/admin/effects/save"})
public class EffectsServlet extends HttpServlet {

    /** Must match the modes assets/js/effects.js knows how to draw. */
    private static final List<String> MODES = List.of("NONE", "SNOW", "FIREWORKS", "LEAVES");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            req.setAttribute("effectsMode", Settings.getString(conn, "effects.mode", "NONE"));
            req.setAttribute("effectsIntensity", Settings.getInt(conn, "effects.intensity", 2));
            req.setAttribute("pageTitle", "Hiệu ứng trang chủ");
            req.getRequestDispatcher("/WEB-INF/views/admin/effects.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User admin = (User) req.getSession().getAttribute("user");
        String mode = req.getParameter("mode");
        // Whitelisted rather than trusted: the value ends up as a data attribute the client reads,
        // and an unknown mode would leave the canvas running with nothing to draw.
        if (mode == null || !MODES.contains(mode)) {
            mode = "NONE";
        }
        int intensity = RequestParams.intInRange(req.getParameter("intensity"), 2, 1, 3);

        try (Connection conn = DBConnection.getConnection()) {
            Settings.set(conn, "effects.mode", mode, admin.getUserId());
            Settings.set(conn, "effects.intensity", String.valueOf(intensity), admin.getUserId());
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        req.getSession().setAttribute("actionMessage", "Đã cập nhật hiệu ứng trang chủ.");
        resp.sendRedirect(req.getContextPath() + "/admin/effects");
    }
}
