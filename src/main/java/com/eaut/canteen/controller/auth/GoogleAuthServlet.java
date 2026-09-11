package com.eaut.canteen.controller.auth;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.RoleDAO;
import com.eaut.canteen.dao.UserDAO;
import com.eaut.canteen.dao.impl.RoleDAOImpl;
import com.eaut.canteen.dao.impl.UserDAOImpl;
import com.eaut.canteen.model.AccountStatus;
import com.eaut.canteen.model.AuthProvider;
import com.eaut.canteen.model.Role;
import com.eaut.canteen.model.User;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.GoogleAuthUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Receives the POST from Google Identity Services' "Sign in with Google" button
 * (button is configured with data-ux_mode="redirect" / data-login_uri pointed here in
 * login-customer.jsp). Find-or-create: a customer's first Google sign-in creates their account,
 * every later one just logs them in — there is no separate registration step.
 *
 * Customer accounts only. Staff/Admin keep username+password login via LoginServlet,
 * provisioned by an Admin — this endpoint never touches non-customer-default rows.
 */
@WebServlet("/auth/google")
public class GoogleAuthServlet extends HttpServlet {

    /** EAUT Smart ID: any account signing up with this email domain gets the automatic checkout discount — see AppConfig "smartId.discountPercent". */
    private static final String EAUT_EMAIL_DOMAIN = "@eaut.edu.vn";

    private final UserDAO userDAO = new UserDAOImpl();
    private final RoleDAO roleDAO = new RoleDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!csrfTokenValid(req)) {
            fail(req, resp, "Yêu cầu đăng nhập không hợp lệ, vui lòng thử lại.");
            return;
        }

        Payload payload = GoogleAuthUtil.verify(req.getParameter("credential"));
        if (payload == null) {
            fail(req, resp, "Không thể xác thực tài khoản Google, vui lòng thử lại.");
            return;
        }

        String email = payload.getEmail();
        if (!Boolean.TRUE.equals(payload.getEmailVerified()) || email == null) {
            fail(req, resp, "Không thể xác thực email tài khoản Google, vui lòng thử lại.");
            return;
        }

        String googleSub = payload.getSubject();

        try (Connection conn = DBConnection.getConnection()) {
            User user = userDAO.findByGoogleSub(conn, googleSub);

            if (user == null) {
                User existing = userDAO.findByEmail(conn, email);
                if (existing == null) {
                    user = createCustomer(conn, payload, email, googleSub);
                } else if (existing.getRole().isCustomerDefault() && existing.getAuthProvider() == AuthProvider.LOCAL) {
                    // Same person's pre-Google account (created before this feature existed) — link it.
                    userDAO.linkGoogleAccount(conn, existing.getUserId(), googleSub);
                    user = existing;
                } else {
                    fail(req, resp, "Email này đã được dùng cho một tài khoản khác.");
                    return;
                }
            }

            if (user.getStatus() == AccountStatus.DISABLED) {
                fail(req, resp, "Tài khoản của bạn đã bị vô hiệu hoá.");
                return;
            }

            user.setPasswordHash(null);
            user.setPermissions(roleDAO.findPermissionMapForRole(conn, user.getRole().getRoleId()));
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            resp.sendRedirect(req.getContextPath() + "/products");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private User createCustomer(Connection conn, Payload payload, String email, String googleSub) throws SQLException {
        Object nameClaim = payload.get("name");
        String fullName = nameClaim == null ? email.substring(0, email.indexOf('@')) : nameClaim.toString();

        Role customerRole = roleDAO.findCustomerDefaultRole(conn);
        if (customerRole == null) {
            throw new IllegalStateException("No role has is_customer_default=TRUE — check /admin/roles or the seed data.");
        }

        User user = new User();
        user.setUsername(generateUsername(conn, email));
        user.setPasswordHash(null);
        user.setGoogleSub(googleSub);
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(customerRole);
        user.setStatus(AccountStatus.ACTIVE);
        user.setEautStudent(email.toLowerCase().endsWith(EAUT_EMAIL_DOMAIN));

        int userId = userDAO.insert(conn, user);
        user.setUserId(userId);
        return user;
    }

    /** Customers sign in with Google, not a chosen username — derive one from the email so every user row still has one. */
    private String generateUsername(Connection conn, String email) throws SQLException {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9._-]", "");
        if (base.isEmpty()) {
            base = "user";
        }
        if (base.length() > 45) {
            base = base.substring(0, 45);
        }
        String candidate = base;
        int suffix = 1;
        while (userDAO.existsByUsername(conn, candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    /**
     * Google's redirect-mode button POST carries a g_csrf_token both as a cookie and as a form
     * field; the two must match. This is Google's own documented CSRF defense for this flow —
     * without it, a page on another origin could POST an attacker's own valid Google credential
     * into a victim's browser session (a login-CSRF), signing the victim into the attacker's account.
     */
    private boolean csrfTokenValid(HttpServletRequest req) {
        String bodyToken = req.getParameter("g_csrf_token");
        if (bodyToken == null) {
            return false;
        }
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if ("g_csrf_token".equals(cookie.getName())) {
                return bodyToken.equals(cookie.getValue());
            }
        }
        return false;
    }

    private void fail(HttpServletRequest req, HttpServletResponse resp, String message)
            throws ServletException, IOException {
        LoginServlet.showCustomerForm(req, resp, message);
    }
}
