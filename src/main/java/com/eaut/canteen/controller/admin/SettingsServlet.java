package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eaut.canteen.dao.AppSettingDAO;
import com.eaut.canteen.dao.impl.AppSettingDAOImpl;
import com.eaut.canteen.model.AppSetting;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.Settings;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The system settings screen. It renders itself from the app_settings rows — sections come from
 * group_name, labels from display_name, input controls from value_type — so a new setting is a
 * migration INSERT with no code change here.
 *
 * <p>Values are validated against their declared type before being written. A settings table is
 * read by checkout and by the reports; letting "abc" into loyalty.vndPerPoint would not throw
 * here, it would throw somewhere far away later.
 */
@WebServlet({"/admin/settings", "/admin/settings/save"})
public class SettingsServlet extends HttpServlet {

    private static final AppSettingDAO settingDAO = new AppSettingDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            render(conn, req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void render(Connection conn, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException {
        // LinkedHashMap because the DAO already returns the rows in display order; grouping must
        // not reshuffle them.
        Map<String, List<AppSetting>> groups = new LinkedHashMap<>();
        for (AppSetting setting : settingDAO.findAll(conn)) {
            groups.computeIfAbsent(setting.getGroupName(), key -> new ArrayList<>()).add(setting);
        }
        req.setAttribute("settingGroups", groups);
        req.setAttribute("pageTitle", "Cài đặt hệ thống");
        req.getRequestDispatcher("/WEB-INF/views/admin/settings.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User admin = (User) req.getSession().getAttribute("user");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<String> rejected = saveAll(conn, req, admin);
                if (rejected.isEmpty()) {
                    conn.commit();
                    req.getSession().setAttribute("actionMessage", "Đã lưu cài đặt.");
                } else {
                    // All or nothing: a half-applied settings change is confusing to diagnose, and
                    // the values interact (opening hours, discount rates) more than they look.
                    conn.rollback();
                    req.getSession().setAttribute("actionError",
                            "Giá trị không hợp lệ, chưa lưu gì cả: " + String.join(", ", rejected));
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

        // The cache is dropped by Settings.set, but only once the transaction it ran in has
        // committed can the new values actually be read — so clear it again here.
        Settings.invalidate();
        resp.sendRedirect(req.getContextPath() + "/admin/settings");
    }

    /**
     * Writes every setting present in the form.
     *
     * @return the display names of settings whose submitted value failed validation. Empty means
     *         everything was written.
     */
    private List<String> saveAll(Connection conn, HttpServletRequest req, User admin) throws SQLException {
        List<String> rejected = new ArrayList<>();
        for (AppSetting setting : settingDAO.findAll(conn)) {
            String key = setting.getSettingKey();
            String submitted;
            if (setting.isBooleanType()) {
                // An unchecked checkbox sends nothing at all, which is what makes "false" here
                // different from "the form did not include this field".
                submitted = req.getParameter("has_" + key) == null ? null
                        : String.valueOf(req.getParameter(key) != null);
            } else {
                submitted = req.getParameter(key);
            }
            if (submitted == null) {
                continue;
            }
            submitted = submitted.trim();
            if (!isValid(setting.getValueType(), submitted)) {
                rejected.add(setting.getDisplayName());
                continue;
            }
            Settings.set(conn, key, submitted, admin.getUserId());
        }
        return rejected;
    }

    /**
     * A value must parse as its declared type, and a few keys carry an extra rule that only makes
     * sense for them — a negative loyalty rate would be accepted by Integer.parseInt and then
     * silently award negative points.
     */
    private boolean isValid(String valueType, String value) {
        if (value.isEmpty()) {
            return false;
        }
        try {
            switch (valueType == null ? "STRING" : valueType) {
                case "INT" -> {
                    if (Integer.parseInt(value) < 0) {
                        return false;
                    }
                }
                case "DECIMAL" -> {
                    if (new BigDecimal(value).signum() < 0) {
                        return false;
                    }
                }
                case "TIME" -> LocalTime.parse(value);
                case "BOOL" -> {
                    if (!"true".equals(value) && !"false".equals(value)) {
                        return false;
                    }
                }
                default -> {
                    // A bad timezone would make every date query fall back silently, so it is
                    // checked here where the admin can still see the error.
                    return !value.contains("/") || isKnownZone(value);
                }
            }
            return true;
        } catch (NumberFormatException | DateTimeParseException e) {
            return false;
        }
    }

    private boolean isKnownZone(String value) {
        try {
            ZoneId.of(value);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
