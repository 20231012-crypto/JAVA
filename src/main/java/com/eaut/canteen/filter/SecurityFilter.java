package com.eaut.canteen.filter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.eaut.canteen.model.User;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Single merged auth+RBAC gate. Two separate filters would need a guaranteed execution
 * order (a role check assuming an auth check already ran) which the servlet spec does not
 * promise for annotation-declared filters — so both checks live in one filter instead.
 *
 * <p>Route -&gt; required-permission is a static table, not a fixed role, so which actual roles
 * can reach a route is entirely up to what's assigned in /admin/roles — a brand-new role with the
 * right permission gets in with zero code changes here. Rules are checked most-specific-first
 * (see RULES) since some sub-paths need a narrower permission than the rest of their prefix
 * (e.g. "/sales/orders/confirm" is stricter than plain "/sales/orders").
 */
@WebFilter("/*")
public class SecurityFilter implements Filter {

    private record PathRule(String prefix, String permission) {
    }

    static final String CSRF_PARAM = "csrfToken";
    /**
     * Google's Sign-In button POSTs here from accounts.google.com, so it can never carry our token.
     * It is not unprotected: GoogleAuthServlet checks Google's own g_csrf_token cookie/body pair,
     * which is the defence designed for that flow.
     */
    private static final String GOOGLE_AUTH_PATH = "/auth/google";

    private static final List<PathRule> RULES = List.of(
            new PathRule("/admin/buildings", "buildings.manage"),
            new PathRule("/admin/banners", "banners.manage"),
            new PathRule("/admin/categories", "categories.manage"),
            new PathRule("/admin/products", "products.manage"),
            new PathRule("/admin/staff", "staff.manage"),
            new PathRule("/admin/roles", "roles.manage"),
            new PathRule("/admin/stock-imports", "stock.import"),
            new PathRule("/admin/wallet", "wallet.topup"),
            new PathRule("/admin/customers", "customers.manage"),
            new PathRule("/admin/attendance", "attendance.view"),
            // Refunding moves real money, so it needs its own permission and MUST stay above the
            // plain /admin/orders rule — order matters here, first match wins.
            new PathRule("/admin/orders/refund", "orders.refund"),
            new PathRule("/admin/orders", "orders.manage"),
            new PathRule("/admin/inventory", "stock.adjust"),
            new PathRule("/admin/settings", "settings.manage"),
            new PathRule("/admin/effects", "settings.manage"),
            new PathRule("/admin/store-report", "reports.view"),
            new PathRule("/admin/reports", "reports.view"),
            new PathRule("/admin", "admin.dashboard"),
            new PathRule("/sales/orders/confirm", "orders.action"),
            new PathRule("/sales/orders/reject", "orders.action"),
            new PathRule("/sales/orders/cancel", "orders.action"),
            new PathRule("/sales/orders/mark-paid", "orders.payment_confirm"),
            new PathRule("/sales/orders", "orders.queue"),
            new PathRule("/sales/counter-sale", "sales.counter"),
            // The till moved to /pos as a sales channel of its own; same permission, so anyone who
            // could ring up a sale before still can and nobody new gained the ability.
            new PathRule("/pos", "sales.counter"),
            new PathRule("/sales/shop-status", "shop.status"),
            new PathRule("/store/transfers", "store.transfer"),
            new PathRule("/store/orders", "store.fulfillment"));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getServletPath();

        // Every request gets a token so that even a form rendered on a public page (the login
        // form) can carry one; it is minted once per session and reused.
        String csrfToken = ensureCsrfToken(req.getSession(true));
        req.setAttribute(CSRF_PARAM, csrfToken);

        if (isCsrfRejected(req, path, csrfToken)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên làm việc không hợp lệ, vui lòng tải lại trang.");
            return;
        }

        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login?redirect=" + path);
            return;
        }

        String requiredPermission = resolveRequiredPermission(path);
        if (requiredPermission != null && !user.hasPermission(requiredPermission)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }

    private String ensureCsrfToken(HttpSession session) {
        String token = (String) session.getAttribute(CSRF_PARAM);
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute(CSRF_PARAM, token);
        }
        return token;
    }

    /**
     * Synchronizer-token check on every state-changing request. GET/HEAD are not checked because
     * they must not change state in the first place; anything that does is the bug to fix.
     *
     * Reading the parameter also works for the multipart upload forms: the container parses the
     * body against the target servlet's @MultipartConfig and caches the parts, so the servlet's
     * own getPart() calls still see them afterwards.
     */
    private boolean isCsrfRejected(HttpServletRequest req, String path, String sessionToken) {
        if (!"POST".equalsIgnoreCase(req.getMethod()) || GOOGLE_AUTH_PATH.equals(path)) {
            return false;
        }
        String submitted = req.getParameter(CSRF_PARAM);
        return submitted == null || !submitted.equals(sessionToken);
    }

    /** null means "no admin/sales/store rule applies" — e.g. /cart, /checkout, /orders, which only require being logged in. */
    private String resolveRequiredPermission(String path) {
        for (PathRule rule : RULES) {
            if (isUnderPrefix(path, rule.prefix())) {
                return rule.permission();
            }
        }
        return null;
    }

    private boolean isPublic(String path) {
        return path.isEmpty() || path.equals("/")
                || path.equals("/login") || path.equals("/auth/google") || path.equals("/logout")
                || path.equals("/manifest.json") || path.equals("/sw.js")
                || isUnderPrefix(path, "/products")
                || isUnderPrefix(path, "/assets")
                || isUnderPrefix(path, "/images")
                || path.equals("/banner-image");
    }

    private boolean isUnderPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
